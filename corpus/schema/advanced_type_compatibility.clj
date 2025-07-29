(ns schema.advanced-type-compatibility
  (:require [schema.core :as s]))

;; ============================================================================
;; ADVANCED SCHEMA TYPE COMPATIBILITY TESTS
;; ============================================================================

;; 1. CONSTRAINED AND PREDICATE TYPES
(s/defn positive-number :- (s/constrained s/Int pos?)
  [x :- s/Int]
  (Math/abs x))  ; Should work - returns int that may be positive

(s/defn filtered-string :- (s/pred string?)
  [x :- s/Str]
  x)  ; Should work - predicate types

;; 2. EITHER/UNION TYPES  
(s/defn flexible-return :- (s/either s/Int s/Str)
  [flag :- s/Bool]
  (if flag 42 "hello"))  ; Should work - returns either int or string

(s/defn bad-either :- (s/either s/Int s/Str)
  []
  [])  ; Should warn - returns vector, not int or string

;; 3. ENUM TYPES
(s/defschema Status (s/enum :active :inactive :pending))

(s/defn get-status :- Status
  [code :- s/Int]
  (case code
    1 :active
    2 :inactive
    :pending))  ; Should work

(s/defn bad-status :- Status
  []
  :unknown)  ; Should warn - :unknown not in enum

;; 4. CONDITIONAL TYPES (should fall back to :any)
(s/defn conditional-example :- (s/conditional map? {:a s/Int} vector? [s/Str])
  [x :- s/Any]
  x)  ; Should work - complex conditional types

;; 5. RECURSIVE SCHEMA REFERENCES
(s/defschema TreeNode
  {:value s/Int
   :children [(s/recursive #(s/maybe TreeNode))]})

(s/defn make-tree :- TreeNode
  [value :- s/Int]
  {:value value :children []})  ; Should work with recursive schema

;; 6. NESTED COLLECTION TYPES WITH COMPATIBILITY
(s/defn process-nested :- [[s/Int]]
  [data :- [[s/Num]]]
  (mapv #(mapv int %) data))  ; Nested vector compatibility

(s/defn transform-map :- {s/Str s/Int}
  [data :- {s/Keyword s/Num}]
  (into {} (map (fn [[k v]] [(name k) (int v)]) data)))  ; Map key/value transformation

;; 7. FUNCTION SCHEMA TYPES (disabled - advanced feature)
;; (s/defschema IntToStr (=> [s/Int] s/Str))
;; 
;; (s/defn use-function :- s/Str
;;   [f :- IntToStr x :- s/Int]
;;   (f x))  ; Function type usage

;; 8. PROTOCOL TYPES (should fall back gracefully)
(defprotocol TestProtocol
  (test-method [this]))

(s/defn use-protocol :- (s/protocol TestProtocol)
  [x :- (s/protocol TestProtocol)]
  x)  ; Protocol type compatibility

;; ============================================================================
;; PERFORMANCE AND EDGE CASE TESTS
;; ============================================================================

;; Deep nesting
(s/defschema DeepNested
  {:level1 {:level2 {:level3 {:level4 {:level5 s/Str}}}}})

(s/defn create-deep :- DeepNested
  [value :- s/Str]
  {:level1 {:level2 {:level3 {:level4 {:level5 value}}}}})

;; Large map schemas
(s/defschema LargeSchema
  {:field1 s/Str :field2 s/Int :field3 s/Bool :field4 s/Keyword
   :field5 s/Str :field6 s/Int :field7 s/Bool :field8 s/Keyword
   :field9 s/Str :field10 s/Int :field11 s/Bool :field12 s/Keyword})

(s/defn create-large :- LargeSchema
  []
  {:field1 "a" :field2 1 :field3 true :field4 :kw
   :field5 "b" :field6 2 :field7 false :field8 :kw2
   :field9 "c" :field10 3 :field11 true :field12 :kw3
   :extra-field "should be allowed"})  ; Extra field in large map

;; Mixed optional and required keys
(s/defschema MixedSchema
  {:required1 s/Str
   :required2 s/Int
   (s/optional-key :opt1) s/Str
   (s/optional-key :opt2) s/Int
   (s/optional-key :opt3) s/Bool})

(s/defn create-mixed :- MixedSchema
  [r1 :- s/Str r2 :- s/Int]
  {:required1 r1
   :required2 r2
   :opt1 "optional"
   :extra "allowed"})  ; Mix of required, optional, and extra

;; ============================================================================
;; BOUNDARY AND ERROR CASES
;; ============================================================================

;; Nil handling edge cases
(s/defn nil-edge-case :- (s/maybe (s/maybe s/Str))
  []
  nil)  ; Double-maybe with nil

;; Empty collections
(s/defn empty-vector :- [s/Int]
  []
  [])  ; Empty vector should be compatible

(s/defn empty-map :- {s/Str s/Int}
  []
  {})  ; Empty map should be compatible

;; Type coercion boundaries
(s/defn boundary-number :- s/Num
  [x :- s/Int]
  (double x))  ; Int to Num should work

(s/defn boundary-any :- s/Any
  [x :- s/Str]
  x)  ; Anything to Any should work

;; Schema reference chains
(s/defschema Chain1 s/Str)
(s/defschema Chain2 Chain1)
(s/defschema Chain3 Chain2)
(s/defschema Chain4 Chain3)

(s/defn use-chain :- Chain4
  [x :- Chain1]
  x)  ; Long schema reference chain

;; ============================================================================
;; REGRESSION TESTS - Cases that previously failed
;; ============================================================================

;; Previously failed: arithmetic with int expectation
(s/defn regression-arithmetic :- s/Int
  [a :- s/Int b :- s/Int]
  (+ a b 1 2 3))  ; Multiple arithmetic operands

;; Previously failed: nilable parsing
(s/defn regression-nilable :- (s/maybe s/Int)
  [x :- s/Int]
  (when (even? x) x))  ; Conditional return of int or nil

;; Previously failed: map extra keys
(s/defschema RegressionSchema {:id s/Int :name s/Str})

(s/defn regression-map :- RegressionSchema
  [id :- s/Int name :- s/Str extra :- s/Str]
  {:id id :name name :description extra :metadata {}})  ; Multiple extra keys 