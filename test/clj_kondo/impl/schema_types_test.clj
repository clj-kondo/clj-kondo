(ns clj-kondo.impl.schema-types-test
  (:require
   [clj-kondo.impl.schema-types :as schema-types]
   [clojure.string]
   [clojure.test :refer [deftest is testing are]]))

(deftest extract-schema-type-test
  "Test schema type extraction for various schema forms"
  
  (testing "Basic schema types"
    (are [schema-form expected] (= expected (schema-types/extract-schema-type {:value schema-form}))
      's/Str :string
      's/Int :int
      's/Num :number
      's/Bool :boolean
      's/Keyword :keyword
      's/Symbol :symbol
      's/Any :any
      's/Regex :regex
      'schema.core/Str :string
      'schema.core/Int :int))
  
  (testing "Nilable types extraction"
    (are [schema-form expected] (= expected (schema-types/extract-schema-type {:value schema-form}))
      '(s/maybe s/Str) :nilable/string
      '(s/maybe s/Int) :nilable/int
      '(maybe s/Bool) :nilable/boolean))
  
  (testing "Collection types extraction"
    (is (= {:type :vector :element-type :string}
           (schema-types/extract-schema-type {:value '[s/Str]})))
    (is (= {:type :set :element-type :int}
           (schema-types/extract-schema-type {:value '#{s/Int}})))
    (is (= :vector (schema-types/extract-schema-type {:value '[]})))))

(deftest schema-type-compatible?-test
  "Test our enhanced schema type compatibility logic"
  
  (testing "Number/int hierarchy compatibility"
    ;; Our key improvement: number should be compatible with int expectation
    (is (schema-types/schema-type-compatible? :int :number)
        "Number should be compatible with int expectation")
    (is (not (schema-types/schema-type-compatible? :number :int))
        "Int should not be compatible with number expectation (stricter)"))
  
  (testing "Nilable type compatibility"
    ;; Our key improvement: fixed nilable type parsing
    (is (schema-types/schema-type-compatible? :nilable/string :nil)
        "Nilable string should accept nil")
    (is (schema-types/schema-type-compatible? :nilable/string :string)
        "Nilable string should accept string")
    (is (not (schema-types/schema-type-compatible? :nilable/string :int))
        "Nilable string should reject int")
    (is (schema-types/schema-type-compatible? :nilable/int :nil)
        "Nilable int should accept nil")
    (is (schema-types/schema-type-compatible? :nilable/int :int)
        "Nilable int should accept int")
    (is (schema-types/schema-type-compatible? :nilable/int :number)
        "Nilable int should accept number (hierarchy)"))
  
  (testing "Map compatibility"
    (let [user-schema {:type :map 
                       :keys {:name :string :age :int}
                       :req-keys [:name :age]}
          user-with-extra {:type :map
                          :keys {:name :string :age :int :extra :any}
                          :req-keys [:name :age :extra]}
          incomplete-user {:type :map
                          :keys {:name :string}
                          :req-keys [:name]}]
      
      ;; Our key improvement: maps with extra keys should be compatible
      (is (schema-types/schema-type-compatible? user-schema user-with-extra)
          "Map with extra keys should be compatible")
      
      ;; Maps missing required keys should not be compatible
      (is (not (schema-types/schema-type-compatible? user-schema incomplete-user))
          "Map missing required keys should not be compatible")))
  
  (testing "Collection type compatibility"
    (let [int-vector {:type :vector :element-type :int}
          number-vector {:type :vector :element-type :number}
          string-vector {:type :vector :element-type :string}]
      
      ;; Our improvement: element type hierarchy should work
      (is (schema-types/schema-type-compatible? int-vector number-vector)
          "Vector with number elements should be compatible with int vector expectation")
      
      (is (not (schema-types/schema-type-compatible? int-vector string-vector))
          "Vector with string elements should not be compatible with int vector")))
  
  (testing "Any type compatibility"
    ;; :any should be compatible with everything
    (is (schema-types/schema-type-compatible? :any :string))
    (is (schema-types/schema-type-compatible? :string :any))
    (is (schema-types/schema-type-compatible? :any {:type :map}))
    (is (schema-types/schema-type-compatible? {:type :vector} :any)))
  
  (testing "Exact type matching"
    (are [expected actual] (schema-types/schema-type-compatible? expected actual)
      :string :string
      :int :int
      :boolean :boolean
      {:type :map} {:type :map}))
  
  (testing "Incompatible types"
    (are [expected actual] (not (schema-types/schema-type-compatible? expected actual))
      :string :int
      :boolean :string
      :int :boolean
      {:type :vector} {:type :map})))

(deftest schema-type-to-string-test
  "Test human-readable string conversion"
  
  (testing "Basic type strings"
    (are [schema-type expected] (= expected (schema-types/schema-type-to-string schema-type))
      :string "string"
      :int "integer"
      :number "number"
      :boolean "boolean"
      :keyword "keyword"
      :symbol "symbol"
      :any "any"
      :nil "nil"))
  
  (testing "Complex type strings"
    (is (= "vector" (schema-types/schema-type-to-string {:type :vector})))
    (is (= "vector of string" 
           (schema-types/schema-type-to-string {:type :vector :element-type :string})))
    (is (= "set of integer"
           (schema-types/schema-type-to-string {:type :set :element-type :int})))
    
    ;; Map with keys should show structure
    (let [map-type {:type :map :keys {:name :string :age :int}}]
      (is (clojure.string/includes? (schema-types/schema-type-to-string map-type) "name: string")))
    
    ;; Nilable types
    (is (= "string" (schema-types/schema-type-to-string :nilable/string)))))

(deftest schema-registry-test
  "Test schema definition storage and lookup"
  
  (testing "Schema storage and retrieval"
    ;; Reset registry for clean test
    (reset! schema-types/*schema-registry* {})
    
    ;; Store a simple schema
    (schema-types/store-schema-definition! {} 'TestSchema {:value 's/Int})
    
    ;; Should be able to retrieve it
    (is (= :int (schema-types/lookup-schema-definition 'TestSchema)))
    
    ;; Schema extraction should use registry for unknown symbols
    (is (= :int (schema-types/extract-schema-type {:value 'TestSchema})))
    
    ;; Unknown schemas should return :any
    (is (= :any (schema-types/extract-schema-type {:value 'UnknownSchema})))))

(deftest edge-cases-test
  "Test edge cases and boundary conditions"
  
  (testing "Nil and empty values"
    (is (nil? (schema-types/extract-schema-type nil)))
    (is (= :any (schema-types/extract-schema-type {:value nil}))))
  
  (testing "Complex nested structures"
    (let [nested-schema {:value '{:level1 {:level2 {:level3 s/Str}}}}
          result (schema-types/extract-schema-type nested-schema)]
      (is (map? result))
      (is (= :map (:type result)))))
  
  (testing "Malformed schemas"
    ;; Should gracefully handle malformed input
    (is (= :any (schema-types/extract-schema-type {:value 'not-a-schema})))
    ;; Empty vector is a valid schema (empty vector), not malformed
    (is (= :vector (schema-types/extract-schema-type {:value []}))))
  
  (testing "Performance with large structures"
    ;; Should handle large schemas without issues
    (let [large-map (into {} (map #(vector (keyword (str "field" %)) :string) (range 100)))
          large-schema {:value large-map}]
      (is (map? (schema-types/extract-schema-type large-schema))))))

(deftest compatibility-regression-test
  "Regression tests for our specific improvements"
  
  (testing "Arithmetic expression compatibility (was failing)"
    ;; These were false positives before our improvements
    (is (schema-types/schema-type-compatible? :int :number)
        "Arithmetic expressions return :number but should be compatible with :int"))
  
  (testing "Nilable keyword parsing (was failing)"
    ;; This was broken due to namespaced keyword handling
    (is (schema-types/schema-type-compatible? :nilable/string :string)
        "Nilable string parsing should work correctly"))
  
  (testing "Map extra keys (was failing)"
    ;; Maps with extra keys should be compatible
    (let [minimal-map {:type :map :keys {:name :string} :req-keys [:name]}
          extended-map {:type :map :keys {:name :string :extra :any} :req-keys [:name :extra]}]
      (is (schema-types/schema-type-compatible? minimal-map extended-map)
          "Maps with extra keys should be compatible")))
  
  (testing "Schema reference resolution (was failing)"
    ;; Custom schemas should resolve properly
    (reset! schema-types/*schema-registry* {'CustomType :string})
    (is (= :string (schema-types/extract-schema-type {:value 'CustomType}))
        "Custom schema references should resolve"))) 