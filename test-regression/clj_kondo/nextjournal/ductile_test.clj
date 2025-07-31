(ns clj-kondo.nextjournal.ductile-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clj-kondo.core :as clj-kondo]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest clerk-test
  (let [test-regression-checkouts (fs/file "test-regression" "checkouts")
        _ (fs/create-dirs test-regression-checkouts)
        dir (fs/file test-regression-checkouts "ductile")
        config-dir (fs/file dir ".clj-kondo")
        sha "1448fc66dad4550f716f35c6718ef03b0af94d9a"]
    (when-not (fs/exists? dir)
      (p/shell {:dir test-regression-checkouts} "git clone --no-checkout --depth 1 ssh://git@github.com/nextjournal/ductile"))

    (p/shell {:dir dir} "git fetch --depth 1 origin" sha)
    (p/shell {:dir dir} "git fetch  --depth 1 origin" sha)
    (p/shell {:dir dir} "git checkout" sha "src" "resources" "dev" "test" "bb" ".clj-kondo" "deps.edn")
    (let [paths (mapv #(str (fs/file dir %)) ["src" "test" "bb" "dev"])
          lint-result (clj-kondo/run! {:config-dir config-dir
                                       :lint paths
                                       :repro true})]
      (prn (:summary lint-result))
      (is (empty? (:findings lint-result))))))
