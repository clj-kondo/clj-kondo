(ns clj-kondo.impl.types-test
  (:require
   [clj-kondo.impl.types :as types]
   [clojure.test :as t :refer [deftest is testing]]
   [clj-kondo.impl.clojure.spec.alpha :as s]
   [clojure.set :as set]))

(def all-types (set/union types/nilable-types types/other-types))

(defn all-used-types []
  (let [rels (merge types/is-a-relations types/could-be-relations)
        ks (keys rels)
        vs (reduce into #{} (vals rels))
        nilables (keys types/nilable->type)
        non-nilables (vals types/nilable->type)
        all (concat ks vs nilables non-nilables)]
    (set all)))

(deftest nilable-and-other-types-is-all-types
  ;; (prn (all-used-types))
  ;; (prn (set/difference (all-used-types) all-types))
  (doseq [t (all-used-types)]
    (is (s/get-spec t))))

