(ns defprotocol)

(defprotocol ^{:added "1.3"} Foo
  "This is my protocol"
  (^{:added "1.3"} -foo [this] [this x] [this x y] "foo docs"))

(extend-protocol Foo
  nil
  (-foo ([this]) ([this x]) ([this x y])))

(-foo nil)
(-foo nil 1)
(-foo nil 1 2)
(-foo nil 1 2 3) ;; wrong

;; #314:
(defprotocol IntoInputStream
  (into-input-stream ^java.io.InputStream [this]))

(ns useprotocol (:require [defprotocol :as protocols]))

;; #314:
(fn [x]
  (some-> x protocols/into-input-stream clojure.core/slurp))
