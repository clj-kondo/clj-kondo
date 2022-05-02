(ns clj-kondo.core-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.core :refer [path-separator]]
   [clj-kondo.test-utils :refer [assert-submaps file-path file-separator]]
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

(deftest backwards-compatibility-with-analysis-in-output-config-test
  (let [res (fn [config]
              (with-in-str "(fn [a] a)"
                (clj-kondo/run!
                 {:lint ["-"]
                  :config config})))
        old-style (res {:output {:analysis {:locals true}}})
        new-style (res {:analysis {:locals true}})]
    (is (seq (:analysis new-style)))
    (is (= (:analysis old-style)
           (:analysis new-style)))
    (is (nil? (:analysis (res {:analysis false :output {:analysis {:locals true}}}))))))

(deftest analyze-project-skeleton-test
  (let [{:keys [analysis findings]}
        (with-in-str "(ns my-ns (:require [clojure.set :as set])) (defn foo [a] a) (defn bar [] (foo 1))"
          (clj-kondo/run!
           {:lint ["-"]
            :config {:analysis {:var-usages false
                                :var-definitions {:shallow true}}}
            :skip-lint true}))]
    (is (empty? findings))
    (is (empty? (:var-usages analysis)))
    (assert-submaps
     '[{:name my-ns}]
     (:namespace-definitions analysis))
    (assert-submaps
     '[{:from my-ns, :to clojure.set, :alias set}]
     (:namespace-usages analysis))
    (assert-submaps
     '[{:fixed-arities #{1}, :ns my-ns, :name foo, :defined-by clojure.core/defn}
       {:fixed-arities #{0}, :ns my-ns, :name bar, :defined-by clojure.core/defn}]
     (:var-definitions analysis))))

(deftest analysis-findings-interaction-test
  (testing "github issue 1246")
  (let [res (with-in-str "(fn [{:keys [a] :or {a 1}}] a)"
              (clj-kondo/run!
               {:lint ["-"]
                :config {:analysis {:locals true}
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
                 :analysis true}
        :custom-lint-fn (fn [{:keys [analysis reg-finding!]}]
                          (let [evals (filter #(and (= 'clojure.core (:to %))
                                                    (= 'eval (:name %))) (:var-usages analysis))]
                            (doseq [e evals]
                              (reg-callback
                               (reg-finding! (assoc (select-keys e [:filename :row :end-row :col :end-col])
                                                    :end-row (:name-end-row e)
                                                    :end-col (:name-end-col e)
                                                    :type :org.acme/forbidden-var))))))}))))

(defn file-analyzed-fn [paths lang file-analyzed-fn extra-config]
  (clj-kondo/run!
   (merge
    {:lint paths
     :lang lang
     :config {:analysis true}
     :file-analyzed-fn file-analyzed-fn}
    extra-config)))

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
                :parallel true})]
      (is (empty? (:findings res)))
      (is (empty? (:analysis res))))))

(deftest file-analyzed-fn-test
  (testing "we call the callback fn for all given entries"
    (let [calls (atom [])
          res (file-analyzed-fn
                ["corpus/use.clj"
                 "corpus/case.clj"
                 "corpus/schema"
                 "corpus/exports/dir"]
                :clj
                (fn [entry-map]
                  (swap! calls conj entry-map))
                {})]
      (is (= 6 (:files (:summary res))))
      (assert-submaps
        #{{:filename "corpus/use.clj" :uri #"file:/.*/corpus/use.clj" :total-files 6}
          {:filename "corpus/case.clj" :uri #"file:/.*/corpus/case.clj" :total-files 6}
          {:filename "corpus/schema/defs.clj" :uri #"file:/.*/corpus/schema/defs.clj" :total-files 6}
          {:filename "corpus/schema/defmethod.clj" :uri #"file:/.*/corpus/schema/defmethod.clj" :total-files 6}
          {:filename "corpus/schema/calls.clj" :uri #"file:/.*/corpus/schema/calls.clj" :total-files 6}
          {:filename "corpus/schema/defrecord.clj" :uri #"file:/.*/corpus/schema/defrecord.clj" :total-files 6}}
        (set @calls))
      (is (every? #(and (int? (:total-files %))
                        (<= (:total-files %) 6)) @calls))))
  (testing "when lint is classpath"
    (let [calls (atom [])
          res (file-analyzed-fn
                [(str/join
                   path-separator
                   ["corpus/invalid_arity" "corpus/private"])]
                :clj
                (fn [entry-map]
                  (swap! calls conj entry-map))
                {})]
      (is (= 5 (:files (:summary res))))
      (assert-submaps
        #{{:filename "corpus/invalid_arity/defs.clj" :uri #"file:/.*/corpus/invalid_arity/defs.clj" :total-files 5}
          {:filename "corpus/invalid_arity/order.clj" :uri #"file:/.*/corpus/invalid_arity/order.clj" :total-files 5}
          {:filename "corpus/invalid_arity/calls.clj" :uri #"file:/.*/corpus/invalid_arity/calls.clj" :total-files 5}
          {:filename "corpus/private/private_calls.clj" :uri #"file:/.*/corpus/private/private_calls.clj" :total-files 5}
          {:filename "corpus/private/private_defs.clj" :uri  #"file:/.*/corpus/private/private_defs.clj" :total-files 5}}
        (set @calls))
      (is (every? #(and (int? (:total-files %))
                        (<= (:total-files %) 5)) @calls))))
  (testing "when parallel"
    (let [calls (atom [])
          res (file-analyzed-fn
                ["corpus/use.clj"
                 "corpus/case.clj"
                 "corpus/schema"
                 "corpus/exports/dir"]
                :clj
                (fn [entry-map]
                  (swap! calls conj entry-map))
                {:parallel true})]
      (is (= 6 (:files (:summary res))))
      (assert-submaps
        #{{:filename "corpus/use.clj" :uri #"file:/.*/corpus/use.clj" :total-files 6}
          {:filename "corpus/schema/defs.clj" :uri #"file:/.*/corpus/schema/defs.clj" :total-files 6}
          {:filename "corpus/case.clj" :uri #"file:/.*/corpus/case.clj" :total-files 6}
          {:filename "corpus/schema/defmethod.clj" :uri #"file:/.*/corpus/schema/defmethod.clj" :total-files 6}
          {:filename "corpus/schema/calls.clj" :uri #"file:/.*/corpus/schema/calls.clj" :total-files 6}
          {:filename "corpus/schema/defrecord.clj" :uri #"file:/.*/corpus/schema/defrecord.clj" :total-files 6}}
        (set @calls))
      (is (every? #(and (int? (:total-files %))
                        (<= (:total-files %) 6)) @calls)))))

;;;; Scratch

(comment
  (.getPath (io/file "foo" "bar"))
  (-> (clj-kondo/run! {:lint ["corpus"] :config {:output {:progress true}}})
      (clj-kondo/print!)))
