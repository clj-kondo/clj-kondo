(ns clj-kondo.impl.parser
  {:no-doc true}
  (:require
   [clojure.string :as str]
   [clj-kondo.impl.utils :as utils :refer [parse-string-all]]
   [clj-kondo.impl.profiler :refer [profile]]))

(defn parse-string [s]
  (let [input (-> s
                  ;; workaround for https://github.com/xsc/rewrite-clj/issues/75
                  (str/replace "##Inf" "::Inf")
                  (str/replace "##-Inf" "::-Inf")
                  (str/replace "##NaN" "::NaN")
                  ;; workaround for https://github.com/borkdude/clj-kondo/issues/11
                  #_(str/replace #_"#:a{#::a {:a b}}"
                               #"#(::?)(.*?)\{" (fn [[_ colons name]]
                                                  (str "#_" colons name "{"))))
        parsed (profile :parse-string-all (parse-string-all input))]
    parsed))

;;;; Scratch

(comment
  (parse-string "(+ 1 2 3) #_1 2 ;; no")
  (:meta (utils/parse-string "^{:a 1} [1 2 3]"))
  (:meta (utils/parse-string "^{:a 1} ^{:b 1} [1 2 3]"))
  (:meta (utils/parse-string "^{:a 1} ^{:b 1} [1 2 3]"))
  )
