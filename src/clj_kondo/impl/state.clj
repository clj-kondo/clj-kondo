(ns clj-kondo.impl.state
  {:no-doc true})

(defonce findings (atom []))

(defn reg-finding! [m]
  (swap! findings conj m))

(defn reg-findings! [maps]
  (swap! findings into maps))

(defn clear-findings! []
  (reset! findings []))

;;;; Scratch

(comment
  @findings

  )
