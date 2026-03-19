(ns clj-kondo.impl.analysis.aot-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.analysis.aot :as aot]
   [clj-kondo.impl.cache]
   [clj-kondo.test-utils :refer [file-path make-dirs remove-dir]]
   [clojure.test :refer [deftest is testing]])
  (:import
   [java.util.jar JarFile]))

(def aot-jar (file-path "corpus" "aot" "aot-only.jar"))

(deftest extract-ns-vars-test
  ;; Verify bytecode extraction produces correct var metadata
  ;; from an AOT-compiled __init.class.
  (testing "extract-ns-vars"
    (with-open [jar (JarFile. ^String aot-jar)]
      (let [entry (.getJarEntry jar "aot_test/sample__init.class")
            vars (aot/extract-ns-vars jar entry)]

        (testing "extracts public functions with correct arities"
          (is (= #{1} (get-in vars ['one-arg :fixed-arities])))
          (is (= #{2} (get-in vars ['two-args :fixed-arities])))
          (is (= #{3} (get-in vars ['three-args :fixed-arities]))))

        (testing "extracts varargs with min arity"
          (is (= 1 (get-in vars ['varargs :varargs-min-arity])))
          (is (nil? (get-in vars ['varargs :fixed-arities]))))

        (testing "detects private vars"
          (is (true? (get-in vars ['private-fn :private])))
          (is (nil? (get-in vars ['one-arg :private]))))

        (testing "detects macros"
          (is (true? (get-in vars ['sample-macro :macro])))
          (is (= :macro (get-in vars ['sample-macro :type]))))

        (testing "adjusts macro arities for implicit &form/&env"
          (is (= 0 (get-in vars ['sample-macro :varargs-min-arity]))))

        (testing "extracts plain defs"
          (is (contains? vars 'a-value))
          (is (nil? (get-in vars ['a-value :fixed-arities]))))

        (testing "sets correct namespace on all vars"
          (is (every? #(= 'aot-test.sample (:ns %))
                      (vals vars))))

        (testing "excludes clojure.core/in-ns"
          (is (not (contains? vars 'in-ns))))))))

(deftest init-class->ns-name-test
  ;; Verify path-to-namespace conversion (approximate, pre-underscore)
  (testing "init-class->ns-name"
    (testing "converts path separators to dots"
      (is (= "foo.bar" (aot/init-class->ns-name "foo/bar__init.class"))))
    (testing "handles single segment"
      (is (= "foo" (aot/init-class->ns-name "foo__init.class"))))))

(defn- with-temp-cache [f]
  (let [cache-dir (file-path ".clj-kondo-aot-test")]
    (try
      (make-dirs cache-dir)
      (f cache-dir)
      (finally
        (remove-dir cache-dir)))))

(deftest aot-refer-all-test
  ;; Lint code using :refer :all from an AOT-only namespace.
  ;; Verify no false-positive unresolved-symbol warnings.
  (with-temp-cache
    (fn [cache-dir]
      (testing "refer-all from AOT-only namespace"
        ;; Populate cache from AOT jar
        (clj-kondo/run! {:lint [aot-jar]
                          :dependencies true
                          :cache-dir cache-dir})

        (testing "resolves all vars without false positives"
          (let [res (clj-kondo/run!
                      {:lint ["corpus/aot/use_aot.clj"]
                       :cache-dir cache-dir
                       :config {:linters {:refer-all {:level :off}}}})
                findings (:findings res)]
            (is (empty? findings))))))))

(deftest aot-refer-specific-test
  ;; Lint code using :refer [specific-var] from an AOT-only namespace.
  (with-temp-cache
    (fn [cache-dir]
      (testing "refer specific vars from AOT-only namespace"
        (clj-kondo/run! {:lint [aot-jar]
                          :dependencies true
                          :cache-dir cache-dir})

        (testing "resolves referred vars without false positives"
          (let [res (clj-kondo/run!
                      {:lint ["corpus/aot/use_aot_refer.clj"]
                       :cache-dir cache-dir})
                findings (:findings res)]
            (is (empty? findings))))))))

(deftest aot-arity-checking-test
  ;; Verify arity errors are still reported for AOT-only functions.
  (with-temp-cache
    (fn [cache-dir]
      (testing "arity checking for AOT-only functions"
        (clj-kondo/run! {:lint [aot-jar]
                          :dependencies true
                          :cache-dir cache-dir})

        (testing "reports wrong arities"
          (let [res (clj-kondo/run!
                      {:lint ["corpus/aot/use_aot_arity_error.clj"]
                       :cache-dir cache-dir})
                findings (:findings res)]
            (is (= 2 (count findings)))
            (is (every? #(= :invalid-arity (:type %)) findings))
            (is (some #(re-find #"one-arg.*3 args.*expects 1"
                                (:message %))
                      findings))
            (is (some #(re-find #"two-args.*1 arg.*expects 2"
                                (:message %))
                      findings))))))))

(def aot-with-source-jar (file-path "corpus" "aot" "aot-with-source.jar"))

(deftest aot-source-precedence-test
  ;; When both .clj source and __init.class exist in a jar, source
  ;; analysis takes precedence. Source-analyzed cache entries have
  ;; :row/:col location data that AOT extraction does not produce.
  (with-temp-cache
    (fn [cache-dir]
      (testing "source analysis data is used when both source and class exist"
        (clj-kondo/run! {:lint [aot-with-source-jar]
                          :dependencies true
                          :cache-dir cache-dir})
        ;; Read the cached data for the namespace.
        ;; resolve-cache-dir appends "v1" to the cache-dir path.
        (let [internal-cache-dir (str cache-dir "/v1")
              cached (clj-kondo.impl.cache/with-thread-lock
                       (clj-kondo.impl.cache/with-cache internal-cache-dir 6
                         (clj-kondo.impl.cache/from-cache-1
                           internal-cache-dir :clj 'aot-test.sample)))]
          (testing "cache entry has source-analysis location data"
            ;; Source analysis includes :row/:col; AOT extraction does not
            (is (some? (get-in cached ['one-arg :row])))
            (is (some? (get-in cached ['one-arg :col]))))

          (testing "cache entry has full source-analysis metadata"
            (is (= #{1} (get-in cached ['one-arg :fixed-arities])))
            (is (= 'aot-test.sample (get-in cached ['one-arg :ns])))))

        (testing "linting against the jar works correctly"
          (let [res (clj-kondo/run!
                      {:lint ["corpus/aot/use_aot_arity_error.clj"]
                       :cache-dir cache-dir})
                findings (:findings res)]
            (is (= 2 (count findings)))
            (is (every? #(= :invalid-arity (:type %)) findings))))))))
