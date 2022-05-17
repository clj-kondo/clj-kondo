(ns clj-kondo.impl.copied-config-test
  (:require [babashka.fs :as fs]
            [clj-kondo.impl.core :as core-impl]
            [clj-kondo.impl.utils :as utils]
            [clojure.test :refer [deftest is testing]]))

(deftest copied-configs-resolution-test
  (testing "no findings"
    (testing "when clj-kondo :config-dir is not present"
      (is (nil? (core-impl/copied-config-paths {:detected-configs (atom ["cfg1" "cfg2"])}))))
    (testing "when no configs are copied"
      (is (nil? (core-impl/copied-config-paths {:config-dir "cfg-dir"
                                                :detected-configs (atom [])})))))
  (testing "found copied configs"
    (testing "are sorted"
      (is (= ["cfg-dir/cfg/b"
              "cfg-dir/cfg/c"
              "cfg-dir/cfg/w"
              "cfg-dir/cfg/z"]
             (core-impl/copied-config-paths {:config-dir "cfg-dir"
                                             :detected-configs (atom ["cfg/c" "cfg/z" "cfg/b" "cfg/w"])}))))
    (testing "returns config-dir relative to current dir"
      (is (= ["cfg-dir/cfg1"]
             (core-impl/copied-config-paths {:config-dir (str (fs/absolutize "cfg-dir"))
                                             :detected-configs (atom ["cfg1"])}))))
    (when utils/windows?
      (testing "unixifies all paths on Windows"
        (is (= ["cfg-dir/cfg/b" "cfg-dir/cfg/c" "cfg-dir/cfg/w" "cfg-dir/cfg/z"]
               (core-impl/copied-config-paths {:config-dir (str (fs/absolutize "cfg-dir"))
                                               :detected-configs (atom ["cfg\\c" "cfg\\z" "cfg\\b" "cfg\\w"])})))))))

(deftest print-copied-configs-test
  (is (= (apply str (interleave
                     ["Configs copied:"
                      "- cfg/a/b"
                      "- cfg/c/d"]
                     (repeat (System/getProperty "line.separator"))))
         (let [s (new java.io.StringWriter)]
           (binding [*err* s]
             (core-impl/print-copied-configs
              ["cfg/a/b" "cfg/c/d"]))
           (str s)))))

(deftest print-no-copied-configs-test
  (is (= (str "No configs copied." (System/getProperty "line.separator"))
         (let [s (new java.io.StringWriter)]
           (binding [*err* s]
             (core-impl/print-copied-configs []))
           (str s)))))
