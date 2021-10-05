(ns clj-kondo.impl.linters
  {:no-doc true}
  (:require
   [clj-kondo.impl.analysis :as analysis]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.types :as types]
   [clj-kondo.impl.types.utils :as tu]
   [clj-kondo.impl.utils :as utils :refer [node->line constant? sexpr tag export-ns-sym]]
   [clj-kondo.impl.var-info :as var-info]
   [clojure.set :as set]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn lint-cond-constants! [ctx conditions]
  (loop [[condition & rest-conditions] conditions]
    (when condition
      (let [v (sexpr condition)]
        (when-not (or (nil? v) (false? v))
          (when (and (constant? condition)
                     (not (or (nil? v) (false? v))))
            (when (not= :else v)
              (findings/reg-finding!
               ctx
               (node->line (:filename ctx) condition :cond-else
                           "use :else as the catch-all test expression in cond")))
            (when (seq rest-conditions)
              (findings/reg-finding!
               ctx
               (node->line (:filename ctx) (first rest-conditions)
                           :unreachable-code "unreachable code"))))))
      (recur rest-conditions))))

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
  [ctx expr]
  (when-not (even? (count (rest (:children expr))))
    (findings/reg-finding!
     ctx
     (node->line (:filename ctx) expr :syntax
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

(defn expected-test-assertion? [callstack]
  (when callstack
    (let [parent (first callstack)]
      (case parent
        ([clojure.core let] [cljs.core let]) (recur (next callstack))
        ([clojure.test testing] [cljs.test testing]) (recur (next callstack))
        ([clojure.test deftest] [cljs.test deftest]) true
        false))))

(defn lint-missing-test-assertion [ctx call]
  (when (expected-test-assertion? (next (:callstack call)))
    (findings/reg-finding! ctx
                           (node->line (:filename ctx) (:expr call)
                                       :missing-test-assertion "missing test assertion"))))

(defn lint-missing-else-branch
  "Lint missing :else branch on if-like expressions"
  [ctx expr]
  (let [config (:config ctx)
        level (-> config :linters :missing-else-branch :level)]
    (when-not (identical? :off level)
      (let [children (:children expr)
            args (rest children)]
        (when (= (count args) 2)
          (findings/reg-finding! ctx
                                 (node->line (:filename ctx) expr :missing-else-branch
                                             (format "Missing else branch."))))))))

(defn lint-single-key-in [ctx called-name call]
  (when-not (utils/linter-disabled? ctx :single-key-in)
    (let [[_ _ keyvec] (:children call)]
      (when (and keyvec (= :vector (tag keyvec)) (= 1 (count (:children keyvec))))
        (findings/reg-finding!
         ctx
         (node->line (:filename ctx) keyvec :single-key-in
                     (format "%s with single key" called-name)))))))

(defn lint-specific-calls! [ctx call called-fn]
  (let [called-ns (:ns called-fn)
        called-name (:name called-fn)]
    (case [called-ns called-name]
      ([clojure.core cond] [cljs.core cond])
      (lint-cond ctx (:expr call))
      ([clojure.core if-let] [clojure.core if-not] [clojure.core if-some]
       [cljs.core if-let] [cljs.core if-not] [cljs.core if-some])
      (lint-missing-else-branch ctx (:expr call))
      ([clojure.core get-in] [clojure.core assoc-in] [clojure.core update-in]
       [cljs.core get-in] [cljs.core assoc-in] [cljs.core update-in])
      (lint-single-key-in ctx called-name (:expr call))
      #_([clojure.test is] [cljs.test is])
      #_(lint-test-is ctx (:expr call))
      nil)

    ;; special forms which are not fns
    (when (= 'if (:name call))
      (lint-missing-else-branch ctx (:expr call)))

    (when (get-in var-info/predicates [called-ns called-name])
      (lint-missing-test-assertion ctx call))))

(defn lint-arg-types! [ctx idacs call called-fn]
  (when-let [arg-types (:arg-types call)]
    (let [arg-types @arg-types
          tags (map #(tu/resolve-arg-type idacs %) arg-types)]
      ;; (prn "tags" tags)
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

(defn arity-error [ns-nm fn-name called-with fixed-arities varargs-min-arity]
  (format "%s is called with %s %s but expects %s"
          (if ns-nm (str ns-nm "/" fn-name) fn-name)
          (str called-with)
          (if (= 1 called-with) "arg" "args")
          (show-arities fixed-arities varargs-min-arity)))

(defn lint-single-operand-comparison
  "Lints calls of single operand comparisons with always the same value."
  [call]
  (let [ns-nm (:resolved-ns call)
        core-ns (utils/one-of ns-nm [clojure.core cljs.core])]
    (when core-ns
      (let [fn-name (:name call)
            const-true (utils/one-of fn-name [= > < >= <= ==])
            const-false (= 'not= fn-name)]
        (when (and (or const-true const-false)
                   (= 1 (:arity call)))
          (node->line
           (:filename call)
           (:expr call)
           :single-operand-comparison
           (format "Single operand use of %s is always %s"
                   (str ns-nm "/" fn-name)
                   (some? const-true))))))))

(defn lint-single-logical-operand
  "Lints calls of single operand logical operators with always the same value."
  [call]
  (when (utils/one-of (:resolved-ns call) [clojure.core cljs.core])
    (let [call-name (:name call)
          const-true (utils/one-of call-name [and or])]
      (when (and const-true (= 1 (:arity call)))
        (node->line
         (:filename call)
         (:expr call)
         :single-logical-operand
         (format "Single arg use of %s always returns the arg itself" call-name))))))

#_(require 'clojure.pprint)

(defn lint-var-usage
  "Lints calls for arity errors, private calls errors. Also dispatches
  to call-specific linters."
  [ctx idacs]
  (let [config (:config ctx)
        output-analysis? (-> config :output :analysis)
        linted-namespaces (:linted-namespaces idacs)]
    ;; (prn :from-cache from-cache)
    (doseq [ns (namespace/list-namespaces ctx)
            :let [base-lang (:base-lang ns)]
            call (:used-vars ns)
            :let [;; _ (clojure.pprint/pprint (dissoc call :config))
                  call? (= :call (:type call))
                  unresolved? (:unresolved? call)
                  unresolved-ns (:unresolved-ns call)]
            :when (not unresolved-ns)
            :let [fn-name (:name call)
                  caller-ns-sym (:ns call)
                  call-lang (:lang call)
                  ctx (assoc ctx :lang call-lang)
                  caller-ns (get-in @(:namespaces ctx)
                                    [base-lang call-lang caller-ns-sym])
                  resolved-ns (:resolved-ns call)
                  refer-alls (:refer-alls caller-ns)
                  filename (:filename call)
                  row (:row call)
                  col (:col call)
                  end-row (:end-row call)
                  end-col (:end-col call)
                  ;; _ (prn :used (:used-namespaces idacs))
                  #_#__ (prn (keys (:defs (:clj idacs))))
                  called-fn (utils/resolve-call idacs call call-lang
                                                resolved-ns fn-name unresolved? refer-alls)
                  #_#__(when (not call?)
                      (clojure.pprint/pprint (dissoc call :config)))
                  name-meta (meta fn-name)
                  name-row (:row name-meta)
                  name-col (:col name-meta)
                  name-end-row (:end-row name-meta)
                  name-end-col (:end-col name-meta)
                  unresolved-symbol
                  (when (and (not called-fn)
                             (not (:interop? call))
                             row col end-row end-col
                             (contains? linted-namespaces resolved-ns)
                             (not (:resolved-core? call))
                             ;; the var could be :refer-all'ed, in this case unresolved? is true
                             (not unresolved?))
                    (namespace/reg-unresolved-var!
                     ctx caller-ns-sym resolved-ns fn-name
                     (if call?
                       (assoc call
                              :row name-row
                              :col name-col
                                :end-row name-end-row
                                :end-col name-end-col)
                       call))
                    true)
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
                  _ (when (and (not unresolved-symbol)
                               (not valid-call?))
                      (namespace/reg-unresolved-symbol!
                       ctx caller-ns-sym fn-name
                       (if call?
                         (assoc call
                                :row name-row
                                :col name-col
                                :end-row name-end-row
                                :end-col name-end-col)
                         call)))
                  ;; row (:row call)
                  ;; col (:col call)
                  ;; end-row (:end-row call)
                  ;; end-col (:end-col call)
                  ;; filename (:filename call)
                  fn-ns (:ns called-fn)
                  resolved-ns (or fn-ns resolved-ns)
                  arity (:arity call)
                  in-def (:in-def call)
                  in-reg (:in-reg call)
                  recursive? (and
                              (= fn-ns caller-ns-sym)
                              (= fn-name in-def))
                  _ (when output-analysis?
                      (analysis/reg-usage! ctx
                                           filename
                                           row
                                           col
                                           caller-ns-sym
                                           resolved-ns fn-name arity
                                           (when (= :cljc base-lang)
                                             call-lang)
                                           (or in-def (when-let [k (:k in-reg)] (name k)))
                                           (assoc called-fn
                                                  :reg (:reg in-reg)
                                                  :alias (:alias call)
                                                  :refer (:refer call)
                                                  :name-row name-row
                                                  :name-col name-col
                                                  :name-end-row name-end-row
                                                  :name-end-col name-end-col
                                                  :end-row end-row
                                                  :end-col end-col)))]
            :when valid-call?
            :let [fn-name (:name called-fn)
                  _ (when (and  ;; unresolved?
                           (:simple? call)
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
                  skip-arity-check?
                  (and call?
                       (or (utils/linter-disabled? call :invalid-arity)
                           (config/skip? config :invalid-arity (rest (:callstack call)))))
                  arity-error?
                  (and
                   call?
                   (not skip-arity-check?)
                   (or (not-empty fixed-arities)
                       varargs-min-arity)
                   (not (or (contains? fixed-arities arity)
                            (and varargs-min-arity (>= arity varargs-min-arity)))))
                  single-operand-comparison-error
                  (and call?
                       (not (utils/linter-disabled? call :single-operand-comparison))
                       (lint-single-operand-comparison call))
                  single-logical-operand-error
                  (and call?
                       (not (utils/linter-disabled? call :single-logical-operand))
                       (lint-single-logical-operand call))]]
      (when arity-error?
        (findings/reg-finding!
         ctx
         {:filename filename
          :row row
          :end-row end-row
          :col col
          :end-col end-col
          :type :invalid-arity
          :message (arity-error fn-ns fn-name arity fixed-arities varargs-min-arity)}))
      (when single-operand-comparison-error
        (findings/reg-finding! ctx
                               single-operand-comparison-error))
      (when single-logical-operand-error
        (findings/reg-finding! ctx
                               single-logical-operand-error))
      (when (and (:private called-fn)
                 (not= caller-ns-sym
                       fn-ns)
                 (not (:private-access? call))
                 (not (utils/linter-disabled? call :private-call)))
        (findings/reg-finding! ctx
                               {:filename filename
                                :row row
                                :col col
                                :type :private-call
                                :message (format "#'%s is private"
                                                 (str (:ns called-fn) "/" (:name called-fn)))}))
      (when-let [deprecated (:deprecated called-fn)]
        (when-not
            (or
             ;; recursive call
             recursive?
             (config/deprecated-var-excluded
              config
              (symbol (str fn-ns)
                      (str fn-name))
              caller-ns-sym in-def))
          (findings/reg-finding! ctx
                                 {:filename filename
                                  :row row
                                  :end-row end-row
                                  :col col
                                  :end-col end-col
                                  :type :deprecated-var
                                  :message (str
                                            (format "#'%s is deprecated"
                                                    (str fn-ns "/" fn-name))
                                            (if (true? deprecated)
                                              nil
                                              (str " since " deprecated)))})))

      (let [ctx (assoc ctx :filename filename)]
        (when call?
          (lint-specific-calls!
           (assoc ctx
                  :filename filename)
           call called-fn)
          (when-not (or arity-error? skip-arity-check?)
            ;; (prn :>> fn-name @(:arg-types call))
            (lint-arg-types! ctx idacs call called-fn)))))))

(defn lint-unused-namespaces!
  [ctx]
  (let [config (:config ctx)]
    (doseq [ns (namespace/list-namespaces ctx)
            :let [required (:required ns)
                  used-namespaces (:used-namespaces ns)
                  unused (set/difference
                          (set required)
                          (set used-namespaces))
                  referred-vars (:referred-vars ns)
                  used-referred-vars (:used-referred-vars ns)
                  refer-alls (:refer-alls ns)
                  refer-all-nss (set (keys refer-alls))
                  ns-config (:config ns)
                  config (or ns-config config)
                  ctx (if ns-config (assoc ctx :config config) ctx)
                  ctx (assoc ctx :lang (:lang ns))]]
      (doseq [ns-sym unused]
        (when-not (config/unused-namespace-excluded config ns-sym)
          (let [m (meta ns-sym)
                filename (:filename m)]
            (findings/reg-finding!
             ctx
             (-> (node->line filename ns-sym :unused-namespace
                             (format "namespace %s is required but never used" ns-sym))
                 (assoc :ns (export-ns-sym ns-sym)))))))
      (doseq [[k v] referred-vars]
        (let [var-ns (:ns v)]
          (when-not
              (or (contains? used-referred-vars k)
                  (config/unused-referred-var-excluded config var-ns k)
                  (contains? refer-all-nss var-ns))
            (let [filename (:filename v)
                  referred-ns (export-ns-sym var-ns)]
              (findings/reg-finding!
               ctx
               (-> (node->line filename k :unused-referred-var (str "#'" var-ns "/" (:name v) " is referred but never used"))
                   (assoc :ns referred-ns
                          :referred-ns referred-ns
                          :refer (:name v))))))))
      (doseq [[referred-all-ns {:keys [:referred :node] :as refer-all}] refer-alls
              :when (not (config/refer-all-excluded? config referred-all-ns))]
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
                                 (str/join " " (sort referred)))))
              filename (:filename refer-all)]
          (findings/reg-finding!
           ctx
           (node->line filename node
                       finding-type msg)))))))

(defn lint-unused-bindings!
  [ctx]
  (doseq [ns (namespace/list-namespaces ctx)]
    (let [ns-config (:config ns)
          ctx (if ns-config
                (assoc ctx :config ns-config)
                ctx)
          ctx (assoc ctx :lang (:lang ns))]
      (when-not (identical? :off (-> ctx :config :linters :unused-binding :level))
        (let [bindings (:bindings ns)
              used-bindings (:used-bindings ns)
              diff (set/difference bindings used-bindings)
              defaults (:destructuring-defaults ns)]
          (doseq [binding diff]
            (let [nm (:name binding)]
              (when-not (str/starts-with? (str nm) "_")
                (findings/reg-finding!
                 ctx
                 {:type :unused-binding
                  :filename (:filename binding)
                  :message (str "unused binding " nm)
                  :row (:row binding)
                  :col (:col binding)
                  :end-row (:end-row binding)
                  :end-col (:end-col binding)}))))
          (doseq [default defaults
                  :let [binding (:binding default)]
                  :when (not (contains? (:used-bindings ns) binding))]
            (findings/reg-finding!
             ctx
             {:type :unused-binding
              :filename (:filename binding)
              :message (str "unused default for binding " (:name binding))
              :row (:row default)
              :col (:col default)
              :end-row (:end-row default)
              :end-col (:end-col default)})))))))

(defn lint-unused-private-vars!
  [ctx]
  (let [config (:config ctx)]
    (doseq [{:keys [:filename :vars :used-vars :lang]
             ns-nm :name
             ns-config :config} (namespace/list-namespaces ctx)
            :let [config (or ns-config config)
                  ctx (if ns-config (assoc ctx :config config) ctx)
                  ctx (assoc ctx :lang lang)
                  vars (vals vars)
                  used-vars (into #{} (comp (filter #(and (= (:ns %) ns-nm)
                                                          (not= (:name %) (:in-def %))))
                                            (map :name))
                                  used-vars)]
            v vars
            :let [var-name (:name v)]
            :when (:private v)
            :when (not (contains? used-vars var-name))
            :when (not (config/unused-private-var-excluded config ns-nm var-name))
            :when (not (utils/one-of (:defined-by v)
                                     [clojure.core/defrecord
                                      cljs.core/defrecord
                                      clojure.core/deftype
                                      cljs.core/deftype
                                      clojure.core/definterface
                                      cljs.core/definterface]))]
      (findings/reg-finding!
       ctx
       {:type :unused-private-var
        :filename filename
        :row (:name-row v)
        :col (:name-col v)
        :end-row (:name-end-row v)
        :end-col (:name-end-col v)
        :message (str "Unused private var " ns-nm "/" var-name)}))))

(defn lint-unresolved-symbols!
  [ctx]
  (let [hide-duplicates? (not (get-in ctx [:config :linters :unresolved-symbol :report-duplicates]))]
    (doseq [ns (namespace/list-namespaces ctx)
            :let [lang (:lang ns)
                  ctx (assoc ctx :lang lang)]
            [_ vs] (:unresolved-symbols ns)
            v (cond->> vs
                hide-duplicates? (take 1))]
      (let [filename (:filename v)
            n (:name v)]
        (findings/reg-finding!
          ctx
          {:type :unresolved-symbol
           :filename filename
           :message (str "Unresolved symbol: " n)
           :row (:row v)
           :col (:col v)
           :end-row (:end-row v)
           :end-col (:end-col v)})))))

(defn lint-unresolved-vars!
  [ctx]
  (let [hide-duplicates? (not (get-in ctx [:config :linters :unresolved-var :report-duplicates]))]
    (doseq [ns (namespace/list-namespaces ctx)
            :let [lang (:lang ns)
                  ctx (assoc ctx :lang lang)]
            [_ vs] (:unresolved-vars ns)
            v (cond->> vs
                hide-duplicates? (take 1))]
      (let [filename (:filename v)
            expr (:expr v)
            n (if-let [children (:children expr)]
                (str (first children))
                (str expr))]
        (findings/reg-finding!
          ctx
          {:type :unresolved-var
           :filename filename
           :message (str "Unresolved var: " n)
           :row (:row v)
           :col (:col v)
           :end-row (:end-row v)
           :end-col (:end-col v)})))))

(defn lint-unused-imports!
  [ctx]
  (doseq [ns (namespace/list-namespaces ctx)
          :let [ns-config (:config ns)
                ctx (if ns-config (assoc ctx :config ns-config)
                        ctx)
                ctx (assoc ctx :lang (:lang ns))]
          :when (not (identical? :off (-> ns-config :linters :unused-import :level)))
          :let [filename (:filename ns)
                imports (:imports ns)
                used-imports (:used-imports ns)]
          [import package] imports
          :when (not (contains? used-imports import))]
    (findings/reg-finding!
     ctx
     (-> (node->line filename import :unused-import (str "Unused import " import))
         (assoc :class (symbol (str package "." import)))))))

(defn lint-unresolved-namespaces!
  [ctx]
  (let [hide-duplicates? (not (get-in ctx [:config :linters :unresolved-namespace :report-duplicates]))]
    (doseq [ns (namespace/list-namespaces ctx)
            :let [ctx (assoc ctx :lang (:lang ns))]
            [_ uns] (:unresolved-namespaces ns)
            un (cond->> uns
                 hide-duplicates? (take 1))
            :let [m (meta un)
                  filename (:filename m)]]
      (findings/reg-finding!
        ctx
        {:type :unresolved-namespace
         :filename filename
         :message (str "Unresolved namespace " un ". Are you missing a require?")
         :row (:row m)
         :col (:col m)
         :end-row (:end-row m)
         :end-col (:end-col m)}))))

;;;; scratch

(comment
  )
