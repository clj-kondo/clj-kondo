(ns clj-kondo.impl.types
  {:no-doc true}
  (:require
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.types.clojure.core :refer [clojure-core]]
   [clj-kondo.impl.types.clojure.set :refer [clojure-set]]
   [clj-kondo.impl.types.clojure.string :refer [clojure-string]]
   [clj-kondo.impl.utils :as utils :refer
    [tag sexpr]]
   [clojure.string :as str]))

(def built-in-specs
  {'clojure.core clojure-core
   'cljs.core clojure-core
   'clojure.set clojure-set
   'clojure.string clojure-string})

(def is-a-relations
  {:string #{:char-sequence :seqable}
   :char-sequence #{:seqable}
   :int #{:number}
   :pos-int #{:int :nat-int :number}
   :nat-int #{:int :number}
   :neg-int #{:int :number}
   :double #{:number}
   :vector #{:seqable :sequential :associative :coll :ifn :stack}
   :map #{:seqable :associative :coll :ifn}
   :nil #{:seqable}
   :coll #{:seqable}
   :set #{:seqable :coll :ifn}
   :fn #{:ifn}
   :keyword #{:ifn}
   :symbol #{:ifn}
   :associative #{:seqable :coll :ifn}
   :transducer #{:ifn}
   :list #{:seq :sequential :seqable :coll :stack}
   :seq #{:seqable :sequential :coll}
   :sequential #{:coll :seqable}})

(def could-be-relations
  {:char-sequence #{:string}
   :int #{:neg-int :nat-int :pos-int}
   :number #{:neg-int :pos-int :nat-int :int :double}
   :coll #{:map :vector :set :list  :associative :seq :sequential :ifn :stack}
   :seqable #{:coll :vector :set :map :associative
              :char-sequence :string :nil
              :list :seq :sequential :ifn :stack}
   :associative #{:map :vector :sequential :stack}
   :ifn #{:fn :transducer :symbol :keyword :map :set :vector :associative :seqable :coll
          :sequential :stack}
   :nat-int #{:pos-int}
   :seq #{:list :stack}
   :stack #{:list :vector :seq :sequential :seqable :coll :ifn :associative}
   :sequential #{:seq :list :vector :ifn :associative :stack}})

(def misc-types #{:boolean :atom :regex :char})


(defn nilable? [k]
  (= "nilable" (namespace k)))

(defn unnil
  "Returns the non-nilable version of k when it's nilable. Returns k otherwise."
  [k]
  (if (nilable? k)
    (keyword (name k))
    k))

(def labels
  {:nil "nil"
   :string "string"
   :number "number"
   :int "integer"
   :double "double"
   :pos-int "positive integer"
   :nat-int "natural integer"
   :neg-int "negative integer"
   :seqable "seqable collection"
   :seq "seq"
   :vector "vector"
   :stack "stack (list, vector, etc.)"
   :associative "associative collection"
   :map "map"
   :coll "collection"
   :list "list"
   :regex "regular expression"
   :char "character"
   :boolean "boolean"
   :atom "atom"
   :fn "function"
   :ifn "function"
   :keyword "keyword"
   :symbol "symbol"
   :transducer "transducer"
   :seqable-or-transducer "seqable or transducer"
   :set "set"
   :char-sequence "char sequence"
   :sequential "sequential collection"})

(defn label [k]
  (cond
    (map? k) (recur (:type k))
    (nilable? k)
    (str (get labels (unnil k)) " or nil")
    :else (get labels k)))

(def current-ns-name (str (ns-name *ns*)))

(defn sub? [k target]
  (or (identical? k target)
      (when-let [targets (get is-a-relations k)]
        (some #(sub? % target) targets))))

(defn super? [k target]
  (or (identical? k target)
      (when-let [targets (get could-be-relations k)]
        (some #(super? % target) targets))))

(defn match? [k target]
  (cond
    (or (identical? k target)
        (identical? k :any)
        (identical? target :any)
        (contains? (get is-a-relations k) target)
        (contains? (get could-be-relations k) target)) true
    (identical? k :nil) (or (nilable? target)
                            (identical? :seqable target))
    (map? k) (recur (:type k) target)
    :else
    (let [k (unnil k)
          target (unnil target)]
      (or
       (identical? k target)
       (contains? (get is-a-relations k) target)
       (contains? (get could-be-relations k) target)))))

;; TODO: we could look more intelligently at the source of the tag, e.g. if it
;; is not a third party String type
(defn tag-from-meta
  ([meta-tag] (tag-from-meta meta-tag false))
  ([meta-tag out?]
   (case meta-tag
     void :nil
     (boolean) :boolean
     (Boolean java.lang.Boolean) :nilable/boolean
     (byte) :byte
     (Byte java.lang.Byte) :nilable/byte
     (Number java.lang.Number) :nilable/number
     (int long) :int
     (Long java.lang.Long) :nilable/int #_(if out? :any-nilable-int :any-nilable-int) ;; or :any-nilable-int? , see 2451 main-test
     (float double) :double
     (Float Double java.lang.Float java.lang.Double) :nilable/double
     (CharSequence java.lang.CharSequence) :nilable/char-sequence
     (String java.lang.String) :nilable/string ;; as this is now way to
     ;; express non-nilable,
     ;; we'll go for the most
     ;; relaxed type
     (char) :char
     (Character java.lang.Character) :nilable/char
     (Seqable clojure.lang.Seqable) :seqable
     (do #_(prn "did not catch tag:" meta-tag) nil nil))))

(defn number->tag [v]
  (cond (int? v)
        (cond (pos-int? v) :pos-int
              (nat-int? v) :nat-int
              (neg-int? v) :neg-int)
        (double? v) :double
        :else :number))

(declare expr->tag)

(defn map-key [_ctx expr]
  (case (tag expr)
    :token (sexpr expr)
    ::unknown))

(defn map->tag [ctx expr]
  (let [children (:children expr)
        ks (map #(map-key ctx %) (take-nth 2 children))
        mvals (take-nth 2 (rest children))
        vtags (map (fn [e]
                     (assoc (meta e)
                            :tag (expr->tag ctx e))) mvals)]
    {:type :map
     :val (zipmap ks vtags)}))

(defn ret-from-arities [arities arity]
  (when-let [called-arity (or (get arities arity) (:varargs arities))]
    (when-let [t (:ret called-arity)]
      {:tag t})))

(defn ret-tag-from-call [{:keys [:config]} call _expr]
  (when (and (not (:unresolved? call)))
    (when-let [arg-types (:arg-types call)]
      (let [called-ns (:resolved-ns call)
            called-name (:name call)]
        ;; (prn called-ns called-name)
        (if-let [spec
                 (or
                  (config/type-mismatch-config config called-ns called-name)
                  (get-in built-in-specs [called-ns called-name]))]
          (or
           (when-let [a (:arities spec)]
             (when-let [called-arity (or (get a (:arity call)) (:varargs a))]
               (when-let [t (:ret called-arity)]
                 {:tag t})))
           (if-let [fn-spec (:fn spec)]
             {:tag (fn-spec @arg-types)}
             {:tag (:ret spec)}))
          ;; we delay resolving this call, because we might find the spec for by linting other code
          ;; see linters.clj
          {:call (select-keys call [:type :lang :base-lang :resolved-ns :ns :name :arity])})))))

(defn spec-from-list-expr [{:keys [:calls-by-id] :as ctx} expr]
  (or (if-let [id (:id expr)]
        (if-let [call (get @calls-by-id id)]
          (or (ret-tag-from-call ctx call expr)
              {:tag :any})
          {:tag :any})
        {:tag :any})))

(defn expr->tag [{:keys [:bindings :lang :quoted] :as ctx} expr]
  (let [t (tag expr)
        quoted? (or quoted (= :edn lang))]
    (case t
      :map (map->tag ctx expr)
      :vector :vector
      :set :set
      :list (if quoted? :list
                (:tag (spec-from-list-expr ctx expr))) ;; a call we know nothing about
      :fn :fn
      :token (let [v (sexpr expr)]
               (cond
                 (nil? v) :nil
                 (symbol? v) (if quoted? :symbol
                                 (if-let [b (get bindings v)]
                                   (or (:tag b) :any)
                                   :any))
                 (string? v) :string
                 (keyword? v) :keyword
                 (number? v) (number->tag v)
                 :else :any))
      :regex :regex
      :any)))

(defn add-arg-type-from-expr
  ([ctx expr] (add-arg-type-from-expr ctx expr (expr->tag ctx expr)))
  ([ctx expr tag] (when-let [arg-types (:arg-types ctx)]
                    (let [{:keys [:row :col]} (meta expr)]
                      (swap! arg-types conj {:tag tag
                                             :row row
                                             :col col})))))

(defn add-arg-type-from-call [ctx call _expr]
  (when-let [arg-types (:arg-types ctx)]
    (swap! arg-types conj (if-let [r (ret-tag-from-call ctx call _expr)]
                            (assoc r
                                   :row (:row call)
                                   :col (:col call))
                            {:tag :any}))))

(defn args-spec-from-arities [arities arity]
  (when-let [called-arity (or (get arities arity)
                              (:varargs arities))]
    (when-let [s (:args called-arity)]
      (vec s))))

(defn emit-non-match! [{:keys [:findings :filename]} s arg t]
  (let [expected-label-1-fn #(or (label %) (name %))
        expected-label (cond (keyword? s) (expected-label-1-fn s)
                             (set? s) (str/join " or " (map expected-label-1-fn s)))
        offending-tag-label (or (label t) (name t))]
    (findings/reg-finding! findings
                           {:filename filename
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
  (findings/reg-finding! findings
                         {:filename filename
                          :row (:row arg)
                          :col (:col arg)
                          :type :type-mismatch
                          :message (str "Insufficient input.")}))

(defn emit-missing-required-key! [{:keys [:findings :filename]} arg k]
  (findings/reg-finding! findings
                         {:filename filename
                          :row (:row arg)
                          :col (:col arg)
                          :type :type-mismatch
                          :message (str "Missing required key: " k)}))

(declare lint-map!)

(defn lint-map-types! [ctx arg mval spec spec-key required?]
  (doseq [[k target] (get spec spec-key)]
    (if-let [v (get mval k)]
      (when-let [t (:tag v)]
        (if (= :keys (:op target))
          (lint-map! ctx target v (:tag v))
          (when-not (match? t target)
            (emit-non-match! ctx target v t))))
      (when required?
        (emit-missing-required-key! ctx arg k)))))

(defn lint-map! [ctx s a t]
  (cond (keyword? t)
        (when-not (match? t :map)
          (emit-non-match! ctx :map a t))
        :else
        (when-let [mval (-> t :val)]
          (lint-map-types! ctx a mval s :req true)
          (lint-map-types! ctx a mval s :opt false))))

(defn lint-arg-types
  [{:keys [:config] :as ctx}
   {called-ns :ns called-name :name arities :arities :as _called-fn}
   args tags call]
  (let [called-ns (or called-ns (:resolved-ns call))
        called-name (or called-name (:name call))
        arity (:arity call)]
    (when-let [args-spec
               (or
                (when-let [s (config/type-mismatch-config config called-ns called-name)]
                  (when-let [a (:arities s)]
                    (args-spec-from-arities a arity)))
                (when-let [s (get-in built-in-specs [called-ns called-name])]
                  (when-let [a (:arities s)]
                    (args-spec-from-arities a arity)))
                (args-spec-from-arities arities arity))]
      (when (vector? args-spec)
        (loop [check-ctx {}
               [s & rest-args-spec :as all-specs] args-spec
               [a & rest-args :as all-args] args
               [t & rest-tags :as all-tags] tags]
          ;; (prn "S" s "CTX" check-ctx)
          (let [op (:op s)]
            (cond (and (empty? all-args)
                       (empty? all-specs)) :done
                  op
                  (case op
                    :rest
                    (recur (assoc check-ctx
                                  :rest (:spec s)
                                  :last (:last s))
                           nil
                           all-args
                           all-tags)
                    :keys
                    (do (lint-map! ctx s a t)
                        (recur check-ctx rest-args-spec rest-args rest-tags)))
                  (nil? s) (cond (seq all-specs)
                                 ;; nil is :any
                                 (recur check-ctx rest-args-spec rest-args rest-tags)
                                 (:rest check-ctx)
                                 (if (seq rest-args)
                                   ;; not the last one
                                   (recur check-ctx [(:rest check-ctx)] all-args all-tags)
                                   ;; the last arg
                                   (recur check-ctx [(some check-ctx [:last :rest])] all-args all-tags)))
                  (vector? s) (recur check-ctx (concat s rest-args-spec) all-args all-tags)
                  (set? s) (do (when-not (some #(match? t %) s)
                                 (emit-non-match! ctx s a t))
                               (recur check-ctx rest-args-spec rest-args rest-tags))
                  (keyword? s)
                  (cond (empty? all-args) (emit-more-input-expected! ctx (last args))
                        :else
                        (do (when-not (do
                                        ;; (prn "match t s" t s)
                                        nil
                                        (match? t s))
                              (emit-non-match! ctx s a t))
                            (recur check-ctx rest-args-spec rest-args rest-tags))))))))))

;;;; Scratch

(comment
  (match? :seqable :vector)
  (match? :map :associative)
  (match? :map :nilable/associative)
  (label :nilable/set)
  )
