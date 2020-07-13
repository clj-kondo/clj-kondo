(ns clj-kondo.profile
  (:require [clj-async-profiler.core :as prof]
            [clj-kondo.main :as main]))

(defn -main [& options]
  ;; prevent loading async profiler when lein test scans test files
  (prof/profile (apply main/main options))
  (shutdown-agents))
