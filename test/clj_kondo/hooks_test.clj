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
     {:file "corpus/macroexpand.clj", :row 17, :col 7, :level :error, :message "Expected: number, received: string."}
     {:file "corpus/macroexpand.clj", :row 20, :col 1, :level :error, :message #"No sym and val provided"}
     {:file "corpus/macroexpand.clj", :row 20, :col 1, :level :error, :message "foo/weird-macro is called with 0 args but expects 1 or more"}
     {:file "corpus/macroexpand.clj", :row 31, :col 48, :level :warning, :message "unused binding tree"}
     {:file "corpus/macroexpand.clj", :row 39, :col 1, :level :warning, :message "Missing catch or finally in try"}
     {:file "corpus/macroexpand.clj", :row 72, :col 20, :level :error, :message "Expected: string, received: number."}
     {:file "corpus/macroexpand.clj", :row 109, :col 1, :level :error, :message "quux/with-mixin is called with 4 args but expects 1"}
     {:file "corpus/macroexpand.clj", :row 109, :col 13, :level :error, :message "unresolved symbol a"}
     {:file "corpus/macroexpand.clj", :row 111, :col 1, :level :warning, :message "redefined var #'quux/with-mixin"})
   (let [results (lint! (io/file "corpus" "macroexpand.clj")
                        {:linters {:unresolved-symbol {:level :error}
                                   :unused-binding {:level :warning}
                                   :type-mismatch {:level :error}}}
                        "--config-dir" (.getPath (io/file "corpus" ".clj-kondo")))]
     ;;(prn-seq results)
     results)))

(deftest preserve-arity-linting-test
  (assert-submaps
   '({:file "<stdin>", :row 16, :col 1, :level :error, :message "foo/fixed-arity is called with 3 args but expects 2"}
     {:file "<stdin>", :row 16, :col 1, :level :error, :message "clojure.core/inc is called with 3 args but expects 1"})
   (lint! "
(ns foo)
(defmacro fixed-arity [x y] ::TODO)

(ns bar
  {:clj-kondo/config '{:hooks {foo/fixed-arity \"

(require '[clj-kondo.hooks-api :as api])
(fn [{:keys [:node]}]
  {:node (with-meta (api/list-node (list* (api/token-node 'inc) (rest (:children node))))
           (meta node))})

\"}}}
  (:require [foo :refer [fixed-arity]]))

(fixed-arity 1 2 3)"
          {:linters {:unresolved-symbol {:level :error}
                     :invalid-arity {:level :error}}})))

(deftest error-in-macro-fn-test
  (when-not native?
    (let [err (java.io.StringWriter.)]
      (binding [*err* err] (lint! "
(ns bar
  {:clj-kondo/config '{:hooks {foo/fixed-arity \"(fn [{:keys [:node]}] {:a :sexpr 1})\"}}}
  (:require [foo :refer [fixed-arity]]))

(fixed-arity 1 2 3)"
                                  {:linters {:unresolved-symbol {:level :error}
                                             :invalid-arity {:level :error}}}))
      (is (str/includes? (str err) "WARNING: error while trying to read hook for foo/fixed-arity: The map literal starting with :a contains 3 form(s).")))))

(deftest hook-test
  (assert-submaps
   '({:file "corpus/hook.clj", :row 17, :col 11, :level :error, :message #"dispatch arg should be vector!"}
     {:file "corpus/hook.clj", :row 18, :col 12, :level :error, :message #"keyword should be fully qualified!"})
   (lint! (io/file "corpus" "hook.clj")
          {:linters {:unresolved-symbol {:level :error}
                     :invalid-arity {:level :error}}})))

;; TODO: fix
#_(deftest location-test
  (testing "Sexprs that are numbers, strings or keywords cannot carry metadata. Hence their location is lost when converting a rewrite-clj node into a sexpr."
    (assert-submaps
     '({:file "corpus/hooks/location.clj", :row 7, :col 9, :level :error, :message "Expected: number, received: string."})
     (lint! (io/file "corpus" "hooks" "location.clj")
            {:linters {:type-mismatch {:level :error}}}))))
