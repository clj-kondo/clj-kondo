(ns clj-kondo.impl.topo-sort-test
  (:require [clj-kondo.impl.core :as impl-core]
            [clj-kondo.test-utils :refer [lint!]]
            [clojure.java.io :as io]
            [clojure.test :as t :refer [deftest is testing]]))

(def ^:private read-ns-decl @#'impl-core/read-ns-decl)
(def ^:private topo-sort-sources @#'impl-core/topo-sort-sources)

(deftest read-ns-decl-test
  (testing "basic ns form"
    (is (= '(ns foo.bar (:require [baz.qux]))
           (read-ns-decl "(ns foo.bar (:require [baz.qux]))"))))
  (testing "no ns form"
    (is (nil? (read-ns-decl "(defn foo [] 1)"))))
  (testing "with docstring"
    (let [decl (read-ns-decl "(ns foo.bar \"docstring\" (:require [baz]))")]
      (is (= 'foo.bar (second decl))))))

(deftest topo-sort-sources-test
  (testing "empty sources"
    (is (= [] (topo-sort-sources []))))
  (testing "single source"
    (let [sources [{:source "(ns a)" :filename "a.clj"}]]
      (is (= sources (topo-sort-sources sources)))))
  (testing "two sources, dependency ordering"
    (let [a {:source "(ns a)" :filename "a.clj"}
          b {:source "(ns b (:require [a]))" :filename "b.clj"}]
      ;; b depends on a, so a should come first regardless of input order
      (is (= [a b] (topo-sort-sources [a b])))
      (is (= [a b] (topo-sort-sources [b a])))))
  (testing "three-level chain"
    (let [a {:source "(ns a)" :filename "a.clj"}
          b {:source "(ns b (:require [a]))" :filename "b.clj"}
          c {:source "(ns c (:require [b]))" :filename "c.clj"}]
      (is (= [a b c] (topo-sort-sources [c b a])))))
  (testing "diamond dependency"
    (let [a {:source "(ns a)" :filename "a.clj"}
          b {:source "(ns b (:require [a]))" :filename "b.clj"}
          c {:source "(ns c (:require [a]))" :filename "c.clj"}
          d {:source "(ns d (:require [b] [c]))" :filename "d.clj"}
          result (topo-sort-sources [d c b a])
          idx (into {} (map-indexed (fn [i s] [(:filename s) i])) result)]
      ;; a before b and c, b and c before d
      (is (< (idx "a.clj") (idx "b.clj")))
      (is (< (idx "a.clj") (idx "c.clj")))
      (is (< (idx "b.clj") (idx "d.clj")))
      (is (< (idx "c.clj") (idx "d.clj")))))
  (testing "source without ns form preserved"
    (let [a {:source "(ns a)" :filename "a.clj"}
          edn {:source "{:a 1}" :filename "config.edn"}
          b {:source "(ns b (:require [a]))" :filename "b.clj"}
          result (topo-sort-sources [b edn a])]
      ;; a must come before b
      (is (< (.indexOf result a) (.indexOf result b)))
      ;; edn should still be present
      (is (some #{edn} result))))
  (testing "cyclic dependency handled gracefully"
    (let [a {:source "(ns a (:require [b]))" :filename "a.clj"}
          b {:source "(ns b (:require [a]))" :filename "b.clj"}]
      ;; should not throw, returns all sources
      (is (= 2 (count (topo-sort-sources [a b]))))))
  (testing "external deps are ignored"
    (let [a {:source "(ns a (:require [clojure.string :as str]))" :filename "a.clj"}
          b {:source "(ns b (:require [a]))" :filename "b.clj"}]
      (is (= [a b] (topo-sort-sources [b a])))))
  (testing "prefix list"
    (let [a-str {:source "(ns a.core)" :filename "a/core.clj"}
          b-str {:source "(ns b.core (:require [a [core]]))" :filename "b/core.clj"}]
      (is (= [a-str b-str] (topo-sort-sources [b-str a-str])))))
  (testing "use clause"
    (let [a {:source "(ns a)" :filename "a.clj"}
          b {:source "(ns b (:use a))" :filename "b.clj"}]
      (is (= [a b] (topo-sort-sources [b a]))))))

(deftest topo-sort-toggle-test
  (testing "topo sort can be disabled via dynamic var"
    (binding [impl-core/*topo-sort* false]
      (let [b {:source "(ns b (:require [a]))" :filename "b.clj"}
            a {:source "(ns a)" :filename "a.clj"}]
        (is (= [b a] (topo-sort-sources [b a])))))))

(deftest ns-analysis-benefits-from-topo-sort-test
  (let [corpus (io/file "corpus" "topo_sort")
        config-dir (.getPath (io/file corpus ".clj-kondo"))]
    (testing "with topo sort, ns-analysis finds the lib (no false positive)"
      (is (empty? (lint! corpus "--cache" "false" "--config-dir" config-dir))))
    (testing "without topo sort, ns-analysis can't find the lib (false positive)"
      (binding [impl-core/*topo-sort* false]
        (is (seq (lint! corpus "--cache" "false" "--config-dir" config-dir)))))))
