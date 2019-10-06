(ns clj-kondo.impl.parser
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :as utils :refer [parse-string-all]]
   [clj-kondo.impl.profiler :refer [profile]]))

(defn parse-string [s]
  (let [parsed (profile :parse-string-all (parse-string-all s))]
    parsed))

;;;; Scratch

(comment
  (parse-string "(+ 1 2 3) #_1 2 ;; no")
  (:meta (utils/parse-string "^{:a 1} [1 2 3]"))
  (:meta (utils/parse-string "^{:a 1} ^{:b 1} [1 2 3]"))
  (:meta (utils/parse-string "^{:a 1} ^{:b 1} [1 2 3]"))
  )
