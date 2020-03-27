(ns jdbc.next-test
  "Syntax check examples for next.jdbc/with-transaction.

  The clojure.java.jdbc/with-db-* functions would all be treated the same way."
  (:require [next.jdbc :as jdbc]))

(def db {:dbtype "some-db" :dbname "kondo"})

(jdbc/with-transaction [tx (jdbc/get-datasource db) {} {}] ;; 2 or 3 forms
  (jdbc/execute! tx ["select * from table where foo = ?" 123]))

(jdbc/with-transaction [tx] ;; 2 or 3 forms
  (jdbc/execute! tx ["select * from table where foo = ?" 123]))

(jdbc/with-transaction ;; requires vector for binding
  (jdbc/execute! tx ["select * from table where foo = ?" 123]))

(jdbc/with-transaction [[tx] (jdbc/get-datasource db)] ;; requires a symbol
  (jdbc/execute! tx ["select * from table where foo = ?" 123]))
