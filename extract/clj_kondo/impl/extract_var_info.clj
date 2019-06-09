(ns clj-kondo.impl.extract-var-info
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clj-kondo.impl.cache :as cache]
            [clj-kondo.impl.core :as core-impl]
            [clj-kondo.impl.namespace :as namespace]
            [clj-kondo.impl.config :as config]
            [clojure.set :as set]))

;; extracting information from eastwood with permission from maintainer Andy
;; Fingerhut

  (def code-template "(ns clj-kondo.impl.var-info-gen
  \"GENERATED, DO NOT EDIT. EXTRACTED FROM EASTWOOD WITH PERMISSION.\"
  {:no-doc true})
  (in-ns 'clj-kondo.impl.var-info)
  (def predicates '%s)


  (def clojure-core-syms '%s)


  (def cljs-core-syms '%s)
  ")

(defn cljs-core-vars
  "FIXME: write test for this"
  []
  (let [public? #(-> % meta :private not)
        namespaces (atom {})]
    (doall
     (core-impl/process-files
      {:config config/default-config
       :findings (atom [])
       :namespaces namespaces}
      [(io/file (System/getProperty "user.home")
                ".m2" "repository" "org" "clojure" "clojurescript"
                "1.10.520" "clojurescript-1.10.520.jar")]
      :clj))
    (def dude
      (reduce into #{}
              [(filter public? (get-in @namespaces '[:cljs :cljs cljs.core :vars]))
               (filter public? (get-in @namespaces '[:cljc :clj cljs.core :vars]))
               (filter public? (get-in @namespaces '[:cljc :cljs cljs.core :vars]))]))))

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
        clojure-core-syms-eastwood (set (map (comp symbol name key) core))
        clojure-core-syms-from-cache (set (keep (fn [[k v]]
                                          (when-not (:private? v) k))
                                        (cache/from-cache-1 nil :clj 'clojure.core)))
        clojure-core-syms (into clojure-core-syms-eastwood clojure-core-syms-from-cache)
        cljs-core-syms-cljs (set (keep (fn [[k v]]
                                    (when-not (:private? v) k))
                                       (cache/from-cache-1 nil :cljs 'cljs.core)))
        cljs-core-syms-cljc (let [cljc (cache/from-cache-1 nil :cljc 'cljs.core)]
                              (set (keep (fn [[k v]]
                                           (when-not (:private? v) k))
                                         (concat (get cljc :clj) (get cljc :cljs)))))
        cljs-core-syms (into cljs-core-syms-cljs cljs-core-syms-cljc)
        ;; defn is defined with def, so we currently don't recognize it as a function:
        ;; https://github.com/clojure/clojurescript/blob/47386d7c03e6fc36dc4f0145bd62377802ac1c02/src/main/clojure/cljs/core.cljc#L3243
        ;; now we do
        ;; ns is a special case in the CLJS analyzer:
        ;; https://github.com/clojure/clojurescript/blob/47386d7c03e6fc36dc4f0145bd62377802ac1c02/src/main/clojure/cljs/analyzer.cljc#L3002
        cljs-core-syms (conj cljs-core-syms 'defn 'ns)
        ;; _ (def ccore-syms cljs-core-syms)
        code (format code-template predicates-by-ns
                     clojure-core-syms cljs-core-syms)]
    (spit "src/clj_kondo/impl/var_info_gen.clj" code)))

;;;; Scratch

(comment
  (def var-info (edn/read-string (slurp (io/resource "var-info.edn"))))
  (namespace (key (first var-info)))
  (keep (fn [[k v]]
          (when-not (:private? v) k))
        (cache/from-cache-1 nil :clj 'clojure.core))
  (-main)
  (set/difference ccore-syms dude)
  )
