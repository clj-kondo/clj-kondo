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
  (is (= '#{foo bar baz} (-> (merge-config! '{:linters {:unresolved-namespace {:exclude #{foo bar}}}}
                                            '{:linters {:unresolved-namespace {:exclude [baz]}}})
                             :linters :unresolved-namespace :exclude)))
  (is (= '[foo bar baz](-> (merge-config! '{:linters {:unresolved-namespace {:exclude [foo bar]}}}
                                          '{:linters {:unresolved-namespace {:exclude #{baz}}}})
                           :linters :unresolved-namespace :exclude))))

(deftest merge-min-clj-kondo-version-test
  (testing "preserves highest min-clj-kondo-version when later config is lower"
    (is (= "2025.02.01"
           (:min-clj-kondo-version
            (merge-config! {:min-clj-kondo-version "2025.02.01"}
                           {:min-clj-kondo-version "2025.01.01"})))))
  (testing "preserves highest min-clj-kondo-version when later config is higher"
    (is (= "2025.02.01"
           (:min-clj-kondo-version
            (merge-config! {:min-clj-kondo-version "2025.01.01"}
                           {:min-clj-kondo-version "2025.02.01"})))))
  (testing "preserves min-clj-kondo-version when only one config defines it"
    (is (= "2025.01.01"
           (:min-clj-kondo-version
            (merge-config! {:min-clj-kondo-version "2025.01.01"} {})))))
  (testing "highest min-clj-kondo-version across multiple merged configs"
    (is (= "2025.03.01"
           (:min-clj-kondo-version
            (merge-config! {:min-clj-kondo-version "2025.01.01"}
                           {:min-clj-kondo-version "2025.03.01"}
                           {:min-clj-kondo-version "2025.02.01"})))))
  (testing "a lower :min-clj-kondo-version keeps the other keys of the config"
    (is (= {:min-clj-kondo-version "2025.02.01" :linters {:a 1 :b 2}}
           (merge-config! {:min-clj-kondo-version "2025.02.01" :linters {:a 1}}
                          {:min-clj-kondo-version "2025.01.01" :linters {:b 2}}))))
  (testing "^:replace config sets a lower :min-clj-kondo-version"
    (is (= {:min-clj-kondo-version "2025.01.01"}
           (merge-config! {:min-clj-kondo-version "2025.02.01"}
                          ^:replace {:min-clj-kondo-version "2025.01.01"}))))
  (testing "^:replace config without :min-clj-kondo-version drops it"
    (is (= {:linters {:a 1}}
           (merge-config! {:min-clj-kondo-version "2025.02.01"}
                          ^:replace {:linters {:a 1}})))))

(deftest merge-config!-test
  (testing "type-mismatch :arities are overwritten instead of merged"
    (is (= {:linters {:type-mismatch {:namespaces '{my-ns {shared   {:arities {1 {:args [:str] :ret :str}
                                                                               2 {:args [:int :int] :ret :int}
                                                                               3 {:args [:str :str :str] :ret :str}}}
                                                           clj-only {:arities {1 {:args [:int] :ret :int}}}
                                                           cljs-only {:arities {1 {:args [:int] :ret :int}}}}}}}}
           (merge-config!
            {:linters {:type-mismatch {:namespaces '{my-ns {shared   {:arities {1 {:args [:int] :ret :int}
                                                                                2 {:args [:int :int] :ret :int}}}
                                                            clj-only {:arities {1 {:args [:int] :ret :int}}}}}}}}
            {:linters {:type-mismatch {:namespaces '{my-ns {shared   {:arities {1 {:args [:str] :ret :str}
                                                                                3 {:args [:str :str :str] :ret :str}}}
                                                            cljs-only {:arities {1 {:args [:int] :ret :int}}}}}}}})))))
