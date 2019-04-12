(ns case-without-default)

(def k :foo)

;; this one should be caught:
(case k :foo 1 :bar 2)

;; this one should not be caught:
(case k :foo 1 :bar 2 3)
