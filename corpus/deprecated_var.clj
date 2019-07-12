(ns deprecated-var)

(ns foo.foo) (defn ^:deprecated deprecated-fn [])
(ns foo.bar
  (:require [foo.foo :refer [deprecated-fn]]))
(deprecated-fn) ;; <- unreported

(ns foo.baz
  (:require [foo.foo :refer [deprecated-fn]]))
(deprecated-fn) ;; <- reported
(defn allowed []
  (deprecated-fn)) ;; <- unreported
(defn ignore []
  (deprecated-fn)) ;; <- unreported

(ns bar.bar
  (:require [foo.foo :refer [deprecated-fn]]))
(deprecated-fn) ;; <- unreported
