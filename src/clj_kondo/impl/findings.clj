(ns clj-kondo.impl.findings
  {:no-doc true}
  (:require [clj-kondo.impl.analyzer.common :as common]
            [clj-kondo.impl.utils :as utils]))

;; ignore  row 1, col 21, end-row 1, end-col 31
;; finding row 1, col 26, end-row 1, end-col 30

(defn ignore-match? [ignore tp]
  (or (identical? :all ignore)
      (contains? ignore tp)))

(defn ignored?
  "Ignores are sorted in order of rows and cols. So if we are handling a node with a row before the "
  [ctx m tp]
  (let [!ignores (:ignores ctx)
        ignores @!ignores
        filename (:filename m)
        base-lang (:lang ctx)
        row (:row m)]
    (when row
      (when-let [[ignores lang] (or (some-> (get-in ignores [filename base-lang])
                                            (vector base-lang))
                                    (when (or (identical? :cljc base-lang)
                                              (nil? base-lang))
                                      (or (some-> (get-in ignores [filename :clj])
                                                  (vector :clj))
                                          (some-> (get-in ignores [filename :cljs])
                                                  (vector :cljs))))
                                    (when (or (identical? :edn base-lang)
                                              (nil? base-lang))
                                      (some-> (get-in ignores [filename :edn])
                                              (vector :edn))))]
        (loop [ignores ignores
               idx 0]
          (when ignores
            (let [ignore (first ignores)
                  ignore-row (:row ignore)]
              (if (> ignore-row row)
                ;; since ignores are sorted on row (and col) we can skip the rest of the checking here
                (recur (next ignores) (inc idx))
                ;; (>= row ignore row) is true from here
                (if (or
                     (> row ignore-row)
                     ;; row and ignore-row are equal, so the col of the
                     ;; finding has to be before the col of the ignore
                     (>= (:col m) (:col ignore)))
                  (let [ignore-end-row (:end-row ignore)
                        end-col (:end-col m)]
                    (if (and end-col ;; if there is no end-col our finding location is incomplete...
                             (or (< row ignore-end-row)
                                 (and (= row ignore-end-row)
                                      (<= (:end-col m) (:end-col ignore)))))
                      (if (ignore-match? (:ignore ignore) tp)
                        ;; TODO: this is a race condition, or maybe not since the same file isn't analyzed by multiple threads
                        (do (swap! !ignores assoc-in [filename lang idx :used] true)
                            true)
                        (recur (next ignores) (inc idx)))
                      (recur (next ignores) (inc idx))))
                  (recur (next ignores) (inc idx)))))))))))

(defn reg-finding!
  "Register a new finding.
  Returns truthy value if the finding was applied / not ignored."
  [ctx m]
  (let [dependencies (:dependencies ctx)
        findings (:findings ctx)
        skip-lint? (:skip-lint ctx)
        config (:config ctx)
        tp (:type m)
        level (or (:level m)
                  (-> config :linters tp :level))
        base-lang (:base-lang ctx)
        lang (:lang ctx)
        m (cond-> m
            (identical? :cljc base-lang)
            (assoc :cljc true :lang lang))]
    (when (and level (not (identical? :off level)) (not dependencies) (not skip-lint?))
      (when (or (identical? :redundant-ignore (:type m))
                (not (ignored? ctx m tp)))
        (let [m (assoc m :level level)]
          (swap! findings conj m)
          m)))))

(vswap! common/common assoc 'reg-finding! reg-finding!)

(defn warn-reflection [ctx expr]
  (when (:warn-only-on-interop ctx)
    (when-not (some #(and (= (:filename ctx)
                             (:filename %))
                          (= :warn-on-reflection (:type %))) @(:findings ctx))
      (reg-finding!
       ctx (utils/node->line (:filename ctx)
                             expr
                             :warn-on-reflection
                             "Var *warn-on-reflection* is not set in this namespace.")))))

;;;; Scratch

(comment
  ;; (reg-finding! (atom nil) {})
  )
