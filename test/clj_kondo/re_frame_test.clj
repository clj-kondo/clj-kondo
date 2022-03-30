(ns clj-kondo.re-frame-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [lint!]]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.tools.deps.alpha :as deps]))

(deftest re-frame-athens-lint-test
  (let [deps '{:deps {com.github.athensresearch/athens {:git/sha "0866af62c00b1b026db5f7a6b8083e9c1da38385"}}
               :mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                           "clojars" {:url "https://repo.clojars.org/"}}}
        paths (->> ((deps/resolve-deps deps nil) 'com.github.athensresearch/athens)
                   :paths
                   (filter #(str/ends-with? % "src/cljs")))
        lint-result (clj-kondo/run! {:lang :cljs
                                     :lint paths
                                     :config ;; linters and lint-as as athens' clj-kondo config
                                     '{:linters {:unresolved-namespace         {:exclude [clojure.string]}
                                                 :unresolved-symbol            {:exclude [random-uuid
                                                                                          goog.DEBUG
                                                                                          (com.rpl.specter/recursive-path)]}
                                                 :unused-referred-var          {:exclude {clojure.test [is deftest testing]}}
                                                 :unsorted-required-namespaces {:level :warning}}
                                       :lint-as {day8.re-frame.tracing/fn-traced   clojure.core/fn
                                                 day8.re-frame.tracing/defn-traced clojure.core/defn
                                                 reagent.core/with-let             clojure.core/let
                                                 instaparse.core/defparser         clojure.core/def
                                                 athens.common.sentry/defntrace    clojure.core/defn}
                                       ;; trigger full re-frame analysis to test it for lint regressions
                                       :output {:analysis {:context [:re-frame.core]
                                                           :keywords true}}}})]
    (println "paths analyzed ----")
    (println (str/join ", " paths))
    (println "summary ----")
    (prn (:summary lint-result))
    (is (empty? (:findings lint-result)))))

(deftest subscribe-arguments-are-used-test
  (is (empty? (lint! "
(require '[re-frame.core :as rf])

(defn show-id [subscription]     ;; <------ this parameter
  (let [id @(rf/subscribe subscription)]     ;; <------ is here used
    [:h4 id]))"
                  '{:linters {:unused-binding {:level :error}}}))))
