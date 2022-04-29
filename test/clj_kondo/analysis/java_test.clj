(ns clj-kondo.analysis.java-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.utils :refer [err]]
   [clj-kondo.test-utils :refer [assert-submap assert-submaps]]
   #_[clojure.edn :as edn]
   #_[clojure.string :as string]
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.tools.deps.alpha :as deps]))

(defn analyze
  ([paths] (analyze paths nil))
  ([paths config]
   (:analysis
    (clj-kondo/run! (merge
                     {:lint paths
                      :config {:output {:canonical-paths true}
                               :analysis {:java-class-definitions true
                                          :java-class-usages true}}}
                     config)))))

(deftest jar-classes-test
  (let [deps '{:deps {org.clojure/clojure {:mvn/version "1.10.3"}}
               :mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                           "clojars" {:url "https://repo.clojars.org/"}}}
        jar (-> (deps/resolve-deps deps nil)
                (get-in ['org.clojure/clojure :paths 0]))
        {:keys [:java-class-definitions :java-class-usages]} (analyze [jar])
        rt-def (some #(when (= (:class %) "clojure.lang.RT")
                        %) java-class-definitions)
        rt-usage (some #(when (= (:class %) "clojure.lang.RT")
                          %) java-class-usages)]
    (assert-submap
     {:class "clojure.lang.RT",
      :uri #"jar:file:.*/org/clojure/clojure/1.10.3/clojure-1.10.3.jar!/clojure/lang/RT.class",
      :filename #"\.class"}
     rt-def)
    (assert-submap
     {:class "clojure.lang.RT",
      :uri #"jar:file:.*\.clj",
      :filename #".*\.clj"}
     rt-usage)
    (is (every? number? ((juxt :row
                               :col
                               :end-row
                               :end-col) rt-usage)))))

(deftest local-classes-test
  (let [{:keys [:java-class-definitions]} (analyze ["corpus/java/classes"])]
    (assert-submaps
     '[{:class "foo.bar.AwesomeClass",
        :uri #"file:.*/corpus/java/classes/foo/bar/AwesomeClass.class",
        :filename #".*corpus/java/classes/foo/bar/AwesomeClass.class"}]
     java-class-definitions))
  (let [{:keys [:java-class-definitions]} (analyze ["corpus/java/sources"])]
    (assert-submaps
     '[{:class "foo.bar.AwesomeClass",
        :uri #"file:.*/corpus/java/sources/foo/bar/AwesomeClass.java",
        :filename #".*corpus/java/sources/foo/bar/AwesomeClass.java"}]
     java-class-definitions))
  (testing "linting just one java source"
    (let [{:keys [:java-class-definitions]} (analyze ["corpus/java/sources/foo/bar/AwesomeClass.java"])]
      (assert-submaps
       '[{:class "foo.bar.AwesomeClass",
          :uri #"file:.*/corpus/java/sources/foo/bar/AwesomeClass.java",
          :filename #".*corpus/java/sources/foo/bar/AwesomeClass.java"}]
       java-class-definitions))))

(deftest class-usages-test
  (let [{:keys [:java-class-usages :var-usages]} (analyze ["corpus/java/usages.clj"])]
    (is (= '(try fn new import) (map :name var-usages)))
    (assert-submaps
     [{:class "clojure.lang.PersistentVector", :uri #"file:.*corpus/java/usages.clj",
       :filename #"corpus/java/usages.clj", :row 1, :col 40, :end-row 1, :end-col 56
       :import true}
      {:class "java.lang.Exception"
       :uri #"file:.*corpus/java/usages.clj"
       :filename #"corpus/java/usages.clj"
       :row 3 :col 13 :end-row 3 :end-col 22
       :name-row 3 :name-col 13 :name-end-row 3 :name-end-col 22}
      {:class "java.lang.Thread"
       :uri #"file:.*corpus/java/usages.clj"
       :filename #"corpus/java/usages.clj"
       :row 4 :col 1 :end-row 4 :end-col 13
       :name-row 4 :name-col 1 :name-end-row 4 :name-end-col 13}
      {:class "java.lang.Thread"
       :uri #"file:.*corpus/java/usages.clj"
       :filename #"corpus/java/usages.clj"
       :row 5 :col 1 :end-row 5 :end-col 19
       :name-row 5 :name-col 2 :name-end-row 5 :name-end-col 14}
      {:class "java.lang.Thread"
       :uri #"file:.*corpus/java/usages.clj"
       :filename #"corpus/java/usages.clj"
       :row 6 :col 2 :end-row 6 :end-col 9
       :name-row 6 :name-col 2 :name-end-row 6 :name-end-col 9}
      {:end-row 7, :name-end-col 17, :name-end-row 7, :name-row 7,
       :filename #"corpus/java/usages.clj", :col 1,
       :class "clojure.lang.PersistentVector", :name-col 1,
       :uri #"file:.*corpus/java/usages.clj", :end-col 17, :row 7}
      {:class "clojure.lang.Compiler", :uri #"file:.*corpus/java/usages.clj",
       :filename #"corpus/java/usages.clj",
       :row 9, :col 24, :end-row 9, :end-col 32, :import true}
      {:end-row 10, :name-end-col 18, :name-end-row 10, :name-row 10,
       :uri #"file:.*corpus/java/usages.clj", :col 1, :class "clojure.lang.Compiler", :name-col 1,
       :filename #"corpus/java/usages.clj"
       :end-col 18, :row 10}
      {:class "foo.bar.Baz",
       :uri #"file:.*corpus/java/usages.clj",
       :filename #"corpus/java/usages.clj", :row 11, :col 1, :end-row 11, :end-col 12}
      {:class "foo.bar.Baz", :uri #"file:.*corpus/java/usages.clj",
       :filename #"corpus/java/usages.clj", :row 12, :col 1, :end-row 12, :end-col 18}
      {:class "java.util.Date", :uri #"file:.*corpus/java/usages.clj",
       :filename #"corpus/java/usages.clj", :row 13, :col 1, :end-row 13, :end-col 15}
      {:class "java.io.File", :uri #"file:.*corpus/java/usages.clj",
       :filename #"corpus/java/usages.clj", :row 14, :col 1, :end-row 14, :end-col 42}]
     java-class-usages)))

(comment

  #_(assert-submap {:filename #"\.class"} {:filename "/Users/borkdude/.m2/repository/org/clojure/clojure/1.10.3/clojure-1.10.3.jar:clojure/lang/RT.class"})
  #_(:filename rt-def)
  )
