(ns clj-kondo.impl.calls
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
        {:keys [:row :col]} (meta expr)
        defn
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
           :lang lang})
        call {:type :call
              :name 'defn
              :row row
              :col col
              :lang lang
              :expr expr
              :arity (count children)}]
    (into [defn call]
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

(defn parse-arities
  ;; TODO: refactor and split into multiple functions
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

(defn analyze-calls
  "Collects defs and calls into a map. To optimize cache lookups later
  on, calls are indexed by the namespace they call to, not the
  ns where the call occurred."
  ([filename lang expr] (analyze-calls filename lang lang expr))
  ([filename lang expanded-lang expr] (analyze-calls filename lang expanded-lang expr false))
  ([filename lang expanded-lang expr debug?]
   (loop [[first-parsed & rest-parsed] (parse-arities expanded-lang expr)
          ns (analyze-ns-decl expanded-lang (parse-string "(ns user)"))
          results {:calls {}
                   :defs {}
                   :loaded (:loaded ns)
                   :findings []
                   :lang lang}]
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

(defn lint-def* [filename expr in-def?]
  (let [fn-name (call expr)
        simple-fn-name (when fn-name (symbol (name fn-name)))]
    ;; TODO: it would be nicer if we could have the qualified calls of this expression somehow
    ;; so we wouldn't have to deal with these primitive expressions anymore
    (when-not (= 'case simple-fn-name)
      (let [current-def? (contains? '#{expr def defn defn- deftest defmacro} fn-name)
            new-in-def? (and (not (contains? '#{:syntax-quote :quote}
                                             (node/tag expr)))
                             (or in-def? current-def?))]
        (if (and in-def? current-def?)
          [(node->line filename expr :warning :inline-def "inline def")]
          (when (:children expr)
            (mapcat #(lint-def* filename % new-in-def?) (:children expr))))))))

(defn lint-def [filename expr]
  (mapcat #(lint-def* filename % true) (:children expr)))

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

(defn call-specific-findings [config filename call called-fn]
  (case (:ns called-fn)
    (clojure.core cljs.core)
    (case (:name called-fn)
      (cond) (lint-cond filename (:expr call))
      (def defn defn- defmacro) (lint-def filename (:expr call))
      [])
    (clojure.test cljs.test)
    (case (:name called-fn)
      (deftest) (lint-def filename (:expr call))
      [])
    #_#_[clojure.test deftest] (lint-deftest config filename (:expr call))
    #_#_[cljs.test deftest] (lint-deftest config filename (:expr call))
    []))

(defn resolve-call [idacs call fn-ns fn-name]
  (let [call-lang (:lang call)
        base-lang (or (:base-lang call) call-lang) ;; .cljc, .cljs or .clj file
        caller-ns (:ns call)
        ;; this call was unqualified and inferred as a function in the same namespace until now
        unqualified? (:unqualified? call)
        same-ns? (= caller-ns fn-ns)]
    (case [base-lang call-lang]
      [:clj :clj] (or (get-in idacs [:clj :defs fn-ns fn-name])
                      (get-in idacs [:cljc :defs fn-ns :clj fn-name]))
      [:cljs :cljs] (or (get-in idacs [:cljs :defs fn-ns fn-name])
                        ;; when calling a function in the same ns, it must be in
                        ;; another file, hence qualified via a require
                        ;; an exception to this would be :refer :all, but this doesn't exist in CLJS
                        (when-not (and same-ns? unqualified?)
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

(defn call-findings
  "Analyzes indexed defs and calls and returns findings."
  [idacs config]
  (let [findings (for [lang [:clj :cljs :cljc]
                       ns-sym (keys (get-in idacs [lang :calls]))
                       call (get-in idacs [lang :calls ns-sym])
                       :let [fn-name (:name call)
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
                       :let [;; a macro in a CLJC file with the same namespace
                             ;; in that case, looking at the row and column is
                             ;; not reliable.  we may look at the lang of the
                             ;; call and the lang of the function def context in
                             ;; the case of in-ns, the bets are off. we may
                             ;; support in-ns in a next version.
                             valid-order? (if (and (= caller-ns
                                                      fn-ns)
                                                   (= (:base-lang call)
                                                      (:base-lang called-fn))
                                                   ;; some built-ins may not have a row and col number
                                                   (:row called-fn))
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
                              (call-specific-findings config filename call called-fn))]
                       e errors
                       :when e]
                   e)]
    findings))

;;;; Scratch

(comment
  (count (:children (parse-string "(def ^:dynamic ^:private mismatch?)")))
  )
