(ns clj-kondo.impl.linters
  {:no-doc true}
  (:require
   [clj-kondo.impl.analysis :as analysis]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.types :as types]
   [clj-kondo.impl.types.utils :as tu]
   [clj-kondo.impl.utils :as utils :refer [constant? export-ns-sym node->line
                                           tag]]
   [clj-kondo.impl.var-info :as var-info]
   [clojure.set :as set]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(def ^:private shadowed-defmethod-msg
  "Shadowed defmethod: %s for dispatch value %s")

(defn- condition-value [condition]
  (or (:value condition)
      (:k condition)))

(defn- constant-condition-truthy? [condition]
  (and (constant? condition)
       (let [v (condition-value condition)]
         (not (or (nil? v) (false? v))))))

(defn lint-cond-constants! [ctx conditions]
  (loop [[condition & rest-conditions] conditions]
    (when condition
      (when (constant-condition-truthy? condition)
        (when (not= :else (condition-value condition))
          (findings/reg-finding!
           ctx
           (node->line (:filename ctx) condition :cond-else
                       "use :else as the catch-all test expression in cond")))
        (when (seq rest-conditions)
          (findings/reg-finding!
           ctx
           (node->line (:filename ctx) (first rest-conditions)
                       :unreachable-code "unreachable code"))))
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
                 "cond requires even number of forms"))
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

(defn expected-test-assertion? [callstack idx]
  (when callstack
    (let [parent (first callstack)]
      (case parent
        ([clojure.core let] [cljs.core let]) (recur (next callstack) nil)
        ([clojure.test testing] [cljs.test testing])
        (if (and idx (zero? idx))
          false
          (recur (next callstack) nil))
        ([clojure.test deftest] [cljs.test deftest])
        true
        false))))

(defn lint-missing-test-assertion [ctx call]
  (when (expected-test-assertion? (next (:callstack call)) (:idx call))
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
        (when (= 2 (count args))
          (findings/reg-finding! ctx
                                 (node->line (:filename ctx) expr :missing-else-branch
                                             "Missing else branch.")))))))

(defn lint-if-nil-return
  "Lint returning nil from if-like expressions. When-like expressions are
  preferred."
  [ctx expr]
  (let [config (:config ctx)
        level (-> config :linters :if-nil-return :level)]
    (when-not (identical? :off level)
      (let [children (:children expr)
            [_condition then-branch else-branch] (rest children)]
        (cond (= [:value nil] (find else-branch :value))
              (when-let [preferred (get '{if when
                                          if-let when-let
                                          if-some when-some
                                          if-not when-not}
                                        (:value (first children)))]
                (findings/reg-finding! ctx
                                       (node->line (:filename ctx) expr :if-nil-return
                                                   (format "For nil return, prefer %s." preferred))))
              (= [:value nil] (find then-branch :value))
              ;; We don't check if-let and if-some here as there'd be an unused binding warning anyway
              (when-let [preferred (get '{if when-not
                                          if-not when}
                                        (:value (first children)))]
                (findings/reg-finding! ctx
                                       (node->line (:filename ctx) expr :if-nil-return
                                                   (format "For nil return, prefer %s." preferred)))))))))

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
        called-name (:name called-fn)
        config (:config call)
        ctx (if config (assoc ctx :config config)
                ctx)]
    (case [called-ns called-name]
      ([clojure.core cond] [cljs.core cond])
      (lint-cond ctx (:expr call))
      ([clojure.core if-let] [clojure.core if-not] [clojure.core if-some]
       [cljs.core if-let] [cljs.core if-not] [cljs.core if-some])
      (do (lint-missing-else-branch ctx (:expr call))
          (lint-if-nil-return ctx (:expr call)))
      ([clojure.core get-in] [clojure.core assoc-in] [clojure.core update-in]
       [cljs.core get-in] [cljs.core assoc-in] [cljs.core update-in])
      (lint-single-key-in ctx called-name (:expr call))
      nil)

    ;; special forms which are not fns
    (when (= 'if (:name call))
      (lint-missing-else-branch ctx (:expr call))
      (lint-if-nil-return ctx (:expr call)))
    (when (contains? var-info/unused-values
                     (symbol (let [cns (str called-ns)]
                               (if (= "cljs.core" cns) "clojure.core" cns))
                             (str called-name)))
      (lint-missing-test-assertion ctx call))))

(defn- lint-is-message-not-string! [ctx call called-fn tags]
  (when (and
         (= 'is (:name called-fn))
         (utils/one-of (:ns called-fn) [clojure.test cljs.test])
         (= 2 (count tags))
         (not (utils/linter-disabled? ctx :is-message-not-string))
         (not (:clj-kondo.impl/generated (:expr call))))
    (let [second-arg (-> call :expr :children (nth 2 nil))
          literal-string? (or (:lines second-arg)
                              (= :multi-line (tag second-arg)))]
      (when (and (not literal-string?)
                 (not (some-> (second tags) (types/match? :string))))
        (findings/reg-finding! ctx
                               (merge (utils/location (meta second-arg))
                                      (select-keys call [:filename])
                                      {:type :is-message-not-string
                                       :message "Test assertion message should be a string"}))))))

(defn lint-arg-types! [ctx idacs call called-fn]
  (when-let [arg-types (:arg-types call)]
    (let [arg-types @arg-types
          tags (map #(tu/resolve-arg-type idacs %) arg-types)]
      (types/lint-arg-types ctx called-fn arg-types tags call)
      (when (and
             (= 'str (:name called-fn))
             (utils/one-of (:ns called-fn) [clojure.core cljs.core])
             (= 1 (count tags))
             (identical? :string (first tags))
             (not (identical? :off (-> call :config :linters :redundant-str-call :level)))
             (not (:clj-kondo.impl/generated (:expr call))))
        (findings/reg-finding! ctx
                               (assoc (utils/location call)
                                      :type :redundant-str-call
                                      :message "Single argument to str already is a string")))
      (lint-is-message-not-string! ctx call called-fn tags)
      (when-let [expected-type ('{double :double, float :float, long :long, int :int
                                  short :short, byte :byte, char :char, boolean :boolean}
                                (:name called-fn))]
        (when (and
               (not (identical? :off (-> call :config :linters :redundant-primitive-coercion :level)))
               (utils/one-of (:ns called-fn) [clojure.core cljs.core])
               (= 1 (count tags))
               (identical? expected-type (first tags))
               (not (:clj-kondo.impl/generated (:expr call))))
          (findings/reg-finding! ctx
                                 (assoc (utils/location call)
                                        :type :redundant-primitive-coercion
                                        :message (str "Redundant " (:name called-fn)
                                                      " coercion - expression already has type "
                                                      (name expected-type))))))
      (when (and
             (= '= (:name called-fn))
             (utils/one-of (:ns called-fn) [clojure.core cljs.core])
             (some #(= :double %) tags)
             (not (identical? :off (-> call :config :linters :equals-float :level))))
        (findings/reg-finding! ctx
                               (assoc (utils/location call)
                                      :type :equals-float
                                      :message "Equality comparison of floating point numbers")))
      (when (and
             (= 'empty? (:name called-fn))
             (utils/one-of (:ns called-fn) [clojure.core cljs.core])
             (utils/one-of (second (:callstack call)) [[cljs.core not] [clojure.core not]])
             (let [t (first tags)]
               (or (= :seq t)
                   (contains? (get types/is-a-relations t) :seq)))
             (not (identical? :off (-> call :config :linters :not-empty? :level))))
        (findings/reg-finding! ctx
                               (assoc (utils/location call)
                                      :type :not-empty?
                                      :message "Use (seq x) instead of (not (empty? x)) when x is a seq"))))))

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

(defn lint-redundant-nested-call
  "Lints calls of variadic functions/macros when nested."
  [call]
  (let [[[call-ns call-name :as c] parent] (:callstack call)
        mc (meta c)]
    (when (and (utils/one-of call-ns [clojure.core cljs.core])
               (utils/one-of call-name [* *' + +'
                                        and or
                                        lazy-cat max merge min str]
                             ;; disabled for perf reasons: comp concat every-pred some-fn
                             )
               (= [call-ns call-name] parent)
               ;; Exclude instances of nesting when directly inside threading macros
               (not (:derived-location mc))
               (let [{call-row :row
                      call-col :col} mc
                     {parent-row :row
                      parent-col :col} (meta parent)]
                 (and parent-row call-row
                      (<= parent-row call-row)
                      parent-col call-col
                      (< parent-col call-col))))
      (node->line
       (:filename call)
       (:expr call)
       :redundant-nested-call
       (format "Redundant nested call: %s" call-name)))))

#_(require 'clojure.pprint)

(defn lint-var-usage
  "Lints calls for arity errors, private calls errors. Also dispatches
  to call-specific linters."
  [ctx idacs]
  (let [config (:config ctx)
        linted-namespaces (:linted-namespaces idacs)]
    ;; (prn :from-cache from-cache)
    (doseq [ns (namespace/list-namespaces ctx)
            :let [base-lang (:base-lang ns)]
            call (:used-vars ns)
            :let [;; _ (clojure.pprint/pprint (dissoc call :config))
                  call? (= :call (:type call))
                  unresolved? (:unresolved? call)
                  allow-forward-reference? (:allow-forward-reference? call)
                  unresolved-ns (:unresolved-ns call)]
            :when (not unresolved-ns)
            :let [call-fn-name (:name call)
                  caller-ns-sym (:ns call)
                  call-lang (:lang call)
                  ctx (assoc ctx :lang call-lang :base-lang base-lang)
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
                                                resolved-ns call-fn-name unresolved? refer-alls)

                  #_#__ (when (not call?)
                          (clojure.pprint/pprint (dissoc call :config)))
                  name-meta (meta call-fn-name)
                  name-row (:row name-meta)
                  name-col (:col name-meta)
                  name-end-row (:end-row name-meta)
                  name-end-col (:end-col name-meta)
                  unresolved-var
                  (when (and (not called-fn)
                             (not (:interop? call))
                             row col end-row end-col
                             (contains? linted-namespaces resolved-ns)
                             (not (:resolved-core? call))
                             ;; the var could be :refer-all'ed, in this case unresolved? is true
                             (not allow-forward-reference?)
                             (not unresolved?))
                    (namespace/reg-unresolved-var!
                     ctx caller-ns-sym resolved-ns call-fn-name
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
                  valid-call? (or (and allow-forward-reference?
                                       called-fn)
                                  (not unresolved?)
                                  (when called-fn
                                    (or different-file?
                                        (not row-called-fn)
                                        (> row-call row-called-fn)
                                        (and (= row-call row-called-fn)
                                             (> (:col call) (:col called-fn))))))
                  _ (when-let [t (:type called-fn)]
                      (when (and call? (utils/one-of t [:string]))
                        (findings/reg-finding!
                         ctx
                         {:filename filename
                          :level :error
                          :row row
                          :end-row end-row
                          :col col
                          :end-col end-col
                          :type :not-a-function
                          :message (str "Can't call a " (name t)
                                        " as a function")})))
                  _ (when (and (not unresolved-var)
                               (not valid-call?))
                      (namespace/reg-unresolved-symbol!
                       ctx caller-ns-sym call-fn-name
                       (if call?
                         (assoc call
                                :row (or name-row row)
                                :col (or name-col col)
                                :end-row (or name-end-row end-row)
                                :end-col (or name-end-col end-col))
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
                  recursive? (and
                              (= fn-ns caller-ns-sym)
                              (= call-fn-name in-def))
                  _ (when (:analysis ctx)
                      (when-not (:interop? call)
                        (let [mexpr (meta (:expr call))]
                          (when-not (:skip-analysis mexpr)
                            (analysis/reg-usage! (assoc ctx :context (:context call))
                                                 filename
                                                 row
                                                 col
                                                 caller-ns-sym
                                                 resolved-ns call-fn-name arity
                                                 (when (= :cljc base-lang)
                                                   call-lang)
                                                 in-def
                                                 (assoc called-fn
                                                        :alias (:alias call)
                                                        :refer (:refer call)
                                                        :defmethod (:defmethod call)
                                                        :dispatch-val-str (:dispatch-val-str call)
                                                        :name-row name-row
                                                        :name-col name-col
                                                        :name-end-row name-end-row
                                                        :name-end-col name-end-col
                                                        :end-row end-row
                                                        :end-col end-col
                                                        :derived-location (:derived-location call)
                                                        :derived-name-location (:derived-location name-meta)))))))
                  call-config (:config call)
                  fn-sym (symbol (str resolved-ns)
                                 (str call-fn-name))]
            :when valid-call?
            :let [fn-name (:name called-fn)
                  _ (when (and ;; unresolved?
                           (:simple? call)
                           (contains? refer-alls
                                      fn-ns))
                      (namespace/reg-referred-all-var! (assoc ctx
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
                       (lint-single-logical-operand call))
                  redundant-nesting-error
                  (and call?
                       (not (utils/linter-disabled? call :redundant-nested-call))
                       (lint-redundant-nested-call call))]]
      (namespace/lint-discouraged-var! ctx (:config call) resolved-ns call-fn-name filename row end-row col end-col fn-sym {:varargs-min-arity varargs-min-arity
                                                                                                                            :fixed-arities fixed-arities
                                                                                                                            :arity arity} (:expr call))
      (when (and (not call?)
                 (identical? :fn (:type called-fn)))
        (when (:condition call)
          (findings/reg-finding!
           ctx (-> call
                   utils/location
                   (assoc :type :condition-always-true
                          :message "Condition always true")))))
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
      (when redundant-nesting-error
        (findings/reg-finding! ctx
                               redundant-nesting-error))
      (when (and (:private called-fn)
                 (not= caller-ns-sym
                       fn-ns)
                 (not (:private-access? call))
                 (not (utils/linter-disabled? call :private-call)))
        (findings/reg-finding! ctx
                               {:filename filename
                                :row row
                                :end-row end-row
                                :col col
                                :end-col end-col
                                :type :private-call
                                :message (format "#'%s is private"
                                                 (str fn-sym))}))
      (when-let [deprecated (:deprecated called-fn)]
        (when-not
         (or
          ;; recursive call
          recursive?
          (utils/linter-disabled? call :deprecated-var)
          (config/deprecated-var-excluded
           ctx
           (:config call)
           fn-sym
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
                                            (when-not (true? deprecated)
                                              (str " since " deprecated)))})))
      (when called-fn
        (when-let [loc (:redundant-fn-wrapper-parent-loc call)]
          (when-not (:macro called-fn)
            (findings/reg-finding! ctx (assoc loc
                                              :filename filename
                                              :type :redundant-fn-wrapper
                                              :message "Redundant fn wrapper")))))
      (when (and called-fn
                 (not (identical? :off (-> call-config :linters :redundant-call :level)))
                 (= 1 (:arity call))
                 ;; handled based on argument type
                 (not (utils/one-of fn-sym [clojure.core/str cljs.core/str]))
                 (config/redundant-call-included? call-config fn-sym))
        (findings/reg-finding!
         ctx
         {:filename filename
          :row row
          :end-row end-row
          :col col
          :end-col end-col
          :type :redundant-call
          :message (format "Single arg use of %s always returns the arg itself" fn-sym)}))
      (let [ctx (assoc ctx :filename filename)]
        (when call?
          (lint-specific-calls!
           (assoc ctx :filename filename)
           call called-fn)
          (when-not (or arity-error? skip-arity-check?)
            (lint-arg-types! ctx idacs call called-fn))))
      (when call?
        (when-let [idx (:idx call)]
          (let [normalized-sym (symbol (let [cns (str (:ns called-fn))]
                                         (if (= "cljs.core" cns) "clojure.core" cns))
                                       (str (:name called-fn)))]
            (when (contains? var-info/unused-values normalized-sym)
              (let [unused-value-conf (-> config :linters :unused-value)]
                (when-not (identical? :off (:level unused-value-conf))
                  (let [cs (:callstack call)
                        parent-call (second cs)
                        core? (utils/one-of (first parent-call) [clojure.core cljs.core])
                        core-sym (when core?
                                   (second parent-call))
                        unused?
                        (and (not (:clj-kondo.impl/generated (meta (first cs))))
                             (or (and core?
                                      (or
                                       ;; doseq always return nil
                                       (utils/one-of core-sym [doseq])
                                       (< idx (dec (:len call))))
                                      (utils/one-of core-sym [do fn defn defn-
                                                              let when-let loop binding with-open
                                                              doseq try when when-not when-first
                                                              when-some future]))
                                 (= '[clojure.test deftest] parent-call)))]
                    (when unused?
                      (findings/reg-finding!
                       (cond-> ctx
                         call-config
                         (assoc :config call-config))
                       {:filename filename
                        :row row
                        :end-row end-row
                        :col col
                        :end-col end-col
                        :type :unused-value
                        :message "Unused value"}))))))))))))

(defn- lint-aliased-referred-var! [ctx ns]
  (when-not (utils/linter-disabled? ctx :aliased-referred-var)
    (when-let [referred-vars (seq (:referred-vars ns))]
      (let [referred-by-full-name (->> referred-vars
                                       (map (fn [[k info]]
                                              [[(str (:ns info))
                                                (str (:name info))]
                                               k]))
                                       (into {}))]
        (doseq [{:keys [alias resolved-ns] :as usage} (:used-vars ns)
                :let [v-name (str (:name usage))
                      v-ns (str (or resolved-ns (:to usage)))
                      full-name [v-ns v-name]
                      referred-name (when alias
                                      (get referred-by-full-name full-name))]
                :when referred-name
                :let [msg (format "Var %s is referred but used via alias: %s"
                                  v-name alias)]]
          (findings/reg-finding!
           ctx
           {:filename (:filename ns)
            :row (:row usage)
            :col (:col usage)
            :type :aliased-referred-var
            :message msg}))))))

(defn lint-unused-namespaces!
  [ctx idacs]
  (let [config (:config ctx)]
    (doseq [ns (namespace/list-namespaces ctx)
            :let [required (:required ns)
                  used-namespaces (:used-namespaces ns)
                  unused (set/difference
                          (set required)
                          (set used-namespaces))
                  unused (remove (comp :unused-namespace-disabled meta) unused)
                  referred-vars (:referred-vars ns)
                  used-referred-vars (:used-referred-vars ns)
                  used-aliases (:used-aliases ns)
                  aliases (:aliases ns)
                  refer-alls (:refer-alls ns)
                  refer-all-nss (set (keys refer-alls))
                  ns-config (:config ns)
                  config (or ns-config config)
                  ns-excluded-config (config/unused-namespace-excluded-config config)
                  refer-all-excluded-config (config/refer-all-excluded-config config)
                  deprecated-namespace-excluded-config (config/deprecated-namespace-excluded-config config)
                  ctx (if ns-config (assoc ctx :config config) ctx)
                  ctx (assoc ctx :lang (:lang ns) :base-lang (:base-lang ns))]]
      (doseq [required required]
        (when-let [depr (:deprecated (utils/resolve-ns idacs (:base-lang ns) (:lang ns) required))]
          (when-not (config/deprecated-namespace-excluded? deprecated-namespace-excluded-config required)
            (let [filename (:filename (meta required))]
              (findings/reg-finding!
               ctx
               (node->line filename required :deprecated-namespace
                           (format "Namespace %s is deprecated%s."
                                   (str required)
                                   (if (string? depr)
                                     (str " since " depr)
                                     ""))))))))
      (doseq [ns-sym unused]
        (let [ns-meta (meta ns-sym)]
          (when-not (or (config/unused-namespace-excluded ctx ns-excluded-config ns-sym)
                        (some-> ns-meta :alias meta :as-alias))
            (let [m (meta ns-sym)
                  filename (:filename m)]
              (findings/reg-finding!
               ctx
               (-> (node->line filename ns-sym :unused-namespace
                               (format "namespace %s is required but never used" ns-sym))
                   (assoc :ns (export-ns-sym ns-sym))))))))
      (doseq [[k v] referred-vars]
        (let [var-ns (:ns v)
              config (:config v)
              ctx (assoc ctx :config config)]
          (when-not
           (or (contains? used-referred-vars k)
               (config/unused-referred-var-excluded config var-ns k)
               (contains? refer-all-nss var-ns)
               (:cljs-macro-self-require (meta k)))
            (let [filename (:filename v)
                  referred-ns (export-ns-sym var-ns)]
              (findings/reg-finding!
               ctx
               (-> (node->line filename k :unused-referred-var (str "#'" var-ns "/" (:name v) " is referred but never used"))
                   (assoc :ns referred-ns
                          :referred-ns referred-ns
                          :refer (:name v))))))))
      (doseq [[referred-all-ns {:keys [referred node] :as refer-all}] refer-alls
              :when (not (config/refer-all-excluded? refer-all-excluded-config referred-all-ns))]
        (let [{:keys [k value]} node
              use? (or (= :use k)
                       (= 'use value))
              finding-type (if use? :use :refer-all)
              referred (sort referred)
              msg (str (format "use %salias or :refer"
                               (if use?
                                 (str (when k ":") "require with ")
                                 ""))
                       (when (seq referred)
                         (format " [%s]"
                                 (str/join " " referred))))
              filename (:filename refer-all)]
          (findings/reg-finding!
           ctx
           (assoc (node->line filename node
                              finding-type msg)
                  :refers (vec referred)))))
      (doseq [alias (keys aliases)]
        (when-not (contains? used-aliases alias)
          (findings/reg-finding!
           ctx
           (node->line (:filename (meta alias)) alias
                       :unused-alias (str "Unused alias: " alias)))))
      (lint-aliased-referred-var! ctx ns))))

(defn lint-unused-excluded-vars! [ctx]
  (when-not (utils/linter-disabled? ctx :unused-excluded-var)
    (doseq [{:keys [clojure-excluded] :as ns} (namespace/list-namespaces ctx)
            :when (and (seq clojure-excluded)
                       (not (utils/linter-disabled? ns :unused-excluded-var)))
            :let [{:keys [lang base-lang referred-vars vars bindings]} ns
                  used (set (concat (keys vars)
                                    (map :name (vals referred-vars))
                                    (map :name bindings)))
                  ctx (assoc ctx :lang lang :base-lang base-lang)]
            excluded clojure-excluded
            :when (and (not (contains? used excluded))
                       (not (utils/ignored? excluded))
                       (var-info/core-sym? lang excluded))]
      (findings/reg-finding!
       ctx
       (node->line (:filename ns) excluded :unused-excluded-var
                   (format "Unused excluded var: %s" excluded))))))

(defn lint-discouraged-namespaces!
  [ctx]
  (let [config (:config ctx)]
    (doseq [ns (namespace/list-namespaces ctx)
            ns-sym (:required ns)
            :let [ns-config (:config ns)
                  lang (:lang ns)
                  m (meta ns-sym)
                  filename (:filename m)
                  config (or ns-config config)
                  ns-groups (config/ns-groups ctx config ns-sym filename)
                  linter-configs (keep #(get-in config [:linters :discouraged-namespace %]) (concat ns-groups [ns-sym]))]
            :when (seq linter-configs)
            :let [linter-config (apply config/merge-config! linter-configs)
                  {:keys [message level]} linter-config
                  message (or message (str "Discouraged namespace: " ns-sym))
                  ctx (assoc ctx :lang lang :base-lang (:base-lang ns) :config config)]]
      (findings/reg-finding!
       ctx
       (-> (node->line filename ns-sym :discouraged-namespace message)
           (assoc :ns (export-ns-sym ns-sym))
           (cond-> level
             (assoc :level level)))))))

(defn lint-bindings!
  [ctx]
  (doseq [ns (namespace/list-namespaces ctx)]
    (let [ns-config (:config ns)
          ctx (if ns-config
                (assoc ctx :config ns-config)
                ctx)
          ctx (assoc ctx :lang (:lang ns) :base-lang (:base-lang ns))
          config (:config ctx)
          unused-binding-excluded-config (config/unused-binding-excluded-config config)
          used-underscored-binding-excluded-config (config/used-underscored-binding-excluded-config config)]
      (when-not (identical? :off (-> ctx :config :linters :used-underscored-binding :level))
        (doseq [binding (into #{}
                              (comp
                               (remove :clj-kondo/mark-used)
                               (remove (comp :clj-kondo.impl/generated meta))
                               (remove :clj-kondo.impl/generated)
                               (filter #(str/starts-with? (str (:name %)) "_")))
                              (:used-bindings ns))
                :when (not (config/used-underscored-binding-excluded? ctx used-underscored-binding-excluded-config
                                                                      (:name binding)))]
          (findings/reg-finding!
           ctx
           {:type :used-underscored-binding
            :filename (:filename binding)
            :message (str "Used binding is marked as unused: " (:name binding))
            :row (:row binding)
            :col (:col binding)
            :end-row (:end-row binding)
            :end-col (:end-col binding)})))
      (when-not (identical? :off (-> ctx :config :linters :unused-binding :level))
        (let [bindings (:bindings ns)
              used-bindings (:used-bindings ns)
              diff (set/difference bindings used-bindings)
              diff (remove :clj-kondo/mark-used diff)
              defaults (:destructuring-defaults ns)]
          (doseq [binding diff]
            (let [nm (:name binding)]
              (when-not (or (str/starts-with? (str nm) "_")
                            (config/unused-binding-excluded? ctx unused-binding-excluded-config nm))
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
              :end-col (:end-col default)}))))
      (when (not (identical? :off (-> ctx :config :linters :keyword-binding :level)))
        (doseq [binding (filter :keyword (:bindings ns))]
          (when-not (or (namespace (:keyword binding))
                        (:auto-resolved binding))
            (findings/reg-finding!
             ctx
             {:type :keyword-binding
              :filename (:filename binding)
              :message (str "Keyword binding should be a symbol: " (keyword (:name binding)))
              :row (:row binding)
              :col (:col binding)
              :end-row (:end-row binding)
              :end-col (:end-col binding)})))))))

(defn lint-unused-private-vars!
  [ctx]
  (let [config (:config ctx)]
    (doseq [{:keys [filename vars used-vars base-lang lang]
             ns-nm :name
             ns-config :config} (namespace/list-namespaces ctx)
            :let [config (or ns-config config)
                  ctx (if ns-config (assoc ctx :config config) ctx)
                  ctx (assoc ctx :lang lang :base-lang base-lang)
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
            :when
            (not (utils/one-of (:defined-by->lint-as v)
                               [clojure.core/defrecord
                                cljs.core/defrecord
                                clojure.core/deftype
                                cljs.core/deftype
                                clojure.core/definterface
                                cljs.core/definterface]))
            :when (not (str/starts-with? var-name "_"))]
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
                  ctx (assoc ctx :lang lang :base-lang (:base-lang ns))]
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
                  ctx (assoc ctx :lang lang :base-lang (:base-lang ns))]
            [_ vs] (:unresolved-vars ns)
            v (cond->> vs
                hide-duplicates? (take 1))]
      (let [filename (:filename v)
            expr (:expr v)
            n (if-let [fst-child (some-> expr :children first)]
                (str fst-child)
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
                ctx (assoc ctx :lang (:lang ns) :base-lang (:base-lang ns))]
          :when (not (identical? :off (-> ns-config :linters :unused-import :level)))
          :let [filename (:filename ns)
                imports (:imports ns)
                used-imports (:used-imports ns)]
          [import package] imports
          :when (and
                 (not (:clj-kondo/mark-used (meta import)))
                 (not (contains? used-imports import)))]
    (findings/reg-finding!
     ctx
     (-> (node->line filename import :unused-import (str "Unused import " import))
         (assoc :class (symbol (str package "." import)))))))

(defn lint-unresolved-namespaces!
  [ctx]
  (let [hide-duplicates? (not (get-in ctx [:config :linters :unresolved-namespace :report-duplicates]))]
    (doseq [ns (namespace/list-namespaces ctx)
            :let [ctx (assoc ctx :lang (:lang ns) :base-lang (:base-lang ns))]
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
        :end-col (:end-col m)
        :ns un
        :name (:name m)}))))

(defn lint-shadowed-defmethods!
  "Lints for shadowed defmethods.
  - In same namespace: second+ definitions shadow the first
  - Across namespaces: all definitions are flagged since load order is 
   non-deterministic"
  [ctx]
  (letfn [(group-defmethods [namespaces]
            (reduce
             (fn [acc ns]
               (reduce
                (fn [acc* [[mm-name dispatch-val] metadata]]
                  (if (constant? (:dispatch-val-node metadata))
                    (let [lang (:lang metadata :clj)
                          defmulti-ns (:defmulti-ns metadata)
                          group-key [defmulti-ns mm-name dispatch-val lang]]
                      (update acc* group-key
                              (fnil conj [])
                              (assoc metadata :ns-name (:name ns))))
                    acc*))
                acc (:defmethods ns [])))
             {} namespaces))]
    (when-not (utils/linter-disabled? ctx :shadowed-defmethod)
      (let [namespaces (namespace/list-namespaces ctx)
            defmethod-groups (group-defmethods namespaces)]
        (doseq [[[defmulti-ns mm-name dispatch-val _lang] defmethods] defmethod-groups
                :when (> (count defmethods) 1)
                :let [ns-names (set (map :ns-name defmethods))
                      same-ns? (= 1 (count ns-names))
                      msg (format shadowed-defmethod-msg mm-name dispatch-val)
                      shadowed-defmethods (cond->> defmethods
                                            same-ns? rest)]
                dm shadowed-defmethods]
          (findings/reg-finding! ctx (assoc (utils/location dm)
                                            :type :shadowed-defmethod
                                            :message msg)))))))

(defn lint-class-usage [ctx idacs]
  (when-let [jm (:java-member-definitions idacs)]
    (doseq [usage @(:java-class-usages ctx)

            :when (:call usage)]
      (let [method (:method-name usage)
            clazz (:class usage)
            config (:config usage)
            filename (:filename usage)
            lang (:lang usage)
            ctx (cond-> ctx
                  config (assoc :config config)
                  filename (assoc :filename filename)
                  lang (assoc :lang lang))]
        (when-let [info (get jm clazz)]
          (when-let [meth-info (get (:members info) method)]
            (when (and (:call usage)
                       (every? #(contains? (:flags %) :field) meth-info))
              (findings/reg-finding!
               ctx
               (assoc (dissoc usage :config)
                      :type :java-static-field-call
                      :message "Static fields should be referenced without parens unless they are intended as function calls")))))))))

(defn lint-redundant-ignores
  [ctx]
  (let [cfg (-> ctx :config :linters :redundant-ignore)
        excludes (some-> cfg :exclude set)]
    (when-not (identical? :off (-> cfg :level))
      (let [ignores @(:ignores ctx)]
        (doseq [[filename m] ignores
                [lang ignores] m
                ignore ignores]
          (let [linters (:ignore ignore)
                m (:clj-kondo/ignore ignore)]
            (when (map? m)
              (when-not (and excludes
                             (seqable? linters)
                             (some excludes linters))
                (when-not (or (:used ignore)
                              ;; #2433
                              (:derived-location ignore))
                  (findings/reg-finding! ctx (assoc m
                                                    :cljc (:cljc ignore)
                                                    :type :redundant-ignore
                                                    :message "Redundant ignore"
                                                    :lang lang
                                                    :filename filename)))))))))))

(defn lint-protocol-impls!
  [ctx idacs]
  (doseq [ns (namespace/list-namespaces ctx)
          :let [ns-config (:config ns)
                ctx (if ns-config (assoc ctx :config ns-config)
                        ctx)
                ctx (assoc ctx :lang (:lang ns) :base-lang (:base-lang ns))]
          protocol-impl (:protocol-impls ns)
          :let [protocol-methods (:methods protocol-impl)
                protocol-ns (:protocol-ns protocol-impl)
                protocol-name (:protocol-name protocol-impl)]]
    (when-let [resolved (utils/resolve-call* idacs ctx protocol-ns protocol-name)]
      (when-let [methods (:methods resolved)]
        (when-let [unresolved-protocol-methods (seq (remove (set methods) protocol-methods))]
          (doseq [unresolved unresolved-protocol-methods]
            (findings/reg-finding! ctx (assoc (meta unresolved)
                                              :type :unresolved-protocol-method
                                              :filename (:filename ns)
                                              :message (str "Unresolved protocol method: " unresolved)))))
        (when-let [missing (seq (remove (set protocol-methods) methods))]
          (findings/reg-finding! ctx (assoc protocol-impl
                                            :type :missing-protocol-method
                                            :filename (:filename ns)
                                            :message (str "Missing protocol method(s): " (str/join ", " missing)))))))))

;;;; scratch

(comment)
