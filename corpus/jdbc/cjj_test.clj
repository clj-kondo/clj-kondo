(ns jdbc.cjj-test
  (:require [clojure.java.jdbc :as jdbc]))

(defn tx-check
  []
  (let [db {:dbtype "some-db" :dbname "kondo"}]
    ;; should not flag tx
    (jdbc/with-db-transaction [tx (jdbc/get-connection db)]
      (jdbc/execute! tx ["select * from table where foo = ?" 123]))
    ;; should not flag tx or binding arity
    (jdbc/with-db-transaction [tx (jdbc/get-connection db) {}]
      (jdbc/execute! tx ["select * from table where foo = ?" 123]))))

(defn con-check
  []
  (let [db {:dbtype "some-db" :dbname "kondo"}]
    ;; should not flag con
    (jdbc/with-db-connection [con db]
      (jdbc/query con ["select * from table where foo = ?" 123]))
    ;; should not flag con or binding arity
    (jdbc/with-db-connection [con db {}]
      (jdbc/query con ["select * from table where foo = ?" 123]))))

(defn meta-check
  []
  (let [db {:dbtype "some-db" :dbname "kondo"}]
    ;; should not flag m-con
    (jdbc/with-db-metadata [m-con db]
      (jdbc/metadata-query (.getTables m-con nil nil nil (into-array String [\"TABLE\"]))))
    ;; should not flag m-con or binding arity
    (jdbc/with-db-metadata [m-con db {}]
      (jdbc/metadata-query (.getTables m-con nil nil nil (into-array String [\"TABLE\"]))))))
