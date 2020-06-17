(ns clj-kondo.macroexpand-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest testing is]]))

(defn prn-seq [coll]
  (doseq [i coll]
    (prn i)))

(deftest macroexpand-test
  (assert-submaps
   '({:file "corpus/macroexpand.clj", :row 16, :col 7, :level :error, :message "Expected: number, received: keyword."}
     {:file "corpus/macroexpand.clj", :row 18, :col 1, :level :error, :message "No sym and val provided [at line 4, column 7]"}
     {:file "corpus/macroexpand.clj", :row 29, :col 48, :level :warning, :message "unused binding tree"}
     {:file "corpus/macroexpand.clj", :row 37, :col 1, :level :warning, :message "Missing catch or finally in try"}
     {:file "corpus/macroexpand.clj", :row 69, :col 20, :level :error, :message "Expected: string, received: number."}
     {:file "corpus/macroexpand.clj", :row 105, :col 1, :level :error, :message "quux/with-mixin is called with 4 args but expects 1"}
     {:file "corpus/macroexpand.clj", :row 105, :col 13, :level :error, :message "unresolved symbol a"}
     {:file "corpus/macroexpand.clj", :row 107, :col 1, :level :warning, :message "redefined var #'quux/with-mixin"})
   (let [results (lint! (io/file "corpus" "macroexpand.clj")
                        {:linters {:unresolved-symbol {:level :error}
                                   :unused-binding {:level :warning}
                                   :type-mismatch {:level :error}}})]
     #_(prn-seq results)
     results)))

(deftest preserve-arity-linting-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 1, :level :error, :message "clojure.core/inc is called with 3 args but expects 1"} {:file "<stdin>", :row 9, :col 1, :level :error, :message "foo/fixed-arity is called with 3 args but expects 2"})
   (lint! "
(ns foo)
(defmacro fixed-arity [x y] ::TODO)

(ns bar
  {:clj-kondo/config {:macroexpand {foo/fixed-arity \"(fn [{:keys [:sexpr]}] {:sexpr `(inc ~@(rest sexpr))})\"}}}
  (:require [foo :refer [fixed-arity]]))

(fixed-arity 1 2 3)"
              {:linters {:unresolved-symbol {:level :error}
                         :invalid-arity {:level :error}}})))
