(ns clj-kondo.config-in-tag-test
  (:require
   [clj-kondo.test-utils :refer
    [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest config-in-tag-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 17, :level :error, :message "Unresolved symbol: x"})
   (lint! "#jsx [:a {:href x}]"
          '{:linters {:unresolved-symbol {:level :error}
                      :unused-binding {:level :warning}}
            :config-in-tag {jsx {:linters {:unresolved-symbol {:level :error}}}}}))
  (assert-submaps
   '({:file "<stdin>", :row 2, :col 29, :level :error, :message "Unresolved symbol: x"})
   (lint! "(ns dude {:clj-kondo/config '{:config-in-tag {jsx {:linters {:unresolved-symbol {:level :error}}}}}})
            #jsx [:a {:href x}]"
          '{:linters {:unresolved-symbol {:level :error}
                      :unused-binding {:level :warning}}})))
