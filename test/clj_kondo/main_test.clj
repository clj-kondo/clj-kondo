(ns clj-kondo.main-test
  (:require
   [clj-kondo.main :refer [main]]
   [clj-kondo.test-utils :refer [lint! assert-submaps assert-submap submap?]]
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
    (is (empty? (lint! "(defn foo [] '(def x 3))" "--lang" (name lang))))
    (is (not-empty (lint! "(defmacro foo [] `(def x# (def x# 1)))" "--lang" (name lang))))))

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
  (is (empty? (lint! "(let [x 2] `(let [y# 3]))")))
  (is (empty? (lint! "(let [x 2] '(let [y 3]))")))
  (is (empty? (lint! "(let [x 2] (let [y 1]) (let [y 2]))")))
  (is (empty? (lint! "(let [x 2] (when true (let [y 1])))")))
  (is (empty? (lint! "(let [z 1] (when true (let [x (let [y 2] y)])))")))
  (is (empty? (lint! "#(let [x 1])"))))

(deftest redundant-do-test
  (let [linted (lint! (io/file "corpus" "redundant_do.clj"))
        row-col-files (set (map #(select-keys % [:row :col :file])
                                linted))]
    (is (= #{{:row 7, :col 13, :file "corpus/redundant_do.clj"}
             {:row 4, :col 7, :file "corpus/redundant_do.clj"}
             {:row 3, :col 1, :file "corpus/redundant_do.clj"}
             {:row 6, :col 8, :file "corpus/redundant_do.clj"}
             {:row 5, :col 14, :file "corpus/redundant_do.clj"}}
           row-col-files))
    (is (= #{"redundant do"} (set (map :message linted)))))
  (is (empty? (lint! "(do 1 `(do 1 2 3))")))
  (is (empty? (lint! "(do 1 '(do 1 2 3))")))
  (is (not-empty (lint! "(fn [] (do :foo :bar))")))
  (is (empty? (lint! "#(do :foo :bar)"))))

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
    (is (every? #(str/includes? % "wrong number of args")
                (map :message linted))))
  (let [invalid-core-function-call-example "
(ns clojure.core)
(defn inc [x])
(ns cljs.core)
(defn inc [x])

(ns myns)
(inc 1 2 3)
"
        linted (lint! invalid-core-function-call-example "--config" "{:linters {:redefined-var {:level :off}}}")]
    (is (pos? (count linted)))
    (is (every? #(str/includes? % "wrong number of args")
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
        :message "wrong number of args (0) passed to clojure.core/defn"}
       {:file "<stdin>",
        :row 1,
        :col 8,
        :level :error
        :message "wrong number of args (0) passed to clojure.core/defmacro"})
     (lint! "(defn) (defmacro)")))
  (testing "redefining clojure var gives no error about incorrect arity of clojure var"
    (is (empty? (lint! "(defn inc [x y] (+ x y))
                        (inc 1 1)" "--config" "{:linters {:redefined-var {:level :off}}}")))))

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
  (let [linted (lint! (io/file "corpus" "spec"))]
    (is (= 1 (count linted)))
    (assert-submap {:file "corpus/spec/alpha.cljs",
                    :row 6,
                    :col 1,
                    :level :error,
                    :message "wrong number of args (2) passed to spec.alpha/def"}
                   (first linted))))

(deftest exclude-clojure-test
  (let [linted (lint! (io/file "corpus" "exclude_clojure.clj"))]
    (is (= '({:file "corpus/exclude_clojure.clj",
              :row 12,
              :col 1,
              :level :error,
              :message "wrong number of args (4) passed to clojure.core/get"})
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
                :row 0,
                :col 0,
                :level :error,
                :message
                "can't parse corpus/read_error/error.clj, Unexpected EOF. [at line 2, column 1]"}
               {:file "corpus/read_error/ok.clj",
                :row 6,
                :col 1,
                :level :error,
                :message "wrong number of args (1) passed to read-error.ok/foo"})
             linted)))))

(deftest nested-namespaced-maps-test
  (let [linted (lint! (io/file "corpus" "nested_namespaced_maps.clj"))]
    (is (= '({:file "corpus/nested_namespaced_maps.clj",
              :row 9,
              :col 1,
              :level :error,
              :message "wrong number of args (2) passed to nested-namespaced-maps/test-fn"}
             {:file "corpus/nested_namespaced_maps.clj",
              :row 11,
              :col 12,
              :level :error,
              :message "duplicate key :a"})
           linted))))

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
                   :message "wrong number of args (4) passed to cljs.core/for"}
                 (first (lint! "(for [x []] 1 2 3)" "--lang" "cljs"))))

(deftest built-in-test
  (is (= {:file "<stdin>",
          :row 1,
          :col 1,
          :level :error,
          :message "wrong number of args (1) passed to clojure.core/select-keys"}
         (first (lint! "(select-keys 1)" "--lang" "clj"))))
  (is (= {:file "<stdin>",
          :row 1,
          :col 1,
          :level :error,
          :message "wrong number of args (1) passed to cljs.core/select-keys"}
         (first (lint! "(select-keys 1)" "--lang" "cljs"))))
  (is (= {:file "<stdin>",
          :row 1,
          :col 1,
          :level :error,
          :message "wrong number of args (1) passed to clojure.core/select-keys"}
         (first (lint! "(select-keys 1)" "--lang" "cljc"))))
  (assert-submap {:file "<stdin>" :level :error,
                  :message "wrong number of args (3) passed to clojure.test/successful?"}
                 (first (lint! "(ns my-cljs (:require [clojure.test :refer [successful?]]))
    (successful? 1 2 3)" "--lang" "clj")))
  (assert-submap {:file "<stdin>" :level :error,
                  :message "wrong number of args (3) passed to cljs.test/successful?"}
                 (first (lint! "(ns my-cljs (:require [cljs.test :refer [successful?]]))
    (successful? 1 2 3)" "--lang" "cljs")))
  (assert-submap {:file "<stdin>", :row 2, :col 5, :level :error,
                  :message "wrong number of args (0) passed to clojure.set/difference"}
                 (first (lint! "(ns my-cljs (:require [clojure.set :refer [difference]]))
    (difference)" "--lang" "clj")))
  (assert-submap {:file "<stdin>", :row 2, :col 5, :level :error,
                  :message "wrong number of args (0) passed to clojure.set/difference"}
                 (first (lint! "(ns my-cljs (:require [clojure.set :refer [difference]]))
    (difference)" "--lang" "cljs"))))

(deftest built-in-java-test
  (is (= {:file "<stdin>", :row 1, :col 1,
          :level :error,
          :message "wrong number of args (3) passed to java.lang.Thread/sleep"}
         (first (lint! "(Thread/sleep 1 2 3)" "--lang" "clj"))))
  (is (= {:file "<stdin>", :row 1, :col 1,
          :level :error,
          :message "wrong number of args (3) passed to java.lang.Thread/sleep"}
         (first (lint! "(java.lang.Thread/sleep 1 2 3)" "--lang" "clj"))))
  (is (= {:file "<stdin>", :row 1, :col 1,
          :level :error,
          :message "wrong number of args (3) passed to java.lang.Math/pow"}
         (first (lint! "(Math/pow 1 2 3)" "--lang" "clj"))))
  (is (= {:file "<stdin>", :row 1, :col 1,
          :level :error,
          :message "wrong number of args (3) passed to java.math.BigInteger/valueOf"}
         (first (lint! "(BigInteger/valueOf 1 2 3)" "--lang" "clj"))))
  (is (empty?
       (first (lint! "(java.lang.Thread/sleep 1 2 3)" "--lang" "cljs"))))
  (is (= {:file "<stdin>", :row 1, :col 9,
          :level :error,
          :message "wrong number of args (3) passed to java.lang.Thread/sleep"}
         (first (lint! "#?(:clj (java.lang.Thread/sleep 1 2 3))" "--lang" "cljc")))))

(deftest resolve-core-ns-test
  (assert-submap '{:file "<stdin>",
                   :row 1,
                   :col 1,
                   :level :error,
                   :message "wrong number of args (0) passed to clojure.core/vec"}
                 (first (lint! "(clojure.core/vec)" "--lang" "clj")))
  (assert-submap '{:file "<stdin>",
                   :row 1,
                   :col 1,
                   :level :error,
                   :message "wrong number of args (0) passed to cljs.core/vec"}
                 (first (lint! "(cljs.core/vec)" "--lang" "cljs")))
  (assert-submap '{:file "<stdin>",
                   :row 1,
                   :col 1,
                   :level :error,
                   :message "wrong number of args (0) passed to cljs.core/vec"}
                 (first (lint! "(clojure.core/vec)" "--lang" "cljs"))))

(deftest override-test
  (is (empty? (lint! "(cljs.core/array 1 2 3)" "--lang" "cljs"))))

(deftest override-test
  (is (empty? (lint! "(cljs.core/array 1 2 3)" "--lang" "cljs"))))

(deftest cljs-clojure-ns-alias-test []
  (assert-submap '{:file "<stdin>",
                   :row 2,
                   :col 1,
                   :level :error,
                   :message "wrong number of args (3) passed to cljs.test/do-report"}
                 (first (lint! "(ns foo (:require [clojure.test :as t]))
(t/do-report 1 2 3)" "--lang" "cljs"))))

(deftest prefix-libspec-test []
  (assert-submaps
   '({:file "corpus/prefixed_libspec.clj",
      :row 14,
      :col 1,
      :level :error,
      :message "wrong number of args (0) passed to foo.bar.baz/b"}
     {:file "corpus/prefixed_libspec.clj",
      :row 15,
      :col 1,
      :level :error,
      :message "wrong number of args (0) passed to foo.baz/c"})
   (lint! (io/file "corpus" "prefixed_libspec.clj"))))

(deftest rename-test
  (testing "the renamed function isn't available under the referred name"
    (assert-submaps
     '({:file "<stdin>",
        :row 2,
        :col 11,
        :level :error,
        :message "wrong number of args (1) passed to clojure.string/includes?"})
     (lint! "(ns foo (:require [clojure.string :refer [includes?] :rename {includes? i}]))
          (i \"str\")
          (includes? \"str\")"))))

(deftest refer-all-rename-test
  (testing ":require with :refer :all and :rename"
    (assert-submaps '({:file "corpus/refer_all.clj",
                       :level :error,
                       :message "wrong number of args (0) passed to funs/foo"}
                      {:file "corpus/refer_all.clj",
                       :level :error,
                       :message "wrong number of args (0) passed to funs/bar"})
                    (lint! (io/file "corpus" "refer_all.clj")))
    (assert-submaps '({:file "corpus/refer_all.cljs",
                       :level :error,
                       :message "wrong number of args (0) passed to macros/foo"})
                    (lint! (io/file "corpus" "refer_all.cljs")))))

(deftest alias-test
  (assert-submap
   '{:file "<stdin>",
     :row 1,
     :col 35,
     :level :error,
     :message "wrong number of args (0) passed to clojure.core/select-keys"}
   (first (lint! "(ns foo) (alias 'c 'clojure.core) (c/select-keys)"))))

(deftest case-test
  (testing "case dispatch values should not be linted as function calls"
    (assert-submaps
     '({:file "corpus/case.clj",
        :row 7,
        :col 3,
        :level :error,
        :message "wrong number of args (3) passed to clojure.core/filter"}
       {:file "corpus/case.clj",
        :row 9,
        :col 3,
        :level :error,
        :message "wrong number of args (3) passed to clojure.core/filter"}
       {:file "corpus/case.clj",
        :row 14,
        :col 3,
        :level :error,
        :message "wrong number of args (3) passed to clojure.core/filter"}
       {:file "corpus/case.clj",
        :row 15,
        :col 3,
        :level :error,
        :message "wrong number of args (2) passed to clojure.core/odd?"})
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
      :message "wrong number of args (3) passed to foo"})
   (lint! "(fn foo [x] (foo 1 2 3))"))
  (is (empty? (lint! "(fn foo ([x] (foo 1 2)) ([x y]))")))
  (assert-submaps
   '({:message "wrong number of args (3) passed to f"})
   (lint! "(let [f (fn [])] (f 1 2 3))"))
  (assert-submaps
   '({:message "wrong number of args (3) passed to f"})
   (lint! "(let [f #()] (f 1 2 3))"))
  (assert-submaps
   '({:message "wrong number of args (0) passed to f"})
   (lint! "(let [f #(apply println % %&)] (f))"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 19,
      :level :error,
      :message "wrong number of args (1) passed to fn"})
   (lint! "(let [fn (fn [])] (fn 1))"))
  (is (empty? (lint! "(let [f #(apply println % %&)] (f 1))")))
  (is (empty? (lint! "(let [f #(apply println % %&)] (f 1 2 3 4 5 6))")))
  (is (empty? (lint! "(fn ^:static meta [x] (if (instance? clojure.lang.IMeta x)
                       (. ^clojure.lang.IMeta x (meta))))")))
  (is (empty? (lint! "(doseq [fn [inc]] (fn 1))"))))

(deftest let-test
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 6,
    :level :error,
    :message "let binding vector requires even number of forms"}
   (first (lint! "(let [x 1 y])")))
  (assert-submaps
   '({:message "wrong number of args (0) passed to clojure.core/select-keys"})
   (lint! "(let [x 1 y (select-keys)])"))
  (is (empty? (lint! "(let [select-keys (fn []) y (select-keys)])")))
  (assert-submaps
   '({:message "wrong number of args (1) passed to f"})
   (lint! "(let [f (fn []) y (f 1)])"))
  (assert-submaps
   '({:message "wrong number of args (1) passed to f"})
   (lint! "(let [f (fn [])] (f 1))"))
  (is (empty (lint! "(let [f (fn []) f (fn [_]) y (f 1)])")))
  (is (empty? (lint! "(let [err (fn [& msg])] (err 1 2 3))"))))

(deftest if-let-test
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 9,
    :level :error,
    :message "if-let binding vector requires exactly 2 forms"}
   (first (lint! "(if-let [x 1 y 2])")))
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 9,
    :level :error,
    :message "if-let binding vector requires exactly 2 forms"}
   (first (lint! "(if-let [x 1 y])")))
  (is (empty? (lint! "(if-let [{:keys [:row :col]} {:row 1 :col 2}])"))))

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
       (lint! "(select-keys 1 2 3)" "--config" "{:linters {:invalid-arity {:level :off}}}")))
  (is (empty?
       (lint! "(clojure.core/is-annotation? 1)" "--config" "{:linters {:private-call {:level :off}}}")))
  (is (empty?
       (lint! "(def (def x 1))" "--config" "{:linters {:inline-def {:level :off}}}")))
  (is (empty?
       (lint! "(do (do 1 2 3))" "--config" "{:linters {:redundant-do {:level :off}}}")))
  (is (empty?
       (lint! "(let [x 1] (let [y 2]))" "--config" "{:linters {:redundant-let {:level :off}}}")))
  (is (empty?
       (lint! "(cond 1 2)" "--config" "{:linters {:cond-else {:level :off}}}")))
  (is (str/starts-with?
       (with-out-str
         (lint! (io/file "corpus") "--config" "{:output {:show-progress true}}"))
       "...."))
  (is (not (some #(str/includes? % "datascript")
                 (map :file (lint! (io/file "corpus")
                                   "--config" "{:output {:exclude-files [\"datascript\"]}}")))))
  (is (not (some #(str/includes? % "datascript")
                 (map :file (lint! (io/file "corpus")
                                   "--config" "{:output {:include-files [\"inline_def\"]}}")))))
  (require '[clj-kondo.impl.config] :reload) ;; reset config
  (is (str/starts-with?
       (with-out-str
         (with-in-str "(do 1)"
           (main "--lint" "-" "--config" "{:output {:pattern \"{{LEVEL}}_{{filename}}\"}}")))
       "WARNING_<stdin>"))
  (is (empty? (lint! "(comment (select-keys))" "--config" "{:skip-args [clojure.core/comment]}")))
  (assert-submap
   '({:file "<stdin>",
      :row 1,
      :col 16,
      :level :error,
      :message "wrong number of args (2) passed to user/foo"})
   (lint! "(defn foo [x]) (foo (comment 1 2 3) 2)" "--config" "{:skip-comments true}"))
  (is (empty? (lint! "(ns foo (:require [foo.specs] [bar.specs])) (defn my-fn [x] x)"
                     "--config" "{:linters {:unused-namespace {:exclude [foo.specs bar.specs]}}}")))
  (is (empty? (lint! "(ns foo (:require [foo.specs] [bar.specs])) (defn my-fn [x] x)"
                     "--config" "{:linters {:unused-namespace {:exclude [\".*\\\\.specs$\"]}}}")))
  (is (empty? (lint! "(ns foo (:require [foo.specs] [bar.spex])) (defn my-fn [x] x)"
                     "--config" "{:linters {:unused-namespace {:exclude
                                   [\".*\\\\.specs$\"
                                    \".*\\\\.spex$\"]}}}"))))

(deftest map-duplicate-keys
  (is (= '({:file "<stdin>", :row 1, :col 7, :level :error, :message "duplicate key :a"}
           {:file "<stdin>",
            :row 1,
            :col 10,
            :level :error,
            :message "wrong number of args (1) passed to clojure.core/select-keys"}
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
         (lint! "(loop [x {:a {:a }}] x)"))))

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
    :col 13,
    :level :error,
    :message "wrong number of args (1) passed to clojure.core/select-keys"}
   (first (lint! "(-> (1 2 3) select-keys)")))
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 13,
    :level :error,
    :message "wrong number of args (1) passed to clojure.core/select-keys"}
   (first (lint! "(-> (1 2 3) (select-keys))")))
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 14,
    :level :error,
    :message "wrong number of args (1) passed to clojure.core/select-keys"}
   (first (lint! "(->> (1 2 3) select-keys)")))
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 14,
    :level :error,
    :message "wrong number of args (1) passed to clojure.core/select-keys"}
   (first (lint! "(->> (1 2 3) (select-keys))")))
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
      :message "wrong number of args (1) passed to java.lang.Math/pow"}
     (first (lint! "(-> 1 #?(:clj (Math/pow)))" "--lang" "cljc"))))
  (testing "with type hints"
    (assert-submap
     {:file "<stdin>",
      :row 1,
      :col 60,
      :level :error,
      :message "wrong number of args (1) passed to clojure.string/includes?"}
     (first (lint! "(ns foo (:require [clojure.string])) (-> \"foo\" ^String str clojure.string/includes?)")))
    (assert-submap
     {:file "<stdin>", :row 1, :col 12, :level :error, :message "duplicate key :a"}
     (first (lint! "(-> ^{:a 1 :a 2} [1 2 3])"))))
  (testing "macroexpansion of anon fn literal"
    (assert-submaps
     '({:message "wrong number of args (1) passed to clojure.core/select-keys"})
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
   [{:file "corpus/schema/calls.clj",
     :row 4,
     :col 1,
     :level :error,
     :message "wrong number of args (0) passed to schema.defs/verify-signature"}
    {:file "corpus/schema/calls.clj",
     :row 4,
     :col 1,
     :level :error,
     :message "call to private function schema.defs/verify-signature"}
    {:file "corpus/schema/defs.clj",
     :row 10,
     :col 1,
     :level :error,
     :message "wrong number of args (2) passed to schema.defs/verify-signature"}]
   (lint! (io/file "corpus" "schema"))))

(deftest in-ns-test
  (assert-submaps
   '({:file "corpus/in-ns/base_ns.clj",
      :row 5,
      :col 1,
      :level :error,
      :message "wrong number of args (3) passed to in-ns.base-ns/foo"}
     {:file "corpus/in-ns/in_ns.clj",
      :row 5,
      :col 1,
      :level :error,
      :message "wrong number of args (3) passed to in-ns.base-ns/foo"})
   (lint! (io/file "corpus" "in-ns")))
  (assert-submap
   {:file "<stdin>",
    :row 1,
    :col 55,
    :level :error,
    :message "wrong number of args (3) passed to foo/foo-2"}
   (first (lint! "(ns foo) (defn foo-1 [] (in-ns 'bar)) (defn foo-2 []) (foo-2 1 2 3)"))))

(deftest skip-args-test
  (is
   (empty?
    (lint! (io/file "corpus" "skip_args" "comment.cljs") "--config" "{:skip-args [cljs.core/comment]}")))
  (assert-submaps
   '({:file "corpus/skip_args/streams_test.clj",
      :row 4,
      :col 33,
      :level :error,
      :message "duplicate key :a"})
   (lint! (io/file "corpus" "skip_args" "streams_test.clj") "--config" "{:linters {:invalid-arity {:skip-args [riemann.test/test-stream]}}}"))
  (assert-submaps
   '({:file "corpus/skip_args/arity.clj",
      :row 6,
      :col 1,
      :level :error,
      :message "wrong number of args (4) passed to skip-args.arity/my-macro"})
   (lint! (io/file "corpus" "skip_args" "arity.clj") "--config" "{:skip-args [skip-args.arity/my-macro]}"))
  (assert-submaps
   '({:file "corpus/skip_args/arity.clj",
      :row 6,
      :col 1,
      :level :error,
      :message "wrong number of args (4) passed to skip-args.arity/my-macro"})
   (lint! (io/file "corpus" "skip_args" "arity.clj") "--config" "{:linters {:invalid-arity {:skip-args [skip-args.arity/my-macro]}}}")))

(deftest missing-test-assertion-test
  (is (empty? (lint! "(ns foo (:require [clojure.test :as t])) (t/deftest (t/is (odd? 1)))")))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 53,
      :level :warning,
      :message "missing test assertion"})
   (lint! "(ns foo (:require [clojure.test :as t])) (t/deftest (odd? 1))"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 75,
      :level :warning,
      :message "missing test assertion"})
   (lint! "(ns foo (:require [clojure.test :as t] [clojure.set :as set])) (t/deftest (set/subset? #{1 2} #{1 2 3}))")))

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
      :message "wrong number of args (3) passed to foo/foo"})
   (lint! "(ns foo) (defmacro my-defn [name args & body] `(defn ~name ~args ~@body)) (my-defn foo [x]) (foo 1 2 3)"
          "--config" "{:lint-as {foo/my-defn clojure.core/defn}}")))

(deftest letfn-test
  (assert-submaps '({:file "<stdin>",
                     :row 1,
                     :col 11,
                     :level :error,
                     :message "wrong number of args (0) passed to clojure.core/select-keys"})
                  (lint! "(letfn [] (select-keys))"))
  (assert-submaps '({:file "<stdin>",
                     :row 1,
                     :col 19,
                     :level :error,
                     :message "wrong number of args (0) passed to f1"})
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
      :message "wrong number of args (0) passed to f2"})
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
      :col 31,
      :level :warning,
      :message "namespace baz is required but never used"})
   (lint! "(ns foo (:require [bar :as b] baz)) #::{:a #::bar{:a 1}}"))
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
  ;; TODO: this is probably not correct, since you need to write :b with double colons:
  (is (empty? (lint! "(ns foo (:require [bar :as b])) #:b{:a 1}")))
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
                       (loop [{:keys [:id] :or {id (str/lower-case \"HI\")}} {:id \"hello\"}])"))))



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
      :message "redefining user/foo"})
   (lint! "(defn foo []) (defn foo [])"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :warning,
      :message "redefining clojure.core/inc"})
   (lint! "(defn inc [])"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 1,
      :level :warning,
      :message "redefining cljs.core/inc"})
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
                     :message "redefining bar/x"})
                  (lint! "(ns foo (:require [bar :refer [x]])) (defn x [])"))
  (is (empty? (lint! "(defn foo [])")))
  (is (empty? (lint! "(ns foo (:refer-clojure :exclude [inc])) (defn inc [])"))))

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
                          :message "wrong number of args (0) passed to clojure.core/inc"}
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
      :message "wrong number of args (3) passed to keyword :x"})
   (lint! "(:x 1 2 3)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 10,
      :level :error,
      :message "wrong number of args (3) passed to keyword :b/x"})
   (lint! "(ns foo) (:b/x {:bar/x 1} 1 2)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 33,
      :level :error,
      :message "wrong number of args (3) passed to keyword :bar/x"})
   (lint! "(ns foo (:require [bar :as b])) (::b/x {:bar/x 1} 1 2)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 10
      :level :error,
      :message "wrong number of args (3) passed to keyword ::b/x"})
   (lint! "(ns foo) (::b/x {:bar/x 1} 1 2)"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 10,
      :level :error,
      :message "wrong number of args (3) passed to keyword :foo/x"})
   (lint! "(ns foo) (::x {::x 1} 2 3)")))

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
