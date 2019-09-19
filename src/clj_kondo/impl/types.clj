(ns clj-kondo.impl.types
  {:no-doc true}
  (:require
   ;;[clj-kondo.impl.clojure.spec.alpha :as s]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :as utils :refer
    [tag sexpr]]))

(def labels
  {::nil "nil"
   ::string "string"
   ::nilable-string "string or nil"
   ::number "number"
   ::nilable-number "number or nil"
   ::int "integer"
   ::nilable-int "integer or nil"
   ::pos-int "positive integer"
   ::nat-int "natural integer"
   ::neg-int "negative integer"
   ::seqable-out "seqable collection"
   ::seqable "seqable collection"
   ::vector "vector"
   ::associative "associative collection"
   ::nilable-associative "associative collection or nil"
   ::map "map"
   ::atom "atom"
   ::fn "function"
   ::ifn "function"
   ::keyword "keyword"
   ::transducer "transducer"
   ::seqable-or-transducer "seqable or transducer"
   ::set "set"
   ::nilable-set "set or nil"
   ::char-sequence "char sequence"})

(def is-a-relations
  {::string #{::char-sequence ::seqable} ;; string is a char-sequence
   ::regex #{::char-sequence}
   ::char #{::char-sequence}
   ::int #{::number} ;; int is a number
   ::pos-int #{::int ::nat-int}
   ::nat-int #{::int}
   ::neg-int #{::int}
   ::double #{::number}
   ::vector #{::seqable ::associative ::coll ::ifn}
   ::map #{::seqable ::associative ::coll ::ifn}
   ::nil #{::seqable}
   ::seqable-out #{::seqable}
   ::coll #{::seqable}
   ::set #{::seqable ::ifn}
   ::fn #{::ifn}
   ::keyword #{::ifn}
   ::symbol #{::ifn}
   ::associative #{::seqable}})

(def could-be-relations
  {::char-sequence #{::string ::char ::regex}
   ::int #{::neg-int ::nat-int ::pos-int}
   ::number #{::int ::double}
   ::seqable-out #{::coll}
   ::seqable #{::coll ::string ::nil}
   ::associative #{::seqable ::map ::vector}})

;; TODO: check that every nilable type occurs as a key in this table!
(def nilable->type
  {::nilable-string ::string
   ::nilable-char-sequence ::char-sequence
   ::nilable-number ::number
   ::nilable-int ::int
   ::nilable-boolean ::boolean
   ::nilable-associative ::associative
   ::nilable-set ::set})

(def current-ns-name (str (ns-name *ns*)))

(defn sub? [k target]
  ;; (prn "sub?" k '-> target)
  (or (identical? k target)
      (when-let [targets (get is-a-relations k)]
        (some #(sub? % target) targets))))

(defn super? [k target]
  ;; (prn "super?" k '-> target)
  (or (identical? k target)
      (when-let [targets (get could-be-relations k)]
        (some #(super? % target) targets))))

(defn match? [k target]
  ;; (prn k '-> target)
  (cond (identical? k ::any) true
        (identical? target ::any) true
        (identical? k ::nil) (or (contains? nilable->type target)
                                 (identical? ::seqable target))
        :else
        (let [nk (get nilable->type k)
              nt (get nilable->type target)]
          ;; (prn k '-> nk '| target '-> nt)
          (case [(some? nk) (some? nt)]
            [true true]
            (match? nk nt)
            [true false]
            (match? nk target)
            [false true]
            (match? k nt)
            (or (sub? k target)
                (super? k target))))))

(comment
  (match? ::associative ::seqable)
  (match? ::nilable-number ::seqable)
  (match? ::pos-int ::seqable)
  )

(def nilable-types
  #{::char-sequence ::string ::regex ::char
    ::number ::double ::int ::neg-int ::nat-int ::pos-int
    ::coll ::vector ::set ::map ::list
    ::associative
    ::ifn ::fn ::transducer
    ::boolean
    ::atom
    ::keyword ::symbol})

(def other-types
  #{::seqable ::seqable-out ::nil})

;; (s/def ::any any?)

#_(defmacro reg-specs!
    "Defines spec for type k and type nilable-k."
    []
    `(do ~@(for [k nilable-types]
             (let [nilable-k (keyword current-ns-name (str "nilable-" (name k)))]
               `(do (s/def ~k #(match? % ~k))
                    (s/def ~nilable-k #(match? % ~nilable-k)))))
         ~@(for [k other-types]
             `(do (s/def ~k #(match? % ~k))))))

;; (reg-specs!)

(defn tag-from-meta
  ([meta-tag] (tag-from-meta meta-tag false))
  ([meta-tag out?]
   (case meta-tag
     void ::nil
     (boolean) ::boolean
     (Boolean java.lang.Boolean) ::nilable-boolean
     (byte) ::byte
     (Byte java.lang.Byte) ::nilable-byte
     (Number java.lang.Number) ::nilable-number
     (int long) ::int
     (Long java.lang.Long) ::nilable-int #_(if out? ::any-nilable-int ::any-nilable-int) ;; or ::any-nilable-int? , see 2451 main-test
     (float double) ::double
     (Float Double java.lang.Float java.lang.Double) ::nilable-double
     (CharSequence java.lang.CharSequence) ::nilable-char-sequence
     (String java.lang.String) ::nilable-string ;; as this is now way to
     ;; express non-nilable,
     ;; we'll go for the most
     ;; relaxed type
     (char) ::char
     (Character java.lang.Character) ::nilable-char
     (Seqable clojure.lang.Seqable) (if out? ::seqable-out ::seqable)
     (do #_(prn "did not catch tag:" meta-tag) nil nil))))

(defmacro with-meta-fn [fn-expr]
  `(with-meta
     ~fn-expr
     {:form '~fn-expr}))

(def clojure-core
  {;; 22
   'cons {:arities {2 {:arg-tags [::any ::seqable]}}}
   ;; 181
   'assoc {:arities {3 {:arg-tags [::nilable-associative ::any ::any]
                        :ret-tag ::associative}
                     :varargs {:min-arity 3
                               :arg-tags '[::nilable-associative ::any ::any (* [::any ::any])]
                               :ret-tag ::associative}}}
   ;; 544
   'str {:arities {:varargs {:arg-tags '[(* ::any)]
                             :ret-tag ::string}}}
   ;; 922
   'inc {:arities {1 {:arg-tags [::number]}}
         :ret ::number}
   ;; 947
   'reverse {:arities {1 {:arg-tags [::seqable]}}
             :ret ::seqable-out}
   ;; 2327
   'atom {:ret ::atom}
   ;; 2345
   'swap! {:arities {:varargs {:arg-tags '[::atom ::ifn (* ::any)]
                               :ret-tag ::any}}}
   ;; 2576
   'juxt {:arities {:varargs {:min-arity 0
                              :arg-tags '[(* ::ifn)]
                              :ret-tag ::ifn}}}
   ;; 2727
   'map {:arities {1 {:arg-tags [::ifn]
                      :ret-tag ::transducer}
                   :varargs {:arg-tags '[::ifn ::seqable (* ::seqable)]
                             :ret-tag ::seqable-out}}}
   ;; 2793
   'filter {:arities {1 {:arg-tags [::ifn]
                         :ret-tag ::transducer}
                      2 {:arg-tags [::ifn ::seqable]
                         :ret-tag ::seqable-out}}}
   ;; 2826
   'remove {:arities {1 {:arg-tags [::ifn]
                         :ret-tag ::transducer}
                      2 {:arg-tags [::ifn ::seqable]
                         :ret-tag ::seqable-out}}}
   ;; 4105
   'set {:ret ::set}
   ;; 4981
   'subs {:arities {2 {:arg-tags [::string ::nat-int]
                       :ret-tag ::string}
                    3 {:arg-tags [::string ::nat-int ::nat-int]
                       :ret-tag ::string}}}
   ;; 6790
   'reduce {:arities {2 {:arg-tags [::ifn ::seqable]
                         :ret-tag ::any}
                      3 {:arg-tags [::ifn ::any ::seqable]
                         :ret-tag ::any}}}
   ;; 6887
   'into {:arities {0 {:arg-tags []
                       :ret-tag ::coll}
                    1 {:arg-tags [::coll]}
                    2 {:arg-tags [::coll ::seqable]}
                    3 {:arg-tags [::coll ::transducer ::seqable]}}
          :fn (with-meta-fn
                (fn [args]
                  (let [t (:tag (first args))]
                    (if (identical? ::any t)
                      ::coll
                      t))))}
   ;; 6903
   'mapv {:arities {1 {:arg-tags [::ifn]
                       :ret-tag ::transducer}
                    :varargs {:arg-tags '[::ifn ::seqable (* ::seqable)]
                              :ret-tag ::vector}}}
   ;; 7313
   'filterv {:arities {2 {:arg-tags [::ifn ::seqable]
                          :ret-tag ::vector}}}
   ;; 7313
   'keep {:arities {1 {:arg-tags [::ifn]
                       :ret-tag ::transducer}
                    2 {:arg-tags [::ifn ::seqable]
                       :ret-tag ::seqable-out}}}})

(def specs
  {'clojure.core clojure-core
   'cljs.core clojure-core
   'clojure.set
   {'union
    {:arities {:varargs {:min-arity 0
                         :arg-tags '[(* ::nilable-set)]
                         :ret-tag ::nilable-set}}}
    'intersection
    {:arities {:varargs {:arg-tags '[::nilable-set (* ::nilable-set)]
                         :ret-tag ::nilable-set}}}
    'difference
    {:arities {:varargs {:arg-tags '[::nilable-set (* ::nilable-set)]
                         :ret-tag ::nilable-set}}}}
   'clojure.string
   {'join
    {:arities {1 {:arg-tags [::seqable]
                  :ret-tag ::string}
               2 {:arg-tags [::any ::seqable]
                  :ret-tag ::string}}}
    'starts-with?
    {:arities {2 {:arg-tags [::char-sequence ::string]
                  :ret-tag ::boolean}}}
    'ends-with?
    {:arities {2 {:arg-tags [::char-sequence ::string]
                  :ret-tag ::boolean}}}
    'includes?
    {:arities {2 {:arg-tags [::char-sequence ::char-sequence]
                  :ret-tag ::boolean}}}}})

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
          (or
           (when-let [a (:arities spec)]
             ;; TODO: match varargs
             (when-let [called-arity (or (get a (:arity call)) (:varargs a))]
               (when-let [t (:ret-tag called-arity)]
                 {:tag t})))
           (if-let [fn-spec (:fn spec)]
             {:tag (fn-spec @arg-types)}
             {:tag (:ret spec)}))
          {:call call})))))

(defn add-arg-type-from-call [ctx call _expr]
  (when-let [arg-types (:arg-types ctx)]
    (swap! arg-types conj (if-let [r (spec-from-call ctx call _expr)]
                            (assoc r
                                   :row (:row call)
                                   :col (:col call))
                            {:tag ::any}))))

(defn emit-warning* [{:keys [:findings] :as ctx} problem args via offending-arg offending-tag]
  (let [via-label (or (get labels via)
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
                                                         ".")}))))

(defn emit-warning! [ctx problem args tags]
  (let [via (first (:via problem))
        in-path (:in problem)
        pos (first in-path)
        offending-arg (when pos (nth args pos))
        offending-tag (when pos (nth tags pos))]
    (emit-warning* ctx problem args via offending-arg offending-tag)))

(defn args-spec-from-arities [arities arity]
  (when-let [called-arity (or (get arities arity)
                              (:varargs arities))]
    (when-let [s (:arg-tags called-arity)]
      (vec s))))

(defn emit-non-match! [{:keys [:findings :filename]} s arg t]
  (let [expected-label (or (get labels s) (name s))
        offending-tag-label (or (get labels t) (name t))]
    ;; (prn s arg t)
    (findings/reg-finding! findings {:filename filename
                                     :row (:row arg)
                                     :col (:col arg)
                                     :type :type-mismatch
                                     :message (str "Expected: " expected-label
                                                   (when (= "true" (System/getenv "CLJ_KONDO_DEV"))
                                                     (format " (%s)" s))
                                                   ", received: " offending-tag-label
                                                   (when (= "true" (System/getenv "CLJ_KONDO_DEV"))
                                                     (format " (%s)" t))
                                                   ".")})))

(defn emit-more-input-expected! [{:keys [:findings :filename]} arg]
  (findings/reg-finding! findings {:filename filename
                                   :row (:row arg)
                                   :col (:col arg)
                                   :type :type-mismatch
                                   :message (str "Insufficient input.")}))

(defn lint-arg-types [ctx {called-ns :ns called-name :name arities :arities :as _called-fn} args tags]
  (let [ ;; TODO also pass the call, so we don't need the count
        arity (count args)]
    (when-let [args-spec (or (when-let [s (get-in specs [called-ns called-name])]
                               (or (when-let [a (:arities s)]
                                     (args-spec-from-arities a arity))
                                   (:args s)))
                             (args-spec-from-arities arities arity))]
      ;; (prn "ARGS SPEC" called-ns called-name args-spec)
      (if (vector? args-spec)
        (loop [check-ctx {}
               [s & rest-args-spec :as all-specs] args-spec
               [a & rest-args :as all-args] args
               [t & rest-tags :as all-tags] tags]
          ;; (prn all-specs all-args)
          ;; (prn s t)
          (cond (and (empty? all-args)
                     (empty? all-specs)) ::done
                (list? s) (let [op (first s)]
                            ;; (prn "s" s)
                            (case op
                              * (recur
                                 (assoc check-ctx :remaining (second s))
                                 nil
                                 all-args
                                 all-tags)))
                (nil? s) (cond (seq all-specs) (recur check-ctx rest-args-spec rest-args rest-tags)
                               (:remaining check-ctx)
                               (recur check-ctx [(:remaining check-ctx)] all-args all-tags)) ;; nil is ::any
                (vector? s) (recur
                             check-ctx
                             (concat s rest-args-spec)
                             all-args
                             all-tags)
                (keyword? s)
                (cond (empty? all-args) (emit-more-input-expected! ctx (last args))
                      :else
                      (do (when-not (do
                                      ;; (prn "match t s" t s)
                                      nil
                                      (match? t s))
                            (emit-non-match! ctx s a t)
                            #_(let [d (s/explain-data s t)]
                                ;; (prn called-ns called-name tags)
                                (run! #(emit-warning* ctx % args s a t)
                                      (:clj-kondo.impl.clojure.spec.alpha/problems d))))
                          (recur check-ctx rest-args-spec rest-args rest-tags)))
                :else
                (throw (Exception. (str "unexpected spec: " (pr-str s))))))
        (throw (ex-info "unexpected" {}))
        #_(when-not (s/valid? args-spec tags)
            (let [d (s/explain-data args-spec tags)]
              ;; (prn called-ns called-name tags)
              ;; (prn "D" d)
              (run! #(emit-warning! ctx % args tags)
                    (:clj-kondo.impl.clojure.spec.alpha/problems d))))))))

;;;; Scratch

(comment
  (match? ::seqable ::vector)
  (match? ::map ::associative)
  (match? ::map ::nilable-associative)
  )
