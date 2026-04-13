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

(ns app.api2
  (:require
   [clojure.walk]
   [clojure.data]
   [app.core]
   [potemkin :refer [import-vars]]))

(import-vars
 [clojure.walk :refer [prewalk postwalk]]
 [clojure.data :refer [diff]]
 [app.core :refer [foo]])

(ns app.api3
  (:require
   [app.core]
   [potemkin :refer [import-vars]]))

(import-vars
 [app.core :refer [foo] :rename {foo my-foo}])

(ns consumer
  (:require [app.api :refer [prewalk foo]]
            [app.api2 :as api2]
            [app.api3 :as api3]))

(prewalk) ;; wrong arity for clojure.walk/prewalk
(foo) ;; wrong arity for app.core/foo
(api2/prewalk) ;; wrong arity for clojure.walk/prewalk
(api2/foo) ;; wrong arity for app.core/foo
(api3/my-foo) ;; wrong arity for app.core/foo
