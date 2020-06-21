# HugSQL

The [HugSQL](https://github.com/layerware/hugsql) macro `def-db-fns` introduces
vars into the namespace in which it is called. You can teach clj-kondo about
these vars by using `declare`. Example:

``` clojure
(ns foo.db
  (:require [hugsql.core :as hugsql]))

(declare select-things)

;; this will define a var #'select-things:
(hugsql/def-db-fns "select_things.sql")

(defn get-my-things [conn params]
  (select-things conn params))
```

If the amount of symbols introduced by HugSQL becomes too unwieldy, consider
introducing a separate namespace in which HugSQL generates the vars:

``` clojure
(ns foo.db.hugsql
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "select_things.sql")
```

You can then use this namespace like:

``` clojure
(ns foo.db
  (:require [foo.db.hugsql :as sql]))

(defn get-my-things [conn params]
  (sql/select-things conn params))
```

and clj-kondo will not complain about this.
