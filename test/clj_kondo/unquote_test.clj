(ns clj-kondo.unquote-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2 with-temp-dir]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest unquote-outside-syntax-quote-test
  (testing "unquote outside syntax-quote"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 1
        :level :warning
        :message "Unquote (~) not syntax-quoted"})
     (lint! "~x" {:linters {:unquote-not-syntax-quoted
                            {:level :warning}}})))
  (testing "unquote-splicing outside syntax-quote"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 1
        :level :warning
        :message "Unquote-splicing (~@) not syntax-quoted"})
     (lint! "~@x" {:linters {:unquote-not-syntax-quoted
                             {:level :warning}}})))
  (testing "unquote outside syntax-quote by double unquote"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 14
        :level :warning
        :message "Unquote (~) not syntax-quoted"})
     (lint! "(def x 1) `[~~x]"
            {:linters {:unquote-not-syntax-quoted
                       {:level :warning}}})))
  (testing "unquote inside syntax-quote is allowed"
    (is (empty? (lint! "`(foo ~x)" {:linters {:unquote-not-syntax-quoted
                                              {:level :warning}}}))))
  (testing "unquote-splicing inside syntax-quote is allowed"
    (is (empty? (lint! "`(foo ~@xs)" {:linters {:unquote-not-syntax-quoted
                                                {:level :warning}}}))))
  (testing "quoted unquote warns"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 2
        :level :warning
        :message "Unquote (~) not syntax-quoted"})
     (lint! "'~x" {:linters {:unquote-not-syntax-quoted
                             {:level :warning}}})))
  (testing "quoted unquote-splicing warns"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 2
        :level :warning
        :message "Unquote-splicing (~@) not syntax-quoted"})
     (lint! "'~@x" {:linters {:unquote-not-syntax-quoted {:level :warning}}})))
  (testing "linter can be disabled"
    (is (empty? (lint! "~x" {:linters {:unquote-not-syntax-quoted
                                       {:level :off}}})))
    (is (empty? (lint! "'~x" {:linters {:unquote-not-syntax-quoted
                                        {:level :off}}}))))
  (testing "linter can be disabled in specific calls with config-in-call"
    (assert-submaps2
     '({:file "<stdin>"
        :row 7
        :col 1
        :level :warning
        :message "Unquote (~) not syntax-quoted"})
     (lint! "(ns scratch
  {:clj-kondo/config '{:config-in-call {babashka2.process/$$ {:linters {:unquote-not-syntax-quoted {:level :off}}}}}})

(require '[babashka2.process :as proc])

(proc/$$ 1 ~2) ;; no warning here
~2  ;; warning" {:linters {:unquote-not-syntax-quoted {:level :warning}}})))
  (testing "defproject with config-in-call disables unquote-not-syntax-quoted"
    (is (empty? (lint! "(def proto-version \"1.2.3\")

(defproject my-project \"0.1.0\"
  :dependencies [[some/lib ~proto-version]
                 [other/lib ~proto-version]])"
                       {:linters {:unquote-not-syntax-quoted {:level :warning}}
                        :config-in-call {'user/defproject {:linters {:unquote-not-syntax-quoted {:level :off}}}}}))))
  (testing "defproject with unquote in dependencies - should warn without config"
    (assert-submaps2
     '({:file "<stdin>"
        :row 4
        :col 28
        :level :warning
        :message "Unquote (~) not syntax-quoted"}
       {:file "<stdin>"
        :row 5
        :col 29
        :level :warning
        :message "Unquote (~) not syntax-quoted"})
     (lint! "(def proto-version \"1.2.3\")

(defproject my-project \"0.1.0\"
  :dependencies [[some/lib ~proto-version]
                 [other/lib ~proto-version]])"
            {:linters {:unquote-not-syntax-quoted {:level :warning}}})))
  (testing "defproject with config-in-call using fully qualified symbol"
    ;; Use leiningen.core/defproject which is the actual namespace
    (is (empty? (lint! "(def proto-version \"1.2.3\")

(defproject my-project \"0.1.0\"
  :dependencies [[some/lib ~proto-version]
                 [other/lib ~proto-version]])"
                       {:linters {:unquote-not-syntax-quoted {:level :warning}}
                        :config-in-call {'user/defproject {:linters {:unquote-not-syntax-quoted {:level :off}}}}}))))
  (testing "defproject in real project.clj file with config-in-call"
    (with-temp-dir [tmp-dir "defproject-test"]
      (let [project-clj (io/file tmp-dir "project.clj")]
        (spit project-clj "(def proto-version \"1.2.3\")

(defproject my-project \"0.1.0\"
  :dependencies [[some/lib ~proto-version]
                 [other/lib ~proto-version]])")
        (is (empty? (lint! project-clj
                           {:linters {:unquote-not-syntax-quoted {:level :warning}}
                            :config-in-call {'user/defproject {:linters {:unquote-not-syntax-quoted {:level :off}}}}})))
        (let [results (lint! project-clj
                             {:linters {:unquote-not-syntax-quoted {:level :warning}}})]
          (assert-submaps2
           '({:file #".*project\.clj"
              :row 4
              :col 28
              :level :warning
              :message "Unquote (~) not syntax-quoted"}
             {:file #".*project\.clj"
              :row 5
              :col 29
              :level :warning
              :message "Unquote (~) not syntax-quoted"})
           results))))))
