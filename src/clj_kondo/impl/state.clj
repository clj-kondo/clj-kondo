(ns clj-kondo.impl.state
  {:no-doc true})

#_(defonce findings (atom []))

(defn reg-finding! [findings m]
  (swap! findings conj m)
  nil)

(defn reg-findings! [findings maps]
  (swap! findings into maps)
  nil)

#_(defn clear-findings! []
  (reset! findings [])
  nil)

;;;; Scratch

(comment
  @findings

  )
