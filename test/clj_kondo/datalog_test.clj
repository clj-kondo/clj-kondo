(ns clj-kondo.datalog-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :refer [deftest testing is]]))

(deftest datalog-syntax
    (testing "datalog parsing"
      (assert-submaps
       '({:file "<stdin>", :row 2, :col 19,
          :level :error, :message "Query for unknown vars: [?a]"})
       (lint! "(ns user (:require [datahike.api :refer [q]]))
               (q '[:find ?a :where [?b :foo _]] 42)"
              {:linters {:datalog-syntax {:level :error}}}))
      (assert-submaps
       '({:file "<stdin>", :row 3, :col 22,
          :level :warning, :message "unused binding y"}
         {:file "<stdin>", :row 4, :col 23, :level :error,
          :message "Unresolved symbol: db"})
       (lint! "(ns user (:require [datahike.api :refer [q]]))
               (let [x '[:find ?a :where [?a :foo _]]
                     y 42]
                 (q x db))"
              {:linters {:datalog-syntax {:level :error}
                         :unused-binding {:level :warning}
                         :unresolved-symbol {:level :error}}}))
      (testing "EDN checks still work and additional arguments are linted"
        (assert-submaps
         '[{:file "<stdin>", :row 2, :col 28, :level :error, :message "duplicate key :a"}
           {:file "<stdin>", :row 2, :col 40, :level :error, :message "duplicate key :b"}]
         (lint! "(ns user (:require [datahike.api :refer [q]]))
               (q x '{:a 1 :a 2} {:b 1 :b 2})")))
      ;; avoiding false positives
      (is (empty? (lint! "(ns user (:require [datahike.api :refer [q]]))
                          (q '[:find ?a :where [?a :foo _]] 42)"
                         {:linters {:datalog-syntax {:level :error}}})))
      (testing ":order-by, :having, :timeout, :limit and :offset report no :datalog-syntax"
        (is (empty? (lint! "(ns user (:require [datalevin.core :refer [q]]))
                          (q '[:find ?e ?score :where [?e :item/score ?score] :order-by [?score :desc] :limit 5] db)
                          (q '{:find [?e] :where [[?e :age 30]] :limit 5 :offset 2} db)
                          (q '[:find ?e (count ?a) :where [?e :foo ?a] :having [(> (count ?a) 1)] :timeout 1000] db)"
                           {:linters {:datalog-syntax {:level :error}
                                      :unresolved-symbol {:level :off}}}))))
      (testing "absence of argument doesn't make linter throw"
        (is (empty? (lint! "(ns user (:require [datahike.api :refer [q]]))
                          (q) (q nil)"
                           {:linters {:datalog-syntax {:level :error}}})))))

    (testing "supports multiple datalog libraries"
      (doseq [lib ["datahike.api"
                   "datascript.core"
                   "datomic.api"
                   "datomic.client.api"
                   "datalevin.core"
                   "datomic-type-extensions.api"]]
        (assert-submaps
         '({:file "<stdin>", :row 2, :col 19,
            :level :error, :message "Query for unknown vars: [?a]"})
         (lint! (str "(ns user (:require [" lib " :refer [q]]))
               (q '[:find ?a :where [?b :foo _]] 42)")
                {:linters {:datalog-syntax {:level :error}}})))))
