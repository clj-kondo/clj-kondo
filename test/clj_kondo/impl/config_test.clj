(ns clj-kondo.impl.config-test
  (:require [clj-kondo.impl.config :refer [merge-config!]]
            [clojure.test :refer [deftest is testing]]))

(deftest merge-replace-config-test
  (testing "^:replace top-level value"
    (is (= {:linters {:b 2}
            :lint-as {'b 'y}}
           (merge-config! {:linters {:a 1}
                           :lint-as {'a 'x}}
                          ^:replace {:linters {:b 2}
                                     :lint-as {'b 'y}}))))

  (testing "^:replace merging supports nested values"
    (is (= {:linters {:b 2}
            :lint-as {'a 'x 'b 'y}}
           (merge-config! {:linters {:a 1}
                           :lint-as {'a 'x}}
                          {:linters ^:replace {:b 2}
                           :lint-as {'b 'y}})))))

(deftest merge-set-and-vecs-test
  (-> (merge-config! '{:linters {:unresolved-namespace {:exclude #{foo bar}}}}
                     '{:linters {:unresolved-namespace {:exclude [baz]}}})
      :linters :unresolved-namespace :exclude
      (= '#{foo bar baz})
      is)
  (-> (merge-config! '{:linters {:unresolved-namespace {:exclude [foo bar]}}}
                     '{:linters {:unresolved-namespace {:exclude #{baz}}}})
      :linters :unresolved-namespace :exclude
      (= '[foo bar baz])
      is))
