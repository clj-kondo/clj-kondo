(ns clj-kondo.impl.version
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def version
  (str/trim
   (slurp (io/resource "CLJ_KONDO_VERSION"))))
