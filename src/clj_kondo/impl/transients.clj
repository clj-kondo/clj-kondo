(ns clj-kondo.impl.transients
  {:no-doc true}
  (:require [clj-kondo.impl.findings :as findings]
            [clj-kondo.impl.utils :refer [unused-expr?]]))

(defn lint-unused-transient! [ctx expr]
  (when (unused-expr? ctx expr true)
    (findings/reg-finding! ctx (assoc (meta expr)
                                      :level :warning
                                      :type :unused-transient
                                      :message (str "Unused transient")
                                      :filename (:filename ctx)))))
