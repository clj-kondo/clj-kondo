(ns clj-kondo.impl.database
  {:no-doc true}
  (:require [next.jdbc :as jdbc]))

(defn db-spec [cfg-dir]
  (format "jdbc:hsqldb:file:%s/.cache/db;sql.syntax_mys=true" (str cfg-dir)))

(defn ddl [db]
  (jdbc/execute! db ["create table if not exists var_usages (name text)"]))

(defn insert [db]
  (jdbc/execute! db ["insert into var_usages (name) values ('dude')"]))
