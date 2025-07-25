(ns clj-kondo.redundant-ignore-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2]]
            [clojure.test :refer [deftest is testing]]))

(def config {:linters {:redundant-ignore {:level :warning}
                       :unresolved-protocol-method {:level :warning}
                       :missing-protocol-method {:level :warning}
                       :unresolved-symbol {:level :error}}})

(deftest redundant-ignore-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 3, :level :warning, :message "Redundant ignore"})
   (lint! "#_:clj-kondo/ignore (+ 1 2 3)" config)))

(deftest redundant-ignore-unused-private-var-test
  (assert-submaps2
   []
   (lint! "#_{:clj-kondo/ignore [:unused-private-var]}
(defn- -debug [& strs]
  (.println System/err
            (with-out-str
              (apply println strs))))"
          config)))

(deftest redundant-ignore-exclude-test
  (is (empty?
       (lint! "#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var :unused-binding]} (defn foo [])"
              (assoc-in config [:linters :redundant-ignore :exclude] [:clojure-lsp/unused-public-var])))))

(deftest redundant-ignore-on-missing-and-unresolved-protocol-test
  (is (empty? (lint! "(defprotocol IFoo
  (dude [_]))

(defrecord MyFoo []
  #_:clj-kondo/ignore IFoo
  (#_:clj-kondo/ignore dudex [_]))"
                     config))))

(deftest cljc-test
  (is (empty? (lint! "#_{:clj-kondo/ignore #?(:cljs [:unresolved-symbol])}
(defn foo []
  [#?(:cljs z)]) ;; x is only used in cljs, but unused is ignored for clj, so no warning
"
                     config "--filename" "foo.cljc"))))
