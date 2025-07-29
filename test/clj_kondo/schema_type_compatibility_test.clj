(ns clj-kondo.schema-type-compatibility-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :refer [deftest is testing]]))

(deftest schema-type-compatibility-improvements-test
  "Test our improved schema type compatibility logic"
  
  (testing "Number/int hierarchy compatibility - should NOT warn"
    ;; These were false positives before our improvements
    (is (empty? (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defn add-ten :- s/Int [x :- s/Int] (+ x 10))
      (s/defn multiply :- s/Int [a :- s/Int b :- s/Int] (* a b))
      (s/defn calculate :- s/Num [x :- s/Int] (+ x 5.5))
    "
    '{:linters {:schema-type-mismatch {:level :warning}}}))))
  
  (testing "Nilable type handling - should NOT warn"
    ;; These were failing before our nilable type fixes
    (is (empty? (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defn maybe-string :- (s/maybe s/Str) [flag :- s/Bool] (if flag \"hello\" nil))
      (s/defn optional-number :- (s/maybe s/Int) [x :- s/Int] (when (pos? x) x))
      (s/defn get-maybe :- (s/maybe s/Str) [] nil)
    "
    '{:linters {:schema-type-mismatch {:level :warning}}}))))
  
  (testing "Map compatibility with extra keys - should NOT warn"
    ;; Extra keys should be allowed
    (is (empty? (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defschema UserSchema {:name s/Str :age s/Int})
      (s/defn create-user :- UserSchema [name :- s/Str age :- s/Int] 
        {:name name :age age :extra \"allowed\"})
      (s/defn build-user :- UserSchema [data :- {:name s/Str :age s/Int :id s/Int}]
        (select-keys data [:name :age]))
    "
    '{:linters {:schema-type-mismatch {:level :warning}}}))))

  (testing "Schema reference resolution - should NOT warn"
    ;; Custom schema references should resolve properly
    (is (empty? (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defschema SimpleSchema s/Str)
      (s/defn use-simple :- SimpleSchema [x :- s/Str] x)
      (s/defschema ComplexSchema {:id s/Int :data SimpleSchema})
      (s/defn use-complex :- ComplexSchema [id :- s/Int data :- SimpleSchema] 
        {:id id :data data})
    "
    '{:linters {:schema-type-mismatch {:level :warning}}}))))

  (testing "Collection type compatibility - should NOT warn"
    ;; Collection element type compatibility
    (is (empty? (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defn make-vector :- [s/Int] [nums :- [s/Num]] (mapv int nums))
      (s/defn process-set :- #{s/Str} [strs :- #{s/Any}] (set (map str strs)))
    "
    '{:linters {:schema-type-mismatch {:level :warning}}}))))

  (testing "Negative cases - SHOULD still warn about real mismatches"
    ;; These should continue to produce warnings
    (let [findings (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defschema UserSchema {:name s/Str :age s/Int})
      (s/defn bad-return :- s/Int [x :- s/Str] x)
      (s/defn bad-nilable :- (s/maybe s/Int) [] \"not-int-or-nil\")
      (s/defn bad-map :- UserSchema [name :- s/Str] {:wrong \"structure\"})
      (s/defn bad-vector :- [s/Int] [] [\"not\" \"ints\"])
    "
    '{:linters {:schema-type-mismatch {:level :warning}}})]
      
      ;; Should have warnings for actual type mismatches
      (is (>= (count findings) 4) "Should detect real type mismatches")
      
      ;; Verify specific error messages
      (is (some #(and (= (:level %) :warning)
                      (clojure.string/includes? (:message %) "Expected: integer, actual: string"))
                findings)
          "Should detect string returned instead of int")
      
      (is (some #(and (= (:level %) :warning)
                      (clojure.string/includes? (:message %) "int or nil"))
                findings)
          "Should detect wrong type for nilable")))

  (testing "Complex nested scenarios - comprehensive tests"
    ;; Test various combinations
    (is (empty? (lint! "
      (ns test (:require [schema.core :as s]))
      
      ;; Deep schema references
      (s/defschema Level1 s/Int)
      (s/defschema Level2 Level1) 
      (s/defschema Level3 Level2)
      (s/defn deep-reference :- Level3 [x :- s/Int] x)
      
      ;; Optional keys with extra fields
      (s/defschema AdvancedUser 
        {:name s/Str 
         :age s/Int
         (s/optional-key :email) s/Str})
      (s/defn create-advanced :- AdvancedUser [name :- s/Str age :- s/Int]
        {:name name :age age :email \"optional\" :extra \"allowed\"})
      
      ;; Nested collections
      (s/defn process-nested :- [[s/Int]] [data :- [[s/Num]]]
        (mapv #(mapv int %) data))
    "
    '{:linters {:schema-type-mismatch {:level :warning}}}))))

  (testing "Boundary and edge cases"
    ;; Test edge cases that might break
    (is (empty? (lint! "
      (ns test (:require [schema.core :as s]))
      
      ;; Empty collections
      (s/defn empty-vector :- [s/Int] [] [])
      (s/defn empty-map :- {s/Str s/Int} [] {})
      
      ;; Type hierarchy boundaries
      (s/defn boundary-number :- s/Num [x :- s/Int] (double x))
      (s/defn boundary-any :- s/Any [x :- s/Str] x)
      
      ;; Double nilable
      (s/defn nil-edge-case :- (s/maybe (s/maybe s/Str)) [] nil)
    "
    '{:linters {:schema-type-mismatch {:level :warning}}}))))

  (testing "Performance with large schemas"
    ;; Ensure performance doesn't degrade with complex schemas
    (is (empty? (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defschema LargeSchema
        {:field1 s/Str :field2 s/Int :field3 s/Bool :field4 s/Keyword
         :field5 s/Str :field6 s/Int :field7 s/Bool :field8 s/Keyword
         :field9 s/Str :field10 s/Int :field11 s/Bool :field12 s/Keyword})
      (s/defn create-large :- LargeSchema []
        {:field1 \"a\" :field2 1 :field3 true :field4 :kw
         :field5 \"b\" :field6 2 :field7 false :field8 :kw2
         :field9 \"c\" :field10 3 :field11 true :field12 :kw3
         :extra-field \"allowed\"})
    "
    '{:linters {:schema-type-mismatch {:level :warning}}}))))

  (testing "Regression tests for previously broken cases"
    ;; Ensure our fixes don't regress
    (is (empty? (lint! "
      (ns test (:require [schema.core :as s]))
      
      ;; Previously failed: arithmetic with int expectation
      (s/defn regression-arithmetic :- s/Int [a :- s/Int b :- s/Int] (+ a b 1 2 3))
      
      ;; Previously failed: nilable parsing  
      (s/defn regression-nilable :- (s/maybe s/Int) [x :- s/Int] (when (even? x) x))
      
      ;; Previously failed: map extra keys
      (s/defschema RegressionSchema {:id s/Int :name s/Str})
      (s/defn regression-map :- RegressionSchema [id :- s/Int name :- s/Str extra :- s/Str]
        {:id id :name name :description extra :metadata {}})
    "
    '{:linters {:schema-type-mismatch {:level :warning}}})))))