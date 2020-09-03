(ns clj-kondo.impl.findings
  {:no-doc true})

(defn ignored? [ctx m]
  (let [ignores @(:ignores ctx)
        filename (:filename m)
        row (:row m)]
    (when-let [ignores (get ignores filename)]
      (some (fn [ignore]
              (and (>= row (:row ignore))
                   (<= row (:end-row ignore))
                   ;; TODO: rest of positions
                   ))
            ignores))))

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
