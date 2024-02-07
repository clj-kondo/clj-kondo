(ns clj-kondo.impl.hooks
  {:no-doc true}
  (:require
   [clj-kondo.hooks-api :as api]
   [clj-kondo.impl.config :as config]
   [clj-kondo.impl.metadata :as meta]
   [clj-kondo.impl.utils :as utils :refer [*ctx*]]
   [clojure.java.io :as io]
   [clojure.pprint]
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
   'tag api/tag
   'reg-finding! api/reg-finding!
   'reg-keyword! api/reg-keyword!
   'coerce api/coerce
   'ns-analysis api/ns-analysis
   'generated-node? api/generated-node?
   'resolve api/resolve
   'set-node api/set-node
   'set-node? api/set-node?
   'node? api/node?})

(defn pprint [& args]
  (binding [*out* @sci/out]
    (apply clojure.pprint/pprint args)))

(defn initial-ctx []
  (sci/init {:namespaces {'clojure.core {'time (with-meta time* {:sci/macro true})}
                          'clojure.pprint {'pprint pprint}
                          'clj-kondo.hooks-api api-ns}
             :classes {'java.io.Exception Exception
                       'java.lang.System System
                       ;; enable with-in-str:
                       'java.io.StringReader java.io.StringReader
                       'clojure.lang.LineNumberingPushbackReader clojure.lang.LineNumberingPushbackReader
                       ;; enable assert
                       'java.lang.AssertionError java.lang.AssertionError}
             :imports {'Exception 'java.io.Exception
                       'System java.lang.System}
             :load-fn (fn [{:keys [:namespace]}]
                        (let [^String ns-str (namespace-munge (name namespace))
                              base-path (.replace ns-str "." "/")]
                          (if-let [f (find-file-on-classpath base-path)]
                            {:file (.getAbsolutePath f)
                             :source (slurp f)}
                            (binding [*out* *err*]
                              (println "WARNING: file" base-path "not found while loading hook")
                              nil))))}))

(def sci-ctx
  (initial-ctx))

(defn reset-ctx! []
  (alter-var-root #'sci-ctx (constantly (initial-ctx))))

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


(defn walk
  [inner outer form]
  (cond
    (instance? clj_kondo.impl.rewrite_clj.node.protocols.Node form)
    (outer (update form :children #(mapv inner %)))
    :else (outer form)))

(defn prewalk
  [f form]
  (walk (partial prewalk f) identity (f form)))

(defn annotate
  {:no-doc true}
  [node original-meta]
  (let [!!last-meta (volatile! (assoc original-meta :derived-location true))]
    (prewalk (fn [node]
               (cond
                 (and (instance? clj_kondo.impl.rewrite_clj.node.seq.SeqNode node)
                      (identical? :list (utils/tag node)))
                 (if-let [m (meta node)]
                   (if-let [m (not-empty (select-keys m [:row :end-row :col :end-col]))]
                     (do (vreset! !!last-meta (assoc m :derived-location true))
                         (utils/mark-generate node))
                     (-> (with-meta node
                           (merge @!!last-meta (meta node)))
                         utils/mark-generate))
                   (->
                    (with-meta node
                      @!!last-meta)
                    utils/mark-generate))
                 (instance? clj_kondo.impl.rewrite_clj.node.protocols.Node node)
                 (let [m (meta node)]
                   (if (:row m)
                     node
                     (-> (with-meta node
                           (merge @!!last-meta m))
                         utils/mark-generate)))
                 :else node))
             node)))

(defn macroexpand [macro node bindings]
  (let [call (api/sexpr node)
        args (rest call)
        res (apply macro call bindings args)
        coerced (api/coerce res)
        annotated (annotate coerced (meta node))
        lifted (meta/lift-meta-content2 utils/*ctx* annotated)]
    ;;
    lifted))

(def hook-fn
  (let [delayed-cfg
        (fn [ctx config ns-sym var-sym]
          (try (let [sym (symbol (str ns-sym)
                                 (str var-sym))
                     hook-cfg (:hooks config)
                     filename (:filename ctx)]
                 (when hook-cfg
                   (if-let [x (or (get-in hook-cfg [:analyze-call sym])
                                  (some (fn [group-sym]
                                          (get-in hook-cfg [:analyze-call (symbol (str group-sym)
                                                                                  (str var-sym))]))
                                        (config/ns-groups config ns-sym filename)))]
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
                                      (format "(require '%s %s)\n%s" ns
                                              (if api/*reload* :reload "")
                                              x)))]
                         (binding [*ctx* ctx]
                           ;; require isn't thread safe in SCI
                           (locking load-lock (sci/eval-string* sci-ctx code)))))
                     (when-let [x (or (get-in hook-cfg [:macroexpand sym])
                                      (some (fn [group-sym]
                                              (get-in hook-cfg [:macroexpand (symbol (str group-sym)
                                                                                     (str var-sym))]))
                                            (config/ns-groups config ns-sym filename)))]
                       (sci/binding [sci/out *out*
                                     sci/err *err*]
                         (let [code (if (string? x)
                                      (when (:allow-string-hooks ctx)
                                        x)
                                      ;; x is a function symbol
                                      (let [ns (namespace x)]
                                        (format "(require '%s %s)\n(deref (var %s))"
                                                ns
                                                (if api/*reload* :reload "")
                                                x)))
                               macro (binding [*ctx* ctx]
                                       (locking load-lock
                                         ;; require isn't thread safe in SCI
                                         (sci/eval-string* sci-ctx code)))]
                           (fn [{:keys [node]}]
                             {:node (macroexpand macro node (:bindings *ctx*))})))))))
               (catch Exception e
                 (binding [*out* *err*]
                   (println "WARNING: error while trying to read hook for"
                            (str ns-sym "/" var-sym ":")
                            (.getMessage e))
                   (when (= "true" (System/getenv "CLJ_KONDO_DEV"))
                     (println e)))
                 nil)))
        memo-delayed-cfg (memoize-without-ctx delayed-cfg)
        hook-fn' (fn [& args]
                   (apply (if api/*reload* delayed-cfg memo-delayed-cfg)
                          args))]
    hook-fn'))
