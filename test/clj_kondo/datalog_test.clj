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

(deftest datalog-engine-specific-clauses
  (testing "clauses beyond the Datomic dialect are not syntax errors"
    (is (empty? (lint! "(ns user (:require [datalevin.core :refer [q]]))
                        (q '[:find ?e ?score
                             :where [?e :item/score ?score]
                             :order-by [?score :desc]
                             :limit 5] 42)"
                       {:linters {:datalog-syntax {:level :error}}})))
    (is (empty? (lint! "(ns user (:require [datahike.api :refer [q]]))
                        (q '{:find [?e] :where [[?e :age 30]] :limit 5} 42)"
                       {:linters {:datalog-syntax {:level :error}}}))))

  (testing "Datahike pre-installs its bitemporal rules, so no % is needed"
    (is (empty? (lint! "(ns user (:require [datahike.api :refer [q]]))
                        (q '[:find ?s :in $ ?at
                             :where (valid-at ?tx ?at) [?e :salary ?s ?tx true]] 42 43)"
                       {:linters {:datalog-syntax {:level :error}}}))))

  (testing "an engine without pre-installed rules still needs it"
    (assert-submaps
     '({:level :error, :message "Missing rules var '%' in :in"})
     (lint! "(ns user (:require [datascript.core :refer [q]]))
             (q '[:find ?e :where (my-rule ?e)] 42)"
            {:linters {:datalog-syntax {:level :error}}})))

  (testing "the default is configurable"
    (assert-submaps
     '({:level :error, :message "Missing rules var '%' in :in"})
     (lint! "(ns user (:require [datahike.api :refer [q]]))
             (q '[:find ?e :where (my-rule ?e)] 42)"
            {:linters {:datalog-syntax {:level :error :implicit-rules false}}}))
    (is (empty? (lint! "(ns user (:require [datascript.core :refer [q]]))
                        (q '[:find ?e :where (my-rule ?e)] 42)"
                       {:linters {:datalog-syntax {:level :error :implicit-rules true}}})))))
