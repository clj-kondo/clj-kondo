(ns clj-kondo.analysis.instance-invocations-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.impl.utils :refer [err]]
   [clj-kondo.test-utils :refer [assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(defn analyze
  ([paths] (analyze paths nil))
  ([paths config]
   (:analysis
    (clj-kondo/run! (merge
                     {:lint paths
                      :config {:output {:canonical-paths true}
                               :analysis {:instance-invocations true}}}
                     config)))))

(deftest invocations-test
  (assert-submaps
   [{:method-name "length", :filename "<stdin>", :name-row 1, :name-col 2, :name-end-row 1, :name-end-col 9}]
   (-> (with-in-str "(.length \"hello\")"
         (clj-kondo/run! {:lint ["-"] :config {:analysis {:instance-invocations true}}}))
       :analysis :instance-invocations)))
