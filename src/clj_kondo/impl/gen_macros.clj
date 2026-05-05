(ns clj-kondo.impl.gen-macros
  {:no-doc true}
  (:require
   [babashka.fs :as fs]
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.parser :as parser]
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
       (if-let [alias-key (get ns->alias-key ns)]
         (let [alias-sym (with-meta (symbol (name alias-key)) {})
               src-as-alias? (:as-alias (meta alias-key))
               kind (if src-as-alias? :as-alias kind)
               cur (get m alias-sym)]
           (cond
             (nil? cur) (assoc m alias-sym {:ns ns :kind kind})
             (and (= :as-alias (:kind cur)) (= :as kind))
             (assoc m alias-sym {:ns ns :kind :as})
             :else m))
         m))
     {}
     usages)))

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
  "Push an entry onto the per-file `:gen-macros` ctx atom and rewrite the
  gen file from scratch using the current accumulated entries (in source
  order). A gen namespace is owned by exactly one source file - aliases
  and forms removed from the source disappear from the gen ns on the next
  rewrite.

  `:alias-usages` is the per-usage `{:ns :kind}` vector produced by the
  analyzer while walking the macro/helper body. `:source-aliases` is the
  alias map of the source namespace (`{alias full-ns}`), used to look up
  the alias symbol for each used namespace and to honor `:as-alias`
  intent (preserved as meta on the alias key)."
  [ctx {:keys [orig-ns fn-name expr alias-usages source-aliases]}]
  (when (and (identical? :clj (:lang ctx))
             orig-ns fn-name expr)
    (when-let [cfg-dir (some-> ctx :config :cfg-dir io/file)]
      (let [gen-ns (gen-ns-sym orig-ns)
            f (gen-file cfg-dir gen-ns)
            new-aliases (aggregate-alias-usages alias-usages source-aliases)
            entries (swap! (:gen-macros ctx) conj
                           {:orig-ns orig-ns :fn-name fn-name :gen-ns gen-ns
                            :form-str (str expr) :aliases new-aliases})]
        (with-gen-lock cfg-dir
          (let [forms (mapv (fn [{:keys [form-str]}] (parse-form form-str)) entries)
                aliases (reduce merge-alias-maps {} (map :aliases entries))]
            (write-file! f gen-ns aliases forms)))))))

(defn delete-for-file!
  "Delete the gen file owned by this source file. Called when the source
  file currently has no marker forms or inline-configs auto-loading is
  disabled. Skips the :cljs language pass. Single-source-per-gen-ns
  assumption: the gen file's owning namespace is derived from the source
  file's `(ns ...)` declaration."
  [ctx lang]
  (when-not (identical? :cljs lang)
    (when-let [cfg-dir (some-> ctx :config :cfg-dir io/file)]
      (when-let [main-ns (some-> ctx :main-ns deref)]
        (let [gen-ns (gen-ns-sym main-ns)
              f (gen-file cfg-dir gen-ns)]
          (when (fs/exists? f)
            (with-gen-lock cfg-dir
              (fs/delete f))))))))
