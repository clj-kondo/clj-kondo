;; invoke with:
;; cd /path/to/metabase && clj -Sdeps '{:deps {clj-kondo/clj-kondo {:local/root "/Users/borkdude/dev/clj-kondo"}}}' benchmarks/metabase-lint.clj
(require '[clj-kondo.core :as kondo]
         '[clojure.java.io :as io])

(import
 '[com.sun.management GarbageCollectionNotificationInfo GcInfo]
 '[javax.management NotificationEmitter]
 '[java.lang.management ManagementFactory MemoryUsage]
 '[javax.management NotificationListener]
 '[javax.management.openmbean CompositeData])

(def gc-events (atom []))

(defn calc-freed [^GcInfo gc-info]
  (let [before (.getMemoryUsageBeforeGc gc-info)
        after  (.getMemoryUsageAfterGc gc-info)]
    (reduce (fn [total pool]
              (let [used-before (.getUsed ^MemoryUsage (get before pool))
                    used-after  (.getUsed ^MemoryUsage (get after pool))]
                (if (> used-before used-after)
                  (+ total (- used-before used-after))
                  total)))
            0
            (keys before))))

(defn install-gc-listener! []
  (doseq [gc-bean (ManagementFactory/getGarbageCollectorMXBeans)]
    (.addNotificationListener
     ^NotificationEmitter gc-bean
     (reify NotificationListener
       (handleNotification [_ notification _]
         (when (= (.getType notification)
                  GarbageCollectionNotificationInfo/GARBAGE_COLLECTION_NOTIFICATION)
           (let [info (GarbageCollectionNotificationInfo/from
                       ^CompositeData (.getUserData notification))
                 gc-info (.getGcInfo info)
                 freed   (calc-freed gc-info)]
             (swap! gc-events conj [freed (.getDuration gc-info)])))))
     nil nil)))

(install-gc-listener!)

(let [start (System/currentTimeMillis)
      result (kondo/run! {:lint ["src"]})
      elapsed (- (System/currentTimeMillis) start)]
  (println (format "Linting took %dms, errors: %d, warnings: %d"
                   elapsed
                   (count (filter #(= :error (:level %)) (:findings result)))
                   (count (filter #(= :warning (:level %)) (:findings result)))))
  (println (format "GC stats: %d collections, collected %.1fGB, spent %.1f seconds on GC"
                   (count @gc-events)
                   (double (/ (reduce + (map first @gc-events)) 1e9))
                   (double (/ (reduce + (map second @gc-events)) 1e3)))))

(shutdown-agents)
