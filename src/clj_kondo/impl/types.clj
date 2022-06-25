(ns clj-kondo.impl.types
  {:no-doc true}
  (:refer-clojure :exclude [keyword])
  (:require
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.types.clojure.core :refer [clojure-core cljs-core]]
   [clj-kondo.impl.types.clojure.set :refer [clojure-set]]
   [clj-kondo.impl.types.clojure.string :refer [clojure-string]]
   [clj-kondo.impl.types.utils :as type-utils]
   [clj-kondo.impl.utils :as utils :refer
    [tag sexpr]]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(def built-in-specs
  {'clojure.core clojure-core
   'cljs.core cljs-core
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
   :byte #{:number}
   :vector #{:seqable :sequential :associative :coll :ifn :stack}
   :map #{:seqable :associative :coll :ifn}
   :nil #{:seqable}
   :coll #{:seqable}
   :set #{:seqable :coll :ifn}
   :fn #{:ifn}
   :keyword #{:ifn}
   :symbol #{:ifn}
   :associative #{:seqable :coll :ifn}
   :transducer #{:ifn :fn}
   :list #{:seq :sequential :seqable :coll :stack}
   :seq #{:seqable :sequential :coll}
   :sequential #{:coll :seqable}
   :sorted-map #{:map :seqable :associative :coll :ifn}})

(def could-be-relations
  {:char-sequence #{:string}
   :int #{:neg-int :nat-int :pos-int}
   :number #{:neg-int :pos-int :nat-int :int :double :byte}
   :coll #{:map :sorted-map :vector :set :list  :associative :seq :sequential :ifn :stack}
   :seqable #{:coll :vector :set :map :associative
              :char-sequence :string :nil
              :list :seq :sequential :ifn :stack :sorted-map}
   :associative #{:map :vector :sequential :stack :sorted-map}
   :ifn #{:fn :transducer :symbol :keyword :map :set :vector :associative :seqable :coll
          :sequential :stack :sorted-map}
   :fn #{:transducer}
   :nat-int #{:pos-int}
   :seq #{:list :stack}
   :stack #{:list :vector :seq :sequential :seqable :coll :ifn :associative}
   :sequential #{:seq :list :vector :ifn :associative :stack}
   :map #{:sorted-map}})

(def misc-types #{:boolean :atom :regex :char})


(defn nilable? [k]
  (= "nilable" (namespace k)))

(defn unnil
  "Returns the non-nilable version of k when it's nilable. Returns k otherwise."
  [k]
  (if (nilable? k)
    (clojure.core/keyword (name k))
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
   :byte "byte"
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
   :sequential "sequential collection"
   :throwable "throwable"
   :sorted-map "sorted map"})

(defn label [k]
  (cond
    (map? k) (recur (:type k))
    (nilable? k)
    (str (get labels (unnil k)) " or nil")
    :else (get labels k)))

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
    (set? k) (some #(match? % target) k)
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
  [meta-tag]
  (case meta-tag
    void :nil
    (boolean) :boolean
    (Boolean java.lang.Boolean) :nilable/boolean
    (byte) :byte
    (Byte java.lang.Byte) :nilable/byte
    (Number java.lang.Number) :nilable/number
    (int long) :int
    (Integer java.lang.Integer Long java.lang.Long) :nilable/int #_(if out? :any-nilable-int :any-nilable-int) ;; or :any-nilable-int? , see 2451 main-test
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
    nil))

(defn number->tag [v]
  (cond (int? v)
        (cond (pos-int? v) :pos-int
              (nat-int? v) :nat-int
              (neg-int? v) :neg-int)
        (double? v) :double
        :else :number))

(declare expr->tag)

(defn map-key [ctx expr]
  (case (tag expr)
    :token (if (:namespaced? expr)
             (let [k (:k expr)
                   kname (name k)]
               (if-let [kns (namespace k)]
                 (let [kns (symbol kns)
                       kns (some-> ctx :ns :qualify-ns (get kns))
                       res (if kns (clojure.core/keyword (str kns) kname)
                               (sexpr expr))]
                   res)
                 (if-let [kns (some-> ctx :ns :name)]
                   (clojure.core/keyword (str kns) kname)
                   (sexpr expr))))
             (sexpr expr))
    ::unknown))

(defn map->tag [ctx expr]
  (let [children (:children expr)
        ks (map #(map-key ctx %) (take-nth 2 children))
        mvals (take-nth 2 (rest children))
        vtags (map (fn [e]
                     (let [t (expr->tag ctx e)
                           m (meta e)
                           m (if t (assoc m :tag t) m)]
                       m)) mvals)]
    {:type :map
     :val (zipmap ks vtags)}))

(defn ret-tag-from-call
  [ctx call _expr]
  ;; Note, we need to return maps here because we are adding row and col later on.
  (or (:ret call)
      (when (not (:unresolved? call))
        (or (when-let [ret (:ret call)]
              {:tag ret})
            (when-let [arg-types (:arg-types call)]
              (let [called-ns (:resolved-ns call)
                    called-name (:name call)]
                (if-let [spec
                         (or
                          (config/type-mismatch-config (:config ctx) called-ns called-name)
                          (get-in built-in-specs [called-ns called-name]))]
                  (or
                   (when-let [a (:arities spec)]
                     (when-let [called-arity (or (get a (:arity call)) (:varargs a))]
                       (when-let [t (:ret called-arity)]
                         {:tag t})))
                   (if-let [fn-spec (:fn spec)]
                     (when-let [t (fn-spec @arg-types)]
                       {:tag t})
                     (when-let [t (:ret spec)]
                       {:tag t})))
                  ;; we delay resolving this call, because we might find the spec for by linting other code
                  ;; see linters.clj
                  {:call (select-keys call [:filename :type :lang :base-lang :resolved-ns :ns :name :arity])})))))
      ;; Keyword calls are handled differently, we try to resolve the return
      ;; type dynamically for the 1-arity version.
      (let [nm (:name call)
            arg-types (:arg-types call)
            arg-types (when arg-types (deref arg-types))
            arg-count (count arg-types)]
        (when (and (keyword? nm)
                   (= 1 arg-count))
          (when-let [arg-type (first arg-types)]
            (let [call* (:call arg-type)]
              (if call*
                {:call (assoc call*
                              ;; build chain of keyword calls
                              :kw-calls ((fnil conj []) (:kw-calls call*)
                                         nm))}
                (let [t (:tag arg-type)
                      nm (:name call)]
                  (if (:req t)
                    {:tag (get (:req (:tag arg-type))
                               nm)}
                    (when-let [v (:val t)]
                      {:tag (get v nm)}))))))))))

(defn keyword
  "Converts tagged item into single keyword, if possible."
  [maybe-tag]
  (if (keyword? maybe-tag) maybe-tag
      (when (map? maybe-tag)
        (if (identical? :map (:type maybe-tag))
          :map
          (when-let [t (:tag maybe-tag)]
            (keyword t))))))

(defn spec-from-list-expr [{:keys [:calls-by-id] :as ctx} expr]
  (when-let [id (:id expr)]
    (when-let [call (get @calls-by-id id)]
      (ret-tag-from-call ctx call expr))))

(defn expr->tag [{:keys [:bindings :lang :quoted] :as ctx} expr]
  (let [t (tag expr)
        quoted? (or quoted (identical? :edn lang))
        ret (case t
              :map (map->tag ctx expr)
              :vector :vector
              :set :set
              :list (if quoted? :list
                        (:tag (spec-from-list-expr ctx expr))) ;; a call we know nothing about
              :fn :fn
              :multi-line :string
              :token (let [v (sexpr expr)]
                       (cond
                         (nil? v) :nil
                         (symbol? v) (if quoted? :symbol
                                         (when-let [b (get bindings v)]
                                           (:tag b)))
                         (boolean? v) :boolean
                         (string? v) :string
                         (keyword? v) :keyword
                         (number? v) (number->tag v)
                         (char? v) :char))
              :regex :regex
              :quote (expr->tag (assoc ctx :quoted true) (first (:children expr)))
              nil)]
    ;; (prn (sexpr expr) '-> ret)
    ret))

(defn add-arg-type-from-expr
  ([ctx expr] (add-arg-type-from-expr ctx expr (expr->tag ctx expr)))
  ([ctx expr tag]
   (when-let [arg-types (:arg-types ctx)]
     (let [m (meta expr)]
       (swap! arg-types conj (when tag
                               {:tag tag
                                :row (:row m)
                                :col (:col m)
                                :end-row (:end-row m)
                                :end-col (:end-col m)}))))))

(defn add-arg-type-from-call [ctx call expr]
  (when-let [arg-types (:arg-types ctx)]
    (swap! arg-types conj (when-let [r (ret-tag-from-call ctx call expr)]
                            (assoc r
                                   :row (:row call)
                                   :col (:col call)
                                   :end-row (:end-row call)
                                   :end-col (:end-col call))))))

(defn args-spec-from-arities [arities arity]
  (when-let [called-arity (or (get arities arity)
                              (:varargs arities))]
    (when-let [s (:args called-arity)]
      (vec s))))

(defn tag->label [x]
  (let [label-fn #(or (label %) (name %))
        l (cond (keyword? x) (label-fn x)
                (set? x) (str/join " or " (map label-fn x))
                ;; TODO:
                (map? x) "map")]
    l))

(defn emit-non-match! [ctx s arg t]
  (let [expected-label (tag->label s)
        offending-tag-label (tag->label t)]
    (findings/reg-finding! ctx
                           {:filename (:filename ctx)
                            :row (:row arg)
                            :col (:col arg)
                            :end-row (:end-row arg)
                            :end-col (:end-col arg)
                            :type :type-mismatch
                            :message (str "Expected: " expected-label
                                          (when (= "true" (System/getenv "CLJ_KONDO_DEV"))
                                            (format " (%s)" s))
                                          ", received: " offending-tag-label
                                          (when (= "true" (System/getenv "CLJ_KONDO_DEV"))
                                            (format " (%s)" t))
                                          ".")})))

(defn emit-more-input-expected! [ctx call arg]
  (let [expr (or arg call)]
    (findings/reg-finding! ctx
                           {:filename (:filename ctx)
                            :row (:row expr)
                            :col (:col expr)
                            :end-row (:end-row expr)
                            :end-col (:end-col expr)
                            :type :type-mismatch
                            :message (str "Insufficient input.")})))

(defn emit-missing-required-key! [ctx arg k]
  (findings/reg-finding! ctx
                         {:filename (:filename ctx)
                          :row (:row arg)
                          :col (:col arg)
                          :end-row (:end-row arg)
                          :end-col (:end-col arg)
                          :type :type-mismatch
                          :message (str "Missing required key: " k)}))

(declare lint-map!)

(defn lint-map-types! [ctx arg mval spec spec-key required?]
  (doseq [[k target] (get spec spec-key)]
    (if-let [v (get mval k)]
      (when-let [t (type-utils/resolve-arg-type ctx v)]
        (if (= :keys (:op target))
          (lint-map! ctx target v t)
          (when-not (match? t target)
            (emit-non-match! ctx target v t))))
      (when required?
        (emit-missing-required-key! ctx arg k)))))

(defn lint-map! [ctx s a t]
  (cond (and (:nilable s) (= :nil t))
        nil
        (keyword? t)
        (when-not (match? t :map)
          (emit-non-match! ctx :map a t))
        :else
        (when-let [mval (-> t :val)]
          (lint-map-types! ctx a mval s :req true)
          (lint-map-types! ctx a mval s :opt false))))

(defn lint-arg-types
  [ctx {called-ns :ns called-name :name arities :arities :as _called-fn}
   args tags call]
  (let [config (:config ctx)
        called-ns (or called-ns (:resolved-ns call))
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
                  (cond (empty? all-args) (emit-more-input-expected! ctx call (last args))
                        :else
                        (do (when-not (match? t s)
                              (emit-non-match! ctx s a t))
                            (recur check-ctx rest-args-spec rest-args rest-tags))))))))))

;;;; Scratch

(comment
  (match? :seqable :vector)
  (match? :map :associative)
  (match? :map :nilable/associative)
  (label :nilable/set)
  )
