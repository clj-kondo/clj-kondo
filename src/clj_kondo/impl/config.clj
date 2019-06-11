(ns clj-kondo.impl.config
  {:no-doc true}
  (:require [clj-kondo.impl.profiler :as profiler]
            [clj-kondo.impl.utils :refer [vconj deep-merge]]))

(def default-config
  '{;; no linting inside calls to these functions/macros
    ;; note that you will still get an arity error when calling the fn/macro itself incorrectly
    :skip-args [#_clojure.core/comment #_cljs.core/comment]
    :skip-comments false ;; convient shorthand for :skip-args [clojure.core/comment cljs.core/comment]
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
              :missing-test-assertion {:level :warning}
              :duplicate-map-key {:level :error}
              :duplicate-set-key {:level :error}
              :missing-map-value {:level :error}
              :redefined-var {:level :warning}
              :unreachable-code {:level :warning}
              :unbound-destructuring-default {:level :warning}
              :unused-binding {:level :warning}
              :unused-namespace {:level :warning
                                 ;; don't warn about these namespaces:
                                 :exclude [#_clj-kondo.impl.var-info-gen]}
              :unresolved-symbol {:level :error
                                  :exclude []}}
    :lint-as {cats.core/->= clojure.core/->
              cats.core/->>= clojure.core/->>
              rewrite-clj.custom-zipper.core/defn-switchable clojure.core/defn
              clojure.core.async/go-loop clojure.core/loop
              cljs.core.async/go-loop clojure.core/loop
              cljs.core.async.macros/go-loop clojure.core/loop}
    :output {:format :text ;; or :edn
             :summary true ;; prints summary at end, only applicable to output :text
             ;; set to truthy to print progress while linting, only applicable to output :text
             :progress false
             ;; output can be filtered and removed by regex on filename. empty options leave the output untouched.
             :include-files [] #_["^src" "^test"]
             :exclude-files [] #_["^cljs/core"]
             ;; the output pattern can be altered using a template. use {{LEVEL}} to print the level in capitals.
             ;; the default template looks like this:
             ;; :pattern "{{filename}}:{{row}}:{{col}}: {{level}}: {{message}}"
             }})

(defn merge-config! [cfg* cfg]
  (if (empty? cfg) cfg*
    (let [cfg (cond-> cfg
                (:skip-comments cfg)
                (-> (update :skip-args vconj 'clojure.core/comment 'cljs.core/comment)))]
      (if (:replace (meta cfg))
        cfg
        (deep-merge cfg* cfg)))))

(defn fq-syms->vecs [fq-syms]
  (map (fn [fq-sym]
         [(symbol (namespace fq-sym)) (symbol (name fq-sym))])
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

(defn lint-as [config v] (get (lint-as-config config) v))

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

(def unresolved-symbol-excluded
  (let [delayed-cfg (fn [config]
                      (let [excluded (get-in config [:linters :unresolved-symbol :exclude])
                            syms (set excluded)]
                        syms))
        delayed-cfg (memoize delayed-cfg)]
    (fn [config sym]
      (let [syms (delayed-cfg config)]
        (contains? syms sym)))))

;;;; Scratch

(comment
  )
