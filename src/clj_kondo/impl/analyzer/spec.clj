(ns clj-kondo.impl.analyzer.spec
  {:no-doc true}
  (:require
   [clj-kondo.impl.analyzer.common :as common]
   [clj-kondo.impl.analyzer.usages :as usages]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.linters.keys :as keys]
   [clj-kondo.impl.namespace :as namespace]
   [clj-kondo.impl.utils :as utils]))

(defn- redefined-spec-enabled?
  "The :redefined-spec linter reports duplicates project-wide from a
  post-analysis pass, so we record registrations whenever the linter isn't
  globally off, regardless of namespace-local config."
  [ctx]
  (not (identical? :off (get-in ctx [:global-config :linters :redefined-spec :level]))))

(defn reg-spec-def!
  "Records a spec registration for the :redefined-spec linter. `tp` is :def or
  :fdef, `name-node` is the registered name node and `resolved-ns`/`resolved-name`
  its fully-resolved identity."
  [ctx tp name-node resolved-ns resolved-name]
  (when (and resolved-ns resolved-name (redefined-spec-enabled? ctx))
    (namespace/reg-spec-def! ctx (-> ctx :ns :name)
                             (assoc (utils/location (meta name-node))
                                    :filename (:filename ctx)
                                    :type tp
                                    :ns resolved-ns
                                    :name resolved-name))))

(defn analyze-fdef [{:keys [analyze-children ns] :as ctx} expr]
  (let [[sym-expr & body] (next (:children expr))
        ns-nm (-> ns :name)]
    (keys/lint-map-keys ctx {:children body} {:known-key? #{:args :ret :fn}})
    (let [sym (:value sym-expr)]
      (if-not (and sym (symbol? sym))
        (findings/reg-finding! ctx
                               (utils/node->line (:filename ctx)
                                                 sym-expr
                                                 :syntax
                                                 "expected symbol"))
        (let [{resolved-ns :ns resolved-name :name unresolved? :unresolved?}
              (namespace/resolve-name ctx true ns-nm
                                      sym nil)]
          (when resolved-ns
            (namespace/reg-used-namespace! ctx ns-nm resolved-ns)
            ;; an unqualified fdef target registers under the current namespace,
            ;; regardless of whether its var has been analyzed yet, so key it
            ;; there for consistent cross-file detection
            (if (and unresolved? (not (namespace sym)))
              (reg-spec-def! ctx :fdef sym-expr ns-nm resolved-name)
              (reg-spec-def! ctx :fdef sym-expr resolved-ns resolved-name))
            ;; revisit this when needed
            #_(findings/reg-finding! ctx
                                     (utils/node->line (:filename ctx)
                                                       sym-expr
                                                       :unresolved-symbol
                                                       (str "Unresolved symbol: " sym)))))))
    (analyze-children ctx body)))

(defn analyze-def [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        reg-val (if (:k name-expr)
                  (assoc name-expr :reg fq-def)
                  name-expr)]
    (when (and (:k name-expr) (redefined-spec-enabled? ctx))
      (let [{:keys [ns name]} (usages/resolve-keyword ctx name-expr (-> ctx :ns :name))]
        (reg-spec-def! ctx :def name-expr ns name)))
    (common/analyze-expression** (utils/ctx-with-linter-disabled ctx :unresolved-symbol) reg-val)
    (common/analyze-children ctx body)))

(defn analyze-keys [ctx expr]
  (let [body (next (:children expr))]
    (keys/lint-map-keys ctx {:children body} {:known-key? #{:req :opt :req-un :opt-un :gen}})
    (common/analyze-children ctx body)))

;;;; Scratch
(require '[clj-kondo.impl.parser])

(comment
  (:lines (first (:children (clj-kondo.impl.parser/parse-string "\"foo\"")))))
