(ns baz
  {:clj-kondo/config '{:hooks {:analyze-call {state-flow.cljtest/defflow hooks.state-flow/defflow}}}}
  (:require [state-flow.cljtest :refer [defflow]]))

(defflow my-flow)
