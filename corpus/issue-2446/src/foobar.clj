(ns foobar
  (:require [com.climate.claypoole :as cp]))

(defn without-ignore-claypoole
  [& _args]
  (let [pool (cp/threadpool 2)]
    ;; value should not be reported as unused value
    (cp/upfor pool [n 5]
              (fn [] (prn {:n n})))
    :do-other-work))

(defn with-ignore-claypoole
  "info: Redundant ignore"
  [& _args]
  (let [pool (cp/threadpool 2)]
    (cp/upfor pool [n 5]
              #_(fn [] (prn {:n n})))
    #_{:clj-kondo/ignore [:unused-binding]}
    (cp/upfor pool [n 5]
              #_(fn [] (prn {:n n})))
    :do-other-work))
