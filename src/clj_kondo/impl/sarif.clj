(ns clj-kondo.impl.sarif
  (:require [clj-kondo.impl.config :as config]))

;; https://github.com/microsoft/sarif-tutorials/blob/main/docs/1-Introduction.md#simple-example

(set! *warn-on-reflection* true)

(defn finding->sarif [rules files finding]
  {:level (:level finding)
   :message {:text (:message finding)}
   :locations [{:physicalLocation
                {:artifactLocation
                 {:uri (:filename finding)
                  :index (.indexOf ^java.util.List files (:filename finding))
                  :region {:startLine (:row finding)
                           :startColumn (:col finding)}}}}]
   :ruleId (:type finding)
   :ruleIndex (:index (get rules (:type finding)))})

(defn generate-sarif [{:keys [findings]}]
  (let [linters (:linters config/default-config)
        rules (zipmap (keys linters)
                      (mapv (fn [[k _] i]
                              {:id k :index i})
                            linters
                            (range)))
        files (vec (distinct (map :filename findings)))]
    {:version "2.1.0"
     "$schema" "http://json.schemastore.org/sarif-2.1.0-rtm.4"
     :runs [{:tool {:driver {:name "Clj-kondo"
                             :informationUri "https://github.com/clj-kondo/clj-kondo"
                             :rules (mapv #(dissoc % :index) (vals rules))}}
             :artifacts (mapv (fn [file]
                                {:location {:uri file}})
                              files)
             :results (mapv #(finding->sarif rules files %) findings)}]}))
