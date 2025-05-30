(ns clj-kondo.missing-protocol-method-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(deftest single-alias-test
  (assert-submaps2 '({:file "<stdin>", :row 8, :col 3, :level :warning, :message "Missing protocol method(s): foo, bar"}
                     {:file "<stdin>", :row 11, :col 3, :level :warning, :message "Missing protocol method(s): bar"}
                     {:file "<stdin>", :row 20, :col 3, :level :warning, :message "Missing protocol method(s): foo, bar"}
                     {:file "<stdin>", :row 23, :col 3, :level :warning, :message "Missing protocol method(s): bar"})
                   (lint! "(ns scratch)

(defprotocol IDude
  (foo [_])
  (bar [_]))

(defrecord Foo []
  IDude)

(defrecord Dude []
  IDude
  (foo [_]))

(defrecord Dude2 []
  IDude
  (foo [_])
  (bar [_]))

(deftype FooT []
  IDude)

(deftype DudeT []
  IDude
  (foo [_]))

(deftype Dude2T []
  IDude
  (foo [_])
  (bar [_]))
"))
  (assert-submaps2 '({:file "<stdin>", :row 8, :col 5, :level :warning, :message "Missing protocol method(s): foo, bar"}
                     {:file "<stdin>", :row 12, :col 5, :level :warning, :message "Missing protocol method(s): bar"}

                     {:file "<stdin>", :row 27, :col 3, :level :warning, :message "Missing protocol method(s): foo, bar"}
                     {:file "<stdin>", :row 31, :col 3, :level :warning, :message "Missing protocol method(s): bar"}

                     {:file "<stdin>",
                      :row 44,
                      :col 3,
                      :level :warning,
                      :message "Missing protocol method(s): foo, bar"}
                     {:file "<stdin>",
                      :row 47,
                      :col 3,
                      :level :warning,
                      :message "Missing protocol method(s): bar"})
                   (lint!
                    "(ns scratch)

(defprotocol IDude
  (foo [_])
  (bar [_]))

(extend-protocol
    IDude
  Number)

(extend-protocol
    IDude
  Number
  (foo [this]
    (recur this)))

(extend-protocol
    IDude
  Number
  (foo [this]
    (recur this))
  (bar [this]
    (recur this)))

(extend-type
    Number
  IDude)

(extend-type
    Number
  IDude
  (foo [this]
    (recur this)))

(extend-type
    Number
  IDude
  (foo [this]
    (recur this))
  (bar [this]
    (recur this)))

(reify
  IDude)

(reify
  IDude
  (foo [_]))

(reify
  IDude
  (foo [_])
  (bar [_]))")))

(deftest ignore-hint-test
  (is (empty? (lint!
              "(ns scratch)

(defprotocol IDude
  (foo [_])
  (bar [_]))

(defrecord Foo []
  #_:clj-kondo/ignore IDude)

(defrecord Dude []
  #_:clj-kondo/ignore IDude
  (foo [_]))

(defrecord Dude2 []
  IDude
  (foo [_])
  (bar [_]))
"))))

(deftest config-in-ns-test
  (is (empty? (lint! "(ns repro)

(defprotocol IFoo
  (foo [_])
  (bar [_]))

(defrecord Foo []
  IFoo
  (foo [_]
    ;; trigger the :unused-value linter so we can verify :config-in-ns settings
    (do
      :foo
      :bar)))"
                     '{:config-in-ns {repro {:linters {:missing-protocol-method {:level :off}
                                                       :unused-value {:level :off}}}}} ))))


(deftest ns-groups-test
  (is (empty? (lint! "(ns foo-test)

(defprotocol IFoo
  (foo [_])
  (bar [_]))

(defrecord Foo []
  IFoo
  (foo [_]
    ;; trigger the :unused-value linter so we can verify :config-in-ns settings
    (do
      :foo
      :bar)))"
                     '{:ns-groups [{:pattern ".*-test$" :name test-namespaces}]
                       :config-in-ns {test-namespaces {:linters {:missing-protocol-method {:level :off}
                                                                 :unused-value {:level :off}}}}} ))))

(deftest ignore-prefix-test
  (is (empty? (lint! "

(ns protocols)

(defprotocol InlineValue (sqlize [_]))

(require '[protocols :as p])

(extend-protocol p/InlineValue
  nil
  (p/sqlize [_] \"NULL\"))"))))
