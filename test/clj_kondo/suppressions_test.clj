(ns clj-kondo.suppressions-test
  (:require
   [babashka.fs :as fs]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.main :as main]
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is testing]]))

(defn- lint-file [source baseline options]
  (clj-kondo/run!
   (merge {:lint [(str source)]
           :cache false
           :suppressions-location (str baseline)}
          options)))

(defn- suppression-entries [baseline]
  (:suppressions (edn/read-string (slurp baseline))))

(defn- with-temp-dir [f]
  (let [dir (fs/create-temp-dir)]
    (try
      (f dir)
      (finally
        (fs/delete-tree dir)))))

(deftest baseline-suppressions-test
  (with-temp-dir
    (fn [dir]
      (let [source (fs/file dir "example.clj")
            baseline (fs/file dir ".clj-kondo" "suppressions.edn")]
        (spit source "(defn foo [x] y)")
        (testing "generating a baseline suppresses all current findings"
          (is (empty? (:findings
                       (lint-file source baseline {:suppress-all true}))))
          (is (= #{:unresolved-symbol :unused-binding}
                 (set (map :type (suppression-entries baseline))))))

        (testing "moving findings to another row does not invalidate the baseline"
          (spit source "\n\n(defn foo [x] y)")
          (is (empty? (:findings (lint-file source baseline {})))))

        (testing "new findings remain visible"
          (spit source "(defn foo [x] (+ y z))")
          (let [findings (:findings (lint-file source baseline {}))]
            (is (= 1 (count findings)))
            (is (= :unresolved-symbol (:type (first findings))))
            (is (= "Unresolved symbol: z" (:message (first findings))))))

        (testing "pruning removes entries that no longer match"
          (spit source "(defn foo [] z)")
          (let [findings (:findings
                          (lint-file source baseline {:prune-suppressions true}))]
            (is (= ["Unresolved symbol: z"] (mapv :message findings)))
            (is (empty? (suppression-entries baseline)))))))))

(deftest selective-suppression-test
  (with-temp-dir
    (fn [dir]
      (let [source (fs/file dir "example.clj")
            baseline (fs/file dir "suppressions.edn")]
        (spit source "(defn foo [x] y)")
        (is (= [:unused-binding]
               (mapv :type
                     (:findings
                      (lint-file source baseline
                                 {:suppress-rules [:unresolved-symbol]})))))
        (is (= [:unresolved-symbol]
               (mapv :type (suppression-entries baseline))))))))

(deftest suppressions-cli-test
  (testing "suppression options are parsed"
    (is (= {:suppress-all true
            :suppress-rules [:unresolved-symbol :unused-binding]
            :suppressions-location "baseline.edn"
            :prune-suppressions true}
           (select-keys
            (#'main/parse-opts
             ["--suppress-all"
              "--suppress-rule" "unresolved-symbol"
              "--suppress-rule=unused-binding"
              "--suppressions-location" "baseline.edn"
              "--prune-suppressions"])
            [:suppress-all
             :suppress-rules
             :suppressions-location
             :prune-suppressions]))))
  (testing "suppressed findings do not affect the exit code"
    (with-temp-dir
      (fn [dir]
        (let [baseline (str (fs/file dir "suppressions.edn"))
              options ["--lint" "-"
                       "--suppressions-location" baseline]]
          (with-out-str
            (is (zero?
                 (with-in-str "(defn foo [x] y)"
                   (apply main/main (conj options "--suppress-all")))))
            (is (zero?
                 (with-in-str "(defn foo [x] y)"
                   (apply main/main options))))))))))
