(ns clj-kondo.error-message-quality-test
  "Tests for human-readable error message improvements"
  (:require
   [clj-kondo.test-utils :refer [lint!]]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(deftest error-message-readability-test
  "Test that all our error messages are human-readable and helpful"
  
  (testing "Simple type mismatch messages"
    (let [findings (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defn get-string :- s/Str [] 42)
      (s/defn get-number :- s/Int [] \"hello\")
      (s/defn get-boolean :- s/Bool [] :keyword)
    " '{:linters {:schema-type-mismatch {:level :warning}}})]
      
      (is (= 3 (count findings)) "Should detect all three type mismatches")
      
      ;; Test that messages are clear and consistent
      (let [messages (map :message findings)]
        (is (every? #(str/includes? % "Expected:") messages)
            "All messages should clearly state expected type")
        (is (every? #(str/includes? % "actual:") messages)
            "All messages should clearly state actual type")
        (is (some #(str/includes? % "string") messages)
            "Should mention string type")
        (is (some #(str/includes? % "integer") messages)
            "Should mention integer type")
        (is (some #(str/includes? % "boolean") messages)
            "Should mention boolean type"))))
  
  (testing "Union type error messages are readable"
    (let [findings (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defn bad-maybe :- (s/maybe s/Str) [] 42)
      (s/defn bad-either :- (s/either s/Int s/Bool) [] \"wrong\")
    " '{:linters {:schema-type-mismatch {:level :warning}}})]
      
      (when (seq findings)
        (let [messages (map :message findings)]
          ;; Should NOT contain ugly AST representations
          (is (not-any? #(str/includes? % "{:tag") messages)
              "Should not contain raw AST representations")
          (is (not-any? #(str/includes? % ":end-col") messages)
              "Should not contain AST metadata")
          
          ;; Should contain readable union descriptions
          (is (some #(or (str/includes? % "or nil")
                         (str/includes? % "or ")
                         (str/includes? % "string or nil")) messages)
              "Should contain readable union type descriptions")))))
  
  (testing "Complex schema error messages"
    (let [findings (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defschema User {:name s/Str :age s/Int})
      (s/defn bad-user :- User [] {:name 123 :age \"not-number\"})
      (s/defn missing-keys :- User [] {:name \"john\"})
    " '{:linters {:schema-type-mismatch {:level :warning}}})]
      
      (when (seq findings)
        (let [messages (map :message findings)]
          ;; Should mention schema names when possible
          (is (some #(or (str/includes? % "User")
                         (str/includes? % "map")
                         (str/includes? % "{")) messages)
              "Should reference schema or map structure")))))
  
  (testing "Collection type error messages"
    (let [findings (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defn bad-vector :- [s/Str] [] [1 2 3])
      (s/defn bad-set :- #{s/Int} [] #{\"a\" \"b\"})
    " '{:linters {:schema-type-mismatch {:level :warning}}})]
      
      (when (seq findings)
        (let [messages (map :message findings)]
          ;; Should describe collection types clearly
          (is (some #(or (str/includes? % "vector")
                         (str/includes? % "set")
                         (str/includes? % "collection")) messages)
              "Should describe collection types clearly")))))
  
  (testing "Error message consistency"
    (let [findings (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defn consistent1 :- s/Str [] 42)
      (s/defn consistent2 :- s/Int [] \"hello\")
      (s/defn consistent3 :- s/Bool [] :keyword)
    " '{:linters {:schema-type-mismatch {:level :warning}}})]
      
      (is (= 3 (count findings)) "Should detect all mismatches")
      
      (let [messages (map :message findings)]
        ;; All messages should follow the same format
        (is (every? #(re-matches #".*Expected: .*, actual: .*\." %) messages)
            "All messages should follow consistent format")
        
        ;; Should not mix different error message styles
        (let [message-styles (map #(if (str/includes? % "Schema type mismatch") :schema :other) messages)]
          (is (or (every? #(= :schema %) message-styles)
                  (every? #(= :other %) message-styles))
              "Should use consistent message style"))))))

(deftest error-message-context-test
  "Test that error messages provide helpful context"
  
  (testing "Function name and location context"
    (let [findings (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defn problematic-function :- s/Str [] 42)
    " '{:linters {:schema-type-mismatch {:level :warning}}})]
      
      (is (= 1 (count findings)) "Should detect the mismatch")
      
      (let [finding (first findings)]
        ;; Should have location information
        (is (number? (:row finding)) "Should have row information")
        (is (number? (:col finding)) "Should have column information")
        (is (string? (:file finding)) "Should have file information"))))
  
  (testing "Schema name preservation in errors"
    (let [findings (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defschema CustomType s/Str)
      (s/defn bad-custom :- CustomType [] 42)
    " '{:linters {:schema-type-mismatch {:level :warning}}})]
      
      (when (seq findings)
        ;; Should preserve schema context when possible
        (let [message (:message (first findings))]
          (is (or (str/includes? message "string")
                  (str/includes? message "CustomType"))
              "Should preserve meaningful schema information")))))
  
  (testing "Nested schema context"
    (let [findings (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defschema Address {:street s/Str :city s/Str})
      (s/defschema Person {:name s/Str :address Address})
      (s/defn bad-person :- Person [] 
        {:name \"John\" :address \"not-an-address\"})
    " '{:linters {:schema-type-mismatch {:level :warning}}})]
      
      (when (seq findings)
        ;; Should provide context about nested structure
        (let [message (:message (first findings))]
          (is (or (str/includes? message "Address")
                  (str/includes? message "map")
                  (str/includes? message "Person"))
              "Should provide context about schema structure"))))))

(deftest error-message-special-cases-test
  "Test error messages for special cases and edge conditions"
  
  (testing "Nil handling in error messages"
    (let [findings (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defn no-nil :- s/Str [] nil)
      (s/defn needs-nil :- (s/maybe s/Str) [] 42)
    " '{:linters {:schema-type-mismatch {:level :warning}}})]
      
      (when (seq findings)
        (let [messages (map :message findings)]
          ;; Should handle nil appropriately in messages
          (is (some #(str/includes? % "nil") messages)
              "Should mention nil when relevant")))))
  
  (testing "Any type handling"
    (let [findings (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defn specific-from-any :- s/Str [x :- s/Any] x)
      (s/defn any-from-specific :- s/Any [x :- s/Str] x)
    " '{:linters {:schema-type-mismatch {:level :warning}}})]
      
      ;; s/Any should generally be compatible with everything
      ;; If there are warnings, they should be reasonable
      (is (every? #(not (str/includes? % "Expected: any")) (map :message findings))
          "Should not warn about any type expectations")))
  
  (testing "Recursive schema error messages"
    (let [findings (lint! "
      (ns test (:require [schema.core :as s]))
      (s/defschema TreeNode {:value s/Int :children [TreeNode]})
      (s/defn bad-tree :- TreeNode [] {:value \"not-int\" :children []})
    " '{:linters {:schema-type-mismatch {:level :warning}}})]
      
      (when (seq findings)
        ;; Should handle recursive schemas gracefully
        (let [message (:message (first findings))]
          (is (not (str/includes? message "StackOverflow"))
              "Should not cause stack overflow in error generation")
          (is (or (str/includes? message "integer")
                  (str/includes? message "TreeNode"))
              "Should provide meaningful error for recursive schemas"))))))

(deftest error-message-performance-test
  "Test that error message generation is efficient"
  
  (testing "Large schema error message performance"
    ;; Generate a schema with many fields
    (let [large-schema-code
          (str "(ns test (:require [schema.core :as s]))\n"
               "(s/defschema LargeSchema {\n"
               (str/join "\n" (for [i (range 20)]
                               (str ":field" i " s/Str")))
               "})\n"
               "(s/defn bad-large :- LargeSchema [] {})\n")]
      
      ;; Test performance and result quality
      (let [start-time (System/currentTimeMillis)
            findings (lint! large-schema-code
                           '{:linters {:schema-type-mismatch {:level :warning}}})
            duration (- (System/currentTimeMillis) start-time)]
        
        ;; Should complete reasonably quickly (less than 5 seconds)
        (is (< duration 5000)
            "Error message generation should be efficient for large schemas")
        
        ;; Check message quality
        (when (seq findings)
          (is (< (count (:message (first findings))) 10000)
              "Error messages should not be excessively long"))))))