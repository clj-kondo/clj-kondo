(ns clj-kondo.analysis-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.utils :refer [err]]
   [clj-kondo.test-utils :refer [assert-submap assert-submaps]]
   [clojure.edn :as edn]
   [clojure.string :as string]
   [clojure.test :as t :refer [deftest is testing]]))

(defn analyze
  ([input] (analyze input nil))
  ([input config]
   (:analysis
    (with-in-str
      input
      (clj-kondo/run! (merge
                       {:lint ["-"]
                        :config {:analysis true}}
                       config))))))

(deftest locals-analysis-test
  (let [a (analyze "#(inc %1 %&)" {:config {:analysis {:locals true}}})]
    (is (= [] (:locals a) (:local-usages a))))
  (let [a (analyze "(areduce [] i j 0 (+ i j))" {:config {:analysis {:locals true}}})
        [first-a second-a] (:locals a)
        [first-use second-use] (:local-usages a)]
    (assert-submaps
     [{:end-col 14 :scope-end-col 27}
      {:end-col 16 :scope-end-col 27}]
     (:locals a))
    (is (= (:id first-a) (:id first-use)))
    (is (= (:id second-a) (:id second-use))))
  (let [a (analyze "(defn x [a] (let [a a] a) a)" {:config {:analysis {:locals true}}})
        [first-a second-a] (:locals a)
        [first-use second-use third-use] (:local-usages a)]
    (assert-submaps
     [{:end-col 11 :scope-end-col 29}
      {:end-col 20 :scope-end-col 26}]
     (:locals a))
    (is (= (:id first-a) (:id first-use) (:id third-use)))
    (is (= (:id second-a) (:id second-use))))
  (let [a (analyze "(defn x ([a] a) ([b c] (+ b c)))" {:config {:analysis {:locals true}}})
        [first-a first-b first-c] (:locals a)
        [a-use b-use c-use] (:local-usages a)]
    (assert-submaps
     [{:end-col 12 :scope-end-col 16}
      {:end-col 20 :scope-end-col 32}
      {:end-col 22 :scope-end-col 32}]
     (:locals a))
    (is (= (:id first-a) (:id a-use)))
    (is (= (:id first-b) (:id b-use)))
    (is (= (:id first-c) (:id c-use))))
  (let [a (analyze "(as-> {} $ $)" {:config {:analysis {:locals true}}})
        [first-a] (:locals a)
        [first-use] (:local-usages a)]
    (assert-submaps
     [{:end-col 11 :scope-end-col 14}]
     (:locals a))
    (is (= (:id first-a) (:id first-use))))
  (let [a (analyze "(letfn [(a [b] b)] a)" {:config {:analysis {:locals true}}})
        [first-a second-a] (:locals a)
        [first-use second-use] (:local-usages a)]
    (assert-submaps
     [{:end-col 11 :scope-end-col 22}
      {:end-col 14 :scope-end-col 18}]
     (:locals a))
    (is (= (:id first-a) (:id first-use)))
    (is (= (:id second-a) (:id second-use))))
  (let [a (analyze "(letfn [(a ([b] b) ([c d] (+ c d)))] a)" {:config {:analysis {:locals true}}})
        [first-a first-b first-c first-d] (:locals a)
        [first-use second-use third-use fourth-use] (:local-usages a)]
    (assert-submaps
     [{:end-col 11 :scope-end-col 40}
      {:end-col 15 :scope-end-col 19}
      {:end-col 23 :scope-end-col 35}
      {:end-col 25 :scope-end-col 35}]
     (:locals a))
    (is (= (:id first-a) (:id first-use)))
    (is (= (:id first-b) (:id second-use)))
    (is (= (:id first-c) (:id third-use)))
    (is (= (:id first-d) (:id fourth-use))))
  (let [a (analyze "(let [a 0] (let [a a] a))" {:config {:analysis {:locals true}}})
        [first-a second-a] (:locals a)
        [first-use second-use] (:local-usages a)]
    (assert-submaps
     [{:end-col 8 :scope-end-col 26}
      {:end-col 19 :scope-end-col 25}]
     (:locals a))
    (is (= (:id first-a) (:id first-use)))
    (is (= (:id second-a) (:id second-use))))
  (let [a (analyze "(let [a 0 a a] a)" {:config {:analysis {:locals true}}})
        [first-a second-a] (:locals a)
        [first-use second-use] (:local-usages a)]
    (assert-submaps
     [{:end-col 8 :scope-end-col 18}
      {:end-col 12 :scope-end-col 18}]
     (:locals a))
    (is (= (:id first-a) (:id first-use)))
    (is (= (:id second-a) (:id second-use))))
  (let [a (analyze "(if-let [a 0] a a)" {:config {:analysis {:locals true}}})
        [first-a second-a] (:locals a)
        [first-use second-use] (:local-usages a)]
    (assert-submaps
     [{:end-col 11 :scope-end-col 16}]
     (:locals a))
    (is (= (:id first-a) (:id first-use)))
    (is (= nil second-a second-use)))
  (let [a (analyze "(for [a [123] :let [a a] :when a] a)" {:config {:analysis {:locals true}}})
        [first-a second-a] (:locals a)
        [first-use second-use third-use] (:local-usages a)]
    (assert-submaps
     [{:end-col 8 :scope-end-col 37}
      {:end-col 22 :scope-end-col 37}]
     (:locals a))
    (is (not= (:id first-a) (:id second-a)))
    (is (= (:id first-a) (:id first-use)))
    (is (= (:id second-a) (:id second-use) (:id third-use))))
  (testing "local usages are reported with correct positions"
    (let [ana (analyze "(let [x (set 1 2 3)] (+ x 1))" {:config {:analysis {:locals true}}})
          [x] (:locals ana)]
      (assert-submaps
       [{:row 1, :col 25,
         :end-row 1, :end-col 26,
         :name-row 1, :name-col 25,
         :name-end-row 1, :name-end-col 26,
         :name (:name x),
         :filename "<stdin>",
         :id 1}]
       (:local-usages ana))))
  (testing "Names are reported in binding usages when called as fn"
    (let [ana (analyze "(let [x #(set 1 2 3)] (x 1))" {:config {:analysis {:locals true}}})
          [x] (:locals ana)]
      (assert-submaps
       [{:row 1, :col 23,
         :end-col 28, :end-row 1,
         :name-row 1, :name-col 24,
         :name-end-col 25, :name-end-row 1,
         :name (:name x),
         :filename "<stdin>",
         :id 1}]
       (:local-usages ana))))
  (testing "generated nodes should not be included on analysis"
    (let [ana (analyze "(cond-> {:a 1 :b 2} true (merge {}))" {:config {:analysis {:locals true}}})]
      (is (empty? (:locals ana)))
      (is (empty? (:local-usages ana))))
    (let [ana (analyze "(doto {:a 1 :b 2} (merge {}))" {:config {:analysis {:locals true}}})]
      (is (empty? (:locals ana)))
      (is (empty? (:local-usages ana))))))

(deftest deftype-locals-test
  (let [{:keys [:locals :local-usages]}
        (analyze "(deftype Foo [a b c] clojure.lang.IFn (invoke [_] [a b]))"
                 {:config {:analysis {:locals true}}})]
    ;; (clj-kondo.impl.utils/stderr :locals (pr-str locals))
    (assert-submaps '[{:end-row 1, :scope-end-row 1, :name a, :scope-end-col 58, :filename "<stdin>", :str "a", :col 15, :id 1, :end-col 16, :row 1}
                      {:end-row 1, :scope-end-row 1, :name b, :scope-end-col 58, :filename "<stdin>", :str "b", :col 17, :id 2, :end-col 18, :row 1} {:end-row 1, :scope-end-row 1, :name c, :scope-end-col 58, :filename "<stdin>", :str "c", :col 19, :id 3, :end-col 20, :row 1}
                      {:end-row 1, :scope-end-row 1, :name _, :scope-end-col 57, :filename "<stdin>", :str "_", :col 48, :id 5, :end-col 49, :row 1}]
                    locals)
    (assert-submaps '[{:end-row 1, :name-end-col 53, :name-end-row 1, :name-row 1, :name a, :filename "<stdin>", :col 52, :id 1, :name-col 52, :end-col 53, :row 1}
                      {:end-row 1, :name-end-col 55, :name-end-row 1, :name-row 1, :name b, :filename "<stdin>", :col 54, :id 2, :name-col 54, :end-col 55, :row 1}]
                    local-usages)))

(deftest protocol-impls-test
  (testing "defrecord without protocol"
    (let [{:keys [:protocol-impls]} (analyze "
(defrecord MyBar []
  (something [_]
    123)

  (^Bla other-thing [_ a b]
    456
    789))" {:config {:analysis {:protocol-impls true}}})]
      (assert-submaps
       []
       protocol-impls)))
  (testing "defrecord with simple protocols"
    (let [{:keys [:protocol-impls]} (analyze "
(defprotocol AProtocol
  (a-method [this])
  (^Bla b-method [this a b]))

(defprotocol BProtocol
  (c-method [this])
  (^Bla d-method [this a b]))

(defrecord ARecord []
  AProtocol
  (a-method [_]
    123)
  (^Bla b-method [_ a b]
    456
    789)

  BProtocol
  (c-method [_]
    123)
  (^Bla d-method [_ a b]
    456
    789))" {:config {:analysis {:protocol-impls true}}})]
      (assert-submaps
       '[{:protocol-name AProtocol
          :protocol-ns user
          :method-name a-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/defrecord
          :name-row 12 :name-col 4 :name-end-row 12 :name-end-col 12
          :row 12 :col 3 :end-row 13 :end-col 9}
         {:protocol-name AProtocol
          :protocol-ns user
          :method-name b-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/defrecord
          :name-row 14 :name-col 9 :name-end-row 14 :name-end-col 17
          :row 14 :col 3 :end-row 16 :end-col 9}
         {:protocol-name BProtocol
          :protocol-ns user
          :method-name c-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/defrecord
          :name-row 19 :name-col 4 :name-end-row 19 :name-end-col 12
          :row 19 :col 3 :end-row 20 :end-col 9}
         {:protocol-name BProtocol
          :protocol-ns user
          :method-name d-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/defrecord
          :name-row 21 :name-col 9 :name-end-row 21 :name-end-col 17
          :row 21 :col 3 :end-row 23 :end-col 9}]
       protocol-impls)))
  (testing "defrecord with aliased protocol"
    (let [{:keys [:protocol-impls]}
          (analyze (->> ["(ns some-ns)"
                         "(defprotocol AProtocol"
                         "  (a-method [this])"
                         "  (^Bla b-method [this a b]))"
                         "(ns other-ns (:require [some-ns :as some]))"
                         "(defrecord ARecord []"
                         "  some/AProtocol"
                         "  (a-method [_]"
                         "    123)"
                         ""
                         "  (^Bla b-method [_ a b]"
                         "    456"
                         "    789))"]
                        (string/join "\n"))
                   {:config {:analysis {:protocol-impls true}}})]
      (assert-submaps
       '[{:protocol-name AProtocol
          :protocol-ns some-ns
          :method-name a-method
          :impl-ns other-ns
          :filename "<stdin>"
          :defined-by clojure.core/defrecord
          :name-row 8 :name-col 4 :name-end-row 8 :name-end-col 12
          :row 8 :col 3 :end-row 9 :end-col 9}
         {:protocol-name AProtocol
          :protocol-ns some-ns
          :method-name b-method
          :impl-ns other-ns
          :filename "<stdin>"
          :defined-by clojure.core/defrecord
          :name-row 11 :name-col 9 :name-end-row 11 :name-end-col 17
          :row 11 :col 3 :end-row 13 :end-col 9}]
       protocol-impls)))
  (testing "deftype"
    (let [{:keys [:protocol-impls]} (analyze "
(defprotocol AProtocol
  (a-method [this])
  (^Bla b-method [this a b]))

(defprotocol BProtocol
  (c-method [this])
  (^Bla d-method [this a b]))

(deftype AType []
  AProtocol
  (a-method [_]
    123)
  (^Bla b-method [_ a b]
    456
    789)

  BProtocol
  (c-method [_]
    123)
  (^Bla d-method [_ a b]
    456
    789))" {:config {:analysis {:protocol-impls true}}})]
      (assert-submaps
       '[{:protocol-name AProtocol
          :protocol-ns user
          :method-name a-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/deftype
          :name-row 12 :name-col 4 :name-end-row 12 :name-end-col 12
          :row 12 :col 3 :end-row 13 :end-col 9}
         {:protocol-name AProtocol
          :protocol-ns user
          :method-name b-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/deftype
          :name-row 14 :name-col 9 :name-end-row 14 :name-end-col 17
          :row 14 :col 3 :end-row 16 :end-col 9}
         {:protocol-name BProtocol
          :protocol-ns user
          :method-name c-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/deftype
          :name-row 19 :name-col 4 :name-end-row 19 :name-end-col 12
          :row 19 :col 3 :end-row 20 :end-col 9}
         {:protocol-name BProtocol
          :protocol-ns user
          :method-name d-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/deftype
          :name-row 21 :name-col 9 :name-end-row 21 :name-end-col 17
          :row 21 :col 3 :end-row 23 :end-col 9}]
       protocol-impls)))
  (testing "extend-protocol"
    (let [{:keys [:protocol-impls]} (analyze "
(defprotocol AProtocol
  (a-method [this])
  (^Bla b-method [this a b]))

(defrecord ARecord [])
(deftype AType [])

(extend-protocol AProtocol
  ARecord
  (a-method [_]
    123)
  (^Bla b-method [_ a b]
    456
    789)

  AType
  (a-method [_]
    123)
  (^Bla b-method [_ a b]
    456
    789))" {:config {:analysis {:protocol-impls true}}})]
      (assert-submaps
       '[{:protocol-name AProtocol
          :protocol-ns user
          :method-name a-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/extend-protocol
          :name-row 11 :name-col 4 :name-end-row 11 :name-end-col 12
          :row 11 :col 3 :end-row 12 :end-col 9}
         {:protocol-name AProtocol
          :protocol-ns user
          :method-name b-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/extend-protocol
          :name-row 13 :name-col 9 :name-end-row 13 :name-end-col 17
          :row 13 :col 3 :end-row 15 :end-col 9}
         {:protocol-name AProtocol
          :protocol-ns user
          :method-name a-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/extend-protocol
          :name-row 18 :name-col 4 :name-end-row 18 :name-end-col 12
          :row 18 :col 3 :end-row 19 :end-col 9}
         {:protocol-name AProtocol
          :protocol-ns user
          :method-name b-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/extend-protocol
          :name-row 20 :name-col 9 :name-end-row 20 :name-end-col 17
          :row 20 :col 3 :end-row 22 :end-col 9}]
       protocol-impls)))
  (testing "extend-type"
    (let [{:keys [:protocol-impls]} (analyze "
(defprotocol AProtocol
  (a-method [this])
  (^Bla b-method [this a b]))

(defprotocol BProtocol
  (c-method [this])
  (^Bla d-method [this a b]))

(deftype AType [])

(extend-type AType
  AProtocol
  (a-method [_]
    123)
  (^Bla b-method [_ a b]
    456
    789)

  BProtocol
  (c-method [_]
    123)
  (^Bla d-method [_ a b]
    456
    789))" {:config {:analysis {:protocol-impls true}}})]
      (assert-submaps
       '[{:protocol-name AProtocol
          :protocol-ns user
          :method-name a-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/extend-type
          :name-row 14 :name-col 4 :name-end-row 14 :name-end-col 12
          :row 14 :col 3 :end-row 15 :end-col 9}
         {:protocol-name AProtocol
          :protocol-ns user
          :method-name b-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/extend-type
          :name-row 16 :name-col 9 :name-end-row 16 :name-end-col 17
          :row 16 :col 3 :end-row 18 :end-col 9}
         {:protocol-name BProtocol
          :protocol-ns user
          :method-name c-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/extend-type
          :name-row 21 :name-col 4 :name-end-row 21 :name-end-col 12
          :row 21 :col 3 :end-row 22 :end-col 9}
         {:protocol-name BProtocol
          :protocol-ns user
          :method-name d-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/extend-type
          :name-row 23 :name-col 9 :name-end-row 23 :name-end-col 17
          :row 23 :col 3 :end-row 25 :end-col 9}]
       protocol-impls)))
  (testing "reify"
    (let [{:keys [:protocol-impls]} (analyze "
(defprotocol AProtocol
  (a-method [this])
  (^Bla b-method [this a b]))

(defprotocol BProtocol
  (c-method [this])
  (^Bla d-method [this a b]))

(reify
  AProtocol
  (a-method [_]
    123)
  (^Bla b-method [_ a b]
    456
    789)

  BProtocol
  (c-method [_]
    123)
  (^Bla d-method [_ a b]
    456
    789))" {:config {:analysis {:protocol-impls true}}})]
      (assert-submaps
       '[{:protocol-name AProtocol
          :protocol-ns user
          :method-name a-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/reify
          :name-row 12 :name-col 4 :name-end-row 12 :name-end-col 12
          :row 12 :col 3 :end-row 13 :end-col 9}
         {:protocol-name AProtocol
          :protocol-ns user
          :method-name b-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/reify
          :name-row 14 :name-col 9 :name-end-row 14 :name-end-col 17
          :row 14 :col 3 :end-row 16 :end-col 9}
         {:protocol-name BProtocol
          :protocol-ns user
          :method-name c-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/reify
          :name-row 19 :name-col 4 :name-end-row 19 :name-end-col 12
          :row 19 :col 3 :end-row 20 :end-col 9}
         {:protocol-name BProtocol
          :protocol-ns user
          :method-name d-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by clojure.core/reify
          :name-row 21 :name-col 9 :name-end-row 21 :name-end-col 17
          :row 21 :col 3 :end-row 23 :end-col 9}]
       protocol-impls)))
  (testing "specify!"
    (let [{:keys [:protocol-impls]} (analyze "
(defprotocol AProtocol
  (a-method [this])
  (^Bla b-method [this a b]))

(defprotocol BProtocol
  (c-method [this])
  (^Bla d-method [this a b]))

(def a-map {})

(specify! a-map
  AProtocol
  (a-method [_]
    123)
  (^Bla b-method [_ a b]
    456
    789)

  BProtocol
  (c-method [_]
    123)
  (^Bla d-method [_ a b]
    456
    789))" {:lang :cljs, :config {:analysis {:protocol-impls true}}})]
      (assert-submaps
       '[{:protocol-name AProtocol
          :protocol-ns user
          :method-name a-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by cljs.core/specify!
          :name-row 14 :name-col 4 :name-end-row 14 :name-end-col 12
          :row 14 :col 3 :end-row 15 :end-col 9}
         {:protocol-name AProtocol
          :protocol-ns user
          :method-name b-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by cljs.core/specify!
          :name-row 16 :name-col 9 :name-end-row 16 :name-end-col 17
          :row 16 :col 3 :end-row 18 :end-col 9}
         {:protocol-name BProtocol
          :protocol-ns user
          :method-name c-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by cljs.core/specify!
          :name-row 21 :name-col 4 :name-end-row 21 :name-end-col 12
          :row 21 :col 3 :end-row 22 :end-col 9}
         {:protocol-name BProtocol
          :protocol-ns user
          :method-name d-method
          :impl-ns user
          :filename "<stdin>"
          :defined-by cljs.core/specify!
          :name-row 23 :name-col 9 :name-end-row 23 :name-end-col 17
          :row 23 :col 3 :end-row 25 :end-col 9}]
       protocol-impls))))

(deftest defmulti-defmethod-test
  (testing "defmulti and defmethod"
    (let [{:keys [:var-usages :var-definitions]} (analyze "
(defmulti my-multi :some-key)
​
(defmethod my-multi :some-value
  [_]
  :bar)" {:config {:analysis true}})]
      (assert-submaps
       '[{:name my-multi
          :defined-by clojure.core/defmulti
          :ns user
          :name-row 2 :name-col 11 :name-end-row 2 :name-end-col 19
          :row 2 :col 1 :end-row 2 :end-col 30}]
       var-definitions)
      (assert-submaps
       '[{:name defmulti}
         {}
         {:name defmethod}
         {:name my-multi
          :from user
          :to user
          :defmethod true
          :name-row 4 :name-col 12 :name-end-row 4 :name-end-col 20
          :row 4 :col 12 :end-row 4 :end-col 20}]
       var-usages))))

(deftest name-position-test
  (let [{:keys [:var-definitions :var-usages]} (analyze "(defn foo [] foo)" {:config {:analysis {:locals true}}})]
    (assert-submaps
     '[{:name foo :name-row 1 :name-col 7 :name-end-row 1 :name-end-col 10 :end-row 1 :end-col 18}]
     var-definitions)
    (assert-submaps
     '[{:name foo :name-row 1 :name-col 14 :name-end-row 1 :name-end-col 17} {}]
     var-usages))
  (let [{:keys [:var-definitions :var-usages]} (analyze "(defprotocol Foo (bar [])) Foo bar" {:config {:analysis {:locals true}}})]
    (assert-submaps
     '[{:name Foo :name-row 1 :name-col 14 :name-end-row 1 :name-end-col 17 :end-row 1 :end-col 27}
       {:name bar :name-row 1 :name-col 19 :name-end-row 1 :name-end-col 22 :end-row 1 :end-col 27}]
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
  (let [{:keys [:namespace-definitions :namespace-usages]} (analyze "(ns foo (:require [bar :as b :refer [x]] [clojure [string :as str]]))" {:config {:analysis {:locals true}}})]
    (assert-submaps
     '[{:name foo
        :row 1,
        :end-row 1,
        :col 1,
        :end-col 70,
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
  (let [{:keys [:var-definitions :var-usages]} (analyze "(def a (atom nil)) (:foo @a)" {:config {:analysis {:locals true}}})]
    (assert-submaps
     '[{:name-row 1 :name-col 6 :name-end-row 1 :name-end-col 7 :end-row 1 :end-col 19}]
     var-definitions)
    (assert-submaps
     '[{} {} {:name-row 1 :name-col 27 :name-end-row 1 :name-end-col 28}]
     var-usages)))

(deftest scope-usage-test
  (testing "when the var-usage is called as function"
    (let [{:keys [:var-usages]} (analyze "(defn foo [a] a) (foo 2)" {:config {:analysis true}})]
      (is (some #(= % '{:fixed-arities #{1}
                        :name-end-col 22
                        :name-end-row 1
                        :name-row 1
                        :name-col 19
                        :name foo
                        :filename "<stdin>"
                        :from user
                        :arity 1
                        :row 1
                        :col 18
                        :end-row 1
                        :end-col 25
                        :to user})
                var-usages))))
  (testing "when the var-usage is not called as function"
    (let [{:keys [:var-usages]} (analyze "(defn foo [a] a) foo" {:config {:analysis true}})]
      (is (some #(= % '{:fixed-arities #{1}
                        :name-end-col 21
                        :name-end-row 1
                        :name-row 1
                        :name-col 18
                        :name foo
                        :filename "<stdin>"
                        :from user
                        :row 1
                        :col 18
                        :end-row 1
                        :end-col 21
                        :to user})
                var-usages))))
  (testing "when the var-usage call is unknown"
    (let [{:keys [:var-usages]} (analyze "(defn foo [a] a) (bar 2)" {:config {:analysis true}})]
      (is (some #(= % '{:name-end-row 1
                        :name-end-col 22
                        :name-row 1
                        :name-col 19
                        :name bar
                        :filename "<stdin>"
                        :from user
                        :arity 1
                        :row 1
                        :col 18
                        :end-row 1
                        :end-col 25
                        :to :clj-kondo/unknown-namespace})
                var-usages)))))

(deftest analysis-test
  (let [{:keys [:var-definitions
                :var-usages]} (analyze "(defn ^:deprecated foo \"docstring\" {:added \"1.2\"} [])")]
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 1,
        :end-row 1,
        :end-col 54,
        :ns user,
        :name foo,
        :defined-by clojure.core/defn
        :fixed-arities #{0},
        :doc "docstring",
        :added "1.2",
        :deprecated true}]
     var-definitions)
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 1,
        :name-row 1,
        :name-col 2,
        :from user,
        :to clojure.core,
        :name defn,
        :arity 4}]
     var-usages))

  (let [{:keys [:var-definitions]} (analyze "(defn foo \"docstring with\\n \\\"escaping\\\"\" [])")]
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 1,
        :end-row 1,
        :end-col 46,
        :ns user,
        :name foo,
        :defined-by clojure.core/defn,
        :fixed-arities #{0},
        :doc "docstring with\n \"escaping\""}]
     var-definitions))

  (let [{:keys [:var-definitions]} (analyze "(def ^:deprecated x \"docstring\" 1)")]
    (assert-submaps
     '[{:filename "<stdin>",
        :row 1,
        :col 1,
        :end-row 1,
        :end-col 35,
        :defined-by clojure.core/def,
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
        :end-row 2,
        :end-col 42,
        :ns foo,
        :name f,
        :defined-by clojure.core/defn
        :fixed-arities #{0},
        :lang :clj}
       {:filename "<stdin>",
        :row 2,
        :col 19,
        :ns foo,
        :name f,
        :defined-by cljs.core/defn
        :fixed-arities #{0},
        :lang :cljs}]
     var-definitions)
    (assert-submaps
     '[{:filename "<stdin>",
        :row 2,
        :col 30,
        :name-row 2,
        :name-col 31,
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
        :col 19,
        :name-row 2,
        :name-col 20,
        :arity 3,
        :row 2,
        :to clojure.core}
       {:filename "<stdin>",
        :row 2,
        :col 30,
        :name-row 2,
        :name-col 31,
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
        :col 19,
        :name-row 2,
        :name-col 20,
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
        :col 19,
        :name-row 2,
        :name-col 20,
        :name fn,
        :from foo,
        :to clojure.core}
       {:filename "<stdin>",
        :row 3,
        :col 19,
        :name-row 3,
        :name-col 20,
        :name fn*,
        :from foo,
        :to clojure.core}
       {:filename "<stdin>",
        :row 4,
        :col 19,
        :name-row 4,
        :name-col 20,
        :name bound-fn,
        :from foo,
        :to clojure.core}]
     var-usages))
  (let [{:keys [:var-usages]}
        (analyze "(ns foo)
                  (defn foo [x] x)
                  (def bar foo)"
                 {:config {:analysis {:var-usages false}}})]
    (assert-submaps
     '[]
     var-usages))
  (let [{:keys [:var-definitions
                :var-usages]}
        (analyze "(ns foo)
                  (defn foo [x] (inc x))
                  (def bar foo)"
                 {:config {:analysis {:var-definitions {:shallow true}}}})]
    (assert-submaps ;; var definitions are analyzed
     '[{:fixed-arities #{1},
        :ns foo,
        :name foo,
        :defined-by clojure.core/defn}
       {:ns foo,
        :name bar,
        :defined-by clojure.core/def}]
     var-definitions)
    (assert-submaps ;; but their bodies aren't
     '[{:name defn,
        :to clojure.core}
       {:name def,
        :to clojure.core}]
     var-usages)))

(deftest hooks-custom-defined-by-test
  (assert-submaps
   '[{:ns user,
      :name foobar,
      :defined-by user/defflow}]
   (:var-definitions
    (analyze "(user/defflow foobar)"
             {:config {:analysis {:keywords true}
                       :hooks {:__dangerously-allow-string-hooks__ true
                               :analyze-call
                               {'user/defflow
                                (str "(require '[clj-kondo.hooks-api :as api])"
                                     "(fn [{:keys [:node]}]"
                                     "  (let [[test-name] (rest (:children node))"
                                     "       new-node (api/list-node"
                                     "                 [(api/token-node 'def)"
                                     "                  test-name])]"
                                     "   {:node (with-meta new-node (meta node))"
                                     "     :defined-by 'user/defflow}))")}}}}))))

(deftest analysis-alias-test
  (let [{:keys [:var-usages]}
        (analyze "(ns foo (:require [bar :as b] baz))
                  (b/w)
                  (bar/x)
                  b/y
                  bar/z")]
    (assert-submaps
     '[{:to bar :alias b :name w}
       {:to bar :name x}
       {:to bar :alias b :name y}
       {:to bar :name z}]
     var-usages)))

(deftest analysis-arglists-test
  (testing "arglist-strs are present on definitions"
    (let [{:keys [:var-definitions]}
          (analyze "(defn f1 [d] d)
                    (defn f2 ([e] e) ([f f'] f))
                    (defprotocol A (f3 [g] \"doc\") (f4 [h] [i i']))
                    (defrecord A [j k])
                    (defmacro f5 [l m])"
                   {:config {:analysis {:arglists true}}})]
      (assert-submaps
       '[{:name f1,
          :defined-by clojure.core/defn
          :arglist-strs ["[d]"]}
         {:name f2,
          :defined-by clojure.core/defn
          :arglist-strs ["[e]" "[f f']"]}
         {:name f3,
          :defined-by clojure.core/defprotocol
          :arglist-strs ["[g]"]}
         {}
         {:name f4,
          :defined-by clojure.core/defprotocol
          :arglist-strs ["[h]" "[i i']"]}
         {}
         {:name ->A
          :defined-by clojure.core/defrecord
          :arglist-strs ["[j k]"]}
         {:name map->A
          :defined-by clojure.core/defrecord
          :arglist-strs ["[m]"]}
         {:name f5
          :defined-by clojure.core/defmacro
          :arglist-strs ["[l m]"]}]
       var-definitions))))

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
     '[{:filename "<stdin>", :row 2, :col 19, :end-row 2, :end-col 34, :ns foo, :name foo, :fixed-arities #{0},
        :test true :defined-by clojure.test/deftest}]
     var-definitions)))

(deftest schema-var-test
  (let [{:keys [:var-definitions]}
        (analyze "(ns foo (:require [schema.core :as s]))
                  (s/def bar)
                  (s/defn f1 [d] d)
                  (s/defn f2 ([e] e) ([f f'] f))
                  (s/defrecord A [j k])")]
    (assert-submaps
     '[{:end-row 2, :name-end-col 29, :name-end-row 2, :name-row 2, :ns foo, :name bar, :defined-by schema.core/def, :filename "<stdin>", :col 19, :name-col 26, :end-col 30, :row 2}
       {:fixed-arities #{1}, :end-row 3, :name-end-col 29, :name-end-row 3, :name-row 3, :ns foo, :name f1, :defined-by schema.core/defn, :filename "<stdin>", :col 19, :name-col 27, :end-col 36, :row 3}
       {:fixed-arities #{1 2}, :end-row 4, :name-end-col 29, :name-end-row 4, :name-row 4, :ns foo, :name f2, :defined-by schema.core/defn, :filename "<stdin>", :col 19, :name-col 27, :end-col 49, :row 4}
       {:end-row 5, :name-end-col 33, :name-end-row 5, :name-row 5, :ns foo, :name A, :defined-by schema.core/defrecord, :filename "<stdin>", :col 19, :name-col 32, :end-col 40, :row 5}
       {:fixed-arities #{2}, :end-row 5, :name-end-col 33, :name-end-row 5, :name-row 5, :ns foo, :name ->A, :defined-by schema.core/defrecord, :filename "<stdin>", :col 19, :name-col 32, :end-col 40, :row 5}
       {:fixed-arities #{1}, :end-row 5, :name-end-col 33, :name-end-row 5, :name-row 5, :ns foo, :name map->A, :defined-by schema.core/defrecord, :filename "<stdin>", :col 19, :name-col 32, :end-col 40, :row 5}]
     var-definitions)))

(deftest declare-var-test
  (let [{:keys [:var-definitions]}
        (analyze "(ns foo)
                  (declare bar)")]
    (assert-submaps
     '[{:filename "<stdin>", :row 2, :col 19, :end-row 2, :end-col 32, :ns foo, :name bar, :name-row 2, :name-col 28, :name-end-row 2, :name-end-col 31
        :defined-by clojure.core/declare}]
     var-definitions)))

(deftest deftype-test
  (let [{:keys [:var-definitions]}
        (analyze "(ns foo)
                  (deftype Foo [])")]
    (assert-submaps
     '[{:filename "<stdin>", :row 2, :col 19, :end-row 2, :end-col 35, :ns foo, :name Foo, :defined-by clojure.core/deftype}
       {:filename "<stdin>", :row 2, :col 19, :end-row 2, :end-col 35, :ns foo, :name ->Foo, :fixed-arities #{0}, :defined-by clojure.core/deftype}]
     var-definitions)))

(deftest defprotocol-test
  (let [{:keys [:var-definitions]}
        (analyze "(ns foo)
                  (defprotocol Foo (bar [_]) (baz [_]))")]
    (is (= '#{clojure.core/defprotocol} (set (map :defined-by var-definitions))))
    (is (= {} (select-keys (first var-definitions) [:protocol-ns :protocol-name])))
    (assert-submaps
     '[{:name Foo}
       {:name bar :protocol-ns foo :protocol-name Foo}
       {:name baz :protocol-ns foo :protocol-name Foo}]
     var-definitions)))

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

(deftest refer-var-usages-test
  (testing "from require"
    (let [{:keys [:namespace-usages :var-usages]}
          (analyze "(ns foo (:require [clojure [set :refer [union]]])) (union #{1 2 3} #{3 4 5})")]
      (is (= 'clojure.set (:to (first namespace-usages))))
      (assert-submaps
       '[{:name union :to clojure.set :name-col 41 :refer true}
         {:name union :to clojure.set :name-col 53}]
       var-usages)))
  (testing "from use"
    (let [{:keys [:namespace-usages :var-usages]}
          (analyze "(ns foo (:use [clojure [set :only [union]]])) (union #{1 2 3} #{3 4 5})")]
      (is (= 'clojure.set (:to (first namespace-usages))))
      (assert-submaps
       '[{:name union :to clojure.set :name-col 36 :refer true}
         {:name union :to clojure.set :name-col 48}]
       var-usages))))

(deftest standalone-require-test
  (let [{:keys [:namespace-usages :var-usages]}
        (analyze "(require '[clojure [set :refer [union]]])")]
    (is (= 'clojure.set (:to (first namespace-usages))))
    (assert-submaps
     '[{:name union :name-row 1 :name-end-row 1 :name-col 33 :name-end-col 38}
       {:name require :name-row 1 :name-end-row 1 :name-col 2 :name-end-col 9}]
     var-usages)))

(deftest hof-test
  (let [{:keys [:var-usages]}
        (analyze "(defn foo [x y] (reduce foo x [y 3 4]))")]
    (is (some (fn [usage]
                (and (= 'foo (:name usage))
                     (= 2 (:arity usage))
                     (= 'foo (:from-var usage)))) var-usages))
    (is (every? (fn [usage]
                  (or (not  (= 'foo (:name usage)))
                      (= 'foo (:from-var usage)))) var-usages))))

(defn- ana-var-meta [s cfg]
  (-> (with-in-str s
        (clj-kondo/run! {:lint ["-"] :config
                         {:analysis
                          {:var-definitions
                           cfg}}}))
      :analysis :var-definitions first))

(defn- ana-def-expected [m]
  (merge {:row 1 :col 1 :end-row 1 :name-row 1 :name-end-row 1 :filename "<stdin>"
          :ns 'user :name 'x :defined-by 'clojure.core/def} m))

(defn- ana-defn-expected [m]
  (merge {:row 1 :col 1 :end-row 1 :name-row 1 :name-end-row 1 :filename "<stdin>"
          :ns 'user :name 'my-fn :defined-by 'clojure.core/defn} m))

(deftest meta-var-test
  (testing "def"
    (testing "all"
      (is (= (ana-def-expected {:meta {:no-doc true}
                                :end-col 34
                                :name-col 15
                                :doc "docstring"
                                :name-end-col 16})
             (ana-var-meta "(def ^:no-doc x \"docstring\" true)"
                           {:meta true}))))
    (testing "specific"
      (is (= (ana-def-expected {:meta {:no-doc true}
                                :end-col 30
                                :name-col 23
                                :name-end-col 24})
             (ana-var-meta "(def ^:no-doc ^:other x true)"
                           {:meta [:no-doc]}))))
    (testing "none"
      (is (= (ana-def-expected {:end-col 30
                                :name-col 23
                                :name-end-col 24})
             (-> (with-in-str "(def ^:no-doc ^:other x true)"
                   (clj-kondo/run! {:lint ["-"] :config
                                    {:analysis true}}))
                 :analysis :var-definitions first))))
    (testing "we don't clobber :user-meta"
      (is (= (ana-def-expected {:meta {:user-meta :foo-bar}
                                :end-col 36
                                :name-col 29
                                :name-end-col 30})
             (ana-var-meta "(def ^{:user-meta :foo-bar} x true)"
                           {:meta true}))))
    (testing "when user specifies metadata with same keys as positional metadata, it is returned"
      (is (= (ana-def-expected {:meta {:row :r
                                       :col :c
                                       :end-col :ec
                                       :end-row :er
                                       :name-row :nr :name-col :nc
                                       :name-end-row :ner
                                       :name-end-col :nec
                                       :cool :yes}
                                :name-col 128
                                :name-end-col 129
                                :end-col 130})
             (ana-var-meta (str "(def ^{:row :r :col :c"
                                " :end-col :ec :end-row :er"
                                " :name-row :nr :name-col :nc"
                                " :name-end-row :ner :name-end-col :nec"
                                " :cool :yes} x)")
                           {:meta true})))))
  (testing "defn"
    (testing "reader macro shorthand"
      (is (= (ana-defn-expected {:meta {:my-meta1 true :my-meta2 true :my-meta3 true}
                                 :end-col 50
                                 :name-col 40
                                 :name-end-col 45
                                 :fixed-arities #{1}})
             (ana-var-meta "(defn ^:my-meta1 ^:my-meta2 ^:my-meta3 my-fn [x])"
                           {:meta true}))))
    (testing "reader macro longhand"
      (is (= (ana-defn-expected {:meta {:my-meta1 true :my-meta2 true :my-meta3 true}
                                 :end-col 65
                                 :name-col 55
                                 :name-end-col 60
                                 :fixed-arities #{1}})
             (ana-var-meta "(defn ^{:my-meta1 true :my-meta2 true :my-meta3 true} my-fn [x])"
                           {:meta true}))))
    (testing "attr-map"
      (is (= (ana-defn-expected {:meta {:my-meta1 true :my-meta2 true :my-meta3 true}
                                 :end-col 64
                                 :name-col 7
                                 :name-end-col 12
                                 :fixed-arities #{1}})
             (ana-var-meta "(defn my-fn {:my-meta1 true :my-meta2 true :my-meta3 true} [x])"
                           {:meta true}))))
    (testing "docs, if specified as doc-string, is not returned under :meta"
      (is (= (ana-defn-expected {:meta {:my-meta-here true}
                                 :end-col 53
                                 :name-col 29
                                 :name-end-col 34
                                 :doc "some fn docs"
                                 :fixed-arities #{0}})
             (ana-var-meta "(defn ^{:my-meta-here true} my-fn \"some fn docs\" [])"
                           {:meta true}))))
    (testing "docs, if specified as user coded metadata, is returned under :meta"
      (is (= (ana-defn-expected {:meta {:my-meta-here true :doc "some fn docs"}
                                 :end-col 58
                                 :name-col 49
                                 :name-end-col 54
                                 :doc "some fn docs"
                                 :fixed-arities #{0}})
             (ana-var-meta "(defn ^{:my-meta-here true :doc \"some fn docs\"} my-fn [])"
                           {:meta true}))))
    (testing "metadata reader-macro and attr-map are merged"
      (is (= (ana-defn-expected {:meta {:deprecated true :added "1.2.3"}
                                 :end-col 64
                                 :name-col 38
                                 :name-end-col 43
                                 :added "1.2.3"
                                 :deprecated true
                                 :fixed-arities #{0}})
             (ana-var-meta "(defn ^:deprecated ^{:added \"0.1.2\"} my-fn {:added \"1.2.3\"} [])"
                           {:meta true}))))
    (testing "user meta does not clobber our meta"
      (is (= (ana-defn-expected {:meta {:user-meta :foo-bar
                                        :row :r
                                        :col :c
                                        :end-col :ec
                                        :end-row :er
                                        :name-row :nr :name-col :nc
                                        :name-end-row :ner
                                        :name-end-col :nec
                                        :cool :yes}
                                 :name-col 149
                                 :name-end-col 154
                                 :end-col 158
                                 :fixed-arities #{0}})
             (ana-var-meta (str "(defn ^{:user-meta :foo-bar"
                                " :row :r :col :c"
                                " :end-col :ec :end-row :er"
                                " :name-row :nr :name-col :nc"
                                " :name-end-row :ner :name-end-col :nec"
                                " :cool :yes} my-fn [])")
                           {:meta true}))))
    (testing "2nd attr-map"
      (testing "is recognized for single multi-arity"
        (is (= (ana-defn-expected {:meta {:deprecated true :added :attr2 :l true :a1 true :a2 true}
                                   :end-col 108
                                   :name-col 47
                                   :name-end-col 52
                                   :added :attr2
                                   :deprecated true
                                   :fixed-arities #{0}})
               (ana-var-meta (str "(defn ^:deprecated ^{:added :leading :l true} my-fn"
                                  " {:added :attr1 :a1 true} ([]) {:added :attr2 :a2 true})")
                             {:meta true}))))
      (testing "is recognized for multi multi-arity"
        (is (= (ana-defn-expected {:meta {:deprecated true :added :attr2 :l true :a1 true :a2 true}
                                   :end-col 122
                                   :name-col 47
                                   :name-end-col 52
                                   :added :attr2
                                   :deprecated true
                                   :fixed-arities #{0 1 2}})
               (ana-var-meta (str "(defn ^:deprecated ^{:added :leading :l true} my-fn"
                                  " {:added :attr1 :a1 true} ([]) ([x]) ([x y]) {:added :attr2 :a2 true})")
                             {:meta true}))))
      (testing "is recognized for multi multi-arity when it is only metadata expressed"
        (is (= (ana-defn-expected {:meta {:added :attr2 :a2 true}
                                   :end-col 57
                                   :name-col 7
                                   :name-end-col 12
                                   :added :attr2
                                   :fixed-arities #{0 1 2}})
               (ana-var-meta (str "(defn my-fn ([]) ([x]) ([x y]) {:added :attr2 :a2 true})")
                             {:meta true}))))
      (testing "is not recognized for single arity syntax"
        (is (= (ana-defn-expected {:meta {:deprecated true :added :attr1 :l true :a1 true}
                                   :end-col 106
                                   :name-col 47
                                   :name-end-col 52
                                   :added :attr1
                                   :deprecated true
                                   :fixed-arities #{0}})
               (ana-var-meta (str "(defn ^:deprecated ^{:added :leading :l true} my-fn"
                                  ;; this is technically invalid
                                  " {:added :attr1 :a1 true} [] {:added :attr2 :a2 true})")
                             {:meta true}))))))
  (testing "defmacro (sanity, see defn testing for full suite)"
    (testing "2nd attr-map is recognized and parsed"
      (is (= (ana-defn-expected {:meta {:deprecated true :added :attr2 :l true :a1 true :a2 true}
                                 :end-col 125
                                 :name-col 51
                                 :name-end-col 59
                                 :macro true
                                 :name 'my-macro
                                 :defined-by 'clojure.core/defmacro
                                 :added :attr2
                                 :deprecated true
                                 :fixed-arities #{0 3}})
             (ana-var-meta (str "(defmacro ^:deprecated ^{:added :leading :l true} my-macro"
                                " {:added :attr1 :a1 true} ([]) ([x y z]) {:added :attr2 :a2 true})")
                           {:meta true})))))
  (testing "defmulti (sanity, see def, defn testing for full suite)"
    (is (= (ana-defn-expected {:meta {:deprecated true :added :attr1 :l true :a1 true}
                               :end-col 95
                               :name-col 51
                               :name-end-col 59
                               :name 'my-multi
                               :defined-by 'clojure.core/defmulti
                               :added :attr1
                               :deprecated true})
           (ana-var-meta (str "(defmulti ^:deprecated ^{:added :leading :l true} my-multi"
                              " {:added :attr1 :a1 true} :dispatch)")
                         {:meta true})))))

(defn- ana-ns-meta [s cfg]
  (-> (with-in-str s
        (clj-kondo/run! {:lint ["-"] :config
                         {:analysis
                          {:namespace-definitions
                           cfg}}}))
      :analysis :namespace-definitions first))

(defn- ana-ns-expected [m]
  (merge {:row 1 :col 1 :name-row 1 :name-end-row 1 :filename "<stdin>"
          :name 'my.ns.here} m))

(deftest meta-ns-test
  (testing "return all"
    (testing "reader-macro shorthand"
      (is (= (ana-ns-expected {:meta {:my-meta1 true :my-meta2 true :my-meta3 true}
                               :name-col 38
                               :name-end-col 48
                               :end-row 1
                               :end-col 64
                               :doc "some ns docs"})
             (ana-ns-meta "(ns ^:my-meta1 ^:my-meta2 ^:my-meta3 my.ns.here \"some ns docs\")"
                          {:meta true}))))
    (testing "reader-macro longhand"
      (is (= (ana-ns-expected {:meta {:my-meta1 true :my-meta2 true :my-meta3 true}
                               :name-col 53
                               :name-end-col 63
                               :end-row 1
                               :end-col 79
                               :doc "some ns docs"})
             (ana-ns-meta "(ns ^{:my-meta1 true :my-meta2 true :my-meta3 true} my.ns.here \"some ns docs\")"
                          {:meta true}))))
    (testing "attr-map"
      (is (= (ana-ns-expected {:meta {:my-meta1 true :my-meta2 true :my-meta3 true}
                               :name-col 5
                               :name-end-col 15
                               :end-row 1
                               :end-col 78
                               :doc "some ns docs"})
             (ana-ns-meta "(ns my.ns.here \"some ns docs\" {:my-meta1 true :my-meta2 true :my-meta3 true})"
                          {:meta true}))))
    (testing "metadata reader-macro and attr-map are merged"
      (is (= (ana-ns-expected {:meta {:deprecated true :added "1.2.3"}
                               :name-col 36
                               :name-end-col 46
                               :end-row 1
                               :end-col 67
                               :deprecated true
                               :added "1.2.3"})
             (ana-ns-meta "(ns ^:deprecated ^{:added \"0.1.2\"} my.ns.here {:added \"1.2.3\"} [])"
                          {:meta true}))))
    (testing "docs, if specified as user coded metadata, is returned"
      (is (= (ana-ns-expected {:meta {:my-meta-here true :doc "some ns docs"}
                               :doc "some ns docs"
                               :name-col 47
                               :name-end-col 57
                               :end-row 1
                               :end-col 58})
             (ana-ns-meta "(ns ^{:my-meta-here true :doc \"some ns docs\"} my.ns.here)"
                          {:meta true}))))
    (testing "we don't clobber :user-meta"
      (is (= (ana-ns-expected {:meta {:user-meta :foo-bar}
                               :name-col 28
                               :name-end-col 38
                               :end-row 1
                               :end-col 39})
             (ana-ns-meta "(ns ^{:user-meta :foo-bar} my.ns.here)"
                          {:meta true})))))
  (testing "return specific"
    (is (= (ana-ns-expected {:meta {:my-meta1 true :my-meta3 true}
                             :name-col 38
                             :name-end-col 48
                             :end-row 1
                             :end-col 49})
           (ana-ns-meta "(ns ^:my-meta1 ^:my-meta2 ^:my-meta3 my.ns.here)"
                        {:meta #{:my-meta1 :my-meta3}}))))
  (testing "when user specifies metadata with same keys as positional metadata, it is returned"
    (is (= (ana-ns-expected {:meta {:row :r
                                    :col :c
                                    :end-col :ec
                                    :end-row :er
                                    :name-row :nr :name-col :nc
                                    :name-end-row :ner
                                    :name-end-col :nec
                                    :cool :yes}
                             :name-col 127
                             :name-end-col 137
                             :end-row 1
                             :end-col 138})
           (ana-ns-meta (str "(ns ^{:row :r :col :c"
                             " :end-col :ec :end-row :er"
                             " :name-row :nr :name-col :nc"
                             " :name-end-row :ner :name-end-col :nec"
                             " :cool :yes} my.ns.here)")
                        {:meta true}))))
  (testing "request none"
    (is (= (ana-ns-expected {:name-col 38
                             :name-end-col 48
                             :end-row 1
                             :end-col 49})
           (-> (with-in-str "(ns ^:my-meta1 ^:my-meta2 ^:my-meta3 my.ns.here)"
                 (clj-kondo/run! {:lint ["-"] :config
                                  {:analysis true}}))
               :analysis :namespace-definitions first)))))

(deftest derived-doc
  (testing "namespace"
    (let [enable? [false true]]
      (doseq [lead? enable?
              docs? enable?
              attr? enable?
              :let [lead (when lead? "lead")
                    docs (when docs? "docs")
                    attr (when attr? "attr")
                    s (format "(ns %s my.ns %s %s)"
                              (if lead "^{:doc \"lead\"}" "")
                              (if docs "\"docs\"" "")
                              (if attr "{:doc \"attr\"}" ""))
                    e (remove nil? [lead docs attr])
                    expected-doc (last e)
                    expected-meta-doc (last (remove #(= "docs" %) e))]]
        (is (= expected-doc (-> (ana-ns-meta s {:meta true}) :doc)) (str ":doc " s))
        (is (= expected-meta-doc (-> (ana-ns-meta s {:meta true}) :meta :doc))  (str ":meta :doc " s)))))

  (testing "def"
    (let [enable? [false true]]
      (doseq [lead? enable?
              docs? enable?
              :let [lead (when lead? "lead")
                    docs (when docs? "docs")
                    s (format "(def %s x %s 42)"
                              (if lead "^{:doc \"lead\"}" "")
                              (if docs "\"docs\"" ""))
                    e (remove nil? [lead docs])
                    expected-doc (last e)
                    expected-meta-doc (last (remove #(= "docs" %) e))]]
        (is (= expected-doc (-> (ana-var-meta s {:meta true}) :doc)) (str ":doc " s))
        (is (= expected-meta-doc (-> (ana-var-meta s {:meta true}) :meta :doc))  (str ":meta :doc " s)))))

  (testing "defn variants"
    (let [enable? [false true]]
      (doseq [call ["defn" "defn-" "defmacro"]
              lead? enable?
              docs? enable?
              attr? enable?
              prepost? enable?
              :let [lead (when lead? "lead")
                    docs (when docs? "docs")
                    attr (when attr? "attr")
                    s (format "(%s %s x %s %s [y] %s y)"
                              call
                              (if lead "^{:doc \"lead\"}" "")
                              (if docs "\"docs\"" "")
                              (if attr "{:doc \"attr\"}" "")
                              (if prepost? "{:pre [(string? y)]}" ""))
                    e (remove nil? [lead docs attr])
                    expected-doc (last e)
                    expected-meta-doc (last (remove #(= "docs" %) e))]]
        (is (= expected-doc (-> (ana-var-meta s {:meta true}) :doc)) (str ":doc " s))
        (is (= expected-meta-doc (-> (ana-var-meta s {:meta true}) :meta :doc))  (str ":meta :doc " s)))))

  (testing "defmulti"
    (let [enable? [false true]]
      (doseq [lead? enable?
              docs? enable?
              attr? enable?
              :let [lead (when lead? "lead")
                    docs (when docs? "docs")
                    attr (when attr? "attr")
                    s (format "(defmulti %s x %s %s :hi-there)"
                              (if lead "^{:doc \"lead\"}" "")
                              (if docs "\"docs\"" "")
                              (if attr "{:doc \"attr\"}" ""))
                    e (remove nil? [lead docs attr])
                    expected-doc (last e)
                    expected-meta-doc (last (remove #(= "docs" %) e))]]
        (is (= expected-doc (-> (ana-var-meta s {:meta true}) :doc)) (str ":doc " s))
        (is (= expected-meta-doc (-> (ana-var-meta s {:meta true}) :meta :doc))  (str ":meta :doc " s)))))

  (testing "multi-arity variants"
    (let [enable? [false true]]
      (doseq [call ["defn-" "defn" "defmacro"]
              lead? enable?
              docs? enable?
              attr1? enable?
              attr2? enable?
              :let [lead (when lead? "lead")
                    docs (when docs? "docs")
                    attr1 (when attr1? "attr1")
                    attr2 (when attr2? "attr2")
                    s (format "(%s %s x %s %s ([]) ([x] x) %s)"
                              call
                              (if lead "^{:doc \"lead\"}" "")
                              (if docs "\"docs\"" "")
                              (if attr1 "{:doc \"attr1\"}" "")
                              (if attr2 "{:doc \"attr2\"}" ""))
                    e (remove nil? [lead docs attr1 attr2])
                    expected-doc (last e)
                    expected-meta-doc (last (remove #(= "docs" %) e))]]
        (is (= expected-doc (-> (ana-var-meta s {:meta true}) :doc)) (str ":doc " s))
        (is (= expected-meta-doc (-> (ana-var-meta s {:meta true}) :meta :doc))  (str ":meta :doc " s))))))

(deftest context-test
  (testing "re-frame context"
    (testing "var usages have re-frame subscription context"
      (testing "with meta/location info about the reg"
        (let [analysis (-> (with-in-str "
(ns app (:require [re-frame.core :as re-frame]))
(def foo-def :foo)
(re-frame/reg-sub ::foo (fn [x] [foo-def (inc x)]))
(re-frame/reg-event-db ::bar (fn [x] (dec x)))
"
                             (clj-kondo/run! {:lang :cljs :lint ["-"] :config
                                              {:analysis {:context [:re-frame.core]
                                                          :keywords true}}}))
                           :analysis)
              usages (:var-usages analysis)
              keywords (:keywords analysis)
              inc-usage (some #(when (= 'inc (:name %)) %) usages)
              inc-re-frame-id (-> inc-usage :context :re-frame.core :in-id)
              inc-k (some (fn [k]
                            (when (some-> k :context :re-frame.core :id (= inc-re-frame-id))
                              k))
                          keywords)
              foo-def-usage (some #(when (= 'foo-def (:name %)) %) usages)
              foo-def-re-frame-id (-> foo-def-usage :context :re-frame.core :in-id)
              foo-def-k (some (fn [k]
                                (when (some-> k :context :re-frame.core :id (= foo-def-re-frame-id))
                                  k))
                              keywords)
              dec-usage (some #(when (= 'dec (:name %)) %) usages)
              dec-re-frame-id (-> dec-usage :context :re-frame.core :in-id)
              dec-k (some (fn [k]
                            (when (some-> k :context :re-frame.core :id (= dec-re-frame-id))
                              k))
                          keywords)]
          (is inc-re-frame-id)
          (is (= "foo" (:name inc-k)))
          (is (= 'app (:ns inc-k)))
          (is (:auto-resolved inc-k))
          (is (= "reg-sub" (-> inc-k :context :re-frame.core :var)))
          (is foo-def-re-frame-id)
          (is (= "foo" (:name foo-def-k)))
          (is (= 'app (:ns foo-def-k)))
          (is (:auto-resolved foo-def-k))
          (is (= "reg-sub" (-> foo-def-k :context :re-frame.core :var)))
          (is dec-re-frame-id)
          (is (= "bar" (:name dec-k)))
          (is (= 'app (:ns dec-k)))
          (is (:auto-resolved dec-k))
          (is (= "reg-event-db" (-> dec-k :context :re-frame.core :var))))))))

(defn- re-frame-id [re-frame-name kw]
  (when-let [id (and (= re-frame-name (:name kw)) (-> kw :context :re-frame.core :id))] id))

(deftest re-frame-reg-sub-subscription-test
  (testing "reg-sub and subscription relations are trackable through context"
    (let [analysis (-> (with-in-str "
(ns foo (:require [re-frame.core :as rf]))
(rf/reg-sub :a (constantly {}))
(ns bar (:require [re-frame.core :as rf]))
(rf/reg-sub :b :<- [:a] (fn [a _] a))
(rf/reg-sub :c (fn [] [(rf/subscribe [:a]) (rf/subscribe [:b])]) (fn [[a b]] (vector a b)))
(rf/reg-sub :d :<- [:a] :<- [:b] (fn [[a b]] (vector (:c a) b)))
(rf/reg-sub :e (fn [] [(rf/subscribe [:a (:d foobar)])]) (fn [a] (vector a)))
(defn barfn [] @(rf/subscribe [:a]))
"
                         (clj-kondo/run! {:lang :cljs :lint ["-"] :config
                                          {:analysis {:context [:re-frame.core]
                                                      :keywords true}}}))
                       :analysis)
          usages (:var-usages analysis)
          keywords (:keywords analysis)
          constantly-usage (some #(when (= 'constantly (:name %)) %) usages)
          constantly-in-re-frame-id (-> constantly-usage :context :re-frame.core :in-id)
          a-sub-id (some (partial re-frame-id "a") keywords)
          b-sub-id (some (partial re-frame-id "b") keywords)
          c-sub-id (some (partial re-frame-id "c") keywords)
          d-sub-id (some (partial re-frame-id "d") keywords)
          e-sub-id (some (partial re-frame-id "e") keywords)]
      (testing "var usages in re-frame subscription is tracked"
        (is constantly-in-re-frame-id)
        (is (some #(when (some-> % :context :re-frame.core :id (= constantly-in-re-frame-id)) %) keywords)))
      (is a-sub-id)
      (is b-sub-id)
      (is c-sub-id)
      (is d-sub-id)
      (is e-sub-id)
      (testing "arrow style syntatic sugar references are tracked"
        (is (some #(when (and (= "a" (:name %))
                              (= b-sub-id (-> % :context :re-frame.core :in-id))
                              (some-> % :context :re-frame.core :subscription-ref)) %) keywords))
        (is (some #(when (and (= "a" (:name %))
                              (= d-sub-id (-> % :context :re-frame.core :in-id))
                              (some-> % :context :re-frame.core :subscription-ref)) %) keywords))
        (is (some #(when (and (= "b" (:name %))
                              (= d-sub-id (-> % :context :re-frame.core :in-id))
                              (some-> % :context :re-frame.core :subscription-ref)) %) keywords)))
      (testing "subscribe calls in signal fns are tracked"
        (is (some #(when (and (= "a" (:name %))
                              (= c-sub-id (-> % :context :re-frame.core :in-id))
                              (some-> % :context :re-frame.core :subscription-ref)) %) keywords))
        (is (some #(when (and (= "b" (:name %))
                              (= c-sub-id (-> % :context :re-frame.core :in-id))
                              (some-> % :context :re-frame.core :subscription-ref)) %) keywords)))
      (testing "keyword that is also used as a subscription id reused in subscription not resulting in :subscription-ref"
        (is (some #(when (and (= "c" (:name %)) (some-> % :context :re-frame.core :in-id (= d-sub-id))) %) keywords))
        (is (not-any? #(when (and (= "c" (:name %)) (some-> % :context :re-frame.core :subscription-ref)) %) keywords)))
      (testing "keyword used as subscription param not resulting in subscription-ref"
        (is (some #(when (and (= "d" (:name %)) (some-> % :context :re-frame.core :in-id (= e-sub-id))) %) keywords))
        (is (not-any? #(when (and (= "d" (:name %)) (some-> % :context :re-frame.core :subscription-ref)) %) keywords)))
      (testing "from-var and from filled on keyword that is a subscription reference for subscription in a cljs var"
        (->> (filter #(and (= "a" (:name %)) (some-> % :context :re-frame.core :subscription-ref)) keywords)
             (some #(when (and (= 'barfn (:from-var %)) (= 'bar (:from %))) %))
             is)))))

(deftest potemkin-import-vars-test
  (testing "var usages from potemkin usage are available"
    (testing "when using import-vars with full qualified symbol"
      (let [analysis (-> (with-in-str "
(ns foo)
(defn my-func [a] a)

(defn my-other [a] a)

(ns api (:require
  [foo]
  [potemkin :refer [import-vars]]))
(import-vars
  foo/my-func
  foo/my-other)
"
                           (clj-kondo/run! {:lang :clj :lint ["-"] :config
                                            {:analysis true}}))
                         :analysis)
            usages (:var-usages analysis)
            my-func-usage (some #(when (= 'my-func (:name %)) %) usages)
            my-other-usage (some #(when (= 'my-other (:name %)) %) usages)
            definitions (:var-definitions analysis)
            my-func-definition (some #(when (and (= 'my-func (:name %))
                                                 (= 'api (:ns %))) %) definitions)
            my-other-definition (some #(when (and (= 'my-other (:name %))
                                                  (= 'api (:ns %))) %) definitions)]
        (is (assert-submap
             {:name 'my-func
              :name-row 11
              :name-col 3
              :from 'api
              :to 'foo}
             my-func-usage))
        (is (assert-submap
             {:name 'my-other
              :name-row 12
              :name-col 3
              :from 'api
              :to 'foo}
             my-other-usage))
        (is (assert-submap
             {:name 'my-func
              :ns 'api
              :defined-by 'potemkin/import-vars
              :imported-ns 'foo
              :row 10
              :col 1
              :end-row 12
              :end-col 16
              :name-row 11
              :name-col 3
              :name-end-row 11
              :name-end-col 14}
             my-func-definition))
        (is (assert-submap
             {:name 'my-other
              :ns 'api
              :defined-by 'potemkin/import-vars
              :imported-ns 'foo
              :row 10
              :col 1
              :end-row 12
              :end-col 16
              :name-row 12
              :name-col 3
              :name-end-row 12
              :name-end-col 15}
             my-other-definition))))
    (testing "when using import-vars with vectors"
      (let [analysis (-> (with-in-str "
(ns foo)
(defn my-func [a] a)

(defn my-other [a] a)

(ns api (:require
  [foo]
  [potemkin :refer [import-vars]]))
(import-vars
  [foo my-func
       my-other])
"
                           (clj-kondo/run! {:lang :clj :lint ["-"] :config
                                            {:analysis true}}))
                         :analysis)
            usages (:var-usages analysis)
            my-func-usage (some #(when (= 'my-func (:name %)) %) usages)
            my-other-usage (some #(when (= 'my-other (:name %)) %) usages)
            definitions (:var-definitions analysis)
            my-func-definition (some #(when (and (= 'my-func (:name %))
                                                 (= 'api (:ns %))) %) definitions)
            my-other-definition (some #(when (and (= 'my-other (:name %))
                                                  (= 'api (:ns %))) %) definitions)]
        (is (assert-submap
             {:name 'my-func
              :from 'api
              :to 'foo
              :name-row 11
              :name-col 8
              :name-end-row 11
              :name-end-col 15}
             my-func-usage))
        (is (assert-submap
             {:name 'my-other
              :from 'api
              :to 'foo
              :name-row 12
              :name-col 8
              :name-end-row 12
              :name-end-col 16}
             my-other-usage))
        (is (assert-submap
             {:name 'my-func
              :ns 'api
              :defined-by 'potemkin/import-vars
              :imported-ns 'foo
              :row 10
              :col 1
              :end-row 12
              :end-col 18
              :name-row 11
              :name-col 8
              :name-end-row 11
              :name-end-col 15}
             my-func-definition))
        (is (assert-submap
             {:name 'my-other
              :ns 'api
              :defined-by 'potemkin/import-vars
              :imported-ns 'foo
              :row 10
              :col 1
              :end-row 12
              :end-col 18
              :name-row 12
              :name-col 8
              :name-end-row 12
              :name-end-col 16}
             my-other-definition))))))

(deftest re-frame-dispatch-reg-event-fx-test
  (let [analysis (:analysis (with-in-str
                              "(ns foo (:require [re-frame.core :as rf]))
(rf/reg-event-db :foo (fn [db [_ arg1]] (dissoc db arg1)))
(rf/reg-event-db :other-foo (fn [db [_ arg1 arg2]] (assoc db arg1 arg2)))

(ns bar (:require [re-frame.core :as rf]))
(defn bar-fn [] (rf/dispatch [:foo :some-key]))
(rf/reg-event-fx :simple-dispatch (fn [{:keys [db]} [_ arg1]] {:dispatch [:foo (:bar arg1)]}))
(rf/reg-event-fx :fx-dispatch (fn [{:keys [db]} [_ arg1]] {:fx [[:dispatch [:foo (:bar arg1)]]]}))
(rf/reg-event-fx :later-dispatch (fn [{:keys [db]} [_ arg1]] {:fx [[:dispatch-later {:ms 10 :dispatch [:foo (:bar arg1)]}]]}))
(rf/reg-event-fx :multiple-dispatch (fn [{:keys [db]} [_ arg1 arg2]] {:fx [[:dispatch-n [[:foo (:bar arg1)] [:other-foo arg1 arg2]]]]}))"
                              (clj-kondo/run! {:lang :cljs :lint ["-"] :config
                                               {:analysis {:context [:re-frame.core]
                                                           :keywords true}}})))
        keywords (:keywords analysis)
        simple-dispatch-id (some (partial re-frame-id "simple-dispatch") keywords)
        fx-dispatch-id (some (partial re-frame-id "fx-dispatch") keywords)
        later-dispatch-id (some (partial re-frame-id "later-dispatch") keywords)
        multiple-dispatch-id (some (partial re-frame-id "multiple-dispatch") keywords)]
    (is simple-dispatch-id)
    (is fx-dispatch-id)
    (is later-dispatch-id)
    (is multiple-dispatch-id)
    (testing "from-var and from filled on keyword that is an event reference for dispatch in a cljs var"
      (->> (filter #(and (= "foo" (:name %)) (some-> % :context :re-frame.core :event-ref)) keywords)
           (some #(when (and (= 'bar-fn (:from-var %)) (= 'bar (:from %))) %))
           is))
    (testing ":dispatch keyword in reg-event-fx return map is tracked"
      (is (some #(when (and (= "foo" (:name %))
                            (= simple-dispatch-id (-> % :context :re-frame.core :in-id))
                            (some-> % :context :re-frame.core :event-ref)) %) keywords)))
    (testing ":dispatch keyword in vector in reg-event-fx under :fx keyword is tracked"
      (is (some #(when (and (= "foo" (:name %))
                            (= fx-dispatch-id (-> % :context :re-frame.core :in-id))
                            (some-> % :context :re-frame.core :event-ref)) %) keywords)))
    (testing ":dispatch keyword in reg-event-fx under :dispatch-later keyword is tracked"
      (is (some #(when (and (= "foo" (:name %))
                            (= later-dispatch-id (-> % :context :re-frame.core :in-id))
                            (some-> % :context :re-frame.core :event-ref)) %) keywords)))
    (testing "references to multiple dispatches under :dispatch-n are tracked"
      (is (some #(when (and (= "foo" (:name %))
                            (= multiple-dispatch-id (-> % :context :re-frame.core :in-id))
                            (some-> % :context :re-frame.core :event-ref)) %) keywords))
      (is (some #(when (and (= "other-foo" (:name %))
                            (= multiple-dispatch-id (-> % :context :re-frame.core :in-id))
                            (some-> % :context :re-frame.core :event-ref)) %) keywords)))
    (testing "keyword used as param in a dispatch not resulting in subscription-ref"
      (is (some #(when (and (= "bar" (:name %)) (some-> % :context :re-frame.core :in-id (= simple-dispatch-id))) %) keywords))
      (is (some #(when (and (= "bar" (:name %)) (some-> % :context :re-frame.core :in-id (= fx-dispatch-id))) %) keywords))
      (is (some #(when (and (= "bar" (:name %)) (some-> % :context :re-frame.core :in-id (= later-dispatch-id))) %) keywords))
      (is (some #(when (and (= "bar" (:name %)) (some-> % :context :re-frame.core :in-id (= multiple-dispatch-id))) %) keywords))
      (is (not-any? #(when (and (= "bar" (:name %)) (some-> % :context :re-frame.core :event-ref)) %) keywords)))))

(deftest re-frame-inject-cofx-test
  (let [analysis (:analysis (with-in-str
                              "(ns foo (:require [re-frame.core :as rf]))
(rf/reg-cofx :foo (fn [cofx _] (assoc :cofx :bar :goo)))
(rf/reg-event-fx :use-foo [(rf/inject-cofx :foo)] (fn [{:keys [db bar]}] {:db (assoc db :lolfoo bar)}))
"
                              (clj-kondo/run! {:lang :cljs :lint ["-"] :config
                                               {:analysis {:context [:re-frame.core]
                                                           :keywords true}}})))
        keywords (:keywords analysis)
        foo-cofx-id (some (partial re-frame-id "foo") keywords)
        use-foo-id (some (partial re-frame-id "use-foo") keywords)]
    (is foo-cofx-id)
    (testing "cofx in inject-cofx is tracked"
      (is (some #(when (and (= "foo" (:name %))
                            (= use-foo-id (-> % :context :re-frame.core :in-id))
                            (some-> % :context :re-frame.core :cofx-ref)) %) keywords)))))

(comment
  (context-test)
  )
