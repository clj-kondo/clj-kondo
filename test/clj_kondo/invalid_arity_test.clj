(ns clj-kondo.invalid-arity-test
  (:require
   [clj-kondo.test-utils :refer
    [lint! assert-submaps]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]
   [missing.test.assertions]))

(deftest invalid-arity-test
  (let [linted (lint! (io/file "corpus" "invalid_arity"))
        row-col-files (sort-by (juxt :file :row :col)
                               (map #(select-keys % [:row :col :file])
                                    linted))]
    (assert-submaps
     '({:row 7, :col 1, :file "corpus/invalid_arity/calls.clj"}
       {:row 8, :col 1, :file "corpus/invalid_arity/calls.clj"}
       {:row 9, :col 1, :file "corpus/invalid_arity/calls.clj"}
       {:row 10, :col 1, :file "corpus/invalid_arity/calls.clj"}
       {:row 11, :col 1, :file "corpus/invalid_arity/calls.clj"}
       {:row 7, :col 1, :file "corpus/invalid_arity/defs.clj"}
       {:row 10, :col 1, :file "corpus/invalid_arity/defs.clj"}
       {:row 11, :col 1, :file "corpus/invalid_arity/defs.clj"}
       {:row 9, :col 1, :file "corpus/invalid_arity/order.clj"})
     row-col-files)
    (is (every? #(str/includes? % "is called with")
                (map :message linted))))
  (let [invalid-core-function-call-example "
(ns clojure.core)
(defn inc [x])
(ns cljs.core)
(defn inc [x])

(ns myns)
(inc 1 2 3)
"
        linted (lint! invalid-core-function-call-example '{:linters {:redefined-var {:level :off}}})]
    (is (pos? (count linted)))
    (is (every? #(str/includes? % "is called with")
                linted)))
  (is (empty? (lint! "(defn foo [x]) (defn bar [foo] (foo))")))
  (is (empty? (lint! "(defn foo [x]) (let [foo (fn [])] (foo))")))
  (testing "macroexpansion of ->"
    (is (empty? (lint! "(defn xinc [x] (+ x 1)) (-> x xinc xinc)")))
    (is (= 1 (count (lint! "(defn xinc [x] (+ x 1)) (-> x xinc (xinc 1))")))))
  (testing "macroexpansion of fn literal"
    (is (= 1 (count (lint! "(defn xinc [x] (+ x 1)) #(-> % xinc (xinc 1))")))))
  (testing "only invalid calls after definition are caught"
    (let [linted (lint! (io/file "corpus" "invalid_arity" "order.clj"))
          row-col-files (map #(select-keys % [:row :col :file])
                             linted)]
      (assert-submaps
       '({:row 9, :col 1, :file "corpus/invalid_arity/order.clj"})
       row-col-files)))
  (testing "varargs"
    (is (some? (seq (lint! "(defn foo [x & xs]) (foo)"))))
    (is (empty? (lint! "(defn foo [x & xs]) (foo 1 2 3)"))))
  (testing "defn arity error"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 1,
        :level :error,
        :message "clojure.core/defn is called with 0 args but expects 2 or more"}
       {:file "<stdin>",
        :row 1,
        :col 1,
        :level :error,
        :message "Invalid function body."}
       {:file "<stdin>",
        :row 1,
        :col 8,
        :level :error,
        :message "clojure.core/defmacro is called with 0 args but expects 2 or more"}
       {:file "<stdin>",
        :row 1,
        :col 8,
        :level :error,
        :message "Invalid function body."})
     (lint! "(defn) (defmacro)")))
  (testing "redefining clojure var gives no error about incorrect arity of clojure var"
    (is (empty? (lint! "(defn inc [x y] (+ x y))
                        (inc 1 1)" '{:linters {:redefined-var {:level :off}}}))))
  (testing "defn with metadata"
    (assert-submaps
     '({:file "<stdin>",
        :row 4,
        :col 14,
        :level :error,
        :message "user/my-chunk-buffer is called with 2 args but expects 1"})
     (lint! "(defn ^:static ^:foo my-chunk-buffer ^:bar [capacity]
              (clojure.lang.ChunkBuffer. capacity))
             (my-chunk-buffer 1)
             (my-chunk-buffer 1 2)")))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :error,
      :message "clojure.core/areduce is called with 0 args but expects 5"})
   (lint! "(areduce)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :error,
      :message "cljs.core/this-as is called with 0 args but expects 1 or more"})
   (lint! "(this-as)" "--lang" "cljs"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 24,
      :level :error,
      :message "user/deep-merge is called with 0 args but expects 2"})
   (lint! "(defn deep-merge [x y] (deep-merge))"))
  (assert-submaps
   '({:file "corpus/skip_args/streams_test.clj",
      :row 4,
      :col 33,
      :level :error,
      :message "duplicate key :a"})
   (lint! (io/file "corpus" "skip_args" "streams_test.clj") '{:linters {:invalid-arity {:skip-args [riemann.test/test-stream]}}}))
  (assert-submaps
   '({:file "corpus/skip_args/arity.clj",
      :row 6,
      :col 1,
      :level :error,
      :message "skip-args.arity/my-macro is called with 4 args but expects 3"})
   (lint! (io/file "corpus" "skip_args" "arity.clj") '{:linters {:invalid-arity {:skip-args [skip-args.arity/my-macro]}}}))
  (is (empty? (lint! "
(ns my-ns
  {:clj-kondo/config '{:linters {:invalid-arity {:skip-args [my-ns/loop-10]}
                                 :unresolved-symbol {:exclude [(my-ns/loop-10)]}}}})

(fn [_ _]
  (loop-10 [n]
           (when (pos? n)
             (recur (dec n)))))"))))

(deftest invalid-arity-schema-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 67, :level :error, :message "foo/foo is called with 2 args but expects 1"})
   (lint! "(ns foo (:require [schema.core :as s])) (s/defn foo [a :- s/Int]) (foo 1 2)")))

(deftest invalid-arity-hof-test
  (is (empty? (lint! "(map inc [1 2 3])")))
  (is (empty? (lint! "(map-indexed (fn [i e]) [1 2 3])")))
  (is (empty? (lint! "(keep-indexed (fn [i e]) [1 2 3])")))
  (is (empty? (lint! "(reduce (fn [acc e]) [1 2 3])")))
  (is (empty? (lint! "(sequence (map (fn [_ _])) (range 10) (range 10))")))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 6, :level :error,
      :message "clojure.core/inc is called with 2 args but expects 1"})
   (lint! "(map inc [1 2 3] [4 5 6])"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 9, :level :error, :message "fn is called with 1 arg but expects 2"})
   (lint! "(filter (fn [_ _]) [1 2 3])"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 6, :level :error, :message "fn is called with 1 arg but expects 0"})
   (lint! "(map (fn []) [1 2 3])"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 6, :level :error, :message "fn* is called with 1 arg but expects 2"})
   (lint! "(map #(do % %2) [1 2 3])"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 7, :level :error, :message "fn* is called with 1 arg but expects 2"})
   (lint! "(mapv #(do % %2) [1 2 3])"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 23, :level :error, :message "f is called with 1 arg but expects 0"})
   (lint! "(let [f (fn [])] (map f [1 2 3]))"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 14, :level :error, :message "fn is called with 2 args but expects 1"})
   (lint! "(map-indexed (fn [e]) [1 2 3])"))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 7, :level :error, :message "fn is called with 1 arg but expects 2"})
   (lint! "(some (fn [i e]) [1 2 3])")))

(deftest def+fn-test
  (assert-submaps
   '({:file "corpus/def_fn.clj", :row 12, :col 1, :level :error,
      :message "def-fn/cons is called with 3 args but expects 2"})
   (lint! (io/file "corpus/def_fn.clj"))))
