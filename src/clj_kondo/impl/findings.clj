(ns clj-kondo.impl.findings
  {:no-doc true})

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
    (when-let [ignores (get-in ignores [filename lang])]
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
                (let [ignore-end-row (:end-row ignore)]
                  (if (or (< row ignore-end-row)
                          (and (= row ignore-end-row)
                               (<= (:end-col m) (:end-col ignore))))
                    (if (ignore-match? (:ignore ignore) tp)
                      true
                      (recur (next ignores)))
                    (recur (next ignores))))
                (recur (next ignores))))))))))

(defn reg-finding! [ctx m]
  (let [no-warnings (:no-warnings ctx)
        findings (:findings ctx)
        config (:config ctx)
        tp (:type m)
        level (-> config :linters tp :level)]
    (when (and level (not (identical? :off level)) (not no-warnings))
      (when-not (ignored? ctx m tp)
        (let [m (assoc m :level level)]
          (swap! findings conj m)))))
  nil)

;;;; Scratch

(comment
  ;; (reg-finding! (atom nil) {})
  )
