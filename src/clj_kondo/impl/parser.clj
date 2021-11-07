(ns clj-kondo.impl.parser
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :as utils :refer [parse-string-all]]))

(defn parse-string [s]
  (parse-string-all s))

;;;; Scratch

(comment
  (parse-string "(+ 1 2 3) #_1 2 ;; no")
  (parse-string "123 : 1")
  (parse-string "123 foo/")
  (:meta (utils/parse-string "^{:a 1} [1 2 3]"))
  (:meta (utils/parse-string "^{:a 1} ^{:b 1} [1 2 3]"))
  (:meta (utils/parse-string "^{:a 1} ^{:b 1} [1 2 3]"))
  )
