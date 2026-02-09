(ns clj-kondo.impl.utils
  {:no-doc true}
  (:refer-clojure :exclude [update-vals])
  (:require
   [babashka.fs :as fs]
   [clj-kondo.impl.analyzer.common :as common]
   [clj-kondo.impl.rewrite-clj.node.keyword :as k]
   [clj-kondo.impl.rewrite-clj.node.protocols :as node]
   [clj-kondo.impl.rewrite-clj.node.quote :as node-quote]
   [clj-kondo.impl.rewrite-clj.node.seq :as seq]
   [clj-kondo.impl.rewrite-clj.node.string :as node-string]
   [clj-kondo.impl.rewrite-clj.node.token :as token]
   [clj-kondo.impl.rewrite-clj.parser :as p]
   [clj-kondo.impl.rewrite-clj.reader :refer [*reader-exceptions*]]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

;;; export rewrite-clj functions

(defn tag [expr]
  (when expr (node/tag expr)))

(def map-node seq/map-node)
(def vector-node seq/vector-node)
(def list-node seq/list-node)
(def set-node seq/set-node)
(def token-node token/token-node)
(def quote-node node-quote/quote-node)
(def keyword-node k/keyword-node)
(def string-node node-string/string-node)

(defn sexpr [node]
  (try
    (node/sexpr node)
    (catch clojure.lang.ExceptionInfo e
      (if *reader-exceptions*
        (let [{:keys [type ex-kind]} (ex-data e)]
          (if (and (= :reader-exception type)
                   (= :reader-error ex-kind))
            (let [m (meta node)
                  f (assoc m :message (.getMessage e))]
              (swap! *reader-exceptions* conj (ex-info "Syntax error" {:findings [f]})))
            (throw e)))
        (throw e)))))

(defn list-node? [n]
  (and (instance? clj_kondo.impl.rewrite_clj.node.seq.SeqNode n)
       (identical? :list (tag n))))

(defn set-node? [n]
  (and (instance? clj_kondo.impl.rewrite_clj.node.seq.SeqNode n)
       (identical? :set (tag n))))

;;; end export

(defn print-err! [& strs]
  (binding [*out* *err*]
    (apply println strs))
  nil)

(defn symbol-call
  "Returns symbol of call"
  [expr]
  (when (= :list (node/tag expr))
    (let [first-child (-> expr :children first)
          ?sym (:value first-child)]
      (when (symbol? ?sym)
        ?sym))))

(defn keyword-node? [n]
  (instance? clj_kondo.impl.rewrite_clj.node.keyword.KeywordNode n))

(defn node->keyword
  "Returns keyword from node, if it contains any."
  [node]
  (:k node))

;; this zipper version is much slower than the above
#_(defn remove-noise
    ([expr] (remove-noise expr nil))
    ([expr config]
     (let [zloc (z/edn* expr)]
       (loop [zloc zloc]
         (cond (z/end? zloc) (z/root zloc)
               :else (let [node (z/node zloc)
                           remove?
                           (or (whitespace? node)
                               (uneval? node)
                               (comment? node))]
                       (recur (if remove? (z/remove zloc)
                                  (cz/next zloc)))))))))

(defn attach-branch* [node lang]
  (vary-meta node assoc :branch lang))

(defn attach-branch [node lang splice?]
  (cond-> (attach-branch* node lang)
    splice? (update :children (fn [children]
                                (map #(attach-branch* % lang) children)))))

(defn linter-disabled? [ctx linter]
  (= :off (get-in ctx [:config :linters linter :level])))

(defn location [m]
  (select-keys m [:filename :row :col :end-row :end-col]))

(defn node->line [filename node t message]
  (let [m (meta node)]
    (assoc (location m)
           :type t
           :message message
           :filename filename)))

(defn- lint-unreachable-reader-conditional! [ctx k ts]
  (when (and (= :default (:k k))
             (seq ts)
             (not (linter-disabled? ctx :unreachable-code)))
    (common/reg-finding! ctx (node->line (:filename ctx)
                                         k
                                         :unreachable-code
                                         "Unreachable code: default reader conditional branch should go last"))))

(defn process-reader-conditional [ctx node lang splice?]
  (if (and node
           (= :reader-macro (node/tag node))
           (let [sv (-> node :children first :string-value)]
             (str/starts-with? sv "?")))
    (let [children (-> node :children last :children)]
      (loop [[k v & ts] children
             default nil]
        (when-not (keyword? (:k k))
          (common/reg-finding! ctx (assoc (meta k)
                                          :filename (:filename ctx)
                                          :level :error
                                          :type :syntax
                                          :message "Feature should be a keyword")))
        (lint-unreachable-reader-conditional! ctx k ts)
        (let [kw (:k k)
              default (or default
                          (when (= :default kw)
                            v))]
          (if (= lang kw)
            (attach-branch v lang splice?)
            (if (seq ts)
              (recur ts default)
              default)))))
    node))

(declare select-lang)

(defn select-lang-children [ctx node lang]
  (if-let [children (:children node)]
    (let [new-children (loop [acc []
                              children children]
                         (if-let [node (first children)]
                           (let [splice? (= "?@" (some-> node :children first :string-value))
                                 node-meta (meta node)]
                             (cond
                               (not (:clj-kondo/uneval node-meta))
                               (if-let [processed (select-lang ctx node lang splice?)]
                                 (if splice?
                                   (recur (into acc (:children processed)) (next children))
                                   (recur (conj acc processed) (next children)))
                                 (recur acc (next children)))
                               (contains? (:clj-kondo/uneval node-meta) lang)
                               (recur acc (drop 2 children))
                               :else
                               (recur acc (next children))))
                           acc))]
      (assoc node :children
             new-children))
    node))

(defn select-lang
  ([ctx node lang] (select-lang ctx node lang nil))
  ([ctx node lang splice?]
   (when-let [processed (process-reader-conditional ctx node lang splice?)]
     (select-lang-children ctx processed lang))))

(defn parse-string [s]
  (p/parse-string s))

(defn parse-string-all
  [s]
  (p/parse-string-all s))

(def vconj (fnil conj []))

(defn deep-merge
  "deep merge that also mashes together sequentials"
  ([])
  ([a] a)
  ([a b]
   (cond (when-let [m (meta b)]
           (:replace m)) b
         (and (map? a) (map? b)) (merge-with deep-merge a b)
         (and (or (sequential? a) (set? a))
              (or (sequential? b) (set? b))) (into a b)
         (false? b) b
         :else (or b a)))
  ([a b & more]
   (apply merge-with deep-merge a b more)))

(defn constant?
  "returns true of expr represents a compile time constant"
  [expr]
  (let [t (node/tag expr)]
    (case t
      ;; boolean, single-line string, char, number, keyword
      :token
      (not (symbol? (:value expr)))
      ;; multi-line string and quoted values
      (:multi-line :quote) true
      (:vector :set :map) (every? constant? (:children expr))
      :namespaced-map (every? constant? (-> expr :children first :children))
      false)))

(defn boolean-token? [node]
  (boolean? (:value node)))

(defn char-token? [node]
  (char? (:value node)))

(defn string-from-token [node]
  (when (:lines node)
    (node/sexpr node)))

(defn number-token? [node]
  (number? (:value node)))

(defn symbol-token? [node]
  (symbol? (:value node)))

(defn symbol-from-token [node]
  (when-let [?sym (:value node)]
    (when (symbol? ?sym)
      ?sym)))

(defn map-node-vals [{:keys [children]}]
  (take-nth 2 (rest children)))

(defn map-node-get-value-node
  "Return value node from map node matching given keyword `kw`"
  [{:keys [children]} kw]
  (loop [kvs (partition 2 children)]
    (let [kv (first kvs)]
      (cond
        (nil? kv) nil
        (= kw (node->keyword (first kv))) (second kv)
        :else (recur (rest kvs))))))

(defmacro one-of [x elements]
  `(let [x# ~x]
     (case x# (~@elements) x# nil)))

(defn ctx-with-bindings [ctx bindings]
  (update ctx :bindings (fn [b]
                          (into b bindings))))

(defn kw->sym [^clojure.lang.Keyword k]
  (.sym k))

(defn- reduce-map
  "From medley"
  [f coll]
  (when coll
    (let [coll' (if (record? coll) (into {} coll) coll)
          e (empty coll')]
      (persistent! (reduce-kv (f assoc!) (transient e) coll')))))

(defn map-vals
  "Maps a function over the values of an associative collection. From medley."
  [f coll]
  (reduce-map (fn [xf] (fn [m k v] (xf m k (f v)))) coll))

(defn assoc-some
  "Associates a key with a value in a map, if and only if the value is
  not nil. From medley."
  ([m k v]
   (if (nil? v) m (assoc m k v)))
  ([m k v & kvs]
   (reduce (fn [m [k v]] (assoc-some m k v))
           (assoc-some m k v)
           (partition 2 kvs))))

(defn select-some
  "Like select-keys, but only selects when value is not nil."
  ([m ks]
   (persistent!
    (reduce (fn [acc k]
              (if-some [v (get m k)]
                (assoc! acc k v)
                acc))
            (transient {})
            ks))))

(defn keep-remove [p xs]
  (loop [xs xs
         kept (transient [])
         removed (transient [])]
    (if xs
      (let [x (first xs)] (if-let [v (p x)]
                            (recur (next xs)
                                   (conj! kept v) removed)
                            (recur (next xs) kept (conj! removed x))))
      [(persistent! kept) (persistent! removed)])))

(defn resolve-ns [idacs base-lang lang ns]
  (case [base-lang lang]
    [:clj :clj] (or (get-in idacs [:clj :defs ns])
                    (get-in idacs [:cljc :defs ns]))
    [:cljs :cljs] (or (get-in idacs [:cljs :defs ns])
                      ;; cljs func in another cljc file
                      (get-in idacs [:cljc :defs ns])
                      ;; maybe a macro?
                      (get-in idacs [:clj :defs ns])
                      (get-in idacs [:cljc :defs ns]))
    ;; calling a clojure function from cljc
    [:cljc :clj] (or (get-in idacs [:clj :defs ns])
                     (get-in idacs [:cljc :defs ns]))
    ;; calling function in a CLJS conditional from a CLJC file
    [:cljc :cljs] (or (get-in idacs [:cljs :defs ns])
                      (get-in idacs [:cljc :defs ns])
                      ;; could be a macro
                      (get-in idacs [:clj :defs ns])
                      (get-in idacs [:cljc :defs ns]))))

(defn resolve-call* [idacs call fn-ns fn-name]
  ;; (prn "RES" fn-ns fn-name)
  (let [call-lang (:lang call)
        base-lang (:base-lang call) ;; .cljc, .cljs or .clj file
        unresolved? (:unresolved? call)
        unknown-ns? (identical? fn-ns :clj-kondo/unknown-namespace)
        fn-ns (if unknown-ns? (:ns call) fn-ns)]
    ;; (prn "FN NS" fn-ns fn-name (keys (get (:defs (:clj idacs)) 'clojure.core)))
    (case [base-lang call-lang]
      [:clj :clj] (or (get-in idacs [:clj :defs fn-ns fn-name])
                      (get-in idacs [:cljc :defs fn-ns :clj fn-name]))
      [:cljs :cljs] (or (get-in idacs [:cljs :defs fn-ns fn-name])
                        ;; when calling a function in the same ns, it must be in another file
                        ;; an exception to this would be :refer :all, but this doesn't exist in CLJS
                        (when (not (and unknown-ns? unresolved?))
                          (or
                           ;; cljs func in another cljc file
                           (get-in idacs [:cljc :defs fn-ns :cljs fn-name])
                           ;; maybe a macro?
                           (get-in idacs [:clj :defs fn-ns fn-name])
                           (get-in idacs [:cljc :defs fn-ns :clj fn-name]))))
      ;; calling a clojure function from cljc
      [:cljc :clj] (or (get-in idacs [:clj :defs fn-ns fn-name])
                       (get-in idacs [:cljc :defs fn-ns :clj fn-name]))
      ;; calling function in a CLJS conditional from a CLJC file
      [:cljc :cljs] (or (get-in idacs [:cljs :defs fn-ns fn-name])
                        (get-in idacs [:cljc :defs fn-ns :cljs fn-name])
                        ;; could be a macro
                        (get-in idacs [:clj :defs fn-ns fn-name])
                        (get-in idacs [:cljc :defs fn-ns :clj fn-name])))))

(defn stderr [& msgs]
  (binding [*out* *err*]
    (apply println msgs)))

(defn resolve-call
  ([idacs call call-lang fn-ns fn-name unresolved? refer-alls]
   (resolve-call idacs call call-lang fn-ns fn-name unresolved? refer-alls #{}))
  ([idacs call call-lang fn-ns fn-name unresolved? refer-alls seen]
   (when-let [called-fn
              (or (resolve-call* idacs call fn-ns fn-name)
                  (when unresolved?
                    (some #(resolve-call* idacs call % fn-name)
                          (into (vec
                                 (keep (fn [[ns {:keys [excluded]}]]
                                         (when-not (contains? excluded fn-name)
                                           ns))
                                       refer-alls))
                                (when (not (:clojure-excluded? call))
                                  [(case call-lang #_base-lang
                                         :clj 'clojure.core
                                         :cljs 'cljs.core
                                         :cljc 'clojure.core)])))))]
     (if-let [imported-ns (:imported-ns called-fn)]
       (or
        (let [imported-var (:imported-var called-fn)
              seenv [imported-ns imported-var]]
          (when (not (or
                      (seen seenv)
                      (and (not= fn-ns imported-ns)
                           (not= fn-name imported-var))))
            (resolve-call idacs call call-lang imported-ns imported-var
                          unresolved? refer-alls (conj seen seenv))))
        ;; if we cannot find the imported var here, we fall back on called-fn
        called-fn)
       called-fn))))

(defn handle-ignore [ctx expr]
  (let [cljc? (identical? :cljc (:base-lang ctx))
        lang (:lang ctx)
        m (meta expr)]
    (when-let [ignore (:clj-kondo/ignore m)]
      (let [node (:linters ignore)
            linters (if node
                      (let [node (if cljc?
                                   (select-lang ctx node (:lang ctx))
                                   node)
                            linters (when node (node/sexpr node))]
                        linters)
                      ignore)
            linters (if (or (true? linters)
                            (identical? :all linters))
                      :all (set linters))
            ignore (cond-> (assoc m :ignore linters)
                     cljc? (assoc :cljc true)
                     node (assoc-in [:clj-kondo/ignore :linters] nil))]
        (swap! (:ignores ctx) update-in [(:filename ctx) lang]
               (fn [ignores]
                 (let [id (:clj-kondo/ignore-id ignore)]
                   (if (and id (some #(= id (:clj-kondo/ignore-id %)) ignores))
                     ignores
                     (vconj ignores ignore)))))))))

(defn err [& xs]
  (binding [*out* *err*]
    (apply prn xs)))

;;; os specific

(def windows? (-> (System/getProperty "os.name")
                  (str/lower-case)
                  (str/includes? "win")))

(defn unixify-path
  "Convert dir separators in `s`, when on Windows, to forward slashes.
   Using forward slashes in paths make paths platform agnostic as Java does understand / as a path separator on Windows.
   Note that you'll only want to call this function on relative paths."
  [s]
  (if windows?
    (str/replace s "\\" "/")
    s))

(defn export-ns-sym [sym]
  (let [m (meta sym)
        raw (:raw-name m)]
    (if (and raw (string? raw))
      raw sym)))

(def ^:dynamic *ctx* nil)

(defn log [& xs]
  (.println System/err (str/join " " xs)))

(defn where-am-i
  ([] (where-am-i 10))
  ([depth]
   (let [ks [:fileName :lineNumber :className]]
     (pprint/print-table
      ks
      (map (comp #(select-keys % ks) bean)
           (take depth (.getStackTrace (Thread/currentThread))))))))

(defn ->uri [jar entry file]
  (cond file (when (fs/exists? file)
               (str (.toURI (fs/file file))))
        (and jar entry)
        (str "jar:" (.toURI (io/file jar)) "!/" entry)))

(defn file-ext [fn]
  (when-let [last-dot (str/last-index-of fn ".")]
    (subs fn (inc last-dot))))

(defn strip-file-ext [fn]
  (if-let [last-dot (str/last-index-of fn ".")]
    (subs fn 0 last-dot)
    fn))

(defn ctx-with-linter-disabled [ctx linter]
  (assoc-in ctx [:config :linters linter :level] :off))

(defn ctx-with-linters-disabled [ctx linters]
  (let [config (get ctx :config)
        linters-config (get config :linters)
        linters-config (reduce (fn [linters linter]
                                 (assoc-in linters [linter :level] :off))
                               linters-config linters)
        config (assoc config :linters linters-config)
        ctx (assoc ctx :config config)]
    ctx))

(defn reg-call [ctx call id]
  (when id
    (swap! (:calls-by-id ctx) assoc id call))
  nil)

(defn update-vals
  "m f => {k (f v) ...}

  Given a map m and a function f of 1-argument, returns a new map where the keys of m
  are mapped to result of applying f to the corresponding values of m."
  {:added "1.11"}
  [m f]
  (with-meta
    (persistent!
     (reduce-kv (fn [acc k v] (assoc! acc k (f v)))
                (if (instance? clojure.lang.IEditableCollection m)
                  (transient m)
                  (transient {}))
                m))
    (meta m)))

(defn mark-generate [node]
  (assoc node :clj-kondo.impl/generated true))

(defn format-callstack [ctx]
  (mapv (fn [[ns var]]
          {:ns ns
           :name var})
        (:callstack ctx)))

(defn ignored? [expr linter]
  (when-let [{:keys [linters] :as ignore} (:clj-kondo/ignore (meta expr))]
    (or (identical? :all linters)
        (some #(identical? linter %) ignore)
        (some #(identical? linter (:k %)) (:children linters)))))

;;;; Scratch

(comment
  (false? (node/sexpr (parse-string "false")))
  (false? (node/sexpr (parse-string "nil")))
  (constant? (parse-string "foo"))
  (map-node-vals (parse-string "{:a 1 :b 2}"))
  (assoc-some {} :a 1 :b nil :c false)
  (select-some {:a 1 :b nil :c 2 :d false} [:a :b :d]) ;; => {:a 1, :d false}
  (tag (parse-string "\"x

y\""))
  (tag (parse-string "\"xy\""))
  (map-node-get-value-node (p/parse-string "{:binky 2 :arglists #_ :ha '([a b c]) :boingo 4}")
                           :arglists))
