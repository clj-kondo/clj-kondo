(ns clj-kondo.impl.linters.keys
  {:no-doc true}
  (:require [rewrite-clj.node.protocols :as node]
            [clj-kondo.impl.state :as state]
            [clj-kondo.impl.utils :refer [node->line]]))

(defn key-value
  "We only support tokens as key values for now."
  [node]
  (case (node/tag node)
    :token (node/string node)
    nil))

(defn lint-map-keys [ctx expr]
  (let [children (:children expr)]
    (reduce
     (fn [{:keys [:seen] :as acc} key-expr]
       (if-let [k (key-value key-expr)]
         (if (contains? seen k)
           (do
             (state/reg-finding! (node->line (:filename ctx)
                                             key-expr :error :duplicate-map-key
                                             (str "duplicate key " k)))
             (update acc :seen conj k))
           (update acc :seen conj k))
         acc))
     {:seen #{}
      :findings []}
     (take-nth 2 children))
    (when (odd? (count children))
      (let [last-child (last children)]
        (state/reg-finding!
         (node->line (:filename ctx) last-child :error :missing-map-value
                     (str "missing value for key " (key-value last-child))))))))

;;;; end map linter

;;;; set linter

(defn lint-set [ctx expr]
  (let [children (:children expr)]
    (reduce
     (fn [{:keys [:seen] :as acc} set-element]
       (if-let [k (key-value set-element)]
         (do (when (contains? seen k)
               (state/reg-finding!
                (node->line (:filename ctx) set-element :error :duplicate-set-key
                            (str "duplicate set element " k))))
            (update acc :seen conj k))
         acc))
     {:seen #{}
      :findings []}
     children)))

;;;; end set linter


