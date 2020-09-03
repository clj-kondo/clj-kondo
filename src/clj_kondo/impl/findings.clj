(ns clj-kondo.impl.findings
  {:no-doc true})

(defn ignored? [ctx m]
  (let [ignores @(:ignores ctx)
        filename (:filename m)]
    (when-let [ignores (get ignores filename)]
      (let [row (:row m)]
        (some (fn [ignore]
                (let [ignore-row (:row ignore)]
                  (and (or (> row ignore-row)
                           (and (= row ignore-row)
                                (>= (:col m) (:col ignore))))
                       (let [ignore-end-row (:end-row ignore)]
                         (or (< row ignore-end-row)
                             (or (and (= row ignore-end-row)
                                      (<= (:end-col m) (:end-col ignore)))))))))
              ignores)))))

(defn reg-finding! [ctx m]
  (let [findings (:findings ctx)
        config (:config ctx)
        type (:type m)
        level (-> config :linters type :level)]
    (when (and level (not (identical? :off level)))
      (when-not (ignored? ctx m)
        (let [m (assoc m :level level)]
          (swap! findings conj m)))))
  nil)

;;;; Scratch

(comment
  ;; (reg-finding! (atom nil) {})
  )
