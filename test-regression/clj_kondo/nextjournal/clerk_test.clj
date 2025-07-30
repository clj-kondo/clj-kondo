(ns clj-kondo.nextjournal.clerk-test
  (:require
   [babashka.fs :as fs]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [assert-submaps2]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest clerk-test
  (let [libname 'io.github.nextjournal/clerk
        git-sha "b0fd4458a1af7f3ecd95e0cde41761ac297e24f5"
        git-dir (format ".gitlibs/libs/%s/%s" libname git-sha)
        git-dir (fs/file (fs/home) git-dir)
        paths (mapv #(str (fs/file git-dir %)) ["src" "test" "bb"])
        config-dir (fs/file git-dir ".clj-kondo")
        lint-result (clj-kondo/run! {:config-dir config-dir
                                     :lint paths
                                     :repro true})]
    (assert-submaps2
     []
     (:findings lint-result))))
