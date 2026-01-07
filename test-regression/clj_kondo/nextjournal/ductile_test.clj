(ns clj-kondo.nextjournal.ductile-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [assert-submaps2]]
   [clojure.edn :as edn]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest ductile-test
  (if (System/getenv "GITHUB_DUCTILE_PAT")
    (let [test-regression-checkouts (fs/file "test-regression" "checkouts")
          _ (fs/create-dirs test-regression-checkouts)
          dir (fs/file test-regression-checkouts "ductile")
          config-dir (fs/file dir ".clj-kondo")
          sha "6779c9dc7043e2494eb1667a37054bbd430d6c6e"]
      (when-not (fs/exists? dir)
        (p/shell {:dir test-regression-checkouts}
                 (str/replace "git clone --no-checkout --depth 1 https://x-access-token:$GITHUB_DUCTILE_PAT@github.com/nextjournal/ductile"
                              "$GITHUB_DUCTILE_PAT"
                              (System/getenv "GITHUB_DUCTILE_PAT"))))

      (p/shell {:dir dir} "git fetch --depth 1 origin" sha)
      (p/shell {:dir dir} "git fetch  --depth 1 origin" sha)
      (p/shell {:dir dir} "git checkout" sha "src" "resources" "dev" "test" "bb" ".clj-kondo" "deps.edn")
      (fs/delete-tree (fs/file config-dir ".cache"))
      (let [cp (-> (p/shell {:dir dir :out :string} "clojure -Spath") :out str/trim)]
        (clj-kondo/run! {:config-dir config-dir ;; important to pass this to set the right dir for copy-configs!
                         :copy-configs true
                         :lint [cp]
                         :dependencies true
                         :parallel true}))
      (let [paths (mapv #(str (fs/file dir %)) ["src" "test" "bb" "dev"])
            _ (clj-kondo/run! {:config-dir config-dir
                               :lint paths
                               :repro true})
            lint-result (clj-kondo/run! {:config-dir config-dir
                                         :lint paths
                                         :repro true})
            findings (:findings lint-result)
            _ (when (System/getenv "CLJ_KONDO_REGRESSION_UPDATE")
                (spit "test-regression/clj_kondo/nextjournal/ductile-findings.edn" (with-out-str (clojure.pprint/pprint findings))))
            expected (edn/read-string (slurp "test-regression/clj_kondo/nextjournal/ductile-findings.edn"))]
        (assert-submaps2 expected findings)))
    (println "GITHUB_DUCTILE_PAT not set, skipping ductile test")))
