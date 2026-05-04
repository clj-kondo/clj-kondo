(ns clj-kondo.impl.gen-macros
  {:no-doc true}
  (:require
   [babashka.fs :as fs]
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.parser :as parser]
   [clj-kondo.impl.rewrite-clj.node.protocols :as node]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import [java.io File]))

(set! *warn-on-reflection* true)

(defn gen-ns-sym
  "Reserved namespace symbol used for an extracted source macro."
  [orig-ns]
  (symbol (str "clj-kondo.gen-macros." orig-ns)))

(defn- ns-path [ns-sym]
  (str/replace (namespace-munge (str ns-sym)) "." "/"))

(defn- gen-file ^File [^File cfg-dir gen-ns]
  (io/file cfg-dir (str (ns-path gen-ns) ".clj")))

(defn- safe-sexpr [n]
  (try (node/sexpr n) (catch Exception _ nil)))

(defn- defmacro-form-name [n]
  (when (= :list (node/tag n))
    (let [[head name-node] (:children n)]
      (when (and head name-node
                 (= 'defmacro (safe-sexpr head)))
        (safe-sexpr name-node)))))

(defn- ns-form? [n]
  (and (= :list (node/tag n))
       (when-let [head (first (:children n))]
         (= 'ns (safe-sexpr head)))))

(defn- top-defmacro-forms
  "Parse content and return its top-level forms with the leading (ns ...)
  form (if any) stripped."
  [content]
  (when content
    (let [parsed (parser/parse-string content)]
      (->> (:children parsed)
           (remove ns-form?)
           vec))))

(defn- compose-content [gen-ns top-forms]
  (str "(ns " gen-ns ")\n\n"
       (str/join "\n\n" (map node/string top-forms))
       "\n"))

(defn- merge-form
  "Replace the (defmacro fn-name ...) entry in top-forms with new-form, or
  append it if absent."
  [top-forms fn-name new-form]
  (let [replaced? (volatile! false)
        merged (mapv (fn [n]
                       (if (= fn-name (defmacro-form-name n))
                         (do (vreset! replaced? true) new-form)
                         n))
                     top-forms)]
    (if @replaced? merged (conj merged new-form))))

(defn- remove-forms [top-forms fn-names]
  (let [drop-set (set fn-names)]
    (vec (remove #(contains? drop-set (defmacro-form-name %)) top-forms))))

(defn- write-file! [^File f gen-ns top-forms]
  (if (seq top-forms)
    (do (io/make-parents f)
        (spit f (compose-content gen-ns top-forms)))
    (when (.exists f)
      (.delete f))))

(defn- with-gen-lock* [cfg-dir body-fn]
  (binding [cache/*lock-file-name* (str (io/file ".cache" ".gen-macros-lock"))]
    (cache/with-thread-lock
      (cache/with-cache cfg-dir 10
        (body-fn)))))

(defmacro ^:private with-gen-lock [cfg-dir & body]
  `(with-gen-lock* ~cfg-dir (fn [] ~@body)))

(defn- read-existing [^File f]
  (when (.exists f) (slurp f)))

(defn- parse-form [s]
  (first (:children (parser/parse-string s))))

(def ^:private reserved-ns-prefix "clj-kondo.gen-macros.")

(defn reserved-ns?
  "True when the namespace is one this feature has already extracted (so the
  defmacro form inside is a copy, not a fresh source). Used to guard against
  recursive re-extraction when the generated file ends up on the lint path."
  [orig-ns]
  (str/starts-with? (str orig-ns) reserved-ns-prefix))

(defn record!
  "Synchronously emit an extracted macro source file under the cfg-dir and
  push a manifest entry onto the per-file `:gen-macros` ctx atom. Only
  fires for the :clj language pass."
  [ctx {:keys [orig-ns fn-name expr]}]
  (when (and (identical? :clj (:lang ctx))
             orig-ns fn-name expr)
    (when-let [cfg-dir (some-> ctx :config :cfg-dir io/file)]
      (let [gen-ns (gen-ns-sym orig-ns)
            f (gen-file cfg-dir gen-ns)
            new-form (parse-form (node/string expr))]
        (with-gen-lock cfg-dir
          (let [forms (top-defmacro-forms (read-existing f))
                merged (merge-form forms fn-name new-form)]
            (write-file! f gen-ns merged)))
        (swap! (:gen-macros ctx) conj
               {:orig-ns orig-ns :fn-name fn-name :gen-ns gen-ns})))))

(defn- manifest-file ^File [cfg-dir main-ns ext]
  (io/file cfg-dir "inline-configs"
           (str (namespace-munge (str main-ns))
                (when ext (str "." ext)))
           "gen-macros.edn"))

(defn- read-manifest [^File f]
  (when (.exists f)
    (try (edn/read-string (slurp f))
         (catch Exception _ nil))))

(defn- write-manifest! [^File f entries]
  (if (seq entries)
    (do (io/make-parents f)
        (binding [*print-namespace-maps* false]
          (spit f (pr-str (vec entries)))))
    (when (.exists f)
      (.delete f))))

(defn- prune-orphans! [^File cfg-dir removed]
  (doseq [[orig-ns entries] (group-by :orig-ns removed)]
    (let [gen-ns (gen-ns-sym orig-ns)
          f (gen-file cfg-dir gen-ns)
          forms (top-defmacro-forms (read-existing f))
          forms (remove-forms forms (map :fn-name entries))]
      (write-file! f gen-ns forms))))

(defn finalize-file!
  "Diff the previous on-disk manifest for this source file against the
  current per-file `:gen-macros` atom, prune any orphan entries from the
  generated source files, and rewrite the manifest. Skips the :cljs
  language pass."
  [ctx lang]
  (when-not (identical? :cljs lang)
    (when-let [cfg-dir (some-> ctx :config :cfg-dir io/file)]
      (when-let [main-ns (some-> ctx :main-ns deref)]
        (let [ext (some-> (:filename ctx) fs/extension)
              man-file (manifest-file cfg-dir main-ns ext)
              prev (read-manifest man-file)
              current (vec @(:gen-macros ctx))
              cur-set (set (map (juxt :orig-ns :fn-name) current))
              removed (vec (remove #(cur-set [(:orig-ns %) (:fn-name %)]) prev))]
          (when (or (seq removed) (not= (boolean prev) (boolean (seq current))))
            (with-gen-lock cfg-dir
              (when (seq removed) (prune-orphans! cfg-dir removed))
              (write-manifest! man-file current))))))))

(defn delete-for-file!
  "Remove every contribution this source file made under cfg-dir. Used when
  inline-configs auto-loading is disabled or no inline-configs were emitted
  for this file. Skips the :cljs language pass."
  [ctx lang]
  (when-not (identical? :cljs lang)
    (when-let [cfg-dir (some-> ctx :config :cfg-dir io/file)]
      (when-let [main-ns (some-> ctx :main-ns deref)]
        (let [ext (some-> (:filename ctx) fs/extension)
              man-file (manifest-file cfg-dir main-ns ext)
              prev (read-manifest man-file)]
          (when (seq prev)
            (with-gen-lock cfg-dir
              (prune-orphans! cfg-dir prev)
              (write-manifest! man-file nil))))))))
