(ns clj-kondo.tools.ast
  (:require [clj-kondo.core :as clj-kondo]
            [clojure.pprint :as pp]
            [clojure.walk :as walk]
            [rewrite-clj.node :as node]
            [rewrite-clj.parser :as parser]))

(defn locs [analysis k]
  (->> analysis
       k
       (map (fn [m]
              [(or (:name-row m) (:row m))
               (or (:name-col m) (:col m))])) set))

(defn node->map [node]
  (let [children (:children node)
        children (when children (remove #(#{:whitespace
                                            :newline
                                            :comment} (node/tag %)) children))]
    (cond-> {}
      children (assoc :children children))))

(defn ast [{:keys [file]}]
  (let [nodes (parser/parse-string-all (slurp file))
        analysis (:analysis (clj-kondo/run! {:lint [file] :config {:analysis {:locals true}}}))
        local-locs (locs analysis :locals)
        local-usages-locs (locs analysis :local-usages)
        var-locs (locs analysis :var-definitions)
        var-usages (locs analysis :var-usages)]
    (walk/prewalk
     (fn [node]
       (when node
         (let [tag (node/tag node)]
           (case tag
             :token
             (if (symbol? (:value node))
               (let [local (contains? local-locs ((juxt :row :col) (meta node)))
                     local-usage (contains? local-usages-locs ((juxt :row :col) (meta node)))
                     var (contains? var-locs ((juxt :row :col) (meta node)))
                     var-usage (contains? var-usages ((juxt :row :col) (meta node)))]
                 (cond-> (assoc (node->map node)
                                :sexpr (node/sexpr node))
                   local (assoc :local true)
                   local-usage (assoc :local-usage true)
                   var (assoc :var true)
                   var-usage (assoc :var-usage true)))
               (assoc (node->map node) :sexpr (node/sexpr node)))
             :unknown node
             (node->map node)))))
     nodes)))

(defn -main [& [file]]
  (pp/pprint (ast {:file file})))

;;;; Scratch

(comment
  (ast {:file *file*}))
