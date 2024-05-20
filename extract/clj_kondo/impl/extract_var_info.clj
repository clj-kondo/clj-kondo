(ns clj-kondo.impl.extract-var-info
  {:no-doc true}
  (:require
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.core :as core-impl]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.deps.alpha :as deps]
   ))

(set! *warn-on-reflection* true)

(def versions (edn/read-string (slurp "script/versions.edn")))

(def clj-version (:extract-clj-version versions))
(def cljs-version (:extract-cljs-version versions))

(def deps {:deps {'org.clojure/clojure {:mvn/version clj-version}
                  'org.clojure/clojurescript {:mvn/version cljs-version}}
           :mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                       "clojars" {:url "https://repo.clojars.org/"}}})

(deps/resolve-deps deps nil)

(def code-template "(ns clj-kondo.impl.var-info-gen
  \"GENERATED, DO NOT EDIT.\"
  {:no-doc true})
  (in-ns 'clj-kondo.impl.var-info)

  (def clojure-core-syms '%s)

  (def cljs-core-syms '%s)

  (def default-import->qname '%s)

  (def default-fq-imports '%s)

  (def unused-values '%s)
")

(defn eastwood-var-info
  "extracting information from eastwood's var-info.edn with permission
  from maintainer Andy Fingerhut"
  []
  (edn/read-string (slurp (io/resource "var-info.edn"))))

(defn public? [[k v]]
  (when (not (or (:private v)
                 (:class v)))
    k))

(defn extract-clojure-core-vars
  []
  (let [namespaces (atom {})
        special '#{}
        findings (atom [])]
    (doall
     (core-impl/process-files
      {:config config/default-config
       :files (atom 0)
       :findings findings
       :ignores (atom {})
       :java-class-usages (atom [])
       :namespaces namespaces
       :used-namespaces (atom {:clj #{}
                               :cljs #{}
                               :cljc #{}})}
      [(io/file (System/getProperty "user.home")
                ".m2" "repository" "org" "clojure" "clojure"
                clj-version (format "clojure-%s.jar" clj-version))]
      :clj
      nil nil))
    (reduce into special
            [(keep public? (get-in @namespaces '[:clj :clj clojure.core :vars]))])))

(defn extract-cljs-core-vars
  []
  (let [namespaces (atom {})
        ;; built-ins from analyzer, e.g.
        ;; https://github.com/clojure/clojurescript/blob/47386d7c03e6fc36dc4f0145bd62377802ac1c02/src/main/clojure/cljs/analyzer.cljc#L3002
        special '#{ns js* *target* goog.global goog.DEBUG}]
    (doall
     (core-impl/process-files
      {:config config/default-config
       :files (atom 0)
       :findings (atom [])
       :ignores (atom {})
       :java-class-usages (atom [])
       :namespaces namespaces
       :used-namespaces (atom {:clj #{}
                               :cljs #{}
                               :cljc #{}})}
      [(io/file (System/getProperty "user.home")
                ".m2" "repository" "org" "clojure" "clojurescript"
                cljs-version (format "clojurescript-%s.jar" cljs-version))]
      :clj
      nil
      nil))
    (reduce into special
            [(keep public? (get-in @namespaces '[:cljs :cljs cljs.core :vars]))
             (keep public? (get-in @namespaces '[:cljc :clj cljs.core :vars]))
             (keep public? (get-in @namespaces '[:cljc :cljs cljs.core :vars]))])))

(defn extract-default-imports []
  (into {}
        (for [[k v] clojure.lang.RT/DEFAULT_IMPORTS]
          [k (symbol (.getName ^Class v))])))

(defn extract-unused-values [var-info]
  (reduce-kv (fn [acc sym info]
               (if (or (:warn-if-ret-val-unused info)
                       (:pure-fn info)
                       (:lazy info) ;; unrealized laziness = unused value
                       #_(:pure-fn-if-fn-args-pure info))
                 (conj acc sym)
                 acc))
             '#{clojure.core/for}
             var-info))

(defn print-set-sorted [s]
  (format "#{%s}"
          (str/join "\n" (sort s))))

(defn print-map-sorted [s]
  (format "{%s}"
          (str/join "\n"
                    (map (fn [[k v]]
                           (str k " " v))
                         (sort s)))))

(defn -main [& _args]
  (let [var-info (eastwood-var-info)
        #_#_predicates (set (keep (fn [[k v]]
                                (when (:predicate v)
                                  k))
                              var-info))
        #_#_predicates-by-ns (group-by (comp symbol namespace) predicates)
        #_#_predicates-by-ns (zipmap (keys predicates-by-ns)
                                 (map (fn [vals]
                                        (set (map (comp symbol name) vals)))
                                      (vals predicates-by-ns)))
        by-namespace (group-by (comp namespace key)
                               var-info)
        core (get by-namespace "clojure.core")
        clojure-core-syms-eastwood (set (map (comp symbol name key) core))
        extracted-core-vars (extract-clojure-core-vars)
        clojure-core-syms (into clojure-core-syms-eastwood extracted-core-vars)
        cljs-core-vars (extract-cljs-core-vars)
        default-java-imports (extract-default-imports)
        unused-values (extract-unused-values var-info)
        code (format code-template #_predicates-by-ns
                     (print-set-sorted clojure-core-syms)
                     (print-set-sorted cljs-core-vars)
                     (print-map-sorted default-java-imports)
                     (print-set-sorted (vals default-java-imports))
                     (print-set-sorted unused-values))]
    (spit "src/clj_kondo/impl/var_info_gen.clj" code)))

;;;; Scratch

(comment
  (-main)
  )
