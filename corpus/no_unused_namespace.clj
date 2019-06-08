(ns no_unused-namespace
  (:require [clojure.string :as str]))

(let [{score (if (str/starts-with? "foo" "f")
               :starts-with :not-starts-with)}
      {:starts-with 100
       :not-starts-with 0}]
  score) ;;=> 100
