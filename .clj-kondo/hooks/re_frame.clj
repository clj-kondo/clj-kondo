(ns hooks.re-frame
  (:require [clj-kondo.hooks-api :as api]))

(defn dispatch [{:keys [:node]}]
  (let [sexpr (api/sexpr node)
        event (second sexpr)]

    (when-not (vector? event)
      (throw (ex-info "dispatch arg should be vector!"
                      (or (meta (second (:children node))) {}))))

    (when-not (qualified-keyword? (first event))
      (let [{:keys [:row :col]} (some-> node :children second :children first meta)]
        (api/reg-finding! {:message "keyword should be fully qualified!"
                           :type :re-frame/keyword
                           :row row
                           :col col})))))
