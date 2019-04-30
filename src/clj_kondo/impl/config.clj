(ns clj-kondo.impl.config
  {:no-doc true}
  (:require [clj-kondo.impl.profiler :as profiler]
            [clj-kondo.impl.utils :refer [vconj]]))

(defonce config (atom nil))

(defn set-config! [cfg]
  (let [cfg (cond-> cfg
              (:skip-comments cfg)
              (-> (update :skip-args vconj 'clojure.core/comment 'cljs.core/comment)))]
    (reset! config cfg)))

(defn fq-syms->vecs [fq-syms]
  (map (fn [fq-sym]
         [(symbol (namespace fq-sym)) (symbol (name fq-sym))])
       fq-syms))

(defn skip-args*
  ([]
   (fq-syms->vecs (get @config :skip-args)))
  ([linter]
   (fq-syms->vecs (get-in @config [:linters linter :skip-args]))))

(def skip-args (memoize skip-args*))

(defn skip?
  "we optimize for the case that disable-within returns an empty sequence"
  ([parents]
   (profiler/profile
    :disabled?
    (when-let [disabled (seq (skip-args))]
      (some (fn [disabled-sym]
              (some #(= disabled-sym %) parents))
            disabled))))
  ([linter parents]
   (profiler/profile
    :disabled?
    (when-let [disabled (seq (skip-args linter))]
      (some (fn [disabled-sym]
              (some #(= disabled-sym %) parents))
            disabled)))))

;;;; Scratch

(comment
  (reset! config (clojure.edn/read-string (slurp ".clj-kondo/config.edn")))
  )
