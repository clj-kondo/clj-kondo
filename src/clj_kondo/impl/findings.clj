(ns clj-kondo.impl.findings
  {:no-doc true})

(defn reg-finding! [findings m]
  (swap! findings conj m)
  nil)

(defn reg-findings! [findings maps]
  (swap! findings into maps)
  nil)

;;;; Scratch

(comment
  )
