(ns clj-kondo.clj-kondo.diff-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [assert-submaps2]]
   [clojure.edn :as edn]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest clj-kondo-diff-test
  (let [cp (-> (p/shell {:out :string} "clojure -Spath -A:cljs") :out str/trim)
        config-dir (fs/file "test-regression" "clj_kondo" "clj_kondo" ".clj-kondo")
        _ (fs/delete-tree (fs/file config-dir ".cache"))
        lint-result (clj-kondo/run! {:config-dir config-dir
                                     :cache false
                                     :parallel true
                                     :lint [cp]
                                     :repro true
                                     :config {:linters
                                              {:redundant-fn-wrapper {:level :warning}
                                               :condition-always-true {:level :warning}
                                               :not-a-function
                                               '{:skip-args [clojure.pprint/defdirectives
                                                             cljs.pprint/defdirectives
                                                             clojure.data.json/codepoint-case]}
                                               :def-fn {:level :warning}
                                               :redundant-str-call {:level :warning}}
                                              :output {:langs false}}})
        findings (:findings lint-result)
        expected-findings-file (fs/file "test-regression" "clj_kondo" "clj_kondo" "findings.edn")
        _ (when false (spit expected-findings-file (with-out-str (pp/pprint findings))))
        expected (edn/read-string (slurp expected-findings-file))]
    (assert-submaps2 expected findings)))
