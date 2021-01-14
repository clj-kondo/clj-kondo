(ns clj-kondo.analysis-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [assert-submaps]]
   [clojure.edn :as edn]
   [clojure.test :as t :refer [deftest is testing]]))

(defn analyze
  ([input] (analyze input nil))
  ([input config]
   (:analysis
    (with-in-str
      input
      (clj-kondo/run! (merge
                       {:lint ["-"]
                        :config {:output {:analysis true}}}
                       config))))))

(deftest locals-analysis-test
  (let [a (analyze "#(inc %1 %&)" {:config {:output {:analysis {:locals true}}}})]
    (is (= [] (:locals a) (:local-usages a))))
  (let [a (analyze "(areduce [] i j 0 (+ i j))" {:config {:output {:analysis {:locals true}}}})
        [first-a second-a] (:locals a)
        [first-use second-use] (:local-usages a)]
    (assert-submaps
      [{:end-col 14 :scope-end-col 27}
       {:end-col 16 :scope-end-col 27}]
      (:locals a))
    (is (= (:id first-a) (:id first-use)))
    (is (= (:id second-a) (:id second-use))))
  (let [a (analyze "(defn x [a] (let [a a] a) a)" {:config {:output {:analysis {:locals true}}}})
        [first-a second-a] (:locals a)
        [first-use second-use third-use] (:local-usages a)]
    (assert-submaps
      [{:end-col 11 :scope-end-col 29}
       {:end-col 20 :scope-end-col 26}]
      (:locals a))
    (is (= (:id first-a) (:id first-use) (:id third-use)))
    (is (= (:id second-a) (:id second-use))))
  (let [a (analyze "(as-> {} $ $)" {:config {:output {:analysis {:locals true}}}})
        [first-a] (:locals a)
        [first-use] (:local-usages a)]
    (assert-submaps
      [{:end-col 11 :scope-end-col 14}]
      (:locals a))
    (is (= (:id first-a) (:id first-use))))
  (let [a (analyze "(letfn [(a [b] b)] a)" {:config {:output {:analysis {:locals true}}}})
         [first-a second-a] (:locals a)
         [first-use second-use] (:local-usages a)]
    (assert-submaps
      [{:end-col 11 :scope-end-col 22}
       {:end-col 14 :scope-end-col 18}]
      (:locals a))
    (is (= (:id first-a) (:id first-use)))
    (is (= (:id second-a) (:id second-use))))
  (let [a (analyze "(let [a 0] (let [a a] a))" {:config {:output {:analysis {:locals true}}}})
        [first-a second-a] (:locals a)
        [first-use second-use] (:local-usages a)]
    (assert-submaps
      [{:end-col 8 :scope-end-col 26}
       {:end-col 19 :scope-end-col 25}]
      (:locals a))
    (is (= (:id first-a) (:id first-use)))
    (is (= (:id second-a) (:id second-use))))
  (let [a (analyze "(let [a 0 a a] a)" {:config {:output {:analysis {:locals true}}}})
        [first-a second-a] (:locals a)
        [first-use second-use] (:local-usages a)]
    (assert-submaps
      [{:end-col 8 :scope-end-col 18}
       {:end-col 12 :scope-end-col 18}]
      (:locals a))
    (is (= (:id first-a) (:id first-use)))
    (is (= (:id second-a) (:id second-use))))
  (let [a (analyze "(if-let [a 0] a a)" {:config {:output {:analysis {:locals true}}}})
        [first-a second-a] (:locals a)
        [first-use second-use] (:local-usages a)]
    (assert-submaps
      [{:end-col 11 :scope-end-col 16}]
      (:locals a))
    (is (= (:id first-a) (:id first-use)))
    (is (= nil second-a second-use)))
  (let [a (analyze "(for [a [123] :let [a a] :when a] a)" {:config {:output {:analysis {:locals true}}}})
        [first-a second-a] (:locals a)
        [first-use second-use third-use] (:local-usages a)]
    (assert-submaps
      [{:end-col 8 :scope-end-col 37}
       {:end-col 22 :scope-end-col 37}]
      (:locals a))
    (is (not= (:id first-a) (:id second-a)))
    (is (= (:id first-a) (:id first-use)))
    (is (= (:id second-a) (:id second-use) (:id third-use))))
  (testing "Names are reported in binding usages when called as fn"
    (let [ana (analyze "(let [x (set 1 2 3)] (x 1))" {:config {:output {:analysis {:locals true}}}})
          [x] (:locals ana)
          [x-used](:local-usages ana)]
      (is (= 'x (:name x) (:name x-used))))))

(deftest name-position-test
  (let [{:keys [:var-definitions :var-usages]} (analyze "(defn foo [] foo)" {:config {:output {:analysis {:locals true}}}})]
    (assert-submaps
     '[{:name foo :name-row 1 :name-col 7 :name-end-row 1 :name-end-col 10}]
     var-definitions)
    (assert-submaps
      '[{:name foo :name-row 1 :name-col 14 :name-end-row 1 :name-end-col 17} {}]
      var-usages))
  (let [{:keys [:var-definitions :var-usages]} (analyze "(defprotocol Foo (bar [])) Foo bar" {:config {:output {:analysis {:locals true}}}})]
    (assert-submaps
      '[{:name Foo :name-row 1 :name-col 14 :name-end-row 1 :name-end-col 17}
        {:name bar :name-row 1 :name-col 19 :name-end-row 1 :name-end-col 22}]
      var-definitions)
    (assert-submaps
      '[{}
        {:name Foo
         :name-end-col 31,
         :name-end-row 1,
         :name-row 1,
         :name-col 28}
        {:name bar
         :name-end-col 35,
         :name-end-row 1,
         :name-row 1,
         :name-col 32}]
      var-usages))
  (let [{:keys [:namespace-definitions :namespace-usages]} (analyze "(ns foo (:require [bar :as b :refer [x]] [clojure [string :as str]]))" {:config {:output {:analysis {:locals true}}}})]
    (assert-submaps
      '[{:name foo
         :name-end-col 8,
         :name-end-row 1,
         :name-row 1,
         :name-col 5}]
      namespace-definitions)
    (assert-submaps
      '[{:alias b
         :alias-end-col 29,
         :alias-end-row 1,
         :alias-row 1,
         :alias-col 28
         :name-row 1
         :name-col 20
         :name-end-row 1
         :name-end-col 23}
        {:alias str
         :alias-end-col 66,
         :alias-end-row 1,
         :alias-row 1,
         :alias-col 63
         :name-row 1
         :name-col 52
         :name-end-row 1
         :name-end-col 58}]
      namespace-usages))
  (let [{:keys [:var-definitions :var-usages]} (analyze "(try (catch Exception foo foo))" {:config {:output {:analysis {:locals true}}}})]
    (assert-submaps
      '[]
      var-definitions)
    (assert-submaps
      '[{:name-row 1 :name-col 13 :name-end-row 1 :name-end-col 22} {}]
      var-usages))
  (let [{:keys [:var-definitions :var-usages]} (analyze "(def a (atom nil)) (:foo @a)" {:config {:output {:analysis {:locals true}}}})]
    (assert-submaps
      '[{:name-row 1 :name-col 6 :name-end-row 1 :name-end-col 7}]
      var-definitions)
    (assert-submaps
      '[{} {} {:name-row 1 :name-col 27 :name-end-row 1 :name-end-col 28}]
      var-usages)))

(deftest analysis-test
  (let [{:keys [:var-definitions
                :var-usages]} (analyze "(defn ^:deprecated foo \"docstring\" {:added \"1.2\"} [])")]
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 1,
        :ns user,
        :name foo,
        :fixed-arities #{0},
        :doc "docstring",
        :added "1.2",
        :deprecated true}]
     var-definitions)
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 2,
        :from user,
        :to clojure.core,
        :name defn,
        :arity 4}]
     var-usages))

  (let [{:keys [:var-definitions]} (analyze "(def ^:deprecated x \"docstring\" 1)")]
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 1,
        :ns user,
        :name x,
        :doc "docstring",
        :deprecated true}]
     var-definitions))
  (let [{:keys [:namespace-definitions
                :namespace-usages]}
        (analyze
         "(ns ^:deprecated foo \"docstring\"
            {:added \"1.2\" :no-doc true :author \"Michiel Borkent\"}
            (:require [clojure.string]))")]
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 1,
        :name foo,
        :deprecated true,
        :doc "docstring",
        :added "1.2",
        :no-doc true,
        :author "Michiel Borkent"}]
     namespace-definitions)
    (assert-submaps
     '[{:filename "<stdin>", :row 3, :col 24, :from foo, :to clojure.string}]
     namespace-usages))
  (let [{:keys [:namespace-definitions
                :namespace-usages
                :var-usages
                :var-definitions]}
        (analyze "(ns foo (:require [clojure.string :as string]))
                  (defn f [] (inc 1 2 3))" {:lang :cljc})]
    (assert-submaps
     '[{:filename "<stdin>", :row 1, :col 1, :name foo, :lang :clj}
       {:filename "<stdin>", :row 1, :col 1, :name foo, :lang :cljs}]
     namespace-definitions)
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 20,
        :from foo,
        :to clojure.string,
        :alias string
        :lang :clj}
       {:filename "<stdin>",
        :row 1,
        :col 20,
        :from foo,
        :to clojure.string,
        :alias string
        :lang :cljs}]
     namespace-usages)
    (assert-submaps
     '[{:filename "<stdin>",
        :row 2,
        :col 19,
        :ns foo,
        :name f,
        :fixed-arities #{0},
        :lang :clj}
       {:filename "<stdin>",
        :row 2,
        :col 19,
        :ns foo,
        :name f,
        :fixed-arities #{0},
        :lang :cljs}]
     var-definitions)
    (assert-submaps
     '[{:filename "<stdin>",
        :row 2,
        :col 31,
        :from foo,
        :to clojure.core,
        :name inc,
        :fixed-arities #{1},
        :arity 3,
        :lang :clj}
       {:name defn,
        :varargs-min-arity 2,
        :lang :clj,
        :filename "<stdin>",
        :from foo,
        :macro true,
        :col 20,
        :arity 3,
        :row 2,
        :to clojure.core}
       {:filename "<stdin>",
        :row 2,
        :col 31,
        :from foo,
        :to cljs.core,
        :name inc,
        :fixed-arities #{1},
        :arity 3,
        :lang :cljs}
       {:name defn,
        :varargs-min-arity 2,
        :lang :cljs,
        :filename "<stdin>",
        :from foo,
        :macro true,
        :col 20,
        :arity 3,
        :row 2,
        :to cljs.core}]
     var-usages))
  (let [{:keys [:var-usages]}
        (analyze "(ns foo)
                  (fn [x] x)
                  (fn* [x] x)
                  (bound-fn [x] x)")]
    (assert-submaps
     '[{:filename "<stdin>",
        :row 2,
        :col 20,
        :name fn,
        :from foo,
        :to clojure.core}
       {:filename "<stdin>",
        :row 3,
        :col 20,
        :name fn*,
        :from foo,
        :to clojure.core}
       {:filename "<stdin>",
        :row 4,
        :col 20,
        :name bound-fn,
        :from foo,
        :to clojure.core}]
     var-usages)))

(deftest analysis-is-valid-edn-test
  (testing "solution for GH-476, CLJS with string require"
    (let [analysis (analyze "(ns foo (:require [\"@dude\" :as d])) (d/fn-call)")
          analysis-edn (pr-str analysis)]
      (is (edn/read-string analysis-edn)))))

(deftest test-var-test
  (let [{:keys [:var-definitions]}
        (analyze "(ns foo (:require [clojure.test :as t]))
                  (t/deftest foo)")]
    (assert-submaps
     '[{:filename "<stdin>", :row 2, :col 19, :ns foo, :name foo, :fixed-arities #{0},
        :test true :defined-by clojure.test/deftest}]
     var-definitions)))

(deftest deftype-test
  (let [{:keys [:var-definitions]}
        (analyze "(ns foo)
                  (deftype Foo [])")]
    (assert-submaps
     '[{:filename "<stdin>", :row 2, :col 19, :ns foo, :name Foo, :defined-by clojure.core/deftype}
       {:filename "<stdin>", :row 2, :col 19, :ns foo, :name ->Foo, :fixed-arities #{0}, :defined-by clojure.core/deftype}]
     var-definitions)))

(deftest defprotocol-test
  (let [{:keys [:var-definitions]}
        (analyze "(ns foo)
                  (defprotocol Foo (foo [_]))")]
    (is (= '#{clojure.core/defprotocol} (set (map :defined-by var-definitions))))))

(deftest export-test
  (let [{:keys [:var-definitions]}
        (analyze "(ns foo)
                  (defn ^:export foo [])")]
    (is (true? (:export (first var-definitions))))))

(deftest nested-libspec-test
  (let [{:keys [:namespace-usages :var-usages]}
        (analyze "(ns foo (:require [clojure [set :refer [union]]])) (union #{1 2 3} #{3 4 5})")]
    (is (= 'clojure.set (:to (first namespace-usages))))
    (is (= 'clojure.set (:to (first var-usages))))))
