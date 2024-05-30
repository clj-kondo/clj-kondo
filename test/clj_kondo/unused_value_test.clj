(ns clj-kondo.unused-value-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps2] :rename {assert-submaps2 assert-submaps}]
   [clojure.test :refer [deftest is testing]]))

(deftest unused-value-simple-expressions-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 5, :level :warning, :message "Unused value: 1"})
   (lint! "(do 1 2) " {:linters {:unused-value {:level :warning}}}))
  (assert-submaps '({:file "<stdin>", :row 2, :col 6, :level :warning, :message "Unused value: 1"}
                    {:file "<stdin>", :row 2, :col 8, :level :warning, :message "Unused value: 2"})
                  (lint! "
(try 1 2 3 (catch Exception _))"
                         {:linters {:unused-value {:level :warning}}}))
  (is (empty?
       (lint! "(fn [] (:foo {}))" {:linters {:unused-value {:level :warning}}})))
  (is (empty?
       (lint! "(map
 (fn [{:keys [:vars :proxied-namespaces]}]
   (assoc {:vars vars}
          :proxied-namespaces proxied-namespaces))
 [])" {:linters {:unused-value {:level :warning}}})))
  (is (empty? (lint! "
(let [f #(+ % %2)] (f 1 2))" {:linters {:unused-value {:level :warning}}})))
  (is (empty? (lint! "
(binding [*print-level* false
          *print-meta* false])" {:linters {:unused-value {:level :warning}}})))
  (is (empty? (lint! "
(when-not (:protocol-symbol var)
  (cljs.analyzer/warning :invalid-protocol-symbol {}))"
                     {:linters {:unused-value {:level :warning}
                                :unresolved-namespace {:level :off}}})))
  (is (empty? (lint! "
(do
  (let [_res (try nil (catch :default _))])
  nil)"
                     {:linters {:unused-value {:level :warning}}})))
  (is (empty? (lint! "
(do ((fn [_ _]) true false) true)
" {:linters {:unused-value {:level :warning}}})))
  (is (empty? (lint! "
(do (true 1 2) 1)
(do (\"foo\" 1 2) 1)
(do (\\a 1 2) 1)
(do (1 1 2) 1)
"
                     {:linters {:unused-value {:level :warning}
                                :not-a-function {:level :off}}})))
  (is (empty? (lint! "
(do (js/download 1 \"data-sources.csv\" \"text/csv\")
    12)
" {:linters {:unused-value {:level :warning}}} "--lang" "cljs")))
  (is (empty? (lint! "
(defn fun
  [{:keys [a-key]}]
  (when-let [{:keys [a-deep]} a-key] a-deep)
  (println a-key))
" {:linters {:unused-value {:level :warning}}})))
  (is (empty? (lint! "
(declare member)
(do (clojure.core/defn disjoint?-impl [t1 t2 default]
      {:post [(member % (quote (true false :dont-know)))]}
      (list t1 t2 default))
    (declare disjoint?))" {:linters {:unused-value {:level :warning}}}))))

(deftest unused-value-calls-test
  (testing "unused pure function call"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 14, :level :warning, :message "Unused value"})
     (lint! "(defn foo [] (update {} :dude 1) 1)" {:linters {:unused-value {:level :warning}}})))
  (testing "unused lazy seq"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 14, :level :warning, :message "Unused value"})
     (lint! "(defn foo [] (map inc [1 2 3]) 1)" {:linters {:unused-value {:level :warning}}})))
  (testing "unused transient call"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 14, :level :warning, :message "Unused value"})
     (lint! "(defn foo [] (assoc! (transient {}) :dude 1) 1)" {:linters {:unused-value {:level :warning}}})))
  (testing "unused in doseq"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 20, :level :warning, :message "Unused value"})
     (lint! "(doseq [x [1 2 3]] (assoc! (transient {}) x 1))" {:linters {:unused-value {:level :warning}}}))))

(deftest unused-value-var-refs-test
  (testing "unused var ref"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 14, :level :warning, :message "Unused value: update"})
     (lint! "(defn foo [] update 1)" {:linters {:unused-value {:level :warning}}})))
  (testing "unused local ref"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 15, :level :warning, :message "Unused value: x"})
     (lint! "(defn foo [x] x 1)" {:linters {:unused-value {:level :warning}}}))))

(deftest clojure-test-docstring-test
  (doseq [lang ["clj" "cljc"]
          code ["(ns test
  (:require [clojure.test :as t :refer [deftest is]]))

(deftest foo
  (+ 1 2 3)
  (is (= 1 2)))"
                "(ns test
  (:require [clojure.test :as t :refer [deftest is]]))

(deftest foo
  \"dude\"
  (is (= 1 2)))"]]
    (assert-submaps
     [{:file "<stdin>",
       :row 5,
       :col 3,
       :level :warning,
       :message #"Unused value"}]
     (lint! code {:linters {:unused-value {:level :warning}
                            :missing-test-assertion {:level :off}}}
            "--lang" lang))))

(deftest issue-2164-test
  (is (empty? (lint! "(ns repro
  (:require
    [clojure.test :refer [deftest is]]))

(let [some-var \"some-value\"]
  (deftest this-one-is-flagged-as-unused
    (is (= some-var \"some-value\")))

  (deftest this-one-is-fine
    (is (= some-var \"some-value\"))))"
                     {:linters {:unused-value {:level :warning}}}))))

(deftest issue-2251-test
  (assert-submaps
   '({:file "<stdin>", :row 6, :col 5, :level :warning, :message "Unused value"}
     {:file "<stdin>", :row 6, :col 15, :level :warning, :message "Unused value: :="})
   (lint! "(ns repl.sample.rcf
  (:require
    [hyperfiddle.rcf :refer [tests]]))

(let [xs (map identity xs)]
    (last xs) := :c)

(tests
  \"same piece taken from rcf readme\"
  (let [xs (map identity xs)]
    (last xs) := :c))"
              '{:linters {:unused-value {:level :warning}}
                :config-in-call {hyperfiddle.rcf/tests {:linters {:unresolved-symbol {:level :off}
                                                                  :unused-value {:level :off}}}}})))

(deftest issue-2304-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 20, :level :warning, :message "Unused value: \"hello\""}
     {:file "<stdin>", :row 1, :col 28, :level :warning, :message "Unused value"})
   (lint! "(defn foo [bar] {} \"hello\" (+ 2 2) bar)"
          {:linters {:unused-value {:level :warning}}})))

(deftest issue-2309-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 14, :level :warning, :message "Unused value"})
   (lint! "(defn foo [] (for [x [1 2 3]] x) nil)"
          {:linters {:unused-value {:level :warning}}})))

(deftest issue-2335-test
  (is (empty?
       (lint! "(do (require '[clojure.tools.reader :as ctr]) (ctr/read) :ok)"
              {:linters {:unused-value {:level :warning}}})))
  (is (empty?
       (lint! "(do (require '[clojure.tools.reader.edn :as ctr.edn]) (ctr.edn/read) :ok)"
              {:linters {:unused-value {:level :warning}}}))))
