(ns clj-kondo.output-test
  (:require [cheshire.core :as cheshire]
            [clj-kondo.main :refer [main]]
            [clj-kondo.test-utils :as tu :refer
             [assert-submap assert-submap2]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(deftest output-test
  (is (str/starts-with?
       (with-in-str ""
         (with-out-str
           (main "--cache" "false" "--lint" "-" "--config" "{:output {:summary true}}")))
       "linting took"))
  (is (not
       (str/starts-with?
        (with-in-str ""
          (with-out-str
            (main "--cache" "false" "--lint" "-" "--config" "{:output {:summary false}}")))
        "linting took")))
  (is (= '({:filename "<stdin>",
            :row 1,
            :col 1,
            :level :error,
            :message "clojure.core/inc is called with 0 args but expects 1"}
           {:filename "<stdin>",
            :row 1,
            :col 6,
            :level :error,
            :message "clojure.core/dec is called with 0 args but expects 1"})
         (let [parse-fn
               (fn [line]
                 (when-let [[_ file row col level message]
                            (re-matches #"(.+):(\d+):(\d+): (\w+): (.*)" line)]
                   {:filename file
                    :row (Integer/parseInt row)
                    :col (Integer/parseInt col)
                    :level (keyword level)
                    :message message}))
               text (with-in-str "(inc)(dec)"
                      (with-out-str
                        (main "--cache" "false" "--lint" "-" "--config" "{:output {:format :text}}")))]
           (keep parse-fn (str/split-lines text)))))
  (doseq [[output-format parse-fn]
          [[:edn edn/read-string]
           [:json #(cheshire/parse-string % true)]]
          summary? [true false]]
    (let [output (with-in-str "(inc)(dec)"
                   (with-out-str
                     (main "--cache" "false" "--lint" "-" "--config"
                           (format "{:output {:format %s :summary %s}}"
                                   output-format summary?))))
          parsed (parse-fn output)]
      (assert-submap2 {:findings
                       [{:type (case output-format :edn :invalid-arity
                                     "invalid-arity"),
                         :filename "<stdin>",
                         :row 1,
                         :col 1,
                         :end-row 1,
                         :end-col 6,
                         :level (case output-format :edn :error
                                      "error"),
                         :message "clojure.core/inc is called with 0 args but expects 1"}
                        {:type (case output-format :edn :invalid-arity
                                     "invalid-arity"),
                         :filename "<stdin>",
                         :row 1,
                         :col 6,
                         :end-row 1,
                         :end-col 11,
                         :level (case output-format :edn :error
                                      "error"),
                         :message "clojure.core/dec is called with 0 args but expects 1"}]}
                      parsed)
      (if summary?
        (assert-submap '{:error 2}
                       (:summary parsed))
        (is (nil? (find parsed :summary))))))
  (doseq [[output-format parse-fn]
          [[:edn edn/read-string]
           [:json #(cheshire/parse-string % true)]]]
    (let [output (with-in-str "(inc)(dec)"
                   (with-out-str
                     (main "--cache" "false" "--lint" "-" "--config"
                           (format "{:output {:format %s}}" output-format))))
          parsed (parse-fn output)]
      (is (map? parsed))))
  (testing "JSON output escapes special characters"
    (let [output (with-in-str "{\"foo\" 1 \"foo\" 1}"
                   (with-out-str
                     (main "--cache" "false" "--lint" "-" "--config"
                           (format "{:output {:format %s}}" :json))))
          parsed (cheshire/parse-string output true)]
      (is (map? parsed)))
    (let [output (with-in-str "{:a 1}"
                   (with-out-str
                     (main "--cache" "false" "--lint" "\"foo\".clj" "--config"
                           (format "{:output {:format %s}}" :json))))
          parsed (cheshire/parse-string output true)]
      (is (map? parsed))))
  (testing "SARIF output"
    (let [output (with-in-str "{\"foo\" 1 \"foo\" 1}"
                   (with-out-str
                     (main "--cache" "false" "--lint" "-" "--config"
                           (format "{:output {:format %s}}" :json))))
          parsed (cheshire/parse-string output true)]
      (is (map? parsed)))))
