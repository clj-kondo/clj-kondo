(ns clj-kondo.hooks-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [lint! assert-submaps assert-submaps2 native?]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest testing is]]))

(set! *warn-on-reflection* true)

(deftest macroexpand-test
  (assert-submaps
   '({:file "corpus/macroexpand.clj", :row 16, :col 7, :level :error, :message "Expected: number, received: keyword."}
     {:file "corpus/macroexpand.clj", :row 17, :col 7, :level :error, :message "Expected: number, received: string."}
     {:file "corpus/macroexpand.clj", :row 20, :col 1, :level :error, :message #"No sym and val provided"}
     {:file "corpus/macroexpand.clj", :row 20, :col 1, :level :error, :message "foo/weird-macro is called with 0 args but expects 1 or more"}
     {:file "corpus/macroexpand.clj", :row 31, :col 48, :level :warning, :message "unused binding tree"}
     {:file "corpus/macroexpand.clj", :row 39, :col 1, :level :warning, :message "Missing catch or finally in try"}
     {:file "corpus/macroexpand.clj", :row 49, :col 20, :level :error, :message "Expected: string, received: number."}
     {:file "corpus/macroexpand.clj", :row 64, :col 1, :level :error, :message "quux/with-mixin is called with 4 args but expects 1"}
     {:file "corpus/macroexpand.clj", :row 64, :col 13, :level :error, :message "Unresolved symbol: a"}
     {:file "corpus/macroexpand.clj", :row 66, :col 1, :level :warning, :message "redefined var #'quux/with-mixin"})
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
  {:clj-kondo/config '{:hooks {:analyze-call {foo/fixed-arity \"

(require '[clj-kondo.hooks-api :as api])
(fn [{:keys [:node]}]
  {:node (with-meta (api/list-node (list* (api/token-node 'inc) (rest (:children node))))
           (meta node))})

\"}}}}
  (:require [foo :refer [fixed-arity]]))

(fixed-arity 1 2 3)"
          {:hooks {:__dangerously-allow-string-hooks__ true}
           :linters {:unresolved-symbol {:level :error}
                     :invalid-arity {:level :error}}})))

(deftest error-in-macro-fn-test
  (when-not native?
    (let [err (java.io.StringWriter.)]
      (binding [*err* err] (lint! "
(ns bar
  {:clj-kondo/config '{:hooks {:analyze-call {foo/fixed-arity \"(fn [{:keys [:node]}] {:a :sexpr 1})\"}}}}
  (:require [foo :refer [fixed-arity]]))

(fixed-arity 1 2 3)"
                                  {:hooks {:__dangerously-allow-string-hooks__ true}
                                   :linters {:unresolved-symbol {:level :error}
                                             :invalid-arity {:level :error}}}))
      (is (str/includes? (str err) "WARNING: error while trying to read hook for foo/fixed-arity: The map literal starting with :a contains 3 form(s).")))))

(deftest re-frame-test
  (assert-submaps
   '({:file "corpus/hooks/re_frame.clj", :row 6, :col 12, :level :warning, :message #"keyword should be fully qualified!"})
   (lint! (io/file "corpus" "hooks" "re_frame.clj")
          {:linters {:unresolved-symbol {:level :error}
                     :invalid-arity {:level :error}}}
          "--config-dir" (.getPath (io/file "corpus" ".clj-kondo")))))

(deftest location-test
  (testing "Sexprs that are numbers, strings or keywords cannot carry
  metadata. Hence their location is lost when converting a rewrite-clj node into
  a sexpr. This is why we started using rewrite-clj directly."
    (assert-submaps
     '({:file "corpus/hooks/location.clj", :row 12, :col 10, :level :error, :message "Expected: number, received: string."}
       {:file "corpus/hooks/location.clj", :row 13, :col 1, :level :error, :message "clojure.core/inc is called with 3 args but expects 1"})
     (lint! (io/file "corpus" "hooks" "location.clj")
            {:hooks {:__dangerously-allow-string-hooks__ true}
             :linters {:type-mismatch {:level :error}}}
            "--config-dir" (.getPath (io/file "corpus" ".clj-kondo"))))))

(deftest expectations-test
  (assert-submaps
   '({:file "corpus/hooks/expectations.clj", :row 24, :col 45, :level :warning, :message "unused binding b"}
     {:file "corpus/hooks/expectations.clj", :row 26, :col 41, :level :error, :message "Unresolved symbol: b'"})
   (lint! (io/file "corpus" "hooks" "expectations.clj")
          {:hooks {:__dangerously-allow-string-hooks__ true}
           :linters {:unused-binding {:level :warning}
                     :unresolved-symbol {:level :error}
                     :invalid-arity {:level :error}}}
          "--config-dir" (.getPath (io/file "corpus" ".clj-kondo")))))

(deftest keys-test
  (when-not native?
    (let [s (with-out-str (lint! "
(ns bar
  {:clj-kondo/config
    '{:hooks {:analyze-call {foo/hook \"(fn [{:keys [:cljc :lang :filename :config]}]
 (prn cljc lang filename (some? (:linters config))))\"}}}}
  (:require [foo :refer [hook]]))

(hook 1 2 3)"
                                 {:hooks {:__dangerously-allow-string-hooks__ true}}))
          s (str/replace s "\r\n" "\n")]
      (is (= s (str/join " "
                         ["false"
                          ":clj"
                          "\"<stdin>\""
                          "true\n"]))))
    (let [s (with-out-str (lint! "
(ns bar
  {:clj-kondo/config
    '{:hooks {:analyze-call {foo/hook \"(fn [{:keys [:cljc :lang :filename :config]}]
 (prn cljc lang filename (some? (:linters config))))\"}}}}
  (:require [foo :refer [hook]]))

(hook 1 2 3)"
                                 {:hooks {:__dangerously-allow-string-hooks__ true}}
                                 "--lang" "cljc"))
          ;; Windows...
          s (str/replace s "\r\n" "\n")]
      (is (= s (str (str/join " "
                              ["true"
                               ":clj"
                               "\"<stdin>\""
                               "true\n"])
                    (str/join " "
                              ["true"
                               ":cljs"
                               "\"<stdin>\""
                               "true\n"])))))))

(deftest tags-test
  (when-not native?
    (let [s (with-out-str (lint! "
(ns bar
 {:clj-kondo/config
  '{:hooks
    {:analyze-call
     {foo/hook \"
      (require '[clj-kondo.hooks-api :as api])
      (fn [{:keys [node]}]
        (println (map api/tag (:children node))))\"}}}}
 (:require [foo :refer [hook]]))

(hook [] (inc 1) 1 \"\n\")"
                                 {:hooks {:__dangerously-allow-string-hooks__ true}}))]
      (is (= (read-string s)
             [:token :vector :list :token :multi-line])))))

(deftest config-test
  (when-not native?
    (let [s (with-out-str (lint! "
(ns bar
  {:clj-kondo/config
    '{:hooks {:analyze-call {foo/hook \"(fn [{:keys [:config]}]
                                         (prn (-> config :linters :custom-linter-configuration)))\"}}}}
  (:require [foo :refer [hook]]))

(hook 1 2 3)"
                                 {:hooks {:__dangerously-allow-string-hooks__ true}
                                  :linters ^:replace {:custom-linter-configuration {:a 1 :b 2}}}))
          s (str/replace s "\r\n" "\n")]
      (is (= s "{:a 1, :b 2}\n")))
    (let [s (with-out-str (lint! "
(ns bar
  {:clj-kondo/config
    '{:hooks {:analyze-call {foo/hook \"(fn [{:keys [:config]}]
                                         (prn (-> config :linters :custom-linter-configuration)))\"}}}}
  (:require [foo :refer [hook]]))

(hook 1 2 3)"
                                 {:hooks {:__dangerously-allow-string-hooks__ true}
                                  :linters ^:replace {:custom-linter-configuration {:a 1 :b 2}}}
                                 "--lang" "cljc"))
          ;; Windows...
          s (str/replace s "\r\n" "\n")]
      (is (= s "{:a 1, :b 2}\n{:a 1, :b 2}\n")))))

(deftest custom-lint-warning-ignore-test
  (when-not native?
    (let [res (lint! "
(ns bar
  {:clj-kondo/config
    '{:hooks {:analyze-call {foo/hook \"(fn [{:keys [node]}]
                                         (clj-kondo.hooks-api/reg-finding!
                                          (assoc (meta node) :message \\\"Yolo\\\"
                                                                                 :type :foo)))\"}}}}
  (:require [foo :refer [hook]]))

(hook 1 2 3)
#_:clj-kondo/ignore (hook 1 2 3)"
                     {:hooks {:__dangerously-allow-string-hooks__ true}
                      :linters {:foo {:level :error}}})]
      (assert-submaps '({:file "<stdin>", :row 10, :col 1, :level :error, :message "Yolo"}) res))))

(deftest redundant-do-let-test
  (testing "hook code generating do or let won't be reported as redundant"
    (when-not native?
      (let [res (lint! "
(ns bar
  {:clj-kondo/config
    '{:hooks {:analyze-call {
foo/hook-do \"

(require '[clj-kondo.hooks-api :as api])
(fn [{:keys [:node]}]
  (let [children (next (:children node))
        new-node (api/list-node (list*
(api/token-node 'do) children))]
    {:node new-node}))
\"

foo/hook-let \"

(require '[clj-kondo.hooks-api :as api])
(fn [{:keys [:node]}]
  (let [children (next (:children node))
        new-node (api/list-node (list*
(api/token-node 'let)
(api/vector-node [])
children))]
    {:node new-node}))
\"

}}}}
  (:require [foo :refer [hook-do hook-let]]))

(hook-do (do (prn :foo) (prn :bar)))
(hook-let (let [x 1] x))"
                       {:hooks {:__dangerously-allow-string-hooks__ true}})]
        (is (empty? res))))))

(deftest macroexpand2-test
  (assert-submaps
   '({:file "corpus/macroexpand2.cljs", :row 20, :col 10, :level :error, :message "Unresolved symbol: foobar"}
     {:file "corpus/macroexpand2.cljs", :row 32, :col 3, :level :error, :message "Expected: number, received: string."}
     {:file "corpus/macroexpand2.cljs", :row 32, :col 3, :level :error, :message "Expected: number, received: keyword."}
     {:file "corpus/macroexpand2.cljs", :row 38, :col 3, :level :error, :message "Expected: number, received: string."}
     {:file "corpus/macroexpand2.cljs", :row 38, :col 3, :level :error, :message "Expected: number, received: nil."}
     {:file "corpus/macroexpand2.cljs", :row 43, :col 1, :level :warning, :message "Unused private var macroexpand2/private-var"})
   (let [results (lint! (io/file "corpus" "macroexpand2.cljs")
                        {:linters {:unresolved-symbol {:level :error}
                                   :unused-binding {:level :warning}
                                   :type-mismatch {:level :error}}}
                        "--config-dir" (.getPath (io/file "corpus" ".clj-kondo")))]
     ;; (prn results)
     results)))

(deftest hook-context-test
  (testing "hook can set node context"
    (when-not native?
      (let [prog "(ns bar
                    (:require [foo :refer [hook-fn]]))
                  (hook-fn :a)"
            {:keys [analysis]}
            (with-in-str prog
              (clj-kondo/run!
               {:lint ["-"]
                :config {:hooks {:__dangerously-allow-string-hooks__ true
                                 :analyze-call {'foo/hook-fn "
                                               (require '[clj-kondo.hooks-api :as api])
                                               (fn [{:keys [:node :context]}]
                                                 (let [child (second (:children node))
                                                       new-node (assoc child :context {:my-hook {:can-set-context true}})]
                                                   {:node new-node}))"}}
                         :analysis {:keywords true :context true}}}))
            {:keys [keywords]} analysis
            a-keyword (some #(when (= "a" (:name %))
                               %) keywords)
            context (:context a-keyword)]
        (is a-keyword)
        (is (= {:my-hook {:can-set-context true}} context)))))
  (testing "hook can set ambient context"
    (when-not native?
      (let [prog "(ns bar
                    (:require [foo :refer [hook-fn]]))
                  (hook-fn :a)"
            {:keys [analysis]}
            (with-in-str prog
              (clj-kondo/run!
               {:lint ["-"]
                :config {:hooks {:__dangerously-allow-string-hooks__ true
                                 :analyze-call {'foo/hook-fn "
                                               (require '[clj-kondo.hooks-api :as api])
                                               (fn [{:keys [:node :context]}]
                                                   (let [child (second (:children node))
                                                         new-node (assoc child :context {:my-hook {:can-set-context true}})]
                                                     {:node new-node
                                                      :context {:yolo true}}))"}}
                         :analysis {:keywords true :context true}}}))
            {:keys [keywords]} analysis
            a-keyword (some #(when (= "a" (:name %))
                               %) keywords)
            context (:context a-keyword)]
        (is a-keyword)
        (is (= {:my-hook {:can-set-context true}, :yolo true} context))))))

(deftest pprint-test
  ;; this doesn't test the output
  ;; however, the "no empty docstring" linter would catch the error
  ;; if there was no output
  (testing "hook code supports pprint"
    (let [res (lint! "

(ns bar
  {:clj-kondo/config
    '{:hooks {:analyze-call {
foo/defdoced \"

(require '[clj-kondo.hooks-api :as api])
(require '[clojure.pprint :as pprint])

(fn [{:keys [:node]}]
  (let [[_defdoced name value] (:children node)
        new-node (api/list-node
        (list
          (api/token-node 'def)
          name
          (api/string-node
            (with-out-str (pprint/pprint value)))
          value))]
    {:node new-node}))
\"

}}}}
  (:require [foo :refer [defdoced]]))

(defdoced mysuperthing {:a 1 :b 2 :c {:d 3 :e 4}})
"
                     {:hooks {:__dangerously-allow-string-hooks__ true}})]
      (is (empty? res)))))

(deftest foo-test
  (testing "hook code supports pprint"
    (let [res (lint! "

(ns my-ns
  {:clj-kondo/config
    '{:hooks {:analyze-call {
my-ns/special-map \"

(require '[clj-kondo.hooks-api :as hooks])

(fn
  [{{[_ & args] :children} :node}]
  (let [[_map-type m] (if (= (count args) 1)
                       [(first args) (hooks/map-node [])]
                       args)]
    {:node m}))
\"

}}}})

(defmacro special-map [_map-type m]
  m)

(defn x []
  (let [num-cans 2]
    (assoc (special-map :world-map {:description (format \"%d cans\" num-cans)}) :wow :ok)))
"
                     {:hooks {:__dangerously-allow-string-hooks__ true}
                      :linters {:type-mismatch {:level :error}}})]
      (is (empty? res)))))

(deftest new-test
  (assert-submaps2
   [{:file "corpus/hooks/new.clj",
     :row 3,
     :col 1,
     :level :warning,
     :message "Interop is no good! false"}
    {:file "corpus/hooks/new.clj",
     :row 4,
     :col 1,
     :level :warning,
     :message "Interop is no good! true"}]
   (lint! (io/file "corpus" "hooks" "new.clj")
          '{:linters {:interop {:level :warning}}
            :hooks {:analyze-call {clojure.core/new hooks.new/new}}}
          "--config-dir" (.getPath (io/file "corpus" ".clj-kondo")))))

(deftest issue-1987-coerce-sexpr-roundtrip-test
  (is (empty?
       (lint! (io/file "corpus" "hooks" "coerce_sexpr_roundtrip.clj")
              '{:linters {:interop {:level :warning}}
                :hooks {:macroexpand {clojure.core/new hooks.new/new-macroexpand
                                      myns/defmodel hooks.roundtrip/defmodel}}}
              "--config-dir" (.getPath (io/file "corpus" ".clj-kondo"))))))

(deftest issue-1980-ignore-with-hook-test
  (is (empty?
       (lint! (io/file "corpus" "issue-1980")
              "--config-dir" (.getPath (io/file "corpus" "issue-1980" ".clj-kondo"))))))

(deftest resolve-in-hook-test
  (assert-submaps2
   '({:file "corpus/issue-1996/src/my_test.clj", :row 8, :col 2, :level :warning, :message "Don't use with-redefs"})
   (lint! (io/file "corpus" "issue-1996" "src")
          "--config-dir" (.getPath (io/file "corpus" "issue-1996" ".clj-kondo")))))

(deftest issue-2067-ns-groups-hooks-test
  (assert-submaps2
   '({:file "corpus/issue-2067/src/my_test2.clj", :row 11, :col 2, :level :warning, :message "Don't use with-redefs"}
     {:file "corpus/issue-2067/src/my_test2.clj", :row 14, :col 16, :level :warning, :message "[]"})
   (lint! (io/file "corpus" "issue-2067" "src")
          "--config-dir" (.getPath (io/file "corpus" "issue-2067" ".clj-kondo")))))

(deftest issue-2215-hook-passthrough-test
  (assert-submaps2
   []
   (prn (lint! (io/file "corpus" "issue-2067" "src")
               "--config-dir" (.getPath (io/file "corpus" "issue-2215" ".clj-kondo"))))))
