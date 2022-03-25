(ns clj-kondo.analysis.java-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.utils :refer [err]]
   #_[clj-kondo.test-utils :refer [assert-submap assert-submaps]]
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

(deftest unresolved-var-test
  (let [deps '{:deps {org.clojure/clojure {:mvn/version "1.10.3"}}
               :mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                           "clojars" {:url "https://repo.clojars.org/"}}}
        jar (-> (deps/resolve-deps deps nil)
                (get-in ['org.clojure/clojure :paths 0]))
        _ (def j jar)
        {:keys [:java-class-definitions :java-class-usages]} (analyze [jar])]
    (is (contains? (set java-class-definitions)
                   {:class "clojure.lang.PersistentVector",
                    :uri
                    "jar:file:/Users/borkdude/.m2/repository/org/clojure/clojure/1.10.3/clojure-1.10.3.jar!/clojure/lang/PersistentVector.class"}))
    (def x java-class-usages)))

(comment
x
  )
