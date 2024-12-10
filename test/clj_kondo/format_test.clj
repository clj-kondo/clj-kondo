(ns clj-kondo.format-test
  (:require
   [clj-kondo.test-utils :refer
    [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest format-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 9, :level :error, :message "Format string expects 2 arguments instead of 1."})
   (lint! "(format \"%s %s\" 1)"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 9, :level :error, :message "Format string expects 2 arguments instead of 1."})
   (lint! "(format \"%2$s\" 1)"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 52, :level :error, :message "Format string expects 1 arguments instead of 2."})
   (lint! "(require '[clojure.tools.logging :as l]) (l/errorf \"%s\" 1 2)"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 68, :level :error, :message "Format string expects 1 arguments instead of 2."})
   (lint! "(require '[clojure.tools.logging :as l]) (l/errorf (ex-info \"\" {}) \"%s\" 1 2)"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 9, :level :error, :message "Format string expects 2 arguments instead of 1."})
   (lint! "(printf \"%s %s\" 1)"))
  (is (empty? (lint! "(format \"%3$s\" 1 2 3)")))
  (is (empty? (lint! "(format \"%3$s %s %s %s\" 1 2 3)")))
  (is (empty? (lint! "(format \"%3$s %s %s %s %s\" 1 2 3 4)")))
  (is (empty? (lint! "(format \"%s %<s\" 1)")))
  (is (empty? (lint! "(format \"%s %<s %s\" 1 2)")))
  (is (empty? (lint! "(format \"%s %2$ %<s %s\" 1 2)")))
  (is (empty? (lint! "(defn foo [x] (format x 1))")))
  (is (empty? (lint! "
(ns foo {:clj-kondo/config '{:linters {:format {:level :off}}}})
(format \"%s\" 1 2)")))
  (is (empty? (lint! "(format \"%n %n %% %s\" 1)")))
  (is (empty? (lint! "(format \"Syntax error reading source at (%s).%n%s%n\" 1 2)")))
  (is (empty? (lint! "(format \"/blah/%s/blah?query=Luke%%20Skywalker\" 1)")))
  (is (empty? (lint! "(require '[clojure.tools.logging :as l]) (defn foo [x] (l/errorf (ex-info \"\" {}) x \"x\" 1 2))")))
  (is (empty? (lint! "

(format \"
Usage: `%1$s action [arg*]`

Examples:
`%1$s foo
`%1$s bar abc
`%1$s baz`\" 'cmd)")))
  (is (empty? (lint! "(format \"%%%s\" \"foo\")"))))
