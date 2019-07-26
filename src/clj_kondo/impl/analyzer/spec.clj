(ns clj-kondo.impl.analyzer.spec
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :as utils]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.linters.keys :as keys]))

(defn analyze-fdef [{:keys [:analyze-children :ns] :as ctx} expr]
  (let [[sym-expr & body] (next (:children expr))
        ns-name (-> ns :name)]
    (keys/lint-map-keys ctx {:children body} {:known-key? #{:args :ret :fn}})
    (let [sym (:value sym-expr)]
      (if-not (and sym (symbol? sym))
        (findings/reg-finding! (:findings ctx)
                               (utils/node->line (:filename ctx)
                                                 sym-expr
                                                 :error
                                                 :syntax
                                                 "expected symbol"))
        (let [{resolved-ns :ns}
              (namespace/resolve-name ctx ns-name
                                      sym)]
          (if resolved-ns
            (namespace/reg-used-namespace! ctx ns-name resolved-ns)
            (findings/reg-finding! (:findings ctx)
                                   (utils/node->line (:filename ctx)
                                                     sym-expr
                                                     :error
                                                     :unresolved-symbol
                                                     (str "unresolved symbol " sym)))))))
    (analyze-children ctx body)))

;;;; Scratch
(require '[clj-kondo.impl.parser])

(comment
  (:lines (first (:children (clj-kondo.impl.parser/parse-string "\"foo\"")))))
