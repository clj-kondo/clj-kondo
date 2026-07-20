(ns clj-kondo.impl.types.utils-test
  (:require
   [clj-kondo.impl.types.utils :as tu]
   [clojure.test :refer [deftest is]]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

(def witnesses
  "Concrete values a tag can take at runtime. The specs below trust this
  table: a claim about a tag must hold for each of its witnesses."
  {:nil [nil]
   :false [false]
   :true [true]
   :boolean [true false]
   :truthy ["s" 1 []]
   :string ["" "s"]
   :int [0 1 -1]
   :keyword [:k]
   :vector [[] [1]]
   :map [{} {:a 1}]
   :seqable [nil [] (list 1)]
   :nilable/string [nil "s"]
   :nilable/int [nil 0]
   :nilable/false [nil false]
   :nilable/nil [nil]
   :any [nil false true 0 "s" :k [1]]})

(def unknown-witnesses
  "An unknown value can be anything."
  [nil false true 0 "s" :k [1]])

(def leaf-gen
  (gen/one-of [(gen/elements (keys witnesses))
               (gen/return {:type :map :val {}})
               ;; unknown tags: absent and an unresolved call marker
               (gen/return nil)
               (gen/return {:call {:name 'f :arity 0}})]))

(def tag-gen
  "A tag: a keyword, a map spec, a union, or a {:tag ..} wrapper as it occurs
  in arg-types entries and fold members."
  (gen/recursive-gen
   (fn [inner]
     (gen/one-of [(gen/set inner {:min-elements 1 :max-elements 3})
                  (gen/fmap (fn [t] {:tag t}) inner)]))
   leaf-gen))

(defn tag-witnesses [t]
  (cond (nil? t) unknown-witnesses
        (keyword? t) (witnesses t)
        (set? t) (mapcat tag-witnesses t)
        (map? t) (cond (identical? :map (:type t)) [{} {:a 1}]
                       (contains? t :tag) (tag-witnesses (:tag t))
                       :else unknown-witnesses)))

(defn- part-members [p]
  (cond (identical? :clj-kondo.impl.types.utils/nothing p) #{}
        (keyword? p) #{p}
        :else p))

(defn- run-or [vals]
  (loop [[v & more] vals]
    (if (seq more)
      (if v v (recur more))
      v)))

(defn- run-and [vals]
  (loop [[v & more] vals]
    (if (seq more)
      (if v (recur more) v)
      v)))

(defn- combos [tags]
  (reduce (fn [acc ws] (for [c acc, w ws] (conj c w)))
          [[]]
          (map tag-witnesses tags)))

(defspec truthiness-tag-shape 1000
  (prop/for-all [t tag-gen]
    (let [tt (tu/truthiness-tag t)]
      (or (nil? tt)
          (keyword? tt)
          (and (set? tt) (seq tt) (every? keyword? tt))))))

(defspec falsiness-predicates-sound 1000
  (prop/for-all [t tag-gen]
    (let [ws (tag-witnesses t)]
      (and (or (not (tu/always-falsy? t)) (not-any? identity ws))
           (or (not (tu/never-falsy? t)) (every? identity ws))
           (not (and (tu/always-falsy? t) (tu/never-falsy? t)))))))

(defspec falsy-part-covers-falsy-witnesses 1000
  (prop/for-all [t tag-gen]
    (let [members (part-members (tu/falsy-part t))
          ws (tag-witnesses t)]
      (and (every? #{:nil :false} members)
           (or (not-any? nil? ws) (contains? members :nil))
           (or (not-any? false? ws) (contains? members :false))))))

(defspec truthy-part-covers-truthy-witnesses 1000
  (prop/for-all [t tag-gen]
    (let [members (part-members (tu/truthy-part t))]
      (and (not-any? tu/falsy-keyword? members)
           (or (not-any? identity (tag-witnesses t))
               (seq members))))))

(defspec or-fold-agrees-with-evaluation 500
  (prop/for-all [tags (gen/vector tag-gen 1 3)]
    (let [folded (tu/fold-logic tags tu/never-falsy? tu/truthy-part)
          results (map run-or (combos tags))]
      (and (or (not (tu/never-falsy? folded)) (every? identity results))
           (or (not (tu/always-falsy? folded)) (not-any? identity results))))))

(defspec and-fold-agrees-with-evaluation 500
  (prop/for-all [tags (gen/vector tag-gen 1 3)]
    (let [folded (tu/fold-logic tags tu/always-falsy? tu/falsy-part)
          results (map run-and (combos tags))]
      (and (or (not (tu/never-falsy? folded)) (every? identity results))
           (or (not (tu/always-falsy? folded)) (not-any? identity results))))))

(defspec absorb-preserves-falsiness 1000
  (prop/for-all [t tag-gen]
    (let [a (tu/absorb t)]
      (and (= (tu/always-falsy? t) (tu/always-falsy? a))
           (= (tu/never-falsy? t) (tu/never-falsy? a))))))

(defspec union-type-commutes 1000
  (prop/for-all [x tag-gen
                 y tag-gen]
    (let [as-set #(if (set? %) % #{%})]
      (= (as-set (tu/union-type x y))
         (as-set (tu/union-type y x))))))

(deftest witness-table-self-check
  (doseq [[tag ws] witnesses]
    (is (seq ws) (str tag " has witnesses"))))
