(ns clj-kondo.bindings-test
  (:require [clj-kondo.test-utils :refer [assert-submaps
                                          assert-submaps2
                                          lint!]]
            [clojure.test :refer [deftest is testing]]
            [missing.test.assertions]))

(deftest unused-binding-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 7, :level :warning, :message "unused binding x"})
   (lint! "(let [x 1])" '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 12,
      :level :warning,
      :message "unused binding x"})
   (lint! "(defn foo [x])"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 15,
      :level :warning,
      :message "unused binding id"})
   (lint! "(let [{:keys [patient/id order/id]} {}] id)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 14,
      :level :warning,
      :message "unused binding a"}
     {:file "<stdin>",
      :row 1,
      :col 23,
      :level :warning,
      :message "unused default for binding a"})
   (lint! "(fn [{:keys [:a] :or {a 1}}])"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 8,
      :level :warning,
      :message "unused binding x"}
     {:file "<stdin>",
      :row 1,
      :col 12,
      :level :warning,
      :message "unused binding y"})
   (lint! "(loop [x 1 y 2])"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 10,
      :level :warning,
      :message "unused binding x"})
   (lint! "(if-let [x 1] 1 2)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 11,
      :level :warning,
      :message "unused binding x"})
   (lint! "(if-some [x 1] 1 2)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 12,
      :level :warning,
      :message "unused binding x"})
   (lint! "(when-let [x 1] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 13,
      :level :warning,
      :message "unused binding x"})
   (lint! "(when-some [x 1] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :level :warning,
      :message "unused binding x"})
   (lint! "(for [x []] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :level :warning,
      :message "unused binding x"})
   (lint! "(doseq [x []] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:level :warning,
      :message "unused binding x"}
     {:level :warning,
      :message "unused binding y"})
   (lint! "(with-open [x ? y ?] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:level :warning,
      :message "unused binding x"})
   (lint! "(with-local-vars [x 1] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :warning,
      :message "unused binding x"}
     {:file "<stdin>",
      :row 1,
      :col 22,
      :level :warning,
      :message "unused binding y"}
     {:file "<stdin>",
      :row 1,
      :col 33,
      :level :error,
      :message "clojure.core/inc is called with 0 args but expects 1"}
     {:file "<stdin>",
      :row 1,
      :col 46,
      :level :error,
      :message "clojure.core/pos? is called with 0 args but expects 1"})
   (lint! "(for [x [] :let [x 1 y x] :when (inc) :while (pos?)] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 48,
      :level :warning,
      :message "unused binding a"}
     {:file "<stdin>",
      :row 1,
      :col 52,
      :level :warning,
      :message "unused binding b"})
   (lint! "(ns foo (:require [cats.core :as c])) (c/mlet [a 1 b 2])"
          '{:linters {:unused-binding {:level :warning}}
            :lint-as {cats.core/mlet clojure.core/let}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 24,
      :level :warning,
      :message "unused binding x"})
   (lint! "(defmacro foo [] (let [x 1] `(inc x)))"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 12,
      :level :warning,
      :message "unused binding x"})
   (lint! "(defn foo [x] (quote x))"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 17,
      :level :warning,
      :message "unused binding variadic"})
   (lint! "(let [{^boolean variadic :variadic?} {}] [])"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 8,
      :level :warning,
      :message "unused binding a"})
   (lint! "#(let [a %])"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :warning,
      :message "unused binding a"})
   (lint! "(let [a 1] `{:a 'a})"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,:col 36,
      :level :warning,
      :message "unused binding b"})
   (lint! "(defmulti descriptive-multi (fn [a b] a))"
          '{:linters {:unused-binding
                      {:level :warning}}}))
  (is (empty? (lint! "(let [{:keys [:a :b :c]} 1 x 2] (a) b c x)"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(defn foo [x] x)"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(defn foo [_x])"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(fn [{:keys [x] :or {x 1}}] x)"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "#(inc %1)"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(let [exprs []] (loop [exprs exprs] exprs))"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(for [f fns :let [children (:children f)]] children)"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(deftype Foo [] (doseq [[key f] []] (f key)))"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(defmacro foo [] (let [x 1] `(inc ~x)))"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(let [[_ _ name] nil]
                        `(cljs.core/let [~name ~e] ~@cb))"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(defmacro foo [] (let [x 1] `(inc ~@[x])))"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(defn false-positive-metadata [a b] ^{:key (str a b)} [:other])"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(doseq [{ts :tests {:keys [then]} :then} nodes]
                        (doseq [test (map :test ts)] test)
                        then)"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(let [a 1] (cond-> (.getFoo a) (some? y) x))"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(defmacro foo [] (let [sym 'my-symbol] `(do '~sym)))"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(let [s 'clojure.string] (require s))"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(defn f [{:keys [:a :b :c]}] a)"
                     '{:linters {:unused-binding
                                 {:level :warning
                                  :exclude-destructured-keys-in-fn-args true}
                                 :unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(ns problem {:clj-kondo/config {:linters {:unused-binding {:level :off}}}})
(defn f [x] (println))"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(defmulti descriptive-multi (fn [a b] a))"
                     '{:linters {:unused-binding
                                 {:level :warning
                                  :exclude-defmulti-args true}}}
                     "--lang" "cljc")))
  (is (empty?  (lint! "(let [_x 0 {:keys [a b] :as _c} v]  [a b _x _c])"
                      '{:linters {:used-underscored-binding {:level :off}}})))
  (is (empty? (lint! "(doto (Object.) (.method))"
                     '{:linters {:used-underscored-binding {:level :warning}}})))
  (is (empty? (lint! "(defmulti foo (fn [this x] x))"
                     '{:linters {:unused-binding {:level :warning :exclude-patterns ["this"]}}})))
  (testing "issue 2713: quote-unquote should register binding usage"
    (is (empty? (lint! "(defmacro evaluator [expr]
                         `(fn [& args#]
                            (eval `(~'~expr ~@args#))))"

                       '{:linters {:unused-binding {:level :warning}}})))
    (is (empty? (lint! "(defn foo [location opts]
                         (let [opts' opts]
                           `(load-string* ~location '[clojure.data.json]
                                          `(fn [~'~'s] (clojure.data.json/read-str ~'~'s ~@~@opts')))))"
                       {:linters {:unused-binding {:level :warning}}}))))

  (testing "Should still warn for truly unused bindings"
    (assert-submaps2
     '({:file "<stdin>"
        :row 1
        :col 27
        :level :warning
        :message "unused binding unused"})
     (lint! "(defmacro partially-used [unused used] `(println ('~used)))"
            '{:linters {:unused-binding {:level :warning}}}))))



(deftest unused-destructuring-default-test
  (doseq [input ["(let [{:keys [:i] :or {i 2}} {}])"
                 "(let [{:or {i 2} :keys [:i]} {}])"
                 "(let [{:keys [:i :j] :or {i 2 j 3}} {}] j)"]]
    (assert-submaps '({:file "<stdin>"
                       :row 1
                       :level :warning
                       :message "unused binding i"}
                      {:file "<stdin>"
                       :row 1
                       :level :warning
                       :message "unused default for binding i"})
                    (lint! input
                           '{:linters
                             {:unused-binding {:level :warning}}})))
  (testing "finding points at the symbol of the default"
    (assert-submaps2 '({:file "<stdin>"
                        :row 1
                        :col 15
                        :level :warning
                        :message "unused binding i"}
                       {:file "<stdin>"
                        :row 1
                        :col 24
                        :level :warning
                        :message "unused default for binding i"})
                     (lint! "(let [{:keys [:i] :or {i 2}} {}] nil)"
                            '{:linters
                              {:unused-binding {:level :warning}}})))
  (testing "respects :exclude-destructured-keys-in-fn-args setting "
    (is (empty? (lint! "(defn f [{:keys [:a] :or {a 1}}] nil)"
                       '{:linters {:unused-binding
                                   {:level :warning
                                    :exclude-destructured-keys-in-fn-args true}}}))))
  (testing "respects :exclude-destructured-as "
    (is (empty? (lint! "(defn f [{:keys [:a] :as config}] a)"
                       '{:linters {:unused-binding
                                   {:level :warning
                                    :exclude-destructured-as true}}})))
    (is (empty? (lint! "(defn f [[a :as config]] a)"
                       '{:linters {:unused-binding
                                   {:level :warning
                                    :exclude-destructured-as true}}})))
    (testing "still shows unused bindings not in as "
      (assert-submaps2 '({:file "<stdin>"
                          :row 1
                          :col 18
                          :level :warning
                          :message "unused binding a"})
                       (lint! "(defn f [{:keys [:a] :as config}] nil)"
                              '{:linters {:unused-binding
                                          {:level :warning
                                           :exclude-destructured-as true}}}))
      (assert-submaps2 '({:file "<stdin>"
                          :row 1
                          :col 18
                          :level :warning
                          :message "unused binding a"})
                       (lint! "(defn f [{:keys [:a] :as config}] config)"
                              '{:linters {:unused-binding
                                          {:level :warning
                                           :exclude-destructured-as true}}}))
      (assert-submaps2 '({:file "<stdin>"
                          :row 1
                          :col 10
                          :level :warning
                          :message "unused binding x"}
                         {:file "<stdin>"
                          :row 1
                          :col 12
                          :level :warning
                          :message "unused binding y"}
                         {:file "<stdin>"
                          :row 1
                          :col 14
                          :level :warning
                          :message "unused binding z"}
                         {:file "<stdin>"
                          :row 1
                          :col 24
                          :level :warning
                          :message "unused binding a"})
                       (lint! "(defn f [x y z {:keys [:a] :as g}] g)"
                              '{:linters {:unused-binding
                                          {:level :warning
                                           :exclude-destructured-as true}}})))))


(deftest used-underscored-binding-test
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :warning,
      :message "Used binding is marked as unused: _x"}
     {:file "<stdin>",
      :row 1,
      :col 29,
      :level :warning,
      :message "Used binding is marked as unused: _c"})
   (lint! "(let [_x 0 {:keys [a b] :as _c} v]  [a b _x _c])"
          '{:linters {:used-underscored-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :warning,
      :message "Used binding is marked as unused: _"})
   (lint! "(let [_ 1] _)"
          '{:linters {:used-underscored-binding {:level :warning}}}))
  (assert-submaps2
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :warning,
      :message "Used binding is marked as unused: _x"})
   (lint! "(let [_x 0 {:keys [a _bar] :as _c} v]  [a _bar _x _c])"
          '{:linters {:used-underscored-binding {:level :warning
                                                 :exclude [_c "^_b.*$"]}}}))
  (is (empty?  (lint! "(let [_x 0 {:keys [a b] :as _c} v]  [a b _x _c])"
                      '{:linters {:used-underscored-binding {:level :off}}})))
  (is (empty? (lint! "(doto (Object.) (.method))"
                     '{:linters {:used-underscored-binding {:level :warning}}}))))

(deftest issue-2046-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 26, :level :error, :message "duplicate key :or"})
   (lint! "(let [{:keys [_x] :or {} :or {}} nil])")))

(deftest issue-2747-test
  (testing "Gensym bindings in nested syntax quotes should not cause unresolved symbol errors"
    (is (empty? (lint! "(defmacro def-some-macro [] `(defmacro ~'some-macro [x#] `(list ~x#)))"
                       {:linters {:unresolved-symbol {:level :error}}})))
    (is (empty? (lint! "(defmacro outer [] `(defmacro ~'inner [a# b#] `(+ ~a# ~b#)))"
                       {:linters {:unresolved-symbol {:level :error}}})))))

