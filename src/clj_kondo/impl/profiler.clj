(ns clj-kondo.impl.profiler
  {:no-doc true})

(def profile? (= "true" (System/getenv "CLJ_KONDO_PROFILE")))

(defonce recordings (atom {}))
(def zplus (fnil + 0))

(defmacro profile [k & body]
  (if profile?
    `(let [t1# (System/currentTimeMillis)
           res# (do ~@body)
           t2# (System/currentTimeMillis)
           time-spent# (- t2# t1#)]
       (swap! recordings update ~k zplus time-spent#)
       res#)
    `(do ~@body)))

(defn print-profile [total-key]
  (when profile?
    (let [r @recordings
          total (get r total-key)]
      (doseq [[k v] (sort-by (comp - val) r)
              :when (not= total-key k)
              :let [percentage (int (* 100 (/ v total)))]
              :when (pos? percentage)]
        (println k v (str percentage "%"))))))
