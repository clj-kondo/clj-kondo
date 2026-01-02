(ns clj-kondo.impl.types.clojure.test)

(def clojure-test
  {;; *load-tests*
   ;; *stack-trace-depth*
   ;; *report-counters*
   ;; *initial-report-counters*
   ;; *testing-vars*
   ;; *testing-contexts*
   ;; *test-out*
   ;; with-test-out
   ;; file-position => deprecated
   'testing-vars-str {:arities {1 {:args [:map]
                                   :ret :string}}}
   'testing-contexts-str {:arities {0 {:ret :string}}}
   ;; inc-report-counter
   'report {:arities {1 {:args [:map]
                         :ret :any}}}
   ;; file-and-line
   ;; stacktrace-file-and-line
   'do-report {:arities {1 {:args [:map]
                            :ret :any}}}
   'get-possibly-unbound-var {:arities {1 {:args [:var]
                                           :ret :nilable/var}}}
   'function? {:arities {1 {:ret :boolean}}}
   ;; assert-predicate
   ;; assert-any
   ;; assert-expr
   ;; try-expr
   'is {:fn (fn [args]
              (if (seq args)
                (:tag (first args))
                :any))
        :arities {1 {:args [:any]}
                  2 {:args [:any :string]}}}
   'are {:arities {:varargs {:ret :boolean}}}
   'testing {:fn last}
   'with-test {:arities {:varargs {:ret :var}}}
   'deftest {:arities {:varargs {:ret :var}}}
   'deftest- {:arities {:varargs {:ret :var}}}
   'set-test {:arities {:varargs {:ret :map}}}
   ;; add-ns-meta
   'use-fixtures {:arities {:varargs {:args [{:op :rest :spec :ifn}]
                                      :ret :map}}}
   ;; default-fixture
   'compose-fixtures {:arities {2 {:args [:ifn :ifn]
                                   :ret :ifn}}}
   'join-fixtures {:arities {1 {:args [:seqable]
                                :ret :ifn}}}
   ;; Exception testing
   'thrown? {:arities {:varargs {:ret :throwable}}}
   'thrown-with-msg? {:arities {:varargs {:ret :throwable}}}
   'test-var {:arities {1 {:args [:var]
                           :ret :any}}}
   'test-vars {:arities {1 {:args [:seqable]
                            :ret :nil}}}
   'test-all-vars {:arities {1 {:args [:symbol]
                                :ret :nil}}}
   'test-ns {:arities {1 {:args [:symbol]
                          :ret :map}}}
   'run-tests {:arities {0 {:ret :map}
                         :varargs {:args [{:op :rest :spec :symbol}]
                                   :ret :map}}}
   'run-all-tests {:arities {0 {:ret :map}
                             1 {:args [:any]
                                :ret :map}}}
   'successful? {:arities {1 {:args [:map]
                              :ret :boolean}}}})

;;;; Scratch

(comment
  (require '[clojure.test :as t :refer [is deftest]])

  (deftest my-fn
    (is (= 4 (my-fn 2))))
  ;; => #'clj-kondo.impl.types.clojure.test/my-fn

  (type (t/with-test
          (defn my-fn [x] (* x 2))
          (is (= 4 (my-fn 2)))))
  ;; => clojure.lang.Var

  (t/run-tests 'clj-kondo.types-clojure-test-test)
  ;;=> {:test 1, :pass 1, :fail 0, :error 0, :type :summary}

  (t/set-test my-fn
              (is (= 4 (my-fn 2))))
  ;; => {:test #function[clj-kondo.impl.types.clojure.test/eval9837/fn--9838], :line 75, :column 3, :file "/home/jon/sandbox/apps/clj-kondo/src/clj_kondo/impl/types/clojure/test.clj", :name my-fn, :ns #namespace[clj-kondo.impl.types.clojure.test]}


  (t/use-fixtures :each (fn [f] (f)))
  ;; => #:clojure.test{:each-fixtures (#function[clj-kondo.impl.types.clojure.test/eval9843/fn--9844])}

  (t/compose-fixtures (fn [t] (t)) (fn [t] (t)))
  ;; => #function[clojure.test/compose-fixtures/fn--9755]

  (t/join-fixtures [(fn [t] (t)) (fn [t] (t))])
  ;; => #function[clojure.test/compose-fixtures/fn--9755]

  (t/get-possibly-unbound-var #'my-fn)
  ;; => #function[clj-kondo.impl.types.clojure.test/my-fn]

  (t/test-vars [#'my-fn])
  ;; => nil

  (t/test-all-vars 'clj-kondo.types-clojure-test-test)
  ;; => nil
  )