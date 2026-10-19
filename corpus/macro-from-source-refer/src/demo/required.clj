(ns demo.required
  (:require [demo.api :as api]
            [demo.macros]))

(api/defvalue-aliased aliased)
aliased
(api/defvalue-referred referred)
referred
