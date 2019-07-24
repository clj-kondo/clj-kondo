(ns ^{:clj-kondo/config '{:lint-as {compojure.compojure/defroutes clojure.core/def}
                          :linters {:unresolved-symbol {:exclude [(compojure.compojure/GET)]}}}}
    compojure.consumer
  (:require [compojure.compojure :refer :all]))

(defroutes) ;; <- warning
(GET)
(POST)

(defroutes app
  (GET "/" [] "<h1>Hello World</h1>")
  (GET "/user" {{:keys [user-id]} :session}
       (str "The current user is " user-id))
  (route/not-found "<h1>Page not found</h1>"))
