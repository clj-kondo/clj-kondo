(ns clj-kondo.impl.analysis
  "Helpers for analysis output"
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require [clj-kondo.impl.utils :refer [assoc-some select-some]]))

(defn reg-usage! [ctx filename row col from-ns to-ns var-name arity lang in-def metadata]
  (let [analysis (:analysis ctx)]
    (when analysis
      (let [to-ns (or (some-> to-ns meta :raw-name) to-ns)]
        (swap! analysis update :var-usages conj
               (assoc-some
                (merge
                 {:filename filename
                  :row row
                  :col col
                  :from from-ns
                  :to to-ns
                  :name var-name}
                 (select-some metadata
                              [:private :macro
                               :fixed-arities
                               :varargs-min-arity
                               :deprecated]))
                :arity arity
                :lang lang
                :from-var in-def))))))

(defn reg-var! [{:keys [:analysis :base-lang :lang] :as _ctx}
                filename row col ns nom attrs]
  (when analysis
    (let [attrs (select-keys attrs [:private :macro :fixed-arities :varargs-min-arity
                                    :doc :added :deprecated :test :export :defined-by])]
      (swap! analysis update :var-definitions conj
             (assoc-some
              (merge {:filename filename
                      :row row
                      :col col
                      :ns ns
                      :name nom}
                     attrs)
              :lang (when (= :cljc base-lang) lang))))))

(defn reg-namespace! [{:keys [:analysis :base-lang :lang] :as _ctx}
                      filename row col ns-name in-ns? metadata]
  (when analysis
    (swap! analysis update :namespace-definitions conj
           (assoc-some
            (merge {:filename filename
                    :row row
                    :col col
                    :name ns-name}
                   metadata)
            :in-ns (when in-ns? in-ns?) ;; don't include when false
            :lang (when (= :cljc base-lang) lang)))))

(defn reg-namespace-usage! [{:keys [:analysis :base-lang :lang] :as _ctx}
                            filename row col from-ns to-ns alias]
  (when analysis
    (let [to-ns (or (some-> to-ns meta :raw-name) to-ns)]
      (swap! analysis update :namespace-usages conj
             (assoc-some
              {:filename filename
               :row row
               :col col
               :from from-ns
               :to to-ns}
              :lang (when (= :cljc base-lang) lang)
              :alias alias)))))
