(ns compojure.consumer
  ;; namespace local config
  (:require [compojure.core :refer :all] ;; <- refer :all...
            [compojure.handler :as handler]))

(defroutes) ;; <- arity warning
(GET) ;; <- arity warning
(POST) ;; <- arity warning

(defroutes app-routes
  (GET "/" [] "<h1>Hello World</h1>")
  (GET "/user" {{:keys [user-id]} :session} ;; <- no unresolved symbols due to config
       (str "The current user is " user-id) ;; <- no unresolved symbols due to config
       x)) ;; <- this symbol is still unresolved

(def app
  (handler/site app-routes))  ;; <- defroutes is linted as def, so clj-kondo
                              ;; knows app-routes is a var


