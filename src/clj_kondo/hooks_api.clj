(ns clj-kondo.hooks-api
  {:no-doc true}
  (:require
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.rewrite-clj.node :as node]
   [clj-kondo.impl.rewrite-clj.node.protocols]
   [clj-kondo.impl.rewrite-clj.parser :as parser]
   [clj-kondo.impl.utils :as utils])
  (:refer-clojure :exclude [macroexpand resolve]))

(defn generated-node?
  "Checks whether node was generated by other hook, or if the node was
  written by user. Assumes that a node without location metadata was
  generated."
  [node]
  (or (:clj-kondo.impl/generated node)
      (let [m (meta node)]
        (or (:clj-kondo.impl/generated m)
            (not (:row m))))))

(defn parse-string [s]
  (parser/parse-string s))

(defn node? [n]
  (instance? clj_kondo.impl.rewrite_clj.node.protocols.Node n))

(defn keyword-node? [n]
  (utils/keyword-node? n))

(def keyword-node
  (comp utils/mark-generate utils/keyword-node))

(defn string-node? [n]
  (instance? clj_kondo.impl.rewrite_clj.node.string.StringNode n))

(def string-node
  (comp utils/mark-generate utils/string-node))

(defn token-node? [n]
  (instance? clj_kondo.impl.rewrite_clj.node.token.TokenNode n))

(def token-node
  (comp utils/mark-generate utils/token-node))

(defn quote-node? [n]
  (instance? clj_kondo.impl.rewrite_clj.node.quote.QuoteNode n))

(def quote-node
  (comp utils/mark-generate utils/quote-node))

(defn vector-node? [n]
  (and (instance? clj_kondo.impl.rewrite_clj.node.seq.SeqNode n)
       (identical? :vector (utils/tag n))))

(def ^:dynamic *reload* false)
(def ^:dynamic ^:private *debug* false)

(defn- assert-children-nodes [children]
  (when *debug*
    (when-let [node (some #(when-not (node? %)
                             %) children)]
      (throw (new IllegalArgumentException (str "Not a node: " node))))))

(defn vector-node [children]
  (assert-children-nodes children)
  (utils/mark-generate (utils/vector-node children)))

(def list-node? utils/list-node?)

(defn list-node [children]
  (assert-children-nodes children)
  (utils/mark-generate (utils/list-node children)))

(def set-node? utils/set-node?)

(defn set-node [children]
  (assert-children-nodes children)
  (utils/mark-generate (utils/set-node children)))

(defn map-node? [n]
  (and (instance? clj_kondo.impl.rewrite_clj.node.seq.SeqNode n)
       (identical? :map (utils/tag n))))

(defn map-node [children]
  (assert-children-nodes children)
  (utils/mark-generate (utils/map-node children)))

(defn sexpr [expr]
  (node/sexpr expr))

(defn tag [n]
  (node/tag n))

(defn reg-finding! [m]
  (let [ctx utils/*ctx*
        filename (:filename ctx)]
    (findings/reg-finding! ctx (assoc m :filename filename))))

(defn reg-keyword!
  [k reg-by]
  (utils/assoc-some k :reg reg-by))

(defn coerce [s-expr]
  (node/coerce s-expr))

(defn- var-definitions
  "Project cached analysis as a subset of public var-definitions."
  [analysis]
  (let [selected-keys [:ns :name
                       :fixed-arities :varargs-min-arity
                       :private :macro]]
    (->> (dissoc analysis :filename :source)
         (utils/map-vals #(if (map? %)
                            (select-keys % selected-keys)
                            %)))))

(defn- ns-analysis*
  "Adapt from-cache-1 to provide a uniform return format.
  Unifies the format of cached information provided for each source
  language."
  [lang ns-sym]
  (if (= :cljc lang)
    (->> (dissoc
          (cache/from-cache-1 (:cache-dir utils/*ctx*) :cljc ns-sym)
          :filename
          :source)
         (utils/map-vals var-definitions))
    (some->> (cache/from-cache-1 (:cache-dir utils/*ctx*) lang ns-sym)
             var-definitions
             (hash-map lang))))

(defn ns-analysis
  "Return any cached analysis for the namespace identified by ns-sym.
  Returns a map keyed by language keyword with values being maps of var
  definitions keyed by defined symbol. The value for each symbol is a
  subset of the values provide by the top level :analysis option."
  ([ns-sym] (ns-analysis ns-sym {}))
  ([ns-sym {:keys [lang]}]
   (if lang
     (ns-analysis* lang ns-sym)
     (reduce
      merge
      {}
      (map #(ns-analysis* % ns-sym) [:cljc :clj :cljs])))))

(defn resolve [{:keys [name call locals]}]
  (let [ctx utils/*ctx*
        locals (or (:bindings ctx)
                   locals)]
    (if (and (simple-symbol? name)
             (contains? locals name))
      nil
      (let [ret (namespace/resolve-name ctx call (-> ctx :ns :name) name nil)]
        (when-not (:unresolved? ret)
          (select-keys ret [:ns :name]))))))

(defn env []
  (let [bnds (:bindings utils/*ctx*)]
    (zipmap (keys bnds) (repeat {}))))

(defn callstack []
  (utils/format-callstack utils/*ctx*))

;; ctx call? ns-name name-sym expr
