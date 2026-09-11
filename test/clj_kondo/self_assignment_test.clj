(ns clj-kondo.self-assignment-test
  (:require
   [clj-kondo.test-utils :refer [assert-submaps2 lint!]]
   [clojure.test :refer [deftest is]]))

(def config
  {:linters {:self-assignment {:level :warning}}})

(deftest self-assignment-test
  (assert-submaps2
   '({:row 1
      :col 23
      :message "Value is assigned to itself"})
   (lint! "(def ^:dynamic *x* 1) (set! *x* *x*)" config))
  (assert-submaps2
   '({:row 1
      :col 9
      :message "Value is assigned to itself"})
   (lint! "(fn [o] (set! (.-x o) (.-x o)))" config "--lang" "cljs"))
  (assert-submaps2
   '({:row 1
      :col 9
      :message "Value is assigned to itself"})
   (lint! "(fn [o] (set! o -x (.-x o)))" config "--lang" "cljs"))
  (is (empty? (lint! "(def ^:dynamic *x* 1) (set! *x* 2)" config)))
  (is (empty? (lint! "(def ^:dynamic *x* 1) (set! *x* *x*)"
                     {:linters {:self-assignment {:level :off}}}))))
