(ns clj-kondo.impl.copy-config-test
  (:require [clj-kondo.core :as clj-kondo]
            [clj-kondo.test-utils :as tu]
            [clojure.java.io :as io]
            [clojure.test :as t :refer [deftest is testing]])
  (:import [java.nio.file Files]))

(deftest copy-config-from-jar
  (let [tmp-dir (.toFile (Files/createTempDirectory
                          "config"
                          (into-array java.nio.file.attribute.FileAttribute [])))]
    (binding [*err* (java.io.StringWriter.)]
      (clj-kondo/run! {:lint [(io/file "corpus" "exports" "clj-kondo.config.jar")]
                       :config-dir tmp-dir
                       :copy-configs true}))
    (is (.exists (io/file tmp-dir "imports" "clj-kondo" "slingshot")))
    (is (= "{:hooks
 {:analyze-call {slingshot.slingshot/try+ clj-kondo.slingshot.try-plus/try+}}}
" (slurp (tu/normalize-newlines (io/file tmp-dir "imports" "clj-kondo" "slingshot" "config.edn")))))))

(deftest copy-config-from-dir
  (let [tmp-dir (.toFile (Files/createTempDirectory
                          "config"
                          (into-array java.nio.file.attribute.FileAttribute [])))]
    (binding [*err* (java.io.StringWriter.)]
      (clj-kondo/run! {:lint [(io/file "corpus" "exports" "dir")]
                       :config-dir tmp-dir
                       :copy-configs true}))
    (is (.exists (io/file tmp-dir "imports" "clj-kondo" "slingshot")))
    (is (= "{:hooks
 {:analyze-call {slingshot.slingshot/try+ clj-kondo.slingshot.try-plus/try+}}}
" (tu/normalize-newlines (slurp (io/file tmp-dir "imports" "clj-kondo" "slingshot" "config.edn")))))))

(deftest copy-config-skips-unchanged-content
  (let [tmp-dir (.toFile (Files/createTempDirectory "config" (into-array java.nio.file.attribute.FileAttribute [])))
        lint! (fn [] (binding [*err* (java.io.StringWriter.)]
                       (clj-kondo/run! {:lint [(io/file "corpus" "exports" "dir")]
                                        :config-dir tmp-dir
                                        :copy-configs true})))
        config-file (io/file tmp-dir "imports" "clj-kondo" "slingshot" "config.edn")
        hook-file (io/file tmp-dir "imports" "clj-kondo" "slingshot" "clj_kondo" "slingshot" "try_plus.clj")]
    (lint!)
    (testing "all files in a multi-file export are copied, not just the first"
      (is (.exists config-file))
      (is (.exists hook-file)))
    (testing "second run does not rewrite unchanged files"
      (let [before (.lastModified config-file)]
        (Thread/sleep 100)
        (lint!)
        (is (= before (.lastModified config-file)))))))
