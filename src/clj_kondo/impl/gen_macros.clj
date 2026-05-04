(ns clj-kondo.impl.gen-macros
  {:no-doc true}
  (:require
   [babashka.fs :as fs]
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.parser :as parser]
   [clj-kondo.impl.utils :as utils]
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
  (try (utils/sexpr n) (catch Exception _ nil)))

(defn- defmacro-form-name [n]
  (when (= :list (utils/tag n))
    (let [[head name-node] (:children n)]
      (when (and head name-node
                 (= 'defmacro (safe-sexpr head)))
        (safe-sexpr name-node)))))

(defn- ns-form? [n]
  (and (= :list (utils/tag n))
       (when-let [head (first (:children n))]
         (= 'ns (safe-sexpr head)))))

(defn- aggregate-alias-usages
  "Reduce a sequence of `{:ns full-ns :kind :as|:as-alias}` usage entries
  collected by the analyzer into a `{alias-sym {:ns full-ns :kind ...}}` map.

  - Reverse-look up the alias from `source-aliases` (an `{alias full-ns}`
    map provided by the source namespace).
  - When source declared an alias as `:as-alias` (preserved via meta on the
    alias key), pin the result to `:as-alias` regardless of usage location -
    matches source intent and avoids forcing SCI to load a stub namespace.
  - When the same alias is observed both ways, `:as` wins."
  [usages source-aliases]
  (let [ns->alias-key (reduce-kv (fn [m alias-key full-ns]
                                   (assoc m full-ns alias-key))
                                 {}
                                 source-aliases)]
    (reduce
     (fn [m {:keys [ns kind]}]
       (let [alias-key (get ns->alias-key ns)]
         (cond
           (nil? alias-key) m
           :else
           (let [alias-sym (with-meta (symbol (name alias-key)) {})
                 src-as-alias? (:as-alias (meta alias-key))
                 kind (if src-as-alias? :as-alias kind)
                 cur (get m alias-sym)]
             (cond
               (nil? cur) (assoc m alias-sym {:ns ns :kind kind})
               (and (= :as-alias (:kind cur)) (= :as kind))
               (assoc m alias-sym {:ns ns :kind :as})
               :else m)))))
     {}
     usages)))

(defn- parse-existing-aliases
  "Read the (ns ... (:require ...)) form from existing content and extract
  {alias-sym {:ns full-ns :kind :as|:as-alias}}."
  [content]
  (when content
    (let [parsed (parser/parse-string content)
          ns-node (first (filter ns-form? (:children parsed)))]
      (when ns-node
        (when-let [sx (safe-sexpr ns-node)]
          (let [require-clause (some (fn [form]
                                       (when (and (sequential? form)
                                                  (= :require (first form)))
                                         (rest form)))
                                     (rest sx))]
            (reduce (fn [m libspec]
                      (cond
                        (and (vector? libspec) (symbol? (first libspec)))
                        (let [[ns & opts] libspec
                              opts (apply hash-map opts)]
                          (cond
                            (:as opts) (assoc m (:as opts) {:ns ns :kind :as})
                            (:as-alias opts) (assoc m (:as-alias opts) {:ns ns :kind :as-alias})
                            :else m))
                        :else m))
                    {}
                    require-clause)))))))

(defn- merge-alias-maps
  "Union two `{alias {:ns :kind}}` maps, promoting `:as-alias` to `:as`
  when the same alias appears both ways across the two inputs."
  [existing new]
  (reduce-kv
   (fn [m a v]
     (let [cur (get m a)]
       (cond
         (nil? cur) (assoc m a v)
         (and (= :as-alias (:kind cur)) (= :as (:kind v)))
         (assoc m a v)
         :else m)))
   (or existing {})
   new))

(defn- ns-form-string [gen-ns aliases]
  (if (empty? aliases)
    (str "(ns " gen-ns ")\n\n")
    (let [requires (->> aliases
                        (sort-by key)
                        (map (fn [[a {:keys [ns kind]}]]
                               (str "[" ns " " kind " " a "]"))))]
      (str "(ns " gen-ns "\n  (:require " (str/join "\n            " requires) "))\n\n"))))

(defn- parse-content
  "Parse existing gen-file content, returning {:aliases {...} :forms [...]}."
  [content]
  (if-not content
    {:aliases {} :forms []}
    (let [parsed (parser/parse-string content)
          children (:children parsed)]
      {:aliases (or (parse-existing-aliases content) {})
       :forms (vec (remove ns-form? children))})))

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

(defn- write-file! [^File f gen-ns aliases top-forms]
  (if (seq top-forms)
    (do (io/make-parents f)
        (spit f (str (ns-form-string gen-ns aliases)
                     (str/join "\n\n" (map str top-forms))
                     "\n")))
    (when (fs/exists? f)
      (fs/delete f))))

(defn- with-gen-lock* [cfg-dir body-fn]
  (binding [cache/*lock-file-name* (str (io/file ".cache" ".gen-macros-lock"))]
    (cache/with-thread-lock
      (cache/with-cache cfg-dir 10
        (body-fn)))))

(defmacro ^:private with-gen-lock [cfg-dir & body]
  `(with-gen-lock* ~cfg-dir (fn [] ~@body)))

(defn- read-existing [^File f]
  (when (fs/exists? f) (slurp f)))

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
  fires for the :clj language pass.

  `:alias-usages` is the per-usage `{:ns :kind}` vector produced by the
  analyzer while walking the macro body. `:source-aliases` is the alias
  map of the source namespace (`{alias full-ns}`), used to look up the
  alias symbol for each used namespace and to honor `:as-alias` intent
  (preserved as meta on the alias key)."
  [ctx {:keys [orig-ns fn-name expr alias-usages source-aliases]}]
  (when (and (identical? :clj (:lang ctx))
             orig-ns fn-name expr)
    (when-let [cfg-dir (some-> ctx :config :cfg-dir io/file)]
      (let [gen-ns (gen-ns-sym orig-ns)
            f (gen-file cfg-dir gen-ns)
            new-form (parse-form (str expr))
            new-aliases (aggregate-alias-usages alias-usages source-aliases)]
        (with-gen-lock cfg-dir
          (let [{:keys [aliases forms]} (parse-content (read-existing f))
                merged-forms (merge-form forms fn-name new-form)
                merged-aliases (merge-alias-maps aliases new-aliases)]
            (write-file! f gen-ns merged-aliases merged-forms)))
        (swap! (:gen-macros ctx) conj
               {:orig-ns orig-ns :fn-name fn-name :gen-ns gen-ns})))))

(defn- manifest-file ^File [cfg-dir main-ns ext]
  (io/file cfg-dir "inline-configs"
           (str (namespace-munge (str main-ns))
                (when ext (str "." ext)))
           "gen-macros.edn"))

(defn- read-manifest [^File f]
  (when (fs/exists? f)
    (try (edn/read-string (slurp f))
         (catch Exception _ nil))))

(defn- write-manifest! [^File f entries]
  (if (seq entries)
    (do (io/make-parents f)
        (binding [*print-namespace-maps* false]
          (spit f (pr-str (vec entries)))))
    (when (fs/exists? f)
      (fs/delete f))))

(defn- prune-orphans! [^File cfg-dir removed]
  (doseq [[orig-ns entries] (group-by :orig-ns removed)]
    (let [gen-ns (gen-ns-sym orig-ns)
          f (gen-file cfg-dir gen-ns)
          {:keys [aliases forms]} (parse-content (read-existing f))
          forms (remove-forms forms (map :fn-name entries))]
      (write-file! f gen-ns aliases forms))))

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
