(ns clj-kondo.impl.utils-test
  (:require [clj-kondo.impl.utils :as utils :refer [constant? parse-string]]
            [clojure.test :as t :refer [deftest is testing]]))

(deftest constant-test
  (is (constant? (parse-string "{:a 1 'x 2}")))
  (is (constant? (parse-string "'{:a 1 x 2}")))
  (is (constant? (parse-string "1")))
  (is (constant? (parse-string "\"foo\"")))
  (is (constant? (parse-string "\\x")))
  (is (constant? (parse-string "true")))
  (is (constant? (parse-string ":k")))
  (is (constant? (parse-string "::k/foo")))
  (is (false? (constant? (parse-string "x"))))
  (is (false? (constant? (parse-string "(java.util.HashMap. {})"))))
  (is (false? (constant? (parse-string "{:a 1 x 2}")))))

;;;; Scratch

(comment
  (t/run-tests)
  )
