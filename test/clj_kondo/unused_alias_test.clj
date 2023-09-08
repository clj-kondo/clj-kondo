(ns clj-kondo.unused-alias-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :refer [deftest testing is]]))

(def conf {:linters {:unused-alias {:level :warning}
                     :unused-namespace {:level :off}}})

(deftest unused-import-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 34, :level :warning, :message "Unused alias: b"}
     {:file "<stdin>",
      :row 2,
      :col 39,
      :level :warning,
      :message "Unused alias: bz"})
   (lint! "(ns foo (:require [bar :as-alias b]
                             [baz :as bz]))" conf))
  (assert-submaps2
   '({:file "<stdin>", :row 5, :col 31, :level :warning, :message "Unused alias: u"})
   (lint! "
(ns foo (:require [bar :as-alias b]
                  [baz :as bz]
                  [quuz :as q]
                  [unused :as u]))
`b/dude
::bz/dude
q/dude" conf))
  (testing "using alias as object in CLJS"
    (is (empty?
         (lint! "
(ns foo (:require [\"dayjs\" :as dayjs])) dayjs" conf
                "--lang" "cljs")))))
