(ns clj-kondo.impl.state
  {:no-doc true})

(defonce findings (atom []))

(defn reg-finding! [m]
  (swap! findings conj m)
  nil)

(defn reg-findings! [maps]
  (swap! findings into maps)
  nil)

(defn clear-findings! []
  (reset! findings [])
  nil)

;;;; Scratch

(comment
  @findings

  )
