(ns clj-kondo.impl.utils
  {:no-doc true}
  (:require
   [rewrite-clj.node.protocols :as node]
   [rewrite-clj.node.whitespace :refer [whitespace?]]
   [rewrite-clj.parser :as p]
   [clj-kondo.impl.profiler :as profiler]))

(defn tag [maybe-expr]
  (when maybe-expr
    (node/tag maybe-expr)))

(defn uneval? [node]
  (when (= :uneval (tag node)))
  (= :uneval (tag node)))

(defn comment? [node]
  (= :comment (tag node)))

(defn symbol-call
  "Returns symbol of call"
  [expr]
  (when (= :list (node/tag expr))
    (let [first-child (-> expr :children first)
          ?sym (:value first-child)]
      (when (symbol? ?sym)
        ?sym))))

(defn keyword-call
  "Returns keyword of call"
  [expr]
  (when (= :list (node/tag expr))
    (let [first-child (-> expr :children first)
          ?k (:k first-child)]
      (when (keyword? ?k)
        {:k ?k
         :namespaced? (:namespaced? first-child)}))))

(defmacro some-call
  "Determines if expr is a call to some symbol. Returns symbol if so."
  [expr & syms]
  (let [syms (set syms)]
    `(and (= :list (tag ~expr))
          ((quote ~syms) (:value (first (:children ~expr)))))))

(declare remove-noise*)

(defn remove-noise-children [expr]
  (if-let [children (:children expr)]
    (let [new-children (doall (keep remove-noise* children))]
      (assoc expr :children new-children))
    expr))

(defn remove-noise* [node]
  (when-not (or (whitespace? node)
                (uneval? node)
                (comment? node))
    (remove-noise-children node)))

(defn remove-noise [expr]
  (profiler/profile :remove-noise
                    (remove-noise* expr)))

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

(defn process-reader-conditional [node lang]
  (if (= :reader-macro (and node (node/tag node)))
    (let [tokens (-> node :children last :children)]
      (loop [[k v & ts] tokens]
        (if (= lang (:k k))
          v
          (when (seq ts) (recur ts)))))
    node))

(declare select-lang*)

(defn select-lang-children [node lang]
  (if-let [children (:children node)]
    (assoc node :children
           (reduce
            (fn [acc node]
              (if-let [processed (select-lang* node lang)]
                (if (= "?@" (-> node :children first :string-value))
                  (into acc (:children  processed))
                  (conj acc processed))
                acc))
            []
            children))
    node))

(defn select-lang* [node lang]
  (when-let [processed (process-reader-conditional node lang)]
    (select-lang-children processed lang)))

(defn select-lang [expr lang]
  (profiler/profile :select-lang
                    (select-lang* expr lang)))

(defn node->line [filename node level type message]
  (let [m (meta node)]
    {:type type
     :message message
     :level level
     :row (:row m)
     :col (:col m)
     :filename filename}))

(defn parse-string [s]
  (remove-noise (p/parse-string s)))

(defn parse-string-all
  [s]
  (let [p (profiler/profile :rewrite-clj-parse-string-all
                            (p/parse-string-all s))]
    (profiler/profile :remove-noise (remove-noise p))))

(def vconj (fnil conj []))

(defn deep-merge
  "deep merge that also mashes together sequentials"
  ([])
  ([a] a)
  ([a b]
   (cond (and (map? a) (map? b))
         (merge-with deep-merge a b)
         (and (sequential? a) (sequential? b))
         (into a b)
         (and (set? a) (set? b))
         (into a b)
         :else (or b a)))
  ([a b & more]
   (apply merge-with deep-merge a b more)))

#_(defn- constant-val?
  [x]
  (c/or (nil? x)
        (boolean? x)
        (number? x)
        (string? x)
        (ident? x)
        (char? x)
        (c/and (coll? x) (empty? x))
        (c/and (c/or (vector? x) (set? x) (map? x))
               (every? constant-val? x))))

(defn- constant-val?
  [v]
  (or (boolean? v)
      (string? v)
      (char? v)
      (number? v)
      (keyword? v)
      (and (list? v) (= 'quote (first v)))
      (and (or (vector? v) (set? v) (map? v))
           (every? constant-val? v))))

(defn constant?
  "returns true of expr represents a compile time constant"
  [expr]
  (let [v (node/sexpr expr)]
    (constant-val? v)))

(defn boolean-token? [node]
  (boolean? (:value node)))

(defn char-token? [node]
  (char? (:value node)))

(defn string-token? [node]
  (boolean (:lines node)))

(defn number-token? [node]
  (number? (:value node)))

(defn symbol-token? [node]
  (symbol? (:value node)))

;;;; Scratch

(comment
  (meta (lift-meta (parse-string "^:private [x]")))
  (false? (node/sexpr (parse-string "false")))
  (false? (node/sexpr (parse-string "nil")))
  (constant? (parse-string "foo"))
  )
