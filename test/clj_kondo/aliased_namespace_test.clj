(ns clj-kondo.aliased-namespace-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps assert-submaps2]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]))

(deftest single-alias-test
  (assert-submaps [{:row 4,
                    :col 2,
                    :level :warning,
                    :message "An alias is defined for baz.qux: q"}
                   {:file "<stdin>", :row 5, :col 1, :level :warning,
                    :message "An alias is defined for baz.qux: q"}]
                  (lint! "(ns single-alias
  (:require [baz.qux :as q]))

(baz.qux/some-fn 1 2 3)
baz.qux/some-fn
" {:linters {:aliased-namespace-symbol {:level :warning}}})))

(deftest interop-alias-test
  (is (empty? (lint! (io/file "corpus" "aliased_namespaces" "interop.cljc")
                     {:linters {:aliased-namespace-symbol {:level :warning}
                                :unused-namespace {:level :off}}}))))

(deftest multiple-aliases-test
  (let [path (io/file "corpus" "aliased_namespaces" "multiple_aliases.clj")]
    (assert-submaps2 [{:file (str path),
                       :row 5,
                       :col 2,
                       :level :warning,
                       :message "Multiple aliases are defined for baz.qux: q, qq"}]
                     (lint! path
                            {:linters {:duplicate-require {:level :off}
                                       :aliased-namespace-symbol {:level :warning}}}))))

(deftest excluded-alias-test
  (let [path (io/file "corpus" "aliased_namespaces" "excluded_alias.clj")]
    (is (empty? (lint! path
                       {:linters {:aliased-namespace-symbol {:level :warning
                                                             :exclude ['clojure.string]}}})))))

(deftest dont-check-analyze-call-hook-test
  (let [path (io/file "corpus" "aliased_namespaces" "analyze_call_hook.clj")
        hook (slurp (io/file "corpus" "aliased_namespaces" "analyze_call_hook_str.clj"))]
    (is (empty? (lint! path
                       {:linters {:aliased-namespace-symbol {:level :warning}}
                        :hooks {:__dangerously-allow-string-hooks__ true
                                :analyze-call {'analyze-call-hook/new-> hook}}})))))

(deftest dont-check-macroexpand-hook-test
  (let [path (io/file "corpus" "aliased_namespaces" "macroexpansion.clj")
        hook (slurp (io/file "corpus" "aliased_namespaces" "macroexpansion_str.clj"))]
    (is (empty? (lint! path
                       {:linters {:aliased-namespace-symbol {:level :warning}}
                        :hooks {:__dangerously-allow-string-hooks__ true
                                :macroexpand {'macroexpansion/new-> hook}}})))))

(deftest expanded-by-clj-kondo-test
  (let [path (io/file "corpus" "aliased_namespaces" "expanded_by_clj_kondo.clj")]
    (is (empty? (lint! path
                       {:linters {:aliased-namespace-symbol {:level :warning}
                                  :unresolved-symbol {:level :error}}})))))
