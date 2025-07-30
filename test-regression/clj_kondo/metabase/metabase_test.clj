(ns clj-kondo.metabase.metabase-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [assert-submaps2]]
   [clojure.edn :as edn]
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
    (let [cp (-> (p/shell {:dir dir :out :string} "clojure -Spath") :out str/trim)]
      (clj-kondo/run! {:config-dir config-dir ;; important to pass this to set the right dir for copy-configs!
                       :copy-configs true
                       :lint [cp]}))
    (let [paths (mapv #(str (fs/file dir %)) ["src" "test"])
          lint-result (clj-kondo/run! {:config-dir config-dir
                                       :lint paths
                                       :repro true})
          findings (:findings lint-result)
          expected (edn/read-string (slurp "test-regression/clj_kondo/metabase/findings.edn"))]
      #_(spit "test-regression/clj_kondo/metabase/findings.edn" (with-out-str (clojure.pprint/pprint findings)))
      (assert-submaps2 expected findings))))
