(ns clj-kondo.impl.linters.schema-types
  {:no-doc true}
  (:require
   [clj-kondo.impl.schema-types :as schema-types]
   [clj-kondo.impl.types :as types]
   [clojure.string :as str]))

(defn get-function-schema
  "Retrieve stored schema information for a function"
  [ctx ns-name fn-name]
  (when-let [schema-functions (-> @(:findings ctx) :schema-functions)]
    (get-in @schema-functions
            [(:base-lang ctx) (:lang ctx) ns-name fn-name])))

(defn infer-argument-type
  "Infer type from an argument expression, resolving delayed calls"
  [ctx expr]
  (when expr
    (let [tag (types/expr->tag ctx expr)]
      ;; If we get a delayed call structure, try to resolve it
      (if (and (map? tag) (:call tag))
        (if-let [idacs (:idacs ctx)]
          ;; We have idacs available, resolve the delayed call
          (do
            (require 'clj-kondo.impl.types.utils)
            (let [resolve-fn (resolve 'clj-kondo.impl.types.utils/resolve-arg-type)
                  resolved-tag (resolve-fn idacs tag)]
              ;; Extract the actual type from the resolved result
              (cond
                (keyword? resolved-tag) resolved-tag
                (and (map? resolved-tag) (:tag resolved-tag)) (:tag resolved-tag)
                (and (map? resolved-tag) (:type resolved-tag)) resolved-tag
                :else :any)))
          ;; No idacs available, try to infer from call structure 
          ;; This is a fallback to avoid false positives
          (let [call-info (:call tag)
                fn-name (:name call-info)]
            ;; Try to infer from function name patterns
            (cond
              (and fn-name (str/ends-with? (str fn-name) "?")) :boolean
              :else :any)))
        ;; Regular type processing
        (cond
          (= tag :string) :string
          (= tag :int) :int
          (= tag :number) :number
          (= tag :boolean) :boolean
          (= tag :keyword) :keyword
          (= tag :symbol) :symbol
          (= tag :vector) :vector
          (= tag :map) :map
          (= tag :set) :set
          (= tag :nil) :nil
          ;; Fallback
          :else nil)))))

(defn lint-schema-function-call!
  "Enhanced schema function call validation with type checking"
  [_ctx _call-expr _called-fn _arg-types]
  ;; TODO: Implement full argument type checking
  ;; For now, schema type checking happens in the analyzer
  nil)
