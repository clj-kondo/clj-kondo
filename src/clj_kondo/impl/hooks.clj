(ns clj-kondo.impl.hooks
  (:require [clj-kondo.impl.findings :as findings]
            [clj-kondo.impl.utils :refer [vector-node list-node token-node
                                          sexpr]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [sci.core :as sci]))

(set! *warn-on-reflection* true)

(def ^:dynamic *cfg-dir* nil)
(def ^:dynamic *ctx* nil)

(defn reg-finding! [m]
  (let [ctx *ctx*
        filename (:filename ctx)]
    (findings/reg-finding! ctx (assoc m :filename filename))))

(def sci-ctx (sci/init {:namespaces {'clj-kondo.hooks-api {'token-node token-node
                                                           'vector-node vector-node
                                                           'list-node list-node
                                                           'sexpr sexpr
                                                           'reg-finding! reg-finding!}}
                        :classes {'java.io.Exception Exception}
                        :imports {'Exception 'java.io.Exception}
                        :load-fn (fn [{:keys [:namespace]}]
                                   (let [^String ns-str (munge (name namespace))
                                         base-path (.replace ns-str "." "/")
                                         base-path (str base-path ".clj")
                                         f (io/file *cfg-dir* base-path)
                                         path (.getCanonicalPath f)]
                                     (if (.exists f)
                                       {:file path
                                        :source (slurp f)}
                                       (binding [*out* *err*]
                                         (println "WARNING: file" path "not found while loading hook")
                                         nil))))}))

(def hook-fn
  (let [delayed-cfg
        (fn [config key ns-sym var-sym]
          (try (let [sym (symbol (str ns-sym)
                                 (str var-sym))]
                 (when-let [code (get-in config [:hooks key sym])]
                   (let [cfg-dir (:cfg-dir config)]
                     (binding [*cfg-dir* cfg-dir]
                       (sci/binding [sci/out *out*
                                     sci/err *err*]
                         (if (string? code)
                           (let [code (str/triml code)
                                 code (if (and (not (str/starts-with? code "("))
                                               (not (str/index-of code \newline))
                                               (str/ends-with? code ".clj"))
                                        (slurp (io/file cfg-dir code))
                                        code)]
                             (sci/eval-string* sci-ctx code))
                           ;; assume symbol
                           (let [sym code
                                 ns (namespace sym)
                                 code (format "(require '%s)\n%s" ns sym)]
                             (sci/eval-string* sci-ctx code))))))))
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
