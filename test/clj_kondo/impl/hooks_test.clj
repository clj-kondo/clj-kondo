(ns clj-kondo.impl.hooks-test
  (:require
   [clj-kondo.impl.hooks :as hooks-api]
   [clj-kondo.impl.utils :as utils :refer [parse-string]]
   [clojure.test :as t :refer [deftest is]]))

(deftest predicates-test
  (is (hooks-api/keyword-node? (parse-string ":foo")))
  (is (hooks-api/string-node? (parse-string "\"hello\"")))
  (is (hooks-api/string-node? (parse-string "\"hello
there
\"")))
  (is (hooks-api/token-node? (parse-string "foo")))
  (is (hooks-api/token-node? (parse-string "nil")))
  (is (hooks-api/token-node? (parse-string "1")))
  (is (hooks-api/vector-node? (parse-string "[1]")))
  (is (hooks-api/list-node? (parse-string "(+ 1 2 3)"))))
