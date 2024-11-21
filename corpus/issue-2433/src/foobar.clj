(ns foobar)

;; a silly little macro to demonstrate a clj-kondo behaviour
(defmacro dingo [something & body]
  `(let [~something 42]
     ~@body))

(dingo moodog (println "hello"))

;; unused binding supressed, ignore is used
#_{:clj-kondo/ignore [:unused-binding]}
(dingo moodog (println "hello"))
