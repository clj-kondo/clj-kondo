(ns clj-kondo.impl.findings
  {:no-doc true}
  (:require [clj-kondo.impl.utils :as utils]))

;; ignore  row 1, col 21, end-row 1, end-col 31
;; finding row 1, col 26, end-row 1, end-col 30

(defn ignore-match? [ignore tp]
  (or (true? ignore)
      (contains? ignore tp)))

(defn ignored?
  "Ignores are sorted in order of rows and cols. So if we are handling a node with a row before the "
  [ctx m tp]
  (let [ignores @(:ignores ctx)
        filename (:filename m)
        lang (:lang ctx)
        row (:row m)]
    (when row
      (when-let [ignores (or (get-in ignores [filename lang])
                             (when (identical? :cljc lang)
                               (or (get-in ignores [filename :clj])
                                   (get-in ignores [filename :cljs]))))]
        (loop [ignores ignores]
          (when ignores
            (let [ignore (first ignores)
                  ignore-row (:row ignore)]
              (if (> ignore-row row)
                ;; since ignores are sorted on row (and col) we can skip the rest of the checking here
                false
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
                        true
                        (recur (next ignores)))
                      (recur (next ignores))))
                  (recur (next ignores)))))))))))

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
        m (cond-> m
            (identical? :cljc base-lang)
            (assoc :cljc true))]
    (when (and level (not (identical? :off level)) (not dependencies) (not skip-lint?))
      (when-not (ignored? ctx m tp)
        (let [m (assoc m :level level)]
          (swap! findings conj m)
          m)))))

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
