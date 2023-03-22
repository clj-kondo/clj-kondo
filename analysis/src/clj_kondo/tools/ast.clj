(ns clj-kondo.tools.ast
  (:require [clj-kondo.core :as clj-kondo]
            [clojure.walk :as walk]
            [rewrite-clj.node :as node]
            [rewrite-clj.parser :as parser]))

(defn foo [x] x)

(def file *file*)

(defn locals [analysis]
  (->> analysis
       ((juxt :locals :local-usages))
       (apply concat)
       (map (juxt :row :col)) set))

(defn node->map [node]
  (let [children (:children node)
        children (when children (remove #(#{:whitespace
                                            :newline} (node/tag %)) children))]
    (cond-> {:tag (node/tag node)}
      children (assoc :children children))))

(defn ast [_]
  (let [nodes (parser/parse-string-all (slurp file))
        analysis (:analysis (clj-kondo/run! {:lint [file] :config {:analysis {:locals true}}}))
        local-locs (locals analysis)]
    (walk/prewalk (fn [node]
                    (when node
                      (let [tag (node/tag node)]
                        (case tag
                          :token
                          (if (and (simple-symbol? (:value node))
                                   (contains? local-locs ((juxt :row :col) (meta node))))
                            (assoc (node->map node)
                                   :local true
                                   :sexpr (node/sexpr node))
                            (assoc (node->map node) :sexpr (node/sexpr node)))
                          :unknown node
                          (node->map node)))))
                  nodes)))

(defn -main [& _args]
  (ast {}))

;;;; Scratch

(comment
  (ast {})
  (filter #(:local (meta %)) (tree-seq :children :children (ast {}))))
