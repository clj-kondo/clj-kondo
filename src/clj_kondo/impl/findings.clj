(ns clj-kondo.impl.findings
  {:no-doc true})

(defn reg-finding! [findings m]
  #_(when-not (and
               (:end-row m)
               (:end-col m))
    (prn ">" m))
  (swap! findings conj m)
  nil)

;;;; Scratch

(comment
  ;; (reg-finding! (atom nil) {})
  )
