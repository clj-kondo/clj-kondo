(ns clj-kondo.hooks-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps native?]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest testing is]]))

(defn prn-seq [coll]
  (doseq [i coll]
    (prn i)))

(deftest macroexpand-test
  (assert-submaps
   '({:file "corpus/macroexpand.clj", :row 16, :col 7, :level :error, :message "Expected: number, received: keyword."}
     {:file "corpus/macroexpand.clj", :row 18, :col 1, :level :error, :message "No sym and val provided [at line 4, column 7]"}
     {:file "corpus/macroexpand.clj", :row 18, :col 1, :level :error, :message "foo/weird-macro is called with 0 args but expects 1 or more"}
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
     results)))

(deftest preserve-arity-linting-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 1, :level :error, :message "clojure.core/inc is called with 3 args but expects 1"} {:file "<stdin>", :row 9, :col 1, :level :error, :message "foo/fixed-arity is called with 3 args but expects 2"})
   (lint! "
(ns foo)
(defmacro fixed-arity [x y] ::TODO)

(ns bar
  {:clj-kondo/config '{:hooks {foo/fixed-arity \"(fn [{:keys [:sexpr]}] {:sexpr `(inc ~@(rest sexpr))})\"}}}
  (:require [foo :refer [fixed-arity]]))

(fixed-arity 1 2 3)"
              {:linters {:unresolved-symbol {:level :error}
                         :invalid-arity {:level :error}}})))

(deftest error-in-macro-fn-test
  (when-not native?
    (let [err (java.io.StringWriter.)]
      (binding [*err* err] (lint! "
(ns bar
  {:clj-kondo/config '{:hooks {foo/fixed-arity \"(fn [{:keys [:sexpr]}] {:a :sexpr 1})\"}}}
  (:require [foo :refer [fixed-arity]]))

(fixed-arity 1 2 3)"
                                  {:linters {:unresolved-symbol {:level :error}
                                             :invalid-arity {:level :error}}}))
      (is (str/includes? (str err) "WARNING: error while trying to read hook for foo/fixed-arity: The map literal starting with :a contains 3 form(s).")))))

(deftest hook-test
  (assert-submaps
   '({:file "<stdin>", :row 12, :col 1, :level :error, :message "dispatch arg should be vector! [at line 4, column 7]"}
     {:file "<stdin>", :row 13, :col 1, :level :error, :message "keyword should be fully qualified! [at line 6, column 7]"})
   (lint! "
(ns bar
  {:clj-kondo/config '{:hooks {re-frame.core/dispatch \"
(fn [{:keys [:sexpr]}]
  (let [event (second sexpr)]
    (when-not (vector? event)
      (throw (Exception. \\\"dispatch arg should be vector!\\\")))
    (when-not (qualified-keyword? (first event))
      (throw (Exception. \\\"keyword should be fully qualified!\\\")))))\"}}}
  (:require [re-frame.core :refer [dispatch]]))

(dispatch 1)
(dispatch [:foo 1])"
             {:linters {:unresolved-symbol {:level :error}
                        :invalid-arity {:level :error}}})))
