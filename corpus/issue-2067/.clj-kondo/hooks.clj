(ns hooks
  (:require [clj-kondo.hooks-api :as api]))

(defn lint-with-redefs [{:keys [name ns]} expr]
  (when (and (= ns 'clojure.core)
             (= name 'with-redefs))
    (api/reg-finding! (assoc (meta expr)
                             :type :discouraged-var
                             :message "Don't use with-redefs"))))

(defn my-test [{:keys [node]}]
  (let [[core-wrd foo-wrd] (rest (:children node))]
    (lint-with-redefs (api/resolve {:name (api/sexpr (first (:children core-wrd)))}) core-wrd)
    (lint-with-redefs (api/resolve {:name (api/sexpr (first (:children foo-wrd)))}) foo-wrd)))

(defmacro my-test-macro [x]
  (api/reg-finding! (assoc (meta x)
                           :type :discouraged-var
                           :message (str x))))
