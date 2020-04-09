(ns jdbc.next-test
  (:require [next.jdbc :as jdbc]))

(defn tx-check
  []
  (let [db {:dbtype "some-db" :dbname "kondo"}]
    ;; should not flag tx
    (jdbc/with-transaction [tx (jdbc/get-datasource db)]
      (jdbc/execute! tx ["select * from table where foo = ?" 123]))
    ;; should not flag tx or binding arity
    (jdbc/with-transaction [tx (jdbc/get-datasource db) {}]
      (jdbc/execute! tx ["select * from table where foo = ?" 123]))))
