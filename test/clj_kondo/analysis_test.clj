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
                :var-usages]} (analyze "(defn ^:deprecated foo \"docstring\" {:added \"1.2\"} [])")]
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 1,
        :ns user,
        :name foo,
        :fixed-arities #{0},
        :doc "docstring",
        :added "1.2",
        :deprecated true}]
     var-definitions)
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 1,
        :from user,
        :to clojure.core,
        :name defn,
        :arity 4}]
     var-usages))

  (let [{:keys [:var-definitions]} (analyze "(def ^:deprecated x \"docstring\" 1)")]
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 1,
        :ns user,
        :name x,
        :doc "docstring",
        :deprecated true}]
     var-definitions))

  (let [{:keys [:namespace-definitions
                :namespace-usages]}
        (analyze "(ns ^:deprecated foo \"docstring\" {:added \"1.2\"} (:require [clojure.string]))")]
    (assert-submaps
     '[{:filename "<stdin>", :row 1, :col 1, :name user}
       {:filename "<stdin>",
        :row 1,
        :col 1,
        :name foo,
        :deprecated true,
        :doc "docstring"}]
     namespace-definitions)
    (assert-submaps
     '[{:filename "<stdin>", :row 1, :col 60, :from foo, :to clojure.string}]
     namespace-usages)))
