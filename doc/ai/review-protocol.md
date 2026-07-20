# Agent review mailbox

Claude and codex exchange review findings through the shared nREPL instead of
through Michiel. State lives in the `review` namespace and mirrors to
`doc/ai/review-mailbox.edn` on every write.

## API

```clojure
(review/submit! {:from :codex            ;; or :claude
                 :severity :p2           ;; :p1 breaks users, :p2 wrong result, :p3 style/perf/docs
                 :file "src/clj_kondo/impl/types.clj"
                 :line 226
                 :claim "one sentence: what is wrong"
                 :repro "(types/match? :truthy :nil) ;=> true, expected false"})
(review/pending)                          ;; open items
(review/resolve! 3 {:status :verified})   ;; or :fixed :commit "sha", :rejected :reason "..", :acknowledged
```

## Rules

- One finding per submit!, with a REPL-verified :repro.
- Fixer replies by resolve! with :status :fixed and the :commit sha.
- Reviewer re-verifies the repro after :reload-all and sets :verified, or
  reopens with a fresh submit! that references the old :id.
- Codex reviews, Claude fixes: codex does not edit, commit or push.
- Poll (review/pending) at the start and end of every pass.

## Setup after a REPL restart

```clojure
(ns review
  (:require [clojure.java.io :as io] [clojure.pprint :as pp]
            [clojure.edn :as edn]))
(def ^:private mirror "doc/ai/review-mailbox.edn")
(defonce messages (atom (if (.exists (io/file mirror))
                          (edn/read-string (slurp mirror))
                          [])))
(defn- persist! [] (io/make-parents mirror) (spit mirror (with-out-str (pp/pprint @messages))))
(defn submit! [m]
  (let [id (count @messages)]
    (swap! messages conj (assoc m :id id :status :open))
    (persist!) id))
(defn pending [] (filterv #(= :open (:status %)) @messages))
(defn resolve! [id resolution]
  (swap! messages (fn [ms] (mapv #(if (= id (:id %)) (merge % resolution {:status (:status resolution :resolved)}) %) ms)))
  (persist!) (nth @messages id))
```
