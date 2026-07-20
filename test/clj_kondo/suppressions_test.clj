(ns clj-kondo.suppressions-test
  (:require
   [babashka.fs :as fs]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.main :as main]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(defn- lint-file [source baseline options]
  (clj-kondo/run!
   (merge {:lint [(str source)]
           :cache false
           :suppressions-location (str baseline)
           :apply-suppressions true
           :manage-suppressions true
           :config {:linters {:unused-binding {:level :error}}}}
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

        (testing "inserting an expression before findings does not invalidate the baseline"
          (spit source "(def inserted-expression 1)\n(defn foo [x] y)")
          (is (empty? (:findings (lint-file source baseline {})))))

        (testing "new findings remain visible"
          (spit source "(defn foo [x] (+ y z))")
          (let [findings (:findings (lint-file source baseline {}))]
            (is (= [:unresolved-symbol :unresolved-symbol]
                   (mapv :type findings)))
            (is (= ["Unresolved symbol: y" "Unresolved symbol: z"]
                   (mapv :message findings)))))

        (testing "pruning removes unused linters and updates counts"
          (spit source "(defn foo [] z)")
          (let [findings (:findings
                          (lint-file source baseline {:prune-suppressions true}))]
            (is (empty? findings))
            (is (= [{:type :unresolved-symbol
                     :count 1}]
                   (mapv #(select-keys % [:type :count])
                         (suppression-entries baseline))))))))))

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

(deftest warnings-are-not-suppressed-test
  (with-temp-dir
    (fn [dir]
      (let [source (fs/file dir "example.clj")
            baseline (fs/file dir "suppressions.edn")]
        (spit source "(defn foo [x] 1)")
        (let [result (lint-file source baseline
                                {:suppress-all true
                                 :config {}})]
          (is (= [:unused-binding] (mapv :type (:findings result))))
          (is (empty? (:suppressed-findings result)))
          (is (empty? (suppression-entries baseline))))))))

(deftest api-suppressions-are-opt-in-test
  (with-temp-dir
    (fn [dir]
      (let [source (fs/file dir "example.clj")
            baseline (fs/file dir "suppressions.edn")
            options {:lint [(str source)]
                     :cache false
                     :suppressions-location (str baseline)}]
        (spit source "(x)")
        (lint-file source baseline {:suppress-all true})
        (testing "the API does not apply suppressions by default"
          (is (= [:unresolved-symbol]
                 (mapv :type (:findings (clj-kondo/run! options))))))
        (testing "the API returns suppressed findings when explicitly enabled"
          (let [{:keys [findings suppressed-findings]}
                (clj-kondo/run! (assoc options :apply-suppressions true))]
            (is (empty? findings))
            (is (= [:unresolved-symbol]
                   (mapv :type suppressed-findings)))))
        (testing "the API does not generate suppressions"
          (is (thrown-with-msg?
               IllegalArgumentException
               #"only supported by the CLI"
               (clj-kondo/run! (assoc options :suppress-all true)))))))))

(deftest suppressions-cli-test
  (testing "suppression options are parsed"
    (is (= {:suppress-all true
            :suppress-rules [:unresolved-symbol :unused-binding]
            :suppressions-location "baseline.edn"
            :prune-suppressions true
            :pass-on-unpruned-suppressions true}
           (select-keys
            (#'main/parse-opts
             ["--suppress-all"
              "--suppress-rule" "unresolved-symbol"
              "--suppress-rule=unused-binding"
              "--suppressions-location" "baseline.edn"
              "--prune-suppressions"
              "--pass-on-unpruned-suppressions"])
            [:suppress-all
             :suppress-rules
             :suppressions-location
             :prune-suppressions
             :pass-on-unpruned-suppressions]))))
  (testing "conflicting suppression options are rejected"
    (doseq [[options message]
            [[["--suppress-all" "--suppress-rule" "unresolved-symbol"]
              "The --suppress-all and --suppress-rule options cannot be used together."]
             [["--suppress-all" "--prune-suppressions"]
              "The --prune-suppressions option cannot be used while generating suppressions."]
             [["--suppress-rule" "unresolved-symbol" "--prune-suppressions"]
              "The --prune-suppressions option cannot be used while generating suppressions."]]]
      (let [exit-code (atom nil)
            error-output (java.io.StringWriter.)]
        (binding [*err* error-output]
          (reset! exit-code
                  (apply main/main (concat ["--lint" "-"] options))))
        (is (= 2 @exit-code))
        (is (= (str message "\n") (str error-output))))))
  (testing "suppressed findings do not affect the exit code"
    (with-temp-dir
      (fn [dir]
        (let [source (fs/file dir "example.clj")
              baseline (str (fs/file dir "suppressions.edn"))
              options ["--lint" (str source)
                       "--suppressions-location" baseline]]
          (spit source "(y)")
          (with-out-str
            (is (zero?
                 (apply main/main (conj options "--suppress-all"))))
            (is (zero?
                 (apply main/main options)))))))))

(deftest unused-suppressions-cli-test
  (with-temp-dir
    (fn [dir]
      (let [source (fs/file dir "example.clj")
            baseline (fs/file dir "suppressions.edn")
            options ["--lint" (str source)
                     "--suppressions-location" (str baseline)]]
        (spit source "(x)")
        (with-out-str
          (is (zero? (apply main/main (conj options "--suppress-all")))))
        (spit source "(+ 1 2)")
        (let [error-output (java.io.StringWriter.)]
          (binding [*err* error-output]
            (with-out-str
              (is (= 2 (apply main/main options)))))
          (is (str/includes? (str error-output)
                             "There are suppressions left that do not occur anymore.")))
        (binding [*err* (java.io.StringWriter.)]
          (with-out-str
            (is (zero?
                 (apply main/main
                        (conj options "--pass-on-unpruned-suppressions"))))))))))
