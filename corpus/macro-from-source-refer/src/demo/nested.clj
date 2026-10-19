(ns demo.nested
  (:require [demo.api :as api]
            [demo.macros]))

(api/make-defvalue-referred)
(defvalue-referred referred)
referred

(api/make-defvalue-aliased)
(defvalue-aliased aliased)
aliased
