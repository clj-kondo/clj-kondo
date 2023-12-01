(ns clj-kondo.impl.output)

(def err
  "Holds the writer where content going to stderr will be captured.
   Generally used to capture stderr output for testing, defaults to *err*."
  (atom *err*))
 