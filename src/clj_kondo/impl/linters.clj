(ns clj-kondo.impl.linters
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [node->line constant? sexpr]]
   [clj-kondo.impl.var-info :as var-info]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.findings :as findings]
   [clojure.set :as set]
   [clj-kondo.impl.namespace :as namespace]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn lint-cond-constants! [{:keys [:findings :filename]} conditions]
  (loop [[condition & rest-conditions] conditions]
    (when condition
      (let [v (sexpr condition)]
        (when-not (or (nil? v) (false? v))
          (when (and (constant? condition)
                     (not (or (nil? v) (false? v))))
            (when (not= :else v)
              (findings/reg-finding!
               findings
               (node->line filename condition :warning :cond-else
                           "use :else as the catch-all test expression in cond")))
            (when (and (seq rest-conditions))
              (findings/reg-finding!
               findings
               (node->line filename (first rest-conditions) :warning
                           :unreachable-code "unreachable code"))))))
      (recur rest-conditions))))

(defn =? [sexpr]
  (and (list? sexpr)
       (= '= (first sexpr))))

#_(defn lint-cond-as-case! [filename expr conditions]
    (let [[fst-sexpr & rest-sexprs] (map node/sexpr conditions)
          init (when (=? fst-sexpr)
                 (set (rest fst-sexpr)))]
      (when init
        (when-let
            [case-expr
             (let [c (first
                      (reduce
                       (fn [acc sexpr]
                         (if (=? sexpr)
                           (let [new-acc
                                 (set/intersection acc
                                                   (set (rest sexpr)))]
                             (if (= 1 (count new-acc))
                               new-acc
                               (reduced nil)))
                           (if (= :else sexpr)
                             acc
                             (reduced nil))))
                       init
                       rest-sexprs))]
               c)]
          (findings/reg-finding!
           (node->line filename expr :warning :cond-as-case
                       (format "cond can be written as (case %s ...)"
                               (str (node/sexpr case-expr)))))))))

(defn lint-cond-even-number-of-forms!
  [{:keys [:findings :filename]} expr]
  (when-not (even? (count (rest (:children expr))))
    (findings/reg-finding!
     findings
     (node->line filename expr :error :syntax
                 (format "cond requires even number of forms")))
    true))

(defn lint-cond [ctx expr]
  (let [conditions
        (->> expr :children
             next
             (take-nth 2))]
    (when-not (lint-cond-even-number-of-forms! ctx expr)
      (when (seq conditions)
        (lint-cond-constants! ctx conditions)
        #_(lint-cond-as-case! filename expr conditions)))))

;; TODO: refactor like redundant do/let, inline-def, etc. I.e. move to analyzer ns.
(defn lint-missing-test-assertion [{:keys [:findings :filename]} call called-fn]
  (when (get-in var-info/predicates [(:ns called-fn) (:name called-fn)])
    (findings/reg-finding! findings
                           (node->line filename (:expr call) :warning
                                       :missing-test-assertion "missing test assertion"))))

(defn lint-specific-calls! [ctx call called-fn]
  (case [(:ns called-fn) (:name called-fn)]
    ([clojure.core cond] [cljs.core cond])
    (lint-cond ctx (:expr call))
    nil)
  ;; missing test assertion
  (case (second (:callstack call))
    ([clojure.test deftest] [cljs.test deftest])
    (lint-missing-test-assertion ctx call called-fn)
    nil))

(defn resolve-call [idacs call fn-ns fn-name]
  (let [call-lang (:lang call)
        base-lang (:base-lang call)  ;; .cljc, .cljs or .clj file
        unresolved? (:unresolved? call)
        unknown-ns? (= fn-ns :clj-kondo/unknown-namespace)
        fn-ns (if unknown-ns? (:ns call) fn-ns)]
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

(defn show-arities [fixed-arities var-args-min-arity]
  (let [fas (vec (sort fixed-arities))
        max-fixed (peek fas)
        arities (if var-args-min-arity
                  (if (= max-fixed var-args-min-arity)
                    fas (conj fas var-args-min-arity))
                  fas)]
    (cond var-args-min-arity
          (str (str/join ", " arities) " or more")
          (= 1 (count fas)) (first fas)
          :else (str (str/join ", " (pop arities)) " or " (peek arities)))))

(defn arity-error [ns-name fn-name called-with fixed-arities var-args-min-arity]
  (format "%s is called with %s %s but expects %s"
          (if ns-name (str ns-name "/" fn-name) fn-name)
          (str called-with)
          (if (= 1 called-with) "arg" "args")
          (show-arities fixed-arities var-args-min-arity)))

(defn lint-var-usage
  "Lints calls for arity errors, private calls errors. Also dispatches to call-specific linters."
  [ctx idacs]
  (let [config (:config ctx)
        ;; findings* (:findings ctx)
        findings (for [ns (namespace/list-namespaces ctx)
                       :let [base-lang (:base-lang ns)]
                       call (:used-vars ns)
                       :let [call? (= :call (:type call))
                             unresolved? (:unresolved? call)
                             fn-name (:name call)
                             caller-ns-sym (:ns call)
                             call-lang (:lang call)
                             caller-ns (get-in @(:namespaces ctx)
                                               [base-lang call-lang caller-ns-sym])
                             fn-ns (:resolved-ns call)
                             called-fn
                             (or (resolve-call idacs call fn-ns fn-name)
                                 ;; we resolved this call against the
                                 ;; same namespace, because it was
                                 ;; unqualified
                                 (when (= fn-ns :clj-kondo/unknown-namespace)
                                   (some #(resolve-call idacs call % fn-name)
                                         (into (vec
                                                (keep (fn [[ns excluded]]
                                                        (when-not (contains? excluded fn-name)
                                                          ns))
                                                      (:refer-alls caller-ns)))
                                               (when (not (:clojure-excluded? call))
                                                 [(case call-lang #_base-lang
                                                        :clj 'clojure.core
                                                        :cljs 'cljs.core
                                                        :cljc 'clojure.core)])))))
                             unresolved-symbol-disabled? (:unresolved-symbol-disabled? call)
                             different-file? (not= (:filename call) (:filename called-fn))
                             row-called-fn (:row called-fn)
                             row-call (:row call)
                             _ (when (= 'v fn-name)
                                 (prn caller-ns-sym fn-name row-called-fn row-call "different-file?" different-file?
                                      (:filename call) (:filename called-fn) called-fn))
                             valid? (or (not unresolved?)
                                        (when called-fn
                                          (or different-file?
                                              (not row-called-fn)
                                              (do
                                                (when(= 'v fn-name)
                                                  (prn caller-ns-sym "AAAAR" row-call))
                                                (or (> row-call row-called-fn)
                                                     (and (= row-call row-called-fn)
                                                          (> (:col call) (:col called-fn))))))))
                             _ (when (= 'v fn-name)
                                 (prn caller-ns-sym "not unresolved?" (not unresolved?)))
                             _ (when (and (not valid?)
                                          (not unresolved-symbol-disabled?))
                                 ;; (prn caller-ns-sym fn-name (:row call) (:col call) valid?)
                                 (namespace/reg-unresolved-symbol! ctx caller-ns-sym fn-name
                                                                   (if call?
                                                                     (merge call (meta fn-name))
                                                                     call)))]
                       :when called-fn
                       :let [fn-ns (:ns called-fn)
                             ;; if the var was not unresolved, we assume the
                             ;; order was correct. if it was unresolved (maybe due to :refer :all), then we look at the row and colum
                             #_(or (not unresolved?)
                                              unresolved-symbol-disabled?
                                              (if (and (= caller-ns-sym
                                                          fn-ns)
                                                       ;; we could be using a function defined with in-ns
                                                       ;; so we should be looking at the filename (or top-declaring ns)
                                                       ;; some built-ins may not have a row and col number
                                                       #_(:row called-fn))
                                                false #_(or (> (:row call) (:row called-fn))
                                                    (and (= (:row call) (:row called-fn))
                                                         (> (:col call) (:col called-fn))))
                                                true))
                             ]
                       :when valid?
                       :let [arity (:arity call)
                             filename (:filename call)
                             fixed-arities (:fixed-arities called-fn)
                             var-args-min-arity (:var-args-min-arity called-fn)
                             errors
                             [(when (and
                                     (= :call (:type call))
                                     (not (:invalid-arity-disabled? call))
                                     (or (not-empty fixed-arities)
                                         var-args-min-arity)
                                     (not (or (contains? fixed-arities arity)
                                              (and var-args-min-arity (>= arity var-args-min-arity))
                                              (config/skip? config :invalid-arity (rest (:callstack call))))))
                                {:filename filename
                                 :row (:row call)
                                 :col (:col call)
                                 :level :error
                                 :type :invalid-arity
                                 :message (arity-error (:ns called-fn) (:name called-fn) (:arity call) fixed-arities var-args-min-arity)})
                              (when (and (:private called-fn)
                                         (not= caller-ns-sym
                                               fn-ns)
                                         (not (:private-access? call)))
                                {:filename filename
                                 :row (:row call)
                                 :col (:col call)
                                 :level :error
                                 :type :private-call
                                 :message (format "#'%s is private"
                                                  (str (:ns called-fn) "/" (:name called-fn)))})
                              (when-let [deprecated (:deprecated called-fn)]
                                (when-not
                                    (or
                                     ;; recursive call
                                     (and
                                      (= (:ns called-fn) caller-ns-sym)
                                      (= (:name called-fn) (:in-def call)))
                                     (config/deprecated-var-excluded
                                      config
                                      (symbol (str (:ns called-fn))
                                              (str (:name called-fn)))
                                      caller-ns-sym (:in-def call)))
                                  {:filename filename
                                   :row (:row call)
                                   :col (:col call)
                                   :level :error
                                   :type :deprecated-var
                                   :message (str
                                             (format "#'%s is deprecated"
                                                     (str (:ns called-fn) "/" (:name called-fn)))
                                             (if (true? deprecated)
                                               nil
                                               (str " since " deprecated)))}))]
                             _ (lint-specific-calls! (assoc ctx
                                                            :filename filename)
                                                     call called-fn)]
                       e errors
                       :when e]
                   e)]
    findings))

(defn lint-unused-namespaces!
  [{:keys [:config :findings] :as ctx}]
  (doseq [ns (namespace/list-namespaces ctx)
          :let [required (:required ns)
                used (:used ns)
                unused (set/difference
                        (set required)
                        (set used))
                referred-vars (:referred-vars ns)
                used-referred-vars (:used-referred-vars ns)
                filename (:filename ns)]]
    (doseq [ns-sym unused]
      (when-not (config/unused-namespace-excluded config ns-sym)
        (let [{:keys [:row :col :filename]} (meta ns-sym)]
          (findings/reg-finding!
           findings
           {:level :warning
            :type :unused-namespace
            :filename filename
            :message (format "namespace %s is required but never used" ns-sym)
            :row row
            :col col}))))
    (doseq [[k v] referred-vars
            :let [{:keys [:row :col]} (meta k)]]
      (when-not
          (contains? used-referred-vars k)
        (findings/reg-finding!
         findings
         {:level :warning
          :type :unused-referred-var
          :filename filename
          :message (str "#'" (:ns v) "/" (:name v) " is referred but never used")
          :row row
          :col col})))))

(defn lint-unused-bindings!
  [{:keys [:findings] :as ctx}]
  (doseq [ns (namespace/list-namespaces ctx)
          :let [bindings (:bindings ns)
                used-bindings (:used-bindings ns)
                diff (set/difference bindings used-bindings)]
          binding diff]
    (let [{:keys [:row :col :filename :name]} binding]
      (when-not (str/starts-with? (str name) "_")
        (findings/reg-finding!
         findings
         {:level :warning
          :type :unused-binding
          :filename filename
          :message (str "unused binding " name)
          :row row
          :col col})))))

(defn lint-unresolved-symbols!
  [{:keys [:findings] :as ctx}]
  (doseq [ns (namespace/list-namespaces ctx)
          [_ {:keys [:row :col :filename :name]}] (:unresolved-symbols ns)]
    (findings/reg-finding!
     findings
     {:level :error
      :type :unresolved-symbol
      :filename filename
      :message (str "unresolved symbol " name)
      :row row
      :col col})))

;;;; scratch

(comment
  )
