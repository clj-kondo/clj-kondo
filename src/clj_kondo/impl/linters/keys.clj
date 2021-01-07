(ns clj-kondo.impl.linters.keys
  {:no-doc true}
  (:require
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :refer [node->line tag]]))

(defn key-value
  "We only support tokens as key values for now."
  [node]
  (case (tag node)
    :token (or (when-let [v (:k node)]
                 (if (:namespaced? node)
                   (str v) v))
               (str node))
    :vector (map key-value (:children node))
    :list (map key-value (:children node))
    nil))

(defn lint-map-keys
  ([ctx expr]
   (lint-map-keys ctx expr nil))
  ([ctx expr {:keys [:known-key?] :or {known-key? (constantly true)}}]
   (let [filename (:filename ctx)
         t (tag expr)
         children (if (= :namespaced-map t)
                    (-> expr :children first :children)
                    (:children expr))]
     (reduce
      (fn [{:keys [:seen] :as acc} key-expr]
        (if-let [k (key-value key-expr)]
          (do
            (when (contains? seen k)
              (findings/reg-finding!
               ctx
               (node->line filename
                           key-expr :error :duplicate-map-key
                           (str "duplicate key " key-expr))))
            (when-not (known-key? k)
              (findings/reg-finding!
               ctx
               (node->line filename
                           key-expr :error :syntax
                           (str "unknown option " key-expr))))
            (update acc :seen conj k))
          acc))
      {:seen #{}}
      (take-nth 2 children))
     (when (odd? (count children))
       (let [last-child (last children)]
         (findings/reg-finding!
          ctx
          (node->line filename last-child :error :missing-map-value
                      (str "missing value for key " last-child))))))))

;;;; end map linter

;;;; set linter

(defn lint-set [ctx expr]
  (let [children (:children expr)]
    (reduce
     (fn [{:keys [:seen] :as acc} set-element]
       (if-let [k (key-value set-element)]
         (do (when (contains? seen k)
               (findings/reg-finding!
                ctx
                (node->line (:filename ctx) set-element :error :duplicate-set-key
                            (str "duplicate set element " k))))
             (update acc :seen conj k))
         acc))
     {:seen #{}}
     children)))

;;;; end set linter
