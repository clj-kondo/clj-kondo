(ns clj-kondo.impl.sarif
  (:require
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.version :as version]))

;; https://github.com/microsoft/sarif-tutorials/blob/main/docs/1-Introduction.md#simple-example

(set! *warn-on-reflection* true)

(defn- linter-help-uri [linter]
  (format "https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#%s" (name linter)))

(defn- sarif-level [level]
  (case level
    :error "error"
    :warning "warning"
    :off "none"))

(defn- finding->sarif [finding]
  {:level (-> finding :level sarif-level)
   :message {:text (:message finding)}
   :locations [{:physicalLocation
                {:artifactLocation {:uri (:filename finding)}
                 :region {:startLine (:row finding)
                          :startColumn (:col finding)
                          :endLine (:end-row finding)
                          :endColumn (:end-col finding)}}}]
   :ruleId (:type finding)})

(defn generate-sarif [{:keys [findings]}]
  (let [linters (:linters config/default-config)
        rules (zipmap (keys linters)
                      (mapv (fn [[linter {:keys [level]}] i]
                              {:id linter
                               :helpUri (linter-help-uri linter)
                               :defaultConfiguration
                               {:enabled (if (= :off level) false true)
                                :level (sarif-level level)}
                               :index i})
                            linters
                            (range)))]
    {:version "2.1.0"
     :$schema "https://docs.oasis-open.org/sarif/sarif/v2.1.0/errata01/os/schemas/sarif-schema-2.1.0.json"
     :runs [{:tool {:driver {:name "Clj-kondo"
                             :version version/version
                             :informationUri "https://github.com/clj-kondo/clj-kondo"
                             :rules (mapv #(dissoc % :index) (vals rules))}}
             :results (mapv #(finding->sarif %) findings)}]}))
