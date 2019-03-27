(ns clj-kondo.impl.linters
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [call? node->line parse-string
                                 parse-string-all remove-noise]]
   [clj-kondo.impl.vars :refer [analyze-arities]]
   [clojure.string :as str]
   [clojure.walk :refer [prewalk]]
   [rewrite-clj.node.protocols :as node :refer [tag]]
   [rewrite-clj.node.seq :refer [vector-node list-node]]
   [rewrite-clj.node.token :refer [token-node]]
   [rewrite-clj.node.fn :refer [fn-node]]
   [rewrite-clj.parser :as p]))

(set! *warn-on-reflection* true)

;;;; inline def

(defn inline-def* [rw-expr in-def?]
  (let [current-def? (call? rw-expr 'def 'defn 'defn- 'deftest 'defmacro)
        new-in-def? (and (not (contains? '#{:syntax-quote :quote}
                                         (:tag rw-expr)))
                         (or in-def? current-def?))]
    (if (and in-def? current-def?)
      [rw-expr]
      (when (:children rw-expr)
        (mapcat #(inline-def* % new-in-def?) (:children rw-expr))))))

(defn inline-def [filename parsed-expressions]
  (map #(node->line filename % :warning :inline-def "inline def")
       (inline-def* parsed-expressions false)))

;;;; obsolete let

(defn obsolete-let* [{:keys [:children] :as rw-expr}
                     parent-let?]
  (let [current-let? (call? rw-expr 'let)]
    (cond (and current-let? parent-let?)
          [rw-expr]
          current-let?
          (let [;; skip let keywords and bindings
                children (nnext children)]
            (concat (obsolete-let* (first children) current-let?)
                    (mapcat #(obsolete-let* % false) (rest children))))
          :else (mapcat #(obsolete-let* % false) children))))

(defn obsolete-let [filename parsed-expressions]
  (map #(node->line filename % :warning :nested-let "obsolete let")
       (obsolete-let* parsed-expressions false)))

;;;; obsolete do

(defn obsolete-do* [{:keys [:children] :as rw-expr}
                    parent-do?]
  (let [implicit-do? (call? rw-expr 'fn 'defn 'defn-
                            'let 'loop 'binding 'with-open
                            'doseq 'try)
        current-do? (call? rw-expr 'do)]
    (cond (and current-do? (or parent-do?
                               (and (not= :unquote-splicing
                                          (:tag (second children)))
                                    (<= (count children) 2))))
          [rw-expr]
          :else (mapcat #(obsolete-do* % (or implicit-do? current-do?)) children))))

(defn obsolete-do [filename parsed-expressions]
  (map #(node->line filename % :warning :obsolete-do "obsolete do")
       (obsolete-do* parsed-expressions false)))

;;;; macro expand

(defn expand-> [{:keys [:children] :as expr}]
  (let [children (rest children)]
    (loop [[child1 child2 & children :as all-children] children]
      (if child2
        (if (= :list (node/tag child2))
          (recur
           (let [res (into
                      [(with-meta
                         (list-node (reduce into
                                            [[(first (:children child2))]
                                             [child1] (rest (:children child2))]))
                         (meta child2))] children)]
             res))
          (recur (into [(with-meta (list-node [child2 child1])
                          (meta child2))] children)))
        child1))))

(defn find-fn-args [children]
  (mapcat #(cond (and (= :token (tag %))
                      (re-matches #"%\d?\d?" (:string-value %)))
                 [%]
                 (:children %)
                 (find-fn-args (:children %))
                 :else [])
          children))

(defn expand-fn [{:keys [:children] :as expr}]
  (let [fn-body (list-node children)
        args (find-fn-args children)
        arg-list (vector-node args)]
    (list-node [(token-node 'fn) arg-list fn-body])))

(defn expand-expressions [expr]
  (clojure.walk/prewalk
   #(if (:children %)
      (assoc % :children
             (map (fn [n]
                    (cond (call? n '->)
                      (expand-> n)
                      (= :fn (node/tag n))
                      (expand-fn n)
                      :else n))
                  (:children %)))
      %)
   expr))

;;;; processing of string input

(defn process-input
  [input filename language]
  (let [;; workaround for https://github.com/xsc/rewrite-clj/issues/75
        input (-> input
                  (str/replace "##Inf" "::Inf")
                  (str/replace "##-Inf" "::-Inf")
                  (str/replace "##NaN" "::NaN")
                  ;; workaround for https://github.com/borkdude/clj-kondo/issues/11
                  (str/replace #_"#:a{#::a {:a b}}"
                               #"#(::?)(.*?)\{" (fn [[_ colons name]]
                                                  (str colons name "{"))))
        parsed-expressions (parse-string-all input)
        parsed-expressions (expand-expressions parsed-expressions)
        ids (inline-def filename parsed-expressions)
        nls (obsolete-let filename parsed-expressions)
        ods (obsolete-do filename parsed-expressions)
        {:keys [:calls :defns]} (analyze-arities filename language parsed-expressions)]
    {:findings (concat ids nls ods) #_(add-filename filename (concat ids nls ods))
     :calls calls
     :defns defns
     :lang language}))

;;;; scratch

(comment
  ;; TODO: fix/optimize cache format
  ;; TODO: clean up code
  ;; TODO: distribute binaries
  )
