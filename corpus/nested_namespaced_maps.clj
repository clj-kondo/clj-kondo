(ns nested-namespaced-maps
  (:require [clojure.test :as it]))

(defn test-fn
  [map]
  (println #::it {:a #::it {}}))

(test-fn #::it{:a 1}) ;; correct
(test-fn #::it{:a 1} 1) ;; invalid

#::it{:a 1 :a 2}
