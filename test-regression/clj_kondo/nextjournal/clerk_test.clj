(ns clj-kondo.nextjournal.clerk-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clj-kondo.core :as clj-kondo]
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
    (let [paths (mapv #(str (fs/file dir %)) ["src" "test" "bb"])
          lint-result (clj-kondo/run! {:config-dir config-dir
                                       :lint paths
                                       :repro true})]
      (prn (:summary lint-result))
      (is (empty? (:findings lint-result))))))
