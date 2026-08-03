(ns probe5
  (:require [babashka.fs :as fs] [clj-kondo.core :as clj-kondo]))
(def req "[clojure.spec.alpha :as s]")
(defn redef [res] (->> res :findings (filter #(= :redefined-spec (:type %))) (mapv :message)))
(defn lint-str [s & [cfg]]
  (with-in-str s (clj-kondo/run! {:lint ["-"] :cache false :config (or cfg {})})))
(defn -main [& _]
  (println "comment form:"
           (redef (lint-str (str "(ns foo (:require " req "))\n(s/def ::x string?)\n(comment (s/def ::x int?))"))))
  (println "when-false branch:"
           (redef (lint-str (str "(ns foo (:require " req "))\n(s/def ::x string?)\n(when false (s/def ::x int?))"))))
  (println "config-in-ns off:"
           (redef (lint-str (str "(ns foo (:require " req "))\n(s/def ::x string?)\n(s/def ::x int?)")
                            {:config-in-ns {'foo {:linters {:redefined-spec {:level :off}}}}})))
  (println "ignore at original site (not the reported one):"
           (redef (lint-str (str "(ns foo (:require " req "))\n#_{:clj-kondo/ignore [:redefined-spec]}\n(s/def ::x string?)\n(s/def ::x int?)"))))
  (shutdown-agents))
