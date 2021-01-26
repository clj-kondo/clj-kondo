(ns clj-kondo.impl.analyzer.match
  (:require [clj-kondo.impl.analyzer.common :as common]))

(defn analyze-match [ctx expr]
  (common/analyze-children ctx expr))
