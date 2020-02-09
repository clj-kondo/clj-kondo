(ns clj-kondo.impl.findings
  {:no-doc true})

(defn reg-finding! [ctx m]
  (let [findings (:findings ctx)
        config (:config ctx)
        type (:type m)
        level (-> config :linters type :level)]
    (when-not (identical? :off m)
      (let [m (assoc m :level level)]
        (swap! findings conj m))))
  nil)

;;;; Scratch

(comment
  ;; (reg-finding! (atom nil) {})
  )
