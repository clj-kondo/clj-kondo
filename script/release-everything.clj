#!/usr/bin/env bb

;; Interactive driver for everything AFTER the GitHub release is published.
;; Run from the clj-kondo repo root:  bb script/release-everything.clj
;;
;; Assumes the downstream repos are checked out as siblings under the same
;; parent dir as clj-kondo: homebrew-brew, pod-registry, clj-kondo-bb,
;; lein-clj-kondo.
;;
;; Each step waits for input:  [Enter]=run · s=skip · q=quit
;; The released version is read from resources/CLJ_KONDO_VERSION (must NOT yet
;; be a -SNAPSHOT; the post-release step below flips it to the next SNAPSHOT).

(require '[babashka.fs :as fs]
         '[babashka.http-client :as http]
         '[babashka.process :refer [shell]]
         '[cheshire.core :as json]
         '[clojure.string :as str])

(def clj-kondo-root
  (-> (System/getProperty "babashka.file") fs/absolutize fs/parent fs/parent str))
(def dev (str (fs/parent clj-kondo-root)))
(defn repo [name] (str (fs/path dev name)))

(def V (str/trim (slurp (str (fs/path clj-kondo-root "resources/CLJ_KONDO_VERSION")))))

;; ---- helpers ---------------------------------------------------------------

(defn sh  [dir & args] (apply shell {:dir dir} args))
(defn sh* [dir & args] (apply shell {:dir dir :out :string :err :string :continue true} args))
(defn out [dir & args] (str/trim (:out (apply shell {:dir dir :out :string} args))))

(defn ok   [msg] (println (str "   [OK] " msg)))
(defn warn [msg] (println (str "   [WARN] " msg)))
(defn die  [msg] (println (str "   [FAIL] " msg)) (System/exit 1))

(defn commit! [dir msg]
  (if (seq (out dir "git" "status" "--porcelain"))
    (sh dir "git" "commit" "-m" msg)
    (warn "nothing to commit (already done?)")))

(defn replace-in-file! [file regex replacement]
  (let [content (slurp file)
        updated (str/replace content regex replacement)]
    (when (= content updated) (warn (str "no change in " (fs/file-name file))))
    (spit file updated)))

(defn clojars-latest [coords]
  (-> (http/get (str "https://clojars.org/api/artifacts/" coords))
      :body (json/parse-string true) :latest_release))

(defn step [title action]
  (println)
  (println (str "> " title))
  (print "   [Enter]=run  s=skip  q=quit > ") (flush)
  (let [in (str/trim (or (read-line) ""))]
    (case in
      "q" (do (println "Aborted by user.") (System/exit 0))
      "s" (println "   skipped")
      (action))))

;; ---- steps -----------------------------------------------------------------

(defn preflight []
  (println "> Preflight")
  (let [{:keys [out exit]} (sh* clj-kondo-root "gh" "release" "view" (str "v" V)
                                "--json" "isDraft")]
    (cond
      (not (zero? exit)) (die (str "GitHub release v" V
                                   " not found/readable - publish it first (or check gh auth)."))
      (:isDraft (json/parse-string out true)) (die (str "GitHub release v" V
                                                        " is still a DRAFT - publish it first.")))
    (ok (str "GitHub release v" V " published")))
  (let [latest (clojars-latest "clj-kondo/clj-kondo")]
    (if (= latest V)
      (ok (str "Clojars clj-kondo/clj-kondo = " V))
      (warn (str "Clojars latest = " latest " (expected " V ") - CI may still be publishing")))))

(defn homebrew []
  (let [d (repo "homebrew-brew")]
    (sh d "./update-clj-kondo")
    (if (str/includes? (slurp (str (fs/path d "clj-kondo.rb"))) (str "version \"" V "\""))
      (ok (str "clj-kondo.rb -> " V)) (die "clj-kondo.rb missing version"))
    (sh d "git" "add" "clj-kondo.rb")
    (commit! d "clj-kondo")
    (sh d "git" "push")
    (ok "homebrew pushed")))

(defn pod []
  (let [d (repo "pod-registry")]
    (sh d "git" "pull" "--rebase")                       ; shared repo: pull first
    (sh d "bb" "script/upgrade-manifest.clj" "clj-kondo/clj-kondo" V)
    (sh d "bb" "script/derive-manifests.clj")            ; regenerates README table
    (if (fs/exists? (fs/path d "manifests/clj-kondo/clj-kondo" V "manifest.edn"))
      (ok (str "manifest " V " created")) (die "manifest not created"))
    (sh d "git" "add" (str "manifests/clj-kondo/clj-kondo/" V) "README.md")
    (commit! d "clj-kondo")
    (sh d "git" "push")
    (ok "pod-registry pushed")))

(defn post-release []
  (sh clj-kondo-root "script/bump_version" "post-release")
  (let [released (str/trim (slurp (str (fs/path clj-kondo-root "resources/CLJ_KONDO_RELEASED_VERSION"))))]
    (if (= released V) (ok (str "CLJ_KONDO_RELEASED_VERSION = " V))
        (die (str "RELEASED_VERSION = " released ", expected " V))))
  (sh clj-kondo-root "git" "add"
      "resources/CLJ_KONDO_VERSION" "resources/CLJ_KONDO_RELEASED_VERSION"
      "project.clj" ".pre-commit-hooks.yaml")
  (commit! clj-kondo-root "Bump")
  (sh clj-kondo-root "git" "push")
  (ok "clj-kondo post-release pushed"))

(defn clj-kondo-bb []
  (let [d (repo "clj-kondo-bb")
        f (str (fs/path d "src/clj_kondo/core.bb"))]
    (sh d "git" "pull" "--rebase")
    (replace-in-file! f #"\(pods/load-pod 'clj-kondo/clj-kondo \"[^\"]+\"\)"
                      (str "(pods/load-pod 'clj-kondo/clj-kondo \"" V "\")"))
    (if (str/includes? (slurp f) (str "\"" V "\"")) (ok "core.bb bumped") (die "core.bb not bumped"))
    (sh d "git" "add" "src/clj_kondo/core.bb")
    (commit! d V)
    (sh d "git" "push")                                  ; branch: main
    (ok "clj-kondo-bb pushed")))

(defn lein-clj-kondo []
  (let [d (repo "lein-clj-kondo")
        f (str (fs/path d "project.clj"))]
    (sh d "git" "pull" "--rebase")
    ;; bump BOTH the plugin version and the clj-kondo dependency, commit on a
    ;; CLEAN tree, THEN `bb tag` (it runs `git pull` internally which rebases
    ;; under pull.rebase=true and dies on a dirty tree).
    (replace-in-file! f #"com\.github\.clj-kondo/lein-clj-kondo \"[^\"]+\""
                      (str "com.github.clj-kondo/lein-clj-kondo \"" V "\""))
    (replace-in-file! f #"\[clj-kondo/clj-kondo \"[^\"]+\"\]"
                      (str "[clj-kondo/clj-kondo \"" V "\"]"))
    (sh d "git" "add" "project.clj")
    (commit! d "Bump versions")
    (sh d "bb" "tag" V)                                  ; CHANGELOG + tag + push; CI deploys
    (ok "lein-clj-kondo tagged; CI deploying to Clojars")
    (loop [n 12]
      (let [latest (clojars-latest "com.github.clj-kondo/lein-clj-kondo")]
        (cond
          (= latest V) (ok (str "Clojars lein-clj-kondo = " V))
          (zero? n)    (warn (str "Clojars still " latest " - check: gh run list (in " d ")"))
          :else (do (print "   ... waiting for Clojars deploy\r") (flush)
                    (Thread/sleep 10000) (recur (dec n))))))))

;; ---- main ------------------------------------------------------------------

(when (str/includes? V "SNAPSHOT")
  (die (str "CLJ_KONDO_VERSION is " V " - post-release already ran. Nothing to do.")))

(println "=== clj-kondo release-everything ===")
(println "Version:" V "  | downstream repos under" dev)

(preflight)
(step "Homebrew: update formula + push"                 homebrew)
(step "Pod registry: add manifest + regenerate + push"  pod)
(step "clj-kondo-bb: bump pod version + push"           clj-kondo-bb)
(step "lein-clj-kondo: bump + tag + Clojars deploy"     lein-clj-kondo)
;; post-release LAST: it flips CLJ_KONDO_VERSION to the next -SNAPSHOT, which
;; the startup guard treats as "already released". Keeping it last means every
;; downstream step above runs while the version is still $V, and an interrupted
;; run can be safely re-run/resumed until this point.
(step "clj-kondo: post-release bump + push (version-closing, do last)" post-release)

(println)
(println "Done.")
