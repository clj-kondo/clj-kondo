(ns clj-kondo.impl.config
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [deep-merge map-vals]]
   [clojure.set :as set]))

(set! *warn-on-reflection* true)

(def default-config
  '{;; no linting inside calls to these functions/macros
    ;; note that you will still get an arity error when calling the fn/macro itself incorrectly
    :skip-args [#_clojure.core/comment #_cljs.core/comment]
    :skip-comments false ;; convenient shorthand for :skip-args [clojure.core/comment cljs.core/comment]
    ;; linter level can be tweaked by setting :level to :error, :warn or :info (or any other keyword)
    ;; all linters are enabled by default, but can be turned off by setting :level to :off.
    ;; :config-in-comment {} config override for comment blocks
    :linters {:invalid-arity {:level :error
                              :skip-args [#_riemann.test/test-stream]}
              :conflicting-fn-arity {:level :error}
              :not-a-function {:level :error
                               :skip-args [#_user/foo]}
              :private-call {:level :error}
              :inline-def {:level :warning}
              :redundant-do {:level :warning}
              :redundant-let {:level :warning}
              :cond-else {:level :warning}
              :syntax {:level :error}
              :file {:level :error}
              :missing-test-assertion {:level :warning}
              :conflicting-alias {:level :error}
              :duplicate-map-key {:level :error}
              :duplicate-set-key {:level :error}
              :missing-map-value {:level :error}
              :redefined-var {:level :warning}
              :unreachable-code {:level :warning}
              :datalog-syntax {:level :error}
              :unbound-destructuring-default {:level :warning}
              :used-underscored-binding {:level :off}
              :unused-binding {:level :warning
                               :exclude-destructured-keys-in-fn-args false
                               :exclude-destructured-as false
                               :exclude-defmulti-args false
                               ,}
              :unsorted-required-namespaces {:level :off}
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
                                            (user/defproject) ;; ignore project.clj's defproject
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
              :unused-referred-var {:level :warning
                                    :exclude {#_#_taoensso.timbre [debug]}}
              :unused-private-var {:level :warning}
              :duplicate-require {:level :warning}
              :refer {:level :off
                      #_:exclude
                      #_[clojure.test]
                      }
              :refer-all {:level :warning
                          :exclude #{}}
              :use {:level :warning}
              :missing-else-branch {:level :warning}
              :duplicate-case-test-constant {:level :error}
              :quoted-case-test-constant {:level :warning}
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
              :single-operand-comparison {:level :warning}
              :single-logical-operand {:level :warning}
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
              :redundant-call {:level :off
                               #_#_:exclude #{clojure.core/->}
                               #_#_:include #{clojure.core/conj!}}
              :warn-on-reflection {:level :off
                                   :warn-only-on-interop true}}
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
             ;; if below :show-rule-name-in-message is set to true, type (linter name) of reported the finding
             ;; is appended to the end of the default pattern as " [{{type}}]"
             :show-rule-name-in-message false
             :canonical-paths false}}) ;; set to true to see absolute file paths and jar files

(defn merge-config!
  ([])
  ([cfg] cfg)
  ([cfg* cfg]
   (if (empty? cfg) cfg*
       (let [cfg (cond-> cfg
                   (contains? (:linters cfg) :if)
                   (assoc-in [:linters :missing-else-branch] (:if (:linters cfg))))]
         (deep-merge cfg* cfg)))))

(defn fq-sym->vec [fq-sym]
  (if-let [ns* (namespace fq-sym)]
    [(symbol ns*) (symbol (name fq-sym))]
    (throw (ex-info (str "Configuration error. Expected fully qualified symbol, got: " fq-sym)
                    {:type :clj-kondo/config}))))

(defn fq-syms->vecs
  ([fq-syms]
   (map (fn [fq-sym]
          (if-let [ns* (namespace fq-sym)]
            [(symbol ns*) (symbol (name fq-sym))]
            (throw (ex-info (str "Configuration error. Expected fully qualified symbol, got: " fq-sym)
                            {:type :clj-kondo/config}))))
        fq-syms)))

(defn skip-args*
  ([config linter]
   (fq-syms->vecs (get-in config [:linters linter :skip-args]))))

(def skip-args (memoize skip-args*))

(defn skip?
  "Used by invalid-arity linter. We optimize for the case that disable-within returns an empty sequence"
  ([config linter callstack]
   (when-let [disabled (seq (skip-args config linter))]
     (some (fn [disabled-sym]
             (some #(= disabled-sym %) callstack))
           disabled))))

(defn lint-as-config* [config]
  (let [m (get config :lint-as)]
    (zipmap (fq-syms->vecs (keys m))
            (fq-syms->vecs (vals m)))))

(def lint-as-config (memoize lint-as-config*))

(defn lint-as [config v]
  (get (lint-as-config config) v))

(def unused-namespace-excluded
  (let [delayed-cfg (fn [config]
                      (let [excluded (get-in config [:linters :unused-namespace :exclude])
                            syms (set (filter symbol? excluded))
                            regexes (map re-pattern (filter string? excluded))]
                        {:syms syms :regexes regexes}))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config ns-sym]
      (let [{:keys [:syms :regexes]} (delayed-cfg config)]
        (or (contains? syms ns-sym)
            (let [ns-str (str ns-sym)]
              (boolean (some #(re-find % ns-str) regexes))))))))

(def unused-referred-var-excluded
  (let [delayed-cfg (fn [config]
                      (let [excluded (get-in config [:linters :unused-referred-var :exclude])]
                        (map-vals set excluded)))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config ns-sym var-sym]
      (let [excluded (delayed-cfg config)]
        (when-let [vars (get excluded ns-sym)]
          (contains? vars var-sym))))))

(def unresolved-namespace-excluded
  (let [delayed-cfg (fn [config]
                      (set (get-in config [:linters :unresolved-namespace :exclude])))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config ns-sym]
      (let [excluded (delayed-cfg config)]
        (contains? excluded ns-sym)))))

(def unresolved-symbol-excluded
  (let [delayed-cfg
        (fn [config]
          (let [excluded (get-in config [:linters :unresolved-symbol :exclude])
                syms (set (filter symbol? excluded))
                calls (filter list? excluded)]
            {:excluded syms
             :excluded-in
             (reduce (fn [acc [fq-name excluded]]
                       (let [ns-nm (symbol (namespace fq-name))
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
                                         :else identity)))))
                     {} calls)}))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config callstack sym]
      (let [{:keys [:excluded :excluded-in]} (delayed-cfg config)]
        (or (contains? excluded sym)
            (some #(when-let [check-fn (get excluded-in %)]
                     ;; e.g. for user/defproject, check-fn is identity, so any
                     ;; truthy value will be excluded inside of that
                     (check-fn sym))
                  callstack))))))

(def unresolved-var-excluded
  (let [delayed-cfg
        (fn [config]
          (let [excluded (get-in config [:linters :unresolved-var :exclude])
                vars (into #{} (comp (filter qualified-symbol?)
                                     (map fq-sym->vec))
                           excluded)
                nss (into #{} (filter simple-symbol?)
                          excluded)]
            {:excluded-vars vars
             :excluded-nss nss}))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config ns-sym fn-sym]
      (let [cfg (delayed-cfg config)]
        (or (contains? (:excluded-nss cfg) ns-sym)
            (contains? (:excluded-vars cfg) [ns-sym fn-sym]))))))

(def deprecated-var-excluded
  (let [delayed-cfg (fn [config var-sym]
                      (let [excluded (get-in config [:linters :deprecated-var :exclude var-sym])
                            namespaces (:namespaces excluded)
                            namespace-regexes (map re-pattern (filter string? namespaces))
                            namespace-syms (set (filter symbol? namespaces))
                            defs (:defs excluded)
                            def-regexes (map re-pattern (filter string? defs))
                            def-syms (set (filter symbol? defs))]
                        {:namespace-regexes namespace-regexes
                         :namespace-syms namespace-syms
                         :def-regexes def-regexes
                         :def-syms def-syms}))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config var-sym excluded-ns excluded-in-def]
      (let [{:keys [:namespace-regexes :namespace-syms
                    :def-regexes :def-syms]} (delayed-cfg config var-sym)]
        (or (when excluded-in-def
              (let [excluded-in-def (symbol (str excluded-ns) (str excluded-in-def))]
                (or (contains? def-syms excluded-in-def)
                    (let [excluded-in-def-str (str excluded-in-def)]
                      (boolean (some #(re-find % excluded-in-def-str) def-regexes))))))
            (contains? namespace-syms excluded-ns)
            (let [ns-str (str excluded-ns)]
              (boolean (some #(re-find % ns-str) namespace-regexes))))))))

(def type-mismatch-config
  (let [delayed-cfg
        (fn [config var-ns var-name]
          ;; (prn (get-in config [:linters :type-mismatch :namespaces 'foo 'foo]))
          (get-in config [:linters :type-mismatch :namespaces var-ns var-name]))
        delayed-cfg (memoize delayed-cfg)]
    delayed-cfg))

(def unused-private-var-excluded
  (let [delayed-cfg
        (fn [config]
          (let [syms (get-in config [:linters :unused-private-var :exclude])
                vecs (fq-syms->vecs syms)]
            (set vecs)))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config ns-nm var-name]
      (contains? (delayed-cfg config) [ns-nm var-name]))))

(def refer-excluded?
  (let [delayed-cfg (fn [config]
                      (let [syms (get-in config [:linters :refer :exclude])]
                        (set syms)))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config referred-ns]
      (let [excluded (delayed-cfg config)]
        (contains? excluded referred-ns)))))

(def refer-all-excluded?
  (let [delayed-cfg (fn [config]
                      (let [syms (get-in config [:linters :refer-all :exclude])]
                        (set syms)))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config referred-all-ns]
      (let [excluded (delayed-cfg config)]
        (contains? excluded referred-all-ns)))))

(def shadowed-var-excluded?
  (let [delayed-cfg (fn [config]
                      (let [cfg (get-in config [:linters :shadowed-var])
                            exclude (some-> (:exclude cfg) set)
                            include (some-> (:include cfg) set)]
                        (cond-> nil
                          exclude (assoc :exclude exclude)
                          include (assoc :include include))))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config sym]
      (when-let [cfg (delayed-cfg config)]
        (let [{:keys [:exclude :include]} cfg]
          (if include
            (not (contains? include sym))
            (or (not exclude)
                (contains? exclude sym))))))))

(def reduce-without-init-excluded?
  (let [delayed-cfg (fn [config]
                      (let [cfg (get-in config [:linters :reduce-without-init])
                            exclude (some-> (:exclude cfg) set)
                            #_#_include (some-> (:include cfg) set)]
                        (cond-> nil
                          exclude (assoc :exclude exclude)
                          #_#_include (assoc :include include))))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config sym]
      (when-let [cfg (delayed-cfg config)]
        (let [{:keys [:exclude #_:include]} cfg]
          (or (not exclude)
              (contains? exclude sym)))))))

(def redundant-call-included?
  (let [redundant-call-vars '#{clojure.core/-> cljs.core/->
                               clojure.core/->> cljs.core/->>
                               clojure.core/cond-> cljs.core/cond->
                               clojure.core/cond->> cljs.core/cond->>
                               clojure.core/some-> cljs.core/some->
                               clojure.core/some->> cljs.core/some->>
                               clojure.core/partial cljs.core/partial
                               clojure.core/comp cljs.core/comp
                               clojure.core/merge cljs.core/merge}
        delayed-cfg (fn [config]
                      (let [cfg (get-in config [:linters :redundant-call])
                            include (some-> (:include cfg) set)
                            exclude (some-> (:exclude cfg) set)]
                        (-> redundant-call-vars
                            (set/union include)
                            (set/difference exclude))))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config sym]
      (contains? (delayed-cfg config) sym))))

(defn ns-group* [config ns-name]
  (or (some (fn [{:keys [pattern
                         name]}]
              (when (and (string? pattern) (symbol? name)
                         (re-matches (re-pattern pattern) (str ns-name)))
                name))
            (:ns-groups config))
      ns-name))

(def ns-group (memoize ns-group*))

;; (defn ns-group-1 [m full-ns-name]
;;   (when-let [r (:regex m)]
;;     (if (re-matches (re-pattern r) (str full-ns-name))
;;       (:name m)
;;       full-ns-name)))

;; (def ns-group
;;   (let [delayed-cfg (fn [config]
;;                       (let [group-cfg (:ns-groups config)]
;;                         (fn [full-ns-name]
;;                           (or (some #(ns-group-1 % full-ns-name) group-cfg)
;;                               full-ns-name))))
;;         delayed-cfg-fn (memoize delayed-cfg)]
;;     (fn [config sym]
;;       (if-let [cfg-fn (delayed-cfg-fn config)]
;;         (cfg-fn sym)
;;         sym))))

;;;; Scratch

;; (comment
;;   (ns-group {} 'foo.bar)
;;   (ns-group {:ns-groups [{:regex "nubank\\..*\\.service" :name 'nubank.service-group}]}
;;             'nubank.awesome.service) ;; nubank.service-group
;;
;;   (re-matches (re-pattern ".*") "foo.bar")

;;   )
