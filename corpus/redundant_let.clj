(ns redundant-let)

(let [x 1]
  (let [y 2]))

(let [x 1]
  #_(println "hello")
  (let [y 2]))

(let [x 1]
  ;; (println "hello")
  (let [y 2]))
