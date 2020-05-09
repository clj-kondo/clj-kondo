(ns clj-kondo.pod-test
  (:require [babashka.pods :as pods]
            [clojure.test :refer [deftest is]]))

(def pod-spec (if (= "native" (System/getenv "CLJ_KONDO_TEST_ENV"))
                ["./clj-kondo" "--run-as-pod"]
                ["clojure" "-A:clj-kondo" "--run-as-pod"]))

(pods/load-pod pod-spec)
(require '[pod.borkdude.clj-kondo :as clj-kondo])

(deftest pod-test
  (is (= '{:linters {:unresolved-symbol {:exclude [(foo1.bar) (foo2.bar)]}}}
         (clj-kondo/merge-configs
          '{:linters {:unresolved-symbol {:exclude [(foo1.bar)]}}}
          '{:linters {:unresolved-symbol {:exclude [(foo2.bar)]}}}))))
