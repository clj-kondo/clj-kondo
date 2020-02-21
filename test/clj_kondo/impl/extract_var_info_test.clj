(ns clj-kondo.impl.extract-var-info-test
  (:require
    [clj-kondo.impl.extract-var-info :as extract-var-info]
    [clojure.test :refer [deftest is]]))

(deftest eastwood-var-info-test
  (let [var-info (extract-var-info/eastwood-var-info)]
    (is (contains? var-info 'clojure.core/*out*))))

(deftest extract-clojure-core-vars-test
  (let [vars (extract-var-info/extract-clojure-core-vars)]
    (is (contains? vars 'future))
    (is (contains? vars 'transduce))))

(deftest extract-cljs-core-vars-test
  (let [vars (extract-var-info/extract-cljs-core-vars)]
    (is (contains? vars 'clj->js))
    (is (contains? vars 'transduce))
    (is (contains? vars 'eval))
    (is (contains? vars '*target*))))

(deftest default-java-imports-test
  (let [java-imports (extract-var-info/extract-default-imports)]
    (is (contains? java-imports 'Class))
    (is (contains? java-imports 'Object))
    (is (contains? java-imports 'String))
    (is (contains? java-imports 'Exception))))

;;;; Scratch

(comment
  (def eastwood (extract-var-info/eastwood-var-info))
  (get eastwood 'clojure.core/*out*)
  (def vars (extract-var-info/extract-clojure-core-vars))
  vars
  )
