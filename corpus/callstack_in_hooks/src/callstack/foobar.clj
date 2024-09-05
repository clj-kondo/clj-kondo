(ns callstack.foobar)

(defn foobar [])

(try
  (inc (foobar))
  (catch Exception _ nil))
