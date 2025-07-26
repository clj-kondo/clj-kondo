(ns clj-kondo.impl.schema-types
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :as utils]
   [clojure.string :as str]))

;;;; Schema Type System Integration
;;;; Converts Prismatic Schema types to clj-kondo's type system

(defn extract-schema-type
  "Extract a clj-kondo type representation from a schema AST node"
  [schema-node]
  (when schema-node
    (let [value (if (and (contains? schema-node :children)
                         (seq (:children schema-node)))
                  ;; For SeqNodes, convert to sexpr to get the actual form
                  (try 
                    (utils/sexpr schema-node)
                    (catch Exception _
                      (:value schema-node)))
                  (:value schema-node))]
      
      (cond
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
        (let [req-keys (keys (filter (fn [[k _]] (not (keyword? k))) value))
              opt-keys (keys (filter (fn [[k _]] (keyword? k)) value))]
          (if (and (empty? req-keys) (empty? opt-keys))
            :map
            {:type :map 
             :keys (merge req-keys opt-keys)
             :req-keys req-keys
             :opt-keys opt-keys}))
        
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
        (symbol? value) :symbol
        
        ;; Unknown symbols - could be custom schemas
        (symbol? value)
        :any
        
        :else :any))))

(defn schema-type->type-spec
  "Convert a schema type to clj-kondo type spec format"
  [schema-type]
  (if (map? schema-type)
    schema-type
    schema-type))

(defn emit-schema-type-mismatch!
  "Emit a schema type mismatch warning"
  [ctx expected-schema actual-type expr]
  (let [expected-label (cond
                         (keyword? expected-schema)
                         (cond
                           (str/starts-with? (str expected-schema) ":nilable/")
                           (str (subs (str expected-schema) 9) " or nil")
                           
                           :else
                           (case expected-schema
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
                             (name expected-schema)))
                         
                         (map? expected-schema)
                         (case (:type expected-schema)
                           :vector (if (:element-type expected-schema)
                                     (str "vector of " (name (:element-type expected-schema)))
                                     "vector")
                           :set (if (:element-type expected-schema)
                                  (str "set of " (name (:element-type expected-schema)))
                                  "set")
                           :map "structured map"
                           "complex type")
                         
                         :else (str expected-schema))
        
        actual-label (cond
                       (keyword? actual-type)
                       (case actual-type
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
                         (name actual-type))
                       
                       (map? actual-type)
                       (case (:type actual-type)
                         :vector "vector"
                         :set "set"
                         :map "map"
                         "complex type")
                       
                       :else (str actual-type))]
    (when expr
      (require 'clj-kondo.impl.findings)
      ((resolve 'clj-kondo.impl.findings/reg-finding!) ctx
       {:filename (:filename ctx)
        :row (:row expr)
        :col (:col expr)
        :end-row (:end-row expr)
        :end-col (:end-col expr)
        :type :schema-type-mismatch
        :message (str "Schema type mismatch. Expected: " expected-label
                      ", actual: " actual-label ".")}))))

(defn schema-type-compatible?
  "Check if two schema types are compatible"
  [expected actual]
  (cond
    ;; Exact match
    (= expected actual) true
    
    ;; :any is always compatible
    (or (= :any expected) (= :any actual)) true
    
    ;; Number hierarchy: :int is compatible with :number
    (and (= :number expected) (= :int actual)) true
    
    ;; Nilable types compatibility
    (and (keyword? expected) (str/starts-with? (str expected) ":nilable/"))
    (let [base-type (keyword (subs (str expected) 9))]
      (or (= base-type actual) (= :nil actual)))
    
    ;; Handle complex types (maps with structure)
    (and (map? expected) (map? actual))
    (cond
      ;; Both are structured maps
      (and (= :map (:type expected)) (= :map (:type actual))) true
      ;; One structured, one simple
      (or (= :map (:type expected)) (= :map (:type actual))) true
      ;; Default for other map types
      :else true)
    
    ;; Collection types with element types
    (and (map? expected) (map? actual)
         (= (:type expected) (:type actual)))
    true  ; For now, just check container type
    
    ;; Simple collection compatibility
    (and (map? expected) (keyword? actual))
    (= (:type expected) actual)
    
    (and (keyword? expected) (map? actual))
    (= expected (:type actual))
    
    ;; Default
    :else false))

(defn convert-schema-to-type-spec
  "Convert extracted schema type information to clj-kondo type spec format"
  [schema-types]
  (when (seq schema-types)
    (let [args (butlast schema-types)
          ret (last schema-types)]
      {:arities {(count args) {:args (vec args)
                               :ret ret}}})))
