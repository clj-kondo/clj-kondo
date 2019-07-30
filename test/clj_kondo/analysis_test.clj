(ns clj-kondo.analysis-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(defn analyze [input]
  (:analysis
   (with-in-str
     input
     (clj-kondo/run! {:lint ["-"]
                      :config {:output {:analysis true}}}))))

(deftest analysis-test
  (let [{:keys [:var-definitions
                :var-usages]} (analyze "(defn foo [])")]
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 1,
        :ns user,
        :name foo,
        :fixed-arities #{0}}]
     var-definitions)
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 1,
        :from user,
        :to clojure.core,
        :name defn,
        :arity 2}]
     var-usages)))
