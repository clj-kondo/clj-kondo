(ns clj-kondo.impl.analysis
  "Helpers for analysis output"
  {:no-doc true}
  (:refer-clojure :exclude [ns-name]))

(defn reg-usage! [{:keys [analysis] :as _ctx}
                  filename row col from-ns to-ns var-name arity]
  (swap! analysis update :var-usages conj
         (cond->
             {:filename filename
              :row row
              :col col
              :from from-ns
              :to to-ns
              :name var-name}
           arity (assoc :arity arity))))

(defn reg-var! [{:keys [analysis] :as _ctx}
                filename row col ns name attrs]
  (let [attrs (select-keys attrs [:private :macro :fixed-arities :var-args-min-arity])]
    (swap! analysis update :var-definitions conj
           (merge {:filename filename
                   :row row
                   :col col
                   :ns ns
                   :name name}
                  attrs))))

(defn reg-namespace! [{:keys [analysis] :as _ctx} filename row col ns-name in-ns]
  (swap! analysis update :namespace-definitions conj
         (cond->
             {:filename filename
              :row row
              :col col
              :name ns-name}
           in-ns (assoc :in-ns in-ns))))

(defn reg-namespace-usage! [{:keys [analysis] :as _ctx} filename row col from-ns to-ns]
  (swap! analysis update :namespace-usages conj
         {:filename filename
          :row row
          :col col
          :from from-ns
          :to to-ns}))
