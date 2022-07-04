(ns clj-kondo.impl.analysis
  "Helpers for analysis output"
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require
   [clj-kondo.impl.utils :refer [assoc-some export-ns-sym select-some]]))

(defn select-context [selector ctx]
  (when selector
    (if (true? selector)
      (:context ctx)
      (select-keys (:context ctx) selector))))

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
                               :defmethod
                               :name-row
                               :name-col
                               :name-end-row
                               :name-end-col
                               :end-row
                               :end-col]))
                :arity arity
                :lang lang
                :from-var in-def
                :context (select-context (:analysis-context ctx) ctx)))))))

(defn reg-var! [{:keys [:analysis-var-meta :analysis :base-lang :lang] :as _ctx}
                filename row col ns nom attrs]
  (when analysis
    (let [raw-attrs attrs
          attrs (select-keys attrs [:private :macro :fixed-arities :varargs-min-arity
                                    :doc :added :deprecated :test :export :defined-by
                                    :protocol-ns :protocol-name
                                    :imported-ns
                                    :name-row :name-col :name-end-col :name-end-row
                                    :arglist-strs :end-row :end-col])]
      (swap! analysis update :var-definitions conj
             (assoc-some
              (cond-> (merge {:filename filename
                              :row row
                              :col col
                              :ns ns
                              :name nom}
                             attrs)
                analysis-var-meta (assoc :meta
                                         (cond-> (apply merge (:user-meta raw-attrs))
                                           (not (true? analysis-var-meta)) (select-keys analysis-var-meta))))
              :lang (when (= :cljc base-lang) lang))))))

(defn reg-namespace! [{:keys [:analysis-ns-meta :analysis :base-lang :lang] :as _ctx}
                      filename row col ns-name in-ns? metadata]
  (when analysis
    (swap! analysis update :namespace-definitions conj
           (assoc-some
            (cond-> (merge {:filename filename
                            :row      row
                            :col      col
                            :name     ns-name}
                           metadata)
              analysis-ns-meta (-> (assoc :meta
                                          (cond-> (apply merge (:user-meta metadata))
                                            (not (true? analysis-ns-meta)) (select-keys analysis-ns-meta)))
                                   (dissoc :user-meta)))
            :in-ns (when in-ns? in-ns?) ;; don't include when false
            :lang (when (= :cljc base-lang) lang)))))

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
             (assoc-some (select-keys usage [:row :col :end-row :end-col :alias :ns
                                             :keys-destructuring
                                             :keys-destructuring-ns-modifier
                                             :reg :auto-resolved :namespace-from-prefix])
                         :name (name (:name usage))
                         :filename filename
                         :lang (when (= :cljc (:base-lang ctx)) (:lang ctx))
                         :from-var (:in-def ctx)
                         :from (get-in ctx [:ns :name])
                         :context (select-context (:analysis-context ctx) usage))))))

(defn reg-protocol-impl!
  [ctx filename impl-ns protocol-ns protocol-name method-node method-name-node defined-by]
  (when (:analyze-protocol-impls? ctx)
    (when-let [analysis (:analysis ctx)]
      (let [method-meta (meta method-node)
            method-name-meta (meta method-name-node)]
        (swap! analysis update :protocol-impls conj
               {:protocol-name protocol-name
                :protocol-ns protocol-ns
                :method-name (:value method-name-node)
                :impl-ns impl-ns
                :filename filename
                :defined-by defined-by
                :name-row (:row method-name-meta)
                :name-col (:col method-name-meta)
                :name-end-row (:end-row method-name-meta)
                :name-end-col (:end-col method-name-meta)
                :row (:row method-meta)
                :col (:col method-meta)
                :end-row (:end-row method-meta)
                :end-col (:end-col method-meta)})))))

(defn reg-instance-invocation!
  [ctx method-name-node]
  (when (:analyze-instance-invocations? ctx)
    (when-let [analysis (:analysis ctx)]
      (let [method-meta (meta method-name-node)
            k :instance-invocations]
        (when k
          (swap! analysis update k conj
                 (cond->
                     {:method-name (str method-name-node)
                      :filename (:filename ctx)
                      :name-row (:row method-meta)
                      :name-col (:col method-meta)
                      :name-end-row (:end-row method-meta)
                      :name-end-col (:end-col method-meta)}
                   (= :cljc (:base-lang ctx))
                   (assoc :lang (:lang ctx)))))))))
