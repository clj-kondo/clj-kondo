(ns clj-kondo.nextjournal.clerk-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [assert-submaps2]]
   [clojure.edn :as edn]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest clerk-test
  (let [test-regression-checkouts (fs/file "test-regression" "checkouts")
        _ (fs/create-dirs test-regression-checkouts)
        dir (fs/file test-regression-checkouts "clerk")
        config-dir (fs/file dir ".clj-kondo")
        sha "b0fd4458a1af7f3ecd95e0cde41761ac297e24f5"]
    (when-not (fs/exists? dir)
      (p/shell {:dir test-regression-checkouts} "git clone --no-checkout --depth 1 https://github.com/nextjournal/clerk"))

    (p/shell {:dir dir} "git fetch --depth 1 origin" sha)
    (p/shell {:dir dir} "git fetch  --depth 1 origin" sha)
    (p/shell {:dir dir} "git checkout" sha "src" "resources" "test" "bb" ".clj-kondo" "deps.edn")
    (let [cp (-> (p/shell {:dir dir :out :string} "clojure -Spath") :out str/trim)]
      (clj-kondo/run! {:config-dir config-dir ;; important to pass this to set the right dir for copy-configs!
                       :copy-configs true
                       :lint [cp]
                       :dependencies true
                       :parallel true}))
    (let [paths (mapv #(str (fs/file dir %)) ["src" "test" "bb"])
          lint-result (clj-kondo/run! {:config-dir config-dir
                                       ;; enable extra linters here that we want to test
                                       :config {:linters {:redundant-let-binding {:level :warning}
                                                          :redundant-fn-wrapper {:level :warning}}}
                                       :lint paths
                                       :repro true})
          findings (:findings lint-result)
          _ (when (System/getenv "CLJ_KONDO_REGRESSION_UPDATE")
              (spit "test-regression/clj_kondo/nextjournal/clerk-findings.edn" (with-out-str (pp/pprint findings))))
          expected (edn/read-string (slurp "test-regression/clj_kondo/nextjournal/clerk-findings.edn"))]
      (assert-submaps2 expected findings))))
