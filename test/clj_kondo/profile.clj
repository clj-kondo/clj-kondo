(ns clj-kondo.profile
  (:require [clj-async-profiler.core :as prof]
            [clj-kondo.main :as main]))

(defn -main [& options]
  (prof/profile
   (apply main/-main options)))
