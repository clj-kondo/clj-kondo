(ns probe
  (:require [babashka.fs :as fs]
            [clj-kondo.core :as clj-kondo]
            [clojure.string :as str]))

(def req "[clojure.spec.alpha :as s]")

(defn redef [res]
  (->> res :findings
       (filter #(= :redefined-spec (:type %)))
       (mapv #(select-keys % [:filename :row :col :message]))))

(defn lint [paths cache]
  (clj-kondo/run! {:lint (mapv str paths) :cache-dir (str cache)}))

(defn header [s] (println (str "\n===== " s)))

(defn probe-rename []
  (header "1. file renamed on disk -> stale index entry?")
  (fs/with-temp-dir [tmp {}]
    (let [cache (fs/file tmp ".cache")]
      (spit (fs/file tmp "a.clj") (str "(ns a (:require " req "))\n(s/def ::x string?)"))
      (println "lint a.clj:" (redef (lint [(fs/file tmp "a.clj")] cache)))
      (fs/move (fs/file tmp "a.clj") (fs/file tmp "b.clj"))
      (spit (fs/file tmp "b.clj") (str "(ns b (:require " req "))\n(s/def :a/x string?)"))
      (println "after rename a.clj->b.clj, lint b.clj:"
               (redef (lint [(fs/file tmp "b.clj")] cache))))))

(defn probe-path-spelling []
  (header "2. same file, two path spellings (relative vs absolute)")
  (fs/with-temp-dir [tmp {}]
    (let [cache (fs/file tmp ".cache")
          f (fs/file tmp "a.clj")]
      (spit f (str "(ns a (:require " req "))\n(s/def ::x string?)"))
      (println "abs path run :" (redef (lint [(fs/absolutize f)] cache)))
      ;; simulate an editor/CLI passing a different spelling of the same file
      (let [weird (str (fs/file tmp "." "a.clj"))]
        (println "'./' path run:" (redef (lint [weird] cache)))))))

(defn probe-deleted []
  (header "3. file deleted on disk -> index entry survives?")
  (fs/with-temp-dir [tmp {}]
    (let [cache (fs/file tmp ".cache")]
      (spit (fs/file tmp "shared.clj") "(ns shared)")
      (spit (fs/file tmp "a.clj") (str "(ns a (:require " req " [shared :as sh]))\n(s/def ::sh/x string?)"))
      (spit (fs/file tmp "b.clj") (str "(ns b (:require " req " [shared :as sh]))\n(s/def ::sh/x int?)"))
      (println "lint a.clj:" (redef (lint [(fs/file tmp "a.clj")] cache)))
      (fs/delete (fs/file tmp "a.clj"))
      (println "after deleting a.clj, lint b.clj:"
               (redef (lint [(fs/file tmp "b.clj")] cache))))))

(defn probe-symbol-def []
  (header "4. symbol-keyed s/def (docstring claims it is covered)")
  (fs/with-temp-dir [tmp {}]
    (let [cache (fs/file tmp ".cache")]
      (spit (fs/file tmp "a.clj")
            (str "(ns a (:require " req "))\n"
                 "(s/def a/thing string?)\n"
                 "(s/def a/thing int?)"))
      (println "duplicate symbol-keyed s/def:" (redef (lint [(fs/file tmp "a.clj")] cache)))
      (spit (fs/file tmp "c.clj")
            (str "(ns c (:require " req "))\n"
                 "(defn g [x] x)\n"
                 "(s/fdef g :args (s/cat :x int?))\n"
                 "(s/def c/g int?)"))
      (println "s/fdef g + symbol s/def c/g (same registry key):"
               (redef (lint [(fs/file tmp "c.clj")] cache))))))

(defn probe-no-specs []
  (header "5. project with zero specs: is the index still written?")
  (fs/with-temp-dir [tmp {}]
    (let [cache (fs/file tmp ".cache")]
      (spit (fs/file tmp "a.clj") "(ns a)\n(defn f [] 1)")
      (lint [(fs/file tmp "a.clj")] cache)
      (let [idx (fs/file cache "spec-index.transit.json")]
        (println "spec-index written:" (fs/exists? idx)
                 "size:" (when (fs/exists? idx) (fs/size idx))
                 "content:" (when (fs/exists? idx) (slurp idx)))))))

(defn probe-cache-leak []
  (header "6. does :spec-defs leak into the per-namespace cache files?")
  (fs/with-temp-dir [tmp {}]
    (let [cache (fs/file tmp ".cache")]
      (spit (fs/file tmp "a.clj") (str "(ns a (:require " req "))\n(s/def ::x string?)"))
      (lint [(fs/file tmp "a.clj")] cache)
      (doseq [f (fs/glob cache "**")
              :when (fs/regular-file? f)]
        (let [c (slurp (fs/file f))]
          (println (str (fs/file-name f)) "|" (fs/size f) "bytes | spec-defs present:"
                   (str/includes? c "spec-defs")))))))

(defn probe-ignore-external []
  (header "7. can a cross-run finding be suppressed at the original site?")
  (fs/with-temp-dir [tmp {}]
    (let [cache (fs/file tmp ".cache")]
      (spit (fs/file tmp "shared.clj") "(ns shared)")
      (spit (fs/file tmp "a.clj") (str "(ns a (:require " req " [shared :as sh]))\n(s/def ::sh/x string?)"))
      (spit (fs/file tmp "b.clj") (str "(ns b (:require " req " [shared :as sh]))\n(s/def ::sh/x int?)"))
      (lint [(fs/file tmp "a.clj")] cache)
      (println "b.clj alone (a from cache):" (redef (lint [(fs/file tmp "b.clj")] cache)))
      (println "whole dir in one run     :" (redef (lint [tmp] cache))))))

(defn -main [& _]
  (probe-rename)
  (probe-path-spelling)
  (probe-deleted)
  (probe-symbol-def)
  (probe-no-specs)
  (probe-cache-leak)
  (probe-ignore-external)
  (shutdown-agents))
