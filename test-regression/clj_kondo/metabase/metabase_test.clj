(ns clj-kondo.metabase.metabase-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [assert-submaps2]]
   [clojure.edn :as edn]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest metabase-test
  (let [test-regression-checkouts (fs/file "test-regression" "checkouts")
        _ (fs/create-dirs test-regression-checkouts)
        dir (fs/file test-regression-checkouts "metabase")
        config-dir (fs/file dir ".clj-kondo")]
    (when-not (fs/exists? dir)
      (p/shell {:dir test-regression-checkouts} "git clone --no-checkout --depth 1 https://github.com/metabase/metabase"))
    (p/shell {:dir dir} "git fetch --depth 1 origin" "aa0cdb546d7c9e4ef5c52ad23c656272b7599e23")
    (p/shell {:dir dir} "git fetch  --depth 1 origin" "aa0cdb546d7c9e4ef5c52ad23c656272b7599e23")
    (p/shell {:dir dir} "git checkout aa0cdb546d7c9e4ef5c52ad23c656272b7599e23 src test .clj-kondo deps.edn")
    (fs/delete-tree (fs/file config-dir ".cache"))
    (let [cp (-> (p/shell {:dir dir :out :string} "clojure -Spath") :out str/trim)]
      (clj-kondo/run! {:config-dir config-dir ;; important to pass this to set the right dir for copy-configs!
                       :copy-configs true
                       :lint [cp]
                       :dependencies true
                       :parallel true}))
    (let [paths (mapv #(str (fs/file dir %)) ["src" "test"])
          lint-result (clj-kondo/run! {:config-dir config-dir
                                       :lint paths
                                       :repro true
                                       ;; enable extra linters here that we want to test
                                       :config {:linters {:redundant-let-binding {:level :warning}
                                                          :redundant-primitive-coercion {:level :warning}}}})
          findings (:findings lint-result)
          ;; Uncomment this to reset expected findings:
          _ (when (System/getenv "CLJ_KONDO_REGRESSION_UPDATE")
              (spit "test-regression/clj_kondo/metabase/findings.edn" (with-out-str (clojure.pprint/pprint findings))))
          expected (edn/read-string (slurp "test-regression/clj_kondo/metabase/findings.edn"))]
      (when false
        (println "FINDINGS")
        (pp/pprint findings)
        (println "---------"))
      (assert-submaps2 expected findings))))
