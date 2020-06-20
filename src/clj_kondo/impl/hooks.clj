(ns clj-kondo.impl.hooks
  (:require [clj-kondo.impl.utils :refer [vector-node list-node token-node
                                          sexpr]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [sci.core :as sci]))

(def hook-fn
  (let [delayed-sci-ctx-state (volatile! nil)
        load-file* (fn [f]
                     (let [f (io/file f)
                           s (slurp f)]
                       (sci/with-bindings {sci/ns @sci/ns
                                           sci/file (.getCanonicalPath f)}
                         (sci/eval-string* @@delayed-sci-ctx-state s))))
        ;; we're not using this until it's actually needed
        delayed-sci-ctx (delay (sci/init {:aliases {'io 'clojure.java.io}
                                          :namespaces {'clojure.java.io {'file io/file}
                                                       'clojure.core {'load-file load-file*}
                                                       'clj-kondo.hooks-api {'token-node token-node
                                                                             'vector-node vector-node
                                                                             'list-node list-node
                                                                             'sexpr sexpr
                                                                             }}
                                          :classes {'java.io.Exception Exception}
                                          :imports {'Exception 'java.io.Exception}}))
        _ (vreset! delayed-sci-ctx-state delayed-sci-ctx)
        delayed-cfg
        (fn [config ns-sym var-sym]
          (try (let [sym (symbol (str ns-sym)
                                 (str var-sym))]
                 (when-let [code (get-in config [:hooks sym])]
                   (let [code (str/triml code)
                         code (if (and (not (str/starts-with? code "("))
                                       (not (str/index-of code \newline)))
                                (let [cfg-dir (:cfg-dir config)]
                                  (slurp (io/file cfg-dir code)))
                                code)]
                     (sci/eval-string* @delayed-sci-ctx code))))
               (catch Exception e
                 (binding [*out* *err*]
                   (println "WARNING: error while trying to read hook for"
                            (str ns-sym "/" var-sym ":")
                            (.getMessage e))
                   (when (= "true" (System/getenv "CLJ_KONDO_DEV"))
                     (println e)))
                 nil)))
        delayed-cfg (memoize delayed-cfg)]
    delayed-cfg))
