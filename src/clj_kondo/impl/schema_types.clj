(ns clj-kondo.impl.schema-types
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :as utils]
   [clojure.string :as str]
   [clojure.set :as set]))

;;;; Schema Type System Integration
;;;; Converts Prismatic Schema types to clj-kondo's type system

(defonce ^:dynamic *schema-registry* (atom {}))

(defn extract-schema-type
  "Extract a clj-kondo type representation from a schema AST node"
  ([schema-node] (extract-schema-type schema-node nil))
  ([schema-node _ctx]
   (when schema-node
    (let [value (if (and (map? schema-node)
                         (contains? schema-node :children)
                         (seq (:children schema-node)))
                  ;; For SeqNodes, convert to sexpr to get the actual form
                  (try 
                    (utils/sexpr schema-node)
                    (catch Exception _
                      (:value schema-node)))
                  (if (and (map? schema-node) (contains? schema-node :value))
                    (:value schema-node)
                    schema-node))]
      
      (cond
        ;; AST Map nodes (parsed map literals in schema definitions)
        (= :map (:tag schema-node))
        (let [children (:children schema-node)
              extract-key-info (fn [node]
                                 (cond
                                   (:k node) (:k node)  ; keyword key like :name
                                   (:value node) (:value node)  ; other values
                                   (= :list (:tag node)) (utils/sexpr node)  ; list forms like (s/optional-key :nickname)
                                   :else nil))
              ;; Pair up keys and values from the children list
              pairs (partition 2 children)
              process-pair (fn [[key-node value-node]]
                             (let [key-info (extract-key-info key-node)
                                   value-type (extract-schema-type value-node)]
                               [key-info value-type]))
              processed-pairs (map process-pair pairs)
              ;; Separate required and optional keys
              req-pairs (filter (fn [[k _]] (not (and (list? k) 
                                                       (or (= 'optional-key (first k))
                                                           (= 's/optional-key (first k)))))) processed-pairs)
              opt-pairs (filter (fn [[k _]] (and (list? k) 
                                                  (or (= 'optional-key (first k))
                                                      (= 's/optional-key (first k))))) processed-pairs)
              ;; Extract the actual keys from optional-key forms
              req-keys (map first req-pairs)
              opt-keys (map (fn [[k _]] (if (and (list? k) 
                                                 (or (= 'optional-key (first k))
                                                     (= 's/optional-key (first k))))
                                          (second k)
                                          k)) opt-pairs)
              keys-map (into {} (concat
                                 (map (fn [[k v]] [k v]) req-pairs)
                                 (map (fn [[k v]] [(if (and (list? k) 
                                                            (or (= 'optional-key (first k))
                                                                (= 's/optional-key (first k))))
                                                     (second k)
                                                     k) v]) opt-pairs)))]
          {:type :map 
           :keys keys-map
           :req-keys req-keys
           :opt-keys opt-keys})

        ;; Basic Schema types
        (= 's/Str value) :string
        (= 's/Int value) :int  
        (= 's/Num value) :number
        (= 's/Bool value) :boolean
        (= 's/Keyword value) :keyword
        (= 's/Symbol value) :symbol
        (= 's/Any value) :any
        (= 's/Inst value) :any  ; java.util.Date, etc.
        (= 's/Uuid value) :any  ; java.util.UUID
        (= 's/Regex value) :regex
        
        ;; Fully qualified schema.core types
        (= 'schema.core/Str value) :string
        (= 'schema.core/Int value) :int
        (= 'schema.core/Num value) :number
        (= 'schema.core/Bool value) :boolean
        (= 'schema.core/Keyword value) :keyword
        (= 'schema.core/Symbol value) :symbol
        (= 'schema.core/Any value) :any
        (= 'schema.core/Inst value) :any
        (= 'schema.core/Uuid value) :any
        (= 'schema.core/Regex value) :regex
        
        ;; Optional/Maybe types
        (and (list? value) (= 'maybe (first value)))
        (let [inner-schema (second value)]
          (if (keyword? inner-schema)
            (keyword (str "nilable/" (name inner-schema)))
            (let [inner-type (if (symbol? inner-schema)
                               (extract-schema-type {:value inner-schema})
                               (extract-schema-type inner-schema))]
              (if (keyword? inner-type)
                (keyword (str "nilable/" (name inner-type)))
                inner-type))))
        
        (and (list? value) (= 's/maybe (first value)))
        (let [inner-schema (second value)]
          (if (keyword? inner-schema)
            (keyword (str "nilable/" (name inner-schema)))
            (let [inner-type (if (symbol? inner-schema)
                               (extract-schema-type {:value inner-schema})
                               (extract-schema-type inner-schema))]
              (if (keyword? inner-type)
                (keyword (str "nilable/" (name inner-type)))
                inner-type))))
        
        ;; Constrained types (s/constrained s/Int pos?)
        (and (list? value) (= 'constrained (first value)))
        (let [base-schema (second value)]
          (if (symbol? base-schema)
            (extract-schema-type {:value base-schema})
            (extract-schema-type base-schema)))  ; Just use the base type
        
        (and (list? value) (= 's/constrained (first value)))
        (let [base-schema (second value)]
          (if (symbol? base-schema)
            (extract-schema-type {:value base-schema})
            (extract-schema-type base-schema)))
        
        ;; Predicate types (s/pred pos?)
        (and (list? value) (= 'pred (first value)))
        :any  ; Can't infer specific type from predicate alone
        
        (and (list? value) (= 's/pred (first value)))
        :any
        
        ;; Either/Union types (s/either s/Int s/Str)
        (and (list? value) (= 'either (first value)))
        (let [type-schemas (rest value)
              types (map #(if (symbol? %)
                            (extract-schema-type {:value %})
                            (extract-schema-type %)) type-schemas)
              non-nil-types (remove #(= :any %) types)]
          (if (= 1 (count non-nil-types))
            (first non-nil-types)
            :any))  ; Multiple distinct types -> :any
        
        (and (list? value) (= 's/either (first value)))
        (let [type-schemas (rest value)
              types (map #(if (symbol? %)
                            (extract-schema-type {:value %})
                            (extract-schema-type %)) type-schemas)
              non-nil-types (remove #(= :any %) types)]
          (if (= 1 (count non-nil-types))
            (first non-nil-types)
            :any))
        
        ;; Conditional types (s/conditional pred? type1 type2)
        (and (list? value) (= 'conditional (first value)))
        :any  ; Too complex for static analysis
        
        (and (list? value) (= 's/conditional (first value)))
        :any
        
        ;; Enum types (s/enum :a :b :c)
        (and (list? value) (= 'enum (first value)))
        (let [enum-values (rest value)]
          (cond
            (every? keyword? enum-values) :keyword
            (every? string? enum-values) :string
            (every? number? enum-values) :number
            :else :any))
        
        (and (list? value) (= 's/enum (first value)))
        (let [enum-values (rest value)]
          (cond
            (every? keyword? enum-values) :keyword
            (every? string? enum-values) :string
            (every? number? enum-values) :number
            :else :any))
        
        ;; Collection types with element schemas
        (vector? value)
        (if (seq value)
          (let [element-schema (first value)
                element-type (if (symbol? element-schema)
                               (extract-schema-type {:value element-schema})
                               (extract-schema-type element-schema))]
            {:type :vector :element-type element-type})
          :vector)
        
        (set? value)
        (if (seq value)
          (let [element-schema (first value)
                element-type (if (symbol? element-schema)
                               (extract-schema-type {:value element-schema})
                               (extract-schema-type element-schema))]
            {:type :set :element-type element-type})
          :set)
        
        ;; Map schemas with key/value types
        (map? value)
        (let [key-value-pairs (seq value)
              process-entry (fn [[k v]]
                              [k (extract-schema-type v)])
              processed-pairs (map process-entry key-value-pairs)
              ;; Check for optional keys (list forms like (s/optional-key :nickname))
              separate-keys (fn [[k _]]
                              (and (list? k) 
                                   (or (= 'optional-key (first k))
                                       (= 's/optional-key (first k)))))
              req-pairs (remove separate-keys processed-pairs)
              opt-pairs (filter separate-keys processed-pairs)
              req-keys (map first req-pairs)
              opt-keys (map (fn [[k _]] (if (list? k) (second k) k)) opt-pairs)
              keys-map (into {} (concat
                                 req-pairs
                                 (map (fn [[k v]] [(if (list? k) (second k) k) v]) opt-pairs)))]
          (if (seq keys-map)
            {:type :map 
             :keys keys-map
             :req-keys req-keys
             :opt-keys opt-keys}
            :map))
        
        ;; Function schemas (=> [args...] ret)
        (and (list? value) (= '=> (first value)))
        (let [[_ args ret] value
              arg-types (mapv #(if (symbol? %)
                                 (extract-schema-type {:value %})
                                 (extract-schema-type %)) args)
              ret-type (if (symbol? ret)
                         (extract-schema-type {:value ret})
                         (extract-schema-type ret))]
          {:arities {(count args) {:args arg-types
                                   :ret ret-type}}})
        
        ;; Recursive/Named schemas (just treat as :any for now)
        (and (list? value) (= 'recursive (first value)))
        :any
        
        (and (list? value) (= 's/recursive (first value)))
        :any
        
        ;; Protocol schemas
        (and (list? value) (= 'protocol (first value)))
        :any
        
        (and (list? value) (= 's/protocol (first value)))
        :any
        
        ;; Literal values
        (string? value) :string
        (integer? value) :int
        (boolean? value) :boolean
        (keyword? value) :keyword
        
        ;; Unknown symbols - could be custom schemas
        (symbol? value)
        (let [schema-name (symbol value)]
          (or (get @*schema-registry* schema-name)
              :any))  ; fallback if not found
        
        :else :any)))))

(defn store-schema-definition!
  "Store a schema definition in the registry for later lookup"
  [_ctx var-name schema-node]
  (when (and var-name schema-node)
    (let [schema-type (extract-schema-type schema-node)]
      (swap! *schema-registry* assoc var-name schema-type)
      schema-type)))

(defn lookup-schema-definition
  "Look up a schema definition from the registry"
  [var-name]
  (get @*schema-registry* var-name))

(defn schema-type->type-spec
  "Convert a schema type to clj-kondo type spec format"
  [schema-type]
  (if (map? schema-type)
    schema-type
    schema-type))

(defn schema-type-to-string
  "Convert a schema type to a human-readable string"
  [schema-type]
  (cond
    ;; Handle sets (union types) - IMPROVED
    (set? schema-type)
    (let [normalize-item (fn [item]
                          (cond
                            ;; Handle AST objects with :tag
                            (and (map? item) (:tag item)) (:tag item)
                            ;; Handle other types
                            :else item))
          normalized-types (map normalize-item schema-type)
          unique-types (distinct normalized-types)  ; Remove duplicates
          type-strings (map schema-type-to-string (sort unique-types))
          filtered-strings (remove #(= % "nil") type-strings)
          has-nil? (some #(= % "nil") type-strings)]
      (cond
        (and (= 1 (count filtered-strings)) has-nil?)
        (str (first filtered-strings) " or nil")
        
        (and (> (count filtered-strings) 1) has-nil?)
        (str (str/join " or " filtered-strings) " or nil")
        
        (> (count type-strings) 1)
        (str/join " or " type-strings)
        
        :else
        (first type-strings)))
    
    ;; Handle AST objects with :tag field that contains a set (union type)
    (and (map? schema-type) (:tag schema-type) (set? (:tag schema-type)))
    (schema-type-to-string (:tag schema-type))
    
    ;; Handle AST objects with :tag field
    (and (map? schema-type) (:tag schema-type))
    (case (:tag schema-type)
      :string "string"
      :int "integer"
      :number "number"  
      :boolean "boolean"
      :keyword "keyword"
      :symbol "symbol"
      :vector "vector"
      :map "map"
      :set "set"
      :nil "nil"
      :any "any"
      :regex "regex"
      (str (:tag schema-type)))
    
    ;; Handle call objects  
    (and (map? schema-type) (:call schema-type))
    (let [call-info (:call schema-type)]
      (case (:type call-info)
        :call (str "function call (" (:name call-info) ")")
        (str "expression")))
    
    ;; Handle type objects with :type field
    (and (map? schema-type) (:type schema-type))
    (case (:type schema-type)
      :vector (if (:element-type schema-type)
                (str "vector of " (schema-type-to-string (:element-type schema-type)))
                "vector")
      :set (if (:element-type schema-type)
             (str "set of " (schema-type-to-string (:element-type schema-type)))
             "set")
      :map (if (and (:keys schema-type) (seq (:keys schema-type)))
             (let [key-strs (map (fn [[k v]] (str (name k) ": " (schema-type-to-string v))) 
                                (take 3 (:keys schema-type)))
                   more? (> (count (:keys schema-type)) 3)]
               (str "{" (str/join ", " key-strs) (when more? ", ...") "}"))
             "map")
      "complex type")
    
    (keyword? schema-type)
    (case schema-type
      :string "string"
      :int "integer" 
      :number "number"
      :boolean "boolean"
      :keyword "keyword"
      :symbol "symbol"
      :vector "vector"
      :map "map"
      :set "set"
      :nil "nil"
      :any "any"
      :regex "regex"
      (name schema-type))
    
    (map? schema-type)
    "complex type"
    
    :else (str schema-type)))

(defn schema-type-compatible?
  "Enhanced type compatibility checking"
  [expected actual]
  (let [normalize-type (fn [t]
                        (cond
                          ;; Handle AST objects with :tag
                          (and (map? t) (:tag t)) (:tag t)
                          ;; Handle structured types
                          (and (map? t) (:type t)) t  ; Return full map for structured types
                          ;; Already normalized
                          :else t))
        norm-expected (normalize-type expected)
        norm-actual (normalize-type actual)]
    
    (cond
      ;; Exact match after normalization
      (= norm-expected norm-actual) true
      
      ;; :any is always compatible
      (or (= :any norm-expected) (= :any norm-actual)) true
      
      ;; Handle sets (union types) in actual - NEW
      (set? norm-actual)
      (every? #(schema-type-compatible? norm-expected %) norm-actual)
      
      ;; Handle sets (union types) in expected - NEW  
      (set? norm-expected)
      (some #(schema-type-compatible? % norm-actual) norm-expected)
      
      ;; Number hierarchy: number is compatible with int expectation
      (and (= :int norm-expected) (= :number norm-actual)) true
      
      ;; Handle pos-int compatibility with int
      (and (= :int norm-expected) (= :pos-int norm-actual)) true
      (and (= :pos-int norm-expected) (= :int norm-actual)) true
      
      ;; Nilable types compatibility - fixed to handle namespaced keywords
      (and (keyword? expected) 
           (str/includes? (str expected) "nilable/"))
      (let [parts (str/split (str expected) #"/")
            base-type (keyword (last parts))]
        (or (= :nil norm-actual)
            (schema-type-compatible? base-type norm-actual)))
      
      ;; Map compatibility - improved to handle edge cases
      (and (map? norm-expected) (map? norm-actual)
           (= :map (:type norm-expected)) (= :map (:type norm-actual)))
      (let [normalize-map (fn [map-type]
                              (cond
                                ;; Schema format: {:type :map, :keys {...}, :req-keys [...]}
                                (and (map? map-type) (:keys map-type))
                                {:normalized-keys (:keys map-type)
                                 :req-keys (set (:req-keys map-type))}
                                
                                ;; Inferred format: {:type :map, :val {...}}
                                (and (map? map-type) (:val map-type))
                                (let [val-map (:val map-type)
                                      extracted-keys (into {} (map (fn [[k v]]
                                                                      [k (if (map? v) (:tag v) v)])
                                                                val-map))]
                                  {:normalized-keys extracted-keys
                                   :req-keys (set (keys extracted-keys))})
                                
                                :else map-type))
            norm-expected-map (normalize-map norm-expected)
            norm-actual-map (normalize-map norm-actual)
            expected-keys (:req-keys norm-expected-map)
            actual-keys (:req-keys norm-actual-map)]
        (cond
          ;; Empty maps are compatible with any map schema
          (or (empty? expected-keys) (empty? actual-keys)) true
          
          ;; All required keys in expected must be present in actual
          ;; BUT allow extra keys in actual (schemas are typically more permissive)
          :else
          (and
           ;; For now, be more permissive with map compatibility
           ;; This allows empty maps, extra keys, etc.
           (or (set/subset? expected-keys actual-keys)
               ;; If we can't determine key compatibility, allow it
               (empty? expected-keys)
               (empty? actual-keys))
           ;; Check type compatibility for overlapping keys when possible
           (every? (fn [k]
                    (let [expected-type (get-in norm-expected-map [:normalized-keys k])
                          actual-type (get-in norm-actual-map [:normalized-keys k])]
                      (if (and expected-type actual-type)
                        (schema-type-compatible? expected-type actual-type)
                        true))) ; Be permissive if types aren't available
                  (set/intersection expected-keys actual-keys)))))
      
      ;; Handle generic map compatibility - be more permissive
      (and (map? norm-expected) (= :map norm-actual)) true
      (and (= :map norm-expected) (map? norm-actual)) true
      
      ;; Handle complex map schema vs simple :map - be permissive  
      (and (map? norm-expected) (= :map (:type norm-expected)) (= :map norm-actual)) 
      true
      
      ;; Collection types with element types
      (and (map? norm-expected) (map? norm-actual)
           (= (:type norm-expected) (:type norm-actual))
           (:element-type norm-expected))
      (schema-type-compatible? (:element-type norm-expected)
                             (:element-type norm-actual))
      
      ;; Collection types compatibility - if types match, they're compatible
      ;; This handles cases like [] (generic :vector) being compatible with [s/Int] (typed vector)
      ;; But we should be more careful about when to allow this
      (and (map? norm-expected) (keyword? norm-actual)
           (= (:type norm-expected) norm-actual))
      ;; For now, be permissive with generic collection types vs typed schemas
      ;; This is a limitation of the type inference system
      ;; TODO: Enhance type inference to extract element types from literals
      true
      
      ;; Collection types with element types (when both have element type info)
      (and (map? norm-expected) (map? norm-actual)
           (:element-type norm-expected) (:element-type norm-actual)
           (= (:type norm-expected) (:type norm-actual)))
      (schema-type-compatible? (:element-type norm-expected)
                             (:element-type norm-actual))
      
      (and (keyword? norm-expected) (map? norm-actual))
      (= norm-expected (:type norm-actual))
      
      ;; Default
      :else false)))

(defn emit-schema-type-mismatch!
  "Emit a schema type mismatch warning"
  [ctx expected-schema actual-type expr]
  (when (and expected-schema actual-type 
             (not (schema-type-compatible? expected-schema actual-type)))
    (let [expected-label (cond
                           (keyword? expected-schema)
                           (cond
                             (str/starts-with? (str expected-schema) ":nilable/")
                             (str (subs (str expected-schema) 9) " or nil")
                             
                             :else
                             (schema-type-to-string expected-schema))
                           
                           (map? expected-schema)
                           (schema-type-to-string expected-schema)
                           
                           :else (str expected-schema))
          
          actual-label (schema-type-to-string actual-type)]
      (when expr
        (require 'clj-kondo.impl.findings)
        ((resolve 'clj-kondo.impl.findings/reg-finding!) ctx
         (utils/node->line (:filename ctx)
                           expr :schema-type-mismatch
                           (str "Schema type mismatch. Expected: " expected-label
                                ", actual: " actual-label ".")))))))

(defn convert-schema-to-type-spec
  "Convert extracted schema type information to clj-kondo type spec format"
  [schema-types]
  (when (seq schema-types)
    (let [ret (first schema-types)    ; Return type is first
          args (rest schema-types)]   ; Parameters follow
      {:arities {(count args) {:args (vec args)
                               :ret ret}}})))
