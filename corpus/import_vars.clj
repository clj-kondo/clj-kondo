(ns app.api
  (:require [potemkin :refer [import-vars]]))

(import-vars
 [clojure.walk
  prewalk
  postwalk]
 [clojure.data
  diff])

(ns consumer
  (:require [app.api :refer [prewalk]]))

(prewalk) ;; wrong arity for clojure.walk/prewalk
