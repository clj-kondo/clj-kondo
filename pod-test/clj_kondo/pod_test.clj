(ns clj-kondo.pod-test
  (:require [babashka.pods :as pods]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(def pod-spec (if (= "native" (System/getenv "CLJ_KONDO_TEST_ENV"))
                ["./clj-kondo"]
                ["clojure" "-M:clj-kondo/dev"]))

(pods/load-pod pod-spec)
(require '[clj-kondo.core :as clj-kondo])

(deftest pod-test
  (is (= '{:linters {:unresolved-symbol {:exclude [(foo1.bar) (foo2.bar)]}}}
         (clj-kondo/merge-configs
          '{:linters {:unresolved-symbol {:exclude [(foo1.bar)]}}}
          '{:linters {:unresolved-symbol {:exclude [(foo2.bar)]}}})))
  (is (str/includes? (with-out-str (clj-kondo/print! (clj-kondo/run! {:lint ["src"]})))
                     "errors")))

(when (= *file* (System/getProperty "babashka.file"))
  (clojure.test/run-tests 'clj-kondo.pod-test))
