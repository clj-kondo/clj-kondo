(ns clj-kondo.profile
  (:require [clj-kondo.main :as main]))

(defn -main [& options]
  ;; prevent loading async profiler when lein test scans test files
  (require '[clj-async-profiler.core :as prof])
  ((resolve 'clj-async-profiler.core/profile)
   (apply main/main options))
  (shutdown-agents))
