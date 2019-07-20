(ns clj-kondo.impl.parser-test
  (:require [clj-kondo.impl.parser :as parser :refer [parse-string]]
            [clj-kondo.impl.utils :as utils]
            [clojure.test :as t :refer [deftest is]]))

(deftest omit-unevals-test
  (is (zero? (count (:children (parse-string "#_#_1 2"))))))

(deftest namespaced-maps-test
  (is (= '#:it{:a 1} (utils/sexpr (utils/parse-string "#::it {:a 1}"))))
  (is (= '#:it{:a #:it{:a 1}} (utils/sexpr (utils/parse-string "#::it {:a #::it{:a 1}}"))))
  (is (= '#:__current-ns__{:a 1} (utils/sexpr (utils/parse-string "#::{:a 1}")))))

;;;; Scratch

(comment
  )
