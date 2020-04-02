(ns clj-kondo.missing-docstring-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest missing-docstring-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :warning,
      :message "Missing docstring."})
   (lint! "(defn foo [])" {:linters {:missing-docstring {:level :warning}}}))
  (is (empty? (lint! "(defn foo \"dude\" [])" {:linters {:missing-docstring {:level :warning}}})))
  (is (empty? (lint! "(defn- foo []) (foo)" {:linters {:missing-docstring {:level :warning}}})))
  (is (empty? (lint! "(require '[clojure.test :as t]) (t/deftest foo)"
                     {:linters {:missing-docstring {:level :warning}}})))
  (is (empty? (lint! "(require '[rum.core])
(rum.core/defcs hello-world \"Example react component.\"
  [state name]
  \"foo\")"
                     '{:linters {:missing-docstring {:level :warning}}
                       :lint-as {rum.core/defcs clj-kondo.lint-as/def-catch-all}})))
  (is (empty? (lint! (str "(require '[expectations.clojure.test :refer [defexpect]])"
                          "(defexpect foo even? 42)")
                     '{:linters {:missing-docstring {:level :warning}}
                       :lint-as {expectations.clojure.test/defexpect clojure.test/deftest}}))))
