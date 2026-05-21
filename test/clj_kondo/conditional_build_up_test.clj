(ns clj-kondo.conditional-build-up-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :refer [deftest is testing]]))

(def config
  {:linters {:conditional-build-up {:level :warning}}})

(deftest conditional-build-up-positive-test
  (testing "two consecutive conditional assocs (minimum case)"
    (assert-submaps2
     [{:file "<stdin>" :row 1 :col 6 :level :warning
       :message "Prefer cond-> to build a map with successive conditional assocs."}]
     (lint! "(let [m {:k0 0}
                   m (if (pos? in) (assoc m :k1 1) m)
                   m (if (even? in) (assoc m :k2 2) m)]
               m)"
            config)))

  (testing "three consecutive conditional assocs"
    (assert-submaps2
     [{:level :warning}]
     (lint! "(let [m {:k0 0}
                   m (if (p1 in) (assoc m :k1 1) m)
                   m (if (p2 in) (assoc m :k2 2) m)
                   m (if (p3 in) (assoc m :k3 3) m)]
               m)"
            config)))

  (testing "nested expressions inside assoc still count"
    (assert-submaps2
     [{:level :warning}]
     (lint! "(let [m {:k0 (f0 in)}
                   m (if (p1 in) (assoc m :k1 (if (p2 in) (f1 in) (f2 in))) m)
                   m (if (p3 in) (assoc m :k2 (f3 in)) m)]
               m)"
            config)))

  (testing "different symbol name also triggers"
    (assert-submaps2
     [{:level :warning}]
     (lint! "(let [acc {:k0 (f0 in)}
                   acc (if (p1 in) (assoc acc :k1 (f1 in)) acc)
                   acc (if (p2 in) (assoc acc :k2 (f2 in)) acc)]
               acc)"
            config)))

  (testing "non-literal base (function call returning map) still triggers"
    (assert-submaps2
     [{:level :warning}]
     (lint! "(let [m (f0 in)
                   m (if (p1 in) (assoc m :k1 1) m)
                   m (if (p2 in) (assoc m :k2 2) m)]
               m)"
            config)))

  (testing "reader-conditional splice in cljc"
    (assert-submaps2
     [{:level :warning}]
     (lint! "(let [m {:k0 (f0 in)}
                   m (if (p1 in) (assoc m :k1 (f1 in)) m)
                   #?@(:clj
                       [m (if (p2 in) (assoc m :clj-k2 (f2 in)) m)]
                       :cljs
                       [m (if (p3 in) (assoc m :cljs-k3 (f3 in)) m)])]
               m)"
            config "--lang" "cljc"))))

(deftest conditional-build-up-negative-test
  (testing "only one conditional assoc (below threshold)"
    (is (empty? (lint! "(let [m {:k0 0}
                              m (if (pos? in) (assoc m :k1 1) m)]
                          m)"
                       config))))

  (testing "variable changes at each step (no consecutive rebind)"
    (is (empty? (lint! "(let [m {:k0 0}
                              n (if (pos? in) (assoc m :k1 1) m)
                              o (if (even? in) (assoc n :k2 2) n)]
                          o)"
                       config))))

  (testing "predicate references map symbol"
    (is (empty? (lint! "(let [m {}
                              m (if (:a m) (assoc m :a 1) m)
                              m (if (:b m) (assoc m :b 2) m)]
                          m)"
                       config))))

  (testing "else branch returns a different value from the variable itself"
    (is (empty? (lint! "(let [m {:k0 0}
                              m (if (pos? in) (assoc m :k1 1) m)
                              m (if (even? in) (assoc m :k2 2) {:other 3})]
                          m)"
                       config))))

  (testing "intermediate non-conditional step breaks the sequence"
    (is (empty? (lint! "(let [m {:k0 0}
                              m (if (pos? in) (assoc m :k1 1) m)
                              m (assoc m :k1-5 1.5)
                              m (if (even? in) (assoc m :k2 2) m)]
                          m)"
                       config))))

  (testing "if without else (only 3 children, not recognized as a step)"
    (is (empty? (lint! "(let [m {:k0 0}
                              m (if (pos? in) (assoc m :k1 1))
                              m (if (even? in) (assoc m :k2 2))]
                          m)"
                       {:linters {:conditional-build-up {:level :warning}
                                  :missing-else-branch {:level :off}}}))))

  (testing "base binding references the variable itself (not a valid candidate)"
    (is (empty? (lint! "(let [m (f m)
                              m (if (pos? in) (assoc m :k1 1) m)
                              m (if (even? in) (assoc m :k2 2) m)]
                          m)"
                       config))))

  (testing "destructuring in binding resets tracking"
    (is (empty? (lint! "(let [[a b] [1 2]
                              m (if (pos? a) (assoc {} :k1 1) {})
                              m (if (even? b) (assoc m :k2 2) m)]
                          m)"
                       config))))

  (testing "idiomatic cond-> does not produce a warning"
    (is (empty? (lint! "(defn foo-ok [in]
                          (cond-> {:k0 (f0 in)}
                            (p1 in) (assoc :k1 (f1 in))
                            (p2 in) (assoc :k2 (f2 in))))"
                       config)))))
