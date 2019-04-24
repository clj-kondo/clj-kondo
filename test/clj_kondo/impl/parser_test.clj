(ns clj-kondo.impl.parser-test
  (:require [clj-kondo.impl.parser :as parser]
            [clj-kondo.impl.utils :as utils]
            [clojure.test :as t :refer [deftest is testing]]
            [rewrite-clj.node.protocols :as node]))

(deftest omit-unevals-test
  (is (zero? (node/length (utils/parse-string-all "#_#_1 2")))))

