(ns clj-kondo.metabase.metabase-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [assert-submaps2]]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest metabase-test
  (let [test-regression-checkouts (fs/file "test-regression" "checkouts")
        _ (fs/create-dirs test-regression-checkouts)
        dir (fs/file test-regression-checkouts "metabase")
        config-dir (fs/file dir ".clj-kondo")]
    (when-not (fs/exists? dir)
      (p/shell {:dir test-regression-checkouts} "git clone --no-checkout --depth 1 https://github.com/metabase/metabase"))
    (p/shell {:dir dir} "git fetch --depth 1 origin" "aa0cdb546d7c9e4ef5c52ad23c656272b7599e23")
    (p/shell {:dir dir} "git fetch  --depth 1 origin" "aa0cdb546d7c9e4ef5c52ad23c656272b7599e23")
    (p/shell {:dir dir} "git checkout aa0cdb546d7c9e4ef5c52ad23c656272b7599e23 src test .clj-kondo deps.edn")
    (let [cp (-> (p/shell {:dir dir :out :string} "clj -Spath") :out str/trim)]
      (clj-kondo/run! {:config-dir config-dir ;; important to pass this to set the right dir for copy-configs!
                       :copy-configs true
                       :lint [cp]}))
    (let [paths (mapv #(str (fs/file dir %)) ["src" #_"test"])
          lint-result (clj-kondo/run! {:config-dir config-dir
                                       :lint paths
                                       :repro true})]
      (assert-submaps2
       '[{:end-row 43,
          :type :condition-always-true,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/app_db/update_h2.clj",
          :col 19,
          :end-col 57,
          :langs (),
          :message "Condition always true",
          :row 43}
         {:end-row 4,
          :type :deprecated-namespace,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/impl/email.clj",
          :col 5,
          :end-col 16,
          :langs (),
          :message "Namespace hiccup.core is deprecated since 2.0.",
          :row 4}
         {:end-row 4,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/impl/email.clj",
          :col 25,
          :end-col 29,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 4}
         {:end-row 91,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/impl/email.clj",
          :col 22,
          :end-col 62,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 91}
         {:end-row 213,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/impl/email.clj",
          :col 28,
          :end-col 59,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 213}
         {:end-row 262,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/impl/email.clj",
          :col 43,
          :end-col 57,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 262}
         {:end-row 4,
          :type :deprecated-namespace,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/body.clj",
          :col 5,
          :end-col 16,
          :langs (),
          :message "Namespace hiccup.core is deprecated since 2.0.",
          :row 4}
         {:end-row 4,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/body.clj",
          :col 25,
          :end-col 26,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 4}
         {:end-row 156,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/body.clj",
          :col 63,
          :end-col 99,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 156}
         {:end-row 417,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/body.clj",
          :col 7,
          :end-col 16,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 417}
         {:end-row 500,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/body.clj",
          :col 27,
          :end-col 36,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 500}
         {:end-row 512,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/body.clj",
          :col 25,
          :end-col 39,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 512}
         {:end-row 3,
          :type :deprecated-namespace,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/card.clj",
          :col 5,
          :end-col 16,
          :langs (),
          :message "Namespace hiccup.core is deprecated since 2.0.",
          :row 3}
         {:end-row 3,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/card.clj",
          :col 25,
          :end-col 26,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 3}
         {:end-row 32,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/card.clj",
          :col 3,
          :end-col 38,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 32}
         {:end-row 37,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/card.clj",
          :col 3,
          :end-col 86,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 37}
         {:end-row 63,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/card.clj",
          :col 26,
          :end-col 39,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 63}
         {:end-row 11,
          :type :deprecated-namespace,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/png.clj",
          :col 5,
          :end-col 16,
          :langs (),
          :message "Namespace hiccup.core is deprecated since 2.0.",
          :row 11}
         {:end-row 11,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/png.clj",
          :col 25,
          :end-col 29,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 11}
         {:end-row 135,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/png.clj",
          :col 16,
          :end-col 56,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 129}
         {:end-row 7,
          :type :deprecated-namespace,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/preview.clj",
          :col 5,
          :end-col 16,
          :langs (),
          :message "Namespace hiccup.core is deprecated since 2.0.",
          :row 7}
         {:end-row 97,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/preview.clj",
          :col 3,
          :end-col 58,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 97}
         {:end-row 4,
          :type :deprecated-namespace,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/table.clj",
          :col 5,
          :end-col 16,
          :langs (),
          :message "Namespace hiccup.core is deprecated since 2.0.",
          :row 4}
         {:end-row 4,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/table.clj",
          :col 25,
          :end-col 26,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 4}
         {:end-row 79,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/table.clj",
          :col 8,
          :end-col 15,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 79}
         {:end-row 90,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/table.clj",
          :col 26,
          :end-col 35,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 90}
         {:end-row 140,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/table.clj",
          :col 105,
          :end-col 112,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 140}
         {:end-row 178,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/table.clj",
          :col 5,
          :end-col 12,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 178}
         {:end-row 222,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/table.clj",
          :col 26,
          :end-col 34,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 222}
         {:end-row 226,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/table.clj",
          :col 14,
          :end-col 22,
          :langs (),
          :message "#'hiccup.core/h is deprecated since 2.0",
          :row 226}
         {:end-row 4,
          :type :deprecated-namespace,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/util.clj",
          :col 5,
          :end-col 16,
          :langs (),
          :message "Namespace hiccup.core is deprecated since 2.0.",
          :row 4}
         {:end-row 4,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/util.clj",
          :col 25,
          :end-col 29,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 4}
         {:end-row 201,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/channel/render/util.clj",
          :col 3,
          :end-col 58,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 189}
         {:end-row 708,
          :type :condition-always-true,
          :level :warning,
          :lang :clj,
          :filename
          "test-regression/checkouts/metabase/src/metabase/lib/util.cljc",
          :col 27,
          :end-col 79,
          :cljc true,
          :langs (:clj :cljs),
          :message "Condition always true",
          :row 707}
         {:end-row 4,
          :type :deprecated-namespace,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/public_sharing/api.clj",
          :col 5,
          :end-col 16,
          :langs (),
          :message "Namespace hiccup.core is deprecated since 2.0.",
          :row 4}
         {:end-row 390,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/public_sharing/api.clj",
          :col 3,
          :end-col 43,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 387}
         {:end-row 8,
          :type :deprecated-namespace,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/pulse/api/pulse.clj",
          :col 5,
          :end-col 16,
          :langs (),
          :message "Namespace hiccup.core is deprecated since 2.0.",
          :row 8}
         {:end-row 8,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/pulse/api/pulse.clj",
          :col 25,
          :end-col 29,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 8}
         {:end-row 331,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/pulse/api/pulse.clj",
          :col 19,
          :end-col 110,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 328}
         {:end-row 266,
          :type :condition-always-true,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/queries/api/card.clj",
          :col 20,
          :end-col 31,
          :langs (),
          :message "Condition always true",
          :row 266}
         {:end-row 4,
          :type :deprecated-namespace,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/util/embed.clj",
          :col 5,
          :end-col 16,
          :langs (),
          :message "Namespace hiccup.core is deprecated since 2.0.",
          :row 4}
         {:end-row 4,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/util/embed.clj",
          :col 25,
          :end-col 29,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 4}
         {:end-row 27,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/util/embed.clj",
          :col 3,
          :end-col 37,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 24}
         {:end-row 31,
          :type :deprecated-var,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/util/embed.clj",
          :col 3,
          :end-col 58,
          :langs (),
          :message "#'hiccup.core/html is deprecated since 2.0",
          :row 31}
         {:end-row 32,
          :protocol-ns clj-yaml.core,
          :protocol-name YAMLCodec,
          :type :missing-protocol-method,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/util/yaml.clj",
          :methods [encode],
          :col 18,
          :end-col 32,
          :langs (),
          :message "Missing protocol method(s): decode",
          :row 32}
         {:end-row 131,
          :type :condition-always-true,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/warehouse_schema/api/field.clj",
          :col 44,
          :end-col 71,
          :langs (),
          :message "Condition always true",
          :row 131}
         {:end-row 46,
          :type :condition-always-true,
          :level :warning,
          :filename
          "test-regression/checkouts/metabase/src/metabase/warehouse_schema/models/field.clj",
          :col 18,
          :end-col 29,
          :langs (),
          :message "Condition always true",
          :row 46}]
       (:findings lint-result)))))
