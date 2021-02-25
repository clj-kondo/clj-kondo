(ns clj-kondo.redundant-negation-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :refer [deftest is testing]]))

(deftest redundant-negation-test
  (testing "`and` & `or` with `not`s can be simplified"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "And & 2 nots used instead of 1 not with or"})
       (lint! "(and (not :foo) (not :bar))" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "Or & 3 nots used instead of 1 not with and"})
       (lint! "(or (not :foo) (not :bar) (not :baz))" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (is (empty? (lint! "(or) (and)" {:linters {:redundant-negation {:level :warning}}}))
        "'Or' and 'and' without args isn't a problem")
    (is (empty? (lint! "(and (not :foo) (not :bar) :baz)" {:linters {:redundant-negation {:level :warning}}}))
        "If any arg supplied is not a list with first element 'not', then the call is fine"))

  (testing "negated `nil?` can be simplified to `some?`"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and nil? used instead of some?"})
       (lint! "(not (nil? :foo))" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and nil? used instead of some?"})
       (lint! "(comp not nil?)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "complement and nil? used instead of some?"})
       (lint! "(complement nil?)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}}))))

  (testing "negated `some?` can be simplified to `nil?`"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and some? used instead of nil?"})
       (lint! "(not (some? :foo))" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and some? used instead of nil?"})
       (lint! "(comp not some?)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "complement and some? used instead of nil?"})
       (lint! "(complement some?)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}}))))

  (testing "negated `=` can be simplified to `not=`"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and = used instead of not="})
       (lint! "(not (= :foo :bar))" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and = used instead of not="})
       (lint! "(comp not =)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "complement and = used instead of not="})
       (lint! "(complement =)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}}))))

  (testing "negated `even?` can be simplified to `odd?`"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and even? used instead of odd?"})
       (lint! "(not (even? 42))" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and even? used instead of odd?"})
       (lint! "(comp not even?)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "complement and even? used instead of odd?"})
       (lint! "(complement even?)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}}))))

  (testing "negated `odd?` can be simplified to `even?`"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and odd? used instead of even?"})
       (lint! "(not (odd? 42))" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and odd? used instead of even?"})
       (lint! "(comp not odd?)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "complement and odd? used instead of even?"})
       (lint! "(complement odd?)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}}))))

  (testing "negated `seq` can be simplified to `empty?`"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and seq used instead of empty?"})
       (lint! "(not (seq [:foo :bar :baz]))" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and seq used instead of empty?"})
       (lint! "(comp not seq)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "complement and seq used instead of empty?"})
       (lint! "(complement seq)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}}))))

  (testing "negated `some` can be simplified to `not-any?`"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and some used instead of not-any?"})
       (lint! "(not (some string? [:foo :bar :baz]))" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and some used instead of not-any?"})
       (lint! "(comp not some)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "complement and some used instead of not-any?"})
       (lint! "(complement some)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}}))))

  (testing "negated `every?` can be simplified to `not-every?`"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and every? used instead of not-every?"})
       (lint! "(not (every? string? [:foo :bar :baz]))" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "not and every? used instead of not-every?"})
       (lint! "(comp not every?)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "complement and every? used instead of not-every?"})
       (lint! "(complement every?)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}}))))

  (testing "`if-not` or `when-not` used when `if` or `when` can be used"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "if-not and some? used instead of if and nil?"})
       (lint! "(if-not (some? :foo) :a :b)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "when-not and odd? used instead of when and even?"})
       (lint! "(when-not (odd? 2) :b)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}}))))

  (testing "`filter` & `complement` used instead of `remove` or vice versa"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "filter and complement used instead of remove"})
       (lint! "(filter (complement foo) [:bar :baz])" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "remove and complement used instead of filter"})
       (lint! "(remove (complement foo) [:bar :baz])" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}}))))

  (testing "`filter` & `comp` with `not` used instead of `remove` or vice versa"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "filter and comp with not used instead of remove"})
       (lint! "(filter (comp not foo) [:bar :baz])" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "remove and comp with not used instead of filter"})
       (lint! "(remove (comp not foo) [:bar :baz])" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}}))))

  (testing "`filter` & fn with `not` wrapping the rest of the body used instead of `remove` or vice versa"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "filter and not used instead of remove"})
       (lint! "(filter #(not (:foo %)) [:bar :baz])" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "filter and not used instead of remove"})
       (lint! "(filter (fn [x] (not (:foo x))) [:bar :baz])" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 18,
          :level :warning,
          :message "filter and not used instead of remove"})
       (lint! "(->> [:bar :baz] (filter (fn [x] (not (:foo x)))))" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "remove and not used instead of filter"})
       (lint! "(remove #(not (:foo %)) [:bar :baz])" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "remove and not used instead of filter"})
       (lint! "(remove (fn [x] (not (:foo x))) [:bar :baz])" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}}))))

  (testing "`if` or `when` with `not` used instead of `if-not` or `when-not` respectively"
    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "if and not used instead of if-not"})
       (lint! "(if (not :foo) :bar :baz)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (doseq [lang ["clj" "cljs"]]
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :warning,
          :message "when and not used instead of when-not"})
       (lint! "(when (not :foo) :bar)" "--lang" lang
              "--config" {:linters {:redundant-negation {:level :warning}}})))

    (is (empty? (lint! "(if (not= 7 42) :foo :bar)" {:linters {:redundant-negation {:level :warning}}}))
        "If with any other call starting the test form is ok")
    (is (empty? (lint! "(when (or (not :foo) :bar) :bar)" {:linters {:redundant-negation {:level :warning}}}))
        "When with any other call starting the test form is ok")))
