(ns clj-kondo.redundant-expression-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :refer [deftest is testing]]))

(deftest shadowed-var-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 5, :level :warning, :message "Redundant expression: 1"})
   (lint! "(do 1 2) "))
  (assert-submaps '({:file "<stdin>", :row 2, :col 6, :level :warning, :message "Redundant expression: 1"}
                    {:file "<stdin>", :row 2, :col 8, :level :warning, :message "Redundant expression: 2"}) (lint! "
(try 1 2 3 (catch Exception _))"))
  (is (empty?
       (lint! "(fn [] (:foo {}))")))
  (is (empty?
       (lint! "(map
 (fn [{:keys [:vars :proxied-namespaces]}]
   (assoc {:vars vars}
          :proxied-namespaces proxied-namespaces))
 [])")))
  (is (empty? (lint! "
(let [f #(+ % %2)] (f 1 2))")))
  (is (empty? (lint! "
(binding [*print-level* false
          *print-meta* false])")))
  (is (empty? (lint! "
(when-not (:protocol-symbol var)
  (cljs.analyzer/warning :invalid-protocol-symbol {}))"
                     {:linters {:unresolved-namespace {:level :off}}})))
  (is (empty? (lint! "
(do
  (let [_res (try nil (catch :default _))])
  nil)")))
  (is (empty? (lint! "
(do ((fn [_ _]) true false) true)
")))
  (is (empty? (lint! "
(do (true 1 2) 1)
(do (\"foo\" 1 2) 1)
(do (\\a 1 2) 1)
(do (1 1 2) 1)
"
                     {:linters {:not-a-function {:level :off}}})))
  (is (empty? (lint! "
(do (js/download 1 \"data-sources.csv\" \"text/csv\")
    12)
" "--lang" "cljs")))
  (is (empty? (lint! "
(defn fun
  [{:keys [a-key]}]
  (when-let [{:keys [a-deep]} a-key] a-deep)
  (println a-key))
"))))
