(ns clj-kondo.analysis.edn-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.utils :refer [err]]
   [clj-kondo.test-utils :refer [assert-submap2]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]))

(defn analyze
  ([paths] (analyze paths nil))
  ([paths config]
   (clj-kondo/run! (merge
                     {:lint paths
                      :config {:output {:canonical-paths true}
                               :analysis {:keywords true}}}
                     config))))

(deftest edn-keyword-analysis-test
  (let [file (io/file "corpus" "edn" "analysis.edn")
        {:keys [analysis findings]} (analyze [file] {})]
    (is (empty? findings))
    (assert-submap2
     {:keywords
      [{:name "a" :row 1 :col 2 :end-row 1 :end-col 4 :filename (.getCanonicalPath file)}
       {:name "b" :row 1 :col 7 :end-row 1 :end-col 9 :filename (.getCanonicalPath file)}
       {:name "foo" :row 1 :col 10 :end-row 1 :end-col 14 :filename (.getCanonicalPath file)}
       {:name "c" :row 1 :col 15 :end-row 1 :end-col 17 :filename (.getCanonicalPath file)}]}
     analysis)))
