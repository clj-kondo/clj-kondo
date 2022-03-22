(ns clj-kondo.core-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.core :refer [path-separator]]
   [clj-kondo.test-utils :refer [file-path file-separator assert-submaps]]
   [clojure.edn :as edn]
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
                                               ".m2" "repository" "org" "clojure" "spec.alpha" "0.2.194"
                                               "spec.alpha-0.2.194.jar")]}))]
        (is (seq findings))
        (is (= findings
               (:findings
                (clj-kondo/run! {:lint [(io/file (System/getProperty "user.home")
                                                 ".m2" "repository" "org" "clojure" "spec.alpha" "0.2.194"
                                                 "spec.alpha-0.2.194.jar")]}))))))
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

(deftest analysis-findings-interaction-test
  (testing "github issue 1246")
  (let [res (with-in-str "(fn [{:keys [a] :or {a 1}}] a)"
              (clj-kondo/run!
               {:lint ["-"]
                :config {:output {:analysis {:locals true}}
                         :linters {:unused-bindings {:level :warning}}}}))]
    (is (empty? (:findings res)))))

(deftest fn-literal-end-location-test
  (let [res (with-in-str "#(if 1 2)"
              (clj-kondo/run!
               {:lint ["-"]
                :config {:linters {:missing-else-branch {:level :warning}}}}))
        findings (:findings res)
        first-and-only-finding (first findings)]
    (is (= 1 (count findings)))
    (is (= 1 (:end-row first-and-only-finding)))
    (is (= 10 (:end-col first-and-only-finding)))))

(deftest findings-serialization-test
  (let [{:keys [:findings]}
        (with-in-str "(ns test (:require [\"@material-ui/core\" :default mui]))"
          (clj-kondo/run!
           {:lint   ["-"]}))]
    (is (edn/read-string (pr-str findings)))))

(defn custom-linter [code lang reg-callback]
  (let [file? (instance? java.io.File code)]
    (with-in-str (if file? "" code)
      (clj-kondo/run!
       {:lint   [(if file? (str code) "-")]
        :lang lang
        :config {:linters {:org.acme/forbidden-var {:level :error}}
                 :output {:analysis true}}
        :custom-lint-fn (fn [{:keys [analysis reg-finding!]}]
                          (let [evals (filter #(and (= 'clojure.core (:to %))
                                                    (= 'eval (:name %))) (:var-usages analysis))]
                            (doseq [e evals]
                              (reg-callback
                               (reg-finding! (assoc (select-keys e [:filename :row :end-row :col :end-col])
                                                    :end-row (:name-end-row e)
                                                    :end-col (:name-end-col e)
                                                    :type :org.acme/forbidden-var))))))}))))

(deftest custom-lint-fn-test
  (testing "custom-lint reg a new finding and reg-finding! return the new finding"
    ;; TODO
    #_:clj-kondo/ignore
    (let [res (custom-linter "(eval '(+ 1 2 3))" :clj #(is %))]
      (is (= [{:filename "<stdin>", :row 1, :col 1, :end-row 1, :end-col 6,
               :type :org.acme/forbidden-var, :level :error}]
             (:findings res)))))
  (testing "ignore hints return nil during reg-finding! for clj files"
    (let [res (custom-linter "#_:clj-kondo/ignore (eval '(+ 1 2 3))" :clj #(is (not %)))]
      (is (empty? (:findings res)))))
  (testing "ignore hints return nil during reg-finding! for clj files"
    (let [res (custom-linter "#_:clj-kondo/ignore (eval '(+ 1 2 3))" :cljs #(is (not %)))]
      (is (empty? (:findings res)))))
  (testing "ignore hints return nil during reg-finding! for cljc files"
    (let [res (custom-linter (io/file "corpus/custom_lint_fn_ignore.cljc") :cljc #(is (not %)))]
      (is (empty? (:findings res))))))

(deftest run-skip-lint
  (testing "Copy configs only without lint."
    (let [res (clj-kondo/run!
               {:lint [(file-path "corpus" "invalid_arity")]
                :copy-configs true
                :skip-lint true
                :config {:output {:analysis true}}})]
      (is (empty? (:findings res)))
      (is (every? empty? (vals (:analysis res)))))))

;;;; Scratch

(comment
  (.getPath (io/file "foo" "bar"))
  (-> (clj-kondo/run! {:lint ["corpus"] :config {:output {:progress true}}})
      (clj-kondo/print!)))
