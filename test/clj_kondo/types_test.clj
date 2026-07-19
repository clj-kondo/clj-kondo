(ns clj-kondo.types-test
  (:require
   [clj-kondo.test-utils :as tu :refer [assert-submaps2 lint!]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest type-mismatch-test
  (assert-submaps2
   '({:row 1,
      :col 6,
      :message "Expected: number, received: string."})
   (lint! "(inc \"foo\")"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:row 1,
      :col 7,
      :message "Expected: string, received: number."})
   (lint! "(subs (inc 1) 1)"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 25,
      :level :error,
      :message "Expected: number, received: string."})
   (lint! "(let [x \"foo\" y x] (inc y))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
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
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 19,
      :level :error,
      :message "Expected: atom, received: positive integer."})
   (lint! "(let [x 1] (swap! x identity))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 33,
      :level :error,
      :message "Expected: seqable collection, received: transducer."})
   (lint! "(let [x (map (fn [_]))] (cons 1 x))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
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
  (assert-submaps2
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
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 9,
      :level :error,
      :message "Expected: vector, received: seq."})
   (lint! "(subvec (map inc [1 2 3]) 10 20)"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :error,
      :message
      "Expected: stack (list, vector, etc.), received: set."})
   (lint! "(peek #{:a :b :c})"
          {:linters {:type-mismatch {:level :error}}}))
  (testing "No type checking if invalid-arity is disabled"
    (assert-submaps2
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
    (assert-submaps2
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
    (assert-submaps2
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
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 22,
        :level :error,
        :message #"Expected: number"})
     (lint! "(fn [^String x] (inc x))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 21,
        :level :error,
        :message "Expected: string, received: long."})
     (lint! "(fn [^long x] (subs x 1 1))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
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
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 73,
        :level :error,
        :message #"Expected: number"})
     (lint! "(defn foo (^String []) (^long [x]) ([x y]) (^String [x y z & xs])) (inc (foo))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 31,
        :level :error,
        :message "Expected: string or nil, received: positive integer."})
     (lint! "(defn foo [^String x] x) (foo 1)"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 39,
        :level :error,
        :message "Expected: list or nil, received: positive integer."})
     (lint! "(defn foo [^java.util.List x] x) (foo 1)"
            {:linters {:type-mismatch {:level :error}}})))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :error,
      :message "Expected: string, received: nil."})
   (lint! "(subs nil 1 2)"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 39,
      :level :error,
      :message "Expected: number, received: set or nil."})
   (lint! "(require '[clojure.set :as set]) (inc (set/union nil))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 31,
      :level :error,
      :message "Expected: seqable collection, received: number or nil."})
   (lint! "(defn foo [^Number x] (cons 1 x))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 6,
      :level :error,
      :message "Expected: number, received: list."})
   (lint! "(inc ())"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '[{:file "<stdin>", :row 1, :col 9, :level :error, :message "Expected: seqable collection, received: positive integer."}]
   (lint! "(empty? 1)"
          {:linters {:type-mismatch {:level :error}}}))
  (testing "Insufficient input"
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 15,
        :level :error,
        :message "Insufficient input."})
     (lint! "(assoc {} 1 2 3)"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '()
     (lint! "(require '[some-ns :as s]) (assoc {} 1 2 3 #::s{:x 0})"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '()
     (lint! "(assoc {} 1 2 3 #:some-ns{:x 0})"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 15
        :level :error
        :message "Insufficient input."})
     (lint! "(assoc {} 1 2 #:some-ns{:x 0})"
            {:linters {:type-mismatch {:level :error}}})))

  (testing "handle multiple errors"
    (assert-submaps2
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
  (testing "checking also works when function is not found in cache"
    (assert-submaps2
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
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 6,
        :level :error,
        :message "Expected: number, received: list."})
     (lint! "(inc (list 1 2 3))"
            {:linters {:type-mismatch {:level :error}}})))
  (testing "last element can be different in rest op"
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 14,
        :level :error,
        :message "Expected: seqable collection, received: positive integer."})
     (lint! "(apply + 1 2 3)"
            {:linters {:type-mismatch {:level :error}}})))
  (testing "return type of assoc depends on first arg"
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 6,
        :level :error,
        :message "Expected: number, received: map."})
     (lint! "(inc (assoc {} :a 1))"
            {:linters {:type-mismatch {:level :error}}})))
  (testing "printing human readable label of alternative"
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 6,
        :level :error,
        :message #"Expected: number or character, received: string"})
     (lint! "(int \"foo\")"
            {:linters {:type-mismatch {:level :error}}})))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 6,
      :level :error,
      :message #"Expected: seqable collection, received: symbol"})
   (lint! "(seq (symbol \"foo\"))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 8,
      :level :error,
      :message #"Expected: seqable collection, received: symbol"})
   (lint! "(list* (symbol \"foo\"))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 9,
      :level :error,
      :message #"Expected: seqable collection, received: symbol"})
   (lint! "(list* 'foo)"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 12, :level :error,
      :message #"Expected: .*, received: seq\."})
   (lint! "(contains? (map inc [1 2 3]) 1)"
          {:linters {:type-mismatch {:level :error}}}))
  (testing "resolve types via cache"
    (lint! "(ns cached-ns1) (defn foo [] :keyword)"
           {:linters {:type-mismatch {:level :error}}}
           "--cache" "true")
    (assert-submaps2
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
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 42, :level :error, :message "Expected: number, received: map."})
     (lint! "(defn foo [_] (assoc {} :foo true)) (inc (foo {}))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
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
                         {:linters {:type-mismatch {:level :error}}}))))
    (testing (is (empty? (lint! "(defn foo [^double x] x) (foo 1)"
                                {:linters {:type-mismatch {:level :error}}}))))
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 8,
        :level :error,
        :message #"Expected: number"})
     (lint! "(zero? \"foo\")"
            {:linters {:type-mismatch {:level :error}}}))))

(deftest map-spec-test
  (testing "map spec"
    (assert-submaps2
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
    (assert-submaps2
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
                                                                     :req {:b :string}}}}]}}}}}}}}))))

(deftest map-spec-auto-resolved-key-test
  (assert-submaps2
   '({:file "<stdin>", :row 6, :col 4, :level :error, :message "Missing required key: :other-ns/thing"})
   (lint! "
(ns test-ns
  (:require [some-ns :as s]))

(defn x [y] y)
(x {::s/thing 1})"
          '{:linters
            {:type-mismatch
             {:level :error
              :namespaces
              {test-ns
               {x {:arities {1 {:args [{:op :keys, :req {:other-ns/thing :any
                                                         :some-ns/thing :any}}]}}}}}}}})))

(deftest if-let-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 6, :level :error, :message "Expected: number, received: symbol or keyword."})
   (lint! "(inc (if-let [_x 1] :foo 'symbol))"
          {:linters {:type-mismatch {:level :error}}})))

(deftest when-let-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 6, :level :error, :message "Expected: number, received: symbol or nil."})
   (lint! "(inc (when-let [_x 1] 'symbol))"
          {:linters {:type-mismatch {:level :error}}})))

(deftest or-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 6, :level :error, :message "Expected: number, received: symbol or keyword."})
   (lint! "(inc (or :foo 'bar))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   [{:file "<stdin>",
     :row 1,
     :col 6,
     :level :error,
     :message "Expected: number, received: nil."}]
   (lint! "(inc (or))" {:linters {:type-mismatch {:level :error}}})))

(deftest cond-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 46, :level :error, :message "Expected: number, received: symbol or keyword."})
   (lint! "(defn foo [x] (cond x :foo :else 'bar)) (inc (foo 1))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 45, :level :error, :message "Expected: number, received: symbol or keyword or nil."})
   (lint! "(defn foo [x] (cond x :foo x 'symbol)) (inc (foo 1))"
          {:linters {:type-mismatch {:level :error}}})))

(deftest and-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 44, :level :error, :message "Expected: number, received: keyword or nil or boolean."})
   (lint! "(defn foo [_] true) (defn bar [_] :k) (inc (and (foo 1) (bar 2)))"
          {:linters {:type-mismatch {:level :error}}})))

(deftest return-type-inference-test
  (testing "Function return types"
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 26, :level :error, :message "Expected: number, received: string."})
     (lint! "(defn foo [] \"foo\") (inc (foo))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2 '({:file "<stdin>", :row 1, :col 36, :level :error, :message "Expected: number, received: map."})
                     (lint! "(defn foo [] (assoc {} :a 1)) (inc (foo))"
                            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 53, :level :error, :message "Expected: number, received: string."})
     (lint! "(defn foo ([_] 1) ([_ _] \"foo\")) (inc (foo 1)) (inc (foo 1 1))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 40, :level :error, :message "Expected: number, received: string."})
     (lint! "(defn foo [_] (let [_x 1] \"foo\")) (inc (foo 1))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '({:level :error, :message "Expected: number, received: seq."})
     (lint! "(defn foo [_] (let [_x 1] (for [_x [1 2 3]] \"foo\"))) (inc (foo 1))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 30, :level :error, :message "Expected: string or nil, received: boolean."})
     (lint! "(defn foo [^String _x]) (foo true)"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 6, :level :error, :message "Expected: number, received: string."})
     (lint! "(inc \"fooo\nbar\")"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 6, :level :error,
        :message "Expected: number, received: symbol or keyword."})
     (lint! "(inc (if :foo :bar 'baz))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 6, :level :error,
        :message "Expected: number, received: symbol or nil."})
     (lint! "(inc (when :foo 'baz))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
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
    (assert-submaps2
     '({:file "<stdin>", :row 1, :col 28, :level :error, :message "Expected: string, received: integer or nil."})
     (lint! "(defn f [^Integer x] (subs x 1 10))"
            {:linters {:type-mismatch {:level :error}}}))
    (assert-submaps2
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
  (assert-submaps2
   '({:file "<stdin>", :row 3, :col 27, :level :error,
      :message "Regex match arg requires string or function replacement arg, received: keyword"})
   (lint! "
(ns foo (:require [clojure.string :as str]))
(str/replace \"foo\" #\"foo\" :foo)"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>", :row 3, :col 23, :level :error,
      :message "Char match arg requires char replacement arg, received: string"})
   (lint! "
(ns foo (:require [clojure.string :as str]))
(str/replace \"foo\" \\a \"foo\")"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 60, :level :error, :message "String match arg requires string replacement arg, received: function"})
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
" {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "
(ns private.tmp.test
  (:require [clojure.string :as str]))

(defn fun1 [replacement]
  (str/replace \"foo\" #\"foo\" replacement))

(defn fun2 [^String replacement]
  (str/replace \"foo\" #\"foo\" replacement))
"
                     {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "
(require '[clojure.string :as str])
(let [^String z \"z\"]
  (clojure.string/replace \"x\" \"y\" z))
"
                     {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "
(require '[clojure.string :as str]) (defn replace-str [_foo bar] bar)
(str/replace \"foo\" #\"bar\" (partial replace-str \"dude\"))"
                     {:linters {:type-mismatch {:level :error}}})))
  (is (empty? (lint! "
(ns foo (:require [clojure.string :as str]))
(str/replace \"$a\" #\"\\w+\" (comp str))"
                     {:linters {:type-mismatch {:level :error}}}))))

(deftest binding-call-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 19, :level :error, :message "String cannot be called as a function."})
   (lint! "(let [name \"foo\"] (name :foo))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 18, :level :error, :message "Number cannot be called as a function."})
   (lint! "(let [x (inc 2)] (x 2))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 23, :level :error, :message "String or nil cannot be called as a function."})
   (lint! "(defn foo [^String x] (x))"
          {:linters {:type-mismatch {:level :error}}})))

(deftest def+fn-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 37, :level :error, :message "Expected: number, received: keyword."}
     {:file "<stdin>", :row 1, :col 40, :level :error, :message "Expected: string or nil, received: keyword."})
   (lint! "(def x (fn [^String _x] :foo)) (inc (x :foo))"
          {:linters {:type-mismatch {:level :error}}}))
  ;; This is actually an arity error but clj-kondo doesn't handle macro metadata in that way yet
  (is (empty? (lint! "(def ^:macro f (fn [_ _] (list (symbol \"+\") 1 2 3))) (inc (f 1 2))"
                     {:linters {:type-mismatch {:level :error}}}))))

(deftest let+fn-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 38, :level :error, :message "Expected: number, received: keyword."})
   (lint! "(let [x (fn [^String _x] :foo)] (inc (x :foo)))"
          {:linters {:type-mismatch {:level :error}}}))
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 36, :level :error, :message "Expected: number, received: keyword."})
   (lint! "(let [x (fn [x] (keyword x))] (inc (x \"dude\")))"
          {:linters {:type-mismatch {:level :error}}})))

(deftest rseq-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 7, :level :error,
      :message "Expected: vector or sorted map, received: seq."})
   (lint! "(rseq (map inc [1 2 3]))"
          {:linters {:type-mismatch {:level :error}}}))
  (is (empty? (lint! "(rseq (sorted-map :a 1)) (rseq [1 2 3])"
                     {:linters {:type-mismatch {:level :error}}}))))

(def config {:linters {:type-mismatch {:level :error}}})

(deftest namespaced-map-as-arg-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 15, :level :error, :message "Insufficient input."}
     {:file "<stdin>", :row 1, :col 15, :level :warning, :message "Unresolved namespace s. Are you missing a require?"})
   (lint! "(assoc {} 1 3 #::s{:thing 1})" config))
  (is (empty? (lint! "(assoc {} 1 #::{:thing 1})" config)))
  (is (empty? (lint! "(assoc {} 1 2 3 #::{:thing 1})" config)))
  (is (empty? (lint! "(assoc {} 1 2 3 #:some-ns{:thing 1})" config))))

(defn expected-message
  [expected received]
  (format "Expected: %s, received: %s."
          (name expected)
          (name received)))

(def config-2
  '{:linters
    {:type-mismatch
     {:level :error
      :namespaces
      {user
       {fun2
        {:arities
         {1
          {:args [{:op :keys, :req {:a :int}}],
           :ret {:op :keys, :req {:a :string}}}}}
        fun3
        {:arities
         {1
          {:args [{:op :keys, :req {:a :int}}],
           :ret {:op :keys, :req {:user/a :string}}}}}
        fun4
        {:arities
         {1
          {:args [{:op :keys, :req {:a :int} :nilable true}],
           :ret {:op :keys, :req {:a :string}}}}}}}}}})

(deftest keyword-resolution-test
  (testing "keyword call"
    (assert-submaps2
     [{:row 1 :col 6 :message (expected-message :number :string)}]
     (lint! "(inc (:a {:a \"foo\"}))" config)))
  (testing "nested keyword call"
    (assert-submaps2
     [{:row 1 :col 6 :message (expected-message :number :string)}]
     (lint! "(inc (:a {:a (:b {:b \"foo\"})}))" config)))
  (testing "inferred type"
    (assert-submaps2
     [{:row 4 :col 8 :message (expected-message :number :string)}]
     (lint! "
(do
  (defn fun2 [_] {:a \"2\"})
  (+ 1 (:a (fun2 {:a 41}))))"
            config)))
  (testing "nested inferred type with the same keyword"
    (is (empty?
         (lint! "
(do
  (defn fun2 [_] {:a \"2\"})
  (+ 1 (:a (:a (fun2 {:a 41})))))"
                config))))
  (testing "nested inferred type with different keywords"
    (assert-submaps2
     [{:row 4 :col 8 :message (expected-message :number :string)}]
     (lint! "
(do
  (defn fun2 [_] {:b {:a \"2\"}})
  (+ 1 (:a (:b (fun2 {:a 41})))))"
            config)))
  (testing "inferred type for explicit namespaced keyword"
    (assert-submaps2
     [{:row 4 :col 8 :message (expected-message :number :string)}]
     (lint! "
(do
  (defn fun2 [_] {:eita/a \"2\"})
  (+ 1 (:eita/a (fun2 {:a 41}))))"
            config)))
  (testing "inferred type for implict namespaced keyword"
    (assert-submaps2
     [{:row 6 :col 8 :message (expected-message :number :string)}]
     (lint! "
(ns foo)

(do
  (defn fun2 [_] {:foo/a \"2\"})
  (+ 1 (::a (fun2 {:a 41}))))"
            config)))
  (testing "manually typed function"
    (assert-submaps2
     [{:row 4 :col 8 :message (expected-message :number :string)}]
     (lint! "
(do
  (defn fun2 [m] (:b m))
  (+ 1 (:a (fun2 {:a 41}))))"
            config-2)))
  (testing "manually typed function for explicit namespaced keyword"
    (assert-submaps2
     [{:row 4 :col 8 :message (expected-message :number :string)}]
     (lint! "
(do
  (defn fun3 [m] (:b m))
  (+ 1 (:user/a (fun3 {:a 41}))))"
            config-2)))
  (testing "manually typed function for implicit namespaced keyword"
    (assert-submaps2
     [{:row 4 :col 8 :message (expected-message :number :string)}]
     (lint! "
(do
  (defn fun3 [m] (:b m))
  (+ 1 (::a (fun3 {:a 41}))))"
            config-2)))
  (testing "unhandled keywords are properly handled"
    (is (empty? (lint! "
(do
  (defn fun2 [m] (:b m))
  (+ 1 (:b (fun2 {:a 41}))))"
                       config)))
    (is (empty? (lint! "
(do
  (defn fun2 [m] (:b m))
  (+ 1 (::a (fun2 {:a 41}))))"
                       config-2)))
    (is (empty? (lint! "
(do
  (defn fun2 [m] (:b m))
  (+ 1 (:user/a (fun2 {:a 41}))))"
                       config-2))))
  (is (empty? (lint! "(require 'clojure.set) (clojure.set/project [{:foo :bar}] #{:foo})" config-2))))

(deftest function-ret-map-test
  (testing "manually typed function which returns a map"
    (assert-submaps2
     [{:row 4 :col 8 :message (expected-message :number :map)}]
     (lint! "
(do
  (defn fun2 [m] (:a m))
  (+ 1 (fun2 {:a 23})))"
            config-2)))
  (testing "typed ret map function which calls another typed function which also expects a map"
    (assert-submaps2
     [{:row 4 :col 9 :message (expected-message :integer :string)}]
     (lint! "
(do
  (defn fun2 [m] (:a m))
  (fun2 (fun2 {:a 23})))"
            config-2))))

(deftest nilable-map-test
  (testing "pass nil to a nilable map"
    (assert-submaps2
     []
     (lint! "
(do
  (defn fun4 [m] (:a m))
  (fun4 nil))"
            config-2)))

  (testing "pass invalid map to a nilable map"
    (assert-submaps2
     [{:file "<stdin>", :row 4, :col 9, :level :error, :message "Missing required key: :a"}]
     (lint! "
(do
  (defn fun4 [m] (:a m))
  (fun4 {}))"
            config-2))))

(deftest misc-false-positives-test
  (is (empty? (lint! "(even? ('a {'a 10}))" config)))
  (is (empty? (lint! "(keyword (re-find (re-matcher #\"foo\" \"foo\")))" config))))

(deftest map-namespace-test
  (testing "#::ns{:key :val} syntax"
    (is (empty? (lint! "
(ns test-ns (:require [some-ns :as s]))
(defn x [y] y)
(x #::s{:thing 1})"
                       '{:linters
                         {:type-mismatch
                          {:level :error
                           :namespaces
                           {test-ns
                            {x {:arities {1 {:args [{:op :keys, :req {:some-ns/thing :any}}]}}}}}}}}))))
  (testing "#:ns{:key :val} syntax"
    (is (empty? (lint! "
(ns test-ns)
(defn x [y] y)
(x #:some-ns{:thing 1})"
                       '{:linters
                         {:type-mismatch
                          {:level :error
                           :namespaces
                           {test-ns
                            {x {:arities {1 {:args [{:op :keys, :req {:some-ns/thing :any}}]}}}}}}}}))))
  (testing "{::ns/key :val} syntax"
    (is (empty? (lint! "
(ns test-ns (:require [some-ns :as s]))
(defn x [y] y)
(x {::s/thing 1})"
                       '{:linters
                         {:type-mismatch
                          {:level :error
                           :namespaces
                           {test-ns
                            {x {:arities {1 {:args [{:op :keys, :req {:some-ns/thing :any}}]}}}}}}}})))))

(deftest req+op-test
  (when-not tu/native?
    (testing "req + op rest"
      (let [sw (java.io.StringWriter.)]
        (binding [*err* sw]
          (is (empty? (lint! "
(ns test-ns)
(defn x [m x] [m x])
(x {:keys [:foo :bar]} 1)"
                             '{:linters
                               {:type-mismatch
                                {:level :error
                                 :namespaces
                                 {test-ns
                                  {x {:arities {2 {:args [{:op :keys, :req {:keys {:op :rest, :spec :keyword}}}
                                                          :any]}}}}}}}})))
          (is (str/includes? (str sw) "WARNING")))))))

(deftest def-type-mismatch-test
  (let [config '{:linters
                 {:type-mismatch
                  {:level :error}}}]
    (testing "type of def used elsewhere"
      (assert-submaps2
       '({:file "<stdin>", :row 1, :col 19, :level :error, :message "Expected: number, received: keyword."})
       (lint! "(def x :foo) (inc x)"
              config))
      (assert-submaps2
       '({:file "<stdin>", :row 1, :col 6, :level :error, :message "Expected: number, received: function."})
       (lint! "(inc seq)"
              config)))
    (testing "dynamic vars are excluded, as they are often initialized to nil but bound to something else later"
      (is (empty?
           (lint! "(def ^:dynamic *x* nil) (inc x)"
                  config))))
    (testing "defn now has var of type :fn"
      (doseq [sym ['defn 'defn-]
              lang ["clj" "cljc"]]
        (let [lints (lint! (str/replace "(defn foo [] :foo) (inc foo)"
                                        "defn" (str sym))
                           config
                           "--lang" lang)]
          (is (every? :row lints))
          (assert-submaps2
           '({:file "<stdin>", :level :error, :message "Expected: number, received: function."})
           lints))))
    (testing "override with config"
      (let [lints (lint! "(ns foo) (def x) (inc x)"
                         (assoc-in config [:linters :type-mismatch :namespaces] '{foo {x {:type :keyword}}}))]
        (assert-submaps2
         '({:file "<stdin>", :row 1, :col 23, :level :error, :message "Expected: number, received: keyword."})
         lints)))))

(deftest issue-1978-varargs-false-positive-test
  (is (empty? (lint! "(defn foo
  ([x] x)
  ([^String a-ns
    a-name & xs]
   (str a-ns a-name xs)))

(foo 1)"
                     config))))

(deftest throw-test
  (testing "throw expects a throwable in clj"
    (is (assert-submaps2
         '({:file "<stdin>", :row 1, :col 8, :level :error, :message "Expected: throwable, received: positive integer."})
         (lint! "(throw 1)" config)))
    (testing "it is ignored with clj-kondo/ignore"
      (is (empty? (lint! "(throw #_:clj-kondo/ignore 1)" config)))))

  (testing "throw accepts any type in cljs"
    (is (empty? (lint! "(throw 1)" config "--lang" "cljs")))
    (is (empty? (lint! "(throw \"dude\")" config "--lang" "cljs")))
    (testing "with .cljs extension"
      (is (empty? (lint! "(throw \"dude\")" config "--filename" "foo.cljs"))))))

(deftest do-test
  (is (assert-submaps2
       '({:file "<stdin>", :row 1, :col 6, :level :error, :message "Expected: number, received: string."}
         {:file "<stdin>", :row 1, :col 20, :level :error, :message "Expected: number, received: keyword."})
       (lint! "(inc (do (prn (inc :foo)) \"not a number\"))"
              config))))

(deftest doto-test
  (is (assert-submaps2
       [{:file "<stdin>",
         :row 1,
         :col 6,
         :level :error,
         :message "Expected: number, received: keyword."}]
       (lint! "(inc (doto :foo prn))"
              config))))

(deftest deref-test
  (is (assert-submaps2
       [{:file "<stdin>",
         :row 1,
         :col 2,
         :level :error,
         :message "Expected: deref, received: function."}
        {:file "<stdin>",
         :row 1,
         :col 20,
         :level :error,
         :message "Expected: deref, received: nil."}]
       (lint! "@inc (let [x nil] @x)"
              config))))

(deftest issue-2575-test
  (is (empty?
       (lint! "(require '[clojure.string :as str])

(defn foo []
  (let [res (str)]
    {:b {:a res}}))

(str/split-lines
 (:a
  (:b
   (foo))))
"
              config))))

(deftest issue-2580-test
  (is (empty?
       (lint! "(assoc {} :foo '[bar] :id #{})"
              config))))

(deftest var-test
  (is (empty? (lint! "(map #'inc [1 2 3])" config)))
  (is (empty? (lint! "((partial #'+ 1) 2)" config)))
  (is (empty? (lint! "(symbol #'inc)" config))))

(deftest get-test
  (is (empty? (lint! "(get nil :x)" config)))
  (is (empty? (lint! "(get \"foo\" 0)" config)))
  (is (empty? (lint! "(get {:x 10} :x)" config)))
  (is (empty? (lint! "(get \"foo\" 1)" config)))
  (is (empty? (lint! "(-> {:x 10} (get :x))" config)))
  (is (empty? (lint! "(get [1 2 3] 1)" config)))
  (is (empty? (lint! "(-> [1 2 3] (get 1))" config)))
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 6, :level :error, :message #"ILookup.*received: keyword."})
   (lint! "(get :x {:x 10})" config))
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 6, :level :error, :message #"ILookup.*received: positive integer."})
   (lint! "(get 1 [1 2 3])" config))
  (assert-submaps2
   '({:file "<stdin>", :row 2, :col 19, :level :error, :message #"ILookup.*received: atom."})
   (lint! "(let [x (atom {:a 1})]
             (get x :a))" config)))

(deftest repeatedly-test
  (let [config {:linters {:type-mismatch {:level :error}}}]
    (testing "Valid usages of repeatedly"
      (is (empty? (lint! "(repeatedly 10 #(println :foo))" config)) "Valid usage should not warn")
      (is (empty? (lint! "(repeatedly #(println :foo))" config)) "Valid usage should not warn"))
    (testing "Invalid usages of repeatedly"
      (assert-submaps2
       '({:row 1 :col 13 :message "Expected: natural integer, received: function."}
         {:row 1 :col 29 :message "Expected: function, received: positive integer."})
       (lint! "(repeatedly #(println :foo) 10)" config))
      (assert-submaps2
       '({:row 1 :col 13 :message "Expected: function, received: positive integer."})
       (lint! "(repeatedly 10)" config)))))

(deftest to-array-type-test
  (testing "Valid usage: nilable collection argument returns array"
    (is (empty? (lint! "(to-array [1 2 3])" config)))
    (is (empty? (lint! "(to-array '(1 2 3))" config)))
    (is (empty? (lint! "(to-array #{1 2 3})" config)))
    (is (empty? (lint! "(to-array nil)" config))))

  (testing "Invalid usage: non-seqable argument triggers type-mismatch"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 11
        :level :error
        :message "Expected: collection or nil, received: positive integer."})
     (lint! "(to-array 1)" config))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 11
        :level :error :message "Expected: collection or nil, received: string."})
     (lint! "(to-array \"foo\")" config)))
  (testing "Return type: array triggers type-mismatch in inc"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 6
        :level :error
        :message "Expected: number, received: array."})
     (lint! "(inc (to-array [1 2 3]))" config))))

(deftest numerator-denominator-test
  (testing "Valid usages with ratio"
    (is (empty? (lint! "(numerator 1/2)" config))
        "Valid numerator call with ratio")
    (is (empty? (lint! "(denominator 1/2)" config))
        "Valid denominator call with ratio")
    (is (empty? (lint! "(numerator 3/5)" config))
        "Valid numerator call with ratio")
    (is (empty? (lint! "(denominator 3/5)" config))
        "Valid denominator call with ratio")
    (testing "Division results are :number type, which could be :ratio, so no warning"
      (is (empty? (lint! "(numerator (/ 1 2))" config))
          "Division could produce ratio")
      (is (empty? (lint! "(denominator (/ 3 5))" config))
          "Division could produce ratio")
      (is (empty? (lint! "(numerator (/ 1 1))" config))
          "Division type is :number which includes :ratio.")
      (is (empty? (lint! "(denominator (/ 2 2))" config))
          "Division type is :number which includes :ratio")))
  (testing "Invalid usages with non-ratio numbers"
    (assert-submaps2
     '({:row 1 :col 12 :message "Expected: ratio, received: positive integer."})
     (lint! "(numerator 42)" config))
    (assert-submaps2
     '({:row 1 :col 14 :message "Expected: ratio, received: positive integer."})
     (lint! "(denominator 42)" config))
    (assert-submaps2
     '({:row 1 :col 12 :message "Expected: ratio, received: string."})
     (lint! "(numerator \"foo\")" config))
    (assert-submaps2
     '({:row 1 :col 14 :message "Expected: ratio, received: double."})
     (lint! "(denominator 3.14)" config))))

(deftest ex-info-test
  (is (empty? (lint! "(ex-info \"hello\" nil) (ex-info \"hello\" nil nil) (ex-info nil nil nil)" config))))

(deftest sorted-collections-test
  (testing "sorted-map-by"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 16
        :level :error
        :message "Expected: function, received: positive integer."})
     (lint! "(sorted-map-by 1 :a 1)" config))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 6
        :level :error
        :message "Expected: number, received: sorted map."})
     (lint! "(inc (sorted-map-by > :a 1))" config)))
  (testing "sorted-set"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 6
        :level :error
        :message "Expected: number, received: sorted set."})
     (lint! "(inc (sorted-set 1 2 3))" config)))
  (testing "sorted-set-by"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 16
        :level :error
        :message "Expected: function, received: positive integer."})
     (lint! "(sorted-set-by 1 2 3)" config))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 6
        :level :error
        :message "Expected: number, received: sorted set."})
     (lint! "(inc (sorted-set-by > 1 2 3))" config))))

(deftest array-functions-test
  (testing "alength"
    (is (empty? (lint! "(alength (to-array [1 2 3]))" config)))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 10
        :level :error
        :message "Expected: array, received: string."})
     (lint! "(alength \"foo\")" config)))
  (testing "aget 2 args"
    (is (empty? (lint! "(aget (to-array [1 2 3]) 0)" config)))
    (is (empty? (lint! "(aget js/document \"getElementById\")" config
                       "--lang" "cljs")))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 7
        :level :error
        :message "Expected: array, received: vector."})
     (lint! "(aget [1 2 3] 0)" config)))
  (testing "aget 3 args"
    (is (empty? (lint! "(aget (to-array [1 2 3]) 0 1)" config)))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 7
        :level :error
        :message "Expected: array, received: vector."})
     (lint! "(aget [1 2 3] 0 1)" config)))
  (testing "aget varargs"
    (is (empty? (lint! "(aget (to-array [1 2 3]) 0 1 2)" config)))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 7
        :level :error
        :message "Expected: array, received: vector."})
     (lint! "(aget [1 2 3] 0 1 2)" config)))
  (testing "aset 3 args"
    (is (empty? (lint! "(aset (to-array [1 2 3]) 0 4)" config)))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 7
        :level :error
        :message "Expected: array, received: vector."})
     (lint! "(aset [1 2 3] 0 4)" config)))
  (testing "aset 4 args"
    (is (empty? (lint! "(aset (to-array [1 2 3]) 0 1 4)" config)))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 7
        :level :error
        :message "Expected: array, received: vector."})
     (lint! "(aset [1 2 3] 0 1 4)" config)))
  (testing "aset varargs"
    (is (empty? (lint! "(aset (to-array [1 2 3]) 0 1 2 4)" config)))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 7
        :level :error
        :message "Expected: array, received: vector."})
     (lint! "(aset [1 2 3] 0 1 2 4)" config)))
  (testing "aclone"
    (is (empty? (lint! "(aclone (to-array [1 2 3]))" config)))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 9
        :level :error
        :message "Expected: array, received: vector."})
     (lint! "(aclone [1 2 3])" config))))

(deftest comp-test
  (is (empty? (lint! "(comp)" config)))
  (is (empty? (lint! "(comp inc dec)" config)))
  (is (empty? (lint! "(comp (map inc))" config)))
  (assert-submaps2
   '({:file "<stdin>"
      :row 1
      :col 7
      :level :error
      :message "Expected: function, received: seq."})
   (lint! "(comp (map inc (range)))" config)))

(deftest cast-test
  (testing "cast with valid class argument"
    (is (empty? (lint! "(cast String \"hello\")" config)))
    (is (empty? (lint! "(cast java.io.File (java.io.File. \".\"))" config))))
  (testing "cast with invalid argument - not a class"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 7
        :level :error
        :message "Expected: class, received: positive integer."})
     (lint! "(cast 1 \"hello\")" config)))
  (testing "cast return value should be any"
    (is (empty? (lint! "(inc (cast java.lang.Long 5))" config)))))

(deftest bases-test
  (testing "bases with valid class argument"
    (is (empty? (lint! "(bases java.io.File)" config)))
    (is (empty? (lint! "(bases String)" config))))
  (testing "bases with invalid argument - not a class"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 8
        :level :error
        :message "Expected: class, received: positive integer."})
     (lint! "(bases 1)" config)))
  (testing "bases return value should be seq"
    (is (empty? (lint! "(first (bases java.io.File))" config)))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 6
        :level :error
        :message "Expected: number, received: seq."})
     (lint! "(inc (bases java.io.File))" config)))
  (testing "bases in ClojureScript accepts both classes and functions"
    ;; In CLJS, bases can work with constructor functions
    (is (empty? (lint! "(bases identity)"
                       config "--lang" "cljs")))
    (is (empty? (lint! "(bases (fn []))"
                       config "--lang" "cljs")))))

(deftest supers-test
  (testing "supers with valid class argument"
    (is (empty? (lint! "(supers java.io.File)" config)))
    (is (empty? (lint! "(supers Object)" config))))
  (testing "supers with invalid argument - not a class"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 9
        :level :error
        :message "Expected: class, received: positive integer."})
     (lint! "(supers 1)" config))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 9
        :level :error
        :message "Expected: class, received: string."})
     (lint! "(supers \"hello\")" config)))
  (testing "supers return value should be set or nil"
    (is (empty? (lint! "(conj (supers java.io.File) Object)" config)))

    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 6
        :level :error
        :message "Expected: number, received: set or nil."})
     (lint! "(inc (supers java.io.File))" config)))
  (testing "supers in ClojureScript accepts both classes and functions"
    ;; In CLJS, supers can work with constructor functions
    (is (empty? (lint! "(supers identity)"
                       config "--lang" "cljs")))
    (is (empty? (lint! "(supers (fn []))"
                       config "--lang" "cljs")))))

(deftest class-test
  (testing "class argument can be any"
    (is (empty? (lint! "(class \"hello\")" config)))
    (is (empty? (lint! "(class 1)" config)))
    (is (empty? (lint! "(class nil)" config))))
  (testing "class returns class"
    (is (empty? (lint! "(bases (class \"hello\"))" config)))
    (is (empty? (lint! "(supers (class []))" config)))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 6
        :level :error
        :message "Expected: number, received: class or nil."})
     (lint! "(inc (class 42))" config)))
  (testing "class returns nil for nil"
    (is (empty? (lint! "(defn f [x] (when (class x) 1))" config)))))

(deftest instance-test
  (testing "instance? returns boolean"
    (is (empty? (lint! "(instance? String \"hello\")" config)))
    (is (empty? (lint! "(instance? java.io.File (java.io.File. \".\"))" config)))
    (is (empty? (lint! "(if (instance? Number 42) 1 2)" config))))
  (testing "instance? requires class as first argument"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 12
        :level :error
        :message "Expected: class, received: positive integer."})
     (lint! "(instance? 42 \"hello\")" config))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 12
        :level :error
        :message "Expected: class, received: string."})
     (lint! "(instance? \"String\" 42)" config)))
  (testing "instance? in ClojureScript doesn't produce type errors"
    ;; In CLJS, instance? accepts both classes and constructor functions
    (is (empty? (lint! "(instance? ExceptionInfo (ex-info \"msg\" {}))"
                       config "--lang" "cljs")))
    (is (empty? (lint! "(instance? identity 42)"
                       config "--lang" "cljs")))
    (is (empty? (lint! "(ns foo (:require [cljs.core])) (instance? cljs.core/ExceptionInfo {})"
                       config "--lang" "cljs"))))
  (testing "instance? with primitive array class syntax (Clojure 1.12+)"
    (is (empty? (lint! "(instance? byte/1 (byte-array 0))" config)))
    (is (empty? (lint! "(instance? int/1 (int-array 0))" config)))
    (is (empty? (lint! "(instance? long/1 (long-array 0))" config)))
    (is (empty? (lint! "(instance? short/1 (short-array 0))" config)))
    (is (empty? (lint! "(instance? float/1 (float-array 0))" config)))
    (is (empty? (lint! "(instance? double/1 (double-array 0))" config)))
    (is (empty? (lint! "(instance? boolean/1 (boolean-array 0))" config)))
    (is (empty? (lint! "(instance? String/1 (char-array 0))" config)))))

(deftest make-array-test
  (testing "make-array with 2 args (type and length)"
    (is (empty? (lint! "(make-array String 10)" config)))
    (is (empty? (lint! "(make-array Integer/TYPE 5)" config))))
  (testing "make-array with multiple dimensions"
    (is (empty? (lint! "(make-array String 10 20)" config)))
    (is (empty? (lint! "(make-array Integer/TYPE 5 3 2)" config))))
  (testing "make-array requires class as first argument"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 13
        :level :error
        :message "Expected: class, received: positive integer."})
     (lint! "(make-array 42 10)" config))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 13
        :level :error
        :message "Expected: class, received: string."})
     (lint! "(make-array \"String\" 10)" config)))
  (testing "make-array requires int as dimension arguments"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 20
        :level :error
        :message "Expected: integer, received: string."})
     (lint! "(make-array String \"10\")" config))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 23
        :level :error
        :message "Expected: integer, received: string."})
     (lint! "(make-array String 10 \"20\")" config)))
  (testing "make-array returns array"
    (is (empty? (lint! "(alength (make-array String 10))" config)))
    (is (empty? (lint! "(aget (make-array String 10) 0)" config)))
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 6
        :level :error
        :message "Expected: number, received: array."})
     (lint! "(inc (make-array String 10))" config))))

(deftest inst-test
  (let [config {:linters {:type-mismatch {:level :error}}}]
    (testing "inst-ms and inst-ms* handle inst type"
      (is (empty? (lint! "(defn f [^java.util.Date d] (inst-ms d))" config)))
      (is (empty? (lint! "(defn f [^java.util.Date d] (inst-ms* d))" config)))
      (is (empty? (lint! "(inst-ms (java.util.Date.))" config)))
      (is (empty? (lint! "(inst-ms* (java.util.Date.))" config)))
      (is (empty (lint! "(ns foo (:require [clj-time.core :as time]))
                        (inst-ms (time/now))"))))

    (testing "inst-ms and inst-ms* mismatch"
      (assert-submaps2
       '({:message "Expected: instant, received: string."})
       (lint! "(inst-ms \"foo\")" config))
      (assert-submaps2
       '({:message "Expected: instant, received: positive integer."})
       (lint! "(inst-ms* 10)" config))
      (assert-submaps2
       '({:message "Expected: instant, received: long or nil."})
       (lint! "(defn f [^Long d] (inst-ms d))" config)))

    (testing "inst-ms and inst-ms* return long"
      (assert-submaps2
       '({:message "Expected: string, received: long."})
       (lint! "(defn f [^java.util.Date d] (subs (inst-ms d) 1))" config)))))

(deftest -in-fns-test
  (is (empty? (lint! "(assoc-in {:a {:b 42}} [:a :b] 43)" config)))
  (is (empty? (lint! "(get-in {:a {:b 42}} '(:a :b))" config)))
  (assert-submaps2
   '({:file "<stdin>"
      :row 1
      :col 25
      :level :error
      :message "Expected: sequential collection, received: set."})
   (lint! "(update-in {:a {:b 42}} #{:a :b} inc)" config)))

(deftest pmap-test
  (is (empty? (lint! "(pmap inc [1 2 3])" config)))
  (is (empty? (lint! "(pmap + [1 2] [3 4])" config)))
  (assert-submaps2
   '({:file "<stdin>"
      :row 1
      :col 1
      :level :error
      :message "clojure.core/pmap is called with 1 arg but expects 2 or more"})
   (lint! "(pmap inc)" config))
  (assert-submaps2
   '({:file "<stdin>"
      :row 1
      :col 7
      :level :error
      :message "Expected: function, received: seq."})
   (lint! "(comp (pmap inc [1 2 3]))" config)))

(deftest future-type-test
  (testing "future returns future type"
    (is (empty? (lint! "(let [f (future 42)] (deref f))" config)))
    (assert-submaps2
    '({:file "<stdin>",
       :row 1,
       :col 27,
       :level :error,
       :message "Expected: number, received: future."})
     (lint! "(let [f (future 42)] (inc f))" config)))
  (testing "future-call returns future type"
    (is (empty? (lint! "(let [f (future-call (fn [] 42))] (deref f))" config)))
    (is (empty? (lint! "(let [f (future-call (fn [] 42))] @f)" config))))
  (testing "future-done? accepts future"
    (is (empty? (lint! "(let [f (future-call (fn [] 42))] (future-done? f))" config))))
  (testing "future-cancel accepts future and returns boolean"
    (is (empty? (lint! "(let [f (future-call (fn [] 42))] (future-cancel f))" config)))
    (is (empty? (lint! "(let [f (future-call (fn [] 42))] (if (future-cancel f) :ok :not-ok))" config))))
  (testing "future-cancelled? accepts future and returns boolean"
    (is (empty? (lint! "(let [f (future-call (fn [] 42))] (future-cancelled? f))" config)))
    (is (empty? (lint! "(let [f (future-call (fn [] 42))] (if (future-cancelled? f) :ok :not-ok))" config))))
  (testing "future? returns boolean"
    (is (empty? (lint! "(let [f (future-call (fn [] 42))] (if (future? f) :ok :not-ok))" config))))
  (testing "type errors with wrong types"
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 26,
        :level :error,
        :message "Expected: future, received: positive integer."})
     (lint! "(let [x 1] (future-done? x))" config))
    (assert-submaps2
     '({:file "<stdin>",
        :row 1,
        :col 34,
        :level :error,
        :message "Expected: future, received: atom."})
     (lint! "(let [a (atom 1)] (future-cancel a))" config)))
  (testing "java.util.concurrent.Future type hint"
    (is (empty? (lint! "(defn foo [^java.util.concurrent.Future f] (future-done? f))" config)))
    (is (empty? (lint! "(defn foo [^java.util.concurrent.Future f] @f)" config)))
    (assert-submaps2
     '({:file "<stdin>",
        :level :error,
        :message "Expected: atom, received: future or nil."})
     (lint! "(defn foo [^java.util.concurrent.Future f] (swap! f inc))" config))))

(deftest required-keys-inference-test
  (let [config {:linters {:type-mismatch {:level :error}}}]
    (testing "missing required keys inferred from :keys! fn args"
      (assert-submaps2
       '({:file "<stdin>", :row 3, :level :error, :message "Missing required key: :b"}
         {:file "<stdin>", :row 3, :level :error, :message "Missing required key: :c"}
         {:file "<stdin>", :row 4, :level :error, :message "Expected: map, received: nil."})
       (lint! "
(defn foo [{:keys! [a b & :c]}] [a b])
(foo {:a 1})
(foo nil)
(foo {:a 1 :b 2 :c 3})
(defn dyn [m] (foo (assoc m :a 1)))
(foo {(str \"k\") 1 :a 1 :b 2})"
              config)))
    (testing "qualified and auto-resolved keys"
      (assert-submaps2
       '({:file "<stdin>", :row 4, :level :error, :message "Missing required key: :person/id"}
         {:file "<stdin>", :row 5, :level :error, :message "Missing required key: :foo/x"})
       (lint! "(ns foo)
(defn f1 [{:person/keys! [id]}] id)
(defn f2 [{::keys! [x]}] x)
(f1 {:id 1})
(f2 {:x 1})
(f1 {:person/id 1})
(f2 {:foo/x 1})"
              config)))
    (testing ":syms! and :strs!"
      (assert-submaps2
       '({:file "<stdin>", :row 4, :col 21, :level :error, :message "Missing required key: x"}
         {:file "<stdin>", :row 5, :col 22, :level :error, :message "Missing required key: \"y\""})
       (lint! "
(defn fsym [{:syms! [x]}] x)
(defn fstr [{:strs! [y]}] y)
(fsym {'x 1}) (fsym {:x 1})
(fstr {\"y\" 1}) (fstr {:y 1})"
              config)))
    (testing "no false positive for kwargs style"
      (is (empty? (lint! "
(defn kw [& {:keys! [x]}] x)
(kw :other 1)"
                         config))))
    (testing "positional map arg before & rest is still checked"
      (assert-submaps2
       '({:file "<stdin>", :row 3, :level :error, :message "Missing required key: :a"})
       (lint! "
(defn f [{:keys! [a]} & rst] [a rst])
(f {} 1 2)"
              config)))
    (testing "multiple bang modifiers merge"
      (assert-submaps2
       '({:file "<stdin>", :row 3, :level :error, :message "Missing required key: \"y\""}
         {:file "<stdin>", :row 3, :level :error, :message "Missing required key: :a"})
       (lint! "
(defn f [{:keys! [a] :strs! [y]}] [a y])
(f {})"
              config)))
    (testing "modifier order does not matter"
      (assert-submaps2
       '({:file "<stdin>", :row 3, :level :error, :message "Missing required key: :a"})
       (lint! "
(defn f [{q :q :keys! [a]}] [q a])
(f {:q 1})"
              config)))
    (testing "quoted map keys resolve a single quote level only"
      (let [cfg {:linters {:type-mismatch
                           {:level :error
                            :namespaces '{foo {qfn {:arities {1 {:args [{:op :keys
                                                                         :opt {:a :string}}]}}}}}}}}]
        (assert-submaps2
         '({:file "<stdin>", :row 1, :level :error, :message "Expected: string, received: positive integer."})
         (lint! "(ns foo) (defn qfn [m] m) (qfn {':a 1})" cfg))
        (is (empty? (lint! "(ns foo) (defn qfn [m] m) (qfn {'':a 1})" cfg)))))
    (testing "required keys via cache"
      (lint! "(ns req-keys-ns1)
(defn create [{:keys! [id name]}] [id name])
(defn by-sym [{:syms! [x]}] x)"
             config
             "--cache" "true")
      (assert-submaps2
       '({:file "<stdin>", :row 3, :level :error, :message "Missing required key: :name"}
         {:file "<stdin>", :row 5, :level :error, :message "Missing required key: x"})
       (lint! "
(ns req-keys-ns2 (:require [req-keys-ns1]))
(req-keys-ns1/create {:id 1})
(req-keys-ns1/by-sym {'x 1})
(req-keys-ns1/by-sym {:x 1})"
              config
              "--cache" "true")))))

(deftest select-destructuring-types-test
  (let [config {:linters {:type-mismatch {:level :error}}}]
    (testing ":select map misses a required key"
      (assert-submaps2
       '({:file "<stdin>", :row 3, :level :error, :message "Missing required key: :x"})
       (lint! "
(defn f [{:keys! [x]}] x)
(defn g [{:keys [a] :select m}] [a (f m)])"
              config)))
    (testing "keys named anywhere in the form count toward the selection"
      (is (empty? (lint! "
(defn f [{:keys! [x]}] x)
(defn g1 [{:keys [a x] :select m}] [a x (f m)])
(defn g2 [{myx :x :select m}] [myx (f m)])
(defn g3 [{:keys [a & :x] :select m}] [a (f m)])
(defn g4 [{:select m :keys [x]}] [x (f m)])
(defn g5 [{:keys! [x] :select m}] [x (f m)])"
                         config))))
    (testing "nested keys are not selected"
      (assert-submaps2
       '({:file "<stdin>", :row 3, :level :error, :message "Missing required key: :x"})
       (lint! "
(defn f [{:keys! [x]}] x)
(defn g [{{:keys [x]} :inner :select m}] [x (f m)])"
              config)))
    (testing ":strs and :syms selections"
      (assert-submaps2
       '({:file "<stdin>", :row 4, :level :error, :message "Missing required key: \"y\""}
         {:file "<stdin>", :row 6, :level :error, :message "Missing required key: s"})
       (lint! "
(defn fs [{:strs! [y]}] y)
(defn ok1 [{:strs [y] :select m}] [y (fs m)])
(defn bad1 [{:strs [z] :select m}] [z (fs m)])
(defn fy [{:syms! [s]}] s)
(defn bad2 [{:syms [t] :select m}] [t (fy m)])
(defn ok2 [{:syms [s] :select m}] [s (fy m)])"
              config)))
    (testing "qualified and auto-resolved selected keys"
      (is (empty? (lint! "(ns foo)
(defn f1 [{:person/keys! [id]}] id)
(defn g1 [{:person/keys [id] :select m}] [id (f1 m)])
(defn f2 [{::keys! [x]}] x)
(defn g2 [{::keys [x] :select m}] [x (f2 m)])"
                         config))))))

(deftest flow-narrowing-test
  (let [config {:linters {:type-mismatch {:level :error}}}]
    (testing "value is narrowed to the predicate's type in the then branch"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(defn f [x] (if (string? x) (inc x) 0))" config))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: number."})
       (lint! "(defn f [x] (if (number? x) (subs x 1) x))" config)))
    (testing "correct usage in the narrowed branch is not flagged"
      (is (empty? (lint! "(defn f [x] (if (string? x) (subs x 1) x))" config))))
    (testing "the else branch is not narrowed"
      (is (empty? (lint! "(defn f [x] (if (string? x) x (inc x)))" config))))
    (testing "a non-predicate condition does not narrow"
      (is (empty? (lint! "(defn f [x] (if x (inc x) 0))" config))))
    (testing "narrowing is dropped when the binding is shadowed"
      (is (empty? (lint! "(defn f [x] (if (string? x) (let [x 1] (inc x)) x))"
                         config))))
    (testing "narrowing a local used only in the branch does not report it unused"
      (is (empty? (lint! "(defn f [x] (let [y x] (if (string? y) (count y) 0)))"
                         config))))
    (testing "a shadowed or redefined predicate does not narrow"
      (let [type-mismatches #(filter (comp #{:type-mismatch} :type) %)]
        (is (empty? (type-mismatches
                     (lint! "(defn f [string? x] (if (string? x) (inc x) 0))" config))))
        (is (empty? (type-mismatches
                     (lint! "(ns a) (defn string? [_] true) (defn f [x] (if (string? x) (inc x) 0))"
                            config))))))))

(deftest when-narrowing-test
  (let [config {:linters {:type-mismatch {:level :error}}}]
    (testing "value is narrowed in the body of when"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: map."})
       (lint! "(defn f [x] (when (map? x) (subs x 1)))" config)))
    (testing "correct usage in the when body is not flagged"
      (is (empty? (lint! "(defn f [x] (when (string? x) (subs x 1)))" config))))
    (testing "when-not does not narrow, its condition is negated"
      (is (empty? (lint! "(defn f [x] (when-not (string? x) (subs x 1)))" config))))))

(deftest nil-narrowing-test
  (let [config {:linters {:type-mismatch {:level :error}}}]
    (testing "an inverted nil guard is flagged"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: nil."})
       (lint! "(defn f [x] (when (nil? x) (inc x)))" config))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: nil."})
       (lint! "(defn f [x] (if (nil? x) (subs x 1) x))" config)))
    (testing "legitimate use of a proven nil is not flagged"
      (is (empty? (lint! "(defn f [x] (when (nil? x) (count x)))" config)))
      (is (empty? (lint! "(defn f [x] (if (nil? x) 0 (inc x)))" config))))))

(deftest parse-fn-test
  (let [config {:linters {:type-mismatch {:level :error}}}]
    (testing "parse-long and friends take a string"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(parse-long 42)" config))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: keyword."})
       (lint! "(parse-double :x)" config))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: natural integer."})
       (lint! "(parse-boolean 0)" config)))
    (testing "a string argument is fine"
      (is (empty? (lint! "(parse-long \"42\")" config))))))

(deftest backward-inference-test
  (let [config {:linters {:type-mismatch {:level :error}}}]
    (testing "a param used in a spec'd position constrains callers"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [s] (subs s 1)) (f 42)" config))
      (is (empty? (lint! "(defn f [s] (subs s 1)) (f \"ok\")" config))))
    (testing "varargs specs constrain"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: keyword."})
       (lint! "(defn f [n] (+ 1 2 n)) (f :kw)" config)))
    (testing "multiple constraints merge to the most specific"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [s] (first s) (subs s 1)) (f 42)" config)))
    (testing "conflicting constraints prove nothing"
      (is (empty? (lint! "(defn f [x] (inc x) (subs x 1)) (f :kw)" config))))
    (testing "a type-dispatched param is protected by branch suppression"
      (is (empty? (lint! "(defn f [x] (if (string? x) (subs x 1) x)) (f 42)" config))))
    (testing "a spine usage after a predicate still constrains, it runs unconditionally"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: nil."})
       (lint! "(defn f [x] (when (nil? x) x) (subs x 1)) (f nil)" config)))
    (testing "some-> guards the threaded value, its calls do not constrain"
      (is (empty? (lint! "(defn g [x] (some-> x (subs 1))) (g nil)" config)))
      (is (empty? (lint! "(defn g [x] (some->> x (subs \"abc\"))) (g nil)" config))))
    (testing "the initial expression of some-> evaluates unconditionally"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [x] (some-> (subs x 1) (str \"!\"))) (f 42)" config)))
    (testing "the first operand of and/or, the first cond test and the condp
              dispatch expr evaluate unconditionally"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [x] (and (subs x 1) true)) (f 42)" config))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [x] (or (subs x 1) :a)) (f 42)" config))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [x] (cond (subs x 1) 1 :else 2)) (f 42)" config))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [x] (condp = (subs x 1) \"a\" 1 2)) (f 42)" config)))
    (testing "plain -> is unconditional and constrains"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn h [x] (-> x (subs 1))) (h 42)" config)))
    (testing "an unresolved call's args are conditionally evaluated, like a when body"
      (is (empty? (lint! "(defn f [x] (unknown.ns/my-when (string? x) (subs x 1))) (f 42)"
                         (assoc-in config [:linters :unresolved-namespace :level] :off)))))
    (testing "a usage in a conditional branch does not constrain"
      (is (empty? (lint! "(defn f [x b] (if b (subs x 1) x)) (f 42 true)" config)))
      (is (empty? (lint! "(defn f [x b] (when b (subs x 1))) (f 42 true)" config)))
      (is (empty? (lint! "(defn f [x b] (cond b (subs x 1) :else x)) (f 42 true)" config)))
      (is (empty? (lint! "(defn f [x b] (and b (subs x 1))) (f 42 true)" config)))
      (is (empty? (lint! "(defn f [x k] (case k :a (subs x 1) x)) (f 42 :b)" config)))
      (is (empty? (lint! "(defn f [x m] (if-let [v (:k m)] (subs x v) x)) (f 42 {})" config))))
    (testing "a usage on a narrowed binding does not constrain"
      (is (empty? (lint! "(defn f [x] (when (string? x) (subs x 1)) x) (f 42)" config))))
    (testing "a nested fn's usage does not constrain the outer param, the fn may run conditionally or never"
      (is (empty? (lint! "(defn f [x] #(subs x 1)) (f 42)" config)))
      (is (empty? (lint! "(defn f [d avg]
                            (let [out (fn [row] (/ 1 (double avg)))]
                              (cond-> d avg (out))))
                          (f {} nil)" config))))
    (testing "a fn in the fn position of a known hof constrains, it is invoked there"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [x coll] (map (fn [i] (subs x i)) coll)) (f 42 [1])" config))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [x coll] (map #(subs x %) coll)) (f 42 [1])" config))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [x a] (swap! a (fn [v] (subs x v)))) (f 42 (atom 1))" config))
      (testing "but a fn created inside the hof fn is not invoked"
        (is (empty? (lint! "(defn f [x coll] (map (fn [_] (fn [] (subs x 1))) coll)) (f 42 [1])" config))))
      (testing "and an enclosing conditional still suppresses"
        (is (empty? (lint! "(defn f [x b coll] (when b (map #(subs x %) coll))) (f 42 true [1])" config)))))
    (testing "a local fn's dormant constraints activate at a spine call"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [x] (let [g (fn [i] (subs x i))] (g 1))) (f 42)" config))
      (testing "also when passed by name to a hof"
        (assert-submaps2
         '({:row 1 :message "Expected: string, received: positive integer."})
         (lint! "(defn f [x coll] (let [g (fn [i] (subs x i))] (map g coll))) (f 42 [1])" config)))
      (testing "and through a chain of local fns, each proven invoked"
        (assert-submaps2
         '({:row 1 :message "Expected: string, received: positive integer."})
         (lint! "(defn f [x] (let [g (fn [i] (subs x i)) h (fn [] (g 1))] (h))) (f 42)" config)))
      (testing "a conditional call site activates nothing, the guard may be what makes it safe"
        (is (empty? (lint! "(defn f [x b] (let [g (fn [i] (subs x i))] (when b (g 1)))) (f 42 true)" config)))
        (is (empty? (lint! "(defn f [x b coll]
                              (let [g (fn [i] (subs x i))]
                                (when b (map g coll))))
                            (f 42 true [1])" config))))
      (testing "a chain link that is never proven invoked stays dormant"
        (is (empty? (lint! "(defn f [x] (let [g (fn [i] (subs x i))] (fn [] (g 1)))) (f 42)" config))))
      (testing "a conditional inside the local fn drops the constraint even when activated"
        (is (empty? (lint! "(defn f [x b] (let [g (fn [i] (when b (subs x i)))] (g 1))) (f 42 true)" config)))))
    (testing "a nested fn created in a conditional branch does not constrain"
      (is (empty? (lint! "(defn f [x b] (when b #(subs x 1))) (f 42 true)" config))))
    (testing "a conditional inside the nested fn does not constrain the outer param"
      (is (empty? (lint! "(defn f [x b] #(if b (subs x 1) x)) (f 42 true)" config))))
    (testing "fn literals infer like defn"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(def g (fn [y] (subs y 1))) (g 42)"
              (assoc-in config [:linters :def-fn :level] :off))))
    (testing "an outer conditional does not suppress a nested fn's own spine"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn outer [b] (when b (defn g [y] (subs y 1)))) (g 42)"
              (assoc-in config [:linters :inline-def :level] :off))))
    (testing "a nilable hint is upgraded when the body proves a non-nil use"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: nil."})
       (lint! "(defn f [^String s] (subs s 1)) (f nil)" config)))
    (testing "a user config spec wins over inference"
      (is (empty? (lint! "(defn f [x] (inc x)) (f \"s\")"
                         (assoc-in config [:linters :type-mismatch :namespaces 'user 'f]
                                   '{:arities {1 {:args [:string]}}})))))
    (testing "a user config :varargs spec covering the arity also suppresses
              inference, observable through a transitive caller"
      (let [cfg (assoc-in config [:linters :type-mismatch :namespaces 'user 'f]
                          '{:arities {:varargs {:min-arity 1 :args [:any]}}})]
        (is (empty? (lint! "(defn f [x] (subs x 1)) (defn g [y] (f y)) (g 42)" cfg)))))
    (testing "a single set spec passes through, symbol takes several types"
      (assert-submaps2
       '({:row 1 :message "Expected: symbol or string or var or keyword, received: positive integer."})
       (lint! "(defn ->sym [x] (symbol x)) (->sym 42)" config))
      (is (empty? (lint! "(defn ->sym [x] (symbol x)) (->sym :kw)" config))))
    (testing "a single keys spec passes through, required keys and value types chain"
      (let [cfg (assoc-in config [:linters :type-mismatch :namespaces 'user 'g]
                          '{:arities {1 {:args [{:op :keys :req {:port :int}}]}}})]
        (assert-submaps2
         '({:row 1 :message "Expected: integer, received: string."}
           {:row 1 :message "Missing required key: :port"})
         (lint! "(defn g [m] m) (defn f [m] (g m)) (f {:port \"x\"}) (f {})" cfg))))
    (testing "constraints meet to their most specific union"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: keyword."})
       (lint! "(defn f [x] (symbol x) (subs x 1)) (f :kw)"
              (assoc-in config [:linters :unused-value :level] :off)))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [x] (symbol x) (contains? x 1)) (f 42)"
              (assoc-in config [:linters :unused-value :level] :off))))
    (testing "the meet of a tag with a union keeps every member that implies it"
      ;; get's union spec meets first's :seqable, a map satisfies the result
      (is (empty? (lint! "(defn f [m] (first m) (get m :k)) (f {:a 1})"
                         (assoc-in config [:linters :unused-value :level] :off))))
      (assert-submaps2
       '({:row 1 :message #"Expected: .*, received: positive integer\."})
       (lint! "(defn f [m] (first m) (get m :k)) (f 42)"
              (assoc-in config [:linters :unused-value :level] :off))))
    (testing "a nilable hint is one union constraint among the others"
      (assert-submaps2
       '({:row 1 :message #"Expected: (string or nil|nil or string), received: positive integer\."})
       (lint! "(defn f [^String s] (println s) s) (f nil) (f 42)" config))
      (is (empty? (lint! "(defn f [^String s] (println s) s) (f nil)" config))))
    (testing "a hint conflicting with the body flags the body, callers are unchecked"
      (assert-submaps2
       '({:row 1 :message #"Expected: number, received: (string or nil|nil or string)\."})
       (lint! "(defn f [^String s] (inc s)) (f nil) (f 42)" config)))
    (testing "a config-specced arity is not inferred, its sibling arity is"
      (let [cfg (assoc-in config [:linters :type-mismatch :namespaces 'user 'f]
                          '{:arities {1 {:args [:any]}}})]
        (is (empty? (lint! "(defn f ([x] (subs x 1)) ([x _y] x)) (f 42)" cfg)))
        (assert-submaps2
         '({:row 1 :message "Expected: string, received: positive integer."})
         (lint! "(defn f ([x] x) ([x _y] (subs x 1))) (f 42 1)" cfg))))))

(deftest backward-inference-keys-test
  (let [config {:linters {:type-mismatch {:level :error}}}]
    (testing "a destructured key's usage becomes its value type"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(defn f [{:keys [x]}] (inc x)) (f {:x \"foo\"})" config))
      (is (empty? (lint! "(defn f [{:keys [x]}] (inc x)) (f {:x 1})" config))))
    (testing "an unconditional nil-rejecting use proves the key required"
      (assert-submaps2
       '({:row 1 :message "Missing required key: :x"})
       (lint! "(defn f [{:keys [x]}] (inc x)) (f {})" config))
      (assert-submaps2
       '({:row 1 :message "Expected: map, received: nil."})
       (lint! "(defn f [{:keys [x]}] (inc x)) (f nil)" config)))
    (testing "a key with an :or default stays optional"
      (is (empty? (lint! "(defn f [{:keys [x] :or {x 0}}] (inc x)) (f {}) (f nil)" config))))
    (testing "a nil-tolerant use keeps the key and a nil argument fine,
              destructuring nil-punts"
      (is (empty? (lint! "(defn f [{:keys [x]}] (count x)) (f {}) (f nil)" config)))
      (is (empty? (lint! "(defn f [{:keys [x]}] (when x (inc x))) (f {}) (f nil)" config))))
    (testing "a non-map argument is reported"
      (assert-submaps2
       '({:row 1 :message "Expected: map, received: positive integer."})
       (lint! "(defn f [{:keys [x]}] (inc x)) (f 42)" config)))
    (testing "renamed and namespaced keys carry their exact key"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn f [{y :y}] (subs y 1)) (f {:y 42})" config))
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(defn f [{:person/keys [age]}] (inc age)) (f {:person/age \"x\"})" config)))
    (testing "a guarded key usage proves nothing"
      (is (empty? (lint! "(defn f [{:keys [x]}] (when (number? x) (inc x))) (f {:x \"s\"})" config))))
    (testing "inferred value types join required keys from :keys!"
      (assert-submaps2
       '({:row 1 :message "Missing required key: :y"}
         {:row 1 :message "Expected: number, received: string."})
       (lint! "(defn f [{:keys! [x y]}] [(inc x) y]) (f {:x \"s\"})" config)))
    (testing "keys specs chain through wrappers"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(defn g [{:keys [x]}] (inc x)) (defn h [m] (g m)) (h {:x \"s\"})" config))
      (assert-submaps2
       '({:row 1 :message "Missing required key: :x"})
       (lint! "(defn g [{:keys [x]}] (inc x)) (defn h [m] (g m)) (h {})" config)))))

(deftest destructured-map-value-types-test
  (let [config {:linters {:type-mismatch {:level :error}}}]
    (testing "a known map init types its destructured keys"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(let [{:keys [port]} {:port \"8080\"}] (inc port))" config))
      (is (empty? (lint! "(let [{:keys [port]} {:port 8080}] (inc port))" config))))
    (testing "a user fn's returned map types destructured keys, resolved lazily"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(defn cfg [] {:port \"8080\"}) (defn go [] (let [{:keys [port]} (cfg)] (inc port)))"
              config))
      (is (empty? (lint! "(defn cfg [] {:port 8080}) (defn go [] (let [{:keys [port]} (cfg)] (inc port)))"
                         config))))
    (testing "a renamed key carries its value type"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(let [{p :port} {:port \"8080\"}] (inc p))" config)))
    (testing "an unknown init types nothing"
      (is (empty? (lint! "(defn f [m] (let [{:keys [port]} m] port)) (f {})" config))))
    (testing "keyword access on a local bound to a user fn's return"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(defn cfg [] {:port \"8080\"}) (defn go [] (let [m (cfg)] (inc (:port m))))"
              config))
      (is (empty? (lint! "(defn cfg [] {:port 8080}) (defn go [] (let [m (cfg)] (inc (:port m))))"
                         config))))
    (testing "nested keyword access chains through a local"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(defn cfg [] {:db {:port \"8080\"}}) (defn go [] (let [m (cfg)] (inc (:port (:db m)))))"
              config)))
    (testing "the :as binding is the whole init"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: map."})
       (lint! "(let [{:as cfg} {:port 8080}] (inc cfg))" config)))
    (testing "nested map destructuring chains value types"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(let [{{:keys [y]} :inner} {:inner {:y \"s\"}}] (inc y))" config))
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(defn cfg [] {:inner {:y \"s\"}}) (defn go [] (let [{{:keys [y]} :inner} (cfg)] (inc y)))"
              config)))
    (testing "string keys via :strs"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(let [{:strs [port]} {\"port\" \"8080\"}] (inc port))" config)))
    (testing "conditional-let bindings are typed from their init"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(when-let [{:keys [port]} {:port \"8080\"}] (inc port))" config))
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(defn cfg [] {:port \"8080\"}) (defn go [] (if-let [{:keys [port]} (cfg)] (inc port) 0))"
              config))
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(when-let [x (subs \"ab\" 0)] (inc x))" config)))
    (testing "a vector binding form does not leak the init tag onto its elements"
      (is (empty? (lint! "(defn ft [] (when true [1 \"x\"])) (defn go [s] (when-let [[i _] (ft)] (subs s 0 i)))"
                         config))))
    (testing "a key missing from a closed literal map is provably nil"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: nil."})
       (lint! "(inc (:y {}))" config))
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: nil."})
       (lint! "(defn cfg [] {}) (defn go [] (let [{{:keys [y]} :inner} (cfg)] (inc y)))" config))
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: nil."})
       (lint! "(inc (:b (:a {})))" config)))
    (testing "an :or default keeps a missing key unknown"
      (is (empty? (lint! "(let [{:keys [y] :or {y 0}} {}] (inc y))" config))))
    (testing "a present key with an unknown value type stays unknown"
      (is (empty? (lint! "(defn f [x] (let [{:keys [a]} {:a x}] (inc a)))" config))))
    (testing "a map that went through into or assoc is open, they add keys"
      (is (empty? (lint! "(defn f [{:keys [x]}] (inc x)) (f (into {} [[:x 1]]))" config)))
      (is (empty? (lint! "(inc (:a (assoc {} :a 1)))" config))))
    (testing "a dynamic key opens the map, it can evaluate to any key"
      (is (empty? (lint! "(let [k :x] (inc (:x {k 1})))" config)))
      (is (empty? (lint! "(defn f [{:keys [x]}] (inc x)) (let [k :x] (f {k 1}))" config))))
    (testing "an inferred key value spec can be a union"
      (assert-submaps2
       '({:row 1 :message "Expected: symbol or string or keyword, received: positive integer."})
       (lint! "(defn f [{:keys [x]}] (keyword x)) (f {:x 1})" config))
      (is (empty? (lint! "(defn f [{:keys [x]}] (keyword x)) (f {:x \"s\"})" config))))
    (testing "a quoted collection key opens the map, map-key cannot extract it"
      (let [cfg (assoc-in config [:linters :type-mismatch :namespaces 'user 'qfn]
                          '{:arities {1 {:args [{:op :keys :req {(a b) :number}}]}}})]
        (is (empty? (lint! "(defn qfn [m] m) (qfn {'(a b) 1})" cfg)))
        (testing "but a genuinely empty map still misses the required key"
          (assert-submaps2
           '({:row 1 :message "Missing required key: (a b)"})
           (lint! "(defn qfn [m] m) (qfn {})" cfg)))))
    (testing "when-first binds an element, not the init"
      (is (empty? (lint! "(when-first [x [\"ok\"]] (subs x 0))" config))))
    (testing "into can overwrite seed values, they prove nothing"
      (is (empty? (lint! "(subs (:x (into {:x 1} [[:x \"ok\"]])) 0)" config)))
      (is (empty? (lint! "(defn f [{:keys [x]}] (inc x)) (f (into {:x \"bad\"} [[:x 1]]))" config))))
    (testing "a dynamic assoc key invalidates earlier value facts"
      (is (empty? (lint! "(let [k :x] (subs (:x (assoc {} :x 1 k \"ok\")) 0))" config))))
    (testing "a value from a fn's return reports at the call with the key,
              its own coordinates belong to the producer"
      (assert-submaps2
       '({:row 1 :col 68
          :message "Expected: string, received: positive integer for key :port"})
       (lint! "(defn cfg [] {:port 1}) (defn f [{:keys [port]}] (subs port 0)) (f (cfg))"
              config))
      (testing "a nil or false key is named, not treated as no key"
        (assert-submaps2
         '({:row 1 :message "Expected: number, received: string for key nil"})
         (lint! "(defn cfg [] {nil \"bad\"}) (defn f [{x nil}] (inc x)) (f (cfg))" config))
        (assert-submaps2
         '({:row 1 :message "Expected: number, received: string for key false"})
         (lint! "(defn cfg [] {false \"bad\"}) (defn f [{x false}] (inc x)) (f (cfg))" config)))
      (testing "the no-key sentinel cannot collide with a real key value"
        (assert-submaps2
         '({:row 1 :message "Expected: number, received: string for key :clj-kondo.impl.types/no-key"})
         (lint! "(defn cfg [] {:clj-kondo.impl.types/no-key \"bad\"}) (defn f [{x :clj-kondo.impl.types/no-key}] (inc x)) (f (cfg))"
                config))))
    (testing "nil and false are valid destructuring keys"
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(defn f [{x nil}] (inc x)) (f {nil \"bad\"})" config))
      (assert-submaps2
       '({:row 1 :message "Expected: number, received: string."})
       (lint! "(let [{x false} {false \"bad\"}] (inc x))" config))
      (assert-submaps2
       '({:row 1 :message "Missing required key: nil"})
       (lint! "(defn f [{x nil}] (inc x)) (f {})" config)))
    (testing "a let-bound literal in the same scope points at the offending value"
      (assert-submaps2
       '({:row 1 :col 44 :message "Expected: number, received: string."})
       (lint! "(defn f [{:keys [x]}] (inc x)) (let [m {:x \"bad\"}] (f m))" config)))
    (testing "a nested keys-spec finding through a fn's return reports at the call"
      (let [cfg (assoc-in config [:linters :type-mismatch :namespaces 'user 'f]
                          '{:arities {1 {:args [{:op :keys
                                                 :req {:a {:op :keys
                                                           :req {:b :string}}}}]}}})]
        (assert-submaps2
         '({:row 1 :col 45
            :message "Expected: string, received: positive integer for key :b"})
         (lint! "(defn cfg [] {:a {:b 1}}) (defn f [m] m) (f (cfg))" cfg))))
    (testing "an assoc'd entry keeps its source position"
      (assert-submaps2
       '({:row 1 :col 48 :message "Expected: number, received: string."})
       (lint! "(defn f [{:keys [x]}] (inc x)) (f (assoc {} :x \"bad\"))" config)))
    (testing "a qualified :keys entry matches its :or default by name"
      (is (empty? (lint! "(let [{:keys [foo/x] :or {x 1}} {}] (inc x))" config))))
    (testing "a provably nil conditional-let init leaves the dead body unchecked"
      (is (empty? (lint! "(when-let [x (:missing {})] (inc \"bad\"))" config)))
      (testing "the else branch of if-let stays live"
        (assert-submaps2
         '({:row 1 :message "Expected: number, received: string."})
         (lint! "(if-let [x (:missing {})] x (inc \"bad\"))" config)))
      (testing "deadness covers any binding form"
        (is (empty? (lint! "(when-let [[x] nil] (inc \"bad\"))" config))))
      (testing "deadness covers destructuring defaults"
        (is (empty? (lint! "(when-let [{:keys [x] :or {x (inc \"bad\")}} (:missing {})] x)"
                           config)))))
    (testing "when-first's condition is seq, not truthiness"
      (is (empty? (lint! "(when-first [x []] x)" config))))
    (testing "a call-shaped map value carries the call's return type"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: number."})
       (lint! "(defn foo [{:keys [x]}] {:a (inc x)}) (subs (:a (foo {:x 1})) 1)" config))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: number."})
       (lint! "(let [m {:a (inc 1)}] (subs (:a m) 1))" config))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: number."})
       (lint! "(defn foo [] {:a (inc 1)}) (defn go [] (let [{:keys [a]} (foo)] (subs a 1)))"
              config))
      (is (empty? (lint! "(defn foo [] {:a (str 1)}) (defn go [] (subs (:a (foo)) 1))" config))))))

(deftest backward-inference-transitive-test
  (let [config {:linters {:type-mismatch {:level :error}}}]
    (testing "inference chains through user fns"
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn bar [s] (subs s 1)) (defn foo [x] (bar x)) (foo 42)" config))
      (assert-submaps2
       '({:row 1 :message "Expected: string, received: positive integer."})
       (lint! "(defn baz [s] (subs s 1)) (defn bar [y] (baz y)) (defn foo [x] (bar x)) (foo 42)"
              config))
      (is (empty? (lint! "(defn bar [s] (subs s 1)) (defn foo [x] (bar x)) (foo \"ok\")" config))))
    (testing "inference chains across namespaces in one run"
      (assert-submaps2
       '({:row 4 :message "Expected: string, received: positive integer."})
       (lint! "(ns aaa)
(defn bar [s] (subs s 1))
(ns bbb (:require [aaa]))
(aaa/bar 42)" config)))
    (testing "recursive call chains do not blow up or infer"
      (is (empty? (lint! "(defn f [x] (f x)) (f 42)" config)))
      (is (empty? (lint! "(declare g) (defn f [x] (g x)) (defn g [y] (f y)) (f 42)" config))))
    (testing "a guarded callee contributes nothing through the chain"
      (is (empty? (lint! "(defn bar [s] (if (string? s) (subs s 1) s)) (defn foo [x] (bar x)) (foo 42)"
                         config))))))

;;;; Scratch

(comment)
