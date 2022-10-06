(ns clj-kondo.release-artifact
  (:require [borkdude.gh-release-artifact :as ghr]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(defn current-branch []
  (or (System/getenv "APPVEYOR_PULL_REQUEST_HEAD_REPO_BRANCH")
      (System/getenv "APPVEYOR_REPO_BRANCH")
      (System/getenv "CIRCLE_BRANCH")
      (-> (sh "git" "rev-parse" "--abbrev-ref" "HEAD")
          :out
          str/trim)))

(defn release [& args]
  (let [current-version (-> (slurp "resources/CLJ_KONDO_VERSION")
                            str/trim)
        ght (System/getenv "GITHUB_TOKEN")
        file (first args)
        branch (current-branch)
        release-branch? (contains? #{"master" "main"} branch)]
    (when-not ght
      (println "Skipping: not GITHUB_TOKEN"))
    (when-not release-branch?
      (println "Skipping: not on release branch"))
    (if (and ght release-branch?)
      (do (assert file "File name must be provided")
          (ghr/overwrite-asset {:org "clj-kondo"
                                :repo "clj-kondo"
                                :file file
                                :tag (str "v" current-version)
                                :overwrite (str/ends-with? current-version "SNAPSHOT")
                                :sha256 true}))
      (println "Skipping release artifact (no GITHUB_TOKEN or not on main branch)"))
    nil))
