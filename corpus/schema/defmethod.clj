(ns schema.defmethod
  (:require
   [integrant.core :as ig]
   [schema.core :as sc]))

;; no false positives from this:

(sc/defmethod ig/init-key :config :- {:config/env sc/Keyword}
  [_
   {:keys [:config/env]} :- {:config/env sc/Keyword}]
  {:config/env env})

;; Testing when dispatch-val is vector
(sc/defmethod ig/init-key [:config1 :config2] :- {:config/env sc/Keyword}
  [_
   {:keys [:config/env]} :- {:config/env sc/Keyword}]
  {:config/env env})

;; Testing with multiple arities
(sc/defmethod ig/init-key [:config1 :config2] :- {:config/env sc/Keyword}
  ([_
    {:keys [:config/env]} :- {:config/env sc/Keyword}]
   {:config/env env})
  ([_ :- sc/Str
    _ :- sc/Int
    {:keys [:config/env]} :- {:config/env sc/Keyword}]
   {:config/env env}))
