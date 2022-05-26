(ns clj-kondo.impl.hooks
  {:no-doc true}
  (:require
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.metadata :as meta]
   [clj-kondo.impl.rewrite-clj.node :as node]
   [clj-kondo.impl.utils :as utils :refer [*ctx* assoc-some keyword-node
                                           list-node map-node map-vals sexpr
                                           string-node token-node vector-node]]
   [clojure.java.io :as io]
   [sci.core :as sci]
   [clojure.walk :as walk])
  (:refer-clojure :exclude [macroexpand]))

(set! *warn-on-reflection* true)

(defn reg-finding! [m]
  (let [ctx *ctx*
        filename (:filename ctx)]
    (findings/reg-finding! ctx (assoc m :filename filename))))

(defn reg-keyword!
  [k reg-by]
  (assoc-some k :reg reg-by))

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
          (let [f (io/file cp-entry base-path)]
            (when (.exists f) f)))
        (:classpath *ctx*)))

(defn keyword-node? [n]
  (utils/keyword-node? n))

(defn string-node? [n]
  (instance? clj_kondo.impl.rewrite_clj.node.string.StringNode n))

(defn token-node? [n]
  (instance? clj_kondo.impl.rewrite_clj.node.token.TokenNode n))

(defn vector-node? [n]
  (and (instance? clj_kondo.impl.rewrite_clj.node.seq.SeqNode n)
       (identical? :vector (utils/tag n))))

(def list-node? utils/list-node?)

(defn map-node? [n]
  (and (instance? clj_kondo.impl.rewrite_clj.node.seq.SeqNode n)
       (identical? :map (utils/tag n))))

(defn mark-generate [node]
  (assoc node :clj-kondo.impl/generated true))

(defn coerce [s-expr]
  (node/coerce s-expr))

(defn- var-definitions
  "Project cached analysis as a subset of public var-definitions."
  [analysis]
  (let [selected-keys [:ns :name
                       :fixed-arities :varargs-min-arity
                       :private :macro]]
    (->> (dissoc analysis :filename :source)
         (map-vals #(select-keys % selected-keys)))))

(defn- ns-analysis*
  "Adapt from-cache-1 to provide a uniform return format.
  Unifies the format of cached information provided for each source
  language."
  [lang ns-sym]
  (if (= lang :cljc)
    (->> (dissoc
          (cache/from-cache-1 (:cache-dir *ctx*) :cljc ns-sym)
          :filename
          :source)
         (map-vals var-definitions))
    (some->> (cache/from-cache-1 (:cache-dir *ctx*) lang ns-sym)
             var-definitions
             (hash-map lang))))

(defn ns-analysis
  "Return any cached analysis for the namespace identified by ns-sym.
  Returns a map keyed by language keyword with values being maps of var
  definitions keyed by defined symbol. The value for each symbol is a
  subset of the values provide by the top level :analysis option."
  ([ns-sym] (ns-analysis ns-sym {}))
  ([ns-sym {:keys [lang]}]
   (if lang
     (ns-analysis* lang ns-sym)
     (reduce
      merge
      {}
      (map #(ns-analysis* % ns-sym) [:cljc :clj :cljs])))))

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
  (let [call (sexpr node)
        args (rest call)
        res (apply macro call bindings args)
        coerced (coerce res)
        annotated (annotate coerced (meta node))
        lifted (meta/lift-meta-content2 *ctx* annotated)]
    ;;
    lifted))

#_(defmacro macroexpand [macro node]
    `(clj-kondo.hooks-api/-macroexpand (deref (var ~macro)) ~node))

(def ans (sci/create-ns 'clj-kondo.hooks-api nil))

(def api-ns
  {'keyword-node (comp mark-generate keyword-node)
   'keyword-node? keyword-node?
   'string-node (comp mark-generate string-node)
   'string-node? string-node?
   'token-node (comp mark-generate token-node)
   'token-node? token-node?
   'vector-node (comp mark-generate vector-node)
   'vector-node? vector-node?
   'map-node (comp mark-generate map-node)
   'map-node? map-node?
   'list-node (comp mark-generate list-node)
   'list-node? list-node?
   'sexpr sexpr
   'reg-finding! reg-finding!
   'reg-keyword! reg-keyword!
   'coerce coerce
   'ns-analysis ns-analysis
   })

(def sci-ctx
  (sci/init {:namespaces {'clojure.core {'time (with-meta time* {:sci/macro true})}
                          'clj-kondo.hooks-api api-ns}
             :classes {'java.io.Exception Exception
                       'java.lang.System System}
             :imports {'Exception 'java.io.Exception
                       'System java.lang.System}
             :load-fn (fn [{:keys [:namespace]}]
                        (let [^String ns-str (munge (name namespace))
                              base-path (.replace ns-str "." "/")
                              base-path (str base-path ".clj")]
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
