;; invoke with: clojure -M benchmarks/dump-findings.clj <path> <out-file>
(require '[clj-kondo.core :as kondo])
(let [[path out] *command-line-args*
      result (kondo/run! {:lint [path] :cache false})]
  (spit out (with-out-str
              (doseq [f (sort-by (juxt :filename :row :col :type) (:findings result))]
                (prn (select-keys f [:filename :row :col :end-row :end-col :level :type :message])))))
  (println "wrote" out (count (:findings result))))
(shutdown-agents)
