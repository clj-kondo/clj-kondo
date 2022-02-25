(ns case)

(def fn-name 'select-keys)

(case fn-name
  (select-keys filter)
  (filter 1 2 3)  ;; invalid
  (odd? pos? neg?)
  (filter 1 2 3) ;; invalid
  )

(case fn-name
  (select-keys filter)
  (filter 1 2 3)  ;; invalid
  (odd? 1 2) ;; invalid
  )
