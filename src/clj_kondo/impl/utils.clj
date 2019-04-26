(ns clj-kondo.impl.utils
  {:no-doc true}
  (:require
   [clojure.walk :refer [prewalk]]
   [rewrite-clj.node.protocols :as node]
   [rewrite-clj.node.whitespace :refer [whitespace?]]
   [rewrite-clj.parser :as p]))

(defn tag [maybe-expr]
  (when maybe-expr
    (node/tag maybe-expr)))

(defn uneval? [node]
  (when (= :uneval (tag node)))
  (= :uneval (tag node)))

(defn comment? [node]
  (= :comment (tag node)))

(defn call
  "Returns symbol of call"
  [expr]
  (when (= :list (node/tag expr))
    (let [?sym (-> expr :children first :value)]
      (when (symbol? ?sym)
        ?sym))))

(defmacro some-call
  "Determines if expr is a call to some symbol. Returns symbol if so."
  [expr & syms]
  (let [syms (set syms)]
    `(and (= :list (tag ~expr))
          ((quote ~syms) (:value (first (:children ~expr)))))))

(defn update* [m k f]
  (if-let [v (get m k)]
    (if-let [v* (f v)]
      (assoc m k v*)
      m)
    m))

(defn remove-noise
  ([expr] (remove-noise expr nil))
  ([expr config]
   (clojure.walk/postwalk
    #(update* %
              :children
              (fn [children]
                (keep
                 (fn [node]
                   (when-not
                       (or #_(whitespace? node)
                           (uneval? node)
                           (comment? node))
                     node))
                 children)))
    expr)))

(defn process-reader-conditional [node lang]
  ;; TODO: support :default
  (let [tokens (-> node :children last :children)]
    (loop [[k v & ts] tokens]
      (if (= lang (:k k))
        v
        (when (seq ts) (recur ts))))))

(defn select-lang
  ([expr lang]
   (clojure.walk/postwalk
    #(update* %
              :children
              (fn [children]
                (seq
                 (reduce
                  (fn [acc node]
                    (if (= :reader-macro
                           (and node (node/tag node)))
                      (if-let [processed (process-reader-conditional node lang)]
                        (if (= "?@" (-> node :children first :string-value))
                          (into acc (:children  processed))
                          (conj acc processed))
                        acc)
                      (conj acc node)))
                  []
                  children))))
    expr)))

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
  ([s] (parse-string-all s nil))
  ([s config]
   (let [p (p/parse-string-all s)]
     (remove-noise p config))))

(defn filter-children
  "Recursively filters children by pred"
  [pred children]
  (mapcat #(if (pred %)
             [%]
             (if-let [cchildren (:children %)]
               (filter-children pred cchildren)
               []))
          children))

;;;; Scratch

(comment
  (meta (lift-meta (parse-string "^:private [x]")))
  )
