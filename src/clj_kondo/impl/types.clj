(ns clj-kondo.impl.types
  {:no-doc true}
  (:require
   [clj-kondo.impl.clojure.spec.alpha :as s]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :as utils :refer
    [tag sexpr]]))

(def labels
  {::string "string"
   ::number "number"})

(derive ::nat-int ::number)
(derive ::any ::number)

(s/def ::number #(isa? % ::number))
(s/def ::nat-int #(isa? % ::nat-int))
(s/def ::string #{::string})
(s/def ::any any?)

(def specs {'clojure.core {'inc {:args (s/cat :x ::number)}
                           'subs {:args (s/cat :s ::string
                                               :start ::nat-int
                                               :end (s/? ::nat-int))}}})

(defn expr->tag [ctx expr]
  (let [t (tag expr)]
    (case t
      :map ::map
      :token (let [v (sexpr expr)]
               (cond
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
  (let [via (first (:via problem))
        in-path (:in problem)
        offending-arg (get-in args in-path)
        offending-tag (:tag offending-arg)
        msg (str "Expected a " (get labels via) " but received a "
                 (get labels offending-tag) ".")]
    (findings/reg-finding! findings {:filename (:filename ctx)
                                     :row (:row offending-arg)
                                     :col (:col offending-arg)
                                     :type :type-mismatch
                                     :message msg} )))

(defn lint-arg-types [ctx called-ns called-name args]
  (let [spec (get-in specs [called-ns called-name])
        args-spec (:args spec)
        tags (map :tag args)]
    (when-not (s/valid? args-spec tags)
      (let [d (s/explain-data args-spec tags)]
        (run! #(emit-warning! ctx args %)
              (:clj-kondo.impl.clojure.spec.alpha/problems d))))))
