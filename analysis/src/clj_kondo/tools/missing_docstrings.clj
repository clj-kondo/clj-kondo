(ns clj-kondo.tools.missing-docstrings
  (:require [clj-kondo.core :as clj-kondo]))

(defn -main [& paths]
  (let [analysis (:analysis (clj-kondo/run! {:lint paths
                                             :config {:analysis {:var-usages false}}
                                             :skip-lint true}))
        {:keys [:var-definitions]} analysis]
    (doseq [{:keys [:ns :name :private :doc]} var-definitions]
      (when (and (not private)
                 (not doc))
        (println (str ns "/" name ": missing docstring"))))))
