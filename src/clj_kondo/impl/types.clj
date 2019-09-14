(ns clj-kondo.impl.types
  {:no-doc true}
  (:require
   [clj-kondo.impl.clojure.spec.alpha :as s]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :as utils :refer
    [tag sexpr]]))

(def labels
  {::nil "nil"
   ::string "string"
   ::number "number"
   ::any-number "number"
   ::int "integer"
   ::any-integer "integer"
   ::pos-int "positive integer"
   ::nat-int "natural integer"
   ::neg-int "negative integer"
   ::any-seqable "seqable collection"
   ::seqable "seqable collection"
   ::vector "vector"
   ::associative "associative collection"
   ::any-associative "associative collection"
   ::atom "atom"
   ::fn "function"
   ::ifn "function"
   ::keyword "keyword"
   ::seqable-or-transducer "seqable or transducer"})

(defmacro derive! [children parents]
  (let [children (if (keyword? children) [children] children)
        parents (if (keyword? parents) [parents] parents)]
    `(doseq [c# ~children
             p# ~parents]
       (derive c# p#))))

(derive! [::list ::vector ::string] ::seqable)
(derive! ::any-seqable [::list ::vector ::string])

(derive! [::vector ::map] ::associative)
(derive! ::any-associative [::vector ::map])

(derive! [::vector ::keyword ::symbol ::map ::transducer ::fn] ::ifn)
(derive! ::any-ifn [::vector ::keyword ::symbol ::map ::transducer ::fn])

(derive! [::double ::int ::pos-int ::neg-int ::nat-int] ::number)
(derive! ::any-number [::double ::int ::pos-int ::neg-int ::nat-int])

(derive! [::pos-int ::nat-int ::neg-int] ::int)
(derive ::pos-int ::nat-int)
(derive! ::any-int [::pos-int ::neg-int])

(derive! [::vector ::map ::set ::seqable] ::coll)
(derive! ::any-coll [::vector ::map ::set])
(derive ::coll ::conjable)

(defn is? [x parent]
  (or (identical? x ::any)
      (isa? x parent)))

(s/def ::nil #(is? % ::nil))
(s/def ::seqable #(is? % ::seqable))
(s/def ::associative #(is? % ::associative))
(s/def ::number #(is? % ::number))
(s/def ::nat-int #(is? % ::nat-int))
(s/def ::int #(is? % ::int))
(s/def ::atom #(is? % ::atom))
(s/def ::ifn #(is? % ::ifn))
(s/def ::transducer #(is? % ::transducer))
(s/def ::string #(is? % ::string))
(s/def ::conjable #(is? % ::conjable))
;; (s/def ::reducible-coll #(is? % ::reducible-coll))
;; (s/def ::seqable-or-transducer #(is? % ::seqable-or-transducer))
(s/def ::any any?)

(def specs
  {'clojure.core
   {;; 22
    'cons {:args (s/cat :x ::any :seq ::seqable)}
    ;; 181
    'assoc {:args (s/cat :map (s/alt :a ::associative :nil ::nil)
                         :key ::any :val ::any :kvs (s/* (s/cat :ks ::any :vs ::any)))}
    ;; 922
    'inc {:args (s/cat :x ::number)
          :ret ::any-number}
    ;; 947
    'reverse {:args (s/cat :x ::seqable)
              :ret ::any-seqable}
    ;; 2327
    'atom {:ret ::atom}
    ;; 2345
    'swap! {:args (s/cat :atom ::atom :f ::ifn :args (s/* ::any))}
    ;; 2576
    'juxt {:args (s/+ ::ifn)
           :ret ::ifn}
    ;; 2727
    'map {:args (s/alt :transducer (s/cat :f ::ifn)
                       :seqable (s/cat :f ::ifn :colls (s/+ ::seqable)))
          ;; :ret ::seqable-or-transducer
          :fn (fn [args]
                (if (= 1 (count args))
                  ::transducer
                  ::seqable))}
    ;; 2793
    'filter {:args (s/alt :transducer (s/cat :f ::ifn)
                          :seqable (s/cat :f ::ifn :coll ::seqable))
             ;; :ret ::seqable-or-transducer
             :fn (fn [args]
                   (if (= 1 (count args))
                     ::transducer
                     ::any-seqable))}
    ;; 2826
    'remove {:args (s/alt :transducer (s/cat :f ::ifn)
                          :seqable (s/cat :f ::ifn :coll ::seqable))
             ;; :ret ::seqable-or-transducer
             :fn (fn [args]
                   (if (= 1 (count args))
                     ::transducer
                     ::any-seqable))}
    ;; 4981
    'subs {:args (s/cat :s ::string
                        :start ::nat-int
                        :end (s/? ::nat-int))
           :ret ::string}
    ;; 6790
    'reduce {:args (s/cat :f ::ifn :val (s/? ::any) :coll ::seqable)}
    ;; 6887
    'into {:args (s/alt :no-arg (s/cat)
                        :identity (s/cat :to ::conjable)
                        :seqable (s/cat :to ::conjable :from ::seqable)
                        :transducer (s/cat :to ::conjable :xf ::transducer :from ::seqable))
           :ret ::any-seqable}
    ;; 6903
    'mapv {:args (s/alt :transducer (s/cat :f ::ifn)
                        :seqable (s/cat :f ::ifn :colls (s/+ ::seqable)))
           ;; :ret ::seqable-or-transducer
           :fn (fn [args]
                 (if (= 1 (count args))
                   ::transducer
                   ::vector))}
    ;; 7313
    'filterv {:args (s/alt :transducer (s/cat :f ::ifn)
                           :seqable (s/cat :f ::ifn :coll ::seqable))
              ;; :ret ::seqable-or-transducer
              :fn (fn [args]
                    (if (= 1 (count args))
                      ::transducer
                      ::vector))}
    ;; 7313
    'keep {:args (s/alt :transducer (s/cat :f ::ifn)
                        :seqable (s/cat :f ::ifn :coll ::seqable))
           ;; :ret ::seqable-or-transducer
           :fn (fn [args]
                 (if (= 1 (count args))
                   ::transducer
                   ::any-seqable))}}})

(defn number->tag [v]
  (cond (int? v)
        (cond (pos-int? v) ::pos-int
              (nat-int? v) ::nat-int
              (neg-int? v) ::neg-int)
        (double? v) ::double
        :else ::number))

(defn expr->tag [{:keys [:bindings :lang]} expr]
  (let [t (tag expr)
        edn? (= :edn lang)]
    ;; (prn t expr)
    (case t
      :map ::map
      :vector ::vector
      :list (if edn? ::list ::any) ;; a call we know nothing about
      :fn ::fn
      :token (let [v (sexpr expr)]
               (cond
                 (nil? v) ::nil
                 (symbol? v) (if edn? ::symbol
                                 (if-let [b (get bindings v)]
                                   (or (:tag b) ::any)
                                   ::any))
                 (string? v) ::string
                 (keyword? v) ::keyword
                 (number? v) (number->tag v)
                 :else ::any))
      ::any)))

(defn add-arg-type-from-expr [ctx expr]
  ;; (prn expr (expr->tag ctx expr) (meta expr))
  (when-let [arg-types (:arg-types ctx)]
    ;; (prn expr)
    (let [{:keys [:row :col]} (meta expr)]
      (swap! arg-types conj {:tag (expr->tag ctx expr)
                             :row row
                             :col col}))))

;; TODO: rename return-type
(defn spec-from-call [_ctx call _expr]
  (when-not (:unresolved? call)
    (let [call-ns (:resolved-ns call)
          call-name (:name call)]
      ;; (prn call-ns call-name)
      (when-let [spec (get-in specs [call-ns call-name])]
        (if-let [fn-spec (:fn spec)]
          (fn-spec @(:arg-types call))
          (let [r (:ret spec)]
            r #_(get return-types r r)))))))

(defn add-arg-type-from-call [ctx call _expr]
  (when-let [arg-types (:arg-types ctx)]
    (swap! arg-types conj {:tag (or (spec-from-call ctx call _expr) ::any)
                           :row (:row call)
                           :col (:col call)})))

(defn emit-warning! [{:keys [:findings] :as ctx} args problem]
  ;; (prn args problem)
  (let [via (first (:via problem))
        in-path (:in problem)
        offending-arg (get-in args in-path)
        offending-tag (:tag offending-arg)
        via-label (or (get labels via)
                      (when via
                        (name via)))
        offending-tag-label (or (get labels offending-tag)
                                (when offending-tag
                                  (name offending-tag)))
        reason (:reason problem)
        insufficient? (= "Insufficient input" reason)
        extra? (= "Extra input" reason)]
    (cond insufficient?
          (findings/reg-finding! findings {:filename (:filename ctx)
                                           :row (:row (last args))
                                           :col (:col (last args))
                                           :type :type-mismatch
                                           :message "More arguments expected."} )
          extra?
          (findings/reg-finding! findings {:filename (:filename ctx)
                                           :row (:row (last args))
                                           :col (:col (last args))
                                           :type :type-mismatch
                                           :message "Too many arguments."})
          (and via-label offending-tag-label)
          (findings/reg-finding! findings {:filename (:filename ctx)
                                           :row (:row offending-arg)
                                           :col (:col offending-arg)
                                           :type :type-mismatch
                                           :message (str "Expected: " via-label
                                                         ", received: " offending-tag-label ".")}))))

;; (require '[clojure.pprint :refer [pprint]])

(defn lint-arg-types [ctx called-ns called-name args]
  ;; (prn "ARG" args (meta args) called-ns called-name)
  (let [tags (not-empty (map :tag args))]
    (when tags
      (when-let [args-spec (:args (get-in specs [called-ns called-name]))]
        ;; (prn "SPEC" args-spec called-ns called-name)
        ;; (prn (s/valid? args-spec tags))
        ;; (pprint (s/conform args-spec tags))
        (when-not (s/valid? args-spec tags)
          (let [d (s/explain-data args-spec tags)]
            ;; (prn (count (:clj-kondo.impl.clojure.spec.alpha/problems d)))
            (run! #(emit-warning! ctx args %)
                  (take 1 (:clj-kondo.impl.clojure.spec.alpha/problems d)))))))))
