(ns definterface)

(definterface NodeManager
  (node [^java.util.concurrent.atomic.AtomicReference edit arr])
  (empty [])
  (array [node])
  (^java.util.concurrent.atomic.AtomicReference edit [node])
  (^boolean regular [node])
  (clone [^clojure.core.ArrayManager am ^int shift node]))

(defn foo [x]
  (aget ^objects x 0))
