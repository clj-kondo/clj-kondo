(ns clj-kondo.keyword-binding-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :as t :refer [deftest is testing]]))

(def example-with-keyword-bindings
  "(ns test
     {:clj-kondo/config
       '{:linters {:keyword-binding {:level :warning}}}})
   (let [{:keys [:a :b]} {:a 1 :b 2}] (println a b))")

(deftest multiple-keywords-test
  (assert-submaps2
   [{:file "<stdin>", :row 4, :col 18, :level :warning, :message "Keyword binding should be a symbol: :a"}
    {:file "<stdin>", :row 4, :col 21, :level :warning, :message "Keyword binding should be a symbol: :b"}]

   (lint! example-with-keyword-bindings)))

(def mixed-example
  "(ns test
     {:clj-kondo/config
       '{:linters {:keyword-binding {:level :warning}}}})
   (let [{:keys [:a b]} {:a 1 :b 2}] (println a b))")

(deftest mixed-keyword-and-symbols-test
  (assert-submaps2
   [{:file "<stdin>", :row 4, :col 18, :level :warning, :message "Keyword binding should be a symbol: :a"}]
   (lint! mixed-example)))

(deftest namespaced-keyword-test
  (testing "keywords with namespaced are ignored"
    (assert-submaps2
     [{:file "<stdin>", :row 1, :col 24, :level :warning, :message "Keyword binding should be a symbol: :baz"}]
     (lint! "(let [{:keys [:foo/bar :baz ::baz]} {}] bar)"
            {:linters {:keyword-binding {:level :warning}}}))))
