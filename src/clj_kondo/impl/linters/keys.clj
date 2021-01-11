(ns clj-kondo.impl.linters.keys
  {:no-doc true}
  (:require
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :refer [node->line tag]]
   [clojure.string :as string]))

(defn key-value
  "We only support the following cases for now."
  [node]
  (case (tag node)
    :token (or (when-let [v (:k node)]
                 (if (:namespaced? node)
                   (str v) v))
               (str node))
    :vector (map key-value (:children node))
    :list (map key-value (:children node))
    :set (set (map key-value (:children node)))
    :map (apply hash-map (map key-value (:children node)))
    :quote (recur (first (:children node)))
    nil))

(defn- stringify-key-expr
  [node]
  (case (tag node)
    :vector (->> (:children node)
                 (map stringify-key-expr)
                 (string/join " ")
                 (format "[%s]"))
    :list (->> (:children node)
               (map stringify-key-expr)
               (string/join " ")
               (format "(%s)"))
    :set (->> (:children node)
              (map stringify-key-expr)
              (string/join " ")
              (format "#{%s}"))
    :map (->> (:children node)
              (map stringify-key-expr)
              (string/join " ")
              (format "{%s}"))
    :quote (str (:prefix node)
                (reduce str (map stringify-key-expr
                                 (:children node))))
    (str node)))

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
                           (str "duplicate key " (stringify-key-expr key-expr)))))
            (when-not (known-key? k)
              (findings/reg-finding!
               ctx
               (node->line filename
                           key-expr :error :syntax
                           (str "unknown option " (stringify-key-expr key-expr)))))
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
