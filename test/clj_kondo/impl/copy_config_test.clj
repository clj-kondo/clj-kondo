(ns clj-kondo.impl.copy-config-test
  (:require [clj-kondo.core :as clj-kondo]
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
                       :no-warnings true}))
    (is (.exists (io/file tmp-dir "clj-kondo" "slingshot")))))

(deftest copy-config-from-dir
  (let [tmp-dir (.toFile (Files/createTempDirectory
                          "config"
                          (into-array java.nio.file.attribute.FileAttribute [])))]
    (binding [*err* (java.io.StringWriter.)]
      (clj-kondo/run! {:lint [(io/file "corpus" "exports" "dir")]
                       :config-dir tmp-dir
                       :no-warnings true}))
    (is (.exists (io/file tmp-dir "clj-kondo" "slingshot")))))
