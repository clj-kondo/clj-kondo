(ns compojure.compojure)

(defmacro defroutes
  "Define a Ring handler function from a sequence of routes. The name may
  optionally be followed by a doc-string and metadata map."
  [name & routes]
  (let [[name routes] (macro/name-with-attributes name routes)]
    `(def ~name (routes ~@routes))))

(defmacro GET "Generate a `GET` route."
  [path args & body]
  (foo/compile-route :get path args body))

(defmacro POST "Generate a `POST` route."
  [path args & body]
  (foo/compile-route :post path args body))
