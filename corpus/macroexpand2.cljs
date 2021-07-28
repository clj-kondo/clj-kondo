(ns macroexpand2
  {:clj-kondo/config
   '{:hooks
     {:macroexpand {macroexpand2/$ macroexpand2/$}}}})

(def sh (js/require "shelljs"))

(defmacro $ [op & args]
  (list* (symbol (str "." op)) 'sh args))

(prn (str ($ which "git")))
(prn (str ($ pwd)))
($ cd  "..")
(-> ($ ls) prn)

;; so far no errors

($ which foobar) ;; foobar should be reported unresolved
