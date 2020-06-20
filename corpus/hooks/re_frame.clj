(ns bar
  {:clj-kondo/config '{:linters {:re-frame/keyword {:level :warning}}
                       :hooks {:analyze-call {re-frame.core/dispatch hooks.re-frame/dispatch}}}}
  (:require [re-frame.core :as r :refer [dispatch]]))

(dispatch 1)
(dispatch [:foo 1])
