(ns clj-kondo.impl.vars
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [some-call call node->line
                                 parse-string parse-string-all]]
   [clj-kondo.impl.namespace :refer [analyze-ns-decl]]
   [clojure.set :as set]
   [rewrite-clj.node.protocols :as node]
   [clojure.string :as str]))

;;;; function arity

(defn arg-name [{:keys [:children] :as expr}]
  ;; TODO: use strip-meta
  (if-let [n (:value expr)]
    ;; normal argument
    n
    ;; this is an argument with metadata
    (-> children last :value)))

(defn analyze-arity [{:keys [:children] :as arg-decl}]
  (loop [args children
         arity 0
         ;; max-arity nil
         ;; varargs? false
         arg-names #{}]
    (if-let [arg (first args)]
      (if (= '& (:value arg))
        {:arg-names arg-names
         :min-arity arity
         :varargs? true}
        (recur (rest args)
               (inc arity)
               ;; varargs?
               (conj arg-names (arg-name arg))))
      {:arg-names arg-names
       :fixed-arity arity})))

(defn analyze-in-ns [{:keys [:children] :as expr}]
  (let [ns-name (-> children second :children first :value)]
    {:type :in-ns
     :name ns-name}))

(defn fn-call? [expr]
  (let [tag (node/tag expr)]
    (and (= :list tag)
         (symbol? (:value (first (:children expr)))))))

(defn strip-meta* [children]
  (loop [[child & rest-children] children
         stripped []]
    (if child
      (if (contains? '#{:meta :meta*} (node/tag child))
        (recur rest-children
               (into stripped (strip-meta* (rest (:children child)))))
        (recur rest-children
               (conj stripped child)))
      stripped)))

(defn strip-meta [expr]
  (assoc expr
         :children (strip-meta* (:children expr))))

(declare parse-arities)

(defn parse-defn [lang bindings expr]
  ;; TODO: switch to sexpr instead of parsing rewrite-clj output
  (let [macro? (= 'defmacro (call expr))
        children (:children (strip-meta expr))
        ;; TODO: add metadata parsing for private
        private? (= 'defn- (some-call expr defn-))
        children (rest children)
        children (strip-meta* children)
        fn-name (:value (first (filter #(symbol? (:value %)) children)))
        ;;_ (println "FN-NAME" fn-name)
        arg-decl (first (filter #(= :vector (:tag %)) children))
        arg-decls (map (fn [x]
                         ;; skip docstring, etc.
                         (first
                          (keep
                           #(case (:tag %)
                              :vector %
                              :meta (last (:children %))
                              nil)
                           (:children x))))
                       (filter #(= :list (:tag %)) (rest children)))
        arg-decls (if arg-decl [arg-decl]
                      arg-decls)
        arities (map analyze-arity arg-decls)
        fixed-arities (set (keep :fixed-arity arities))
        var-args-min-arity (:min-arity (first (filter :varargs? arities)))
        defn
        (let [{:keys [:row :col]} (meta expr)]
          (if fn-name
            (cond-> {:type :defn
                     :name fn-name
                     :row row
                     :col col
                     :lang lang}
              ;; not yet:
              ;; macro? (assoc :macro true)
              (seq fixed-arities) (assoc :fixed-arities fixed-arities)
              private? (assoc :private? private?)
              var-args-min-arity (assoc :var-args-min-arity var-args-min-arity))
            {:type :debug
             :level :info
             :message "Could not parse defn form"
             :row row
             :col col
             :lang lang}))]
    (cons defn
          (mapcat
           #(parse-arities lang (reduce set/union bindings
                                        (map :arg-names arities))
                           %)
           (rest children)))))

(defn parse-case [lang bindings expr]
  (let [exprs (-> expr :children)]
    (loop [[constant expr :as exprs] exprs
           parsed []]
      (if-not expr
        (into parsed (when constant
                       (parse-arities lang bindings constant)))
        (recur
         (nnext exprs)
         (into parsed (parse-arities lang bindings expr)))))))

(comment
  (parse-case :clj #{} (parse-string-all "(case (+ 1 2 3) (1 2 3) (+ 1 2 3) (+ 2 3 4))"))
  (parse-case :clj #{} (parse-string "(case (+ 1 2 3) (1 2 3) (+ 1 2 3))"))
  )

(defn parse-arities
  ;; TODO: refactor and split into multiple functions
  ;; TODO: handle case, we should not parse the list constants as function calls
  ([lang expr] (parse-arities lang #{} expr))
  ([lang bindings {:keys [:children] :as expr}]
   (let [?full-fn-name (call expr)
         ;; TODO: better resolving for qualified vars...
         fn-name (when ?full-fn-name (symbol (name ?full-fn-name)))
         t (node/tag expr)]
     (if (contains? '#{:quote :syntax-quote} t)
       []
       (case fn-name
         ns
         [(analyze-ns-decl lang expr)]
         ;; TODO: in-ns is not supported yet
         ;; One thing to note: if in-ns is used in a function body, the rest of the namespace is now analyzed in that namespace, which is incorrect.
         (defn defn- defmacro)
         (parse-defn lang bindings expr)
         ;; TODO: better resolving for these macro calls
         (->> cond-> cond->> some-> some->> . .. deftype
              proxy extend-protocol doto reify definterface defrecord defprotocol)
         []
         let
         (let [let-bindings (->> children second :children (map :value) (filter symbol?) set)]
           (mapcat #(parse-arities lang (set/union bindings let-bindings) %) (rest children)))
         (fn fn*)
         ;; TODO better arity analysis like in normal fn
         (let [arg-vec (first (filter #(= :vector (node/tag %)) (rest children)))
               maybe-bindings (->> arg-vec :children (map :value))
               fn-bindings (set (filter symbol? (cons fn-name maybe-bindings)))]
           (mapcat #(parse-arities lang (set/union bindings fn-bindings) %) (rest children)))
         case
         (parse-case lang bindings expr)
         ;; catch-all
         (if (symbol? fn-name)
           (let [args (count (rest children))
                 binding-call? (contains? bindings fn-name)
                 parse-rest (mapcat #(parse-arities lang bindings %) (rest children))]
             (if binding-call?
               parse-rest
               (cons
                (let [{:keys [:row :col]} (meta expr)]
                  {:type :call
                   :name ?full-fn-name
                   :arity args
                   :row row
                   :col col
                   :lang lang
                   :expr expr})
                parse-rest)))
           (mapcat #(parse-arities lang bindings %) children)))))))

(defn resolve-name
  [ns name-sym]
  (if-let [ns* (namespace name-sym)]
    (let [ns-sym (symbol ns*)]
      (if-let [ns* (get (:qualify-ns ns) ns-sym)]
        {:ns ns*
         :name (symbol (name name-sym))}
        (when-let [ns* (get (:java-imports ns) ns-sym)]
          {:java-interop? true
           :ns ns*
           :name (symbol (name name-sym))})))
    (or (get (:qualify-var ns)
             name-sym)
        (let [namespace (:name ns)]
          {:ns namespace
           :name name-sym
           :unqualified? true
           :clojure-excluded? (contains? (:clojure-excluded ns)
                                         name-sym)}))))

(def vconj (fnil conj []))

(defn analyze-arities
  "Collects defs and calls into a map. To optimize cache lookups later
  on, calls are indexed by the namespace they call to, not the
  ns where the call occurred."
  ([filename lang expr] (analyze-arities filename lang lang expr))
  ([filename lang expanded-lang expr] (analyze-arities filename lang expanded-lang expr false))
  ([filename lang expanded-lang expr debug?]
   (loop [[first-parsed & rest-parsed] (parse-arities expanded-lang expr)
          ns (analyze-ns-decl expanded-lang (parse-string "(ns user)"))
          results {:calls {}
                   :defs {}
                   :loaded (:loaded ns)
                   :findings []
                   :lang lang}]
     ;; (println "NS" (:loaded ns))
     ;; (println "REQUIRED" (:loaded results))
     (if first-parsed
       (case (:type first-parsed)
         (:ns :in-ns)
         (recur rest-parsed
                first-parsed
                (update results
                        :loaded into (:loaded first-parsed)))
         (recur rest-parsed
                ns
                (case (:type first-parsed)
                  :debug
                  (if debug?
                    (update-in results
                               [:findings]
                               conj
                               (assoc first-parsed
                                      :filename filename))
                    results)
                  (let [resolved (resolve-name ns (:name first-parsed))
                        first-parsed (cond->
                                         (assoc first-parsed
                                                :name (:name resolved)
                                                :ns (:name ns))
                                       ;; if defined in CLJC file, we add that as the base-lang
                                       (= :cljc lang)
                                       (assoc :base-lang lang))]
                    (case (:type first-parsed)
                      :defn
                      (let [path (case lang
                                   :cljc [:defs (:name ns) (:lang first-parsed) (:name resolved)]
                                   [:defs (:name ns) (:name resolved)])
                            results
                            (if resolved
                              (assoc-in results path
                                        (dissoc first-parsed
                                                :type))
                              results)]
                        (if debug?
                          (update-in results
                                     [:findings]
                                     vconj
                                     (assoc first-parsed
                                            :level :info
                                            :filename filename
                                            :message
                                            (str/join " "
                                                      ["Defn resolved as"
                                                       (str (:ns resolved) "/" (:name resolved)) "with arities"
                                                       "fixed:"(:fixed-arities first-parsed)
                                                       "varargs:"(:var-args-min-arity first-parsed)])
                                            :type :debug))
                          results))
                      :call
                      (if resolved
                        (let [path [:calls (:ns resolved)]
                              unqualified? (:unqualified? resolved) 
                              call (cond-> (assoc first-parsed
                                                  :filename filename
                                                  :resolved-ns (:ns resolved)
                                                  :ns-lookup ns)
                                     (:clojure-excluded? resolved)
                                     (assoc :clojure-excluded? true)
                                     unqualified?
                                     (assoc :unqualified? true))
                              results (cond-> (update-in results path vconj call)
                                        (not unqualified?)
                                        ;; java calls will be done this way
                                        (update :loaded conj (:ns resolved)))]
                          (if debug? (update-in results [:findings] conj
                                                (assoc call
                                                       :level :info
                                                       :message (str "Call resolved as "
                                                                     (str (:ns resolved) "/" (:name resolved)))
                                                       :type :debug))
                              results))
                        (if debug?
                          (update-in results
                                     [:findings]
                                     conj
                                     (assoc first-parsed
                                            :level :info
                                            :message (str "Unrecognized call to "
                                                          (:name first-parsed))
                                            :type :debug))
                          results))
                      results)))))
       results))))

(defn lint-cond [filename expr]
  (let [last-condition
        (->> expr :children
             (take-last 2) first :k)]
    (when (not= :else last-condition)
      [(node->line filename expr :warning :cond-without-else "cond without :else")])))

(defn lint-deftest [config filename expr]
  (let [calls (nnext (:children expr))]
    (for [c calls
          :let [fn-name (some-> c :children first :string-value)]
          :when (and fn-name
                     (not (when-let [excluded (-> config :missing-test-assertion :exclude)]
                            (contains? excluded (symbol fn-name))))
                     (or (= "=" fn-name) (str/ends-with? fn-name "?")))]
      (node->line filename c :warning :missing-test-assertion "missing test assertion"))))

(defn var-specific-findings [config filename call called-fn]
  (case [(:ns called-fn) (:name called-fn)]
    [clojure.core cond] (lint-cond filename (:expr call))
    [cljs.core cond] (lint-cond filename (:expr call))
    #_#_[clojure.test deftest] (lint-deftest config filename (:expr call))
    #_#_[cljs.test deftest] (lint-deftest config filename (:expr call))
    []))

(defn resolve-call [idacs call fn-ns fn-name]
  ;; TODO: for cljs -> clj/cljc calls we can probably store whether a function is a macro or not and use that
  ;; call lang clj. [foo.core] can come from another .clj or .cljc file
  ;; call lang cljs. [foo.core] can come from another .cljs, .clj (macros) or .cljc file
  ;; call lang cljc. [foo.core]. we should split this call into a clj and cljs one (see #67). for now, we'll only look into .clj.
  #_(prn "FN-ns" fn-ns "FN-name" fn-name)
  (let [call-lang (:lang call)
        base-lang (or (:base-lang call) call-lang) ;; .cljc, .cljs or .clj file
        caller-ns (:ns call)
        ;; this call was unqualified and inferred as a function in the same namespace
        unqualified? (:unqualified? call)
        same-ns? (= caller-ns fn-ns)]
    ;; (prn (dissoc call :ns-lookup))
    #_(println "IDACS" (keys (get-in idacs [:cljs :defs fn-ns #_fn-name])))
    (case [base-lang call-lang]
      ;; call from clojure in a clojure file
      ;; could be a call to another clojure file or a .cljc file
      [:clj :clj] (or (get-in idacs [:clj :defs fn-ns fn-name])
                      (get-in idacs [:cljc :defs fn-ns :clj fn-name]))
      [:cljs :cljs] (or (get-in idacs [:cljs :defs fn-ns fn-name])
                        (when-not (and same-ns? unqualified?)
                          (or ;; maybe a macro?
                           (get-in idacs [:cljc :defs fn-ns :cljs fn-name])
                           (get-in idacs [:clj :defs fn-ns fn-name])
                           (get-in idacs [:cljc :defs fn-ns :clj fn-name]))))
      ;; calling a clojure function from cljc
      [:cljc :clj] (or (get-in idacs [:clj :defs fn-ns fn-name])
                       (get-in idacs [:cljc :defs fn-ns :clj fn-name]))
      ;; calling a CLJS function or CLJ(S) macro from cljc
      [:cljc :cljs] (or (get-in idacs [:cljs :defs fn-ns fn-name])
                        (get-in idacs [:cljc :defs fn-ns :cljs fn-name])
                        (when true ;; FIXME
                          (or (get-in idacs [:clj :defs fn-ns fn-name])
                              (get-in idacs [:cljc :defs fn-ns :clj fn-name])))))))

(defn fn-call-findings
  "Analyzes indexed defs and calls and returns findings."
  [idacs config]
  (let [findings (for [lang [:clj :cljs :cljc]
                       ns-sym (keys (get-in idacs [lang :calls]))
                       call (get-in idacs [lang :calls ns-sym])
                       :let [;; _ (prn "call" (dissoc call :ns-lookup))
                             fn-name (:name call)
                             caller-ns (:ns call)
                             fn-ns (:resolved-ns call)
                             called-fn
                             (or (resolve-call idacs call fn-ns fn-name)
                                 ;; we resolved this call against the
                                 ;; same namespace, because it was
                                 ;; unqualified
                                 (when (= caller-ns fn-ns)
                                   (some #(resolve-call idacs call % fn-name)
                                         (into (vec
                                                (keep (fn [[ns excluded]]
                                                        (when-not (contains? excluded fn-name)
                                                          ns))
                                                      (-> call :ns-lookup :refer-alls)))
                                               (when (not (:clojure-excluded? call))
                                                 [(case lang
                                                    :clj 'clojure.core
                                                    :cljs 'cljs.core
                                                    :cljc 'clojure.core)])))))
                             fn-ns (:ns called-fn)]
                       :when called-fn
                       :let [;; _ (prn "CALL" (dissoc call :ns-lookup :expr))
                             ;; _ (prn "CALLED FN" called-fn)
                             ;; a macro in a CLJC file with the same namespace
                             ;; in that case, looking at the row and column is
                             ;; not reliable.  we may look at the lang of the
                             ;; call and the lang of the function def context in
                             ;; the case of in-ns, the bets are off. we may
                             ;; support in-ns in a next version.
                             valid-order? (if (and (= caller-ns
                                                      fn-ns)
                                                   (= (:base-lang call)
                                                      (:base-lang called-fn)))
                                            (or (> (:row call) (:row called-fn))
                                                (and (= (:row call) (:row called-fn))
                                                     (> (:col call) (:col called-fn))))
                                            true)]
                       :when valid-order?
                       :let [arity (:arity call)
                             filename (:filename call)
                             fixed-arities (:fixed-arities called-fn)
                             var-args-min-arity (:var-args-min-arity called-fn)
                             errors
                             (into
                              [(when-not
                                   (or (contains? fixed-arities arity)
                                       (and var-args-min-arity (>= arity var-args-min-arity))
                                       (when-let [excluded (-> config :invalid-arity :exclude)]
                                         (contains? excluded
                                                    (symbol (str fn-ns)
                                                            (str fn-name)))))
                                 {:filename filename
                                  :row (:row call)
                                  :col (:col call)
                                  :level :error
                                  :type :invalid-arity
                                  :message (format "wrong number of args (%s) passed to %s"
                                                   (str (:arity call))
                                                   (str (:ns called-fn) "/" (:name called-fn)))})
                               (when (and (:private? called-fn)
                                          (not= caller-ns
                                                fn-ns))
                                 {:filename filename
                                  :row (:row call)
                                  :col (:col call)
                                  :level :error
                                  :type :private-call
                                  :message (format "call to private function %s"
                                                   (:name call))})]
                              (var-specific-findings config filename call called-fn))]
                       e errors
                       :when e]
                   e)]
    findings))

;;;; Scratch

(comment
  )
