(ns clj-kondo.impl.types
  {:no-doc true}
  (:refer-clojure :exclude [keyword select-keys get-in])
  (:require
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.types.clojure.core :refer [clojure-core cljs-core]]
   [clj-kondo.impl.types.clojure.set :refer [clojure-set]]
   [clj-kondo.impl.types.clojure.string :refer [clojure-string]]
   [clj-kondo.impl.types.clojure.test :refer [clojure-test]]
   [clj-kondo.impl.types.utils :as type-utils]
   [clj-kondo.impl.utils :as utils :refer
    [tag sexpr select-keys get-in]]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(def known-types
  #{:string
    :char-sequence
    :seqable
    :int
    :long
    :short
    :number
    :pos-int
    :nat-int
    :neg-int
    :double
    :byte
    :ratio
    :vector
    :sequential
    :associative
    :coll
    :ideref
    :ifn
    :stack
    :map
    :nil
    :set
    :sorted-set
    :fn
    :keyword
    :symbol
    :transducer
    :list
    :seq
    :sorted-map
    :boolean
    :atom
    :future
    :regex
    :char
    :seqable-or-transducer
    :throwable
    :any
    :float
    :var
    :ilookup
    :array
    :inst
    :class})

(def built-in-specs
  {'clojure.core clojure-core
   'cljs.core cljs-core
   'clojure.set clojure-set
   'clojure.string clojure-string
   'clojure.test clojure-test})

(def predicate->tag
  "Core type predicates mapped to the type each proves about its argument."
  '{string? :string number? :number int? :int integer? :int pos-int? :pos-int
    nat-int? :nat-int neg-int? :neg-int double? :double float? :float
    ratio? :ratio map? :map vector? :vector seq? :seq seqable? :seqable
    coll? :coll keyword? :keyword symbol? :symbol set? :set list? :list
    char? :char boolean? :boolean fn? :fn ifn? :ifn associative? :associative
    sequential? :sequential var? :var nil? :nil})

(def is-a-relations
  {:string #{:char-sequence :seqable}
   :char-sequence #{:seqable}
   :int #{:number}
   :long #{:number}
   :short #{:number}
   :pos-int #{:int :nat-int :number}
   :nat-int #{:int :number}
   :neg-int #{:int :number}
   :double #{:number}
   :float #{:number}
   :byte #{:number}
   :ratio #{:number}
   :vector #{:seqable :sequential :associative :coll :ifn :stack :ilookup}
   :map #{:seqable :associative :coll :ifn :ilookup}
   :nil #{:seqable}
   :coll #{:seqable}
   :set #{:seqable :coll :ifn :ilookup}
   :sorted-set #{:set :seqable :coll :ifn :ilookup}
   :fn #{:ifn}
   :keyword #{:ifn}
   :symbol #{:ifn}
   :associative #{:seqable :coll :ifn :ilookup}
   :transducer #{:ifn :fn}
   :list #{:seq :sequential :seqable :coll :stack}
   :seq #{:seqable :sequential :coll}
   :sequential #{:coll :seqable}
   :sorted-map #{:map :seqable :associative :coll :ifn :ilookup}
   :atom #{:ideref}
   :future #{:ideref}
   :var #{:ideref :ifn}
   :array #{:seqable :ilookup}})

(def could-be-relations
  {:char-sequence #{:string}
   ;; Subtypes and widening primitive conversions (int can widen to float/double)
   :int #{:neg-int :nat-int :pos-int :long :short :float :double :number}
   :long #{:int :neg-int :nat-int :pos-int :short :float :double :number}
   :short #{:int :long :neg-int :nat-int :pos-int :float :double :number}
   :pos-int #{:long :short :float :double :number}
   :nat-int #{:pos-int :long :short :float :double :number}
   :neg-int #{:long :short :float :double :number}
   :byte #{:int :long :short :float :double :number}
   :float #{:double :number}
   :double #{:float :number}
   :number #{:neg-int :pos-int :nat-int :int :long :short :double :byte :ratio :float}
   :coll #{:map :sorted-map :vector :set :sorted-set :list :associative :seq
           :sequential :ifn :stack :ilookup}
   :seqable #{:coll :vector :set :sorted-set :map :associative
              :char-sequence :string :nil
              :list :seq :sequential :ifn :stack :sorted-map :ilookup :array}
   :associative #{:map :vector :sequential :stack :sorted-map}
   :ifn #{:fn :transducer :symbol :keyword :map :set :sorted-set :vector
          :associative :seqable :coll :sequential :stack :sorted-map :var
          :ideref :ilookup}
   :fn #{:transducer}
   :seq #{:list :stack}
   :stack #{:list :vector :seq :sequential :seqable :coll :ifn :associative :ilookup}
   :sequential #{:seq :list :vector :ifn :associative :stack :ilookup}
   :map #{:sorted-map}
   :set #{:sorted-set}
   :ideref #{:atom :future :var :ifn}
   :ilookup #{:map :set :sorted-set :sorted-map :coll :seqable :ifn :associative
              :vector :sequential :stack :array}})

(def misc-types #{:boolean :atom :future :regex :char :class :inst})

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
   :long "long"
   :short "short"
   :double "double"
   :float "float"
   :pos-int "positive integer"
   :nat-int "natural integer"
   :neg-int "negative integer"
   :byte "byte"
   :ratio "ratio"
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
   :future "future"
   :ideref "deref"
   :fn "function"
   :ifn "function"
   :keyword "keyword"
   :symbol "symbol"
   :transducer "transducer"
   :seqable-or-transducer "seqable or transducer"
   :set "set"
   :sorted-set "sorted set"
   :char-sequence "char sequence"
   :sequential "sequential collection"
   :throwable "throwable"
   :sorted-map "sorted map"
   :var "var"
   :ilookup "ILookup"
   :array "array"
   :class "class"
   :inst "instant"})

(defn label [k]
  (cond
    (map? k) (recur (:type k))
    (nilable? k)
    (str (get labels (unnil k)) " or nil")
    :else (get labels k)))

(defn match? [actual expected]
  (cond
    (keyword? actual)
    (or (identical? actual expected)
        (identical? actual :any)
        (identical? expected :any)
        (contains? (get is-a-relations actual) expected)
        (contains? (get could-be-relations actual) expected)
        (let [k (unnil actual)
              target (unnil expected)]
          (or
           (identical? k target)
           ;; :nilable/any, ah well..
           (identical? :any target)
           (contains? (get is-a-relations k) target)
           (contains? (get could-be-relations k) target)
           ;; lenient: someone emitted an unexpected type
           (not (contains? known-types k))))
        (and (identical? actual :nil) (or (nilable? expected)
                                          (identical? :seqable expected))))
    (map? actual) (recur (:type actual) expected)
    (set? actual) (some #(match? % expected) actual)))

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
    (int) :int
    (Integer java.lang.Integer) :nilable/int
    (long) :long
    (Long java.lang.Long) :nilable/long
    (short) :short
    (Short java.lang.Short) :nilable/short
    (double) :double
    (float) :float
    (Double java.lang.Double) :nilable/double
    (Float java.lang.Float) :nilable/float
    (ratio?) :ratio
    (CharSequence java.lang.CharSequence) :nilable/char-sequence
    (String java.lang.String) :nilable/string ;; as this is now way to
    ;; express non-nilable,
    ;; we'll go for the most
    ;; relaxed type
    (char) :char
    (Character java.lang.Character) :nilable/char
    (Seqable clojure.lang.Seqable) :seqable
    (java.util.List) :nilable/list
    (class) :class
    (Class java.lang.Class) :nilable/class
    (Date java.util.Date) :nilable/inst
    (Future java.util.concurrent.Future) :nilable/future
    (future) :future
    nil))

(defn number->tag [v]
  (cond (int? v)
        (cond (pos-int? v) :pos-int
              (nat-int? v) :nat-int
              (neg-int? v) :neg-int)
        (ratio? v) :ratio
        (double? v) :double
        :else :number))

(declare expr->tag)

(defn map-key [ctx expr]
  (case (tag expr)
    :token (cond
             (:prefix expr)
             (let [f (sexpr expr)]
               (if (symbol? f)
                 (clojure.core/symbol
                  (str (:prefix expr))
                  (name f))
                 (if (keyword? f)
                   (clojure.core/keyword (str (:prefix expr))
                                         (name (:k expr)))
                   f)))
             (:namespaced? expr)
             (let [k (:k expr)
                   kname (name (:k expr))]
               (if-let [kns (namespace k)]
                 (let [kns (symbol kns)
                       kns (some-> ctx :ns :qualify-ns (get kns))
                       res (if kns (clojure.core/keyword (str kns) kname)
                               (sexpr expr))]
                   res)
                 (if-let [kns (some-> ctx :ns :name)]
                   (clojure.core/keyword (str kns) kname)
                   (sexpr expr))))

             :else
             (sexpr expr))
    ;; single level only, ''x is the list (quote x), not x
    :quote (let [child (first (:children expr))]
             (if (identical? :token (tag child))
               (map-key ctx child)
               ::unknown))
    ::unknown))

(defn map->tag [ctx expr]
  (let [children (:children expr)
        ks (map #(map-key ctx %) (take-nth 2 children))
        mvals (take-nth 2 (rest children))
        vtags (map (fn [e]
                     (let [t (expr->tag ctx e)
                           m (meta e)]
                       ;; NOTE: be careful to not include any non-serializable data here, see issue #2165
                       (cond-> (select-keys m [:row :end-row :col :end-col])
                         t (assoc :tag t)))) mvals)]
    (cond-> {:type :map
             :val (zipmap ks vtags)}
      ;; a generated literal, e.g. a hook's placeholder map, is no evidence
      ;; of absence: only a map the user wrote is closed
      (let [m (meta expr)]
        (or (:clj-kondo.impl/generated expr)
            (:clj-kondo.impl/generated m)
            (not (:row m))))
      (assoc :open true))))

(defn destructured-key-tag
  "Value tag for map key `dk` of a destructuring form whose init has tag
  `form-tag`: the key's value type of a concrete map, provably :nil when the
  key is missing from a closed one, and a per-key deferred lookup via
  :kw-calls for a {:call ..} init, resolved at lint time like keyword access
  on the call itself. A defaulted binding gets no tag, its runtime value may
  be the :or default."
  [form-tag dk defaulted]
  (when-not defaulted
    (cond (identical? :map (:type form-tag))
          (if-let [e (find (:val form-tag) dk)]
            (:tag (val e))
            (when-not (:open form-tag) :nil))
          (and (:call form-tag) (keyword? dk))
          (let [c (:call form-tag)]
            {:call (assoc c :kw-calls ((fnil conj []) (:kw-calls c) dk))}))))

(defn called-arity [arities arity]
  (or (get arities arity)
      (when-let [v (:varargs arities)]
        (if-let [ma (:min-arity v)]
          (when (>= arity ma)
            v)
          ;; :min-arity isn't present, the arities were specified by a user
          v))))

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
                     (when-let [ca (called-arity a (:arity call))]
                       (when-let [t (:ret ca)]
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
            ;; a local tagged with a deferred call, e.g. a binding of a user
            ;; fn's return, carries the call under its :tag
            (let [call* (or (:call arg-type) (:call (:tag arg-type)))]
              (if call*
                {:call (assoc call*
                              ;; build chain of keyword calls
                              :kw-calls ((fnil conj []) (:kw-calls call*)
                                                        nm))}
                (let [t (:tag arg-type)
                      nm (:name call)]
                  (cond (:req t)
                        {:tag (get (:req (:tag arg-type))
                               nm)}
                        ;; keyword access on provable nil is nil
                        (identical? :nil t)
                        {:tag :nil}
                        :else
                        (when-let [v (:val t)]
                          ;; a closed literal map without the key provably
                          ;; yields nil. A present key with an unknown value
                          ;; type stays unknown
                          {:tag (if-let [e (find v nm)]
                                  (val e)
                                  (when-not (:open t) :nil))}))))))))))

(defn- array-class-literal? [sym]
  (when (and (symbol? sym) (namespace sym))
    (re-matches #"\d+" (name sym))))

(defn tag-from-usage
  [ctx usage expr]
  ;; Note, we need to return maps here because we are adding row and col later on.
  (when-not (:unresolved? usage)
    (let [called-ns (:resolved-ns usage)
          called-name (:name usage)
          conf (config/type-mismatch-config (:config ctx) called-ns called-name)
          tag (:type conf)]
      (cond
        tag {:tag tag}

        ;; Check if this is an array class literal
        ;; (Clojure 1.12+) would emit those as :class tags (type String/1) => java.lang.Class
        (and (identical? :clj (:lang ctx))
             (array-class-literal? (:value expr)))
        {:tag :class}

        :else {:usage (or tag
                          (select-keys usage [:filename :type :lang :base-lang :resolved-ns :ns :name]))}))))

(defn keyword
  "Converts tagged item into single keyword, if possible."
  [maybe-tag]
  (if (keyword? maybe-tag) maybe-tag
      (when (map? maybe-tag)
        (if (identical? :map (:type maybe-tag))
          :map
          (when-let [t (:tag maybe-tag)]
            (keyword t))))))

(defn spec-from-list-expr [{:keys [calls-by-id] :as ctx} expr]
  (when-let [id (:id expr)]
    (when-let [call (get @calls-by-id id)]
      (ret-tag-from-call ctx call expr))))

(defn expr->tag [{:keys [bindings lang quoted] :as ctx} expr]
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
                                           ;; a flow-narrowed tag (see narrow-binding) takes precedence over the declared tag
                                           (or (:narrowed-tag (meta b))
                                               (:tag b))))
                         (boolean? v) :boolean
                         (string? v) :string
                         (keyword? v) :keyword
                         (number? v) (number->tag v)
                         (char? v) :char))
              :regex :regex
              :quote (expr->tag (assoc ctx :quoted true) (first (:children expr)))
              :var :var
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

(defn add-arg-type-from-usage [ctx usage expr]
  (when-let [arg-types (:arg-types ctx)]
    (swap! arg-types conj (when-let [r (tag-from-usage ctx usage expr)]
                            (assoc r
                                   :row (:row usage)
                                   :col (:col usage)
                                   :end-row (:end-row usage)
                                   :end-col (:end-col usage))))))

(defn args-spec-from-arities [arities arity]
  (when-let [ca (called-arity arities arity)]
    (when-let [s (:args ca)]
      (vec s))))

(defn spec-args
  "Positional expected-type spec vector for a call to `called-ns`/`called-name`
  at `arity`, from a user-configured or built-in spec. Nil when no spec is
  known. Used for backward parameter-type inference."
  [config called-ns called-name arity]
  (when-let [spec (or (config/type-mismatch-config config called-ns called-name)
                      (get (get built-in-specs called-ns) called-name))]
    (when-let [a (:arities spec)]
      (args-spec-from-arities a arity))))

(defn spec-at
  "Positional spec for argument `idx`, where a {:op :rest} entry covers all
  remaining positions."
  [specs idx]
  (loop [i 0
         ss (seq specs)]
    (when-let [s (first ss)]
      (if (and (map? s) (identical? :rest (:op s)))
        (:spec s)
        (if (== i idx)
          s
          (recur (inc i) (rest ss)))))))

(defn desugar-nilable
  "A nilable keyword spec is sugar for a union with :nil."
  [s]
  (if (and (keyword? s) (nilable? s))
    #{:nil (unnil s)}
    s))

(defn constraining-spec?
  "A spec worth recording as evidence: a named type, a union, a :keys map spec
  or a deferred :arg-spec-of pointer. :any and unknown operator maps prove
  nothing."
  [s]
  (if (map? s)
    (utils/one-of (:op s) [:arg-spec-of :keys])
    (or (set? s)
        (and (keyword? s) (not (identical? :any s))))))

(defn infer-local-usage!
  "Backward parameter-type inference, triggered where a local usage is
  analyzed: binding `b` appears as argument `idx` of the call `[called-ns
  called-name arity]`, taken from the callstack head's ::infer-call meta.
  Records the callee's expected type as a constraint on the param, or a
  deferred {:op :arg-spec-of ..} constraint for a spec-less user fn. A usage in
  a conditional branch proves nothing, the guard may be what makes it safe.
  That covers narrowed usages, narrowing only happens in branches, and spine
  narrowing (assert, :pre), when it exists, should constrain: the guard throws,
  so the type is the contract. Type predicates need no special case: their arg
  spec is :any, so they record nothing."
  [ctx [called-ns called-name arity] infers b idx]
  (when-let [{:keys [param-infer mark]} (get infers b)]
    ;; equal counts mean no conditional was crossed since the param's fn entry
    (when (== mark (:branch-count ctx 0))
      (let [core? (utils/one-of called-ns [clojure.core cljs.core])
            s (if-let [specs (spec-args (:config ctx) called-ns called-name arity)]
                (spec-at specs idx)
                (when-not core?
                  {:op :arg-spec-of
                   :ns called-ns
                   :name called-name
                   :arity arity
                   :arg-idx idx
                   :lang (:lang ctx)
                   :base-lang (:base-lang ctx)}))]
        (when (constraining-spec? s)
          (let [s (desugar-nilable s)]
            (swap! param-infer update b
                   (fn [cur]
                     ;; insertion-ordered with dedup, for deterministic output
                     (if (some #(= % s) cur)
                       cur
                       (conj (or cur []) s))))))))))

(defn is-a?
  "Provable subtype check: every value of tag `a` is also of tag `b`."
  [a b]
  (or (identical? a b)
      (contains? (get is-a-relations a) b)))

(defn any-spec?
  "A spec satisfied by every value: :any or a union containing it."
  [s]
  (or (identical? :any s)
      (and (set? s) (contains? s :any))))

(defn intersect
  "Intersects specs `a` and `b`, keywords or union sets: the most specific
  union satisfying both, as the maximal named types implying each side. The
  dual of `union-type`. Returns a keyword, a set, or nil when nothing
  satisfies both. nil `a` means no evidence yet."
  [a b]
  (cond
    (nil? a) b
    ;; an any spec constrains nothing. Not all named types are related to
    ;; :any in is-a-relations, so the lattice scan would wrongly conflict
    (and (any-spec? a) (any-spec? b)) :any
    (any-spec? a) b
    (any-spec? b) a
    :else
    (let [as (if (set? a) a #{a})
          bs (if (set? b) b #{b})
          covers? (fn [t union] (some #(is-a? t %) union))
          ;; every named type that satisfies both unions: input members alone
          ;; miss types that imply both without being listed in either, e.g.
          ;; :vector when intersecting get's union with :seqable
          common (into #{} (filter (fn [t]
                                     (and (not (identical? :any t))
                                          (covers? t as)
                                          (covers? t bs))))
                       known-types)
          ;; keep the maximal elements, subsumed members add nothing
          maximal (into #{} (remove (fn [t]
                                      (some #(and (not (identical? t %)) (is-a? t %))
                                            common)))
                        common)]
      (case (count maximal)
        0 nil
        1 (first maximal)
        maximal))))

(defn intersect-all
  "Intersects a constraint vector to one spec, nil on conflict."
  [ts]
  (reduce (fn [acc t]
            (or (intersect acc t) (reduced nil)))
          nil ts))

(defn tag-matches?
  "Whether a value of tag `t` satisfies keyword-or-union spec `s`."
  [t s]
  (if (set? s)
    (some #(match? t %) s)
    (match? t s)))

(defn merge-inferred-arg-tags
  "Fills in arg specs for params from their collected constraints: keyword and
  union-set constraints intersect to the most specific union, a conflict proves
  nothing, and constraints with deferred or map-shaped members are stored as
  {:op :and :specs ..}, resolved when the cache is synced, see
  resolve-inferred-arg-types. A single {:op :keys} constraint passes through
  verbatim. A map-destructured binding's constraints, [i b seed k defaulted]
  entries, become the value type of key `k` in the param's {:op :keys} spec:
  under :req when the spec excludes nil and the key has no :or default, the
  key is proven required since its absence crashes the body, otherwise under
  :opt, next to any required keys the destructuring itself established."
  [simple-params param-infer arg-tags]
  (reduce (fn [tags [i b _ k defaulted]]
            (let [ts (get @param-infer b)]
              (if (seq ts)
                (if k
                  (if-let [spec (when (not-any? map? ts)
                                  (intersect-all ts))]
                    (update tags i (fn [cur]
                                     (let [slot (if (and (map? cur) (identical? :keys (:op cur)))
                                                  cur
                                                  ;; destructuring nil-punts, so nil
                                                  ;; stays a legal argument when no
                                                  ;; key is required
                                                  {:op :keys :nilable true})]
                                       (if (and (not defaulted) (not (tag-matches? :nil spec)))
                                         (-> slot
                                             (assoc-in [:req k] spec)
                                             (dissoc :nilable))
                                         (assoc-in slot [:opt k] spec)))))
                    tags)
                  (cond
                    (not-any? map? ts)
                    (assoc tags i (intersect-all ts))
                    (and (= 1 (count ts))
                         (identical? :keys (:op (first ts))))
                    (assoc tags i (first ts))
                    :else
                    (assoc tags i {:op :and :specs ts})))
                tags)))
          (vec arg-tags)
          simple-params))

(defn inferred-and? [s]
  (and (map? s) (identical? :and (:op s))))

(declare resolve-inferred-spec)

(defn resolve-deferred-arg-spec
  "Resolves one {:op :arg-spec-of ..} constraint via the callee's :args in
  idacs, which may itself be inferred, so inference chains through user fns.
  Nil when the callee or its spec is unknown, or when the chain recurs into
  `seen`."
  [idacs c seen]
  (let [k [(:ns c) (:name c) (:arity c) (:arg-idx c)]]
    (when-not (contains? seen k)
      (when-let [called-fn (utils/resolve-call* idacs c (:ns c) (:name c))]
        (when-let [s (args-spec-from-arities (:arities called-fn) (:arity c))]
          (let [s (get s (:arg-idx c))]
            (cond (keyword? s) (desugar-nilable s)
                  (set? s) s
                  (and (map? s) (identical? :keys (:op s))) s
                  (inferred-and? s)
                  (resolve-inferred-spec idacs s (conj seen k)))))))))

(defn resolve-inferred-spec
  "Resolves an inferred :args entry {:op :and :specs ..} to a concrete spec, or
  nil when nothing is provable."
  [idacs {:keys [specs]} seen]
  (reduce
   (fn [acc c]
     (let [t (cond (keyword? c) c
                   (set? c) c
                   (identical? :arg-spec-of (:op c))
                   (resolve-deferred-arg-spec idacs c seen))]
       (if t
         (or (intersect acc t)
             ;; conflicting constraints prove nothing
             (reduced nil))
         ;; an unresolvable constraint contributes nothing
         acc)))
   nil specs))

(defn trim-trailing-nils
  "Trailing nils check nothing, missing slots neither."
  [v]
  (loop [v v]
    (if (and (pos? (count v)) (nil? (peek v)))
      (recur (pop v))
      v)))

(defn resolve-inferred-arg-types
  "Resolves inferred {:op :and :specs ..} arg specs in `ns-data`'s vars to
  concrete specs, the twin of resolve-return-types. Runs when syncing the
  cache, so both linting and the cache only see the plain spec vocabulary,
  which older versions read fine."
  [idacs ns-data]
  (reduce-kv
   (fn [m k v]
     (if-let [arities (:arities v)]
       (let [new-arities
             (reduce-kv
              (fn [as arity {:keys [args] :as arity-data}]
                (if (vector? args)
                  (let [args* (trim-trailing-nils
                               (if (some inferred-and? args)
                                 (mapv (fn [s]
                                         (if (inferred-and? s)
                                           (resolve-inferred-spec idacs s #{})
                                           s))
                                       args)
                                 args))]
                    (cond (empty? args*)
                          ;; nothing proven, dead weight
                          (assoc as arity (type-utils/not-empty-arity
                                           (dissoc arity-data :args)))
                          (identical? args* args) as
                          :else (assoc as arity (assoc arity-data :args args*))))
                  as))
              arities arities)]
         (if (identical? new-arities arities)
           m
           (assoc m k (assoc v :arities new-arities))))
       m))
   ns-data ns-data))

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
                            :message "Insufficient input."})))

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
  ;; a statically unknown key could be any of the required keys
  (let [required? (and required? (not (contains? mval ::unknown)))]
    (doseq [[k target] (get spec spec-key)]
      (if-let [v (get mval k)]
        (when-let [t (type-utils/resolve-arg-type ctx v)]
          (if (= :keys (:op target))
            (lint-map! ctx target v t)
            (when-not (match? t target)
              (emit-non-match! ctx target v t))))
        (when required?
          (emit-missing-required-key! ctx arg k))))))

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
  (try
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
                          (recur check-ctx rest-args-spec rest-args rest-tags))
                      ;; an op from a newer version's cache: skip this arg
                      (recur check-ctx rest-args-spec rest-args rest-tags))
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
                              (recur check-ctx rest-args-spec rest-args rest-tags)))))))))
    (catch Exception e
      (if (= "true" (System/getenv "CLJ_KONDO_DEV"))
        (throw e)
        (binding [*out* *err*]
          (println "[clj-kondo]" "WARNING: error while checking types: " (-> e .getClass .getName) (str (.getMessage e))))))))

;;;; Scratch

(comment
  (match? :seqable :vector)
  (match? :map :associative)
  (match? :map :nilable/associative)
  (label :nilable/set))
