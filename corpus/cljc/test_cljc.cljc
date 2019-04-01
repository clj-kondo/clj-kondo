(ns corpus.cljc.test-cljc)

#?(:clj (defn foo [x y]
          x)
   :cljs (defn foo [x]
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
