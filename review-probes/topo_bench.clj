(ns topo-bench
  "Isolates the cost the issue-2284 topo sort adds in front of analysis."
  (:require [babashka.fs :as fs]
            [clj-kondo.core :as clj-kondo]
            [edamame.core :as edamame]
            [weavejester.dependency :as dep]))

;;;; verbatim from branch issue-2284, src/clj_kondo/impl/core.clj

(def ^:dynamic *topo-sort* true)

(def edamame-opts
  {:all true :read-cond :allow :features #{:clj :cljs}})

(defn read-ns-decl [source]
  (try
    (let [rdr (edamame/reader source)
          form (edamame/parse-next rdr edamame-opts)]
      (when (and (list? form) (= 'ns (first form)))
        form))
    (catch Exception _ nil)))

(defn topo-sort-sources [sources]
  (let [sources (vec sources)
        n (count sources)]
    (if (or (<= n 1) (not *topo-sort*))
      sources
      (let [ns-decls (mapv #(read-ns-decl (:source %)) sources)
            ns->idxs (reduce-kv (fn [m i decl]
                                  (if decl
                                    (update m (second decl) (fnil conj []) i)
                                    m))
                                {} ns-decls)
            all-nses (set (keys ns->idxs))
            {:keys [dependencies dependents]}
            (reduce-kv
             (fn [acc _i decl]
               (if-not decl acc
                       (let [ns-name (second decl)
                             deps (keep (fn [{:keys [lib]}]
                                          (when (and (contains? all-nses lib)
                                                     (not= lib ns-name))
                                            lib))
                                        (:requires (edamame/parse-ns-form decl)))]
                         (reduce (fn [{:keys [dependencies dependents]} d]
                                   {:dependencies (update dependencies ns-name (fnil conj #{}) d)
                                    :dependents (update dependents d (fnil conj #{}) ns-name)})
                                 acc deps))))
             {:dependencies {} :dependents {}} ns-decls)
            graph (dep/->MapDependencyGraph dependencies dependents)
            sorted (dep/topo-sort graph)
            sorted-idxs (into #{} (mapcat #(get ns->idxs %)) sorted)
            sorted-sources (mapcat (fn [ns-name]
                                     (map sources (get ns->idxs ns-name)))
                                   sorted)
            remaining (keep-indexed
                       (fn [i src]
                         (when-not (contains? sorted-idxs i) src))
                       sources)]
        (into (vec sorted-sources) remaining)))))

;;;; bench

(defn ms [f]
  (let [t (System/nanoTime)
        v (f)]
    [(/ (- (System/nanoTime) t) 1e6) v]))

(defn -main [& dirs]
  (let [files (into []
                    (comp (mapcat #(fs/glob % "**.{clj,cljc,cljs}"))
                          (map str))
                    dirs)
        sources (mapv (fn [f] {:filename f :source (slurp f)}) files)]
    (println "files:" (count files)
             "bytes:" (reduce + (map (comp count :source) sources)))
    ;; warmup
    (dotimes [_ 2] (topo-sort-sources sources))
    (doseq [i (range 3)]
      (let [[t decls] (ms (fn [] (mapv #(read-ns-decl (:source %)) sources)))]
        (println (format "read-ns-decl only    run %d: %8.1f ms (ns forms found: %d)"
                         i t (count (remove nil? decls)))))
      (let [[t sorted] (ms (fn [] (topo-sort-sources sources)))]
        (println (format "topo-sort-sources    run %d: %8.1f ms (out: %d)"
                         i t (count sorted)))))
    ;; full lint of the same tree, cache off, no analysis
    (doseq [i (range 2)]
      (let [[t res] (ms (fn [] (clj-kondo/run! {:lint (vec dirs) :cache false})))]
        (println (format "full clj-kondo run!  run %d: %8.1f ms (findings: %d, files: %d)"
                         i t (count (:findings res)) (-> res :summary :files)))))
    (shutdown-agents)))
