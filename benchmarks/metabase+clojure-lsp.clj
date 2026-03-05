;; invoke with clj -M:profiler:bench benchmarks/metabase+clojure-lsp.clj
(require '[clojure-lsp.api :as api]
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

(time (clojure-lsp.api/analyze-project-only! {:project-root (clojure.java.io/file "/Users/borkdude/dev/metabase")}))

(println (format "GC stats: %s collections, collected %.1fGB, spent %.1f seconds on GC"
                 (count @gc-events)
                 (double (/ (reduce + (map first @gc-events)) 1e9))
                 (double (/ (reduce + (map second @gc-events)) 1e3))))

(prn :done)
(shutdown-agents)
