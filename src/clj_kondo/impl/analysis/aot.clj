(ns clj-kondo.impl.analysis.aot
  "Extract Clojure var definitions from AOT-compiled __init.class bytecode."
  {:no-doc true}
  (:require
   [clj-kondo.impl.analysis.java :as java]
   [clojure.string :as str])
  (:import
   [java.util.jar JarFile JarFile$JarFileEntry]
   [org.objectweb.asm ClassReader ClassVisitor MethodVisitor Opcodes Type]))

(set! *warn-on-reflection* true)

(defn init-class->ns-name
  "Derives namespace name from __init.class path.
  Note: this is approximate since underscores in paths may be hyphens
  in actual namespace names. Use the ns from RT.var bytecode when available."
  [^String entry-name]
  (-> entry-name
      (str/replace "__init.class" "")
      (str/replace "/" ".")
      (str/replace #"\.$" "")))

(defn ^:private collect-bytecode-events
  "Collect bytecode events from __init* and load() methods in a single pass.
  Returns {:init-events [...] :load-events [...]}."
  [^bytes class-bytes]
  (let [reader (ClassReader. class-bytes)
        init-events (atom [])
        load-events (atom [])]
    (.accept
     reader
     (proxy [ClassVisitor] [Opcodes/ASM9]
       (visitMethod [access ^String name desc sig exceptions]
         (cond
           (str/starts-with? name "__init")
           (proxy [MethodVisitor] [Opcodes/ASM9]
             (visitLdcInsn [value]
               (swap! init-events conj {:type :ldc :value value}))
             (visitMethodInsn [_opcode ^String owner ^String mname ^String desc itf]
               (swap! init-events conj {:type :method :name mname :owner owner :desc desc}))
             (visitFieldInsn [opcode _owner ^String fname ^String desc]
               (swap! init-events conj {:type :field
                                        :op (if (= opcode Opcodes/PUTSTATIC)
                                              :putstatic :getstatic)
                                        :name fname
                                        :desc desc})))

           (= "load" name)
           (proxy [MethodVisitor] [Opcodes/ASM9]
             (visitMethodInsn [_opcode ^String owner ^String mname ^String desc itf]
               (swap! load-events conj {:type :method :name mname :owner owner}))
             (visitFieldInsn [opcode _owner ^String fname ^String desc]
               (when (= opcode Opcodes/GETSTATIC)
                 (swap! load-events conj {:type :getstatic :name fname :desc desc}))))

           :else nil)))
     0)
    {:init-events @init-events
     :load-events @load-events}))

(defn ^:private extract-var-defs
  "Extract var names and const field mappings from __init* events.
  Returns {const-field-name {:ns str, :name str, :private bool}}."
  [init-events]
  (let [n (count init-events)]
    (loop [i 0
           const->var {}]
      (if (>= i n)
        const->var
        (let [evt (nth init-events i)]
          (if (and (= :method (:type evt))
                   (= "var" (:name evt))
                   (= "clojure/lang/RT" (:owner evt)))
            ;; RT.var call — look back for ns and name strings
            (let [ns-str (some (fn [j]
                                 (let [e (nth init-events j)]
                                   (when (and (= :ldc (:type e))
                                              (string? (:value e)))
                                     (:value e))))
                               (range (- i 2) -1 -1))
                  name-str (some (fn [j]
                                   (let [e (nth init-events j)]
                                     (when (and (= :ldc (:type e))
                                                (string? (:value e)))
                                       (:value e))))
                                 (range (- i 1) -1 -1))
                  ;; Look forward for PUTSTATIC const__N with Var desc
                  const-field (some (fn [j]
                                      (when (< j n)
                                        (let [e (nth init-events j)]
                                          (when (and (= :field (:type e))
                                                     (= :putstatic (:op e))
                                                     (= "Lclojure/lang/Var;" (:desc e)))
                                            (:name e)))))
                                    (range (inc i) (min (+ i 3) n)))]
              (if (and ns-str name-str const-field)
                ;; Scan metadata region for :private keyword
                ;; Metadata is between this var's PUTSTATIC and the next RT.var call
                (let [next-var-idx (or (some (fn [j]
                                               (let [e (nth init-events j)]
                                                 (when (and (= :method (:type e))
                                                            (= "var" (:name e))
                                                            (= "clojure/lang/RT" (:owner e)))
                                                   j)))
                                             (range (+ i 3) n))
                                       n)
                      meta-region (subvec (vec init-events) (inc i) next-var-idx)
                      private? (some (fn [[a b]]
                                       (and (= :ldc (:type a))
                                            (= "private" (:value a))
                                            (= :method (:type b))
                                            (= "keyword" (:name b))
                                            (= "clojure/lang/RT" (:owner b))))
                                     (partition 2 1 meta-region))]
                  (recur (inc i)
                         (assoc const->var const-field
                                {:ns ns-str
                                 :name name-str
                                 :private (boolean private?)})))
                (recur (inc i) const->var)))
            (recur (inc i) const->var)))))))

(defn ^:private extract-macro-vars
  "Extract which const fields have setMacro called in load().
  Returns a set of const field names."
  [load-events]
  (let [n (count load-events)]
    (loop [i 0
           current-const nil
           macros #{}]
      (if (>= i n)
        macros
        (let [evt (nth load-events i)]
          (cond
            ;; Track which const is on the stack
            (and (= :getstatic (:type evt))
                 (= "Lclojure/lang/Var;" (:desc evt)))
            (recur (inc i) (:name evt) macros)

            ;; setMacro on current const
            (and (= :method (:type evt))
                 (= "setMacro" (:name evt)))
            (recur (inc i) current-const
                   (if current-const (conj macros current-const) macros))

            :else
            (recur (inc i) current-const macros)))))))

(defn ^:private extract-fn-class-bindings
  "Extract which fn class is bound to which const field in load().
  Returns {const-field fn-class-name}."
  [load-events]
  (let [n (count load-events)]
    (loop [i 0
           current-const nil
           fn-class nil
           bindings {}]
      (if (>= i n)
        bindings
        (let [evt (nth load-events i)]
          (cond
            ;; GETSTATIC const__N (Var) — track current var
            (and (= :getstatic (:type evt))
                 (= "Lclojure/lang/Var;" (:desc evt)))
            (recur (inc i) (:name evt) fn-class bindings)

            ;; <init> on a fn class — track which fn is being created
            (and (= :method (:type evt))
                 (= "<init>" (:name evt))
                 (str/includes? (:owner evt) "$"))
            (recur (inc i) current-const (:owner evt) bindings)

            ;; bindRoot — associate fn class with const
            (and (= :method (:type evt))
                 (= "bindRoot" (:name evt))
                 current-const fn-class)
            (recur (inc i) current-const nil
                   (assoc bindings current-const fn-class))

            :else
            (recur (inc i) current-const fn-class bindings)))))))

(defn ^:private fn-class-arities
  "Extract arities from a function class by examining invoke method overloads.
  Returns {:fixed-arities #{int ...} :varargs-min-arity int-or-nil}."
  [^bytes class-bytes]
  (let [reader (ClassReader. class-bytes)
        superclass (atom nil)
        arities (atom #{})
        required-arity (atom nil)]
    (.accept
     reader
     (proxy [ClassVisitor] [Opcodes/ASM9]
       (visit [version access name signature ^String super-name interfaces]
         (reset! superclass super-name)
         nil)
       (visitMethod [access ^String name ^String desc sig exceptions]
         (cond
           ;; invoke methods — count argument types, not return type
           (= "invoke" name)
           (do (swap! arities conj (alength (Type/getArgumentTypes desc)))
               nil)

           ;; getRequiredArity for RestFn subclasses — need to read body
           (= "getRequiredArity" name)
           (proxy [MethodVisitor] [Opcodes/ASM9]
             (visitInsn [opcode]
               (when (<= Opcodes/ICONST_0 opcode Opcodes/ICONST_5)
                 (reset! required-arity (- opcode Opcodes/ICONST_0))))
             (visitIntInsn [opcode operand]
               (when (= opcode Opcodes/BIPUSH)
                 (reset! required-arity operand))))

           :else nil)))
     0)
    (let [is-rest-fn? (= "clojure/lang/RestFn" @superclass)
          fixed @arities
          req @required-arity]
      (if is-rest-fn?
        {:varargs-min-arity req}
        {:fixed-arities fixed}))))

(defn extract-ns-vars
  "Extract var definitions from a Clojure AOT __init.class and its
  companion fn classes in the same jar.

  Returns a cache-compatible map:
  {var-sym {:ns ns-sym, :name var-sym, :fixed-arities #{...}, ...}}"
  [^JarFile jar ^JarFile$JarFileEntry init-entry]
  (let [init-bytes (with-open [is (.getInputStream jar init-entry)]
                     (java/input-stream->bytes is))
        {:keys [init-events load-events]} (collect-bytecode-events init-bytes)
        const->var (extract-var-defs init-events)
        macro-consts (extract-macro-vars load-events)
        fn-bindings (extract-fn-class-bindings load-events)
        ;; Determine actual ns name from the var defs (most common ns string)
        ns-name (->> (vals const->var)
                     (map :ns)
                     (remove #{"clojure.core"})
                     frequencies
                     (sort-by val >)
                     ffirst)]
    (reduce-kv
     (fn [acc const-field {:keys [ns name private]}]
       ;; Skip clojure.core/in-ns and other non-ns vars
       (if (not= ns ns-name)
         acc
         (let [var-sym (symbol name)
               macro? (contains? macro-consts const-field)
               fn-class (get fn-bindings const-field)
               arity-info (when fn-class
                            (when-let [fn-entry (.getJarEntry jar
                                                              (str fn-class ".class"))]
                              (let [fn-bytes (with-open [is (.getInputStream jar fn-entry)]
                                               (java/input-stream->bytes is))]
                                (fn-class-arities fn-bytes))))
               ;; Macros have 2 implicit args (&form, &env) in bytecode
               arity-info (if (and macro? arity-info)
                            (cond-> arity-info
                              (:fixed-arities arity-info)
                              (update :fixed-arities
                                      (fn [arities] (into #{} (map #(- % 2)) arities)))
                              (:varargs-min-arity arity-info)
                              (update :varargs-min-arity - 2))
                            arity-info)
               var-meta (cond-> {:ns (symbol ns-name)
                                 :name var-sym}
                          (:fixed-arities arity-info)
                          (assoc :fixed-arities (:fixed-arities arity-info))

                          (:varargs-min-arity arity-info)
                          (assoc :varargs-min-arity (:varargs-min-arity arity-info))

                          (or fn-class macro?)
                          (assoc :type (if macro? :macro :fn))

                          private
                          (assoc :private true)

                          macro?
                          (assoc :macro true))]
           (assoc acc var-sym var-meta))))
     {}
     const->var)))
