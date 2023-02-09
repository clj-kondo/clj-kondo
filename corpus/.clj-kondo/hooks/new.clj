(ns hooks.new
  (:require [clj-kondo.hooks-api :as api]))

(defn new [{:keys [node]}]
  (api/reg-finding! (assoc (meta node)
                           :message (str "Interop is no good! " (api/generated-node? (first (:children node))))
                           :type :interop)))

(defmacro new-macroexpand [& body]
  `(do ~body))
