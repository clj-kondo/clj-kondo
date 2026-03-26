(ns clj-kondo.namespace-config-test
  (:require
   [babashka.fs :as fs]
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(deftest namespace-config-test
  (fs/delete-tree (fs/file "corpus" "namespace_config" ".clj-kondo" ".cache"))
  (fs/delete-tree (fs/file "corpus" "namespace_config" ".clj-kondo" "inline-configs"))
  (assert-submaps2
   [{:file "corpus/namespace_config/src/macro_usages.clj", :row 5, :col 12, :level :error, :message "Unresolved symbol: a"}
    {:file "corpus/namespace_config/src/macro_usages.clj",
     :row 6,
     :col 3,
     :level :error,
     :message "Unresolved symbol: a"}
    {:file "corpus/namespace_config/src/macro_usages.clj",
     :row 8,
     :col 20,
     :level :error,
     :message "Unresolved symbol: a"}
    {:file "corpus/namespace_config/src/macro_usages.clj",
     :row 9,
     :col 3,
     :level :error,
     :message "Unresolved symbol: a"}]
   (lint! (io/file "corpus" "namespace_config" "src" "macro_usages.clj")
          {:linters {:unresolved-symbol {:level :error :report-duplicates true}}}
          "--config-dir" (str (fs/file "corpus" "namespace_config" ".clj-kondo"))
          "--cache" "true"))
  (assert-submaps2
   [{:file "corpus/namespace_config/src/macros.clj",
     :row 5,
     :col 6,
     :level :error,
     :message "Expected: number, received: keyword."}
    {:file "corpus/namespace_config/src/macros2.cljc",
     :row 5,
     :col 6,
     :level :error,
     :message "Expected: number, received: keyword."}]
   (lint! [(io/file "corpus" "namespace_config" "src" "macros.clj")
           (io/file "corpus" "namespace_config" "src" "macros2.cljc")]
          {:linters {:unresolved-symbol {:level :error}
                     :type-mismatch {:level :error}}}
          "--config-dir" (str (fs/file "corpus" "namespace_config" ".clj-kondo"))
          "--cache" "true"))
  (let [inline-config (fs/file "corpus" "namespace_config" ".clj-kondo" "inline-configs" "macros.clj" "config.edn")]
    (is (fs/exists? inline-config))
    (is (not (str/includes? (slurp inline-config) "#:" ))
        "inline-config is written without namespaced maps shorthand"))
  (testing "Now the warnings are gone due to copied online config"
    (assert-submaps2
     []
     (lint! (io/file "corpus" "namespace_config" "src" "macro_usages.clj")
            {:linters {:unresolved-symbol {:level :error}}}
            "--config-dir" (str (fs/file "corpus" "namespace_config" ".clj-kondo"))
            "--cache" "true"))))
