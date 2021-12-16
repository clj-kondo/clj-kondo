(ns app.core)

(defn foo [_x]) ;; 1 arg

(ns app.api
  (:require
   [clojure.walk]
   [clojure.data]
   [app.core]
   [potemkin :refer [import-vars]]))

(import-vars
 [clojure.walk
  prewalk
  postwalk]
 [clojure.data
  diff]
 [app.core foo])

(ns consumer
  (:require [app.api :refer [prewalk foo]]))

(prewalk) ;; wrong arity for clojure.walk/prewalk
(foo) ;; wrong arity for app.core/foo
