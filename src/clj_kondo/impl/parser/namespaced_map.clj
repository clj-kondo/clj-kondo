(ns clj-kondo.impl.parser.namespaced-map
  (:require
   [rewrite-clj.reader :as reader]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [clj-kondo.impl.node.seq :refer [namespaced-map-node]]))

(defn parse-map-ns
  ;; parse map namespace inside reader tag
  [reader]
  (reader/ignore reader)
  (let [colons (reader/read-while reader (fn [c]
                                           (= \: c)))
        aliased? (= ":" colons)
        s (str/trim (reader/read-until reader
                                       (fn [c]
                                         (= \{ c))))
        k (if (= "" s)
            :__current-ns__
            (keyword s))]
    (node/keyword-node k aliased?)))

(defn parse-namespaced-map
  [reader read-next]
  (let [map-ns (parse-map-ns reader)
        aliased? (:namespaced? map-ns)]
    (namespaced-map-node
     [map-ns (read-next reader)]
     aliased?)))
