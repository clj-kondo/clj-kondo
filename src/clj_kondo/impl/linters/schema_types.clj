(ns clj-kondo.impl.linters.schema-types
  {:no-doc true}
  (:require
   [clj-kondo.impl.schema-types :as schema-types]
   [clj-kondo.impl.types :as types]))

(defn get-function-schema
  "Retrieve stored schema information for a function"
  [ctx ns-name fn-name]
  (when-let [schema-functions (-> @(:findings ctx) :schema-functions)]
    (get-in @schema-functions
            [(:base-lang ctx) (:lang ctx) ns-name fn-name])))

(defn infer-argument-type
  "Infer type from an argument expression"
  [ctx expr]
  (when expr
    (let [tag (types/expr->tag ctx expr)]
      (cond
        ;; Direct type mapping
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
        :else nil))))

(defn lint-schema-function-call!
  "Check function call arguments against schema types"
  [ctx resolved-ns resolved-name arg-exprs call-expr]
  (when-let [schema-info (get-function-schema ctx resolved-ns resolved-name)]
    (let [arg-schemas (:arg-schemas schema-info)
          arg-count (count arg-exprs)]
      (doseq [[i arg-expr] (map-indexed vector arg-exprs)
              :when (< i (count arg-schemas))
              :let [expected-schema-node (nth arg-schemas i)
                    expected-schema (schema-types/extract-schema-type expected-schema-node)
                    actual-type (infer-argument-type ctx arg-expr)]
              :when (and expected-schema actual-type)]
        (when-not (schema-types/schema-type-compatible? expected-schema actual-type)
          (schema-types/emit-schema-type-mismatch! ctx expected-schema actual-type arg-expr))))))
