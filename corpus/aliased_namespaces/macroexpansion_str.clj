(require '[clj-kondo.hooks-api :as api])

(defmacro new-> [x f]
  (list 'clojure.core/-> x f))
