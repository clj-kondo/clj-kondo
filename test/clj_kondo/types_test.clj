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
             (foo {:a (or \"foo\" \"bar\") :b 1} \"foo\") ;; no warning
             (foo {:a (if (odd? 2) \"foo\" \"bar\") :b 1} \"foo\") ;; no warning
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
  (testing "printing human readable label of alternative"
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
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 12, :level :error,
      :message "Expected: associative collection or string or set, received: seq."})
   (lint! "(contains? (map inc [1 2 3]) 1)"
              {:linters {:type-mismatch {:level :error}}}))
  (testing "resolve types via cache"
    (lint! "(ns cached-ns1) (defn foo [] :keyword)"
           {:linters {:type-mismatch {:level :error}}}
           "--cache" "true")
    (assert-submaps
     '({:file "<stdin>", :row 3, :col 6, :level :error, :message "Expected: number, received: keyword."}
       {:file "<stdin>", :row 5, :col 6, :level :error, :message "Expected: number, received: keyword."})
     (lint! "
(ns cached-ns2 (:require [cached-ns1]))
(inc (cached-ns1/foo)) ;; this should give warning
(defn bar [] (cached-ns1/foo))
(inc (bar)) ;; this should also give a warning
"
                {:linters {:type-mismatch {:level :error}}}
                "--cache" "true")))
  (testing "return type of assoc"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 42, :level :error, :message "Expected: number, received: map."})
     (lint! "(defn foo [_] (assoc {} :foo true)) (inc (foo {}))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 41, :level :error, :message "Expected: number, received: associative collection."})
     (lint! "(defn foo [x] (assoc x :foo true)) (inc (foo {}))"
            {:linters {:type-mismatch {:level :error}}})))
  (testing "avoiding false positives"
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
               (str/includes? (re-find #\"foo\" \"foo\") \"foo\")
               (keyword (when-not false \"foo\") \"bar\")"
                {:linters {:type-mismatch {:level :error}}})))
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
    (is (empty? (lint! "(keyword \"widget\" 'foo)"
                       {:linters {:type-mismatch {:level :error}}}
                       "--lang" "cljs")))
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
      (is (empty? (lint! "(conj nil) (conj nil 1 2 3) (dissoc nil) (dissoc nil 1 2 3) (find nil 1) (select-keys nil [1 2 3])"
                         {:linters {:type-mismatch {:level :error}}}))))
    (testing "byte also takes chars"
      (is (empty? (lint! "(byte \\a)"
                         {:linters {:type-mismatch {:level :error}}}))))
    (testing "byte returns number"
      (is (empty? (lint! "(+ (byte 32) 1)"
                         {:linters {:type-mismatch {:level :error}}}))))))

(deftest if-let-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 6, :level :error, :message "Expected: number, received: symbol or keyword."})
   (lint! "(inc (if-let [_x 1] :foo 'symbol))"
          {:linters {:type-mismatch {:level :error}}})))

(deftest when-let-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 6, :level :error, :message "Expected: number, received: symbol or nil."})
   (lint! "(inc (when-let [_x 1] 'symbol))"
          {:linters {:type-mismatch {:level :error}}})))

(deftest or-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 6, :level :error, :message "Expected: number, received: symbol or keyword."})
   (lint! "(inc (or :foo 'bar))"
          {:linters {:type-mismatch {:level :error}}})))

(deftest cond-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 46, :level :error, :message "Expected: number, received: symbol or keyword."})
   (lint! "(defn foo [x] (cond x :foo :else 'bar)) (inc (foo 1))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
      '({:file "<stdin>", :row 1, :col 45, :level :error, :message "Expected: number, received: symbol or keyword or nil."})
      (lint! "(defn foo [x] (cond x :foo x 'symbol)) (inc (foo 1))"
             {:linters {:type-mismatch {:level :error}}})))

(deftest and-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 44, :level :error, :message "Expected: number, received: keyword or nil or boolean."})
   (lint! "(defn foo [_] true) (defn bar [_] :k) (inc (and (foo 1) (bar 2)))"
              {:linters {:type-mismatch {:level :error}}})))

(deftest return-type-inference-test
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
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 30, :level :error, :message "Expected: string or nil, received: boolean."})
     (lint! "(defn foo [^String _x]) (foo true)"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 6, :level :error, :message "Expected: number, received: string."})
     (lint! "(inc \"fooo\nbar\")"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 6, :level :error,
        :message "Expected: number, received: symbol or keyword."})
     (lint! "(inc (if :foo :bar 'baz))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 6, :level :error,
        :message "Expected: number, received: symbol or nil."})
     (lint! "(inc (when :foo 'baz))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>", :level :error,
        :message "Expected: number, received: symbol or keyword."})
     (lint! "
(defn foo1 []
  :bar)

(defn foo2 []
  'baz)

(defn foo [x]
  (if (< x 5) (foo1) (foo2)))

(defn bar []
  (foo 1))

(inc (bar))
" {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 28, :level :error, :message "Expected: string, received: integer or nil."})
     (lint! "(defn f [^Integer x] (subs x 1 10))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 40, :level :error, :message "Expected: number, received: keyword."})
     (lint! "(defn foo [] :foo) (let [a (foo)] (inc a))"
            {:linters {:type-mismatch {:level :error}}}))
    (is (empty? (lint! "(defn foo [] (foo)) (inc (foo))"
                       {:linters {:type-mismatch {:level :error}}})))
    (testing "recursive call doesn't call type checking loop"
      (is (empty? (lint! "(defn macroexpand* [_form] (macroexpand* 1)) (inc (macroexpand* 1)) (macroexpand* (macroexpand* 1))"
                         {:linters {:type-mismatch {:level :error}}})))
      (is (empty? (lint! "(declare bar) (defn foo [] (bar)) (defn bar [] (foo))"
                         {:linters {:type-mismatch {:level :error}}}
                         "--cache" "true"))))))

(deftest clojure-string-replace-test
  (assert-submaps
   '({:file "<stdin>", :row 3, :col 27, :level :error,
      :message "Regex match arg requires string or function replacement arg."})
   (lint! "
(ns foo (:require [clojure.string :as str]))
(str/replace \"foo\" #\"foo\" :foo)"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>", :row 3, :col 23, :level :error,
      :message "Char match arg requires char replacement arg."})
   (lint! "
(ns foo (:require [clojure.string :as str]))
(str/replace \"foo\" \\a \"foo\")"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 60, :level :error, :message "String match arg requires string replacement arg."})
   (lint! "(require '[clojure.string :as str]) (str/replace \"foo\" \"o\" (fn [_]))"
          {:linters {:type-mismatch {:level :error}}}))
  (is (empty? (lint! "
(require '[clojure.string :as str])
(let [x (identity \"foo\")]
  (str/replace \"foo\" \"bar\" x))"
                     {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "
(require '[clojure.string :as str])
(str/replace \" \" #\" \" {\" \" \"-\"})
"
                     {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "
(require '[clojure.string :as str])
(let [replacement (if (odd? (rand-int 100))
                    (str \"a\")
                    (str \"b\"))]
  (str/replace \"foo\" \"bar\" replacement))"
                     {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "
(require 'clojure.string)
(clojure.string/replace \"hallo\" \"a\" (first [\"e\"]))
" {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "
(defn transducer []
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result input]
       (rf result input)))))

(into [] (transducer) [1 2 3])
" {:linters {:type-mismatch {:level :error}}}))))

(deftest binding-call-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 19, :level :error, :message "String cannot be called as a function."})
   (lint! "(let [name \"foo\"] (name :foo))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
    '({:file "<stdin>", :row 1, :col 18, :level :error, :message "Number cannot be called as a function."})
    (lint! "(let [x (inc 2)] (x 2))"
           {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 23, :level :error, :message "String or nil cannot be called as a function."})
   (lint! "(defn foo [^String x] (x))"
          {:linters {:type-mismatch {:level :error}}})))

;;;; Scratch

(comment

  )
