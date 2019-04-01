(ns clj-kondo.impl.cache-test
  (:require
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.main :as main :refer [-main]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]
   [me.raynes.conch :refer [programs with-programs let-programs] :as sh]
   [clojure.string :as str]))

(programs rm mkdir echo)

(deftest cache-test
  (doseq [lang [:clj :cljs]]
    (let [tmp-dir (System/getProperty "java.io.tmpdir")
          test-cache-dir (.getPath (io/file tmp-dir "test-cache-dir"))
          test-source-dir (.getPath (io/file tmp-dir "test-source-dir"))]
      (rm "-rf" test-cache-dir)
      (mkdir "-p" test-cache-dir)
      (rm "-rf" test-source-dir)
      (mkdir "-p" test-source-dir)
      (io/copy "(ns foo) (defn foo [x])"
               (io/file test-source-dir (str "foo."
                                             (name lang))))
      (-main "--lint" test-source-dir "--cache" test-cache-dir)
      (testing
          "var foo is found in cache of namespace foo"
          (is (some? (get (cache/from-cache (io/file test-cache-dir "v1") lang 'foo)
                          'foo/foo))))
      (testing "linting only bar and using the cache option"
        (let [bar-file (.getPath (io/file test-source-dir (str "bar."
                                                               (name lang))))]
          (io/copy "(ns bar (:require [foo :refer [foo]])) (foo 1 2 3)"
                   (io/file bar-file))
          (let [output (with-out-str (-main "--lint" bar-file "--cache" test-cache-dir))]
            (str/includes? output "Wrong number of args (3) passed to foo/foo")))))))

(deftest lock-test
  (let [tmp-dir (System/getProperty "java.io.tmpdir")
        test-cache-dir-file (io/file tmp-dir "test-cache-dir")
        test-cache-dir-path (.getPath test-cache-dir-file)]
    #_(.mkdirs (io/file test-cache-dir-file "v1"))
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
