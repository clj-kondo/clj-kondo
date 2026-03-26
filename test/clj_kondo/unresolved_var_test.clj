(ns clj-kondo.unresolved-var-test
  (:require
   [babashka.fs :as fs]
   [clj-kondo.core :as core]
   [clj-kondo.test-utils :refer [lint! assert-submaps2] :rename {assert-submaps2 assert-submaps}]
   [clojure.test :refer [deftest is testing]]
   [clojure.tools.deps :as deps]))

(deftest unresolved-var-test
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 35, :level :error, :message "Unresolved var: set/onion"})
   (lint! "(require '[clojure.set :as set]) (set/onion) set/union"
          '{:linters {:unresolved-symbol {:level :error}
                      :unresolved-var {:level :error}}}))
  (assert-submaps
    '({:file "<stdin>", :row 1, :col 35, :level :error, :message "Unresolved var: set/onion"}
      {:file "<stdin>", :row 1, :col 47, :level :error, :message "Unresolved var: set/onion"} )
    (lint! "(require '[clojure.set :as set]) (set/onion) (set/onion) set/union"
           '{:linters {:unresolved-symbol {:level :error}
                       :unresolved-var {:level :error :report-duplicates true}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 41, :level :error, :message "Unresolved var: set/onion"})
   (lint! "(require '[clojure.set :as set]) (apply set/onion 1 2 3)"
          '{:linters {:unresolved-symbol {:level :error}
                      :unresolved-var {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 48, :level :error, :message "Unresolved var: foo/bar"})
   (lint! "(ns foo) (defn foo []) (ns bar (:require foo)) foo/bar"
          '{:linters {:unresolved-symbol {:level :error}
                      :unresolved-var {:level :error}}}))
  (assert-submaps
   '({:file "<stdin>", :row 1, :col 2, :level :error, :message "Unresolved var: clojure.core/x"})
   (lint! "(clojure.core/x 1 2 3)"
          '{:linters {:unresolved-symbol {:level :error}
                      :unresolved-var {:level :error}}}))
  (testing "vars from unknown namespaces are ignored"
    (is (empty?
         (lint! "(ns bar (:require foo)) foo/bar"
                '{:linters {:unresolved-symbol {:level :error}
                            :unresolved-var {:level :error}}}))))
  (is (empty?
       (lint! "(do (prn :foo) (prn :bar)) goog.global"
              '{:linters {:unresolved-symbol {:level :error}
                          :unresolved-var {:level :error}}}
              "--lang" "cljs")))
  (is (empty?
       (lint! "(cljs.core/PersistentVector. nil 10 5)"
              '{:linters {:unresolved-symbol {:level :error}
                          :unresolved-var {:level :error}}}
              "--lang" "cljs")))
  (let [prog "
(ns foo)
(defmacro gen-vars [& names]) (gen-vars x y z)

(ns bar)
(defmacro gen-vars [& names]) (gen-vars x y z)

(ns baz (:require foo bar))
foo/x (foo/y)
bar/x (bar/y)
"
        cfg '{:linters {:unresolved-symbol {:exclude [(foo/gen-vars)
                                                      (bar/gen-vars)]
                                            :level :error}
                        :unresolved-var {:level :error}}}]
    (assert-submaps
     '({:file "<stdin>", :row 9, :col 1, :level :error, :message "Unresolved var: foo/x"}
       {:file "<stdin>", :row 9, :col 8, :level :error, :message "Unresolved var: foo/y"}
       {:file "<stdin>", :row 10, :col 1, :level :error, :message "Unresolved var: bar/x"}
       {:file "<stdin>", :row 10, :col 8, :level :error, :message "Unresolved var: bar/y"})
     (lint! prog cfg))
    (assert-submaps
     '({:file "<stdin>", :row 10, :col 8, :level :error, :message "Unresolved var: bar/y"})
     (lint! prog (assoc-in cfg [:linters :unresolved-var :exclude] '[foo bar/x])))))

(deftest cljs-var-and-interop-test
  (is (empty? (lint! "(ns main
  (:require [cljs.nodejs :as node]))
(println node/process.env)"
                    '{:linters {:unresolved-symbol {:level :error}
                                :unresolved-var {:level :error}}}
                    "--lang" "cljs"))))

(deftest built-in-namespaces-test
  (testing "fmap is not reported but xfmap is"
    (assert-submaps
     '({:file "<stdin>", :row 2, :col 22, :level :error, :message "Unresolved var: gen/xfmap"})
     (lint! "(require #?(:clj '[clojure.spec.gen.alpha :as gen] :cljs '[clojure.spec.gen.alpha :as gen]))
            gen/fmap gen/xfmap gen/string-ascii"
            '{:linters {:unresolved-symbol {:level :error}
                        :unresolved-var {:level :error}}}
                           "--lang" "cljc")))
  (testing "clojure.core.reducers"
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 66, :level :error, :message "Unresolved var: r/mapcatz"})
     (lint! "(ns foo (:require [clojure.core.reducers :as r])) r/map r/mapcat r/mapcatz"
            '{:linters {:unresolved-symbol {:level :error}
                        :unresolved-var {:level :error}}}))))

(deftest libs-test
  (let [cache (str (fs/create-temp-dir))
        deps '{:deps {;; org.clojure/clojure {:mvn/version "1.9.0"}
                      org.clojure/core.async {:mvn/version "1.9.829-alpha2"}}
               :mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                           "clojars" {:url "https://repo.clojars.org/"}}}
        jar (-> (deps/resolve-deps deps nil)
                (get-in ['org.clojure/core.async :paths 0]))]
    (core/run! {:lint [jar] :cache-dir cache})
    (assert-submaps
     '({:file "<stdin>", :row 1, :col 39, :level :error, :message "clojure.core.async/<!! is called with 0 args but expects 1"}
       {:file "<stdin>",
        :row 1,
        :col 47,
        :level :error,
        :message
        "clojure.core.async/<! is called with 0 args but expects 1"})
     (lint! "(require '[clojure.core.async :as a]) (a/<!!) (a/<!)" {:linters {:unresolved-symbol {:level :error}
                                                                              :unresolved-var {:level :error}}}
            "--cache" cache))))

(deftest issue-2239-test
  (assert-submaps
   '({:file "corpus/issue-2239/a.clj", :row 6, :col 16, :level :error, :message "Unresolved symbol: x"}
     {:file "corpus/issue-2239/b.clj", :row 6, :col 22, :level :error, :message "Unresolved var: a/x"})
   (lint! [(fs/file "corpus" "issue-2239" "a.clj") (fs/file "corpus" "issue-2239" "b.clj")]
              {:linters {:unresolved-symbol {:level :error}
                         :unresolved-var {:level :error}}}
              "--config-dir" (fs/file "corpus" "issue-2239" ".clj-kondo"))))
