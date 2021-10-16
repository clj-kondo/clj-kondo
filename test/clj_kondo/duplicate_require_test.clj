(ns clj-kondo.duplicate-require-test
  (:require [clj-kondo.core :as clj-kondo]
            [clj-kondo.test-utils :refer [assert-submaps]]
            [clojure.test :refer [deftest is testing]]))

(deftest duplicate-require-test
  (assert-submaps
   '({:type :duplicate-require
      :filename "<stdin>",
      :duplicate-ns clojure.string
      :row 1,
      :col 43,
      :level :warning,
      :message "duplicate require of clojure.string"})
   (-> (with-in-str
         "(ns foo (:require [clojure.string :as s] [clojure.string :as str])) s/join"
         (clj-kondo/run! {:lint ["-"]}))
       :findings)))
