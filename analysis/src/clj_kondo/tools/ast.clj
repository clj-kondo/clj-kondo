(ns clj-kondo.tools.ast
  (:require [clj-kondo.core :as clj-kondo]
            [rewrite-clj.parser :as parser]))

(defn foo [x] x)

(def file *file*)

(defn -main [& _args]
  (let [nodes (parser/parse-string-all (slurp file))
        analysis (:analysis (clj-kondo/run! {:lint [file] :config {:analysis true}}))]
    ))
