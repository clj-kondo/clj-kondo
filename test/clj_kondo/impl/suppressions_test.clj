(ns clj-kondo.impl.suppressions-test
  (:require
   [babashka.fs :as fs]
   [clj-kondo.impl.suppressions :as suppressions]
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is testing]]))

(def findings
  [{:filename "src/example.clj"
    :row 1
    :col 1
    :type :unresolved-symbol
    :message "Unresolved symbol: x"}
   {:filename "src/example.clj"
    :row 3
    :col 5
    :type :unused-binding
    :message "unused binding y"}
   {:filename "src/example.clj"
    :row 5
    :col 5
    :type :unused-binding
    :message "unused binding y"}])

(defn- with-temp-dir [f]
  (let [dir (fs/create-temp-dir)]
    (try
      (f dir)
      (finally
        (fs/delete-tree dir)))))

(deftest findings->entries-test
  (is (= [{:filename "src/example.clj"
           :type :unresolved-symbol
           :message "Unresolved symbol: x"
           :count 1}
          {:filename "src/example.clj"
           :type :unused-binding
           :message "unused binding y"
           :count 2}]
         (suppressions/findings->entries findings))))

(deftest normalize-filename-test
  (with-temp-dir
    (fn [dir]
      (let [filename (str (fs/file dir "src" "example.clj"))
            finding (assoc (first findings) :filename filename)
            entries (suppressions/findings->entries [finding] dir)]
        (is (= "src/example.clj"
               (suppressions/normalize-filename dir filename)))
        (is (= "src/example.clj" (:filename (first entries))))
        (is (empty? (:findings
                     (suppressions/apply-suppressions [finding] entries dir))))))))

(deftest replace-suppressions-test
  (let [other-file-entry {:filename "src/other.clj"
                          :type :unresolved-symbol
                          :message "Unresolved symbol: existing"
                          :count 1}
        entries (conj (suppressions/findings->entries findings) other-file-entry)
        current-findings [(assoc (first findings)
                                 :message "Unresolved symbol: z")]]
    (testing "only generated file and linter scopes are replaced"
      (is (= [{:filename "src/example.clj"
               :type :unresolved-symbol
               :message "Unresolved symbol: z"
               :count 1}
              {:filename "src/example.clj"
               :type :unused-binding
               :message "unused binding y"
               :count 2}
              other-file-entry]
             (suppressions/replace-suppressions entries current-findings nil))))
    (testing "only selected rules are replaced"
      (is (= [{:filename "src/example.clj"
               :type :unresolved-symbol
               :message "Unresolved symbol: z"
               :count 1}
              {:filename "src/example.clj"
               :type :unused-binding
               :message "unused binding y"
               :count 2}
              other-file-entry]
             (suppressions/replace-suppressions
              entries
              current-findings
              [:unresolved-symbol]))))))

(deftest apply-and-prune-suppressions-test
  (let [entries (suppressions/findings->entries findings)
        current-findings [(assoc (first findings) :row 20)
                          (second findings)
                          {:filename "src/example.clj"
                           :row 7
                           :col 1
                           :type :unresolved-symbol
                           :message "Unresolved symbol: z"}]
        {:keys [findings used]}
        (suppressions/apply-suppressions current-findings entries)]
    (testing "matching does not depend on row and column"
      (is (= [(last current-findings)] findings)))
    (testing "pruning removes unused entries and updates counts"
      (is (= [{:filename "src/example.clj"
               :type :unresolved-symbol
               :message "Unresolved symbol: x"
               :count 1}
              {:filename "src/example.clj"
               :type :unused-binding
               :message "unused binding y"
               :count 1}]
             (suppressions/prune-suppressions entries used))))))

(deftest suppressions-file-test
  (with-temp-dir
    (fn [dir]
      (let [file (fs/file dir "nested" "suppressions.edn")
            entries (suppressions/findings->entries findings)]
        (suppressions/write-suppressions! file entries)
        (is (= {:version 1
                :suppressions entries}
               (edn/read-string (slurp file))))
        (is (= entries (suppressions/read-suppressions file)))))))
