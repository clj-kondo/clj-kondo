(ns clj-kondo.impl.findings
  {:no-doc true})

(defn reg-finding! [findings m]
  (swap! findings conj m)
  nil)

;;;; Scratch

(comment
  ;; (reg-finding! (atom nil) {})
  )
