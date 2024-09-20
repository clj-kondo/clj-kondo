(ns static-field-call
  (:import [clojure.lang PersistentQueue]
           [java.time.temporal ChronoField]))

(System/err)
System/err

(clojure.lang.PersistentQueue/EMPTY)
(PersistentQueue/EMPTY)

(java.time.temporal.ChronoField/DAY_OF_MONTH) ;; enum field
(ChronoField/DAY_OF_MONTH)
