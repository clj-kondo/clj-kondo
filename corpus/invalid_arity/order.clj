(ns corpus.invalid-arity.order)

;; call to def special form with docstring
(def x "the number one" 1)
(defmacro def [k spec-form])
;; valid call to macro
(def ::foo int?)
;; invalid call to macro
(def ::foo int? string?)
