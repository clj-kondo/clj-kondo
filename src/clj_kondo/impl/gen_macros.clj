(ns clj-kondo.impl.gen-macros
  {:no-doc true}
  (:require
   [babashka.fs :as fs]
   [clj-kondo.impl.cache :as cache]
   [clj-kondo.impl.rewrite-clj.node.keyword :as keyword-node]
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
  - When the same alias is observed both ways, `:as` wins.
  - When the aliased namespace has its own gen file (it contains its own
    marker macros/helpers), redirect the require to point at the gen ns
    so SCI uses the extracted copy instead of the (possibly SCI-incompatible)
    original."
  [usages source-aliases ^File cfg-dir]
  (let [ns->alias-key (reduce-kv (fn [m alias-key full-ns]
                                   (assoc m full-ns alias-key))
                                 {}
                                 source-aliases)
        ;; Memoize is local to one call - no cross-call retention,
        ;; no leak. Saves redundant `fs/exists?` stats when many
        ;; marker forms share an alias.
        redirect #_{:clj-kondo/ignore [:discouraged-var]}
                 (memoize
                  (fn [ns]
                    (let [gen-ns (gen-ns-sym ns)]
                      (if (and cfg-dir (fs/exists? (gen-file cfg-dir gen-ns)))
                        gen-ns
                        ns))))]
    (reduce
     (fn [m {:keys [ns kind]}]
       (if-let [alias-key (get ns->alias-key ns)]
         (let [alias-sym (symbol (name alias-key))
               kind (if (:as-alias (meta alias-key)) :as-alias kind)
               redirected-ns (redirect ns)
               cur (get m alias-sym)]
           (cond
             (nil? cur) (assoc m alias-sym {:ns redirected-ns :kind kind})
             (and (= :as-alias (:kind cur)) (= :as kind))
             (assoc m alias-sym {:ns redirected-ns :kind :as})
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
   existing
   new))

(defn- ns-form-string [gen-ns orig-ns aliases]
  (let [ns-form (if (empty? aliases)
                  (str "(ns " gen-ns ")\n\n")
                  (let [requires (->> aliases
                                      (sort-by key)
                                      (map (fn [[a {:keys [ns kind]}]]
                                             (str "[" ns " " kind " " a "]"))))]
                    (str "(ns " gen-ns "\n  (:require " (str/join "\n            " requires) "))\n\n")))
        ;; Self-alias the gen ns under the original source ns name so
        ;; expand-time `resolve` of symbols qualified with the source ns
        ;; (e.g. `my.app/foo`) finds the extracted var, like at runtime.
        ;; Skipped when the source ns aliases another ns under its own
        ;; name - that alias wins, matching source resolution.
        self-alias (when-not (contains? aliases orig-ns)
                     (str "(alias '" orig-ns " '" gen-ns ")\n\n"))]
    (str ns-form self-alias)))

(defn- write-file! [^File f gen-ns orig-ns aliases top-forms]
  (if (seq top-forms)
    (do (io/make-parents f)
        (spit f (str (ns-form-string gen-ns orig-ns aliases)
                     (str/join "\n\n" (map str top-forms))
                     "\n")))
    (when (fs/exists? f)
      (fs/delete f))))

(def ^:private gen-lock-file (str (io/file ".cache" ".gen-macros-lock")))

(defmacro ^:private with-gen-lock [cfg-dir & body]
  `(cache/with-named-lock gen-lock-file ~cfg-dir 10 ~@body))

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

  Writes per marker (not batched at post-file) so that subsequent forms
  in the same source file can expand the marker macro mid-analysis: the
  gen file must exist on disk before SCI's `require` of it runs.

  `:alias-usages` is the per-usage `{:ns :kind}` vector produced by the
  analyzer while walking the macro/helper body. `:source-aliases` is the
  alias map of the source namespace (`{alias full-ns}`), used to look up
  the alias symbol for each used namespace and to honor `:as-alias`
  intent (preserved as meta on the alias key)."
  [ctx {:keys [orig-ns expr alias-usages source-aliases]}]
  ;; `(:lang ctx)` here is the per-pass lang during walking: `:clj` or
  ;; `:cljs`. For .cljc files the analyzer runs both passes; we only want
  ;; to write the gen file once, so positive-check `:clj`. See
  ;; `delete-for-file!` for the asymmetric file-level counterpart.
  (when (identical? :clj (:lang ctx))
    (when-let [cfg-dir (some-> ctx :config :cfg-dir io/file)]
      (let [gen-ns (gen-ns-sym orig-ns)
            f (gen-file cfg-dir gen-ns)
            new-aliases (aggregate-alias-usages alias-usages source-aliases cfg-dir)
            entries (swap! (:gen-macros ctx) conj
                           {:form expr :aliases new-aliases})]
        (with-gen-lock cfg-dir
          ;; Bind keyword-node's autoresolve-ns so bare `::foo` keywords
          ;; in the macro/helper body serialize as `:<orig-ns>/foo` and
          ;; SCI reads them back to the same value regardless of the gen
          ;; ns's current-ns at load time.
          (binding [keyword-node/*autoresolve-ns* orig-ns]
            (let [forms (mapv :form entries)
                  aliases (reduce merge-alias-maps {} (map :aliases entries))]
              (write-file! f gen-ns orig-ns aliases forms))))))))

(defn delete-for-file!
  "Delete the gen file owned by this source file. Called when the source
  file currently has no marker forms or inline-configs auto-loading is
  disabled. Skips the :cljs language pass. Single-source-per-gen-ns
  assumption: the gen file's owning namespace is derived from the source
  file's `(ns ...)` declaration."
  [ctx lang]
  ;; `lang` here is the file-level lang from `analyze-expressions`:
  ;; `:clj`, `:cljs`, `:cljc`, or `:edn` (not the per-pass lang used in
  ;; `record!`). Negative-check `:cljs` so `:cljc` files are still
  ;; cleaned up - `(identical? :clj lang)` would miss them.
  (when-not (identical? :cljs lang)
    (when-let [cfg-dir (some-> ctx :config :cfg-dir io/file)]
      (when-let [main-ns (some-> ctx :main-ns deref)]
        (let [gen-ns (gen-ns-sym main-ns)
              f (gen-file cfg-dir gen-ns)]
          (when (fs/exists? f)
            (with-gen-lock cfg-dir
              (fs/delete f))))))))
