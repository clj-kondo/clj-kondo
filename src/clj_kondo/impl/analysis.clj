(ns clj-kondo.impl.analysis
  "Helpers for analysis output"
  {:no-doc true})

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
                filename row col ns name fixed-arities var-args-min-arity]
  (swap! analysis update :var-definitions conj
         (cond->
             {:filename filename
              :row row
              :col col
              :ns ns
              :name name}
           fixed-arities (assoc :fixed-arities fixed-arities)
           var-args-min-arity (assoc :var-args-min-arity var-args-min-arity))))
