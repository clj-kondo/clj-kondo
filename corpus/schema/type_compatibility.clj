(ns schema.type-compatibility
  (:require [schema.core :as s]))

;; ============================================================================
;; Test cases for our improved schema type compatibility logic
;; ============================================================================

;; 1. NUMBER/INT HIERARCHY TESTS
;; These should NOT produce warnings after our improvements

(s/defn add-ten :- s/Int
  [x :- s/Int]
  (+ x 10))  ; (+ x 10) returns :number but should be compatible with :int

(s/defn multiply :- s/Int  
  [a :- s/Int b :- s/Int]
  (* a b))  ; Arithmetic operations should be compatible with int expectations

(s/defn calculate :- s/Num
  [x :- s/Int]
  (+ x 5.5))  ; Int input to number output should work

;; 2. NILABLE TYPE TESTS
;; These should NOT produce warnings after our improvements

(s/defn maybe-string :- (s/maybe s/Str)
  [flag :- s/Bool]
  (if flag "hello" nil))  ; Should accept both string and nil

(s/defn optional-number :- (s/maybe s/Int)
  [x :- s/Int]
  (when (pos? x) x))  ; Should accept int or nil

(s/defn get-maybe :- (s/maybe s/Str)
  []
  nil)  ; Direct nil should be compatible

;; 3. MAP COMPATIBILITY TESTS  
;; These should NOT produce warnings after our improvements

(s/defschema UserSchema
  {:name s/Str
   :age s/Int})

(s/defn create-user :- UserSchema
  [name :- s/Str age :- s/Int]
  {:name name :age age :extra "allowed"})  ; Extra keys should be compatible

(s/defn build-user :- UserSchema
  [data :- {:name s/Str :age s/Int :id s/Int}]
  (select-keys data [:name :age]))  ; Should work with map that has extra keys

;; 4. SCHEMA REFERENCE TESTS
;; These should NOT produce warnings after our improvements

(s/defschema SimpleSchema s/Str)

(s/defn use-simple :- SimpleSchema
  [x :- s/Str]
  x)  ; Schema reference should resolve correctly

(s/defschema ComplexSchema
  {:id s/Int
   :data SimpleSchema})

(s/defn use-complex :- ComplexSchema  
  [id :- s/Int data :- SimpleSchema]
  {:id id :data data})  ; Nested schema references

;; 5. COLLECTION TYPE TESTS
;; These should NOT produce warnings after our improvements

(s/defn make-vector :- [s/Int]
  [nums :- [s/Num]]
  (mapv int nums))  ; Vector element type compatibility

(s/defn process-set :- #{s/Str}
  [strs :- #{s/Any}]
  (set (map str strs)))  ; Set element type compatibility

;; ============================================================================
;; NEGATIVE TEST CASES - These SHOULD produce warnings
;; ============================================================================

;; Wrong return types - should still be caught
(s/defn bad-return :- s/Int
  [x :- s/Str]
  x)  ; Returns string instead of int

(s/defn bad-nilable :- (s/maybe s/Int)
  []
  "not-int-or-nil")  ; Returns string, not int or nil

;; Wrong map structure - should still be caught  
(s/defn bad-map :- UserSchema
  [name :- s/Str]
  {:wrong "structure"})  ; Missing required keys

;; Wrong collection types - should still be caught
(s/defn bad-vector :- [s/Int]
  []
  ["not" "ints"])  ; Vector with wrong element types

;; ============================================================================
;; EDGE CASES AND COMPLEX SCENARIOS
;; ============================================================================

;; Nested nilable types
(s/defn nested-maybe :- (s/maybe [s/Str])
  [flag :- s/Bool]
  (when flag ["hello" "world"]))

;; Multiple levels of schema references
(s/defschema Level1 s/Int)
(s/defschema Level2 Level1)
(s/defschema Level3 Level2)

(s/defn deep-reference :- Level3
  [x :- s/Int]
  x)

;; Complex map with optional keys
(s/defschema AdvancedUser
  {:name s/Str
   :age s/Int
   (s/optional-key :email) s/Str
   (s/optional-key :phone) s/Str})

(s/defn create-advanced-user :- AdvancedUser
  [name :- s/Str age :- s/Int]
  {:name name 
   :age age 
   :email "optional@example.com"
   :extra "this should be allowed"})  ; Extra key beyond optional 