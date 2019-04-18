(ns clj-kondo.impl.vars
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [some-call node->line
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
  (let [children (:children (strip-meta expr))
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
        defn
        (let [{:keys [:row :col]} (meta expr)]
          (if fn-name
            (cond-> {:type :defn
                     :name fn-name
                     :row row
                     :col col
                     :lang lang}
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

(defn reader-conditional-expr? [expr]
  (and (= :reader-macro (node/tag expr))
       (= '? (:value (first (:children expr))))))

(defn parse-reader-conditional [expr]
  (let [kvs (-> expr
                :children second :children)
        kvs (partition 2 kvs)]
    (into {}
          (for [[k v] kvs]
            [(:k k) v]))))

(defn parse-arities
  ;; TODO: refactor and split into multiple functions
  ([lang expr] (parse-arities lang #{} expr))
  ([lang bindings {:keys [:children] :as expr}]
   (cond
     (some-call expr ns)
     [(analyze-ns-decl lang expr)]
     ;; TODO: in-ns is not supported yet
     ;; One thing to note: if in-ns is used in a function body, the rest of the namespace is now analyzed in that namespace, which is incorrect.
     ;; (some-call expr in-ns)
     ;; [(analyze-in-ns expr)]
     ;; TODO: better resolving for these macro calls
     ;; core/->> was hard-coded here because of
     ;; https://github.com/clojure/clojurescript/blob/73272a2da45a4c69d090800fa7732abe3fd05c70/src/main/clojure/cljs/core.cljc#L551
     (some-call expr defn defn- core/defn core/defn- defmacro core/defmacro)
     (parse-defn lang bindings expr)
     ;; TODO: better resolving for these macro calls
     ;; core/->> was hard-coded here because of
     ;; https://github.com/clojure/clojurescript/blob/73272a2da45a4c69d090800fa7732abe3fd05c70/src/main/clojure/cljs/core.cljc#L854
     (or (some-call expr core/->> ->> cond-> cond->> some-> some->> . .. deftype
                    proxy extend-protocol doto reify definterface defrecord defprotocol)
         (contains? '#{:quote :syntax-quote} (node/tag expr)))
     []
     (some-call expr let)
     (let [let-bindings (->> children second :children (map :value) (filter symbol?) set)]
       (mapcat #(parse-arities lang (set/union bindings let-bindings) %) (rest children)))
     (some-call expr fn fn*)
     ;; TODO better arity analysis like in normal fn
     (let [fn-name (-> children second :value)
           arg-vec (first (filter #(= :vector (node/tag %)) (rest children)))
           maybe-bindings (->> arg-vec :children (map :value))
           fn-bindings (set (filter symbol? (cons fn-name maybe-bindings)))]
       (mapcat #(parse-arities lang (set/union bindings fn-bindings) %) (rest children)))
     (fn-call? expr)
     (let [fn-name (:value (first children))
           args (count (rest children))
           binding-call? (contains? bindings fn-name)
           parse-rest (mapcat #(parse-arities lang bindings %) (rest children))]
       (if binding-call?
         parse-rest
         (cons
          (let [{:keys [:row :col]} (meta expr)]
            {:type :call
             :name fn-name
             :arity args
             :row row
             :col col
             :lang lang
             :expr expr})
          parse-rest)))
     (reader-conditional-expr? expr)
     (let [{:keys [:clj :cljs]} (parse-reader-conditional expr)]
       (for [[l e] [[:clj clj] [:cljs cljs]]
             :when e
             expr (parse-arities l bindings e)]
         expr))
     :else (mapcat #(parse-arities lang bindings %) children))))

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
           :clojure-excluded? (contains? (:clojure-excluded ns)
                                         name-sym)}))))

(def vconj (fnil conj []))

(defn analyze-arities
  "Collects defs and calls into a map. To optimize cache lookups later
  on, calls are indexed by the namespace they call to, not the
  ns where the call occurred."
  ([filename lang expr] (analyze-arities filename lang expr false))
  ([filename lang expr debug?]
   (loop [[first-parsed & rest-parsed] (parse-arities lang expr)
          ns (analyze-ns-decl lang (parse-string "(ns user)"))
          results {:calls {}
                   :defs {}
                   :findings []
                   :lang lang}]
     (if first-parsed
       (case (:type first-parsed)
         (:ns :in-ns)
         (recur rest-parsed
                first-parsed
                results)
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
                              call (cond-> (assoc first-parsed
                                                  :filename filename
                                                  :resolved-ns (:ns resolved)
                                                  :ns-lookup ns)
                                     (:clojure-excluded? resolved)
                                     (assoc :clojure-excluded? true))
                              results (update-in results path vconj call)]
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
       [results]))))

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

(defn resolve-call [idacs call-lang fn-ns fn-name]
  ;; TODO: for cljs -> clj/cljc calls we can probably store whether a function is a macro or not and use that
  ;; call lang clj. [foo.core] can come from another .clj or .cljc file
  ;; call lang cljs. [foo.core] can come from another .cljs, .clj (macros) or .cljc file
  ;; call lang cljc. [foo.core]. we should split this call into a clj and cljs one (see #67). for now, we'll only look into .clj.
  (case call-lang
    :clj (or (get-in idacs [:clj :defs fn-ns fn-name])
             (get-in idacs [:cljc :defs fn-ns :cljc fn-name])
             (get-in idacs [:cljc :defs fn-ns :clj fn-name]))
    :cljs (or (get-in idacs [:cljs :defs fn-ns fn-name])
              (get-in idacs [:cljc :defs fn-ns :cljc fn-name])
              (get-in idacs [:cljc :defs fn-ns :cljs fn-name]))
    :cljc (or
           ;; there might be both a .clj and .cljs version of the file with the same name
           (get-in idacs [:clj :defs fn-ns fn-name])
           (get-in idacs [:cljs :defs fn-ns fn-name])
           ;; or there is one .cljc file with this name and the function is defined for both languages
           (get-in idacs [:cljc :defs fn-ns :cljc fn-name]))))

(defn fn-call-findings
  "Analyzes indexed defs and calls and returns findings."
  [idacs config]
  (let [findings (for [lang [:clj :cljs :cljc]
                       ns-sym (keys (get-in idacs [lang :calls]))
                       call (get-in idacs [lang :calls ns-sym])
                       :let [;; _ (prn "call" (-> call :ns* :refer-alls))
                             fn-name (:name call)
                             caller-ns (:ns call)
                             fn-ns (:resolved-ns call)
                             called-fn
                             (or (resolve-call idacs (:lang call) fn-ns fn-name)
                                 ;; we resolved this call against the
                                 ;; same namespace, because it was
                                 ;; unqualified
                                 (when (= caller-ns fn-ns)
                                   (some #(resolve-call idacs (:lang call) % fn-name)
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
                       :let [;; _ (prn called-fn)
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
