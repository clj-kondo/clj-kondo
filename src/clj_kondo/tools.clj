(ns clj-kondo.tools
  (:require [clj-kondo.core :as clj-kondo]
            [clojure.pprint :refer [pprint]]))

(defn find-references [{:keys [:var
                               ;; :include-def
                               :lint]}]
  (let [analysis (-> (clj-kondo/run! {:config {:output {:analysis true}}
                                      :lint (cond (string? lint) [lint]
                                                  (symbol? lint) [(str lint)]
                                                  :else lint)})
                     :analysis)
        ns-to-find (symbol (namespace var))
        name-to-find (symbol (name var))
        usages (:var-usages analysis)]
    (pprint
     {:references (filterv (fn [{:keys [:to :name] :as v}]
                             (and (= to ns-to-find)
                                  (= name name-to-find)))
                           usages)})))
