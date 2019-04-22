(ns clj-kondo.impl.parser
  {:no-doc true}
  (:require [clojure.string :as str]
            [clj-kondo.impl.utils :refer [parse-string-all]]
            [clj-kondo.impl.macroexpand :refer [expand-all]]))

(defn parse-string [s config]
  (let [input (-> s
                  ;; workaround for https://github.com/xsc/rewrite-clj/issues/75
                  (str/replace "##Inf" "::Inf")
                  (str/replace "##-Inf" "::-Inf")
                  (str/replace "##NaN" "::NaN")
                  ;; workaround for https://github.com/borkdude/clj-kondo/issues/11
                  (str/replace #_"#:a{#::a {:a b}}"
                               #"#(::?)(.*?)\{" (fn [[_ colons name]]
                                                  (str "#_" colons name "{"))))
        parsed (parse-string-all input config)
        parsed (expand-all parsed)]
    parsed))

