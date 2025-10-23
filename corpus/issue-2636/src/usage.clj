(ns usage
  (:require [utils]))

(utils/if-bb
 #_{:clj-kondo/ignore [:unresolved-namespace]}
 [foo/bar] [])
