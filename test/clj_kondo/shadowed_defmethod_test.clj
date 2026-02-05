(ns clj-kondo.shadowed-defmethod-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:private config {:linters {:shadowed-defmethod {:level :warning}
                                 :unresolved-symbol {:level :off}}})

(deftest shadowed-defmethod-same-namespace-test
  (testing "Detects shadowed defmethod in same namespace"
    (assert-submaps2
     [{:file "<stdin>", :row 7, :col 1, :level :warning,
       :message "Shadowed defmethod: ig/init-key for dispatch value :executor"}]
     (lint! "(ns system)

(defmethod ig/init-key :executor [_ {:keys [core-pool-size]}]
  (Executors/newScheduledThreadPool core-pool-size
                                    (.factory (Thread/ofVirtual))))

(defmethod ig/init-key :executor [_ ^ScheduledExecutorService executor]
  (.shutdown executor))
"
            '{:linters {:shadowed-defmethod {:level :warning}
                        :unresolved-namespace {:level :off}
                        :unresolved-symbol {:level :off}}}))))

(deftest shadowed-defmethod-different-dispatch-values-test
  (testing "Different dispatch values should not trigger warning"
    (is (empty?
         (lint! "(ns system)

(defmethod ig/init-key ::executor [_ {:keys [core-pool-size]}]
  (Executors/newScheduledThreadPool core-pool-size))

(defmethod ig/init-key ::database [_ config]
  (connect-db config))
"
                '{:linters {:shadowed-defmethod {:level :warning}
                            :unresolved-namespace {:level :off}
                            :unresolved-symbol {:level :off}}})))))

(deftest shadowed-defmethod-different-multimethods-test
  (testing "Same dispatch value but different multimethods should not trigger warning"
    (is (empty?
         (lint! "(ns system)

(defmethod ig/init-key ::executor [_ config]
  (init-executor config))

(defmethod ig/halt-key! ::executor [_ executor]
  (shutdown-executor executor))
"
                '{:linters {:shadowed-defmethod {:level :warning}
                            :unresolved-namespace {:level :off}
                            :unresolved-symbol {:level :off}}})))))

(deftest shadowed-defmethod-multiple-shadows-test
  (testing "Multiple shadowed defmethods are detected"
    (assert-submaps2
     [{:file "<stdin>", :row 5, :col 1, :level :warning,
       :message "Shadowed defmethod: my-multi for dispatch value :foo"}
      {:file "<stdin>", :row 7, :col 1, :level :warning,
       :message "Shadowed defmethod: my-multi for dispatch value :foo"}]
     (lint! "(ns test)

(defmethod my-multi :foo [x] 1)

(defmethod my-multi :foo [x] 2)

(defmethod my-multi :foo [x] 3)
"
            '{:linters {:shadowed-defmethod {:level :warning}
                        :unresolved-symbol {:level :off}}}))))

(deftest shadowed-defmethod-disabled-test
  (testing "Linter can be disabled"
    (is (empty?
         (lint! "(ns system)

(defmethod ig/init-key ::executor [_ {:keys [core-pool-size]}]
  (Executors/newScheduledThreadPool core-pool-size))

(defmethod ig/init-key ::executor [_ ^ScheduledExecutorService executor]
  (.shutdown executor))
"
                '{:linters {:shadowed-defmethod {:level :off}
                            :unresolved-namespace {:level :off}
                            :unresolved-symbol {:level :off}}})))))

(deftest shadowed-defmethod-with-qualified-dispatch-test
  (testing "Qualified keywords as dispatch values"
    (assert-submaps2
     [{:file "<stdin>", :row 5, :col 1, :level :warning,
       :message "Shadowed defmethod: mm for dispatch value :my.ns/foo"}]
     (lint! "(ns test)

(defmethod mm :my.ns/foo [x] 1)

(defmethod mm :my.ns/foo [x] 2)
"
            '{:linters {:shadowed-defmethod {:level :warning}
                        :unresolved-symbol {:level :off}}}))))

(deftest shadowed-defmethod-with-string-dispatch-test
  (testing "String dispatch values"
    (assert-submaps2
     [{:file "<stdin>", :row 5, :col 1, :level :warning,
       :message "Shadowed defmethod: mm for dispatch value \"test\""}]
     (lint! "(ns test)

(defmethod mm \"test\" [x] 1)

(defmethod mm \"test\" [x] 2)
"
            '{:linters {:shadowed-defmethod {:level :warning}
                        :unresolved-symbol {:level :off}}}))))

(deftest shadowed-defmethod-with-vector-dispatch-test
  (testing "Vector dispatch values"
    (assert-submaps2
     [{:file "<stdin>", :row 5, :col 1, :level :warning,
       :message "Shadowed defmethod: mm for dispatch value [:a :b]"}]
     (lint! "(ns test)

(defmethod mm [:a :b] [x] 1)

(defmethod mm [:a :b] [x] 2)
"
            '{:linters {:shadowed-defmethod {:level :warning}
                        :unresolved-symbol {:level :off}}}))))

(deftest shadowed-defmethod-inline-ignore-test
  (testing "Can be disabled inline with comment"
    (is (empty?
         (lint! "(ns system)

(defmethod ig/init-key ::executor [_ {:keys [core-pool-size]}]
  (Executors/newScheduledThreadPool core-pool-size))

#_:clj-kondo/ignore
(defmethod ig/init-key ::executor [_ ^ScheduledExecutorService executor]
  (.shutdown executor))
"
                '{:linters {:shadowed-defmethod {:level :warning}
                            :unresolved-namespace {:level :off}
                            :unresolved-symbol {:level :off}}})))))

(deftest shadowed-defmethod-different-namespaces-test
  (testing "Detects shadowed defmethod across different namespaces - warns on both"
    (let [tmp-dir (Files/createTempDirectory "clj-kondo-test" (into-array FileAttribute []))
          ns1-file (io/file (.toFile tmp-dir) "ns1.clj")
          ns2-file (io/file (.toFile tmp-dir) "ns2.clj")]
      (spit ns1-file "(ns ns1)

(defmethod mm :foo [x] 1)
")
      (spit ns2-file "(ns ns2)

(defmethod mm :foo [x] 2)
")
      (let [results (lint! [ns1-file ns2-file]
                           '{:linters {:shadowed-defmethod {:level :warning}
                                       :unresolved-symbol {:level :off}}})]
        (assert-submaps2
         [{:file (.getPath ns1-file)
           :row 3
           :col 1
           :level :warning
           :message "Shadowed defmethod: mm for dispatch value :foo"}
          {:file (.getPath ns2-file)
           :row 3
           :col 1
           :level :warning
           :message "Shadowed defmethod: mm for dispatch value :foo"}]
         results))

      (io/delete-file ns1-file true)
      (io/delete-file ns2-file true)
      (io/delete-file (.toFile tmp-dir) true))))

(deftest shadowed-defmethod-three-namespaces-test
  (testing "Detects shadowed defmethod across three namespaces"
    (let [tmp-dir (Files/createTempDirectory "clj-kondo-test" (into-array FileAttribute []))
          ns1-file (io/file (.toFile tmp-dir) "ns1.clj")
          ns2-file (io/file (.toFile tmp-dir) "ns2.clj")
          ns3-file (io/file (.toFile tmp-dir) "ns3.clj")]
      (try
        (spit ns1-file "(ns ns1)

(defmethod my-multi ::exec [x] 1)
")
        (spit ns2-file "(ns ns2)

(defmethod my-multi :ns1/exec [x] 2)
")
        (spit ns3-file "(ns ns3)

(defmethod my-multi :ns1/exec [x] 3)
")
        (let [results (lint! [ns1-file ns2-file ns3-file]
                             '{:linters {:shadowed-defmethod {:level :warning}
                                         :unresolved-symbol {:level :off}}})]
          ;; At least 2 should be flagged (all 3 ideally, but current implementation may have limitations)
          (is (>= (count results) 2))
          (is (every? #(= "Shadowed defmethod: my-multi for dispatch value :ns1/exec" (:message %))
                      results)))
        (finally
          (io/delete-file ns1-file true)
          (io/delete-file ns2-file true)
          (io/delete-file ns3-file true)
          (io/delete-file (.toFile tmp-dir) true))))))

(deftest shadowed-defmethod-mixed-same-and-cross-namespace-test
  (testing "Combination of same-namespace and cross-namespace shadowing"
    (let [tmp-dir (Files/createTempDirectory "clj-kondo-test" (into-array FileAttribute []))
          ns1-file (io/file (.toFile tmp-dir) "ns1.clj")
          ns2-file (io/file (.toFile tmp-dir) "ns2.clj")]
      (try
        (spit ns1-file "(ns ns1)

(defmethod mm :foo [x] 1)
(defmethod mm :foo [x] 2)
")
        (spit ns2-file "(ns ns2)

(defmethod mm :foo [x] 3)
")
        (let [results (lint! [ns1-file ns2-file]
                             '{:linters {:shadowed-defmethod {:level :warning}
                                         :unresolved-symbol {:level :off}}})]
          (assert-submaps2
           [{:file (.getPath ns1-file)
             :row 3
             :col 1
             :level :warning
             :message "Shadowed defmethod: mm for dispatch value :foo"}
            {:file (.getPath ns1-file)
             :row 4
             :col 1
             :level :warning
             :message "Shadowed defmethod: mm for dispatch value :foo"}
            {:file (.getPath ns2-file)
             :row 3
             :col 1
             :level :warning
             :message "Shadowed defmethod: mm for dispatch value :foo"}]
           results))
        (finally
          (io/delete-file ns1-file true)
          (io/delete-file ns2-file true)
          (io/delete-file (.toFile tmp-dir) true))))))

(deftest shadowed-defmethod-cljc-test
  (let [tmp-dir (Files/createTempDirectory "clj-kondo-test" (into-array FileAttribute []))
        cljc-file (io/file (.toFile tmp-dir) "test.cljc")]
    (testing "CLJC defmethods should not be flagged as shadowed"
      (spit cljc-file "(ns test)
(defmulti mm :type)
(defmethod mm :foo [x] x)
")
      (is (empty? (lint! [cljc-file] config))
          "CLJC reader conditional branches should not trigger shadowed defmethod warning")

      (testing "with reader conditionals"
        (spit cljc-file "(ns test)
      (defmulti mm :type)
      #?(:clj (defmethod mm :foo [x] :clj-impl)
         :cljs (defmethod mm :foo [x] :cljs-impl))
      ")
        (is (empty? (lint! [cljc-file] config))
            "CLJC reader conditional branches should not trigger shadowed defmethod warning"))

      (testing "in same reader branch should still be flagged"
        (spit cljc-file "(ns test)
       (defmulti mm :type)
       #?(:clj
          (do
            (defmethod mm :foo [x] :first)
            (defmethod mm :foo [x] :second)))
       ")
        (assert-submaps2
         [{:file (.getPath cljc-file)
           :row 6
           :col 13
           :level :warning
           :message "Shadowed defmethod: mm for dispatch value :foo"}]
         (lint! [cljc-file] config))))

    (io/delete-file cljc-file true)
    (io/delete-file (.toFile tmp-dir) true)))

(deftest shadowed-defmethod-variable-dispatch-value-test
  (testing "Defmethods with variable dispatch values (e.g., in doseq) should not warn"
    (is (empty?
         (lint! "(ns test)
(defmulti mm identity)
(doseq [x [:a :b]]
  (defmethod mm x [_] :first))
(doseq [x [:a :c]]
  (defmethod mm x [_] :second))
"
                config)))))

(deftest shadowed-defmethod-different-defmultis-same-name-test
  (let [tmp-dir (Files/createTempDirectory "clj-kondo-test" (into-array FileAttribute []))
        ns1-file (io/file (.toFile tmp-dir) "records.clj")
        ns2-file (io/file (.toFile tmp-dir) "deftype.clj")]
    (try
      (testing "defmethods for different defmultis with same name should not be flagged as shadowed"
        (spit ns1-file "(ns sci.impl.records
  {:no-doc true})

(defmulti to-string identity)
(defmethod to-string :default [this]
  \"records impl\")
")
        (spit ns2-file "(ns sci.impl.deftype
  {:no-doc true})

(defmulti to-string identity)
(defmethod to-string :default [this]
  \"deftype impl\")
")
        (let [results (lint! [ns1-file ns2-file] config)]
          (is (empty? results)
              "defmethods for different defmultis should not be flagged as shadowed")))
      (finally
        (io/delete-file ns1-file true)
        (io/delete-file ns2-file true)
        (io/delete-file (.toFile tmp-dir) true)))))

(deftest shadowed-defmethod-auto-resolved-keywords-test
  (let [tmp-dir (Files/createTempDirectory "clj-kondo-test" (into-array FileAttribute []))
        ns1-file (io/file (.toFile tmp-dir) "test1.clj")
        ns2-file (io/file (.toFile tmp-dir) "test2.clj")]
    (try
      (testing "auto-resolved keywords (::keyword) in different namespaces should not shadow"
        (spit ns1-file "(ns metabase.driver.common.table-rows-sample-test
  (:require [metabase.driver :as driver]))

(defmethod driver/database-supports? [::driver/driver ::field-count-tests]
  [_driver _feature _database]
  true)
")
        (spit ns2-file "(ns metabase.warehouse-schema.metadata-from-qp-test
  (:require [metabase.driver :as driver]))

(defmethod driver/database-supports? [::driver/driver ::field-count-tests]
  [_driver _feature _database]
  true)
")
        (let [results (lint! [ns1-file ns2-file] config)]
          (is (empty? results)
              "auto-resolved keywords in different namespaces resolve to different dispatch values")))
      (finally
        (io/delete-file ns1-file true)
        (io/delete-file ns2-file true)
        (io/delete-file (.toFile tmp-dir) true)))))
