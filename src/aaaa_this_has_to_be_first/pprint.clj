(ns aaaa-this-has-to-be-first.pprint
  (:require [clojure.pprint :as pprint]))

(defonce patched? (volatile! false))

(when-not @patched?
  (alter-var-root #'pprint/write-option-table
                  (fn [m]
                    (zipmap (keys m)
                            (map find-var (vals m))))))

(def new-table-ize
  (fn [t m]
    (apply hash-map
           (mapcat
            #(when-let [v (get t (key %))] [v (val %)])
            m))))

(when-not @patched?
  (alter-var-root #'pprint/table-ize (constantly new-table-ize))
  (alter-meta! #'pprint/write-option-table dissoc :private)
  (alter-meta! #'pprint/with-pretty-writer dissoc :private)
  (alter-meta! #'pprint/pretty-writer? dissoc :private)
  (alter-meta! #'pprint/make-pretty-writer dissoc :private)
  (alter-meta! #'pprint/execute-format dissoc :private))
