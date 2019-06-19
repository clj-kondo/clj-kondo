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

(s/fdef xstr/starts-with? ,,,) ;; unresolved symbol
