(ns corpus.cljc.test-cljc)

#?(:clj (defmacro foo [x y]
          x)
   :cljs (defmacro foo [x] ;; self-hosted macro? :-)
           x))

;; valid calls on lines 9 and 10:
#?(:clj (foo 1 2)
   :cljs (foo 1))

;; invalid calls on lines 13 and 14:
#?(:clj (foo 1)
   :cljs (foo 1 2))

;; bar is function that is callable from both CLJ and CLJS:
(defn bar [x]
  x)

(bar 1)
(bar 1 2 3)
