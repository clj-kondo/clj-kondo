(ns clj-kondo.impl.linters.misc
  {:no-doc true}
  (:require
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :refer [node->line]]))

(defn lint-duplicate-requires!
  ([ctx namespaces] (lint-duplicate-requires! ctx #{} namespaces))
  ([ctx init namespaces]
   (reduce (fn [required ns]
             (if (contains? required ns)
               (let [ns (if (symbol? ns) ns (second ns))]
                 (findings/reg-finding!
                   (:findings ctx)
                   (node->line (:filename ctx)
                               ns
                               :warning
                               :duplicate-require
                               (str "duplicate require of " ns)))
                 required)
               (conj required ns)))
           (set init)
           namespaces)
   nil))
