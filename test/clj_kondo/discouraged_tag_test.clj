(ns clj-kondo.discouraged-tag-test
  (:require
   [clj-kondo.test-utils :as tu :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest discouraged-tag-test

  (assert-submaps '({:file "<stdin>", :row 1, :col 12, :level :warning, :message "Discouraged tag literal: inst"})
                  (lint! "(def date #inst \"2020-01-01T00:00:00Z\")"
                         '{:linters {:discouraged-tag {inst {}}}}))
  (assert-submaps '({:file "<stdin>", :row 1, :col 22, :level :warning, :message "Discouraged tag literal: inst"})
                  (lint! "(defn str->inst [s] #inst s)"
                         '{:linters {:discouraged-tag {inst {}}}}))
  (assert-submaps '({:file "<stdin>", :row 1, :col 23, :level :warning, :message "Discouraged tag literal: inst"})
                  (lint! "(def date-map {:date #inst \"2020-01-01T00:00:00Z\"})"
                         '{:linters {:discouraged-tag {inst {}}}}))
  (assert-submaps '({:file "<stdin>", :row 1, :col 13, :level :warning, :message "Discouraged tag literal: inst"})
                  (lint! "(let [date #inst \"2020-01-01T00:00:00Z\"])"
                         '{:linters {:discouraged-tag {inst {}}}}))
  (assert-submaps '({:file "<stdin>", :row 1, :col 12, :level :warning, :message "Discouraged tag literal: inst"})
                  (lint! "(f {:date #inst \"2020-01-01T00:00:00Z\"})"
                         '{:linters {:discouraged-tag {inst {}}}}))
  (assert-submaps '({:file "<stdin>", :row 1, :col 9, :level :warning, :message "Discouraged tag literal: inst"})
                  (lint! "{:date #inst \"2020-01-01T00:00:00Z\"}"
                         '{:linters {:discouraged-tag {inst {}}}}))
  (assert-submaps '({:file "<stdin>", :row 1, :col 3, :level :warning, :message "Discouraged tag literal: inst"})
                  (lint! "[#inst \"2020-01-01T00:00:00Z\"]"
                         '{:linters {:discouraged-tag {inst {}}}}))
  (assert-submaps '({:file "<stdin>", :row 1, :col 2, :level :warning, :message "Discouraged tag literal: inst"})
                  (lint! "#inst \"2020-01-01T00:00:00Z\""
                         '{:linters {:discouraged-tag {inst {}}}}))
  (assert-submaps '({:file "<stdin>", :row 1, :col 22, :level :warning, :message "The cake is a lie."})
                  (lint! "(defn str->inst [s] #inst s)"
                         '{:linters {:discouraged-tag {inst {:message "The cake is a lie."}}}}))
  (assert-submaps '({:file "<stdin>", :row 1, :col 12, :level :error, :message "Discouraged tag literal: inst"})
                  (lint! "(def date #inst \"2020-01-01T00:00:00Z\")"
                         '{:linters {:discouraged-tag {:level :error
                                                       inst {}}}}))
  (assert-submaps '({:file "<stdin>", :row 1, :col 12, :level :info, :message "Discouraged tag literal: inst"})
                  (lint! "(def date #inst \"2020-01-01T00:00:00Z\")"
                         '{:linters {:discouraged-tag {:level :info
                                                       inst {}}}}))
  (is (empty? (lint! "(def date #inst \"2020-01-01T00:00:00Z\")"
                     '{:linters {:discouraged-tag {:level :off inst {}}}})))
  (is (empty? (lint! "(def date #inst \"2020-01-01T00:00:00Z\")"
                     '{:linters {:discouraged-tag {}}}))))
