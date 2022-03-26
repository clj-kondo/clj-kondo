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
                      :config {:output {:canonical-paths true
                                        :analysis {:java-class-definitions true
                                                   :java-class-usages true}}}}
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
     java-class-definitions)))

(deftest class-usages-test
  (let [{:keys [:java-class-usages]} (analyze ["corpus/java/usages.clj"])]
    (assert-submaps
     [{:class "java.lang.Exception"
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
       :name-row 6 :name-col 2 :name-end-row 6 :name-end-col 9}]
     java-class-usages)))

(comment

  #_(assert-submap {:filename #"\.class"} {:filename "/Users/borkdude/.m2/repository/org/clojure/clojure/1.10.3/clojure-1.10.3.jar:clojure/lang/RT.class"})
  #_(:filename rt-def)
  )
