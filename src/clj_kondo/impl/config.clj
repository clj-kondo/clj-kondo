(ns clj-kondo.impl.config
  {:no-doc true}
  (:require
    [clj-kondo.impl.profiler :as profiler]
    [clj-kondo.impl.utils :refer [vconj deep-merge map-vals]]))

(def default-config
  '{;; no linting inside calls to these functions/macros
    ;; note that you will still get an arity error when calling the fn/macro itself incorrectly
    :skip-args [#_clojure.core/comment #_cljs.core/comment]
    :skip-comments false ;; convenient shorthand for :skip-args [clojure.core/comment cljs.core/comment]
    ;; linter level can be tweaked by setting :level to :error, :warn or :info (or any other keyword)
    ;; all linters are enabled by default, but can be turned off by setting :level to :off.
    :linters {:invalid-arity {:level :error
                              :skip-args [#_riemann.test/test-stream]}
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
              :constant-test-assertion {:level :warning}
              :duplicate-map-key {:level :error}
              :duplicate-set-key {:level :error}
              :missing-map-value {:level :error}
              :redefined-var {:level :warning}
              :unreachable-code {:level :warning}
              :datalog-syntax {:level :error}
              :unbound-destructuring-default {:level :warning}
              :unused-binding {:level :warning
                               :exclude-destructured-keys-in-fn-args false}
              :unsorted-required-namespaces {:level :off}
              :unused-namespace {:level :warning
                                 ;; don't warn about these namespaces:
                                 :exclude [#_clj-kondo.impl.var-info-gen]
                                 ;; :simple-libspec true
                                 }
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
              :unresolved-namespace {:level :warning}
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
              :refer {:level :off}
              :refer-all {:level :warning
                          :exclude #{}}
              :use {:level :warning}
              :missing-else-branch {:level :warning}
              :type-mismatch {:level :error}
              :missing-docstring {:level :off}
              :consistent-alias {:level :warning
                                 ;; warn when alias for clojure.string is
                                 ;; different from str
                                 :aliases {#_clojure.string #_str}}
              :unused-import {:level :warning}
              :single-operand-comparison {:level :warning}
              :single-key-in {:level :off}
              :missing-clause-in-try {:level :warning}
              :missing-body-in-when {:level :warning}}
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
    :output {:format :text ;; or :edn
             :summary true ;; outputs summary at end, only applicable to output :text
             ;; outputs analyzed var definitions and usages of them
             :analysis false
             ;; set to truthy to print progress while linting, only applicable to output :text
             :progress false
             ;; output can be filtered and removed by regex on filename. empty options leave the output untouched.
             :include-files [] #_["^src" "^test"]
             :exclude-files [] #_["^cljs/core"]
             ;; the output pattern can be altered using a template. use {{LEVEL}} to print the level in capitals.
             ;; the default template looks like this:
             ;; :pattern "{{filename}}:{{row}}:{{col}}: {{level}}: {{message}}"
             :canonical-paths false ;; set to true to see absolute file paths and jar files
             }})

(defn merge-config! [cfg* cfg]
  (if (empty? cfg) cfg*
      (let [cfg (cond-> cfg
                        (:skip-comments cfg)
                        (-> (update :skip-args vconj 'clojure.core/comment 'cljs.core/comment))

                        (contains? (:linters cfg) :if) (assoc-in [:linters :missing-else-branch] (:if (:linters cfg))))]
        (if (:replace (meta cfg))
          cfg
          (deep-merge cfg* cfg)))))

(defn fq-syms->vecs [fq-syms]
  (map (fn [fq-sym]
         (if-let [ns* (namespace fq-sym)]
           [(symbol ns*) (symbol (name fq-sym))]
           (throw (ex-info (str "Configuration error. Expected fully qualified symbol, got: " fq-sym)
                           {:type :clj-kondo/config}))))
       fq-syms))

(defn skip-args*
  ([config]
   (fq-syms->vecs (get config :skip-args)))
  ([config linter]
   (fq-syms->vecs (get-in config [:linters linter :skip-args]))))

(def skip-args (memoize skip-args*))

(defn skip?
  "we optimize for the case that disable-within returns an empty sequence"
  ([config callstack]
   (profiler/profile
    :disabled?
    (when-let [disabled (seq (skip-args config))]
      (some (fn [disabled-sym]
              (some #(= disabled-sym %) callstack))
            disabled))))
  ([config linter callstack]
   (profiler/profile
    :disabled?
    (when-let [disabled (seq (skip-args config linter))]
      (some (fn [disabled-sym]
              (some #(= disabled-sym %) callstack))
            disabled)))))

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
        (when-let [vars (get excluded ns-sym )]
          (contains? vars var-sym))))))

(def unresolved-symbol-excluded
  (let [delayed-cfg
        (fn [config]
          (let [excluded (get-in config [:linters :unresolved-symbol :exclude])
                syms (set (filter symbol? excluded))
                calls (filter list? excluded)]
            {:excluded syms
             :excluded-in
             (reduce (fn [acc [fq-name excluded]]
                       (let [ns-name (symbol (namespace fq-name))
                             var-name (symbol (name fq-name))]
                         (update acc [ns-name var-name]
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
                     (check-fn sym))
                  callstack))))))

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
    (fn [config ns-name var-name]
      (contains? (delayed-cfg config) [ns-name var-name]))))

(def refer-all-excluded?
  (let [delayed-cfg (fn [config]
                      (let [syms (get-in config [:linters :refer-all :exclude])]
                        (set syms)))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config referred-all-ns]
      (let [excluded (delayed-cfg config)]
        (contains? excluded referred-all-ns)))))

;;;; Scratch

(comment
  )
