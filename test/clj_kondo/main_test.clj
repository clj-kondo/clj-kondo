(ns clj-kondo.main-test
  (:require
   [clj-kondo.main :refer [main]]
   [clj-kondo.test-utils :refer [lint! assert-submaps assert-submap]]
   [clojure.java.io :as io]
   [clojure.string :as str :refer [trim]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest inline-def-test
  (let [linted (lint! (io/file "corpus" "inline_def.clj"))
        row-col-files (set (map #(select-keys % [:row :col :file])
                                linted))]
    (is (= #{{:row 5, :col 3, :file "corpus/inline_def.clj"}
             {:row 8, :col 3, :file "corpus/inline_def.clj"}
             {:row 10, :col 10, :file "corpus/inline_def.clj"}
             {:row 12, :col 16, :file "corpus/inline_def.clj"}
             {:row 14, :col 18, :file "corpus/inline_def.clj"}}
           row-col-files))
    (is (= #{"inline def"} (set (map :message linted)))))
  (is (empty? (lint! "(defmacro foo [] `(def x 1))")))
  (is (empty? (lint! "(defn foo [] '(def x 3))")))
  (is (not-empty (lint! "(defmacro foo [] `(def x# (def x# 1)))"))))

(deftest redundant-let-test
  (let [linted (lint! (io/file "corpus" "redundant_let.clj"))
        row-col-files (set (map #(select-keys % [:row :col :file])
                                linted))]
    (is (= #{{:row 4, :col 3, :file "corpus/redundant_let.clj"}
             {:row 8, :col 3, :file "corpus/redundant_let.clj"}
             {:row 12, :col 3, :file "corpus/redundant_let.clj"}}
           row-col-files))
    (is (= #{"redundant let"} (set (map :message linted)))))
  (is (empty? (lint! "(let [x 2] `(let [y# 3]))")))
  (is (empty? (lint! "(let [x 2] '(let [y 3]))"))))

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
    (is (= '({:row 7, :col 1, :file "corpus/invalid_arity/calls.clj"}
             {:row 8, :col 1, :file "corpus/invalid_arity/calls.clj"}
             {:row 9, :col 1, :file "corpus/invalid_arity/calls.clj"}
             {:row 10, :col 1, :file "corpus/invalid_arity/calls.clj"}
             {:row 11, :col 1, :file "corpus/invalid_arity/calls.clj"}
             {:row 7, :col 1, :file "corpus/invalid_arity/defs.clj"}
             {:row 10, :col 1, :file "corpus/invalid_arity/defs.clj"}
             {:row 11, :col 1, :file "corpus/invalid_arity/defs.clj"}
             {:row 9, :col 1, :file "corpus/invalid_arity/order.clj"})
           row-col-files))
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
        linted (lint! invalid-core-function-call-example)]
    (is (pos? (count linted)))
    (is (every? #(str/includes? % "wrong number of args")
                linted)))
  (is (empty? (lint! "(defn foo [x]) (defn bar [foo] (foo))")))
  (is (empty? (lint! "(defn foo [x]) (let [foo (fn [])] (foo))")))
  (testing "macroexpansion of ->"
    (is (empty? (lint! "(defn inc [x] (+ x 1)) (-> x inc inc)")))
    (is (= 1 (count (lint! "(defn inc [x] (+ x 1)) (-> x inc (inc 1))")))))
  (testing "macroexpansion of fn literal"
    (is (= 1 (count (lint! "(defn inc [x] (+ x 1)) #(-> % inc (inc 1))")))))
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
        :level :error,
        :message "wrong number of args (0) passed to clojure.core/defn"})
     (lint! "(defn) (defmacro)"))))

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
                       :message "call to private function private"}
                      {:file "corpus/private/private_calls.clj",
                       :row 5,
                       :col 1,
                       :level :error,
                       :message "call to private function private-by-meta"})
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

(deftest nested-namespaced-maps-workaround-test
  (testing "when an error happens in one file, the other file is still linted"
    (let [linted (lint! (io/file "corpus" "nested_namespaced_maps_workaround.clj"))]
      (is (= '({:file "corpus/nested_namespaced_maps_workaround.clj",
                :row 8,
                :col 1,
                :level :error,
                :message
                "wrong number of args (2) passed to nested-namespaced-maps-workaround/test-fn"})
             linted)))))

(deftest exit-code-test
  (with-out-str
    (testing "the exit code is 0 when no errors are detected"
      (is (zero? (with-in-str "(defn foo []) (foo)" (main "--lint" "-")))))
    (testing "the exit code is 2 when warning are detected"
      (is (= 2 (with-in-str "(do (do 1))" (main "--lint" "-")))))
    (testing "the exit code is 1 when errors are detected"
      (is (= 3 (with-in-str "(defn foo []) (foo 1)" (main "--lint" "-")))))))

(deftest cond-without-else-test
  (doseq [lang [:clj #_#_:cljs :cljc]]
    (assert-submaps '({:row 7,
                       :col 1,
                       :level :warning,
                       :message "cond without :else"}
                      {:row 14,
                       :col 1,
                       :level :warning,
                       :message "cond without :else"})
                    (lint! (io/file "corpus" (str "cond_without_else." (name lang)))))))

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
                 (first (lint! "(cljs.core/vec)" "--lang" "cljs"))))

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
  (is (empty? (lint! "(when-let [select-keys (fn [])] (select-keys))"))))

(deftest if-let-test
  (assert-submap {:file "<stdin>",
                  :row 1,
                  :col 9,
                  :level :error,
                  :message "if-let takes only one binding"}
                 (first (lint! "(if-let [x 1 y 2])")))
  (is (empty? (lint! "(if-let [{:keys [:row :col]} {:row 1 :col 2}])"))))

(deftest when-let-test
  (assert-submap {:file "<stdin>",
                  :row 1,
                  :col 11,
                  :level :error,
                  :message "when-let takes only one binding"}
                 (first (lint! "(when-let [x 1 y 2])")))
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
       (lint! "(cond 1 2)" "--config" "{:linters {:cond-without-else {:level :off}}}")))
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
  (is (str/starts-with?
       (with-out-str
         (with-in-str "(do 1)"
           (main "--lint" "-" "--config" "{:output {:pattern \"{{LEVEL}}_{{filename}}\"}}")))
       "WARNING_<stdin>"))
  (is (empty? (lint! "(comment (select-keys))" "--config" "{:skip-comments true}")))
  (assert-submap
   '({:file "<stdin>",
      :row 1,
      :col 16,
      :level :error,
      :message "wrong number of args (2) passed to user/foo"})
   (lint! "(defn foo [x]) (foo (comment 1 2 3) 2)" "--config" "{:skip-comments true}")))

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

(deftest map-missing-key
  (is (= '({:file "<stdin>",
            :row 1,
            :col 7,
            :level :error,
            :message "missing value for key :b"})
         (lint! "{:a 1 :b}"))))

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
     (first (lint! "(-> 1 #?(:clj (Math/pow)))" "--lang" "cljc")))))

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
     :message "call to private function verify-signature"}
    {:file "corpus/schema/defs.clj",
     :row 10,
     :col 1,
     :level :error,
     :message "wrong number of args (2) passed to schema.defs/verify-signature"}]
   (lint! (io/file "corpus" "schema"))))

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
