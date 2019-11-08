(ns repro
  (:require [clojure.test :refer [is testing]]))

(defn pretty-print
  {:test (fn []
           (testing "Default printing options"
             (let [res (as-> (repeat 1 "foo") x x)]
               (is res))))}
  [])

(defn ^{:test (fn []
                (testing "Default printing options"
                  (let [res2 (as-> (repeat 1 "foo") x x)]
                    (println "testing!")
                    (is res2))))}
  pretty-print2
  [])

((:test (meta #'pretty-print2)))
