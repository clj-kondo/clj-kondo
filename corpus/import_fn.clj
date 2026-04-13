(ns app.impl)

(defn my-fn [_x _y]) ;; 2 args

(defmacro my-macro [_body])

(def my-var 42)

(ns app.api
  (:require
   [app.impl]
   [potemkin :refer [import-fn import-macro import-def]]))

(import-fn app.impl/my-fn)
(import-macro app.impl/my-macro)
(import-def app.impl/my-var)

;; with rename
(import-fn app.impl/my-fn renamed-fn)

(ns consumer
  (:require [app.api :refer [my-fn my-macro my-var renamed-fn]]))

(my-fn 1) ;; wrong arity for app.impl/my-fn (expects 2)
(my-fn 1 2) ;; ok
(renamed-fn 1) ;; wrong arity for app.impl/my-fn (expects 2)
(renamed-fn 1 2) ;; ok
my-var ;; ok
