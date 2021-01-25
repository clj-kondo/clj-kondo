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
                           {:linters {:datalog-syntax {:level :error}}}))))))
