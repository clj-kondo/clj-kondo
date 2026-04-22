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

(def namespaced-keyword-example
  "(ns keyword-binding
     (:require [foo.bar :as-alias fb]))

   (let [{:keys [:foo/a :b ::fb/c ::d]} {}] a)")

(deftest namespaced-keyword-test
  (testing "keywords with namespace are ignored by default"
    (assert-submaps2
     [{:file "<stdin>", :row 4, :col 25, :level :warning, :message "Keyword binding should be a symbol: :b"}]
     (lint! namespaced-keyword-example
            {:linters {:keyword-binding {:level :warning}}})))

  (testing "keywords with namespace are not ignored when specified otherwise"
    (assert-submaps2
     [{:file "<stdin>", :row 4, :col 18, :level :warning, :message "Keyword binding should be a symbol: :foo/a"}
      {:file "<stdin>", :row 4, :col 25, :level :warning, :message "Keyword binding should be a symbol: :b"}
      {:file "<stdin>", :row 4, :col 28, :level :warning, :message "Keyword binding should be a symbol: ::fb/c"}
      {:file "<stdin>", :row 4, :col 35, :level :warning, :message "Keyword binding should be a symbol: ::d"}]
     (lint! namespaced-keyword-example
            {:linters {:keyword-binding {:disallow-all-keywords true
                                         :level :warning}}}))))
