(ns clj-kondo.impl.utils
  {:no-doc true}
  (:require
   [clj-kondo.impl.profiler :as profiler]
   [clj-kondo.impl.rewrite-clj.node.protocols :as node]
   [clj-kondo.impl.rewrite-clj.node.seq :as seq]
   [clj-kondo.impl.rewrite-clj.node.token :as token]
   [clj-kondo.impl.rewrite-clj.parser :as p]))

;;; export rewrite-clj functions

(defn tag [expr]
  (node/tag expr))

(defn sexpr [expr]
  (node/sexpr expr))

(def vector-node seq/vector-node)
(def list-node seq/list-node)
(def token-node token/token-node)

;;; end export

(defn print-err! [& strs]
  (binding [*out* *err*]
    (apply println strs))
  nil)

(defn uneval? [node]
  (= :uneval (node/tag node)))

(defn comment? [node]
  (= :comment (node/tag node)))

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
      (loop [[k v & ts] tokens
             default nil]
        (let [kw (:k k)
              default (or default
                          (when (= :default kw)
                            v))]
          (if (= lang kw)
            v
            (if (seq ts)
              (recur ts default)
              default)))))
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
   (cond (and (map? a) (map? b))
         (merge-with deep-merge a b)
         (and (sequential? a) (sequential? b))
         (into a b)
         (and (set? a) (set? b))
         (into a b)
         (false? b) b
         :else (or b a)))
  ([a b & more]
   (apply merge-with deep-merge a b more)))

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

(defn symbol-from-token [node]
  (when-let [?sym (:value node)]
    (when (symbol? ?sym)
      ?sym)))

(defn map-node-vals [{:keys [:children]}]
  (take-nth 2 (rest children)))

(defmacro one-of [x elements]
  `(let [x# ~x]
     (case x# (~@elements) x# nil)))

(defn linter-disabled? [ctx linter]
  (= :off (get-in ctx [:config :linters linter :level])))

(defn kw->sym [^clojure.lang.Keyword k]
  (.sym k))

(def encoder (java.util.Base64/getEncoder))
(def decoder (java.util.Base64/getDecoder))

(defn encode-filename*
  [^String filename]
  (->> filename
       .getBytes
       (.encodeToString ^java.util.Base64$Encoder encoder)
       keyword))

(def encode-filename (memoize encode-filename*))

;;;; Scratch

(comment
  (false? (node/sexpr (parse-string "false")))
  (false? (node/sexpr (parse-string "nil")))
  (constant? (parse-string "foo"))
  (map-node-vals (parse-string "{:a 1 :b 2}"))
  )
