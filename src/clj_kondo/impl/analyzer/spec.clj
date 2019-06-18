(ns clj-kondo.impl.analyzer.spec
  (:require
   [clj-kondo.impl.utils :as utils]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.linters.keys :as keys]
   [rewrite-clj.node.protocols :as node]))

(defn analyze-fdef [{:keys [:analyze-children] :as ctx} expr]
  (let [[sym & body] (next (:children expr))]
    (keys/lint-map-keys ctx {:children body} {:known-key? #{:args :ret :fn}})
    (when-not (utils/symbol-token? sym)
      (findings/reg-finding! (:findings ctx)
                             (utils/node->line (:filename ctx)
                                               sym
                                               :error
                                               :syntax
                                               "expected symbol")))
    (analyze-children ctx body)))

;;;; Scratch

(comment
  (:lines (first (:children (clj-kondo.impl.parser/parse-string "\"foo\"")))))
