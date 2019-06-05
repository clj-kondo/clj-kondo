(ns clj-kondo.core-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

;; NOTE: most functionality is tested in the main_test.clj namespace.

(deftest run!-test
  (testing "file arguments"
    (testing "file arguments can be strings or files"
      (let [findings (:findings (clj-kondo/run! {:files ["corpus/invalid_arity"
                                                         "corpus/private"]}))
            filenames (->> findings
                           (map :filename)
                           (map #(str/split % #"/"))
                           (map #(take 2 %))
                           set)]
        (is (= '#{("corpus" "invalid_arity") ("corpus" "private")}
               filenames))
        (is (seq findings))
        (is (= findings (:findings (clj-kondo/run! {:files [(io/file "corpus/invalid_arity")
                                                            (io/file "corpus/private")]}))))))
    (testing "jar file as string or file"
      (let [findings (:findings
                      (clj-kondo/run! {:files [(.getPath
                                                (io/file (System/getProperty "user.home")
                                                         ".m2" "repository" "org" "clojure" "spec.alpha" "0.2.176"
                                                         "spec.alpha-0.2.176.jar"))]}))]
        (is (seq findings))
        (is (= findings
               (:findings
                (clj-kondo/run! {:files [(io/file (System/getProperty "user.home")
                                                  ".m2" "repository" "org" "clojure" "spec.alpha" "0.2.176"
                                                  "spec.alpha-0.2.176.jar")]}))))))
    (testing "classpath 'file' arg"
      ;; TODO: use the classpath separator here
      (let [findings (:findings (clj-kondo/run! {:files ["corpus/invalid_arity:corpus/private"]}))
            filenames (->> findings
                           (map :filename)
                           (map #(str/split % #"/"))
                           (map #(take 2 %))
                           set)]
        (is (= '#{("corpus" "invalid_arity") ("corpus" "private")}
               filenames))))))

;;;; Scratch

(comment
  (.getPath (io/file "foo" "bar"))
  (-> (clj-kondo/run! {:files ["corpus"] :config {:output {:progress true}}})
      (clj-kondo/print!))
  )
