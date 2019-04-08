(ns clj-kondo.main-test
  (:require
   [clj-kondo.main :refer [main]]
   [clj-kondo.test-utils :refer [lint!]]
   [clojure.java.io :as io]
   [clojure.string :as str :refer [trim]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest inline-def-test
  (let [linted (lint! (io/file "corpus" "inline_def.clj"))
        row-col-files (set (map #(select-keys % [:row :col :file])
                                linted))]
    (is (= #{{:row 9, :col 10, :file "corpus/inline_def.clj"}
             {:row 11, :col 14, :file "corpus/inline_def.clj"}
             {:row 7, :col 3, :file "corpus/inline_def.clj"}
             {:row 13, :col 18, :file "corpus/inline_def.clj"}
             {:row 4, :col 3, :file "corpus/inline_def.clj"}}
           row-col-files))
    (is (= #{"inline def"} (set (map :message linted)))))
  (is (empty? (lint! "(defmacro foo [] `(def x 1))")))
  (is (empty? (lint! "(defn foo [] '(def x 3))")))
  (is (not-empty (lint! "(defmacro foo [] `(def x# (def x# 1)))"))))

(deftest obsolete-let-test
  (let [linted (lint! (io/file "corpus" "obsolete_let.clj"))
        row-col-files (set (map #(select-keys % [:row :col :file])
                                linted))]
    (is (= #{{:row 4, :col 3, :file "corpus/obsolete_let.clj"}
             {:row 8, :col 3, :file "corpus/obsolete_let.clj"}
             {:row 12, :col 3, :file "corpus/obsolete_let.clj"}}
           row-col-files))
    (is (= #{"obsolete let"} (set (map :message linted)))))
  (is (empty? (lint! "(let [x 2] `(let [y# 3]))")))
  (is (empty? (lint! "(let [x 2] '(let [y 3]))"))))

(deftest obsolete-do-test
  (let [linted (lint! (io/file "corpus" "obsolete_do.clj"))
        row-col-files (set (map #(select-keys % [:row :col :file])
                                linted))]
    (is (= #{{:row 7, :col 13, :file "corpus/obsolete_do.clj"}
             {:row 4, :col 7, :file "corpus/obsolete_do.clj"}
             {:row 3, :col 1, :file "corpus/obsolete_do.clj"}
             {:row 6, :col 8, :file "corpus/obsolete_do.clj"}
             {:row 5, :col 14, :file "corpus/obsolete_do.clj"}}
           row-col-files))
    (is (= #{"obsolete do"} (set (map :message linted)))))
  (is (empty? (lint! "(do 1 `(do 1 2 3))")))
  (is (empty? (lint! "(do 1 '(do 1 2 3))"))))

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
    (is (every? #(str/includes? % "Wrong number of args")
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
    (is (every? #(str/includes? % "Wrong number of args")
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
             row-col-files)))))

(deftest cljc-test
  (let [linted (lint! (io/file "corpus" "cljc"))
        row-col-files (sort-by (juxt :file :row :col)
                               (map #(select-keys % [:file :row :col])
                                    linted))]
    row-col-files
    (is (= '({:file "corpus/cljc/test_cljc.cljc", :row 13, :col 9}
             {:file "corpus/cljc/test_cljc.cljc", :row 14, :col 10}
             {:file "corpus/cljc/test_cljc.cljc", :row 21, :col 1}

             {:file "corpus/cljc/test_cljc.cljs", :row 5, :col 1}

             {:file "corpus/cljc/test_cljc_from_clj.clj", :row 5, :col 1}

             {:file "corpus/cljc/test_cljs.cljs", :row 5, :col 1}
             {:file "corpus/cljc/test_cljs.cljs", :row 6, :col 1})
           row-col-files))))

(deftest exclude-clojure-test
  (let [linted (lint! (io/file "corpus" "exclude_clojure.clj"))]
    (is (= '({:file "corpus/exclude_clojure.clj",
              :row 12,
              :col 1,
              :level :error,
              :message "Wrong number of args (4) passed to clojure.core/get"})
          linted))))

(deftest private-call-test
  (let [linted (lint! (io/file "corpus" "private"))]
    (is (= 1 (count linted)))
    (is (= 4 (:row (first linted))))))

(deftest read-error-test
  (testing "when an error happens in one file, the other file is still linted"
    (let [linted (lint! (io/file "corpus" "read_error"))]
      (is (= '({:file "corpus/read_error/error.clj",
                :row 0,
                :col 0,
                :level :error,
                :message
                "Can't parse corpus/read_error/error.clj, Unexpected EOF. [at line 2, column 1]"}
               {:file "corpus/read_error/ok.clj",
                :row 6,
                :col 1,
                :level :error,
                :message "Wrong number of args (1) passed to read-error.ok/foo"})
             linted)))))

(deftest nested-namespaced-maps-workaround-test
  (testing "when an error happens in one file, the other file is still linted"
    (let [linted (lint! (io/file "corpus" "nested_namespaced_maps_workaround.clj"))]
      (is (= '({:file "corpus/nested_namespaced_maps_workaround.clj",
                :row 8,
                :col 1,
                :level :error,
                :message
                "Wrong number of args (2) passed to nested-namespaced-maps-workaround/test-fn"})
             linted)))))

(deftest exit-code-test
  (with-out-str
    (testing "the exit code is 0 when no errors are detected"
      (is (zero? (with-in-str "(defn foo []) (foo)" (main "--lint" "-")))))
    (testing "the exit code is 2 when warning are detected"
      (is (= 2 (with-in-str "(do (do 1))" (main "--lint" "-")))))
    (testing "the exit code is 1 when errors are detected"
      (is (= 3 (with-in-str "(defn foo []) (foo 1)" (main "--lint" "-")))))))

;;;; Scratch

(comment
  (inline-def-test)
  (obsolete-let-test)
  (obsolete-do-test)
  (invalid-arity-test)
  (exit-code-test)
  (t/run-tests)
  )
