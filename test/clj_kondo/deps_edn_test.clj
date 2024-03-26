(ns clj-kondo.deps-edn-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps assert-submaps2]]
            [clojure.test :refer [deftest testing is]]))

(deftest paths-test
  (testing "no report on valid value types"
    (let [bb-edn "{:paths [\"src\" \"test\"]}"]
      (is (empty? (lint! bb-edn
                         "--filename" "bb.edn"))))
    (let [deps-edn "{:paths [\"src\"  \"test\" :alias1]}"]
      (is (empty? (lint! deps-edn
                         "--filename" "deps.edn")))))
  (testing "report on value not a vector"
    (let [edn "{:paths scripts}"]
      (doseq [fname ["bb.edn" "deps.edn"]]
        (assert-submaps
         (list {:file fname :row 1 :col 9 :level :warning :message "Expected vector, found: symbol"})
         (lint! edn
                "--filename" fname)))))
  (testing "when container type wrong, report only on container type and not container elems"
    (let [edn "{:paths {bad 32}}"]
      (doseq [fname ["bb.edn" "deps.edn"]]
        (assert-submaps
         (list {:file fname :row 1 :col 9 :level :warning :message "Expected vector, found: map"})
         (lint! edn
                "--filename" fname))))  )
  (testing "report on each unexpected vector elem type"
    (let [deps-edn "{:paths [scripts 42 \"ok\" :alias]}"]
      (assert-submaps
       '({:file "deps.edn" :row 1 :col 10 :level :warning :message "Expected string or keyword, found: symbol"}
         {:file "deps.edn" :row 1 :col 18 :level :warning :message "Expected string or keyword, found: int"})
       (lint! deps-edn
              "--filename" "deps.edn"))
      (assert-submaps
       '({:file "bb.edn" :row 1 :col 10 :level :warning :message "Expected string, found: symbol"}
         {:file "bb.edn" :row 1 :col 18 :level :warning :message "Expected string, found: int"}
         {:file "bb.edn" :row 1 :col 26 :level :warning :message "Expected string, found: keyword"})
       (lint! deps-edn
              "--filename" "bb.edn")))))

(deftest qualified-lib-test
  (let [deps-edn '{:deps {clj-kondo {:mvn/version "2020.10.10"}}
                   :aliases {:foo {:extra-deps {clj-kondo {:mvn/version "2020.10.10"
                                                           :exclusions [cheshire]}}}}}
        deps-edn (binding [*print-namespace-maps* false] (str deps-edn))]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 9, :level :warning, :message "Libs must be qualified, change clj-kondo => clj-kondo/clj-kondo"}
       {:file "deps.edn", :row 1, :col 78, :level :warning, :message "Libs must be qualified, change clj-kondo => clj-kondo/clj-kondo"}
       {:file "deps.edn", :row 1, :col 129, :level :warning, :message "Libs must be qualified, change cheshire => cheshire/cheshire"})
     (lint! deps-edn
            "--filename" "deps.edn"))))

(deftest coordinate-map-test
  (let [deps-edn '{:deps {foobar/bar "2020.20"}
                   :aliases {:foo {:extra-deps {foobar/baz "2020.20"}}}}
        deps-edn (binding [*print-namespace-maps* false] (str deps-edn))]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 20, :level :warning, :message "Expected map, found: string"}
       {:file "deps.edn", :row 1, :col 72, :level :warning, :message "Expected map, found: string"})
     (lint! (str deps-edn)
            "--filename" "deps.edn"))))

(deftest coordinate-required-key-test
  (let [deps-edn '{:deps {foobar/bar {:mvn/release "2020.20"}}
                   :aliases {:foo {:extra-deps {foo/bar1 {:git/url "..."
                                                          :git/tag "..."}}}}}
        deps-edn (binding [*print-namespace-maps* false] (str deps-edn))]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 20, :level :warning, :message "Missing required key: :mvn/version, :git/url or :local/root."}
       {:file "deps.edn", :row 1, :col 85, :level :warning, :message "Missing required key :git/sha."})
     (lint! (str deps-edn)
            "--filename" "deps.edn"))))

(deftest git-coordinates-required-key-test
  (let [deps-edn '{:deps {foo/bar1 {:git/url "..."
                                    :git/sha "..."}
                          foo/bar2 {:git/url "..."
                                    :git/tag "..."}
                          foo/bar3 {:git/url "..."
                                    :git/tag "..."
                                    :git/sha "..."}}}
        deps-edn (binding [*print-namespace-maps* false] (str deps-edn))]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 61, :level :warning, :message "Missing required key :git/sha."})
     (lint! (str deps-edn)
            "--filename" "deps.edn")))
  (let [deps-edn '{:deps {io.github.cognitect-labs/test-runner
                          {:git/tag "v0.4.0" :git/sha "334f2e2"}}}]
    (is (empty? (lint! (binding [*print-namespace-maps* false] (str deps-edn)) "--filename" "deps.edn"))))
  (let [deps-edn '{:deps {io.github.nextjournal/clerk {:git/url "git@github.com:nextjournal/clerk.git"
                                                       :sha "e7bb8de0e5322e3029a3843ed60139d5ce1c95ca"}}}]
    (is (empty? (lint! (binding [*print-namespace-maps* false] (str deps-edn)) "--filename" "deps.edn")))))

(deftest git-coordinates-conflicting-keys-test
  (let [deps-edn '{:deps {foo/bar1 {:git/url "..."
                                    :git/sha "..."
                                    :sha     "..."}
                          foo/bar2 {:git/url "..."
                                    :git/tag "..."
                                    :tag     "..."
                                    :sha     "..."}}}
        deps-edn (binding [*print-namespace-maps* false] (str deps-edn))]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 18, :level :warning, :message "Conflicting keys :git/sha and :sha."}
       {:file "deps.edn", :row 1, :col 73, :level :warning, :message "Conflicting keys :git/tag and :tag."})
     (lint! (str deps-edn)
            "--filename" "deps.edn"))))

(deftest non-deterministic-version-test
  (let [deps-edn '{:deps {foobar/bar {:mvn/version "RELEASE"}}
                   :aliases {:foo {:extra-deps {foo/bar1 {:mvn/version "LATEST"}}}}}
        deps-edn (binding [*print-namespace-maps* false] (str deps-edn))]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 20, :level :warning, :message "Non-deterministic version."}
       {:file "deps.edn", :row 1, :col 85, :level :warning, :message "Non-deterministic version."})
     (lint! (str deps-edn)
            "--filename" "deps.edn"))))

(deftest spot-check-deps-linting-enabled-for-bb-edn-test
  (let [bb-edn '{:deps {clj-kondo {:mvn/version "LATEST"}
                        foo/baz1 "OOPS"}}]
    (assert-submaps
     '({:file "bb.edn", :row 1, :col 9, :level :warning, :message "Libs must be qualified, change clj-kondo => clj-kondo/clj-kondo"}
       {:file "bb.edn", :row 1, :col 19, :level :warning, :message "Non-deterministic version."}
       {:file "bb.edn", :row 1, :col 54, :level :warning, :message "Expected map, found: string"})
     (lint! (str bb-edn)
            "--filename" "bb.edn"))))

(deftest alias-keyword-names-test
  (let [deps-edn '{:aliases {foo {:extra-deps {foo/bar1 {:mvn/version "..."}}}}}
        deps-edn (binding [*print-namespace-maps* false] (str deps-edn))]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 12, :level :warning, :message "Prefer keyword for alias."})
     (lint! (str deps-edn)
            "--filename" "deps.edn"))))

(deftest suspicious-alias-name-test
  (let [deps-edn '{:aliases {:deps {foo/bar1 {:mvn/version "..."}}
                             :extra-deps {foo/bar1 {:mvn/version "..."}}}}
        deps-edn (binding [*print-namespace-maps* false] (str deps-edn))]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 12, :level :warning, :message "Suspicious alias name: deps"}
       {:file "deps.edn", :row 1, :col 51, :level :warning, :message "Suspicious alias name: extra-deps"})
     (lint! (str deps-edn)
            "--filename" "deps.edn"))))

(deftest jvm-opts-test
  (let [deps-edn '{:aliases {:jvm {:jvm-opts "-Dfoobar"}}}
        deps-edn (binding [*print-namespace-maps* false] (str deps-edn))]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 28, :level :warning, :message "JVM opts should be seqable of strings."})
     (lint! (str deps-edn)
            "--filename" "deps.edn"))))

(deftest mvn-repos-test
  (let [deps-edn '{:mvn/repos {"foo" "bar"
                               "baz" {:link "..."}}}
        deps-edn (binding [*print-namespace-maps* false] (str deps-edn))]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 20, :level :warning, :message "Expected: map with :url."}
       {:file "deps.edn", :row 1, :col 33, :level :warning, :message "Expected: map with :url."})
     (lint! (str deps-edn)
            "--filename" "deps.edn"))))

(deftest namespaced-map-test
  (let [deps-edn "{:deps #:clj-kondo{clj-kondo {:mvn/version \"2020.11.07\"}}}"]
    (is (empty?
         (lint! (str deps-edn)
                "--filename" "deps.edn")))))

(deftest ignore-test
  (let [deps-edn "{:deps #_:clj-kondo/ignore {clj-kondo {:mvn/version \"2020.11.07\"}}}"]
    (is (empty? (lint! (str deps-edn)
                       "--filename" "deps.edn")))))

(deftest inferred-git-urls-test
  (testing "valid inferred git dep urls produce no lint warnings"
    (let [deps-edn '{:deps {com.github.somebody/a-project {:git/sha "..."}
                            io.bitbucket.user/other-project {:git/sha "..."}
                            ht.sr.person/third-project {:git/sha "..."}}}]
      (is (empty? (lint! (str deps-edn) "--filename" "deps.edn")))))
  (testing "invalid inferred git dep urls produce a lint warning"
    (let [deps-edn '{:deps {invalid.url/project {:git/sha "..."}}}]
      (assert-submaps
       '({:file "deps.edn", :row 1, :col 30, :level :warning, :message "Missing required key: :mvn/version, :git/url or :local/root."})
       (lint! (str deps-edn)
              "--filename" "deps.edn")))))

(deftest depend-on-undefined-task-test
  (let [bb-edn '{:tasks
                 {run {:paths ["script"]
                       :depends [compile]
                       :task (call/fn)}
                  cleanup {:depends [run]
                           :paths ["script"]}
                  init (println "init")}}]
    (assert-submaps
     '({:file "bb.edn", :row 1, :col 44, :level :error, :message "Depending on undefined task: compile"})
     (lint! (str bb-edn)
            "--filename" "bb.edn"))))

(deftest cyclic-task-dependencies-test
  (let [bb-edn '{:tasks
                 {run {:paths ["script"]
                       :task (call/fn)}
                  cleanup {:depends [init]
                           :paths ["script"]}
                  init {:depends [cleanup]
                        :task (println "init")}
                  min-task (call/some-fn)
                  :enter (call/some-more)
                  }}]
    (assert-submaps
     '({:file "bb.edn", :row 1, :col 71, :level :error, :message "Cyclic task dependency: cleanup -> init -> cleanup"}
       {:file "bb.edn", :row 1, :col 114, :level :error, :message "Cyclic task dependency: init -> cleanup -> init"})
     (lint! (str bb-edn)
            "--filename" "bb.edn"))))

(deftest unexpected-key-test
  (let [bb-edn '{:requires [[babashka.fs :as fs]]
                 :tasks
                 {run {:paths ["script"]
                       :task (call/fn)}}}]
    (assert-submaps
     '({:file "bb.edn", :row 1, :col 2, :level :warning, :message "Global :requires belong in the :tasks map."})
     (lint! (str bb-edn)
            "--filename" "bb.edn"))))

(deftest missing-task-docstring-test
  (let [bb-edn '{:tasks
                 {run {:paths ["script"]
                       :task (call/fn)}
                  docd {:doc "meaningful description"
                        :task (call/docd)}
                  -hidden {:task (call/another)
                           :doc "is ignored"}
                  private {:task (call/yet-another)
                           :private true}}}]
    (assert-submaps
     '({:file "bb.edn", :row 1, :col 14, :level :warning, :message "Docstring missing for task: run"})
     (lint! (str bb-edn)
            '{:linters {:bb.edn-task-missing-docstring {:level :warning}}}
            "--filename" "bb.edn"))))

(deftest global-jvm-opts-test
  (let [deps-edn "{:jvm-opts [\"foo\"]}"]
    (assert-submaps2
     [{:file "deps.edn",
       :row 1,
       :col 12,
       :level :warning,
       :message
       "Global :jvm-opts not supported (only in aliases)"}]
     (lint! (str deps-edn)
            "--filename" "deps.edn"))))
