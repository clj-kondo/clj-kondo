(ns clj-kondo.impl.types-test
  (:require
   ;; [clj-kondo.impl.clojure.spec.alpha :as s]
   [clj-kondo.impl.types :as types]
   [clojure.test :as t :refer [deftest is testing]]))

(comment
  (defn all-used-types []
    (let [rels (merge types/is-a-relations types/could-be-relations)
          ks (keys rels)
          vs (reduce into #{} (vals rels))
          nilables (keys types/nilable->type)
          non-nilables (vals types/nilable->type)
          all (concat ks vs nilables non-nilables)]
      (set all)))

  (deftest all-used-types-have-specs
    (doseq [t (all-used-types)]
      (is (s/get-spec t))))

  (defn keywords-from-spec
    [spec]
    (filter (fn [k]
              (when (keyword? k)
                (= "clj-kondo.impl.types"
                   (namespace k))))
            (tree-seq seq? identity (or (:form spec) (s/form spec)))))

  (deftest specs-refer-to-known-specs
    (doseq [[_ns ns-specs] types/specs
            [_var-name spec] ns-specs
            spec [(:args spec) (:ret spec) (meta (:fn spec))]
            :when (do nil #_(prn spec) spec)
            k (keywords-from-spec spec)]
      (is (s/get-spec k)))))
