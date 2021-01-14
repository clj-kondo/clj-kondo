(ns clj-kondo.impl.utils
  {:no-doc true}
  (:require
   [clj-kondo.impl.rewrite-clj.node.keyword :as k]
   [clj-kondo.impl.rewrite-clj.node.protocols :as node]
   [clj-kondo.impl.rewrite-clj.node.seq :as seq]
   [clj-kondo.impl.rewrite-clj.node.string :as node-string]
   [clj-kondo.impl.rewrite-clj.node.token :as token]
   [clj-kondo.impl.rewrite-clj.parser :as p]
   [clojure.string :as str]))

;;; export rewrite-clj functions

(defn tag [expr]
  (node/tag expr))

(defn sexpr [expr]
  (node/sexpr expr))

(def vector-node seq/vector-node)
(def list-node seq/list-node)
(def token-node token/token-node)
(def keyword-node k/keyword-node)
(def string-node node-string/string-node)

;;; end export

(defn print-err! [& strs]
  (binding [*out* *err*]
    (apply println strs))
  nil)

(defn symbol-call
  "Returns symbol of call"
  [expr]
  (when (= :list (node/tag expr))
    (let [first-child (-> expr :children first)
          ?sym (:value first-child)]
      (when (symbol? ?sym)
        ?sym))))

(defn node->keyword
  "Returns keyword from node, if it contains any."
  [node]
  (:k node))

;; this zipper version is much slower than the above
#_(defn remove-noise
    ([expr] (remove-noise expr nil))
    ([expr config]
     (let [zloc (z/edn* expr)]
       (loop [zloc zloc]
         (cond (z/end? zloc) (z/root zloc)
               :else (let [node (z/node zloc)
                           remove?
                           (or (whitespace? node)
                               (uneval? node)
                               (comment? node))]
                       (recur (if remove? (z/remove zloc)
                                  (cz/next zloc)))))))))

(defn process-reader-conditional [node lang]
  (if (and node
           (= :reader-macro (node/tag node))
           (let [sv (-> node :children first :string-value)]
             (str/starts-with? sv "?")))
    (let [tokens (-> node :children last :children)]
      (loop [[k v & ts] tokens
             default nil]
        (let [kw (:k k)
              default (or default
                          (when (= :default kw)
                            v))]
          (if (= lang kw)
            (vary-meta  v assoc :branch lang)
            (if (seq ts)
              (recur ts default)
              default)))))
    node))

(declare select-lang)

(defn select-lang-children [node lang]
  (if-let [children (:children node)]
    (let [new-children (reduce
                        (fn [acc node]
                          (if-let [processed (select-lang node lang)]
                            (if (= "?@" (some-> node :children first :string-value))
                              (into acc (:children processed))
                              (conj acc processed))
                            acc))
                        []
                        children)]
      (assoc node :children
             new-children))
    node))

(defn select-lang [node lang]
  (when-let [processed (process-reader-conditional node lang)]
    (select-lang-children processed lang)))

(defn node->line [filename node level t message]
  #_(when (and (= type :missing-docstring)
             (not (:row (meta node))))
    (prn node))
  (let [m (meta node)]
    {:type t
     :message message
     :level level
     :row (:row m)
     :end-row (:end-row m)
     :end-col (:end-col m)
     :col (:col m)
     :filename filename}))

(defn parse-string [s]
  (p/parse-string s))

(defn parse-string-all
  [s]
  (p/parse-string-all s))

(def vconj (fnil conj []))

(defn deep-merge
  "deep merge that also mashes together sequentials"
  ([])
  ([a] a)
  ([a b]
   (cond (and (map? a) (map? b))
         (merge-with deep-merge a b)
         (and (sequential? a) (sequential? b))
         (into a b)
         (and (set? a) (set? b))
         (into a b)
         (false? b) b
         :else (or b a)))
  ([a b & more]
   (apply merge-with deep-merge a b more)))

(defn constant?
  "returns true of expr represents a compile time constant"
  [expr]
  (let [t (node/tag expr)]
    (case t
      ;; boolean, single-line string, char, number, keyword
      :token
      (not (symbol? (:value expr)))
      ;; multi-line string and quoted values
      (:multi-line :quote) true
      (:vector :set :map) (every? constant? (:children expr))
      :namespaced-map (every? constant? (-> expr :children first :children))
      false)))

(defn boolean-token? [node]
  (boolean? (:value node)))

(defn char-token? [node]
  (char? (:value node)))

(defn string-from-token [node]
  (when-let [lines (:lines node)]
    (str/join "\n" lines)))

(defn number-token? [node]
  (number? (:value node)))

(defn symbol-token? [node]
  (symbol? (:value node)))

(defn symbol-from-token [node]
  (when-let [?sym (:value node)]
    (when (symbol? ?sym)
      ?sym)))

(defn map-node-vals [{:keys [:children]}]
  (take-nth 2 (rest children)))

(defmacro one-of [x elements]
  `(let [x# ~x]
     (case x# (~@elements) x# nil)))

(defn linter-disabled? [ctx linter]
  (= :off (get-in ctx [:config :linters linter :level])))

(defn ctx-with-bindings [ctx bindings]
  (update ctx :bindings (fn [b]
                          (into b bindings))))

(defn kw->sym [^clojure.lang.Keyword k]
  (.sym k))

(defn- reduce-map
  "From medley"
  [f coll]
  (when coll
    (let [coll' (if (record? coll) (into {} coll) coll)
          e (empty coll')]
      (persistent! (reduce-kv (f assoc!) (transient e) coll')))))

(defn map-vals
  "Maps a function over the values of an associative collection. From medley."
  [f coll]
  (reduce-map (fn [xf] (fn [m k v] (xf m k (f v)))) coll))

(defn assoc-some
  "Associates a key with a value in a map, if and only if the value is
  not nil. From medley."
  ([m k v]
   (if (nil? v) m (assoc m k v)))
  ([m k v & kvs]
   (reduce (fn [m [k v]] (assoc-some m k v))
           (assoc-some m k v)
           (partition 2 kvs))))

(defn select-some
  "Like select-keys, but only selects when value is not nil."
  ([m ks]
   (persistent!
    (reduce (fn [acc k]
              (if-some [v (get m k)]
                (assoc! acc k v)
                acc))
            (transient {})
            ks))))

(defn keep-remove [p xs]
  (loop [xs xs
         kept (transient [])
         removed (transient [])]
    (if xs
      (let [x (first xs)] (if-let [v (p x)]
                            (recur (next xs)
                                   (conj! kept v) removed)
                            (recur (next xs) kept (conj! removed x))))
      [(persistent! kept) (persistent! removed)])))

(defn resolve-call* [idacs call fn-ns fn-name]
  ;; (prn "RES" fn-ns fn-name)
  (let [call-lang (:lang call)
        base-lang (:base-lang call)  ;; .cljc, .cljs or .clj file
        unresolved? (:unresolved? call)
        unknown-ns? (identical? fn-ns :clj-kondo/unknown-namespace)
        fn-ns (if unknown-ns? (:ns call) fn-ns)]
    ;; (prn "FN NS" fn-ns fn-name (keys (get (:defs (:clj idacs)) 'clojure.core)))
    (case [base-lang call-lang]
      [:clj :clj] (or (get-in idacs [:clj :defs fn-ns fn-name])
                      (get-in idacs [:cljc :defs fn-ns :clj fn-name]))
      [:cljs :cljs] (or (get-in idacs [:cljs :defs fn-ns fn-name])
                        ;; when calling a function in the same ns, it must be in another file
                        ;; an exception to this would be :refer :all, but this doesn't exist in CLJS
                        (when (not (and unknown-ns? unresolved?))
                          (or
                           ;; cljs func in another cljc file
                           (get-in idacs [:cljc :defs fn-ns :cljs fn-name])
                           ;; maybe a macro?
                           (get-in idacs [:clj :defs fn-ns fn-name])
                           (get-in idacs [:cljc :defs fn-ns :clj fn-name]))))
      ;; calling a clojure function from cljc
      [:cljc :clj] (or (get-in idacs [:clj :defs fn-ns fn-name])
                       (get-in idacs [:cljc :defs fn-ns :clj fn-name]))
      ;; calling function in a CLJS conditional from a CLJC file
      [:cljc :cljs] (or (get-in idacs [:cljs :defs fn-ns fn-name])
                        (get-in idacs [:cljc :defs fn-ns :cljs fn-name])
                        ;; could be a macro
                        (get-in idacs [:clj :defs fn-ns fn-name])
                        (get-in idacs [:cljc :defs fn-ns :clj fn-name])))))

(defn resolve-call [idacs call call-lang fn-ns fn-name unresolved? refer-alls]
  (when-let [called-fn
             (or (resolve-call* idacs call fn-ns fn-name)
                 (when unresolved?
                   (some #(resolve-call* idacs call % fn-name)
                         (into (vec
                                (keep (fn [[ns {:keys [:excluded]}]]
                                        (when-not (contains? excluded fn-name)
                                          ns))
                                      refer-alls))
                               (when (not (:clojure-excluded? call))
                                 [(case call-lang #_base-lang
                                        :clj 'clojure.core
                                        :cljs 'cljs.core
                                        :clj1c 'clojure.core)])))))]
    (if-let [imported-ns (:imported-ns called-fn)]
      (recur idacs call call-lang imported-ns
             (:imported-var called-fn) unresolved? refer-alls)
      called-fn)))

(defn handle-ignore [ctx expr]
  (let [cljc? (identical? :cljc (:base-lang ctx))
        lang (:lang ctx)
        m (meta expr)]
    (when-let [ignore-node (:clj-kondo/ignore m)]
      (let [node (if cljc?
                   (select-lang ignore-node (:lang ctx))
                   ignore-node)
            ignore (node/sexpr node)
            ignore (if (boolean? ignore) ignore (set ignore))]
        (swap! (:ignores ctx) update-in [(:filename ctx) lang]
               vconj (assoc m :ignore ignore))))))

;;;; Scratch

(comment
  (false? (node/sexpr (parse-string "false")))
  (false? (node/sexpr (parse-string "nil")))
  (constant? (parse-string "foo"))
  (map-node-vals (parse-string "{:a 1 :b 2}"))
  (assoc-some {} :a 1 :b nil :c false)
  (select-some {:a 1 :b nil :c 2 :d false} [:a :b :d]) ;; => {:a 1, :d false}
  (tag (parse-string "\"x

y\""))
  (tag (parse-string "\"xy\""))
  )
