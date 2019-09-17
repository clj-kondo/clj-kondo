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
   ::int "integer"
   ::pos-int "positive integer"
   ::nat-int "natural integer"
   ::neg-int "negative integer"
   ::seqable-out "seqable collection"
   ::seqable "seqable collection"
   ::vector "vector"
   ::associative "associative collection"
   ::atom "atom"
   ::fn "function"
   ::ifn "function"
   ::keyword "keyword"
   ::seqable-or-transducer "seqable or transducer"
   ::set "set"
   ::nilable-set "set or nil"
   ::nilable-int "integer or nil"
   ::char-sequence "char sequence"
   ::nilable-string "string or nil"})

(defmacro derive! [children parents]
  (let [children (if (keyword? children) [children] children)
        parents (if (keyword? parents) [parents] parents)]
    `(doseq [c# ~children
             p# ~parents]
       (derive c# p#))))

(def current-ns-name (str (ns-name *ns*)))

(defmacro reg-type! [k]
  (let [;; any-k (keyword current-ns-name (str "any-" (name k)))
        nilable (keyword current-ns-name (str "nilable-" (name k)))
        ;; any-nilable (keyword current-ns-name (str "any-nilable-" (name k)))
        ]
    `(do (derive ~k ~nilable)
         (derive ::nil ~nilable)
         ;; (prn "derive" ~any-nilable ~k)
         ;; (derive ~any-k ~k)
         ;; (derive ~any-nilable ~any-k)
         ;; (derive ~any-nilable ::nil)
         (s/def ~k #(is? % ~k))
         ;; (s/def ~any-k #(is? % ~any-k))
         (s/def ~nilable #(is? % ~nilable))
         ;; (s/def ~any-nilable #(is? % ~any-nilable)
         )))

(defmacro derive2! [children parent]
(let [children (if (keyword? children) [children] children)
      #_#_any-parent (keyword current-ns-name (str "any-" (name parent)))]
  `(do (doseq [c# ~children]
         (derive c# ~parent))
       #_(doseq [c# ~children]
           ;; (prn "derive" ~any-parent c#)
           (derive ~any-parent c#)))))

(defn is? [x parent]
;; (prn x parent (isa? x parent) (isa? parent x))
(or
 (identical? x ::any)
 (isa? x parent)
 (isa? parent x) ;; parent COULD be an a x, but we can't prove it just by
 ;; looking at the code!
 ))

(comment

)

(derive2! [::vector ::list ::map ::set] ::coll)
;; (derive! ::any-coll [::vector ::list ::map ::set])

(reg-type! ::set)
;; (derive! ::any-nilable-set [::nil ::set])

(derive2! [::string ::char ::regex] ::char-sequence)

(derive ::coll ::conjable)
;; (derive ::any-conjable ::coll)

(reg-type! ::string)

(derive2! [::nil ::string ::coll] ::seqable)
;; (derive! ::any-seqable [::any-coll ::string ::nil])

(derive2! [::list ::lazy-seq] ::seq)
;; (derive! ::any-seq [::list ::lazy-seq])

;; It seems very unlikely that a sequence function produces a vector, set or
;; map. in any case, you should probably not rely on it. You should also not
;; rely on it giving nil or an empty seq, so nil is left out on purpose.
(derive! ::seqable-out [::list ::vector ::lazy-seq])

(derive2! [::vector ::map] ::associative)
;; (derive! ::any-associative [::vector ::map])
;; (derive ::any-seqable ::any-associative)

(derive2! [::vector ::keyword ::symbol ::map ::set ::transducer ::fn] ::ifn)
;; (derive! ::any-ifn [::vector ::keyword ::symbol ::map ::set ::transducer ::fn])

(reg-type! ::int)
;; (derive! [::nil ::int] ::nilable-int)
(derive2! [::double ::int ::pos-int ::neg-int ::nat-int] ::number)
;; (derive! ::any-number [::double ::int ::pos-int ::neg-int ::nat-int])

(derive2! [::pos-int ::nat-int ::neg-int] ::int)
(derive ::pos-int ::nat-int)
;; (derive! ::any-int [::pos-int ::neg-int])

(reg-type! ::boolean)
;; (derive! [::nil ::boolean] ::nilable-boolean)
;; (derive! ::any-nilable-boolean [::nil ::boolean])


;; (reg-type! ::int)

(comment
(is? ::nil ::nilable-set)
(is? ::nil ::nilable-int)
(is? ::int ::nilable-int)
(is? ::transducer ::ifn)
(s/valid? (s/cat :f ::ifn) [::transducer])
)

(s/def ::nil #(is? % ::nil))
(s/def ::boolean #(is? % ::boolean))
(s/def ::seqable #(is? % ::seqable)) ;; since nil is part of seqable, we have to define it manually
;; (reg-type! ::seqable)
;; (s/def ::associative #(is? % ::associative))
(reg-type! ::associative)
;; (s/def ::number #(is? % ::number))
(reg-type! ::number)
;; (s/def ::nat-int #(is? % ::nat-int))
(reg-type! ::int)
(reg-type! ::nat-int)
;; (s/def ::int #(is? % ::int))
(s/def ::atom #(is? % ::atom))
;; (s/def ::ifn #(is? % ::ifn))
(reg-type! ::ifn)
(s/def ::transducer #(is? % ::transducer))
;; (s/def ::char-sequence #(is? % ::char-sequence))
(reg-type! ::char-sequence)
;; (s/def ::string #(is? % ::string))
(reg-type! ::string)
;; (s/def ::char #(is? % ::char))
(reg-type! ::char)
;; (s/def ::conjable #(is? % ::conjable))
(reg-type! ::conjable)
;; (s/def ::set #(is? % ::set))
(reg-type! ::set)
;; (s/def ::nilable-set #(is? % ::nilable-set))
;; (s/def ::nilable-int #(is? % ::nilable-int))
;; (s/def ::reducible-coll #(is? % ::reducible-coll))
;; (s/def ::seqable-or-transducer #(is? % ::seqable-or-transducer))
(s/def ::any any?)

(reg-type! ::byte)
(reg-type! ::boolean)
(reg-type! ::double)

;; (s/def ::any-nilable-string* ::any-nilable-string)
;; (s/def ::any-nilable-int* ::any-nilable-int)

(defn tag-from-meta
([meta-tag] (tag-from-meta meta-tag false))
([meta-tag out?]
 (case meta-tag
   void ::nil
   (boolean) ::boolean
   (Boolean java.lang.Boolean) ::nilable-boolean
   (byte Byte java.lang.Byte) ::nilable-byte
   (Number java.lang.Number) ::nilable-number ;; as this is now way to
   ;; express non-nilable,
   ;; we'll go for the most
   ;; relaxed type
   (int long Long java.lang.Long) ::nilable-int #_(if out? ::any-nilable-int ::any-nilable-int) ;; or ::any-nilable-int? , see 2451 main-test
   (float double Float Double java.lang.Float java.lang.Double) ::nilable-double
   (CharSequence java.lang.CharSequence) ::nilable-char-sequence
   (String java.lang.String) ::nilable-string ;; as this is now way to
   ;; express non-nilable,
   ;; we'll go for the most
   ;; relaxed type
   (char Character java.lang.Character) ::nilable-char
   (Seqable clojure.lang.Seqable) (if out? ::seqable-out ::seqable)
   (do #_(prn "did not catch tag:" meta-tag) nil nil))))

(def clojure-core
{;; 22
 'cons {:args (s/cat :x ::any :seq ::seqable)}
 ;; 181
 'assoc {:args (s/cat :map (s/alt :a ::associative :nil ::nil)
                      :key ::any :val ::any :kvs (s/* (s/cat :ks ::any :vs ::any)))}
 ;; 544
 'str {:ret ::string}
 ;; 922
 'inc {:args (s/cat :x ::number)
       :ret ::number}
 ;; 947
 'reverse {:args (s/cat :x ::seqable)
           :ret ::seqable-out}
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
               ::seqable-out))}
 ;; 2793
 'filter {:args (s/alt :transducer (s/cat :f ::ifn)
                       :seqable (s/cat :f ::ifn :coll ::seqable))
          ;; :ret ::seqable-or-transducer
          :fn (fn [args]
                (if (= 1 (count args))
                  ::transducer
                  ::seqable-out))}
 ;; 2826
 'remove {:args (s/alt :transducer (s/cat :f ::ifn)
                       :seqable (s/cat :f ::ifn :coll ::seqable))
          ;; :ret ::seqable-or-transducer
          :fn (fn [args]
                (if (= 1 (count args))
                  ::transducer
                  ::seqable-out))}
 ;; 4105
 'set {:ret ::set}
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
        :fn (fn [args]
              (let [t (:tag (first args))]
                (if (identical? ::any t)
                  ::coll
                  t)))}
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
                ::seqable-out))}})

(def specs
  {'clojure.core clojure-core
   'cljs.core clojure-core
   'clojure.set
   {'union
    {:args (s/* ::nilable-set)
     :ret ::nilable-set}
    'intersection
    {:args (s/+ ::nilable-set)
     :ret ::nilable-set}
    'difference
    {:args (s/+ ::nilable-set)
     :ret ::nilable-set}}
   'clojure.string
   {'join
    {:args (s/cat :separator (s/? ::any)
                  :coll ::seqable)
     :ret ::string}
    'starts-with?
    {:args (s/cat :cs ::char-sequence
                  :substr ::string)
     :ret ::string}
    'ends-with?
    {:args (s/cat :cs ::char-sequence
                  :substr ::string)
     :ret ::string}
    'includes?
    {:args (s/cat :cs ::char-sequence
                  :s ::char-sequence)
     :ret ::string}}})

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
  ;; (prn expr "=>" (expr->tag ctx expr) (meta expr))
  (when-let [arg-types (:arg-types ctx)]
    (let [{:keys [:row :col]} (meta expr)]
      (swap! arg-types conj {:tag (expr->tag ctx expr)
                             :row row
                             :col col}))))

;; TODO: rename return-type
(defn spec-from-call [_ctx call _expr]
  (when (and (not (:unresolved? call)))
    (when-let [arg-types (:arg-types call)]
      (let [call-ns (:resolved-ns call)
            call-name (:name call)]
        ;; (prn call-ns call-name)
        (if-let [spec (get-in specs [call-ns call-name])]
          (if-let [fn-spec (:fn spec)]
            {:tag (fn-spec @arg-types)}
            {:tag (:ret spec)})
          {:call call})))))

(defn add-arg-type-from-call [ctx call _expr]
  (when-let [arg-types (:arg-types ctx)]
    (swap! arg-types conj (if-let [r (spec-from-call ctx call _expr)]
                            (assoc r
                                   :row (:row call)
                                   :col (:col call))
                            {:tag ::any}))))

(defn emit-warning! [{:keys [:findings] :as ctx} args tags problem]
  (let [via (first (:via problem))
        in-path (:in problem)
        pos (first in-path)
        offending-arg (when pos (nth args pos))
        offending-tag (when pos (nth tags pos))
        via-label (or (get labels via)
                      (when via
                        (name via)))
        offending-tag-label (or (get labels offending-tag)
                                (when offending-tag
                                  (when (keyword? offending-tag)
                                    (name offending-tag))))
        reason (:reason problem)
        insufficient? (= "Insufficient input" reason)
        extra? (= "Extra input" reason)]
    (cond insufficient?
          (findings/reg-finding! findings {:filename (:filename ctx)
                                           :row (:row (last args))
                                           :col (:col (last args))
                                           :type :type-mismatch
                                           :message "More arguments expected."})
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
                                                         ", received: " offending-tag-label
                                                         (when (= "true" (System/getenv "CLJ_KONDO_DEV"))
                                                           (format " (%s)" offending-tag))
                                                         ".")})
          #_#_:else (prn problem))))

;; (require '[clojure.pprint :refer [pprint]])

(defn args-spec-from-arities [arities arity]
  (when-let [called-arity (or (get arities arity)
                              (when-let [v (:varargs arity)]
                                (when (>= arity (:min-arity v))
                                  v)))]
    (when-let [ats (:arg-tags called-arity)]
      (let [ats (replace {nil ::any} ats)]
        ;; (prn (s/cat-impl [:a :b] ats ats))
        (s/cat-impl (repeatedly #(keyword (gensym))) ats ats)))))

(defn lint-arg-types [ctx {called-ns :ns called-name :name arities :arities :as called-fn} args tags]
  (let [ ;; TODO also pass the call, so we don't need the count
        arity (count args)]
    (when-let [args-spec (or (:args (get-in specs [called-ns called-name]))
                             (args-spec-from-arities arities arity))]
      (when-not (s/valid? args-spec tags)
        (let [d (s/explain-data args-spec tags)]
          ;; (prn called-ns called-name tags)
          ;; (prn "D" d)
          (run! #(emit-warning! ctx args tags %)
                (take 1 (:clj-kondo.impl.clojure.spec.alpha/problems d))))))))

;;;; Scratch

(comment
  )
