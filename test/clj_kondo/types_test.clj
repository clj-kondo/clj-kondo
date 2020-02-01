(ns clj-kondo.types-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest type-mismatch-test
  (assert-submaps
   '({:row 1,
      :col 6,
      :message "Expected: number, received: string."})
   (lint! "(inc \"foo\")"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:row 1,
      :col 7,
      :message "Expected: string, received: number."})
   (lint! "(subs (inc 1) 1)"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 25,
      :level :error,
      :message "Expected: number, received: string."})
   (lint! "(let [x \"foo\" y x] (inc y))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 19,
      :level :error,
      :message "Expected: string, received: positive integer."}
     {:file "<stdin>",
      :row 1,
      :col 30,
      :level :error,
      :message "Expected: number, received: string."})
   (lint! "(let [x 1 y (subs x 1)] (inc y))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 19,
      :level :error,
      :message "Expected: atom, received: positive integer."})
   (lint! "(let [x 1] (swap! x identity))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 32,
      :level :error,
      :message "Expected: seqable collection, received: transducer."})
   (lint! "(let [x (map (fn []))] (cons 1 x))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 2,
      :col 28,
      :level :error,
      :message "Expected: set or nil, received: seq."}
     {:file "<stdin>",
      :row 3,
      :col 28,
      :level :error,
      :message "Expected: set or nil, received: vector."})
   (lint! "(require '[clojure.set :as set])
           (set/difference (map inc [1 2 3]) #{1 2 3})
           (set/difference (into [] [1 2 3]) #{1 2 3})"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 2,
      :col 30,
      :level :error,
      :message "Expected: char sequence, received: positive integer."}
     {:file "<stdin>",
      :row 3,
      :col 46,
      :level :error,
      :message "Expected: char sequence, received: positive integer."})
   (lint! "(require '[clojure.string :as str])
           (str/starts-with? 1 \"s\")
           (str/includes? (str/join [1 2 3]) 1)"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 9,
      :level :error,
      :message "Expected: vector, received: seq."})
   (lint! "(subvec (map inc [1 2 3]) 10 20)"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :error,
      :message
      "Expected: stack (list, vector, etc.), received: set."})
   (lint! "(peek #{:a :b :c})"
          {:linters {:type-mismatch {:level :error}}}))
  (testing "No type checking if invalid-arity is disabled"
    (assert-submaps
     '({:file "corpus/types/insufficient.clj"
        :row 6
        :col 11
        :level :error
        :message "Expected: string or nil, received: keyword."}
       {:file "corpus/types/insufficient.clj"
        :row 10
        :col 11
        :level :error})
     (lint! (io/file "corpus" "types" "insufficient.clj")
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "corpus/types/insufficient.clj"
        :row 6
        :col 11
        :level :error,
        :message "Expected: string or nil, received: keyword."})
     (lint! (io/file "corpus" "types" "insufficient.clj")
            {:linters {:invalid-arity {:skip-args ['corpus.types.insufficient/my-macro]}
                       :type-mismatch {:level :error}}}))
    (is (empty?
         (lint! (io/file "corpus" "types" "insufficient.clj")
                {:linters {:invalid-arity {:level :off}
                           :type-mismatch {:level :error}}}))))
  (testing "CLJS also works"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 6,
        :level :error,
        :message "Expected: number, received: string."}
       {:file "<stdin>",
        :row 1,
        :col 57,
        :level :error,
        :message "Expected: set or nil, received: positive integer."})
     (lint! "(inc \"foo\") (require '[clojure.set :as set]) (set/union 1)"
            {:linters {:type-mismatch {:level :error}}}
            "--lang" "cljs")))
  (testing "leveraging type hints"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 22,
        :level :error,
        :message #"Expected: number"})
     (lint! "(fn [^String x] (inc x))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 21,
        :level :error,
        :message "Expected: string, received: integer."})
     (lint! "(fn [^long x] (subs x 1 1))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 73,
        :level :error,
        :message "Expected: number, received: string or nil."}
       {:file "<stdin>",
        :row 1,
        :col 85,
        :level :error,
        :message "Expected: number, received: string or nil."})
     (lint! "(defn foo (^String []) (^long [x]) ([x y]) (^String [x y z & xs])) (inc (foo)) (inc (foo 1 2 3 4))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 73,
        :level :error,
        :message #"Expected: number"})
     (lint! "(defn foo (^String []) (^long [x]) ([x y]) (^String [x y z & xs])) (inc (foo))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 31,
        :level :error,
        :message "Expected: string or nil, received: positive integer."})
     (lint! "(defn foo [^String x] x) (foo 1)"
            {:linters {:type-mismatch {:level :error}}})))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :error,
      :message "Expected: string, received: nil."})
   (lint! "(subs nil 1 2)"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 39,
      :level :error,
      :message "Expected: number, received: set or nil."})
   (lint! "(require '[clojure.set :as set]) (inc (set/union nil))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 31,
      :level :error,
      :message "Expected: seqable collection, received: number or nil."})
   (lint! "(defn foo [^Number x] (cons 1 x))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 6,
      :level :error,
      :message "Expected: number, received: list."})
   (lint! "(inc ())"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '[{:file "<stdin>", :row 1, :col 9, :level :error, :message "Expected: seqable collection, received: positive integer."}]
   (lint! "(empty? 1)"
          {:linters {:type-mismatch {:level :error}}}))
  (testing "Insufficient input"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 15,
        :level :error,
        :message "Insufficient input."})
     (lint! "(assoc {} 1 2 3)"
            {:linters {:type-mismatch {:level :error}}})))
  (testing "handle multiple errors"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 7,
        :level :error,
        :message "Expected: string, received: nil."}
       {:file "<stdin>",
        :row 1,
        :col 11,
        :level :error,
        :message "Expected: natural integer, received: nil."}
       {:file "<stdin>",
        :row 1,
        :col 15,
        :level :error,
        :message "Expected: natural integer, received: nil."})
     (lint! "(subs nil nil nil)"
            {:linters {:type-mismatch {:level :error}}})))
  (testing "map spec"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 34,
        :level :error,
        :message "Missing required key: :b"}
       {:file "<stdin>",
        :row 1,
        :col 38,
        :level :error,
        :message "Expected: string, received: positive integer."}
       {:file "<stdin>",
        :row 1,
        :col 41,
        :level :error,
        :message "Expected: string, received: positive integer."}
       {:file "<stdin>",
        :row 2,
        :col 45,
        :level :error,
        :message "Expected: string, received: positive integer."}
       {:file "<stdin>",
        :row 3,
        :col 36,
        :level :error,
        :message "Expected: map, received: string."}
       {:file "<stdin>",
        :row 3,
        :col 44,
        :level :error,
        :message "Expected: string, received: positive integer."}
       {:file "<stdin>",
        :row 4,
        :col 37,
        :level :error,
        :message "Expected: string, received: positive integer."})
     (lint! "(ns foo) (defn foo [_x _y]) (foo {:a 1} 1) ;; a should be a string, :b is missing
             (defn bar [x] x) (foo (bar {}) 1) ;; no false positive for this one
             (defn baz [x] x) (foo (baz 1) 1) ;; warning about baz not returning a map
             (foo {:a \"foo\" :b 1 :c 1} \"foo\") ;; the optional key :c has the wrong type
             "
            {:linters {:type-mismatch
                       {:level :error
                        :namespaces '{foo {foo {:arities {2 {:args [{:op :keys
                                                                     :req {:a :string
                                                                           :b :any}
                                                                     :opt {:c :string}}
                                                                    :string]
                                                             :ret :map}}}
                                           bar {:arities {1 {:args [:map]
                                                             :ret :map}}}
                                           baz {:arities {1 {:args [:int]
                                                             :ret :string}}}}}}}}))
    (assert-submaps
     '({:file "<stdin>",
        :row 2,
        :col 23,
        :level :error,
        :message "Expected: map, received: keyword."}
       {:file "<stdin>",
        :row 3,
        :col 27,
        :level :error,
        :message "Expected: string, received: positive integer."})
     (lint! "(ns foo) (defn foo [_m])
             (foo {:a :string})
             (foo {:a {:b 1}})
             (foo {:a {:b \"foo\"}})"
            {:linters {:type-mismatch
                       {:level :error
                        :namespaces '{foo {foo
                                           {:arities {1 {:args
                                                         [{:op :keys
                                                           :req {:a {:op :keys
                                                                     :req {:b :string}}}}]}}}}}}}})))
  (testing "checking also works when function is not found in cache"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 45,
        :level :warning,
        :message "Expected: number, received: string."}
       {:file "<stdin>",
        :row 1,
        :col 50,
        :level :warning,
        :message "Expected: string, received: positive integer."})
     (lint! "(ns bar (:require [foo :refer [foo]])) (inc (foo 1))"
            '{:linters
              {:type-mismatch
               {:level :warning
                :namespaces {foo {foo {:arities {1 {:args [:string]
                                                    :ret :string}}}}}}}})))
  (testing "specs work also when not providing only a ret spec"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 6,
        :level :error,
        :message "Expected: number, received: list."})
     (lint! "(inc (list 1 2 3))"
            {:linters {:type-mismatch {:level :error}}})))
  (testing "last element can be different in rest op"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 14,
        :level :error,
        :message "Expected: seqable collection, received: positive integer."})
     (lint! "(apply + 1 2 3)"
            {:linters {:type-mismatch {:level :error}}})))
  (testing "return type of assoc depends on first arg"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 6,
        :level :error,
        :message "Expected: number, received: map."})
     (lint! "(inc (assoc {} :a 1))"
            {:linters {:type-mismatch {:level :error}}})))
  (testing "printing human readable label of alternative
"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 6,
        :level :error,
        :message #"Expected: number or character, received: string"})
     (lint! "(int \"foo\")"
            {:linters {:type-mismatch {:level :error}}})))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 6,
      :level :error,
      :message #"Expected: seqable collection, received: symbol"})
   (lint! "(seq (symbol \"foo\"))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 8,
      :level :error,
      :message #"Expected: seqable collection, received: symbol"})
   (lint! "(list* (symbol \"foo\"))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 9,
      :level :error,
      :message #"Expected: seqable collection, received: symbol"})
   (lint! "(list* 'foo)"
          {:linters {:type-mismatch {:level :error}}}))
  (testing "Function return types"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 26, :level :error, :message "Expected: number, received: string."})
     (lint! "(defn foo [] \"foo\") (inc (foo))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps '({:file "<stdin>", :row 1, :col 36, :level :error, :message "Expected: number, received: map."})
                    (lint! "(defn foo [] (assoc {} :a 1)) (inc (foo))"
                           {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 53, :level :error, :message "Expected: number, received: string."})
     (lint! "(defn foo ([_] 1) ([_ _] \"foo\")) (inc (foo 1)) (inc (foo 1 1))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 40, :level :error, :message "Expected: number, received: string."})
     (lint! "(defn foo [_] (let [_x 1] \"foo\")) (inc (foo 1))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:level :error, :message "Expected: number, received: seq."})
     (lint! "(defn foo [_] (let [_x 1] (for [_x [1 2 3]] \"foo\"))) (inc (foo 1))"
            {:linters {:type-mismatch {:level :error}}}))
    ;; avoiding false positives:
    (is (empty?
         (lint! "(cons [nil] (list 1 2 3))
               (defn foo [] (:foo x))
               (let [x (atom 1)] (swap! x identity))
               (assoc {} :a `(dude))
               (reduce #(%1 %2) 1 [1 2 3])
               (map :tag [{:tag 1}])
               (map 'foo ['{foo 1}])
               (for [i [1 2 3]] (inc i))
               (let [i (inc 1)] (subs \"foo\" i))
               (assoc (into {} {}) :a 1)
               (into (map inc [1 2 3]) (remove odd? [1 2 3]))
               (cons 1 nil)
               (require '[clojure.string :as str])
               (str/starts-with? (str/join [1 2 3]) \"f\")
               (str/includes? (str/join [1 2 3]) \"f\")
               (remove #{1 2 3} [1 2 3])
               (require '[clojure.set :as set])
               (set/difference (into #{} [1 2 3]) #{1 2 3})
               (reduce conj () [1 2 3])
               (hash-set 1)
               (str/includes? (re-find #\"foo\" \"foo\") \"foo\")"
                {:linters {:type-mismatch {:level :error}}}))))
  (is (empty? (lint! "(require '[clojure.string :as str])
                      (let [[xs] ((juxt butlast last))] (symbol (str (str/join \".\" xs))))"
                     {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "(require '[clojure.string :as str])
                      (let [xs ((juxt butlast last))] (symbol (str (str/join \".\" xs))))"
                     {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "(doto (atom []) (swap! identity))"
                     {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "(defn foo [^Long x] (foo nil))"
                     {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "(defn foo ^Long [x] x) (defn bar [^long x]) (bar (foo 1)) (bar (foo nil))"
                     {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "(assoc {} :key1 2 :key2 (java.util.Date.))"
                     {:linters {:type-mismatch {:level :error}}})))
  (testing "no warning, despite string able to be nil"
    (is (empty? (lint! "(let [^String x \"foo\"] (subs x 1 1))"
                       {:linters {:type-mismatch {:level :error}}})))
    (is (empty? (lint! "(defn foo [^Long x] (subs \"foo\" x))"
                       {:linters {:type-mismatch {:level :error}}}))))
  (testing "set spec (multiple keywords)"
    (is (empty? (lint! "(re-pattern #\"foo\")"
                       {:linters {:type-mismatch {:level :error}}}))))
  (testing "associative is an ifn"
    (is (empty? (lint! "(remove (assoc x :b 2) [:a :a :b :b])"
                       {:linters {:type-mismatch {:level :error}}}))))
  (testing "sequential is a coll"
    (is (empty? (lint! "(conj (flatten []) {})"
                       {:linters {:type-mismatch {:level :error}}}))))
  (testing "collection could be a function"
    (is (empty? (lint! "(filter (conj #{} 1) [1])"
                       {:linters {:type-mismatch {:level :error}}}))))
  (testing "nilable types"
    (is (empty? (lint! "(conj nil) (conj nil 1 2 3) (dissoc nil) (dissoc nil 1 2 3) (find nil 1) (select-keys nil [1 2 3])")))))

;;;; Scratch

(comment

  )
