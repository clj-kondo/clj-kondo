(ns clj-kondo.analysis.keywords-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.utils :refer [err]]
   [clj-kondo.test-utils :refer [assert-submaps]]
   [clojure.edn :as edn]
   [clojure.test :as t :refer [deftest is testing]]))

(defn analyze
  ([code config]
   (:analysis
    (with-in-str code
      (clj-kondo/run! (merge {:lint ["-"]
                              :config {:output {:canonical-paths true}
                                       :analysis {:keywords true}}}
                             config))))))

(deftest keyword-analysis-test
  (testing "standalone keywords with top-level require"
    (let [a (analyze "(require '[bar :as b]) :kw :x/xkwa ::x/xkwb ::fookwa :foo/fookwb ::foo/fookwc :bar/barkwa ::b/barkwb ::bar/barkwc"
                     {:config {:analysis {:keywords true}}})]
      (assert-submaps
       '[{:name "as"}
         {:name "kw"}
         {:name "xkwa" :ns x}
         {:name "xkwb" :ns :clj-kondo/unknown-namespace}
         {:name "fookwa" :ns user}
         {:name "fookwb" :ns foo}
         {:name "fookwc" :ns :clj-kondo/unknown-namespace}
         {:name "barkwa" :ns bar}
         {:name "barkwb" :ns bar :alias b}
         {:name "barkwc" :ns :clj-kondo/unknown-namespace}]
       (:keywords a))))
  (testing "standalone keywords"
    (let [a (analyze "(ns foo (:require [bar :as b])) :kw :2foo :x/xkwa ::x/xkwb ::fookwa :foo/fookwb ::foo/fookwc :bar/barkwa ::b/barkwb ::bar/barkwc"
                     {:config {:analysis {:keywords true}}})]
      (assert-submaps
       '[{:name "require"}
         {:name "as"}
         {:name "kw"}
         {:name "2foo"}
         {:name "xkwa" :ns x}
         {:name "xkwb" :ns :clj-kondo/unknown-namespace}
         {:name "fookwa" :ns foo}
         {:name "fookwb" :ns foo}
         {:name "fookwc" :ns :clj-kondo/unknown-namespace}
         {:name "barkwa" :ns bar}
         {:name "barkwb" :ns bar :alias b}
         {:name "barkwc" :ns :clj-kondo/unknown-namespace}]
       (:keywords a))))
  (testing "destructuring keywords"
    (let [a (analyze (str "(ns foo (:require [bar :as b]))\n"
                          "(let [{::keys [a :b]\n"
                          "       ::b/keys [c :d]\n"
                          "       :bar/keys [e :f]\n"
                          "       :keys [g :h ::i :foo/j :bar/k ::b/l ::bar/m :x/n ::y/o foo/j]\n"
                          "       p :p q ::q r ::b/r s :bar/s t :x/t} {}])")
                     {:config {:analysis {:keywords true}}})]
      (assert-submaps
       '[{:name "require"}
         {:name "as"}
         {:name "keys" :ns foo :keys-destructuring-ns-modifier true}
         {:name "a" :ns foo :keys-destructuring true}
         {:name "b" :ns foo :keys-destructuring true}
         {:name "keys" :ns bar :alias b}
         {:name "c" :ns bar :keys-destructuring true}
         {:name "d" :ns bar :keys-destructuring true}
         {:name "keys" :ns bar}
         {:name "e" :ns bar :keys-destructuring true}
         {:name "f" :ns bar :keys-destructuring true}
         {:name "keys"}
         {:name "g" :keys-destructuring true}
         {:name "h" :keys-destructuring true}
         {:name "i" :ns foo :keys-destructuring true}
         {:name "j" :ns foo :keys-destructuring true}
         {:name "k" :ns bar :keys-destructuring true}
         {:name "l" :ns bar :alias b :keys-destructuring true}
         {:name "m" :ns :clj-kondo/unknown-namespace}
         {:name "n" :ns x}
         {:name "o" :ns :clj-kondo/unknown-namespace}
         {:name "p"}
         {:name "q" :ns foo}
         {:name "r" :ns bar :alias b}
         {:name "s" :ns bar}
         {:name "t" :ns x}
         {:name "j" :ns foo :keys-destructuring true}]
       (:keywords a))))
  (testing "clojure.spec.alpha/def can add :reg"
    (let [a (analyze "(require '[clojure.spec.alpha :as s]) (s/def ::kw (inc))"
                     {:config {:analysis {:keywords true}}})]
      (assert-submaps
       '[{:name "as"}
         {:name "kw" :reg clojure.spec.alpha/def}]
       (:keywords a))))
  (testing "re-frame.core/reg-event-db can add :reg"
    (let [a (analyze "(require '[re-frame.core :as rf])
                      (rf/reg-event-db ::a (constantly {}))
                      (rf/reg-event-fx ::b (constantly {}))
                      (rf/reg-event-ctx ::c (constantly {}))
                      (rf/reg-sub ::d (constantly {}))
                      (rf/reg-sub-raw ::e (constantly {}))
                      (rf/reg-fx ::f (constantly {}))
                      (rf/reg-cofx ::g (constantly {}))"
                     {:config {:analysis {:keywords true}}})]
      (assert-submaps
       '[{:name "as"}
         {:name "a" :reg re-frame.core/reg-event-db}
         {:name "b" :reg re-frame.core/reg-event-fx}
         {:name "c" :reg re-frame.core/reg-event-ctx}
         {:name "d" :reg re-frame.core/reg-sub}
         {:name "e" :reg re-frame.core/reg-sub-raw}
         {:name "f" :reg re-frame.core/reg-fx}
         {:name "g" :reg re-frame.core/reg-cofx}]
       (:keywords a))))
  (testing ":lint-as re-frame.core function will add :reg with the source full qualified ns"
    (let [a (analyze "(user/mydef ::kw (constantly {}))"
                     {:config {:analysis {:keywords true}
                               :lint-as '{user/mydef re-frame.core/reg-event-fx}}})]
      (assert-submaps
       '[{:name "kw" :reg user/mydef}]
       (:keywords a))))
  (testing "hooks can add :reg"
    (let [a (analyze "(user/mydef ::kw (inc))"
                     {:config {:analysis {:keywords true}
                               :hooks {:__dangerously-allow-string-hooks__ true
                                       :analyze-call
                                       {'user/mydef
                                        (str "(require '[clj-kondo.hooks-api :as a])"
                                             "(fn [{n :node}]"
                                             "  (let [c (:children n)]"
                                             "    {:node (a/list-node "
                                             "             (list* 'do"
                                             "                    (a/reg-keyword! (second c) 'user/mydef)"
                                             "                    (drop 2 c)))}))")}}}})]
      (assert-submaps
       '[{:name "kw" :reg user/mydef}]
       (:keywords a))))
  (testing "valid ns name with clojure.data.xml"
    (let [a (analyze "(ns foo (:require [clojure.data.xml :as xml]))
                      (xml/alias-uri 'pom \"http://maven.apache.org/POM/4.0.0\")
                      ::pom/foo"
                     {:config {:analysis {:keywords true}}})]
      (is (edn/read-string (str a)))))
  (testing "namespaced maps"
    (testing "auto-resolved namespace"
      (let [a (analyze "(ns foo (:require [clojure.data.xml :as xml]))
                      #::xml{:a 1}"
                       {:config {:analysis {:keywords true}}})]
        (assert-submaps
         '[{:name "require"}
           {:name "as"}
           {:name "a" :ns clojure.data.xml}]
         (:keywords a))))
    (testing "non-autoresolved namespace"
      (let [a (analyze "(ns foo (:require [clojure.data.xml :as xml]))
                      #:xml{:a 1}"
                       {:config {:analysis {:keywords true}}})]
        (assert-submaps
         '[{:name "require"}
           {:name "as"}
           {:name "a" :ns xml}]
         (:keywords a))))
    ;; Don't use assertmap here to make sure ns is absent
    (testing "no namespace for key :a"
      (let [a (analyze "#:xml{:_/a 1}"
                       {:config {:analysis {:keywords true}}})]
        (is (= '[{:row 1, :col 7, :end-row 1, :end-col 11, :name "a", :filename "<stdin>" :from user}]
               (:keywords a)))))
    ;; Don't use assertmap here to make sure ns is absent
    (testing "no namespace for key :b"
      (let [a (analyze "#:xml{:a {:b 1}}"
                       {:config {:analysis {:keywords true}}})]
        (is (= '[{:row 1, :col 7, :end-row 1, :end-col 9, :ns xml, :name "a", :filename "<stdin>" :namespace-from-prefix true :from user}
                 {:row 1, :col 11, :end-row 1, :end-col 13, :name "b", :filename "<stdin>" :from user}]
               (:keywords a)))))
    (testing "auto-resolved and namespace-from-prefix"
      (let [a (analyze "(ns foo (:require [clojure.set :as set]))
                        :a ::b :bar/c
                        #:d{:e 1 :_/f 2 :g/h 3 ::i 4}
                        {:j/k 5 :l 6 ::m 7}
                        #::set{:a 1}"
                       {:config {:analysis {:keywords true}}})]
        (assert-submaps
         '[{:name "require"}
           {:name "as"}
           {:row 2 :col 25 :end-row 2 :end-col 27 :name "a" :filename "<stdin>" :from foo}
           {:row 2 :col 28 :end-row 2 :end-col 31 :ns foo :auto-resolved true :name "b" :filename "<stdin>" :from foo}
           {:row 2 :col 32 :end-row 2 :end-col 38 :ns bar :name "c" :filename "<stdin>" :from foo}
           {:row 3 :col 29 :end-row 3 :end-col 31 :ns d :namespace-from-prefix true :name "e" :filename "<stdin>" :from foo}
           {:row 3 :col 34 :end-row 3 :end-col 38 :name "f" :filename "<stdin>" :from foo}
           {:row 3 :col 41 :end-row 3 :end-col 45 :ns g :name "h" :filename "<stdin>" :from foo}
           {:row 3 :col 48 :end-row 3 :end-col 51 :ns foo :auto-resolved true :name "i" :filename "<stdin>" :from foo}
           {:row 4 :col 26 :end-row 4 :end-col 30 :ns j :name "k" :filename "<stdin>" :from foo}
           {:row 4 :col 33 :end-row 4 :end-col 35 :name "l" :filename "<stdin>" :from foo}
           {:row 4 :col 38 :end-row 4 :end-col 41 :ns foo :auto-resolved true :name "m" :filename "<stdin>" :from foo}
           {:row 5, :col 32, :end-row 5, :end-col 34, :ns clojure.set, :namespace-from-prefix true,
            :name "a", :filename "<stdin>" :from foo}]
         (:keywords a))))))

(deftest keywords-in-ns-form-test
  (let [analysis (:keywords (analyze "(ns foo (:require [bar :as b]))" {}))]
    (assert-submaps
     '[{:row 1, :col 10, :end-row 1, :end-col 18, :name "require", :filename "<stdin>", :from user}
       {:row 1, :col 24, :end-row 1, :end-col 27, :name "as", :filename "<stdin>", :from user}]
     analysis))
  (let [analysis (:keywords (analyze "(ns foo (:refer-clojure :exclude [assoc]))" {}))]
    (assert-submaps
     '[{:row 1, :col 10, :end-row 1, :end-col 24, :name "refer-clojure", :filename "<stdin>", :from user} {:row 1, :col 25, :end-row 1, :end-col 33, :name "exclude", :filename "<stdin>", :from user}]

     analysis)))
