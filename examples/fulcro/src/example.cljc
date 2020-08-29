(ns example
  (:require
   #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
      :clj  [com.fulcrologic.fulcro.dom-server :as dom])
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]))

(defsc DestructuredExample [this
                            {:keys [db/id] :as props}
                            {:keys [onClick] :as computed :or {onClick identity}}]
  {:query         [:db/id]
   :initial-state {:db/id 22}}
  (dom/div
   (str "Component: " id)))
