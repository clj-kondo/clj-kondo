(ns clj-kondo.re-frame-test
  (:require
   [babashka.fs :as fs]
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [lint!]]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.tools.deps.alpha :as deps]))

(def config
  '{:linters {:unresolved-namespace {:exclude [clojure.string]}
              :unresolved-symbol    {:exclude [random-uuid
                                               goog.DEBUG
                                               (com.rpl.specter/recursive-path)]}
              :unused-referred-var  {:exclude {clojure.test [is deftest testing]}}
              :redundant-call {:level :off}
              :unsorted-required-namespaces {:level :warning}}
    :lint-as {day8.re-frame.tracing/fn-traced   clojure.core/fn
              day8.re-frame.tracing/defn-traced clojure.core/defn
              reagent.core/with-let             clojure.core/let
              instaparse.core/defparser         clojure.core/def
              athens.common.sentry/defntrace    clojure.core/defn}
    ;; trigger full re-frame analysis to test it for lint regressions
    :analysis {:context [:re-frame.core]
               :keywords true}})

(deftest re-frame-athens-lint-test
  (fs/with-temp-dir [tmp {}]
    (spit (fs/file tmp "config.edn") "{:config-paths ^:replace []}")
    (let [deps '{:deps {com.github.athensresearch/athens {:git/sha "0866af62c00b1b026db5f7a6b8083e9c1da38385"}}
                 :mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                             "clojars" {:url "https://repo.clojars.org/"}}}
          paths (->> ((deps/resolve-deps deps nil) 'com.github.athensresearch/athens)
                     :paths
                     (filter #(str/ends-with? % "src/cljs")))
          lint-result (clj-kondo/run! {:config-dir (fs/file tmp)
                                       :lang :cljs
                                       :lint paths
                                       :config config ;; linters and lint-as as athens' clj-kondo config
                                       })]
      (println "paths analyzed ----")
      (println (str/join ", " paths))
      (println "summary ----")
      (prn (:summary lint-result))
      (is (empty? (:findings lint-result))))))

(deftest re-frame-analysis-lint-test
  (is (empty? (:findings
               (with-in-str
                 "
(require '[re-frame.core :as rf])

(rf/reg-event-fx
 ::setup
 (fn [_ [_ {show-key :show-key}]]
   {:fx [[:dispatch [:path show-key]]]}))"
                 (clj-kondo/run!
                  {:lang :cljs
                   :lint "-"
                   :config {:analysis {:context [:re-frame.core]
                                       :keywords true}}}))))))

(deftest re-frame-analysis-dispatch-n-w-conditionals-test
  (is (empty? (:findings
               (with-in-str
                 "
(require '[re-frame.core :as rf])

(rf/reg-event-fx
 ::setup
 (fn [_ [_ {show-key :show-key}]]
   {:fx [[:dispatch-n (cond-> [[:path show-key]]
                        true (conj [:foo])) ]]}))"
                 (clj-kondo/run!
                  {:lang :cljs
                   :lint "-"
                   :config {:analysis {:context [:re-frame.core]
                                       :keywords true}}}))))))

(deftest subscribe-arguments-are-used-test
  (is (empty? (lint! "
(require '[re-frame.core :as rf])

(defn show-id [subscription]     ;; <------ this parameter
  (let [id @(rf/subscribe subscription)]     ;; <------ is here used
    [:h4 id]))"
                     '{:linters {:unused-binding {:level :error}}}))))

(deftest npe-issue-1669-test
  (is (empty? (lint! "
(ns gakki.events
  (:require [re-frame.core :refer [reg-event-fx trim-v]]
            [gakki.util.logging :as log]))

(reg-event-fx
  :player/check-output-device
  [trim-v]
  (fn [_ _]
    (log/debug \"Default output device may have changed...\")
    {:dispatch-later [{:ms 250 :dispatch [::check-output-device]}
                      {:ms 500 :dispatch [::check-output-device]}]}))
"
                     '{:linters {:unused-binding {:level :error}}}))))


(deftest issue-1704-test
  ;; passes without the fix
  (testing "no keyword/re-frame analysis"
    (is (empty? (lint! "
(ns example
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 :foo/bar
 (fn [_ [_my-arg]]
   {:fx [[:dispatch [:foo]]]}))"
                       '{:linters {:unused-binding {:level :error}}}))))
  (testing "with re-frame and keyword analysis on"
    (is (empty? (:findings
                 (with-in-str
                   "
(ns example
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 :foo/bar
 (fn [_ [my-arg]]
   {:fx [[:dispatch [my-arg]]]}))"
                   (clj-kondo/run!
                    {:lang :cljs
                     :lint "-"
                     :config {:analysis {:context [:re-frame.core]
                                         :keywords true}}})))))))
