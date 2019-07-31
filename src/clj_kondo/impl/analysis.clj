(ns clj-kondo.impl.analysis
  "Helpers for analysis output"
  {:no-doc true}
  (:refer-clojure :exclude [ns-name])
  (:require [clj-kondo.impl.utils :refer [assoc-some select-some]]))

;; {:added "1.2", :ns #object[clojure.lang.Namespace 0x79c7532f "clojure.core"], :name inc, :file "clojure/core.clj", :column 1, :line 922, :arglists ([x]), :doc "Returns a number one greater than num. Does not auto-promote\n  longs, will throw on overflow. See also: inc'", :inline #object[clojure.core$inc__inliner__5536 0x64b73e7a "clojure.core$inc__inliner__5536@64b73e7a"]}

(defn reg-usage! [{:keys [analysis] :as _ctx}
                  filename row col from-ns to-ns var-name arity lang metadata]
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
                            :var-args-min-arity
                            :deprecated]))
             :arity arity
             :lang lang)))

(defn reg-var! [{:keys [analysis] :as _ctx}
                filename row col ns name attrs]
  (let [attrs (select-keys attrs [:private :macro :fixed-arities :var-args-min-arity
                                  :doc :added :deprecated])]
    (swap! analysis update :var-definitions conj
           (merge {:filename filename
                   :row row
                   :col col
                   :ns ns
                   :name name}
                  attrs))))

(defn reg-namespace! [{:keys [analysis] :as _ctx} filename row col ns-name in-ns metadata]
  (swap! analysis update :namespace-definitions conj
         (cond->
             (merge {:filename filename
                     :row row
                     :col col
                     :name ns-name}
                    metadata)
           in-ns (assoc :in-ns in-ns))))

(defn reg-namespace-usage! [{:keys [analysis] :as _ctx} filename row col from-ns to-ns]
  (swap! analysis update :namespace-usages conj
         {:filename filename
          :row row
          :col col
          :from from-ns
          :to to-ns}))
