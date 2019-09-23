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

(defn reg-var! [{:keys [:analysis :base-lang :lang] :as _ctx}
                filename row col ns name attrs]
  (let [attrs (select-keys attrs [:private :macro :fixed-arities :var-args-min-arity
                                  :doc :added :deprecated])]
    (swap! analysis update :var-definitions conj
           (assoc-some
            (merge {:filename filename
                    :row row
                    :col col
                    :ns ns
                    :name name}
                   attrs)
            :lang (when (= :cljc base-lang) lang)))))

(defn reg-namespace! [{:keys [:analysis :base-lang :lang] :as _ctx}
                      filename row col ns-name in-ns metadata]
  (swap! analysis update :namespace-definitions conj
         (assoc-some
          (merge {:filename filename
                  :row row
                  :col col
                  :name ns-name}
                 metadata)
          :in-ns (when in-ns in-ns) ;; don't include when false
          :lang (when (= :cljc base-lang) lang))))

(defn reg-namespace-usage! [{:keys [:analysis :base-lang :lang] :as _ctx}
                            filename row col from-ns to-ns alias]
  (swap! analysis update :namespace-usages conj
         (assoc-some
          {:filename filename
           :row row
           :col col
           :from from-ns
           :to to-ns}
          :lang (when (= :cljc base-lang) lang)
          :alias alias)))
