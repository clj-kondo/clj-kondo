#!/usr/bin/env bb

(ns bump-graal-version
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]))

(defn display-help []
  (println (->> [""
                 "Bump the GraalVM version used to build clj-kondo."
                 ""
                 "  script/bump_graal_version.clj -g 25.0.4"
                 ""
                 "Use the JDK version of the GraalVM release, e.g. 25.0.4 for"
                 "GraalVM 25.2.4: that is what the download URL and the"
                 "setup-graalvm action expect."
                 ""
                 "Lines with a graalvm-pin comment are left alone. Mac intel is"
                 "pinned that way: 25.0.1 is the last GraalVM with macos-x64"
                 "builds."
                 ""]
                (str/join \newline))))

(def files-to-edit
  ["doc/build.md"
   ".circleci/config.yml"
   ".cirrus.yml"
   ".github/workflows/ci.yml"
   ".github/workflows/windows.yml"
   "script/install-graalvm"])

;; Every way a GraalVM version shows up in the files above. The version itself
;; never ends in a dot, so the period of "GraalVM 25.0.4." survives a bump. An
;; optional +build suffix is part of the version, as in graalvm-jdk-24+36.1.
(def version "(\\d+(?:\\.\\d+)*(?:\\+[\\d.]+)?)")

(def version-patterns
  (mapv #(re-pattern (str % version))
        ["(GRAALVM_VERSION:\\s*[\"']?)" ;; yaml env var
         "(GRAALVM_VERSION:-)"          ;; bash default
         "(java-version:\\s*[\"'])"    ;; setup-graalvm action
         "(graalvm_version:\\s*[\"'])" ;; ci matrix entry
         "(graalvm-jdk-)"               ;; unpacked dir in docs
         "(graalvm-)"                   ;; install dir
         "(Oracle GraalVM )"]))         ;; prose

(defn pinned? [line]
  (str/includes? line "graalvm-pin"))

(defn bump-line [line new-version]
  (reduce (fn [line pattern]
            (str/replace line pattern (fn [[_ prefix]] (str prefix new-version))))
          line
          version-patterns))

(defn bump-file [file new-version]
  (let [lines (str/split-lines (slurp file))
        bumped (map (fn [line]
                      (let [line' (if (pinned? line) line (bump-line line new-version))]
                        (when-not (= line line')
                          (println (str "  " file ": " (str/trim line'))))
                        line'))
                    lines)]
    (spit file (str (str/join \newline bumped) \newline))))

(def cl-options
  [["-g" "--graal VERSION" "GraalVM version, e.g. 25.0.4"]
   ["-h" "--help"]])

(let [{:keys [graal help]} (:options (cli/parse-opts *command-line-args* cl-options))]
  (if (or help (not graal))
    (display-help)
    (do (println "Bumping GraalVM to" graal)
        (run! #(bump-file % graal) files-to-edit)
        (println "Done. Check the diff: not every version in these files is a GraalVM version."))))
