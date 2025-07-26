(ns clj-kondo.impl.schema-types
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :as utils]))

;;;; Schema Type System Integration
;;;; Converts Prismatic Schema types to clj-kondo's type system

(defn extract-schema-type
  "Extract a clj-kondo type representation from a schema AST node"
  [schema-node]
  (when schema-node
    (let [value (:value schema-node)]
      (cond
        (= 's/Str value) :string
        (= 's/Int value) :int  
        (= 's/Num value) :number
        (= 's/Bool value) :boolean
        (= 's/Keyword value) :keyword
        (= 's/Symbol value) :symbol
        (= 's/Any value) :any
        
        (= 'schema.core/Str value) :string
        (= 'schema.core/Int value) :int
        (= 'schema.core/Num value) :number
        (= 'schema.core/Bool value) :boolean
        (= 'schema.core/Keyword value) :keyword
        (= 'schema.core/Symbol value) :symbol
        (= 'schema.core/Any value) :any
        (and (list? value) (= 'maybe (first value)))
        (extract-schema-type (second value))
        
        (vector? value)
        :vector
        
        (set? value)
        :set
        
        (map? value)
        :map
        
        (and (list? value) (= '=> (first value)))
        (let [[_ args ret] value]
          {:arities {(count args) {:args (mapv extract-schema-type args)
                                   :ret (extract-schema-type ret)}}})
           
        (and (list? value) (= 'enum (first value)))
        :any
        
        (symbol? value)
        :any
        
        (string? value) :string
        (integer? value) :int
        (boolean? value) :boolean
        (keyword? value) :keyword
        
        :else :any))))

(defn schema-type->type-spec
  "Convert a schema type to clj-kondo type spec format"
  [schema-type]
  (if (map? schema-type)
    schema-type
    schema-type))

(defn convert-schema-to-type-spec
  "Convert extracted schema type information to clj-kondo type spec format"
  [schema-types]
  (when (seq schema-types)
    (let [args (butlast schema-types)
          ret (last schema-types)]
      {:arities {(count args) {:args (vec args)
                               :ret ret}}})))
