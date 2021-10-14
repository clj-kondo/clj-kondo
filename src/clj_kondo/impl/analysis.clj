(ns clj-kondo.impl.analysis
  "Helpers for analysis output"
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require [clj-kondo.impl.utils :refer [assoc-some select-some export-ns-sym]]))

(defn reg-usage! [ctx filename row col from-ns to-ns var-name arity lang in-def metadata]
  (let [analysis (:analysis ctx)]
    (when analysis
      (let [to-ns (export-ns-sym to-ns)]
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
                               :deprecated
                               :refer
                               :alias
                               :name-row
                               :name-col
                               :name-end-row
                               :name-end-col
                               :end-row
                               :end-col]))
                :arity arity
                :lang lang
                :from-var in-def))))))

(defn reg-var! [{:keys [:config :analysis :base-lang :lang] :as _ctx}
                filename row col ns nom attrs]
  (when analysis
    (let [raw-attrs attrs
          attrs (select-keys attrs [:private :macro :fixed-arities :varargs-min-arity
                                    :doc :added :deprecated :test :export :defined-by
                                    :name-row :name-col :name-end-col :name-end-row
                                    :arglist-strs :end-row :end-col])
          meta-fn (when-let [keyseq (some-> config :output :analysis :var-definitions :meta)]
                    (if (true? keyseq)
                      identity
                      #(select-keys % keyseq)))]
      (swap! analysis update :var-definitions conj
             (assoc-some
              (cond-> (merge {:filename filename
                              :row row
                              :col col
                              :ns ns
                              :name nom}
                             attrs)
                meta-fn (assoc :meta (meta-fn raw-attrs)))
              :lang (when (= :cljc base-lang) lang))))))

(defn reg-namespace! [{:keys [:config :analysis :base-lang :lang] :as _ctx}
                      filename row col ns-name in-ns? attrs]
  (when analysis
    (let [raw-attrs attrs
          attrs (select-keys attrs [:doc :added :deprecated :author :no-doc
                                    :name-row :name-col :name-end-col :name-end-row])
          meta-fn (when-let [keyseq (some-> config :output :analysis :namespace-definitions :meta)]
                    (if (true? keyseq)
                      identity
                      #(select-keys % keyseq)))]
      (swap! analysis update :namespace-definitions conj
             (assoc-some
               (cond-> (merge {:filename filename
                               :row      row
                               :col      col
                               :name     ns-name}
                              attrs)
                 meta-fn (assoc :meta (meta-fn raw-attrs)))
               :in-ns (when in-ns? in-ns?) ;; don't include when false
               :lang (when (= :cljc base-lang) lang))))))

(defn reg-namespace-usage! [{:keys [:analysis :base-lang :lang] :as _ctx}
                            filename row col from-ns to-ns alias metadata]
  (when analysis
    (let [to-ns (export-ns-sym to-ns)]
      (swap! analysis update :namespace-usages conj
             (assoc-some
               (merge {:filename filename
                       :row row
                       :col col
                       :from from-ns
                       :to to-ns}
                      metadata)
              :lang (when (= :cljc base-lang) lang)
              :alias alias)))))

(defn reg-local! [{:keys [:analysis] :as ctx} filename binding]
  (when (and analysis
             (not (:clj-kondo.impl/generated binding)))
    (swap! analysis update :locals conj
           (assoc-some (select-keys binding [:name :str :id :row :col :end-row :end-col :scope-end-col :scope-end-row])
                       :filename filename
                       :lang (when (= :cljc (:base-lang ctx)) (:lang ctx))))))

(defn reg-local-usage! [{:keys [:analysis] :as ctx} filename binding usage]
  (when (and analysis
             (not (:clj-kondo.impl/generated binding)))
    (swap! analysis update :local-usages conj
           (assoc-some (select-keys usage [:id :row :col :end-row :end-col :name-row :name-col :name-end-row :name-end-col])
                       :name (:name binding)
                       :filename filename
                       :lang (when (= :cljc (:base-lang ctx)) (:lang ctx))
                       :id (:id binding)))))

(defn reg-keyword-usage! [ctx filename usage]
  (when (:analyze-keywords? ctx)
    (when-let [analysis (:analysis ctx)]
      (swap! analysis update :keywords conj
             (assoc-some (select-keys usage [:row :col :end-row :end-col :alias :ns :keys-destructuring :reg :auto-resolved :namespace-from-prefix])
                         :name (name (:name usage))
                         :filename filename
                         :lang (when (= :cljc (:base-lang ctx)) (:lang ctx)))))))
