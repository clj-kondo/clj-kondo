(ns clj-kondo.clj-kondo.diff-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [assert-submaps2]]
   [clojure.edn :as edn]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]
   [matcher-combinators.config :as mconfig]))

(deftest clj-kondo-diff-test
  (binding [mconfig/*use-abbreviation* true]
    (let [cp (-> (p/shell {:out :string} "clojure -Spath -A:cljs:clojure-1.12.1") :out str/trim)
          config-dir (fs/file "test-regression" "clj_kondo" "clj_kondo" ".clj-kondo")
          _ (fs/delete-tree (fs/file config-dir ".cache"))
          _ (fs/create-dirs config-dir)
          _ (spit (fs/file config-dir "config.edn") '{:linters {:unresolved-symbol {:exclude [(clj-kondo.impl.utils/one-of)]}}})
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
                                                 :redundant-str-call {:level :warning}
                                                 :redundant-let-binding {:level :warning}
                                                 :redundant-primitive-coercion {:level :warning}}
                                                :output {:langs false}}})
          findings (:findings lint-result)
          actual (remove #(str/includes? (:filename %) "src/scratch") findings)
          expected-findings-file (fs/file "test-regression" "clj_kondo" "clj_kondo" "findings.edn")
          _ (when (System/getenv "CLJ_KONDO_REGRESSION_UPDATE") (spit expected-findings-file (with-out-str (pp/pprint actual))))
          expected (edn/read-string (slurp expected-findings-file))]
      (when-not (assert-submaps2 expected actual)
        (spit "/tmp/actual.edn" (with-out-str (pp/pprint actual)))
        (if (fs/which "difft")
          (p/shell "difft" (str expected-findings-file) "/tmp/actual.edn")
          (println "Install difftastic for a better diff report"))))))
