(ns demo.direct
  (:require [demo.macros :as macros :refer [defvalue]]))

(macros/defvalue aliased 1)
aliased
(defvalue referred 1)
referred
