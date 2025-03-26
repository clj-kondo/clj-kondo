(ns clj-kondo.analysis.java-test
  (:require
   [babashka.process :as p]
   [borkdude.deflet :as deflet]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.utils :refer [err]]
   [clj-kondo.test-utils :as tu :refer [assert-submap2 assert-submaps2]]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.tools.deps.alpha :as deps]))

(defn analyze [lint]
  (let [config {:output {:canonical-paths true
                         :format :edn}
                :analysis {:java-class-definitions true
                           :java-class-usages true
                           :java-member-definitions true}}]
    (if tu/native?
      (-> (p/sh "./clj-kondo" "--config" (pr-str config) "--lint" (str/join " " lint))
          :out
          edn/read-string
          :analysis)
      (:analysis
       (clj-kondo/run! {:lint lint
                        :config config})))))

(deftest jar-classes-test
  (let [deps '{:deps {org.clojure/clojure {:mvn/version "1.10.3"}}
               :mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                           "clojars" {:url "https://repo.clojars.org/"}}}
        jar (-> (deps/resolve-deps deps nil)
                (get-in ['org.clojure/clojure :paths 0]))
        {:keys [java-class-definitions java-class-usages java-member-definitions]} (analyze [jar])
        rt-def (some #(when (= "clojure.lang.RT" (:class %))
                        %) java-class-definitions)
        rt-usage (some #(when (= "clojure.lang.RT" (:class %))
                          %) java-class-usages)
        keys-rt-member-def (some #(when (and (= "clojure.lang.RT" (:class %) )
                                             (= "keys" (:name %)))
                                    %) java-member-definitions)]

    (assert-submap2
     {:class "clojure.lang.RT",
      :uri #"jar:file:.*/org/clojure/clojure/1.10.3/clojure-1.10.3.jar!/clojure/lang/RT.class",
      :filename #"\.class"}
     rt-def)
    (assert-submap2
     {:class "clojure.lang.RT",
      :uri #"jar:file:.*\.clj",
      :filename #".*\.clj"}
     rt-usage)
    (assert-submap2
     {:class "clojure.lang.RT"
      :uri #"jar:file:.*/org/clojure/clojure/1.10.3/clojure-1.10.3.jar!/clojure/lang/RT.class"
      :name "keys"
      :parameter-types ["java.lang.Object"]
      :flags #{:method :public :static}
      :return-type "clojure.lang.ISeq"}
     keys-rt-member-def)
    (is (every? number? ((juxt :row
                               :col
                               :end-row
                               :end-col) rt-usage)))))

#_(jar-classes-test)
#_(analyze ["/Users/borkdude/.m2/repository/org/clojure/clojure/1.10.3/clojure-1.10.3.jar"])


(deftest local-classes-test
  (let [{:keys [java-class-definitions java-member-definitions]} (analyze ["corpus/java/classes"])]
    (assert-submaps2
     '[{:class "foo.bar.AwesomeClass",
        :uri #"file:.*/corpus/java/classes/foo/bar/AwesomeClass.class",
        :filename #".*corpus/java/classes/foo/bar/AwesomeClass.class"}]
     java-class-definitions)
    (assert-submaps2
     '[{:class "foo.bar.AwesomeClass",
        :uri #"file:.*/corpus/java/classes/foo/bar/AwesomeClass.class"
        :name "bar1"
        :flags #{:public :field}
        :type "java.lang.Double"}
       {:class "foo.bar.AwesomeClass",
        :uri #"file:.*/corpus/java/classes/foo/bar/AwesomeClass.class"
        :name "bar2"
        :flags #{:public :field :final}
        :type "java.lang.Double"}
       {:class "foo.bar.AwesomeClass",
        :uri #"file:.*/corpus/java/classes/foo/bar/AwesomeClass.class"
        :name "bar3"
        :flags #{:public :field :static :final}
        :type "java.lang.Double"}
       {:class "foo.bar.AwesomeClass",
        :uri #"file:.*/corpus/java/classes/foo/bar/AwesomeClass.class"
        :name "<init>"
        :flags #{:public :method}
        :parameter-types ["double"]
        :return-type "void"}
       {:class "foo.bar.AwesomeClass",
        :uri #"file:.*/corpus/java/classes/foo/bar/AwesomeClass.class"
        :name "coolSum1"
        :flags #{:public :method}
        :parameter-types ["double" "double"]
        :return-type "int"}
       {:class "foo.bar.AwesomeClass",
        :uri #"file:.*/corpus/java/classes/foo/bar/AwesomeClass.class"
        :name "coolParse"
        :flags #{:public :static :method}
        :parameter-types ["java.util.List"]
        :return-type "java.io.File[]"}]
     java-member-definitions))
  (let [{:keys [java-class-definitions]} (analyze ["corpus/java/sources"])]
    (assert-submaps2
     '[{:class "foo.bar.AwesomeClass",
        :uri #"file:.*/corpus/java/sources/foo/bar/AwesomeClass.java",
        :filename #".*corpus/java/sources/foo/bar/AwesomeClass.java"}]
     java-class-definitions))
  (testing "linting just one java source"
    (let [{:keys [java-class-definitions java-member-definitions]} (analyze ["corpus/java/sources/foo/bar/AwesomeClass.java"])]
      (assert-submaps2
       '[{:class "foo.bar.AwesomeClass",
          :uri #"file:.*/corpus/java/sources/foo/bar/AwesomeClass.java",
          :filename #".*corpus/java/sources/foo/bar/AwesomeClass.java"}]
       java-class-definitions)
      (assert-submaps2
       (cond->>
           '[{:class "foo.bar.AwesomeClass"
              :uri #"file:.*/corpus/java/sources/foo/bar/AwesomeClass.java"
              :flags #{:public :field}
              :name "bar1"
              :type "Double"
              :row 15 :col 5 :end-row 15 :end-col 23}
             {:class "foo.bar.AwesomeClass"
              :uri #"file:.*/corpus/java/sources/foo/bar/AwesomeClass.java"
              :flags #{:public :field :final}
              :name "bar2"
              :type "Double"
              :row 16 :col 5 :end-row 16 :end-col 35}
             {:class "foo.bar.AwesomeClass"
              :uri #"file:.*/corpus/java/sources/foo/bar/AwesomeClass.java"
              :flags #{:public :static :field :final}
              :name "bar3"
              :type "Double"
              :row 17 :col 5 :end-row 17 :end-col 42}
             {:class "foo.bar.AwesomeClass"
              :uri #"file:.*/corpus/java/sources/foo/bar/AwesomeClass.java"
              :flags #{:method :public}
              :name "AwesomeClass"
              :parameters ["double a"]
              :row 19 :col 5 :end-row 21 :end-col 5}
             {:return-type "int"
              :name "coolSum1"
              :class "foo.bar.AwesomeClass"
              :uri #"file:.*/corpus/java/sources/foo/bar/AwesomeClass.java"
              :flags #{:method :public}
              :parameters ["double a" "double b"]
              :row 23 :col 5 :end-row 29 :end-col 5}
             {:return-type "File[]"
              :name "coolParse"
              :class "foo.bar.AwesomeClass"
              :uri #"file:.*/corpus/java/sources/foo/bar/AwesomeClass.java"
              :flags #{:method :public :static}
              :doc "/*\n     * Some cool doc\n     * @param filenames\n     * @return list of files\n     */"
              :parameters ["List<String> filenames"]
              :row 36 :end-row 38 :col 5 :end-col 5}
             {:return-type "Foo"
              :name "foo"
              :class "foo.bar.AwesomeClass"
              :uri #"file:.*/corpus/java/sources/foo/bar/AwesomeClass.java"
              :flags #{:method :public}
              :parameters []
              :row 40 :end-row 45 :col 5 :end-col 5}]
         tu/windows? (mapv (fn [m]
                             (if (:doc m)
                               (update m :doc #(str/replace % "\n" "\r\n"))
                               m))))
       java-member-definitions))))

(deftest class-usages-test
  (let [{:keys [:java-class-usages :var-usages]} (analyze ["corpus/java/usages.clj"])]
    (is (= '(try fn import) (map :name var-usages)))
    (assert-submaps2
     [{:class "clojure.lang.PersistentVector", :uri #"file:.*corpus/java/usages.clj",
       :filename #"corpus/java/usages.clj", :row 1, :col 40, :end-row 1, :end-col 56
       :import true}
      {:class "java.lang.Exception"
       :uri #"file:.*corpus/java/usages.clj"
       :filename #"corpus/java/usages.clj"
       :row 3 :col 13 :end-row 3 :end-col 22
       :name-row 3 :name-col 13 :name-end-row 3 :name-end-col 22}
      {:class "java.lang.Thread"
       :method-name "sleep"
       :uri #"file:.*corpus/java/usages.clj"
       :filename #"corpus/java/usages.clj"
       :row 4 :col 1 :end-row 4 :end-col 13
       :name-row 4 :name-col 1 :name-end-row 4 :name-end-col 13}
      {:class "java.lang.Thread"
       :uri #"file:.*corpus/java/usages.clj"
       :method-name "sleep"
       :filename #"corpus/java/usages.clj"
       :row 5 :col 1 :end-row 5 :end-col 19
       :name-row 5 :name-col 2 :name-end-row 5 :name-end-col 14}
      {:class "java.lang.Thread"
       :uri #"file:.*corpus/java/usages.clj"
       :filename #"corpus/java/usages.clj"
       :row 6 :col 1 :end-row 6 :end-col 18
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
       :method-name "specials"
       :filename #"corpus/java/usages.clj"
       :end-col 18, :row 10}
      {:class "foo.bar.Baz",
       :uri #"file:.*corpus/java/usages.clj",
       :filename #"corpus/java/usages.clj", :row 11, :col 1, :end-row 11, :end-col 12}
      {:class "foo.bar.Baz", :uri #"file:.*corpus/java/usages.clj",
       :method-name "EMPTY"
       :filename #"corpus/java/usages.clj", :row 12, :col 1, :end-row 12, :end-col 18}
      {:class "java.util.Date", :uri #"file:.*corpus/java/usages.clj",
       :filename #"corpus/java/usages.clj", :row 13, :col 1, :end-row 13, :end-col 15}
      {:class "java.io.File", :uri #"file:.*corpus/java/usages.clj",
       :method-name "createTempFile"
       :filename #"corpus/java/usages.clj", :row 14, :col 1, :end-row 14, :end-col 42
       :name-col 2 :name-end-col 29, :name-end-row 14, :name-row 14}
      {:class "java.io.File",
       :filename #"corpus/java/usages.clj"
       :uri #"file:.*corpus/java/usages.clj"
       :row 15,
       :col 1,
       :end-row 15,
       :end-col 22
       :name-row 15
       :name-col 2}
      {:end-row 16,
       :name-end-col 19,
       :name-end-row 16,
       :name-row 16,
       :col 1,
       :class "java.lang.String",
       :name-col 2,
       :uri #"file:.*corpus/java/usages.clj"
       :end-col 26,
       :row 16}]
     java-class-usages)))

(deftest issue-2288-test
  (deflet/deflet
    (def deps '{:deps {com.google.cloud/google-cloud-vision {:mvn/version "3.32.0"}}
                :mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                            "clojars" {:url "https://repo.clojars.org/"}}})
    (def jar (-> (deps/resolve-deps deps nil)
                 (get-in ['com.google.cloud/google-cloud-vision :paths 0])))

    (def ana (analyze [jar]))
    (def create-meth (some #(when (and (= "com.google.cloud.vision.v1.ImageAnnotatorClient" (:class %) )
                                       (= "create" (:name %)))
                         %) (:java-member-definitions ana)))
    (assert-submaps2
     #{:method :public :static :final}
     (:flags create-meth))))

(comment

  #_(assert-submap {:filename #"\.class"} {:filename "/Users/borkdude/.m2/repository/org/clojure/clojure/1.10.3/clojure-1.10.3.jar:clojure/lang/RT.class"})
  #_(:filename rt-def))
