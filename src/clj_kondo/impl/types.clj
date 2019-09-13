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
   ::nat-int "natural integer"
   ::seqable "seqable collection"
   ::vector "vector"
   ::associative "associative collection"
   ::atom "atom"
   ::fn "function"
   ::ifn "function"})

(derive ::list ::seqable)
(derive ::vector ::seqable)
(derive ::string ::seqable)

(derive ::vector ::associative)
(derive ::map ::associative)

(derive ::nat-int ::number)

(derive ::list ::ifn) ;; for now, we need return types for this

(derive ::list ::atom) ;; for now, we need return types for this

(derive ::fn ::ifn)

(defn is? [x parent]
  (or (identical? x ::any)
      (isa? x parent)))

(s/def ::nil #(is? % ::nil))
(s/def ::seqable #(is? % ::seqable))
(s/def ::associative #(is? % ::associative))
(s/def ::number #(is? % ::number))
(s/def ::nat-int #(is? % ::nat-int))
(s/def ::atom #(is? % ::atom))
(s/def ::ifn #(is? % ::ifn))
(s/def ::string #{::string})
(s/def ::any any?)

(def specs {'clojure.core {'cons {:args (s/cat :x ::any :seq ::seqable)}
                           'assoc {:args (s/cat :map (s/alt :a ::associative :nil ::nil)
                                                :key ::any :val ::any :kvs (s/* (s/cat :ks ::any :vs ::any)))}
                           'swap! {:args (s/cat :atom ::atom :f ::ifn :args (s/* ::any))}
                           'inc {:args (s/cat :x ::number)}
                           'subs {:args (s/cat :s ::string
                                               :start ::nat-int
                                               :end (s/? ::nat-int))}}})

(defn expr->tag [ctx expr]
  (let [t (tag expr)]
    ;; (prn t)
    (case t
      :map ::map
      :vector ::vector
      :list ::list
      :fn ::fn
      :token (let [v (sexpr expr)]
               (cond
                 (nil? v) ::nil
                 (string? v) ::string
                 (nat-int? v) ::nat-int
                 :else ::any))
      ::any)))

(defn add-arg-type [ctx expr]
  (when-let [arg-types (:arg-types ctx)]
    (let [{:keys [:row :col]} (meta expr)]
      (swap! arg-types conj {:tag (expr->tag ctx expr)
                             :row row
                             :col col}))))

(defn emit-warning! [{:keys [:findings] :as ctx} args problem]
  ;; (prn args problem)
  (let [via (first (:via problem))
        in-path (:in problem)
        offending-arg (get-in args in-path)
        offending-tag (:tag offending-arg)
        via-label (get labels via via)
        offending-tag-label (get labels offending-tag offending-tag)
        reason (:reason problem)
        insufficient? (= "Insufficient input" reason)]
    (cond insufficient?
          (findings/reg-finding! findings {:filename (:filename ctx)
                                           :row (:row (last args))
                                           :col (:col (last args))
                                           :type :type-mismatch
                                           :message "More arguments expected."} )
          (and via-label offending-tag-label)
          (findings/reg-finding! findings {:filename (:filename ctx)
                                           :row (:row offending-arg)
                                           :col (:col offending-arg)
                                           :type :type-mismatch
                                           :message (str "Expected: " via-label
                                                         ", received: " offending-tag-label ".")}))))

(defn lint-arg-types [ctx called-ns called-name args]
  (let [spec (get-in specs [called-ns called-name])
        args-spec (:args spec)
        tags (map :tag args)]
    (when-not (s/valid? args-spec tags)
      (let [d (s/explain-data args-spec tags)]
        (run! #(emit-warning! ctx args %)
              (take 1 (:clj-kondo.impl.clojure.spec.alpha/problems d)))))))
