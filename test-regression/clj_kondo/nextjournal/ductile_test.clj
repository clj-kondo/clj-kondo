(ns clj-kondo.nextjournal.ductile-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [assert-submaps2]]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest ductile-test
  (if (System/getenv "GITHUB_DUCTILE_PAT")
    (let [test-regression-checkouts (fs/file "test-regression" "checkouts")
          _ (fs/create-dirs test-regression-checkouts)
          dir (fs/file test-regression-checkouts "ductile")
          config-dir (fs/file dir ".clj-kondo")
          sha "1448fc66dad4550f716f35c6718ef03b0af94d9a"]
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
            findings (:findings lint-result)]
        (assert-submaps2
         []
         findings)))
    (println "GITHUB_DUCTILE_PAT not set, skipping ductile test")))
