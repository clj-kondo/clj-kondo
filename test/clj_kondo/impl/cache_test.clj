(ns clj-kondo.impl.cache-test
  (:require
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.main :as main :refer [main]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]
   [me.raynes.conch :refer [programs with-programs let-programs] :as sh]
   [clojure.string :as str]
   [clj-kondo.test-utils :refer [lint!]]))

(programs rm mkdir echo mv)

(def cache-version @#'main/version)

(deftest cache-test
  (testing "empty cache option warning (this test assumes you have no .clj-kondo
  directory at a higher level than the current working directory)"
    (let [tmp-dir (System/getProperty "java.io.tmpdir")
          test-source-dir (io/file tmp-dir "test-source-dir")]
      (rm "-rf" test-source-dir)
      (mkdir "-p" test-source-dir)
      (when (.exists (io/file ".clj-kondo"))
        (mv ".clj-kondo" ".clj-kondo.bak"))
      (io/copy "(ns foo) (defn foo [x])"
               (io/file test-source-dir (str "foo.clj")))
      (is (str/includes?
           (with-out-str (main "--lint" test-source-dir "--cache"))
           "no .clj-kondo directory found"))
      (when (.exists (io/file ".clj-kondo.bak"))
        (mv ".clj-kondo.bak" ".clj-kondo"))))
  (testing "arity checks work in all languages"
    (doseq [lang [:clj :cljs :cljc]]
      (let [tmp-dir (System/getProperty "java.io.tmpdir")
            test-cache-dir (.getPath (io/file tmp-dir "test-cache-dir"))
            test-source-dir (io/file tmp-dir "test-source-dir")]
        (rm "-rf" test-cache-dir)
        (mkdir "-p" test-cache-dir)
        (rm "-rf" test-source-dir)
        (mkdir "-p" test-source-dir)
        (io/copy "(ns foo) (defn foo [x])"
                 (io/file test-source-dir (str "foo."
                                               (name lang))))
        (lint! test-source-dir "--cache" test-cache-dir)
        (testing
            "var foo is found in cache of namespace foo"
          (let [foo-cache (cache/from-cache-1
                           (io/file test-cache-dir cache-version)
                           lang 'foo)]
            (is (some? (case lang (:clj :cljs)
                             (get foo-cache 'foo)
                             :cljc (get-in foo-cache [:cljc 'foo]))))))
        (testing "linting only bar and using the cache option"
          (let [bar-file (io/file test-source-dir (str "bar."
                                                       (name lang)))]
            (io/copy "(ns bar (:require [foo :refer [foo]])) (foo 1 2 3)"
                     (io/file bar-file))
            (let [output (lint! bar-file "--cache" test-cache-dir)]
              (is (str/includes? (:message (first output))
                                 "wrong number of args (3) passed to foo/foo")))))
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
                                 "wrong number of args (1) passed to foo/foo")))
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
          (rm "-rf" test-cache-dir)
          (mkdir "-p" test-cache-dir)
          (rm "-rf" test-source-dir)
          (mkdir "-p" test-source-dir)
          (io/copy "(ns foo) (defn foo [x])"
                   foo)
          (io/copy "(ns bar (:require [foo :refer [foo]])) (foo 1 2 3)"
                   bar)
          ;; populate cljc cache
          (lint! foo "--cache" test-cache-dir)
          (let [output (lint! bar "--cache" test-cache-dir)]
            (is (str/includes? (:message (first output))
                               "wrong number of args (3) passed to foo/foo"))))))))

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
                              (try
                                (cache/with-cache test-cache-dir-path 0
                                  (+ 1 2 3)))))
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

;;;; Scratch

(comment
  )
