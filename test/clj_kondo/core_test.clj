(ns clj-kondo.core-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.core :refer [path-separator]]
   [clj-kondo.test-utils :refer [file-path file-separator assert-submaps]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

;; NOTE: most functionality is tested in the main_test.clj namespace.

(deftest run!-test
  (testing "file arguments"
    (testing "file arguments can be strings or files"
      (let [res (clj-kondo/run! {:lint [(file-path "corpus" "invalid_arity")
                                        (file-path "corpus" "private")]})
            findings (:findings res)
            filenames (->> findings
                           (map :filename)
                           (map #(str/split % (re-pattern (java.util.regex.Pattern/quote file-separator))))
                           (map #(take 2 %))
                           set)]
        (is (= '#{("corpus" "invalid_arity") ("corpus" "private")}
               filenames))
        (is (seq findings))
        (is (= findings (:findings (clj-kondo/run! {:lint [(file-path "corpus" "invalid_arity")
                                                           (file-path "corpus" "private")]}))))))
    (testing "jar file as string or file"
      (let [findings (:findings
                      (clj-kondo/run! {:lint [(file-path
                                               (System/getProperty "user.home")
                                               ".m2" "repository" "org" "clojure" "spec.alpha" "0.2.187"
                                               "spec.alpha-0.2.187.jar")]}))]
        (is (seq findings))
        (is (= findings
               (:findings
                (clj-kondo/run! {:lint [(io/file (System/getProperty "user.home")
                                                 ".m2" "repository" "org" "clojure" "spec.alpha" "0.2.187"
                                                 "spec.alpha-0.2.187.jar")]}))))))
    (testing "classpath 'file' arg"
      (let [findings (:findings (clj-kondo/run!
                                 {:lint [(str/join
                                          path-separator
                                          ["corpus/invalid_arity" "corpus/private"])]}))
            filenames (->> findings
                           (map :filename)
                           (map #(str/split % (re-pattern (java.util.regex.Pattern/quote file-separator))))
                           (map #(take 2 %))
                           set)]
        (is (= '#{("corpus" "invalid_arity") ("corpus" "private")}
               filenames)))))
  (testing "summary result"
    (let [s (:summary (clj-kondo/run! {:lint ["src"]}))]
      (is s)
      (is (nat-int? (:error s)))
      (is (nat-int? (:warning s)))
      (is (nat-int? (:duration s)))
      (is (nat-int? (:files s)))))
  (testing "end locations are reported correctly"
    (let [{:keys [:findings]}
          (with-in-str
            "(x  )" (clj-kondo/run! {:lint ["-"]}))]
      (assert-submaps
       [{:level :error, :type :unresolved-symbol, :message "Unresolved symbol: x",
         :row 1, :col 2, :end-row 1, :end-col 3}]
       findings)))
  (testing "passing file as config arg"
    (let [{{:keys [error warning info]} :summary}
          (clj-kondo/run!
            {:lint   [(file-path "corpus" "invalid_arity")]
             :config (file-path "corpus" "config" "invalid_arity.edn")})]
      (is (zero? error))
      (is (zero? warning))
      (is (zero? info)))))

;;;; Scratch

(comment
  (.getPath (io/file "foo" "bar"))
  (-> (clj-kondo/run! {:lint ["corpus"] :config {:output {:progress true}}})
      (clj-kondo/print!))
  )
