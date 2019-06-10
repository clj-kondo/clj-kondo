(ns defmulti)

(defmulti greeting
  (fn [x] (x "language")))

(defmethod greetingx "English" [x y]
  x)
