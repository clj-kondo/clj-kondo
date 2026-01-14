(ns clj-kondo.impl.config
  {:no-doc true}
  (:refer-clojure :exclude [unquote])
  (:require
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :as utils :refer [deep-merge map-vals]]
   [clojure.set :as set]
   [clojure.walk :as walk]))

(set! *warn-on-reflection* true)

(def default-config
  '{;; no linting inside calls to these functions/macros
    ;; note that you will still get an arity error when calling the fn/macro itself incorrectly
    :skip-args [#_clojure.core/comment #_cljs.core/comment]
    :skip-comments false ;; convenient shorthand for :skip-args [clojure.core/comment cljs.core/comment]
    ;; linter level can be tweaked by setting :level to :error, :warning or :info (or any other keyword)
    ;; all linters are enabled by default, but can be turned off by setting :level to :off.
    ;; :config-in-comment {} config override for comment blocks
    :linters {:invalid-arity {:level :error
                              :skip-args [#_riemann.test/test-stream]}
              :conflicting-fn-arity {:level :error}
              :not-a-function {:level :error
                               :skip-args [#_user/foo]}
              :private-call {:level :error}
              :inline-def {:level :warning}
              :def-fn {:level :off}
              :redundant-do {:level :warning}
              :redundant-let {:level :warning}
              :redundant-let-binding {:level :off}
              :cond-else {:level :warning}
              :syntax {:level :error}
              :file {:level :error}
              :missing-test-assertion {:level :warning}
              :conflicting-alias {:level :error}
              :duplicate-map-key {:level :error}
              :duplicate-set-key {:level :error}
              :duplicate-require {:level :warning}
              :duplicate-field {:level :error}
              :duplicate-key-args {:level :warning}
              :missing-map-value {:level :error}
              :redefined-var {:level :warning}
              :var-same-name-except-case {:level :warning}
              :unreachable-code {:level :warning}
              :datalog-syntax {:level :error}
              :unbound-destructuring-default {:level :warning}
              :used-underscored-binding {:level :off}
              :unknown-require-option {:level :warning}
              :unknown-ns-option {:level :warning}
              :unused-binding {:level :warning
                               :exclude-destructured-keys-in-fn-args false
                               :exclude-destructured-as false
                               :exclude-defmulti-args false}
              :shadowed-fn-param {:level :warning}
              :unsorted-required-namespaces {:level :off
                                             :sort :case-insensitive}
              :unsorted-imports {:level :off}
              :unused-namespace {:level :warning
                                 ;; don't warn about these namespaces:
                                 :exclude [#_clj-kondo.impl.var-info-gen]
                                 :simple-libspec false}

              :unresolved-symbol {:level :error
                                  :exclude [;; ignore globally:
                                            #_js*
                                            ;; ignore occurrences of service and event in call to riemann.streams/where:
                                            #_(riemann.streams/where [service event])
                                            ;; ignore all unresolved symbols in one-of:
                                            #_(clj-kondo.impl.utils/one-of)
                                            (leiningen.core.project/defproject) ;; ignore project.clj's defproject
                                            (clojure.test/are [thrown? thrown-with-msg?])
                                            (cljs.test/are [thrown? thrown-with-msg?])
                                            (clojure.test/is [thrown? thrown-with-msg?])
                                            (cljs.test/is [thrown? thrown-with-msg?])]}
              :unresolved-var {:level :warning}
              :unresolved-namespace {:level :warning
                                     :exclude [#_foo.bar]}
              ;; for example: foo.bar is always loaded in a user profile
              :reduce-without-init {:level :off
                                    :exclude [#_foo.bar/baz]}
              :misplaced-docstring {:level :warning}
              :not-empty? {:level :warning}
              :deprecated-var {:level :warning
                               #_:exclude
                               #_{foo.foo/deprecated-fn
                                  ;; suppress warnings in the following namespaces
                                  {:namespaces [foo.bar "bar\\.*"]
                                   ;; or in these definitions:
                                   :defs [foo.baz/allowed "foo.baz/ign\\.*"]}}}
              :deprecated-namespace {:level :warning}
              :unused-referred-var {:level :warning
                                    :exclude {#_#_taoensso.timbre [debug]}}
              :unused-private-var {:level :warning}
              :refer {:level :off
                      #_:exclude
                      #_[clojure.test]}
              :refer-all {:level :warning
                          :exclude #{}}
              :unresolved-excluded-var {:level :info}
              :use {:level :warning}
              :missing-else-branch {:level :warning}
              :if-nil-return {:level :off}
              :case-duplicate-test {:level :error}
              :duplicate-refer {:level :warning}
              :case-quoted-test {:level :warning}
              :case-symbol-test {:level :off}
              :type-mismatch {:level :error}
              :missing-docstring {:level :off}
              :docstring-blank {:level :warning}
              :docstring-no-summary {:level :off}
              :docstring-leading-trailing-whitespace {:level :off}
              :consistent-alias {:level :warning
                                 ;; warn when alias for clojure.string is
                                 ;; different from str
                                 :aliases {#_clojure.string #_str}}
              :unused-import {:level :warning}
              :unused-excluded-var {:level :info}
              :single-operand-comparison {:level :warning}
              :single-logical-operand {:level :warning}
              :redundant-nested-call {:level :info}
              :single-key-in {:level :off}
              :missing-clause-in-try {:level :warning}
              :missing-body-in-when {:level :warning}
              :hook {:level :error}
              :format {:level :error}
              :shadowed-var {:level :off
                             #_#_:suggestions {clojure.core/type tajpu
                                               clojure.core/name nomspaco}
                             #_#_:exclude [frequencies]
                             #_#_:include [name]}
              :deps.edn {:level :warning}
              :bb.edn-undefined-task {:level :error}
              :bb.edn-cyclic-task-dependency {:level :error}
              :bb.edn-unexpected-key {:level :warning}
              :bb.edn-task-missing-docstring {:level :off}
              :clj-kondo-config {:level :warning}
              :redundant-expression {:level :warning}
              :loop-without-recur {:level :warning}
              :unexpected-recur {:level :error}
              :main-without-gen-class {:level :off}
              :redundant-fn-wrapper {:level :off}
              :namespace-name-mismatch {:level :error}
              :non-arg-vec-return-type-hint {:level :warning}
              :keyword-binding {:level :off}
              :discouraged-var {:level :warning}
              :discouraged-namespace {:level :warning}
              :discouraged-tag {:level :warning}
              :discouraged-java-method {:level :warning}
              :redundant-call {:level :off
                               #_#_:exclude #{clojure.core/->}
                               #_#_:include #{clojure.core/conj!}}
              :redundant-str-call {:level :info}
              :redundant-primitive-coercion {:level :info}
              :redundant-format {:level :info}
              :warn-on-reflection {:level :off
                                   :warn-only-on-interop true}
              :aliased-namespace-symbol {:level :off
                                         :exclude #{#_clojure.string}}
              :line-length {:level :warning
                            :max-line-length nil}
              :unused-value {:level :warning}
              :dynamic-var-not-earmuffed {:level :off}
              :earmuffed-var-not-dynamic {:level :warning}
              :aliased-namespace-var-usage {:level :warning}
              :uninitialized-var {:level :warning}
              :equals-false {:level :off}
              :equals-true {:level :off}
              :equals-nil {:level :off}
              :equals-float {:level :off}
              :plus-one {:level :off}
              :minus-one {:level :off}
              :protocol-method-varargs {:level :error}
              :unused-alias {:level :off}
              :self-requiring-namespace {:level :warning}
              :condition-always-true {:level :off}
              :underscore-in-namespace {:level :warning}
              :multiple-async-in-deftest {:level :warning}
              :java-static-field-call {:level :error}
              :equals-expected-position {:level :off
                                         :position :first}
              :destructured-or-binding-of-same-map {:level :warning}
              :min-clj-kondo-version {:level :warning
                                      ;; the version itself is configured at the top level
                                      }
              :redundant-ignore {:level :info}
              :schema-misplaced-return {:level :warning}
              :do-template {:level :warning}
              :unresolved-protocol-method {:level :warning}
              :missing-protocol-method {:level :warning}
              :locking-suspicious-lock {:level :warning}
              :destructured-or-always-evaluates {:level :off}
              :unquote-not-syntax-quoted {:level :warning}}
    ;; :hooks {:macroexpand ... :analyze-call ...}
    :lint-as {cats.core/->= clojure.core/->
              cats.core/->>= clojure.core/->>
              rewrite-clj.custom-zipper.core/defn-switchable clojure.core/defn
              clojure.core.async/go-loop clojure.core/loop
              clojure.test.check.generators/let clojure.core/let
              cljs.core.async/go-loop clojure.core/loop
              cljs.core.async.macros/go-loop clojure.core/loop
              schema.core/defschema clojure.core/def
              compojure.core/defroutes clojure.core/def
              compojure.core/let-routes clojure.core/let}
    ;; :auto-load-configs true
    ;; :analysis ;; what to analyze and whether to output it
    :output {:format :text ;; or :edn, or :json
             :summary true ;; include summary in output
             ;; set to truthy to print progress while linting, only applicable to output :text
             :progress false
             ;; output can be filtered and removed by regex on filename. empty options leave the output untouched.
             :include-files [] #_["^src" "^test"]
             :exclude-files [] #_["^cljs/core"]
             ;; the output pattern can be altered using a template. use {{LEVEL}} to print the level in capitals.
             ;; the default template looks like this:
             ;; :pattern "{{filename}}:{{row}}:{{col}}: {{level}}: {{message}}"
             ;; if below :linter-name is set to true, type (linter name) of reported the finding
             ;; is appended to the end of the default pattern as " [{{type}}]"
             :linter-name false
             :canonical-paths false} ;; set to true to see absolute file paths and jar files
    ;; print a warning if used with a clj-kondo release older than this
    ;; :min-clj-kondo-version "2019.10.26"
    })

(defn expand-ignore
  ":ignore true / [:unresolved-symbol] can only be used in
  config-in-call, config-in-ns and ns metadata."
  [cfg]
  (if-let [ignore (:ignore cfg)]
    (let [linters (:linters cfg)
          linters (reduce (fn [linters k]
                            (assoc-in linters [k :level] :off))
                          linters
                          (if (true? ignore)
                            (keys (:linters default-config))
                            ignore))]
      (assoc cfg :linters linters))
    cfg))

(defn merge-config!
  ([])
  ([cfg] cfg)
  ([cfg* cfg]
   (if (empty? cfg) cfg*
       (let [cfg (cond-> cfg
                   (contains? (:linters cfg) :if)
                   (assoc-in [:linters :missing-else-branch] (:if (:linters cfg))))]
         (deep-merge cfg* cfg))))
  ([cfg* cfg & cfgs]
   (reduce merge-config! cfg* (cons cfg cfgs))))

(defn fq-sym->vec [fq-sym]
  (if-let [ns* (namespace fq-sym)]
    [(symbol ns*) (symbol (name fq-sym))]
    (do (findings/reg-finding! utils/*ctx*
                               {:type :clj-kondo-config
                                :level :warning
                                :row 0
                                :col 0
                                :message (str "Configuration error. Expected fully qualified symbol, got: " fq-sym)
                                :filename (:filename utils/*ctx*)})
        nil)))

(defn fq-syms->vecs
  [fq-syms]
  (keep (fn [fq-sym]
          (when (symbol? fq-sym)
            (if-let [ns* (namespace fq-sym)]
              [(symbol ns*) (symbol (name fq-sym))]
              (do (findings/reg-finding! utils/*ctx*
                                         {:type :clj-kondo-config
                                          :level :warning
                                          :row 0
                                          :col 0
                                          :message (str "Configuration error. Expected fully qualified symbol, got: " fq-sym)
                                          :filename (:filename utils/*ctx*)})
                  nil))))
        fq-syms))

(defn skip-args
  ([config linter]
   (some-> (get-in config [:linters linter :skip-args])
           (fq-syms->vecs))))

(defn skip?
  "Used by invalid-arity linter. We optimize for the case that disable-within returns an empty sequence"
  ([config linter callstack]
   (when-let [disabled (seq (skip-args config linter))]
     (some (fn [disabled-sym]
             (some #(= disabled-sym %) callstack))
           disabled))))

(defn lint-as [config v]
  (some-> (get-in config [:lint-as v])
          fq-sym->vec))

(defn unused-namespace-excluded-config [config]
  (let [excluded (get-in config [:linters :unused-namespace :exclude])
        syms (set (filter symbol? excluded))
        regexes (filter string? excluded)]
    {:syms syms :regexes regexes}))

(defn unused-namespace-excluded [ctx config ns-sym]
  (let [re-find-memo (:re-find-memo ctx)
        {:keys [syms regexes]} config]
    (or (contains? syms ns-sym)
        (let [ns-str (str ns-sym)]
          (boolean (some #(re-find-memo % ns-str) regexes))))))

(defn unused-referred-var-excluded [config ns-sym var-sym]
  (let [excluded (let [excluded (get-in config [:linters :unused-referred-var :exclude])]
                   (if (or (nil? excluded)
                           (map? excluded))
                     (when excluded (map-vals set excluded))
                     (let [warning "[clj-kondo] WARNING: configuration value in [:linters :referred-var :exclude] should be a map"]
                       (binding [*out* *err*]
                         (println warning)
                         nil))))]
    (when-let [vars (get excluded ns-sym)]
      (contains? vars var-sym))))

(defn unresolved-namespace-excluded-config [config]
  (set (get-in config [:linters :unresolved-namespace :exclude])))

(defn unresolved-namespace-excluded [excluded ns-sym]
  (contains? excluded ns-sym))

(defn unresolved-symbol-excluded [ctx config callstack sym]
  (let [{:keys [excluded excluded-in exclude-patterns]}
        (let [unresolved-symbol-config (get-in config [:linters :unresolved-symbol])
              excluded (get unresolved-symbol-config :exclude)
              exclude-patterns (get unresolved-symbol-config :exclude-patterns)
              syms (set (filter symbol? excluded))
              calls (filter list? excluded)]
          {:excluded syms
           :excluded-in
           (reduce (fn [acc [fq-name excluded]]
                     (if-let [nss (namespace fq-name)]
                       (let [ns-nm (symbol nss)
                             var-name (symbol (name fq-name))]
                         (update acc [ns-nm var-name]
                                 (fn [old]
                                   (cond (nil? old)
                                         (if excluded
                                           (set excluded)
                                           identity)
                                         (set? old)
                                         (if excluded
                                           (into old excluded)
                                           old)
                                         :else identity))))
                       acc))
                   {} calls)
           :exclude-patterns exclude-patterns})]
    (or (contains? excluded sym)
        (some #(when-let [check-fn (get excluded-in %)]
                 ;; e.g. for user/defproject, check-fn is identity, so any
                 ;; truthy value will be excluded inside of that
                 (check-fn sym))
              callstack)
        (let [sym-str (str sym)
              re-find (:re-find-memo ctx)]
          (some #(re-find % sym-str) exclude-patterns)))))

(defn unresolved-var-excluded [config ns-sym fn-sym]
  (let [cfg (let [excluded (get-in config [:linters :unresolved-var :exclude])
                  vars (into #{} (comp (filter qualified-symbol?)
                                       (map fq-sym->vec))
                             excluded)
                  nss (into #{} (filter simple-symbol?)
                            excluded)]
              {:excluded-vars vars
               :excluded-nss nss})]
    (or (contains? (:excluded-nss cfg) ns-sym)
        (contains? (:excluded-vars cfg) [ns-sym fn-sym]))))

(defn deprecated-var-excluded [ctx config var-sym excluded-ns excluded-in-def]
  (let [{:keys [namespace-regexes namespace-syms
                def-regexes def-syms]} (let [excluded (get-in config [:linters :deprecated-var :exclude var-sym])
                                             namespaces (:namespaces excluded)
                                             namespace-regexes (filter string? namespaces)
                                             namespace-syms (set (filter symbol? namespaces))
                                             defs (:defs excluded)
                                             def-regexes (filter string? defs)
                                             def-syms (set (filter symbol? defs))]
                                         {:namespace-regexes namespace-regexes
                                          :namespace-syms namespace-syms
                                          :def-regexes def-regexes
                                          :def-syms def-syms})
        re-find (:re-find-memo ctx)]
    (or (when excluded-in-def
          (let [excluded-in-def (symbol (str excluded-ns) (str excluded-in-def))]
            (or (contains? def-syms excluded-in-def)
                (let [excluded-in-def-str (str excluded-in-def)]
                  (boolean (some #(re-find % excluded-in-def-str) def-regexes))))))
        (contains? namespace-syms excluded-ns)
        (let [ns-str (str excluded-ns)]
          (boolean (some #(re-find % ns-str) namespace-regexes))))))

(defn type-mismatch-config [config var-ns var-name]
  ;; (prn (get-in config [:linters :type-mismatch :namespaces 'foo 'foo]))
  (get-in config [:linters :type-mismatch :namespaces var-ns var-name]))

(defn unused-private-var-excluded [config ns-nm var-name]
  (contains? (let [syms (get-in config [:linters :unused-private-var :exclude])]
               (set syms)) (symbol (str ns-nm) (str var-name))))

(defn refer-excluded? [config referred-ns]
  (let [excluded (let [syms (get-in config [:linters :refer :exclude])]
                   (set syms))]
    (contains? excluded referred-ns)))

(defn refer-all-excluded-config [config]
  (let [syms (get-in config [:linters :refer-all :exclude])]
    (set syms)))

(defn refer-all-excluded? [excluded referred-all-ns]
  (contains? excluded referred-all-ns))

(defn shadowed-var-excluded? [config sym]
  (when-let [cfg (let [cfg (get-in config [:linters :shadowed-var])
                       exclude (some-> (:exclude cfg) set)
                       include (some-> (:include cfg) set)]
                   (cond-> nil
                     exclude (assoc :exclude exclude)
                     include (assoc :include include)))]
    (let [{:keys [exclude include]} cfg]
      (if include
        (not (contains? include sym))
        (or (not exclude)
            (contains? exclude sym))))))

(defn reduce-without-init-excluded? [config sym]
  (when-let [cfg (let [cfg (get-in config [:linters :reduce-without-init])
                       exclude (some-> (:exclude cfg) set)]
                   (cond-> nil
                     exclude (assoc :exclude exclude)))]
    (let [{:keys [exclude]} cfg]
      (or (not exclude)
          (contains? exclude sym)))))

(def redundant-call-vars '#{clojure.core/-> cljs.core/->
                            clojure.core/->> cljs.core/->>
                            clojure.core/cond-> cljs.core/cond->
                            clojure.core/cond->> cljs.core/cond->>
                            clojure.core/some-> cljs.core/some->
                            clojure.core/some->> cljs.core/some->>
                            clojure.core/partial cljs.core/partial
                            clojure.core/comp cljs.core/comp
                            clojure.core/merge cljs.core/merge})

(defn redundant-call-included? [config sym]
  (contains? (let [cfg (get-in config [:linters :redundant-call])
                   include (some-> (:include cfg) set)
                   exclude (some-> (:exclude cfg) set)]
               (-> redundant-call-vars
                   (set/union include)
                   (set/difference exclude))) sym))

(defn aliased-namespace-symbol-excluded? [config sym-ns]
  (let [excluded (let [syms (get-in config [:linters :aliased-namespace-symbol :exclude])]
                   (set syms))]
    (contains? excluded sym-ns)))

(defn unused-binding-excluded-config [config]
  (let [excluded (get-in config [:linters :unused-binding :exclude-patterns])
        regexes (filter string? excluded)]
    {:regexes regexes}))

(defn unused-binding-excluded? [ctx config binding-sym]
  (let [{:keys [regexes]} config
        re-find (:re-find-memo ctx)
        binding-str (str binding-sym)]
    (boolean (some (fn [regex]
                     (re-find regex binding-str)) regexes))))

(defn used-underscored-binding-excluded-config [config]
  (let [excluded (get-in config [:linters :used-underscored-binding :exclude])
        syms (set (filter symbol? excluded))
        regexes (filter string? excluded)]
    {:syms syms :regexes regexes}))

(defn used-underscored-binding-excluded? [ctx config binding-sym]
  (let [re-find (:re-find-memo ctx)
        {:keys [syms regexes]} config]
    (or (contains? syms binding-sym)
        (let [binding-str (str binding-sym)]
          (boolean (some #(re-find % binding-str) regexes))))))

(defn ns-groups [ctx config ns-name filename]
  (let [ns-groups-matcher (:re-find-memo ctx)]
    (keep (fn [{:keys [pattern
                       filename-pattern
                       name]}]
            (when (or (and (string? pattern) (symbol? name)
                           (ns-groups-matcher pattern (str ns-name)))
                      (and (string? filename-pattern) (symbol? name)
                           (ns-groups-matcher filename-pattern filename)))
              name))
          (:ns-groups config))))

(defn unquote [coll]
  (walk/postwalk
   (fn [x]
     (if (and (seq? x)
              (= 'quote (first x)))
       (second x)
       x))
   coll))

(defn deprecated-namespace-excluded-config [config ]
  (let [excluded (get-in config [:linters :deprecated-namespace :exclude])]
    (set excluded)))

(defn deprecated-namespace-excluded? [config required]
  (contains? config required))

;;;; Scratch

;; (comment
;;   (ns-group {} 'foo.bar)
;;   (ns-group {:ns-groups [{:regex "nubank\\..*\\.service" :name 'nubank.service-group}]}
;;             'nubank.awesome.service) ;; nubank.service-group
;;
;;   (re-matches (re-pattern ".*") "foo.bar")

;;   )
