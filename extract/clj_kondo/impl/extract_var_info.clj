(ns clj-kondo.impl.extract-var-info
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

;; extracting information from eastwood with permission from maintainer Andy
;; Fingerhut

(def code-template "(ns clj-kondo.impl.var-info
  \"GENERATED, DO NOT EDIT. EXTRACTED FROM EASTWOOD WITH PERMISSION.\"
  {:no-doc true})
  (def predicates '%s)
  (def core-syms '%s)
  ")

(defn -main [& args]
  (let [var-info (edn/read-string (slurp (io/resource "var-info.edn")))
        predicates (set (keep (fn [[k v]]
                                (when (:predicate v)
                                  k))
                              var-info))
        predicates-by-ns (group-by (comp symbol namespace) predicates)
        predicates-by-ns (zipmap (keys predicates-by-ns)
                                 (map (fn [vals]
                                        (set (map (comp symbol name) vals)))
                                      (vals predicates-by-ns)))
        by-namespace (group-by (comp namespace key)
                               var-info)
        core (get by-namespace "clojure.core")
        core-syms (set (map (comp symbol name key) core))
        code (format code-template predicates-by-ns core-syms)]
    (spit "src/clj_kondo/impl/var_info.clj" code)))

;;;; Scratch

(comment
  (def var-info (edn/read-string (slurp (io/resource "var-info.edn"))))
  (namespace (key (first var-info)))
  )
