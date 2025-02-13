(ns hooks
  (:require [clj-kondo.hooks-api :as api]))

(defn lint-with-redefs [{:keys [name ns]} expr]
  (when (and (= 'clojure.core ns )
             (= 'with-redefs name))
    (api/reg-finding! (assoc (meta expr)
                             :type :discouraged-var
                             :message "Don't use with-redefs"))))

(defn lint-unresolved [v expr]
  (when-not v
    (api/reg-finding! (assoc (meta expr)
                             :type :syntax
                             :message "Unresolved"))))

(defn my-test [{:keys [node]}]
  (let [[core-wrd foo-wrd
         inc-node
         dude-node] (rest (:children node))]
    (lint-with-redefs (api/resolve {:name (api/sexpr (first (:children core-wrd)))}) core-wrd)
    (lint-with-redefs (api/resolve {:name (api/sexpr (first (:children foo-wrd)))}) foo-wrd)
    (lint-unresolved (api/resolve {:name (api/sexpr (first (:children inc-node)))}) inc-node)
    (lint-unresolved (api/resolve {:name (api/sexpr (first (:children dude-node)))}) dude-node)))
