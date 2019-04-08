(ns nested-namespaced-maps-workaround)

(defn test-fn
  [map]
  (println #::it {:a #::it {}}))

(test-fn #::it{:a 1}) ;; correct
(test-fn #::it{:a 1} 1) ;; invalid
