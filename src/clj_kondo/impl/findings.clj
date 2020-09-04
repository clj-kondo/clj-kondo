(ns clj-kondo.impl.findings
  {:no-doc true})

;; ignore  row 1, col 21, end-row 1, end-col 31
;; finding row 1, col 26, end-row 1, end-col 30

(defn ignore-match? [ignore type]
  (or (true? ignore)
      (contains? ignore type)))

(defn ignored?
  "Ignores are sorted in order of rows and cols. So if we are handling a node with a row before the "
  [ctx m type]
  (let [ignores @(:ignores ctx)
        filename (:filename m)]
    (when-let [ignores (get ignores filename)]
      (let [row (:row m)]
        (reduce (fn [_ ignore]
                  (let [ignore-row (:row ignore)]
                    (if (> ignore-row row)
                      ;; since ignores are sorted on row (and col) we can skip the rest of the checking here
                      (reduced false)
                      ;; (>= row ignore row) is true from here
                      (when (or
                             (> row ignore-row)
                             ;; row and ignore-row are equal, so the col of the
                             ;; finding has to be before the col of the ignore
                             (>= (:col m) (:col ignore)))
                        (let [ignore-end-row (:end-row ignore)]
                          (when (or (< row ignore-end-row)
                                    (and (= row ignore-end-row)
                                         (<= (:end-col m) (:end-col ignore))))
                            (when (ignore-match? (:ignore ignore) type)
                              (reduced true))))))))
                nil
                ignores)))))

(defn reg-finding! [ctx m]
  (let [findings (:findings ctx)
        config (:config ctx)
        type (:type m)
        level (-> config :linters type :level)]
    ;; (prn m)
    (when (and level (not (identical? :off level)))
      (when-not (ignored? ctx m type)
        (let [m (assoc m :level level)]
          (swap! findings conj m)))))
  nil)

;;;; Scratch

(comment
  ;; (reg-finding! (atom nil) {})
  )
