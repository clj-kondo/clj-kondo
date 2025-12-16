(ns clj-kondo.unquote-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest unquote-outside-syntax-quote-test
  (testing "unquote outside syntax-quote"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning, :message "Unquote (~) used outside syntax-quote"})
     (lint! "~x" {:linters {:unquote-outside-syntax-quote {:level :warning}}})))
  (testing "unquote-splicing outside syntax-quote"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 1, :level :warning, :message "Unquote-splicing (~@) used outside syntax-quote"})
     (lint! "~@x" {:linters {:unquote-outside-syntax-quote {:level :warning}}})))
  (testing "unquote inside syntax-quote is allowed"
    (is (empty? (lint! "`(foo ~x)" {:linters {:unquote-outside-syntax-quote {:level :warning}}}))))
  (testing "unquote-splicing inside syntax-quote is allowed"
    (is (empty? (lint! "`(foo ~@xs)" {:linters {:unquote-outside-syntax-quote {:level :warning}}}))))
  (testing "quoted unquote is allowed"
    (is (empty? (lint! "'~x" {:linters {:unquote-outside-syntax-quote {:level :warning}}}))))
  (testing "quoted unquote-splicing is allowed"
    (is (empty? (lint! "'~@x" {:linters {:unquote-outside-syntax-quote {:level :warning}}}))))
  (testing "linter can be disabled"
    (is (empty? (lint! "~x" {:linters {:unquote-outside-syntax-quote {:level :off}}}))))
  (testing "quoted unquote in macro body is allowed"
    (is (empty? (lint! "
(defmacro defenterprise-impl
  \"Some macro docstring.\"
  [{:keys [bar docstr foo-ns fn-tail options schema? return-schema]}]
  (when-not docstr (throw (Exception. (str bar))))
  (let [oss-or-ee (if (System/getenv \"EE\") :ee :oss)]
    (case oss-or-ee
      :ee  (println options)
      :oss (println '~foo-ns options))
    `(let [ee-ns#        '~(or foo-ns (ns-name *ns*))
           ee-fn-name#   (symbol (str ee-ns# \"/\" '~bar))
           oss-or-ee-fn# ~(if schema?
                            `(fn ~(symbol (str bar)) :- ~return-schema ~@fn-tail)
                            `(fn ~(symbol (str bar)) ~@fn-tail))]
       (println ee-fn-name# (merge ~options {~oss-or-ee oss-or-ee-fn#}))
       (def
         ~(vary-meta 'baz assoc :arglists ''([& args]))
         ~docstr
         (println ee-ns# ee-fn-name#)))))"
                       {:linters {:unquote-outside-syntax-quote {:level :warning}}})))))
