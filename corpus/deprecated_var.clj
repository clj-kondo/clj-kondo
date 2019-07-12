(ns deprecated-var)

(ns foo.foo) (defn ^:deprecated deprecated-fn [])
(ns foo.bar
  (:require [foo.foo :refer [deprecated-fn]]))
(deprecated-fn)

(ns foo.baz
  (:require [foo.foo :refer [deprecated-fn]]))
(deprecated-fn)

(ns bar.bar
  (:require [foo.foo :refer [deprecated-fn]]))
(deprecated-fn)
