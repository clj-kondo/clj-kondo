(ns clj-kondo.deps-edn-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps]]
            [clojure.test :refer [deftest testing is]]))

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
     '({:file "deps.edn", :row 1, :col 20, :level :warning, :message "Expected map, found: java.lang.String"}
       {:file "deps.edn", :row 1, :col 72, :level :warning, :message "Expected map, found: java.lang.String"})
     (lint! (str deps-edn)
            "--filename" "deps.edn"))))

(deftest coordinate-required-key-test
  (let [deps-edn '{:deps {foobar/bar {:mvn/release "2020.20"}}
                   :aliases {:foo {:extra-deps {foo/bar1 {:git/url "..."
                                                          :git/sha "..."}}}}}
        deps-edn (binding [*print-namespace-maps* false] (str deps-edn))]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 20, :level :warning, :message "Missing required key: :mvn/version, :git/url or :local/root."}
       {:file "deps.edn", :row 1, :col 85, :level :warning, :message "Missing required key :sha."}
       {:file "deps.edn", :row 1, :col 85, :level :warning, :message "Missing required key: :mvn/version, :git/url or :local/root."})
     (lint! (str deps-edn)
            "--filename" "deps.edn"))))

(deftest non-deterministic-version-test
  (let [deps-edn '{:deps {foobar/bar {:mvn/version "RELEASE"}}
                   :aliases {:foo {:extra-deps {foo/bar1 {:mvn/version "LATEST"}}}}}
        deps-edn (binding [*print-namespace-maps* false] (str deps-edn))]
    (assert-submaps
     '({:file "deps.edn", :row 1, :col 20, :level :warning, :message "Non-determistic version."}
       {:file "deps.edn", :row 1, :col 85, :level :warning, :message "Non-determistic version."})
     (lint! (str deps-edn)
            "--filename" "deps.edn"))))

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
