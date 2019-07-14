(ns clj-kondo.main-test
  (:require
   [cheshire.core :as cheshire]
   [clj-kondo.main :refer [main]]
   [clj-kondo.test-utils :refer [lint! assert-submaps assert-submap submap?
                                 file-path]]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str :refer [trim]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest inline-def-test
  (let [linted (lint! (io/file "corpus" "inline_def.clj") "--config" "{:linters {:redefined-var {:level :off}}}")
        row-col-files (set (map #(select-keys % [:row :col :file])
                                linted))]
    (is (= #{{:row 5, :col 3, :file "corpus/inline_def.clj"}
             {:row 8, :col 3, :file "corpus/inline_def.clj"}
             {:row 10, :col 10, :file "corpus/inline_def.clj"}
             {:row 12, :col 16, :file "corpus/inline_def.clj"}
             {:row 14, :col 18, :file "corpus/inline_def.clj"}}
           row-col-files))
    (is (= #{"inline def"} (set (map :message linted)))))
  (doseq [lang [:clj :cljs]]
    (is (empty? (lint! "(defmacro foo [] `(def x 1))" "--lang" (name lang))))
    (is (empty? (lint! "(defn foo [] '(def x 3))" "--lang" (name lang))))))

(deftest redundant-let-test
  (let [linted (lint! (io/file "corpus" "redundant_let.clj"))
        row-col-files (set (map #(select-keys % [:row :col :file])
                                linted))]
    (is (= #{{:row 4, :col 3, :file "corpus/redundant_let.clj"}
             {:row 8, :col 3, :file "corpus/redundant_let.clj"}
             {:row 12, :col 3, :file "corpus/redundant_let.clj"}}
           row-col-files))
    (is (= #{"redundant let"} (set (map :message linted)))))
  (assert-submaps '({:file "<stdin>", :row 1, :col 12, :level :warning, :message "redundant let"})
                  (lint! "(let [x 2] (let [y 1]))" "--lang" "cljs"))
  (testing "linters still work in areas where arity linter is are disabled"
    (assert-submaps '({:file "<stdin>", :row 1, :col 43, :level :warning, :message "redundant let"})
                    (lint! "(reify Object (toString [this] (let [y 1] (let [x y] x))))")))

  (is (empty? (lint! "(let [x 2] `(let [y# 3]))")))
  (is (empty? (lint! "(let [x 2] '(let [y 3]))")))
  (is (empty? (lint! "(let [x 2] (let [y 1]) (let [y 2]))")))
  (is (empty? (lint! "(let [x 2] (when true (let [y 1])))")))
  (is (empty? (lint! "(let [z 1] (when true (let [x (let [y 2] y)])))")))
  (is (empty? (lint! "#(let [x 1])")))
  (is (empty? (lint! "(let [x 1] [x (let [y (+ x 1)] y)])")))
  (is (empty? (lint! "(let [x 1] #{(let [y 1] y)})")))
  (is (empty? (lint! "(let [x 1] #:a{:a (let [y 1] y)})")))
  (is (empty? (lint! "(let [x 1] {:a (let [y 1] y)})"))))

(deftest redundant-do-test
  (assert-submaps
   '({:row 3, :col 1, :file "corpus/redundant_do.clj" :message "redundant do"}
     {:row 4, :col 7, :file "corpus/redundant_do.clj" :message "redundant do"}
     {:row 5, :col 14, :file "corpus/redundant_do.clj" :message "redundant do"}
     {:row 6, :col 8, :file "corpus/redundant_do.clj" :message "redundant do"}
     {:row 7, :col 13, :file "corpus/redundant_do.clj" :message "redundant do"})
   (lint! (io/file "corpus" "redundant_do.clj")))
  (is (empty? (lint! "(do 1 `(do 1 2 3))")))
  (is (empty? (lint! "(do 1 '(do 1 2 3))")))
  (is (not-empty (lint! "(fn [] (do :foo :bar))")))
  (is (empty? (lint! "#(do :foo)")))
  (is (empty? (lint! "#(do {:a %})")))
  (is (empty? (lint! "#(do)")))
  (is (empty? (lint! "#(do :foo :bar)")))
  (is (empty? (lint! "#(do (prn %1 %2 true) %1)")))
  (is (empty? (lint! "(let [x (do (println 1) 1)] x)"))))

(deftest invalid-arity-test
  (let [linted (lint! (io/file "corpus" "invalid_arity"))
        row-col-files (sort-by (juxt :file :row :col)
                               (map #(select-keys % [:row :col :file])
                                    linted))]
    row-col-files
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
          row-col-files (set (map #(select-keys % [:row :col :file])
                                  linted))]
      (is (= #{{:row 9, :col 1, :file "corpus/invalid_arity/order.clj"}}
             row-col-files))))
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
        :message "invalid function body"}
       {:file "<stdin>",
        :row 1,
        :col 8,
        :level :error,
        :message "clojure.core/defmacro is called with 0 args but expects 2 or more"}
       {:file "<stdin>",
        :row 1,
        :col 8,
        :level :error,
        :message "invalid function body"})
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
   (lint! "(defn deep-merge [x y] (deep-merge))")))

(deftest invalid-arity-schema-test
  (lint! "(ns foo (:require [schema.core :as s])) (s/defn foo [a :- s/Int]) (foo 1 2)"))

(deftest cljc-test
  (let [linted (lint! (io/file "corpus" "cljc"))
        row-col-files (sort-by (juxt :file :row :col)
                               (map #(select-keys % [:file :row :col])
                                    linted))]
    (is (= '({:file "corpus/cljc/datascript.cljc", :row 8, :col 1}
             {:file "corpus/cljc/test_cljc.cljc", :row 13, :col 9}
             {:file "corpus/cljc/test_cljc.cljc", :row 14, :col 10}
             {:file "corpus/cljc/test_cljc.cljc", :row 21, :col 1}
             {:file "corpus/cljc/test_cljc.cljs", :row 5, :col 1}
             {:file "corpus/cljc/test_cljc_from_clj.clj", :row 5, :col 1}
             {:file "corpus/cljc/test_cljs.cljs", :row 5, :col 1}
             {:file "corpus/cljc/test_cljs.cljs", :row 6, :col 1})
           row-col-files)))
  (assert-submaps '({:file "corpus/spec/alpha.cljs",
                     :row 6,
                     :col 1,
                     :level :error,
                     :message "spec.alpha/def is called with 2 args but expects 3"})
                  (lint! (io/file "corpus" "spec")))
  (is (empty? (lint! "(defn foo [#?(:default s :clj s)]) (foo 1)"
                     "--lang" "cljc"))))

(deftest exclude-clojure-test
  (let [linted (lint! (io/file "corpus" "exclude_clojure.clj"))]
    (is (= '({:file "corpus/exclude_clojure.clj",
              :row 12,
              :col 1,
              :level :error,
              :message "clojure.core/get is called with 4 args but expects 2"})
           linted))))

(deftest private-call-test
  (let [linted (lint! (io/file "corpus" "private"))]
    (assert-submaps '({:file "corpus/private/private_calls.clj",
                       :row 4,
                       :col 1,
                       :level :error,
                       :message "call to private function private.private-defs/private"}
                      {:file "corpus/private/private_calls.clj",
                       :row 5,
                       :col 1,
                       :level :error,
                       :message "call to private function private.private-defs/private-by-meta"})
                    linted)))

(deftest read-error-test
  (testing "when an error happens in one file, the other file is still linted"
    (let [linted (lint! (io/file "corpus" "read_error"))]
      (is (= '({:file "corpus/read_error/error.clj",
                :row 2,
                :col 1,
                :level :error,
                :message "Unexpected EOF."}
               {:file "corpus/read_error/ok.clj",
                :row 6,
                :col 1,
                :level :error,
                :message "read-error.ok/foo is called with 1 arg but expects 0"})
             linted)))))

(deftest nested-namespaced-maps-test
  (is (= '({:file "corpus/nested_namespaced_maps.clj",
            :row 9,
            :col 1,
            :level :error,
            :message
            "nested-namespaced-maps/test-fn is called with 2 args but expects 1"}
           {:file "corpus/nested_namespaced_maps.clj",
            :row 11,
            :col 12,
            :level :error,
            :message "duplicate key :a"})
         (lint! (io/file "corpus" "nested_namespaced_maps.clj")))))

(deftest exit-code-test
  (with-out-str
    (testing "the exit code is 0 when no errors are detected"
      (is (zero? (with-in-str "(defn foo []) (foo)" (main "--lint" "-")))))
    (testing "the exit code is 2 when warning are detected"
      (is (= 2 (with-in-str "(do (do 1))" (main "--lint" "-")))))
    (testing "the exit code is 1 when errors are detected"
      (is (= 3 (with-in-str "(defn foo []) (foo 1)" (main "--lint" "-")))))))

(deftest cond-test
  (doseq [lang [:clj :cljs :cljc]]
    (testing (str "lang: " lang)
      (assert-submaps
       '({:row 9,
          :col 3,
          :level :warning}
         {:row 16,
          :col 3,
          :level :warning})
       (lint! (io/file "corpus" (str "cond_without_else." (name lang)))))
      (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 1,
          :level :error,
          :message "cond requires even number of forms"})
       (lint! "(cond 1 2 3)" "--lang" (name lang)))))
  (assert-submaps
   '({:file "corpus/cond_without_else/core.cljc",
      :row 6,
      :col 21,
      :level :warning}
     {:file "corpus/cond_without_else/core.cljs",
      :row 3,
      :col 7,
      :level :warning})
   (lint! [(io/file "corpus" "cond_without_else" "core.cljc")
           (io/file "corpus" "cond_without_else" "core.cljs")])))

(deftest cljs-core-macro-test
  (assert-submap '{:file "<stdin>",
                   :row 1,
                   :col 1,
                   :level :error,
                   :message "cljs.core/for is called with 4 args but expects 2"}
                 (first (lint! "(for [x []] 1 2 3)" "--lang" "cljs"))))

(deftest built-in-test
  (is (= {:file "<stdin>",
          :row 1,
          :col 1,
          :level :error,
          :message "clojure.core/select-keys is called with 1 arg but expects 2"}
         (first (lint! "(select-keys 1)" "--lang" "clj"))))
  (is (= {:file "<stdin>",
          :row 1,
          :col 1,
          :level :error,
          :message "cljs.core/select-keys is called with 1 arg but expects 2"}
         (first (lint! "(select-keys 1)" "--lang" "cljs"))))
  (is (= {:file "<stdin>",
          :row 1,
          :col 1,
          :level :error,
          :message "clojure.core/select-keys is called with 1 arg but expects 2"}
         (first (lint! "(select-keys 1)" "--lang" "cljc"))))
  (assert-submap {:file "<stdin>",
                  :row 2,
                  :col 5,
                  :level :error,
                  :message "clojure.test/successful? is called with 3 args but expects 1"}
                 (first (lint! "(ns my-cljs (:require [clojure.test :refer [successful?]]))
    (successful? 1 2 3)" "--lang" "clj")))
  (assert-submap {:file "<stdin>",
                  :row 2,
                  :col 5,
                  :level :error,
                  :message "cljs.test/successful? is called with 3 args but expects 1"}
                 (first (lint! "(ns my-cljs (:require [cljs.test :refer [successful?]]))
    (successful? 1 2 3)" "--lang" "cljs")))
  (assert-submap {:file "<stdin>",
                  :row 2,
                  :col 5,
                  :level :error,
                  :message
                  "clojure.set/difference is called with 0 args but expects 1, 2 or more"}
                 (first (lint! "(ns my-cljs (:require [clojure.set :refer [difference]]))
    (difference)" "--lang" "clj")))
  (assert-submap {:file "<stdin>",
                  :row 2,
                  :col 5,
                  :level :error,
                  :message
                  "clojure.set/difference is called with 0 args but expects 1, 2 or more"}
                 (first (lint! "(ns my-cljs (:require [clojure.set :refer [difference]]))
    (difference)" "--lang" "cljs"))))

(deftest built-in-java-test
  (is (= {:file "<stdin>",
          :row 1,
          :col 1,
          :level :error,
          :message "java.lang.Thread/sleep is called with 3 args but expects 1 or 2"}
         (first (lint! "(Thread/sleep 1 2 3)" "--lang" "clj"))))
  (is (= {:file "<stdin>",
          :row 1,
          :col 1,
          :level :error,
          :message "java.lang.Thread/sleep is called with 3 args but expects 1 or 2"}
         (first (lint! "(java.lang.Thread/sleep 1 2 3)" "--lang" "clj"))))
  (is (= {:file "<stdin>",
          :row 1,
          :col 1,
          :level :error,
          :message "java.lang.Math/pow is called with 3 args but expects 2"}
         (first (lint! "(Math/pow 1 2 3)" "--lang" "clj"))))
  (is (= {:file "<stdin>",
          :row 1,
          :col 1,
          :level :error,
          :message "java.math.BigInteger/valueOf is called with 3 args but expects 1"}
         (first (lint! "(BigInteger/valueOf 1 2 3)" "--lang" "clj"))))
  (is (empty?
       (first (lint! "(java.lang.Thread/sleep 1 2 3)" "--lang" "cljs"))))
  (is (= {:file "<stdin>",
          :row 1,
          :col 9,
          :level :error,
          :message "java.lang.Thread/sleep is called with 3 args but expects 1 or 2"}
         (first (lint! "#?(:clj (java.lang.Thread/sleep 1 2 3))" "--lang" "cljc")))))

(deftest resolve-core-ns-test
  (assert-submap '{:file "<stdin>",
                   :row 1,
                   :col 1,
                   :level :error,
                   :message "clojure.core/vec is called with 0 args but expects 1"}
                 (first (lint! "(clojure.core/vec)" "--lang" "clj")))
  (assert-submap '{:file "<stdin>",
                   :row 1,
                   :col 1,
                   :level :error,
                   :message "cljs.core/vec is called with 0 args but expects 1"}
                 (first (lint! "(cljs.core/vec)" "--lang" "cljs")))
  (assert-submap '{:file "<stdin>",
                   :row 1,
                   :col 1,
                   :level :error,
                   :message "cljs.core/vec is called with 0 args but expects 1"}
                 (first (lint! "(clojure.core/vec)" "--lang" "cljs"))))

(deftest override-test
  (doseq [lang [:clj :cljs]]
    (testing (str "lang: " (name lang))
      (assert-submaps
       [{:file "<stdin>",
         :row 1,
         :col 1,
         :level :error,
         :message (str (case lang
                         :clj "clojure"
                         :cljs "cljs") ".core/quote is called with 3 args but expects 1")}]
       (lint! "(quote 1 2 3)" "--lang" (name lang)))))
  (is (empty? (lint! "(cljs.core/array 1 2 3)" "--lang" "cljs"))))

(deftest cljs-clojure-ns-alias-test []
  (assert-submap '{:file "<stdin>",
                   :row 2,
                   :col 1,
                   :level :error,
                   :message "cljs.test/do-report is called with 3 args but expects 1"}
                 (first (lint! "(ns foo (:require [clojure.test :as t]))
(t/do-report 1 2 3)" "--lang" "cljs"))))

(deftest prefix-libspec-test []
  (assert-submaps
   '({:file "corpus/prefixed_libspec.clj",
      :row 14,
      :col 1,
      :level :error,
      :message "foo.bar.baz/b is called with 0 args but expects 1"}
     {:file "corpus/prefixed_libspec.clj",
      :row 15,
      :col 1,
      :level :error,
      :message "foo.baz/c is called with 0 args but expects 1"})
   (lint! (io/file "corpus" "prefixed_libspec.clj"))))

(deftest rename-test
  (testing "the renamed function isn't available under the referred name"
    (assert-submaps
     '({:file "<stdin>",
        :row 2,
        :col 11,
        :level :error,
        :message "clojure.string/includes? is called with 1 arg but expects 2"})
     (lint! "(ns foo (:require [clojure.string :refer [includes?] :rename {includes? i}]))
          (i \"str\")
          (includes? \"str\")"))))

(deftest refer-all-rename-test
  (testing ":require with :refer :all and :rename"
    (assert-submaps '({:file "corpus/refer_all.clj",
                       :row 10,
                       :col 1,
                       :level :error,
                       :message "funs/foo is called with 0 args but expects 1"}
                      {:file "corpus/refer_all.clj",
                       :row 11,
                       :col 1,
                       :level :error,
                       :message "funs/bar is called with 0 args but expects 1"})
                    (lint! (io/file "corpus" "refer_all.clj")))
    (assert-submaps '({:file "corpus/refer_all.cljs",
                       :row 8,
                       :col 1,
                       :level :error,
                       :message "macros/foo is called with 0 args but expects 1"})
                    (lint! (io/file "corpus" "refer_all.cljs")))))

(deftest alias-test
  (assert-submap
   '{:file "<stdin>",
     :row 1,
     :col 35,
     :level :error,
     :message "clojure.core/select-keys is called with 0 args but expects 2"}
   (first (lint! "(ns foo) (alias 'c 'clojure.core) (c/select-keys)"))))

(deftest case-test
  (testing "case dispatch values should not be linted as function calls"
    (assert-submaps
     '({:file "corpus/case.clj",
        :row 7,
        :col 3,
        :level :error,
        :message "clojure.core/filter is called with 3 args but expects 1 or 2"}
       {:file "corpus/case.clj",
        :row 9,
        :col 3,
        :level :error,
        :message "clojure.core/filter is called with 3 args but expects 1 or 2"}
       {:file "corpus/case.clj",
        :row 14,
        :col 3,
        :level :error,
        :message "clojure.core/filter is called with 3 args but expects 1 or 2"}
       {:file "corpus/case.clj",
        :row 15,
        :col 3,
        :level :error,
        :message "clojure.core/odd? is called with 2 args but expects 1"})
     (lint! (io/file "corpus" "case.clj"))))
  (testing "no false positive when using defn in case list dispatch"
    (is (empty? (lint! "(case x (defn select-keys) 1 2)")))))

(deftest local-bindings-test
  (is (empty? (lint! "(fn [select-keys] (select-keys))")))
  (is (empty? (lint! "(fn [[select-keys x y z]] (select-keys))")))
  (is (empty? (lint! "(fn [{:keys [:select-keys :b]}] (select-keys))")))
  (is (empty? (lint! "(defn foo [{:keys [select-keys :b]}]
    (let [x 1] (select-keys)))")))
  (is (seq (lint! "(defn foo ([select-keys]) ([x y] (select-keys)))")))
  (is (empty? (lint! "(if-let [select-keys (fn [])] (select-keys))")))
  (is (empty? (lint! "(when-let [select-keys (fn [])] (select-keys))")))
  (is (empty? (lint! "(fn foo [x] (foo x))")))
  (is (empty? (lint! "(fn select-keys [x] (select-keys 1))")))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 13,
      :level :error,
      :message "foo is called with 3 args but expects 1"})
   (lint! "(fn foo [x] (foo 1 2 3))"))
  (is (empty? (lint! "(fn foo ([x] (foo 1 2)) ([x y]))")))
  (assert-submaps
   '({:message "f is called with 3 args but expects 0"})
   (lint! "(let [f (fn [])] (f 1 2 3))"))
  (assert-submaps
   '({:message "f is called with 3 args but expects 0"})
   (lint! "(let [f #()] (f 1 2 3))"))
  (assert-submaps
   '({:message "f is called with 0 args but expects 1 or more"})
   (lint! "(let [f #(apply println % %&)] (f))"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 19,
      :level :error,
      :message "fn is called with 1 arg but expects 0"})
   (lint! "(let [fn (fn [])] (fn 1))"))
  (is (empty? (lint! "(let [f #(apply println % %&)] (f 1))")))
  (is (empty? (lint! "(let [f #(apply println % %&)] (f 1 2 3 4 5 6))")))
  (is (empty? (lint! "(fn ^:static meta [x] (if (instance? clojure.lang.IMeta x)
                       (. ^clojure.lang.IMeta x (meta))))")))
  (is (empty? (lint! "(doseq [fn [inc]] (fn 1))")))
  (is (empty?
       (lint! "(select-keys (let [x (fn [])] (x 1 2 3)) [])" "--config"
              "{:linters {:invalid-arity {:skip-args [clojure.core/select-keys]}
                          :unresolved-symbol {:level :off}}}"))))

(deftest let-test
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 6,
    :level :error,
    :message "let binding vector requires even number of forms"}
   (first (lint! "(let [x 1 y])")))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 13,
      :level :error,
      :message "clojure.core/select-keys is called with 0 args but expects 2"})
   (lint! "(let [x 1 y (select-keys)])"))
  (is (empty? (lint! "(let [select-keys (fn []) y (select-keys)])")))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 19,
      :level :error,
      :message "f is called with 1 arg but expects 0"})
   (lint! "(let [f (fn []) y (f 1)])"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 18,
      :level :error,
      :message "f is called with 1 arg but expects 0"})
   (lint! "(let [f (fn [])] (f 1))"))
  (is (empty (lint! "(let [f (fn []) f (fn [_]) y (f 1)])")))
  (is (empty? (lint! "(let [err (fn [& msg])] (err 1 2 3))"))))

(deftest if-let-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :error,
      :message "clojure.core/if-let is called with 1 arg but expects 2, 3 or more"}
     {:file "<stdin>",
      :row 1,
      :col 9,
      :level :error,
      :message "if-let binding vector requires exactly 2 forms"})
   (lint! "(if-let [x 1 y 2])"))
  (assert-submap
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :error,
      :message "clojure.core/if-let is called with 1 arg but expects 2, 3 or more"}
     {:file "<stdin>",
      :row 1,
      :col 9,
      :level :error,
      :message "if-let binding vector requires exactly 2 forms"})
   (lint! "(if-let [x 1 y])"))
  (is (empty? (lint! "(if-let [{:keys [:row :col]} {:row 1 :col 2}] row)"))))

(deftest when-let-test
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 11,
    :level :error,
    :message "when-let binding vector requires exactly 2 forms"}
   (first (lint! "(when-let [x 1 y 2])")))
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 11,
    :level :error,
    :message "when-let binding vector requires exactly 2 forms"}
   (first (lint! "(when-let [x 1 y])")))
  (is (empty? (lint! "(when-let [{:keys [:row :col]} {:row 1 :col 2}])"))))

(deftest config-test
  (is (empty?
       (lint! "(select-keys 1 2 3)" '{:linters {:invalid-arity {:level :off}}})))
  (is (empty?
       (lint! "(clojure.core/is-annotation? 1)" '{:linters {:private-call {:level :off}}})))
  (is (empty?
       (lint! "(def (def x 1))" '{:linters {:inline-def {:level :off}}})))
  (is (empty?
       (lint! "(do (do 1 2 3))" '{:linters {:redundant-do {:level :off}}})))
  (is (empty?
       (lint! "(let [x 1] (let [y 2]))" '{:linters {:redundant-let {:level :off}}})))
  (is (empty?
       (lint! "(cond 1 2)" '{:linters {:cond-else {:level :off}}})))
  (is (str/starts-with?
       (with-out-str
         (lint! (io/file "corpus") '{:output {:progress true}}))
       "...."))
  (doseq [format [:json :edn]]
    (is (not (str/starts-with?
              (with-out-str
                (lint! (io/file "corpus")
                       {:output {:progress true :format format}}))
              "...."))))
  (is (not (some #(str/includes? % "datascript")
                 (map :file (lint! (io/file "corpus")
                                   '{:output {:exclude-files ["datascript"]}})))))
  (is (not (some #(str/includes? % "datascript")
                 (map :file (lint! (io/file "corpus")
                                   '{:output {:include-files ["inline_def"]}})))))
  (is (str/starts-with?
       (with-out-str
         (with-in-str "(do 1)"
           (main "--lint" "-" "--config" (str '{:output {:pattern "{{LEVEL}}_{{filename}}"}
                                                :linters {:unresolved-symbol {:level :off}}}))))
       "WARNING_<stdin>"))
  (is (empty? (lint! "(comment (select-keys))" '{:skip-args [clojure.core/comment]
                                                 :linters {:unresolved-symbol {:level :off}}})))
  (assert-submap
   '({:file "<stdin>",
      :row 1,
      :col 16,
      :level :error,
      :message "user/foo is called with 2 args but expects 1"})
   (lint! "(defn foo [x]) (foo (comment 1 2 3) 2)" '{:skip-comments true}))
  (is (empty? (lint! "(ns foo (:require [foo.specs] [bar.specs])) (defn my-fn [x] x)"
                     '{:linters {:unused-namespace {:exclude [foo.specs bar.specs]}}})))
  (is (empty? (lint! "(ns foo (:require [foo.specs] [bar.specs])) (defn my-fn [x] x)"
                     '{:linters {:unused-namespace {:exclude [".*\\.specs$"]}}})))
  (is (empty? (lint! "(ns foo (:require [foo.specs] [bar.spex])) (defn my-fn [x] x)"
                     '{:linters {:unused-namespace {:exclude
                                                    [".*\\.specs$"
                                                     ".*\\.spex$"]}}}))))

(deftest replace-config-test
  (let [res (lint! (io/file "corpus") "--config" "^:replace {:linters {:redundant-let {:level :info}}}")]
    (is (pos? (count res)))
    (doseq [f res]
      (is (= :info (:level f))))))

(deftest map-duplicate-keys
  (is (= '({:file "<stdin>", :row 1, :col 7, :level :error, :message "duplicate key :a"}
           {:file "<stdin>",
            :row 1,
            :col 10,
            :level :error,
            :message "clojure.core/select-keys is called with 1 arg but expects 2"}
           {:file "<stdin>", :row 1, :col 35, :level :error, :message "duplicate key :a"})
         (lint! "{:a 1 :a (select-keys 1) :c {:a 1 :a 2}}")))
  (is (= '({:file "<stdin>", :row 1, :col 6, :level :error, :message "duplicate key 1"}
           {:file "<stdin>",
            :row 1,
            :col 18,
            :level :error,
            :message "duplicate key \"foo\""})
         (lint! "{1 1 1 1 \"foo\" 1 \"foo\" 2}"))))

(deftest map-missing-value
  (is (= '({:file "<stdin>",
            :row 1,
            :col 7,
            :level :error,
            :message "missing value for key :b"})
         (lint! "{:a 1 :b}")))
  (is (= '({:file "<stdin>",
            :row 1,
            :col 14,
            :level :error,
            :message "missing value for key :a"})
         (lint! "(let [x {:a {:a }}] x)")))
  (is (= '({:file "<stdin>",
            :row 1,
            :col 15,
            :level :error,
            :message "missing value for key :a"})
         (lint! "(loop [x {:a {:a }}] x)")))
  (is (= '({:file "<stdin>",
            :row 1,
            :col 10,
            :level :error,
            :message "missing value for key :post"})
         (lint! "(fn [x] {:post} x)"))))

(deftest set-duplicate-key
  (is (= '({:file "<stdin>",
            :row 1,
            :col 7,
            :level :error,
            :message "duplicate set element 1"})
         (lint! "#{1 2 1}"))))

(deftest macroexpand-test
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 8,
    :level :error,
    :message "clojure.core/select-keys is called with 1 arg but expects 2"}
   (first (lint! "(-> {} select-keys)")))
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 8,
    :level :error,
    :message "clojure.core/select-keys is called with 1 arg but expects 2"}
   (first (lint! "(-> {} (select-keys))")))
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 9,
    :level :error,
    :message "clojure.core/select-keys is called with 1 arg but expects 2"}
   (first (lint! "(->> {} select-keys)")))
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 9,
    :level :error,
    :message "clojure.core/select-keys is called with 1 arg but expects 2"}
   (first (lint! "(->> {} (select-keys))")))
  (testing "cats"
    (is (seq (lint! "(ns foo (:require [cats.core :as m])) (m/->= (right {}) (select-keys))")))
    (is (seq (lint! "(ns foo (:require [cats.core :as m])) (m/->>= (right {}) (select-keys))"))))
  (testing "with CLJC"
    (is (empty? (lint! "(-> 1 #?(:clj inc :cljs inc))" "--lang" "cljc")))
    (assert-submap
     {:file "<stdin>",
      :row 1,
      :col 15,
      :level :error,
      :message "java.lang.Math/pow is called with 1 arg but expects 2"}
     (first (lint! "(-> 1 #?(:clj (Math/pow)))" "--lang" "cljc"))))
  (testing "with type hints"
    (assert-submap
     {:file "<stdin>",
      :row 1,
      :col 60,
      :level :error,
      :message "clojure.string/includes? is called with 1 arg but expects 2"}
     (first (lint! "(ns foo (:require [clojure.string])) (-> \"foo\" ^String str clojure.string/includes?)")))
    (assert-submap
     {:file "<stdin>", :row 1, :col 12, :level :error, :message "duplicate key :a"}
     (first (lint! "(-> ^{:a 1 :a 2} [1 2 3])"))))
  (testing "macroexpansion of anon fn literal"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 2,
        :level :error,
        :message "clojure.core/select-keys is called with 1 arg but expects 2"})
     (lint! "#(select-keys %)"))
    (is (empty? (lint! "(let [f #(apply println %&)] (f 1 2 3 4))")))
    (testing "fix for issue #181: the let in the expansion is resolved to clojure.core and not the custom let"
      (assert-submaps '({:file "<stdin>",
                         :row 2,
                         :col 41,
                         :level :error,
                         :message "missing value for key :a"})
                      (lint! "(ns foo (:refer-clojure :exclude [let]))
        (defmacro let [_]) #(println % {:a})")))))

(deftest schema-defn-test
  (assert-submaps
   '({:file "corpus/schema/calls.clj",
      :row 4,
      :col 1,
      :level :error,
      :message "schema.defs/verify-signature is called with 0 args but expects 3"}
     {:file "corpus/schema/calls.clj",
      :row 4,
      :col 1,
      :level :error,
      :message "call to private function schema.defs/verify-signature"}
     {:file "corpus/schema/defs.clj",
      :row 10,
      :col 1,
      :level :error,
      :message "schema.defs/verify-signature is called with 2 args but expects 3"}
     {:file "corpus/schema/defs.clj",
      :row 12,
      :col 1,
      :level :error,
      :message "invalid function body"})
   (lint! (io/file "corpus" "schema")
          '{:linters {:unresolved-symbol {:level :error}}})))

(deftest in-ns-test
  (assert-submaps
   '({:file "corpus/in-ns/base_ns.clj",
      :row 5,
      :col 1,
      :level :error,
      :message "in-ns.base-ns/foo is called with 3 args but expects 0"}
     {:file "corpus/in-ns/in_ns.clj",
      :row 5,
      :col 1,
      :level :error,
      :message "in-ns.base-ns/foo is called with 3 args but expects 0"})
   (lint! (io/file "corpus" "in-ns")))
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 55,
    :level :error,
    :message "foo/foo-2 is called with 3 args but expects 0"}
   (first (lint! "(ns foo) (defn foo-1 [] (in-ns 'bar)) (defn foo-2 []) (foo-2 1 2 3)"))))

(deftest skip-args-test
  (is
   (empty?
    (lint! (io/file "corpus" "skip_args" "comment.cljs") '{:skip-args [cljs.core/comment]})))
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
   (lint! (io/file "corpus" "skip_args" "arity.clj") '{:skip-args [skip-args.arity/my-macro]}))
  (assert-submaps
   '({:file "corpus/skip_args/arity.clj",
      :row 6,
      :col 1,
      :level :error,
      :message "skip-args.arity/my-macro is called with 4 args but expects 3"})
   (lint! (io/file "corpus" "skip_args" "arity.clj") '{:linters {:invalid-arity {:skip-args [skip-args.arity/my-macro]}}})))

(deftest missing-test-assertion-test
  (is (empty? (lint! "(ns foo (:require [clojure.test :as t])) (t/deftest (t/is (odd? 1)))")))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 57,
      :level :warning,
      :message "missing test assertion"})
   (lint! "(ns foo (:require [clojure.test :as t])) (t/deftest foo (odd? 1))"))
  (assert-submaps
   '({:file "<stdin>",
      :row 2,
      :col 21,
      :level :warning,
      :message "missing test assertion"})
   (lint! "(ns foo (:require [clojure.test :as t] [clojure.set :as set]))
     (t/deftest foo (set/subset? #{1 2} #{1 2 3}))")))

(deftest recur-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 15,
      :level :error,
      :message "recur argument count mismatch (expected 1, got 2)"})
   (lint! "(defn foo [x] (recur x x))"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 9,
      :level :error,
      :message "recur argument count mismatch (expected 1, got 2)"})
   (lint! "(fn [x] (recur x x))"))
  (is (empty? (lint! "(defn foo [x & xs] (recur x [x]))")))
  (is (empty? (lint! "(defn foo ([]) ([x & xs] (recur x [x])))")))
  (is (empty? (lint! "(fn ([]) ([x y & xs] (recur x x [x])))")))
  (is (empty? (lint! "(loop [x 1 y 2] (recur x y))")))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 17,
      :level :error,
      :message "recur argument count mismatch (expected 2, got 3)"})
   (lint! "(loop [x 1 y 2] (recur x y x))"))
  (is (empty? (lint! "(ns foo (:require [clojure.core.async :refer [go-loop]])) (go-loop [x 1] (recur 1))")))
  (is (empty? (lint! "(ns foo (:require [clojure.core.async :refer [go-loop]]))
                        (defn foo [x y] (go-loop [x nil] (recur 1)))")))
  (is (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 74,
          :level :error,
          :message "recur argument count mismatch (expected 1, got 2)"})
       (lint! "(ns foo (:require [clojure.core.async :refer [go-loop]])) (go-loop [x 1] (recur 1 2))")))
  (is (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 85,
          :level :error,
          :message "recur argument count mismatch (expected 1, got 2)"})
       (lint! "(ns foo (:require-macros [cljs.core.async.macros :refer [go-loop]])) (go-loop [x 1] (recur 1 2))")))
  (is (assert-submaps
       '({:file "<stdin>",
          :row 1,
          :col 78,
          :level :error,
          :message "recur argument count mismatch (expected 1, got 2)"})
       (lint! "(ns foo (:require-macros [cljs.core.async :refer [go-loop]])) (go-loop [x 1] (recur 1 2))")))
  (is (empty? (lint! "#(recur)")))
  (is (empty? (lint! "(ns foo (:require [clojure.core.async :refer [thread]])) (thread (recur))")))
  (is (empty? (lint! "(ns clojure.core.async) (defmacro thread [& body]) (thread (when true (recur)))")))
  (is (empty? (lint! "(fn* ^:static cons [x seq] (recur 1 2))"))))

(deftest lint-as-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 93,
      :level :error,
      :message "foo/foo is called with 3 args but expects 1"})
   (lint! "(ns foo) (defmacro my-defn [name args & body] `(defn ~name ~args ~@body)) (my-defn foo [x]) (foo 1 2 3)"
          '{:lint-as {foo/my-defn clojure.core/defn}}))
  (is (empty?
       (lint! "(ns foo) (defmacro deftest [name & body] `(defn ~name [] ~@body)) (deftest foo)"
              '{:linters {:unresolved-symbol {:level :warning}}
                :lint-as {foo/deftest clojure.test/deftest}}))))

(deftest letfn-test
  (assert-submaps '({:file "<stdin>",
                     :row 1,
                     :col 11,
                     :level :error,
                     :message "clojure.core/select-keys is called with 0 args but expects 2"})
                  (lint! "(letfn [] (select-keys))"))
  (assert-submaps '({:file "<stdin>",
                     :row 1,
                     :col 19,
                     :level :error,
                     :message "f1 is called with 0 args but expects 1"})
                  (lint! "(letfn [(f1 [_])] (f1))"))
  (assert-submaps '({:file "<stdin>",
                     :row 1,
                     :col 17,
                     :level :error,
                     :message "recur argument count mismatch (expected 1, got 0)"})
                  (lint! "(letfn [(f1 [_] (recur))])"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 17,
      :level :error,
      :message "f2 is called with 0 args but expects 1"})
   (lint! "(letfn [(f1 [_] (f2)) (f2 [_])])")))

(deftest unused-namespace-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 20,
      :level :warning,
      :message "namespace clojure.core.async is required but never used"})
   (lint! "(ns foo (:require [clojure.core.async :refer [go-loop]]))"))
  (assert-submaps
   '({:file "<stdin>",
      :row 2,
      :col 30,
      :level :warning,
      :message "namespace rewrite-clj.node is required but never used"}
     {:file "<stdin>",
      :row 2,
      :col 46,
      :level :warning,
      :message "namespace rewrite-clj.reader is required but never used"})
   (lint! "(ns rewrite-clj.parser
     (:require [rewrite-clj [node :as node] [reader :as reader]]))"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 32,
      :level :warning,
      :message "namespace baz is required but never used"})
   (lint! "(ns foo (:require [bar :as b] [baz :as baz])) #::{:a #::bar{:a 1}}"))
  (assert-submap
   '({:file "<stdin>",
      :row 1,
      :col 18,
      :level :warning,
      :message "namespace clojure.string is required but never used"})
   (lint! "(ns f (:require [clojure.string :as s])) :s/foo"))
  (assert-submap
   '({:file "<stdin>",
      :row 1,
      :col 20,
      :level :warning,
      :message "namespace bar is required but never used"})
   (lint! "(ns foo (:require [bar :as b])) #:b{:a 1}"))
  (testing "simple libspecs (without as or refer) are not reported"
    (is (empty? (lint! "(ns foo (:require [foo.specs]))")))
    (is (empty? (lint! "(ns foo (:require foo.specs))")))
    (is (seq (lint! "(ns foo (:require [foo.specs]))"
                    '{:linters {:unused-namespace {:simple-libspec true}}}))))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 12,
      :level :warning,
      :message "namespace clojure.set is required but never used"})
   (lint! "(require '[clojure.set :refer [join]])"))
  (is (empty?
       (lint! "(ns foo (:require [clojure.core.async :refer [go-loop]]))
         ,(ns bar)
         ,(in-ns 'foo)
         ,(go-loop [])")))
  (is (empty? (lint! "(ns foo (:require [clojure.set :as set :refer [difference]]))
    (reduce! set/difference #{} [])")))
  (is (empty? (lint! "(ns foo (:require [clojure.set :as set :refer [difference]]))
    (reduce! difference #{} [])")))
  (is (empty? (lint! "(ns foo (:require [clojure.set :as set :refer [difference]]))
    (defmacro foo [] `(set/difference #{} #{}))")))
  (is (empty? (lint! "(ns foo (:require [clojure.core.async :refer [go-loop]])) (go-loop [x 1] (recur 1))")))
  (is (empty? (lint! "(ns foo (:require bar)) ::bar/bar")))
  (is (empty? (lint! "(ns foo (:require [bar :as b])) ::b/bar")))
  (is (empty? (lint! "(ns foo (:require [bar :as b])) #::b{:a 1}")))
  (is (empty? (lint! "(ns foo (:require [bar :as b] baz)) #::baz{:a #::bar{:a 1}}")))
  (is (empty? (lint! "(ns foo (:require goog.math.Long)) (instance? goog.math.Long 1)")))
  (is (empty? (lint! "(ns foo (:require [schema.core :as s] [bar :as bar])) (s/defn foo :- bar/Schema [])")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str])) {str/join true}")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str])) {true str/join}")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str])) [str/join]")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str]))
                       (defn my-id [{:keys [:id] :or {id (str/lower-case \"HI\")}}] id)")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str]))
                       (fn [{:keys [:id] :or {id (str/lower-case \"HI\")}}] id)")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str]))
                       (let [{:keys [:id] :or {id (str/lower-case \"HI\")}} {:id \"hello\"}] id)")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str]))
                       (if-let [{:keys [:id] :or {id (str/lower-case \"HI\")}} {:id \"hello\"}] id)")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str]))
                       (loop [{:keys [:id] :or {id (str/lower-case \"HI\")}} {:id \"hello\"}])")))
  (is (empty? (lint! (io/file "corpus" "shadow_cljs" "default.cljs"))))
  (is (empty? (lint! "(ns foo (:require [bar])) (:id bar/x)")))
  (is (empty? (lint! (io/file "corpus" "no_unused_namespace.clj"))))
  (is (empty? (lint! "(ns foo (:require [bar :as b])) (let [{::b/keys [:baz]} nil] baz)")))
  (is (empty? (lint! "(require '[clojure.set :refer [join]]) join"))))

(deftest namespace-syntax-test
  (assert-submaps '({:file "<stdin>",
                     :row 1,
                     :col 5,
                     :level :error,
                     :message "namespace name expected"})
                  (lint! "(ns \"hello\")")))

(deftest call-as-use-test
  (is (empty?
       (lint!
        "(extend-protocol
           clojure.lang.IChunkedSeq
           (internal-reduce [s f val]
            (recur (chunk-next s) f val)))"))))

(deftest redefined-var-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 15,
      :level :warning,
      :message "redefined var #'user/foo"})
   (lint! "(defn foo []) (defn foo [])"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :warning,
      :message "inc already refers to #'clojure.core/inc"})
   (lint! "(defn inc [])"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :warning,
      :message "inc already refers to #'cljs.core/inc"})
   (lint! "(defn inc [])" "--lang" "cljs"))
  (assert-submaps '({:file "<stdin>",
                     :row 1,
                     :col 20,
                     :level :warning,
                     :message "namespace bar is required but never used"}
                    {:file "<stdin>",
                     :row 1,
                     :col 38,
                     :level :warning,
                     :message "x already refers to #'bar/x"})
                  (lint! "(ns foo (:require [bar :refer [x]])) (defn x [])"))
  (is (empty? (lint! "(defn foo [])")))
  (is (empty? (lint! "(ns foo (:refer-clojure :exclude [inc])) (defn inc [])")))
  (is (empty? (lint! "(declare foo) (def foo 1)")))
  (is (empty? (lint! "(def foo 1) (declare foo)")))
  (is (empty? (lint! "(if (odd? 3) (def foo 1) (def foo 2))"))))

(deftest unreachable-code-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 15,
      :level :warning,
      :message "unreachable code"})
   (lint! "(cond :else 1 (odd? 1) 2)")))

(deftest dont-crash-analyzer-test
  (doseq [example ["(let)" "(if-let)" "(when-let)" "(loop)" "(doseq)"]
          :let [prog (str example " (inc)")]]
    (is (some (fn [finding]
                (submap? {:file "<stdin>",
                          :row 1,
                          :level :error,
                          :message "clojure.core/inc is called with 0 args but expects 1"}
                         finding))
              (lint! prog)))))

(deftest for-doseq-test
  (is (empty? (lint! "(for [select-keys []] (select-keys 1))")))
  (is (empty? (lint! "(doseq [select-keys []] (select-keys 1))"))))

(deftest keyword-call-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :error,
      :message "keyword :x is called with 3 args but expects 1 or 2"})
   (lint! "(:x 1 2 3)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 10,
      :level :error,
      :message "keyword :b/x is called with 3 args but expects 1 or 2"})
   (lint! "(ns foo) (:b/x {:bar/x 1} 1 2)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 33,
      :level :error,
      :message "keyword :bar/x is called with 3 args but expects 1 or 2"})
   (lint! "(ns foo (:require [bar :as b])) (::b/x {:bar/x 1} 1 2)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 10,
      :level :error,
      :message "keyword ::b/x is called with 3 args but expects 1 or 2"})
   (lint! "(ns foo) (::b/x {:bar/x 1} 1 2)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 10,
      :level :error,
      :message "keyword :foo/x is called with 3 args but expects 1 or 2"})
   (lint! "(ns foo) (::x {::x 1} 2 3)"))
  (is (empty?
       (lint! "(select-keys (:more 1 2 3 4) [])" "--config"
              "{:linters {:invalid-arity {:skip-args [clojure.core/select-keys]}}}"))))

(deftest map-call-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :error,
      :message "map is called with 0 args but expects 1 or 2"})
   (lint! "({:a 1})"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :error,
      :message "map is called with 3 args but expects 1 or 2"})
   (lint! "({:a 1} 1 2 3)"))
  (is (empty? (lint! "(foo ({:a 1} 1 2 3))" "--config"
                     "{:linters {:invalid-arity {:skip-args [user/foo]}
                                 :unresolved-symbol {:level :off}}}"))))

(deftest symbol-call-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :error,
      :message "symbol is called with 0 args but expects 1 or 2"})
   (lint! "('foo)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :error,
      :message "symbol is called with 3 args but expects 1 or 2"})
   (lint! "('foo 1 2 3)"))
  (is (empty? (lint! "(foo ('foo 1 2 3))" "--config"
                     "{:linters {:invalid-arity {:skip-args [user/foo]}
                                 :unresolved-symbol {:level :off}}}"))))

(deftest not-a-function-test
  (assert-submaps '({:file "<stdin>",
                     :row 1,
                     :col 1,
                     :level :error,
                     :message "a boolean is not a function"})
                  (lint! "(true 1)"))
  (assert-submaps '({:file "<stdin>",
                     :row 1,
                     :col 1,
                     :level :error,
                     :message "a string is not a function"})
                  (lint! "(\"foo\" 1)"))
  (assert-submaps '({:file "<stdin>",
                     :row 1,
                     :col 1,
                     :level :error,
                     :message "a number is not a function"})
                  (lint! "(1 1)"))
  (is (empty? (lint! "'(1 1)")))
  (is (empty? (lint! "(foo (1 1))" "--config"
                     "{:linters {:not-a-function {:skip-args [user/foo]}
                                 :unresolved-symbol {:level :off}}}"))))

(deftest cljs-self-require-test
  (is (empty? (lint! (io/file "corpus" "cljs_self_require.cljc")))))

(deftest redefined-test-test
  (assert-submaps
   '({:file "corpus/redefined_deftest.clj",
      :row 4,
      :col 1,
      :level :error,
      :message "clojure.test/deftest is called with 0 args but expects 1 or more"}
     {:file "corpus/redefined_deftest.clj",
      :row 7,
      :col 1,
      :level :warning,
      :message "redefined var #'redefined-deftest/foo"}
     {:file "corpus/redefined_deftest.clj",
      :row 9,
      :col 1,
      :level :error,
      :message "redefined-deftest/foo is called with 1 arg but expects 0"})
   (lint! (io/file "corpus" "redefined_deftest.clj"))))

(deftest unused-binding-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 7, :level :warning, :message "unused binding x"})
   (lint! "(let [x 1])" '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 12,
      :level :warning,
      :message "unused binding x"})
   (lint! "(defn foo [x])"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 15,
      :level :warning,
      :message "unused binding id"})
   (lint! "(let [{:keys [patient/id order/id]} {}] id)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 14,
      :level :warning,
      :message "unused binding a"})
   (lint! "(fn [{:keys [:a] :or {a 1}}])"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
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
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 10,
      :level :warning,
      :message "unused binding x"})
   (lint! "(if-let [x 1] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 12,
      :level :warning,
      :message "unused binding x"})
   (lint! "(when-let [x 1] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 13,
      :level :warning,
      :message "unused binding x"})
   (lint! "(when-some [x 1] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>",
      :level :warning,
      :message "unused binding x"})
   (lint! "(for [x []] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>",
      :level :warning,
      :message "unused binding x"})
   (lint! "(doseq [x []] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
   '({:level :warning,
      :message "unused binding x"}
     {:level :warning,
      :message "unused binding y"})
   (lint! "(with-open [x ? y ?] 1)"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
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
  (assert-submaps
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
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 24,
      :level :warning,
      :message "unused binding x"})
   (lint! "(defmacro foo [] (let [x 1] `(inc x)))"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 12,
      :level :warning,
      :message "unused binding x"})
   (lint! "(defn foo [x] (quote x))"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 17,
      :level :warning,
      :message "unused binding variadic"})
   (lint! "(let [{^boolean variadic :variadic?} {}] [])"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 8,
      :level :warning,
      :message "unused binding a"})
   (lint! "#(let [a %])"
          '{:linters {:unused-binding {:level :warning}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :warning,
      :message "unused binding a"})
   (lint! "(let [a 1] `{:a 'a})"
          '{:linters {:unused-binding {:level :warning}}}))
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
  (is (empty? (lint! "(let [a 1] (cond-> (.getFoo a) x))"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(defmacro foo [] (let [sym 'my-symbol] `(do '~sym)))"
                     '{:linters {:unused-binding {:level :warning}}})))
  (is (empty? (lint! "(let [s 'clojure.string] (require s))"
                     '{:linters {:unused-binding {:level :warning}}}))))

(deftest unsupported-binding-form-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 12,
      :level :error,
      :message "unsupported binding form :x"})
   (lint! "(defn foo [:x])"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 12,
      :level :error,
      :message "unsupported binding form a/a"})
   (lint! "(defn foo [a/a])"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :error,
      :message "unsupported binding form 1"})
   (lint! "(let [1 1])"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 7,
      :level :error,
      :message "unsupported binding form (x)"})
   (lint! "(let [(x) 1])"))
  (is (empty? (lint! "(fn [[x y z] :as x])")))
  (is (empty? (lint! "(fn [[x y z & xs]])")))
  (is (empty? (lint! "(let [^String x \"foo\"])"))))

(deftest non-destructured-binding-test
  (doseq [input ["(let [{:keys [:i] :or {i 2 j 3}} {}] i)"
                 "(let [{:or {i 2 j 3} :keys [:i]} {}] i)"]]
    (assert-submaps '({:file "<stdin>",
                       :row 1
                       :level :warning,
                       :message "j is not bound in this destructuring form"})
                    (lint! input))))

(deftest output-test
  (is (str/starts-with?
       (with-in-str ""
         (with-out-str
           (main  "--lint" "-" "--config" "{:output {:summary true}}")))
       "linting took"))
  (is (not
       (str/starts-with?
        (with-in-str ""
          (with-out-str
            (main  "--lint" "-" "--config" "{:output {:summary false}}")))
        "linting took")))
  (is (= '({:filename "<stdin>",
            :row 1,
            :col 1,
            :level :error,
            :message "clojure.core/inc is called with 0 args but expects 1"}
           {:filename "<stdin>",
            :row 1,
            :col 6,
            :level :error,
            :message "clojure.core/dec is called with 0 args but expects 1"})
         (let [parse-fn
               (fn [line]
                 (when-let [[_ file row col level message]
                            (re-matches #"(.+):(\d+):(\d+): (\w+): (.*)" line)]
                   {:filename file
                    :row (Integer/parseInt row)
                    :col (Integer/parseInt col)
                    :level (keyword level)
                    :message message}))
               text (with-in-str "(inc)(dec)"
                      (with-out-str
                        (main  "--lint" "-" "--config" "{:output {:format :text}}")))]
           (keep parse-fn (str/split-lines text)))))
  (doseq [[output-format parse-fn]
          [[:edn edn/read-string]
           [:json #(cheshire/parse-string % true)]]
          summary? [true false]]
    (let [output (with-in-str "(inc)(dec)"
                   (with-out-str
                     (main  "--lint" "-" "--config"
                            (format "{:output {:format %s :summary %s}}"
                                    output-format summary?))))
          parsed (parse-fn output)]
      (assert-submap {:findings
                      [{:type (case output-format :edn :invalid-arity
                                    "invalid-arity"),
                        :filename "<stdin>",
                        :row 1,
                        :col 1,
                        :level (case output-format :edn :error
                                     "error"),
                        :message "clojure.core/inc is called with 0 args but expects 1"}
                       {:type (case output-format :edn :invalid-arity
                                    "invalid-arity"),
                        :filename "<stdin>",
                        :row 1,
                        :col 6,
                        :level (case output-format :edn :error
                                     "error"),
                        :message "clojure.core/dec is called with 0 args but expects 1"}]}
                     parsed)
      (if summary?
        (assert-submap '{:error 2}
                       (:summary parsed))
        (is (nil? (find parsed :summary))))))
  (doseq [[output-format parse-fn]
          [[:edn edn/read-string]
           [:json #(cheshire/parse-string % true)]]]
    (let [output (with-in-str "(inc)(dec)"
                   (with-out-str
                     (main  "--lint" "-" "--config"
                            (format "{:output {:format %s}}" output-format))))
          parsed (parse-fn output)]
      (is (map? parsed))))
  (testing "JSON output escapes special characters"
    (let [output (with-in-str "{\"foo\" 1 \"foo\" 1}"
                   (with-out-str
                     (main  "--lint" "-" "--config"
                            (format "{:output {:format %s}}" :json))))
          parsed (cheshire/parse-string output true)]
      (is (map? parsed)))
    (let [output (with-in-str "{:a 1}"
                   (with-out-str
                     (main  "--lint" "\"foo\".clj" "--config"
                            (format "{:output {:format %s}}" :json))))
          parsed (cheshire/parse-string output true)]
      (is (map? parsed)))))

(deftest defprotocol-test
  (assert-submaps
   '({:file "corpus/defprotocol.clj",
      :row 14,
      :col 1,
      :level :error,
      :message "defprotocol/-foo is called with 4 args but expects 1, 2 or 3"})
   (lint! (io/file "corpus" "defprotocol.clj"))))

(deftest defrecord-test
  (assert-submaps
   '({:file "corpus/defrecord.clj",
      :row 8,
      :col 1,
      :level :error,
      :message "defrecord/->Thing is called with 3 args but expects 2"}
     {:file "corpus/defrecord.clj",
      :row 9,
      :col 1,
      :level :error,
      :message "defrecord/map->Thing is called with 2 args but expects 1"})
   (lint! (io/file "corpus" "defrecord.clj")
          "--config" "{:linters {:unused-binding {:level :warning}}}")))

(deftest defmulti-test
  (assert-submaps
   '({:file "corpus/defmulti.clj",
      :row 7,
      :col 12,
      :level :error,
      :message "unresolved symbol greetingx"}
     {:file "corpus/defmulti.clj",
      :row 7,
      :col 35,
      :level :warning,
      :message "unused binding y"}
     {:file "corpus/defmulti.clj",
      :row 13,
      :col 24,
      :level :warning,
      :message "unused binding y"}
     {:file "corpus/defmulti.clj",
      :row 13,
      :col 39,
      :level :error,
      :message "clojure.core/inc is called with 0 args but expects 1"})
   (lint! (io/file "corpus" "defmulti.clj")
          '{:linters {:unused-binding {:level :warning}
                      :unresolved-symbol {:level :error}}})))

(deftest unresolved-symbol-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 2,
      :level :error,
      :message "unresolved symbol x"})
   (lint! "(x)" "--config" "{:linters {:unresolved-symbol {:level :error}}}"))
  (testing "unresolved symbol is reported only once"
    (assert-submaps
     '({:file "<stdin>",
        :row 1,
        :col 2,
        :level :error,
        :message "unresolved symbol x"})
     (lint! "(x)(x)" "--config" "{:linters {:unresolved-symbol {:level :error}}}")))
  (assert-submaps '({:file "corpus/unresolved_symbol.clj",
                     :row 11,
                     :col 4,
                     :level :error,
                     :message "unresolved symbol unresolved-fn1"}
                    {:file "corpus/unresolved_symbol.clj",
                     :row 18,
                     :col 1,
                     :level :error,
                     :message "clojure.set/join is called with 0 args but expects 2 or 3"})
                  (lint! (io/file "corpus" "unresolved_symbol.clj")
                         '{:linters {:unresolved-symbol {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :error,
      :message "unresolved symbol x"})
   (lint! "x"
          '{:linters {:unresolved-symbol {:level :error}}}))
  (is (empty? (lint! "(try 1 (catch Exception e e) (finally 3))"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(defmulti foo (fn [_])) (defmethod foo :dude [_]) (foo 1)"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(defonce foo (fn [_])) (foo 1)"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(defmacro foo [] `(let [x# 1]))"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(let [e (Exception.)] (.. e getCause getMessage))"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "`(let [e# (Exception.)] (.. e# getCause getMessage))"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "`~@(let [v nil] (resolve v))"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "#inst \"2019\""
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(if-some [foo true] foo false)"
                     {:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(ns foo) (defn foo [_ _ _]) (foo x y z)"
                     '{:linters {:unresolved-symbol {:level :error
                                                     :exclude [(foo/foo [x y z])]}}})))
  (is (empty? (lint! "(defprotocol IFoo) IFoo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(defrecord Foo []) Foo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(deftype Foo []) Foo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "Object BigDecimal"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(ns foo (:import [my.package Foo])) Foo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(ns foo (:import (my.package Foo))) Foo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(ns foo (:import my.package.Foo)) Foo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(dotimes [_ 10] (println \"hello\"))"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(let [{{:keys [:a]} :stats} {:stats {:a 1}}] a)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "java.math.BitSieve"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "Class Object Cloneable NoSuchFieldError String"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(let [{:keys [:as]} {:as 1}] as)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(as-> 1 x)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(let [x 1 {:keys [:a] :or {a x}} {:a 1}])"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(defmacro foo [] &env &form)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(let [a (into-array [])] (areduce a i ret 0 (+ ret (aget a i))))"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(this-as x [x x x])"
                     '{:linters {:unresolved-symbol {:level :error}}}
                     "--lang" "cljs")))
  (is (empty? (lint! "(as-> 10 x (inc x) (inc x))"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "((memfn ^String substring start end) \"foo\" 0 1)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(goog-define foo \"default\")"
                     '{:linters {:unresolved-symbol {:level :error}}}
                     "--lang" "cljs")))
  (is (empty? (lint! "(definterface Foo (foo [])) Foo"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "Var Namespace LazySeq UUID"
                     '{:linters {:unresolved-symbol {:level :error}}}
                     "--lang" "cljs")))
  (is (empty? (lint! "(defn str-to-str [s] {:post [(string? %)]} s)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (lint! "(fn [s] {:post [(string? %)]} s)"
                     '{:linters {:unresolved-symbol {:level :error}}})))
  (is (empty? (:findings
               (edn/read-string
                (with-out-str
                  (lint! "#(inc %4)"
                         '{:linters {:unused-binding {:level :error}}
                           :output {:format :edn}}))))))
  (is (empty? (lint! "(ns foo (:import [java.util.regex Pattern])) Pattern/compile"
                     '{:linters {:unresolved-symbol {:level :error}}}))))

(deftest misc-false-positives-test
  (is (empty? (lint! "(cond-> 1 true (as-> x (inc x)))")))
  (is (empty? (lint! "(reify clojure.lang.IDeref (deref [_] nil))")))
  (is (empty? (lint! "(ns foo) (defn foo [] (ns bar (:require [clojure.string :as s])))")))
  (is (empty? (lint! "(defn foo [x y z] ^{:a x :b y :c z} [1 2 3])")))
  (is (empty? (lint! (io/file "corpus" "deftest.cljc")
                     '{:linters {:unresolved-symbol {:level :error}}}))))

(deftest file-error-test
  (assert-submaps '({:file "not-existing.clj",
                     :row 0,
                     :col 0,
                     :level :error,
                     :message "file does not exist"})
                  (lint! (io/file "not-existing.clj"))))

(deftest edn-test
  (assert-submaps
   '({:file "corpus/edn/edn.edn",
      :row 1,
      :col 10,
      :level :error,
      :message "duplicate key a"})
   (lint! (io/file "corpus" "edn"))))

(deftest spec-test
  (assert-submaps
   '({:file "corpus/spec_syntax.clj",
      :row 9,
      :col 9,
      :level :error,
      :message "expected symbol"}
     {:file "corpus/spec_syntax.clj",
      :row 9,
      :col 11,
      :level :error,
      :message "missing value for key :args"}
     {:file "corpus/spec_syntax.clj",
      :row 11,
      :col 13,
      :level :error,
      :message "unknown option :xargs"}
     {:file "corpus/spec_syntax.clj",
      :row 20,
      :col 9,
      :level :error,
      :message "unresolved symbol xstr/starts-with?"})
   (lint! (io/file "corpus" "spec_syntax.clj")
          '{:linters {:unresolved-symbol {:level :error}}})))

(deftest hashbang-test
  (assert-submaps
   '({:file "<stdin>",
      :row 2,
      :col 1,
      :level :error,
      :message "clojure.core/inc is called with 0 args but expects 1"})
   (lint! "#!/usr/bin/env clojure\n(inc)")))

(deftest GH-301-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 15,
      :level :error,
      :message "duplicate key :&::before"})
   (lint! "{:&::before 1 :&::before 1}")))

(deftest misplaced-docstring-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 13,
      :level :warning,
      :message "misplaced docstring"})
   (lint! "(defn f [x] \"dude\" x)"))
  (is (empty? (lint! "(defn f [x] \"dude\")")))
  ;; for now this is empty, but in the next version we might warn about the
  ;; string "dude" being a discarded value
  (is (empty? (lint! "(defn f \"dude\" [x] \"dude\" x)"))))

(deftest defn-syntax-test
  (assert-submaps '({:file "<stdin>",
                     :row 1,
                     :col 1,
                     :level :error,
                     :message "invalid function body"})
                  (lint! "(defn f \"dude\" x) (f 1)")))

(deftest not-empty?-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :warning,
      :message "use the idiom (seq x) rather than (not (empty? x))"})
   (lint! "(not (empty? [1]))")))

(deftest deprecated-var-test
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 28,
      :level :warning,
      :message "#'user/foo is deprecated"})
   (lint! "(defn ^:deprecated foo []) (foo)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 35,
      :level :warning,
      :message "#'user/foo is deprecated since 1.0"})
   (lint! "(defn foo {:deprecated \"1.0\"} []) (foo)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :warning,
      :message "#'clojure.core/agent-errors is deprecated since 1.2"})
   (lint! "(agent-errors 1)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 32,
      :level :warning,
      :message "#'user/foo is deprecated"})
   (lint! "(def ^:deprecated foo (fn [])) (foo)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 32,
      :level :warning,
      :message "#'user/foo is deprecated"})
   (lint! "(def ^:deprecated foo (fn [])) foo"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :warning,
      :message "#'clojure.core/replicate is deprecated since 1.3"})
   (lint! "replicate"))
  (testing "config"
    (assert-submaps
     '({:file "corpus/deprecated_var.clj",
        :row 10,
        :col 1,
        :level :warning,
        :message "#'foo.foo/deprecated-fn is deprecated"})
     (lint! (io/file "corpus" "deprecated_var.clj")
            '{:linters
              {:deprecated-var
               {:exclude {foo.foo/deprecated-fn
                          [foo.bar foo.baz/allowed "foo.baz/ign\\.*" "bar\\.*"]}}}}))))

;;;; Scratch

(comment
  (schema-defn-test)
  (inline-def-test)
  (redundant-let-test)
  (redundant-do-test)
  (invalid-arity-test)
  (exit-code-test)
  (t/run-tests)
  )
