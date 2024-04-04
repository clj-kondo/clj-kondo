(ns clj-kondo.impl.sarif)

;; https://github.com/microsoft/sarif-tutorials/blob/main/docs/1-Introduction.md#simple-example

(defn finding->sarif [finding]
  (binding [*out* *err*]
    (println (keys finding)))
  {:level (:level finding)
   :message {:text (:message finding)}
   :locations [{:physicalLocation
                {:artifactLocation
                 {:uri (:uri finding)
                  :index :TODO
                  :region {:startLine (:row finding)
                           :startColumn (:col finding)}}}}]
   :ruleId (:type finding)
   :ruleIndex :TODO})

(defn generate-sarif [{:keys [findings]}]
  {:version "2.1.0"
   "$schema" "http://json.schemastore.org/sarif-2.1.0-rtm.4"
   :runs [{:tool {:driver {:name "Clj-kondo"
                           :informationUri "https://github.com/clj-kondo/clj-kondo"
                           :rules [{:id :unresolved-symbol
                                    :shortDescription {:text "Unresolved symbol"}
                                    :helpUri "https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#unresolved-var"}]}}
           :artifacts [{:location {:uri "..."}}]
           :results (mapv finding->sarif findings)}]})
