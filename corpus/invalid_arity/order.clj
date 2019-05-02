(ns corpus.invalid-arity.order)

;; call to def special form with docstring
(def x "the number one" 1)
(defmacro def [k spec-form])
;; valid call to macro
(corpus.invalid-arity.order/def ::foo int?)
;; invalid call to macro
(corpus.invalid-arity.order/def ::foo int? string?)
