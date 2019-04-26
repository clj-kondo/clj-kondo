(ns clj-kondo.impl.parser
  {:no-doc true}
  (:require [clojure.string :as str]
            [clj-kondo.impl.utils :refer [parse-string-all]]
            [rewrite-clj.parser.core :as pc]
            [rewrite-clj.node :as node]
            [rewrite-clj.reader :as reader]))

(defn skip-whitespace
  "Parse as much whitespace as possible. The created node can either contain
   only linebreaks or only space/tabs."
  [reader]
  (let [c (reader/peek reader)]
    (cond (reader/linebreak? c)
          (do (reader/read-while reader reader/linebreak?)
              nil)

          (reader/comma? c)
          (do (reader/read-while reader reader/comma?)
              nil)

          :else
          (do (reader/read-while reader reader/space?)
              nil))
    (let [start-position (reader/position reader :row :col)]
      (let [x (pc/parse-next reader)]
        (when x
          (with-meta x
            (merge (meta x) start-position)))))))

(defn read-with-meta
  "Use the given function to read value, then attach row/col metadata."
  [reader read-fn]
  (println "READ WITH METAL")
  (let [start-position (reader/position reader :row :col)]
    (if-let [entry (read-fn reader)]
      (let [p (reader/position reader :end-row :end-col)
            p (merge p start-position)
            new-m (merge p (meta entry))]
        (with-meta entry new-m)))))

(intern 'rewrite-clj.parser.whitespace 'parse-whitespace skip-whitespace)
(intern 'rewrite-clj.reader 'read-with-meta read-with-meta)

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
        parsed (parse-string-all input config)]
    parsed))

;;;; Scratch

(comment
  (require '[rewrite-clj.parser.core :as pc])
  (require '[rewrite-clj.node :as node])
  (require '[rewrite-clj.reader :as reader])
  (defmethod pc/parse-next* :whitespace
    [reader]
    )

  

  
  
  (require '[clojure.java.io :as io])
  
  (pc/parse-next (reader/string-reader "(+ 1 2 3)"))
  
  

  )
