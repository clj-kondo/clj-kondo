(ns clj-kondo.docstring-format-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))


(def linter-config
  {:linters
   {:docstring-blank {:level :warning}
    :docstring-no-summary {:level :warning}
    :docstring-leading-trailing-whitespace {:level :warning}}})

(deftest docstring-format-test
  (testing "all linters with def"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 10,
        :level :warning,
        :message "Docstring should not be blank."}
       {:file "<stdin>",
        :row 1,
        :col 10,
        :level :warning,
        :message "First line of the docstring should be a capitalized sentence ending with punctuation."})
     (lint! "(def foo \"\" 1)" linter-config))

    (assert-submaps
     '({:message "Docstring should not have leading or trailing whitespace."
        :row 1
        :col 10})
     (lint! "(def foo \" Some leading whitespace.\" 1)" linter-config))

    (assert-submaps
     ;; Dont't test row/col here; points to `def` expression instead of metadata map
     '({:message "Docstring should not have leading or trailing whitespace."})
     (lint! "(def ^{:doc \" Some leading whitespace.\"} foo 1)" linter-config)))

  (testing "defn"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 11,
        :level :warning,
        :message "First line of the docstring should be a capitalized sentence ending with punctuation."})
     (lint! "(defn foo \"not capitalized.\" [])" linter-config))

    (assert-submaps
     '({:message "Docstring should not have leading or trailing whitespace."
        :row 1
        :col 17})
     (lint! "(defn foo {:doc \"Some trailing whitespace. \"} [] 1)" linter-config))

    (assert-submaps
     ;; Dont't test row/col here; points to `defn` expression instead of metadata map
     '({:message "First line of the docstring should be a capitalized sentence ending with punctuation."})
     (lint! "(defn ^{:doc \"meta\"} foo [] 1)" linter-config)))

  (testing "defprotocol and ns"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 9,
        :message "First line of the docstring should be a capitalized sentence ending with punctuation."}
       {:message "Docstring should not have leading or trailing whitespace."
        :row 3
        :col 14}
       {:file "<stdin>",
        :row 5,
        :col 17,
        :message "First line of the docstring should be a capitalized sentence ending with punctuation."})
     (lint! "(ns foo \"no sentence\")
           (defprotocol Foo
             \"Foo trailing whitespace. \"
             (foo [this x y z]
                \"foo method docstring.\"))" linter-config)))

  (testing "defmulti"
    (assert-submaps
     ;; don't test for row/col here. points to `defmulti` expr instead of entry in metadata.
     '({:file "<stdin>",
        :message "First line of the docstring should be a capitalized sentence ending with punctuation."})
     (lint! "(defmulti ^{:doc \"lead\"} mult :dispatch)" linter-config))

    (assert-submaps
     '({:file "<stdin>",
        :row 1
        :col 22
        :message "First line of the docstring should be a capitalized sentence ending with punctuation."})
     (lint! "(defmulti mult {:doc \"attr\"} :dispatch)" linter-config))

    (assert-submaps
     '({:file "<stdin>",
        :row 1
        :col 16
        :message "First line of the docstring should be a capitalized sentence ending with punctuation."})
     (lint! "(defmulti mult \"doc\" :dispatch)" linter-config))))
