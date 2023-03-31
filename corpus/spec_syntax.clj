(ns spec-syntax
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]))

(s/fdef my-inc ;; symbol isn't reported as unresolved
  :args (s/cat :x int?))

(s/fdef 1 :args) ;; expected symbol, missing value for :args

(s/fdef foo :xargs (s/cat :x int?))

(defn my-inc [x]
  (+ 1 x))

;; this takes care of "using" clojure.string, so it's not reported as unused anymore
(s/fdef str/starts-with? :args (s/cat :s string?
                                      :substr string?))

(s/fdef xstr/starts-with? ,,,) ;; unresolved symbol, but should not give warning by default, #1532

(s/def ::a any?)
(s/def ::bar (s/keys
              ;; fine
              :opt [::a]
              :opt-un [::a]
              :req [::a]
              :req-un [::a]
              :gen (fn [])
              ;; unknown
              ::opt [::a]
              ::opt-un [::a]
              ::req [::a]
              ::req-un [::a]
              ::gen (fn [])))

(require '[spec-keys :as sk]) ;; namespace is used because of below s/keys call

(s/keys :req [::sk/my-key])

(s/def foobar int?) ;; no unresolved-symbol
