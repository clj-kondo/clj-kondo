(ns clj-kondo.impl.linters
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :as utils :refer [node->line constant? sexpr]]
   [clj-kondo.impl.var-info :as var-info]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.analysis :as analysis]
   [clj-kondo.impl.findings :as findings]
   [clojure.set :as set]
   [clj-kondo.impl.namespace :as namespace]
   [clojure.string :as str]
   [clj-kondo.impl.types :as types]))

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

(defn resolve-call* [idacs call fn-ns fn-name]
  ;; (prn "RES" fn-ns fn-name)
  (let [call-lang (:lang call)
        base-lang (:base-lang call)  ;; .cljc, .cljs or .clj file
        unresolved? (:unresolved? call)
        unknown-ns? (= fn-ns :clj-kondo/unknown-namespace)
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

(defn resolve-arg-type [idacs arg-type]
  (or (:tag arg-type)
      (if-let [call (:call arg-type)]
        (let [arity (:arity call)]
          (when-let [called-fn (resolve-call* idacs call (:resolved-ns call) (:name call))]
            (let [arities (:arities called-fn)
                  tag (or (when-let [v (get arities arity)]
                            (:ret v))
                          (when-let [v (get arities :varargs)]
                            (when (>= arity (:min-arity v))
                              (:ret v))))]
              tag)))
        :any)
      :any))

(defn lint-arg-types! [ctx idacs call called-fn]
  (when-let [arg-types (:arg-types call)]
    (let [arg-types @arg-types
          tags (map #(resolve-arg-type idacs %) arg-types)]
      (types/lint-arg-types ctx called-fn arg-types tags call))))

(defn show-arities [fixed-arities varargs-min-arity]
  (let [fas (vec (sort fixed-arities))
        max-fixed (peek fas)
        arities (if varargs-min-arity
                  (if (= max-fixed varargs-min-arity)
                    fas (conj fas varargs-min-arity))
                  fas)]
    (cond varargs-min-arity
          (str (str/join ", " arities) " or more")
          (= 1 (count fas)) (first fas)
          :else (str (str/join ", " (pop arities)) " or " (peek arities)))))

(defn arity-error [ns-name fn-name called-with fixed-arities varargs-min-arity]
  (format "%s is called with %s %s but expects %s"
          (if ns-name (str ns-name "/" fn-name) fn-name)
          (str called-with)
          (if (= 1 called-with) "arg" "args")
          (show-arities fixed-arities varargs-min-arity)))

(defn lint-var-usage
  "Lints calls for arity errors, private calls errors. Also dispatches to call-specific linters.
  TODO: split this out in a resolver and a linter, so other linters
  can leverage the resolved results."
  [ctx idacs]
  (let [{:keys [:config]} ctx
        output-analysis? (-> config :output :analysis)
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
                             resolved-ns (:resolved-ns call)
                             refer-alls (:refer-alls caller-ns)
                             called-fn (resolve-call idacs call call-lang
                                                     resolved-ns fn-name unresolved? refer-alls)
                             unresolved-symbol-disabled? (:unresolved-symbol-disabled? call)
                             ;; we can determine if the call was made to another
                             ;; file by looking at the base-lang (in case of
                             ;; CLJS macro imports or the top-level namespace
                             ;; name (in the case of CLJ in-ns)). Looking at the
                             ;; filename proper isn't reliable since that may be
                             ;; <stdin> in clj-kondo.
                             different-file? (or
                                              (not= (:base-lang call) base-lang)
                                              (not= (:top-ns call) (:top-ns called-fn)))
                             row-called-fn (:row called-fn)
                             row-call (:row call)
                             valid-call? (or (not unresolved?)
                                             (when called-fn
                                               (or different-file?
                                                   (not row-called-fn)
                                                   (or (> row-call row-called-fn)
                                                       (and (= row-call row-called-fn)
                                                            (> (:col call) (:col called-fn)))))))
                             _ (when (and (not valid-call?)
                                          (not unresolved-symbol-disabled?))
                                 (namespace/reg-unresolved-symbol! ctx caller-ns-sym fn-name
                                                                   (if call?
                                                                     (merge call (meta fn-name))
                                                                     call)))
                             row (:row call)
                             col (:col call)
                             filename (:filename call)
                             fn-ns (:ns called-fn)
                             resolved-ns (or fn-ns resolved-ns)
                             arity (:arity call)
                             _ (when output-analysis?
                                 (analysis/reg-usage! ctx
                                                      filename row col caller-ns-sym
                                                      resolved-ns fn-name arity
                                                      (when (= :cljc base-lang)
                                                        call-lang) called-fn))]
                       :when valid-call?
                       :let [fn-name (:name called-fn)
                             _ (when (and unresolved?
                                          (contains? refer-alls
                                                     fn-ns))
                                 (namespace/reg-referred-all-var! (assoc ctx
                                                                         :base-lang base-lang
                                                                         :lang call-lang)
                                                                  caller-ns-sym fn-ns fn-name))
                             arities (:arities called-fn)
                             fixed-arities (or (:fixed-arities called-fn) (into #{} (filter number?) (keys arities)))
                             ;; fixed-arities (:fixed-arities called-fn)
                             varargs-min-arity (or (:varargs-min-arity called-fn) (-> arities :varargs :min-arity))
                             ;; varargs-min-arity (:varargs-min-arity called-fn)
                             ;; _ (prn ">>" (:name called-fn) arities (keys called-fn))
                             arity-error?
                             (and
                              (= :call (:type call))
                              (not (utils/linter-disabled? call :invalid-arity))
                              (or (not-empty fixed-arities)
                                  varargs-min-arity)
                              (not (or (contains? fixed-arities arity)
                                       (and varargs-min-arity (>= arity varargs-min-arity))
                                       (config/skip? config :invalid-arity (rest (:callstack call))))))
                             errors
                             [(when arity-error?
                                {:filename filename
                                 :row row
                                 :col col
                                 :level :error
                                 :type :invalid-arity
                                 :message (arity-error fn-ns fn-name arity fixed-arities varargs-min-arity)})
                              (when (and (:private called-fn)
                                         (not= caller-ns-sym
                                               fn-ns)
                                         (not (:private-access? call))
                                         (not (utils/linter-disabled? call :private-call)))
                                {:filename filename
                                 :row row
                                 :col col
                                 :level :error
                                 :type :private-call
                                 :message (format "#'%s is private"
                                                  (str (:ns called-fn) "/" (:name called-fn)))})
                              (when-let [deprecated (:deprecated called-fn)]
                                (when-not
                                    (or
                                     ;; recursive call
                                     (and
                                      (= fn-ns caller-ns-sym)
                                      (= fn-name (:in-def call)))
                                     (config/deprecated-var-excluded
                                      config
                                      (symbol (str fn-ns)
                                              (str fn-name))
                                      caller-ns-sym (:in-def call)))
                                  {:filename filename
                                   :row row
                                   :col col
                                   :level :error
                                   :type :deprecated-var
                                   :message (str
                                             (format "#'%s is deprecated"
                                                     (str fn-ns "/" fn-name))
                                             (if (true? deprecated)
                                               nil
                                               (str " since " deprecated)))}))]
                             ctx (assoc ctx
                                        :filename filename)
                             _ (when call?
                                 (lint-specific-calls!
                                  (assoc ctx
                                         :filename filename)
                                  call called-fn)
                                 (when-not arity-error?
                                   (lint-arg-types! ctx idacs call called-fn)))]
                       e errors
                       :when e]
                   e)]
    findings))

(defn lint-unused-namespaces!
  [{:keys [:config :findings] :as ctx}]
  (doseq [ns (namespace/list-namespaces ctx)
          :let [required (:required ns)
                used-namespaces (:used-namespaces ns)
                unused (set/difference
                        (set required)
                        (set used-namespaces))
                referred-vars (:referred-vars ns)
                used-referred-vars (:used-referred-vars ns)
                refer-alls (:refer-alls ns)
                filename (:filename ns)
                config (config/merge-config! config (:config ns))]]
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
      (let [var-ns (:ns v)]
        (when-not
            (or (contains? used-referred-vars k)
                (config/unused-referred-var-excluded config var-ns k))
          (findings/reg-finding!
           findings
           {:level :warning
            :type :unused-referred-var
            :filename filename
            :message (str "#'" var-ns "/" (:name v) " is referred but never used")
            :row row
            :col col}))))
    (doseq [[_referred-all-ns {:keys [:referred :node]}] refer-alls]
      (let [{:keys [:k :value]} node
            use? (or (= :use k)
                     (= 'use value))
            finding-type (if use? :use :refer-all)
            msg (str (format "use %salias or :refer"
                             (if use?
                               (str (when k ":") "require with ")
                               ""))
                     (when (seq referred)
                       (format " [%s]"
                               (str/join " " (sort referred)))))]
        (findings/reg-finding!
         findings
         (node->line filename node
                     :warning finding-type msg))))))

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

(defn lint-unused-private-vars!
  [{:keys [:findings :config] :as ctx}]
  (doseq [{:keys [:filename :vars :used-vars]
           ns-name :name} (namespace/list-namespaces ctx)
          :let [ vars (vals vars)
                used-vars (into #{} (comp (filter #(= (:ns %) ns-name))
                                          (map :name))
                                used-vars)]
          v vars
          :let [var-name (:name v)]
          :when (:private v)
          :when (not (contains? used-vars var-name))
          :when (not (config/unused-private-var-excluded config ns-name var-name))
          :let [{:keys [:row :col]} v]]
    (findings/reg-finding!
     findings
     {:level :warning
      :type :unused-private-var
      :filename filename
      :row row ;; row and col are not correct yet
      :col col
      :message (str "Unused private var " ns-name "/" var-name)})))

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
