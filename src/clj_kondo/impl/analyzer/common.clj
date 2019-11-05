(ns clj-kondo.impl.analyzer.common
  {:no-doc true})

(defonce common (volatile! {}))

(defn analyze-expression** [ctx expr]
  ((get @common 'analyze-expression**) ctx expr))

(defn extract-bindings [ctx expr]
  ((get @common 'extract-bindings) ctx expr))

(defn ctx-with-bindings [ctx expr]
  ((get @common 'ctx-with-bindings) ctx expr))

(defn analyze-children [ctx expr]
  ((get @common 'analyze-children) ctx expr))
