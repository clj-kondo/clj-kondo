(ns schema.defmethod
  (:require
   [schema.core :as sc]
   [integrant.core :as ig]))

;; no false positives from this:

(sc/defmethod ig/init-key :config :- {:config/env sc/Keyword}
  [_
   {:keys [:config/env]} :- {:config/env sc/Keyword}]
  {:config/env env})
