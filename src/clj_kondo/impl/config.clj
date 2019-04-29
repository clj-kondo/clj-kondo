(ns clj-kondo.impl.config
  {:no-doc true})

(defonce config (atom nil))

(defn fq-syms->vecs [fq-syms]
  (set (map (fn [fq-sym]
              [(symbol (namespace fq-sym)) (symbol (name fq-sym))])
            fq-syms)))

(defn disable-within*
  ([]
   (fq-syms->vecs (get @config :disable-within)))
  ([linter]
   (fq-syms->vecs (get-in @config [:linters linter :disable-within]))))

(def disable-within (memoize disable-within*))

#_(println "DISABLE WITHIN" (disable-within))

(defn disabled?
  ([parents]
   (some #(contains? (disable-within) %) parents))
  ([linter parents]
   (some #(contains? (disable-within linter) %) parents)))

;;;; Scratch

(comment
  (reset! config (clojure.edn/read-string (slurp ".clj-kondo/config.edn")))
  (disable-within)
  (disable-within :invalid-arity)
  (disabled? :invalid-arity '[[riemann.test test-stream] [foo bar]])
  )
