(ns clj-kondo.re-frame-test
  (:require
   [clj-kondo.test-utils :refer [lint!]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest subscribe-arguments-are-used-test
  (is (empty? (lint! "
(require '[re-frame.core :as rf])

(defn show-id [subscription]     ;; <------ this parameter
  (let [id @(rf/subscribe subscription)]     ;; <------ is here used
    [:h4 id]))"
                  '{:linters {:unused-binding {:level :error}}}))))
