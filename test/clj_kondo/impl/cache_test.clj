(ns clj-kondo.impl.cache-test
  (:require
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.core :as core-impl]
   [clj-kondo.test-utils :refer [lint! make-dirs remove-dir]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

(def cache-version core-impl/version)

(deftest cache-test
  (testing "arity checks work in all languages"
    (doseq [lang [:clj :cljs :cljc]]
      (let [tmp-dir (System/getProperty "java.io.tmpdir")
            test-cache-dir (.getPath (io/file tmp-dir "test-cache-dir"))
            test-source-dir (io/file tmp-dir "test-source-dir")]
        (remove-dir test-cache-dir)
        (make-dirs test-cache-dir)
        (remove-dir test-source-dir)
        (make-dirs test-source-dir)
        (io/copy "(ns foo) (defn foo [x])"
                 (io/file test-source-dir (str "foo."
                                               (name lang))))
        (lint! test-source-dir "--cache" test-cache-dir)
        (testing (format "var foo is found in cache of namespace foo (%s)" lang)
          (let [foo-cache (cache/from-cache-1
                           (io/file test-cache-dir cache-version)
                           lang 'foo)]
            (case lang
              (:clj :cljs)
              (is (some? (get foo-cache 'foo)))
              :cljc
              (do
                (is (some? (get-in foo-cache [:clj 'foo])))
                (is (some? (get-in foo-cache [:cljs 'foo])))))))
        (testing "linting only bar and using the cache option"
          (let [bar-file (io/file test-source-dir (str "bar."
                                                       (name lang)))]
            (io/copy "(ns bar (:require [foo :refer [foo]])) (foo 1 2 3)"
                     (io/file bar-file))
            (let [output (lint! bar-file "--cache" test-cache-dir)]
              (is (str/includes? (:message (first output))
                                 "foo/foo is called with 3 args but expects 1")))))
        (testing "arity of foo has changed"
          (io/copy "(ns foo) (defn foo [x y])"
                   (io/file test-source-dir (str "foo."
                                                 (name lang))))
          (lint! test-source-dir "--cache" test-cache-dir)
          (let [bar-file (io/file test-source-dir (str "bar."
                                                       (name lang)))]
            (io/copy "(ns bar (:require [foo :refer [foo]])) (foo 1)"
                     (io/file bar-file))
            (let [output (lint! bar-file "--cache" test-cache-dir)]
              (is (str/includes? (:message (first output))
                                 "foo/foo is called with 1 arg but expects 2")))
            (io/copy "(ns bar (:require [foo :refer [foo]])) (foo 1 2)"
                     (io/file bar-file))
            (let [output (lint! bar-file "--cache" test-cache-dir)]
              (is (empty? output))))))))
  (testing "arity checks work in clj -> cljc and cljs -> cljc"
    (let [tmp-dir (System/getProperty "java.io.tmpdir")
          test-cache-dir (.getPath (io/file tmp-dir "test-cache-dir"))
          test-source-dir (io/file tmp-dir "test-source-dir")
          foo (io/file test-source-dir "foo.cljc")]
      (doseq [lang [:clj :cljs]]
        (let [bar (io/file test-source-dir (str "bar."
                                                (name lang)))]
          (remove-dir test-cache-dir)
          (make-dirs test-cache-dir)
          (remove-dir test-source-dir)
          (make-dirs test-source-dir)
          (io/copy "(ns foo) (defn foo [x])"
                   foo)
          (io/copy "(ns bar (:require [foo :refer [foo]])) (foo 1 2 3)"
                   bar)
          ;; populate cljc cache
          (lint! foo "--cache" test-cache-dir)
          (let [output (lint! bar "--cache" test-cache-dir)]
            (is (str/includes? (:message (first output))
                               "foo/foo is called with 3 args but expects 1")))))))
  (testing ":refer :all ns is loaded from cache"
      (let [tmp-dir (System/getProperty "java.io.tmpdir")
            test-cache-dir (.getPath (io/file tmp-dir "test-cache-dir"))
            test-source-dir (io/file tmp-dir "test-source-dir")
            foo (io/file test-source-dir "foo.clj")
            bar (io/file test-source-dir (str "bar.clj"))]
        (remove-dir test-cache-dir)
        (make-dirs test-cache-dir)
        (remove-dir test-source-dir)
        (make-dirs test-source-dir)
        (io/copy "(ns foo) (defn foo [x])"
                 foo)
        (io/copy "(ns bar (:require [foo :refer :all])) (foo 1 2 3)"
                 bar)
        ;; populate cache
        (lint! foo "--cache" test-cache-dir)
        (let [output (lint! bar "--cache" test-cache-dir)]
          (is (str/includes? (:message (first output))
                             "foo/foo is called with 3 args but expects 1")))))
  (testing "--cache-dir option (--cache is deprecated as the option for passing dir)"
    (let [tmp-dir (System/getProperty "java.io.tmpdir")
          test-cache-dir (.getPath (io/file tmp-dir "test-cache-dir"))
          test-source-dir (io/file tmp-dir "test-source-dir")
          foo (io/file test-source-dir "foo.clj")
          bar (io/file test-source-dir (str "bar.clj"))]
      (remove-dir test-cache-dir)
      (make-dirs test-cache-dir)
      (remove-dir test-source-dir)
      (make-dirs test-source-dir)
      (io/copy "(ns foo) (defn foo [x])"
               foo)
      (io/copy "(ns bar (:require [foo :refer :all])) (foo 1 2 3)"
               bar)
      ;; populate cache
      (lint! foo "--cache" "true" "--cache-dir" test-cache-dir)
      (let [output (lint! bar "--cache" "true" "--cache-dir" test-cache-dir)]
        (is (str/includes? (:message (first output))
                           "foo/foo is called with 3 args but expects 1"))))))

(deftest lock-test
  (let [tmp-dir (System/getProperty "java.io.tmpdir")
        test-cache-dir-file (io/file tmp-dir "test-cache-dir")
        test-cache-dir-path (.getPath test-cache-dir-file)]
    (testing "with-cache returns value"
      (is (= 6 (cache/with-cache test-cache-dir-path 0
                 (+ 1 2 3)))))
    (testing "the directory lock works"
      (let [fut (future
                  (cache/with-cache test-cache-dir-path 0
                    (Thread/sleep 500)
                    (+ 1 2 3)))]
        (Thread/sleep 10)
        (is (thrown-with-msg? Exception
                              #"cache is locked"
                              (cache/with-cache test-cache-dir-path 0
                                (+ 1 2 3))))
        (is (= 6 @fut))))
    (testing "retries"
      (let [fut (future
                  (cache/with-cache test-cache-dir-path 0
                    (Thread/sleep 500)
                    (+ 1 2 3)))]
        (Thread/sleep 10)
        (is (= 6 (cache/with-cache test-cache-dir-path 10
                   (+ 1 2 3))))
        (is (= 6 @fut))))))

(deftest resolve-config-dir
  (testing "config dir should be resolved relative to the only file linted"
    (let [tmp-dir (System/getProperty "java.io.tmpdir")
          test-source-dir (io/file tmp-dir "test-source-dir")
          test-config-dir (io/file test-source-dir ".clj-kondo")
          foo (io/file test-source-dir "foo.clj")]
      (remove-dir test-source-dir)
      (make-dirs test-source-dir)
      (remove-dir test-config-dir)
      (make-dirs test-config-dir)
      (io/copy "(ns foo) (defn foo [x]) (foo)" foo)
      ;; populate cache
      (is (seq (lint! foo "--filename" (.getPath foo) "--cache" "true")))
      (is (.exists (io/file test-config-dir ".cache"))))))

;;;; Scratch

(comment
  )
