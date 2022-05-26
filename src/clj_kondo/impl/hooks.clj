(ns clj-kondo.impl.hooks
  {:no-doc true}
  (:require
   [clj-kondo.hooks-api :as api]
   [clj-kondo.impl.metadata :as meta]
   [clj-kondo.impl.utils :as utils :refer [*ctx*]]
   [clojure.java.io :as io]
   [clojure.walk :as walk]
   [sci.core :as sci])
  (:refer-clojure :exclude [macroexpand]))

(set! *warn-on-reflection* true)

(defn time*
  "Evaluates expr and prints the time it took.  Returns the value of
  expr."
  [_ _ expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (prn (str "Elapsed time: " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
     ret#))

(defn find-file-on-classpath ^java.io.File
  [base-path]
  (some (fn [cp-entry]
          (some (fn [ext]
                  (let [f (io/file cp-entry (str base-path "." ext))]
                    (when (.exists f) f)))
                ["clj_kondo" "clj"]))
        (:classpath *ctx*)))

(defn annotate [node original-meta]
  (let [!!last-meta (volatile! original-meta)]
    (walk/prewalk (fn [node]
                    (cond
                      (and (instance? clj_kondo.impl.rewrite_clj.node.seq.SeqNode node)
                           (identical? :list (utils/tag node)))
                      (do
                        (when-let [m (meta node)]
                          (vreset! !!last-meta (select-keys m [:row :end-row :col :end-col])))
                        node)
                      (instance? clj_kondo.impl.rewrite_clj.node.protocols.Node node)
                      (with-meta node
                        (merge @!!last-meta (meta node)))
                      :else node))
                  node)))

(defn -macroexpand [macro node bindings]
  (let [call (api/sexpr node)
        args (rest call)
        res (apply macro call bindings args)
        coerced (api/coerce res)
        annotated (annotate coerced (meta node))
        lifted (meta/lift-meta-content2 *ctx* annotated)]
    ;;
    lifted))

#_(defmacro macroexpand [macro node]
    `(clj-kondo.hooks-api/-macroexpand (deref (var ~macro)) ~node))

(def ans (sci/create-ns 'clj-kondo.hooks-api nil))

(def api-ns
  {'keyword-node api/keyword-node
   'keyword-node? api/keyword-node?
   'string-node api/string-node
   'string-node? api/string-node?
   'token-node api/token-node
   'token-node? api/token-node?
   'vector-node api/vector-node
   'vector-node? api/vector-node?
   'map-node api/map-node
   'map-node? api/map-node?
   'list-node api/list-node
   'list-node? api/list-node?
   'sexpr api/sexpr
   'reg-finding! api/reg-finding!
   'reg-keyword! api/reg-keyword!
   'coerce api/coerce
   'ns-analysis api/ns-analysis})

(def sci-ctx
  (sci/init {:namespaces {'clojure.core {'time (with-meta time* {:sci/macro true})}
                          'clj-kondo.hooks-api api-ns}
             :classes {'java.io.Exception Exception
                       'java.lang.System System}
             :imports {'Exception 'java.io.Exception
                       'System java.lang.System}
             :load-fn (fn [{:keys [:namespace]}]
                        (let [^String ns-str (munge (name namespace))
                              base-path (.replace ns-str "." "/")]
                          (if-let [f (find-file-on-classpath base-path)]
                            {:file (.getAbsolutePath f)
                             :source (slurp f)}
                            (binding [*out* *err*]
                              (println "WARNING: file" base-path "not found while loading hook")
                              nil))))}))

(defn memoize-without-ctx
  [f]
  (let [mem (atom {})]
    (fn [ctx & args]
      (if-let [e (find @mem args)]
        (val e)
        (let [ret (apply f ctx args)]
          (swap! mem assoc args ret)
          ret)))))

(def load-lock (Object.))

(def hook-fn
  (let [delayed-cfg
        (fn [ctx config ns-sym var-sym]
          (try (let [sym (symbol (str ns-sym)
                                 (str var-sym))
                     hook-cfg (:hooks config)]
                 (when hook-cfg
                   (if-let [x (get-in hook-cfg [:analyze-call sym])]
                     ;; we return a function of ctx, so we will never memoize on
                     ;; ctx, which will hold on to all the linting state and
                     ;; creates memory leaks for long lives processes (LSP /
                     ;; VSCode), see #1036
                     (sci/binding [sci/out *out*
                                   sci/err *err*]
                       (let [code (if (string? x)
                                    (when (:allow-string-hooks ctx)
                                      x)
                                    ;; x is a function symbol
                                    (let [ns (namespace x)]
                                      (format "(require '%s)\n%s" ns x)))]
                         (binding [*ctx* ctx]
                           ;; require isn't thread safe in SCI
                           (locking load-lock (sci/eval-string* sci-ctx code)))))
                     (when-let [x (get-in hook-cfg [:macroexpand sym])]
                       (sci/binding [sci/out *out*
                                     sci/err *err*]
                         (let [code (if (string? x)
                                      (when (:allow-string-hooks ctx)
                                        x)
                                      ;; x is a function symbol
                                      (let [ns (namespace x)]
                                        (format "(require '%s)\n(deref (var %s))" ns x)))
                               macro (binding [*ctx* ctx]
                                       (locking load-lock
                                         ;; require isn't thread safe in SCI
                                         (sci/eval-string* sci-ctx code)))]
                           (fn [{:keys [node]}]
                             {:node (-macroexpand macro node (:bindings *ctx*))})))))))
               (catch Exception e
                 (binding [*out* *err*]
                   (println "WARNING: error while trying to read hook for"
                            (str ns-sym "/" var-sym ":")
                            (.getMessage e))
                   (when (= "true" (System/getenv "CLJ_KONDO_DEV"))
                     (println e)))
                 nil)))
        delayed-cfg (memoize-without-ctx delayed-cfg)]
    delayed-cfg))
