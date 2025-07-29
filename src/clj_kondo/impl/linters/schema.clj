(ns clj-kondo.impl.linters.schema
  {:no-doc true}
  (:require
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.schema :as schema]
   [clj-kondo.impl.schema-types :as schema-types]
   [clj-kondo.impl.types.utils :as type-utils]
   [clj-kondo.impl.utils :as utils]))

(defn lint-schema-function-call!
  "Check if a function call matches its schema annotations"
  [ctx call called-fn arg-types]
  (when-let [schema-info (schema/get-function-schema 
                          ctx 
                          (:ns called-fn) 
                          (:name called-fn))]
    (let [{:keys [arg-schemas return-schema]} schema-info
          ;; CRITICAL FIX: Pass actual idacs from context instead of empty map
          idacs (or (:idacs ctx) {})
          actual-arg-types (map #(type-utils/resolve-arg-type idacs %) arg-types)]
      
      ;; Check argument types
      (doseq [[expected-schema actual-type idx] 
              (map vector arg-schemas actual-arg-types (range))]
        (when expected-schema
          (let [normalized-schema (schema-types/extract-schema-type expected-schema ctx)]
            (when-not (schema-types/schema-type-compatible? 
                       normalized-schema 
                       (:tag actual-type))
              (schema-types/emit-schema-type-mismatch!
               ctx
               normalized-schema
               (:tag actual-type)
               (nth (:args call) idx nil))))))
      
      (when return-schema
        (let [return-type (schema-types/extract-schema-type return-schema ctx)]
          return-type)))))

(defn lint-schema-var-definition!
  "Store schema information when analyzing s/defn, s/def, etc."
  [ctx ns-name var-name schemas]
  (schema/store-function-schema-types! ctx ns-name var-name schemas))
