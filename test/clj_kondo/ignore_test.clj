(ns clj-kondo.ignore-test
  (:require [clj-kondo.test-utils :refer [lint!]]
            [clojure.test :as t :refer [deftest is testing]]))

(deftest ignore-test
  (is (empty? (lint! "#_:clj-kondo/ignore (inc :foo)"
                     {:linters {:type-mismatch {:level :warning}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore (inc 1 2 3)"
                     {:linters {:type-mismatch {:level :warning}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore (defn foo [] (inc 1 2 3))"
                     {:linters {:type-mismatch {:level :warning}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore (defn foo [] x) x x x"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(require 'foo) (foo/my-bindings #_:clj-kondo/ignore [x1 1 x2 3] x1)"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore (\"foo\")"
                     {:linters {:not-a-function {:level :error}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore {:A}"
                     {:linters {:syntax {:level :error}}})))
  (is (empty? (lint! "(let [#_:clj-kondo/ignore x 1])"
                     {:linters {:unused-binding {:level :warning}}}))))

(deftest positional-checks-test
  (is (seq (lint! "#_:clj-kondo/ignore (defn foo []) x"
                  {:linters {:unresolved-symbol {:level :error}}})))
  (is (seq (lint! "x #_:clj-kondo/ignore (defn foo [])"
                  {:linters {:unresolved-symbol {:level :error}}})))
  (is (seq (lint! "#_:clj-kondo/ignore
(defn foo []
) x"
                  {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "#_:clj-kondo/ignore
(defn foo []
x)"
                     {:linters {:unresolved-symbol {:level :error}}}))))

(deftest fine-grained-ignores-test
  (is (empty? (lint! "#_{:clj-kondo/ignore [:invalid-arity]} (inc 1 2 3)"
                     {:linters {:invalid-arity {:level :error}}})))
  (is (seq (lint! "#_{:clj-kondo/ignore [:type-mismatch]} (inc 1 2 3)"
                  {:linters {:invalid-arity {:level :error}}})))
  (is (empty? (lint! "#_{:clj-kondo/ignore [:type-mismatch]} (inc :foo)"
                     {:linters {:invalid-arity {:level :error}}})))
  (is (seq (lint! "#_{:clj-kondo/ignore [:type-mismatch]} (inc :foo 1)"
                  {:linters {:invalid-arity {:level :error}}})))
  (is (empty? (lint! "#_{:clj-kondo/ignore [:redundant-do :redundant-let]}
                     (do (let [x 1] (let [y 2] [x y])))"
                     {:linters {:invalid-arity {:level :error}}})))
  (is (seq (lint! "#_{:clj-kondo/ignore [:redundant-do :redundant-let]}
                     (do (let [x 1] (let [y 2] [x y])) (inc))"
                  {:linters {:invalid-arity {:level :error}}}))))

;; TODO: (let [#_:clj-kondo/ignore x 1])



