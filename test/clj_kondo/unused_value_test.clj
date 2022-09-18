(ns clj-kondo.unused-value-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :refer [deftest is testing]]))

(deftest unused-value-simple-expressions-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 5, :level :warning, :message "Unused value: 1"})
   (lint! "(do 1 2) " {:linters {:unused-value {:level :warning}}}))
  (assert-submaps '({:file "<stdin>", :row 2, :col 6, :level :warning, :message "Unused value: 1"}
                    {:file "<stdin>", :row 2, :col 8, :level :warning, :message "Unused value: 2"})
                  (lint! "
(try 1 2 3 (catch Exception _))"
                         {:linters {:unused-value {:level :warning}}}))
  (is (empty?
       (lint! "(fn [] (:foo {}))" {:linters {:unused-value {:level :warning}}})))
  (is (empty?
       (lint! "(map
 (fn [{:keys [:vars :proxied-namespaces]}]
   (assoc {:vars vars}
          :proxied-namespaces proxied-namespaces))
 [])" {:linters {:unused-value {:level :warning}}})))
  (is (empty? (lint! "
(let [f #(+ % %2)] (f 1 2))" {:linters {:unused-value {:level :warning}}})))
  (is (empty? (lint! "
(binding [*print-level* false
          *print-meta* false])" {:linters {:unused-value {:level :warning}}})))
  (is (empty? (lint! "
(when-not (:protocol-symbol var)
  (cljs.analyzer/warning :invalid-protocol-symbol {}))"
                     {:linters {:unused-value {:level :warning}
                                :unresolved-namespace {:level :off}}})))
  (is (empty? (lint! "
(do
  (let [_res (try nil (catch :default _))])
  nil)"
                     {:linters {:unused-value {:level :warning}}})))
  (is (empty? (lint! "
(do ((fn [_ _]) true false) true)
" {:linters {:unused-value {:level :warning}}})))
  (is (empty? (lint! "
(do (true 1 2) 1)
(do (\"foo\" 1 2) 1)
(do (\\a 1 2) 1)
(do (1 1 2) 1)
"
                     {:linters {:unused-value {:level :warning}
                                :not-a-function {:level :off}}})))
  (is (empty? (lint! "
(do (js/download 1 \"data-sources.csv\" \"text/csv\")
    12)
" {:linters {:unused-value {:level :warning}}} "--lang" "cljs")))
  (is (empty? (lint! "
(defn fun
  [{:keys [a-key]}]
  (when-let [{:keys [a-deep]} a-key] a-deep)
  (println a-key))
" {:linters {:unused-value {:level :warning}}})))
  (is (empty? (lint! "
(declare member)
(do (clojure.core/defn disjoint?-impl [t1 t2 default]
      {:post [(member % (quote (true false :dont-know)))]}
      (list t1 t2 default))
    (declare disjoint?))" {:linters {:unused-value {:level :warning}}}))))

(deftest unused-value-calls-test
  (testing "unused pure function call"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 14, :level :warning, :message "Unused value"})
     (lint! "(defn foo [] (update {} :dude 1) 1)" {:linters {:unused-value {:level :warning}}})))
  (testing "unused lazy seq"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 14, :level :warning, :message "Unused value"})
     (lint! "(defn foo [] (map inc [1 2 3]) 1)" {:linters {:unused-value {:level :warning}}})))
  (testing "unused transient call"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 14, :level :warning, :message "Unused value"})
     (lint! "(defn foo [] (assoc! (transient {}) :dude 1) 1)" {:linters {:unused-value {:level :warning}}}))))
