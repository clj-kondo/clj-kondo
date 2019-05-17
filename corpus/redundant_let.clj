(ns redundant-let)

(let [x 1]
  (let [y 2]))

(let [x 1]
  #_(println "hello")
  (let [y 2]))

(let [x 1]
  ;; (println "hello")
  (let [y 2]))

;; this one should not be reported:
(defn two-cases []
  (let [resource 1]
    (let [result [1 (inc resource) 3]]
      (println (= 2 (count result))))
    (let [result [1 (inc resource)]]
      (println (= 3 (count result))))))
