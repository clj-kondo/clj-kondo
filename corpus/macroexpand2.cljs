(ns macroexpand2
  {:clj-kondo/config
   '{:hooks
     {:macroexpand {macroexpand2/$ macroexpand2/$
                    macroexpand2/form-env-macro macroexpand2/form-env-macro}}}})

(def sh (js/require "shelljs"))

(defmacro $ [op & args]
  (list* (symbol (str "." op)) 'sh args))

(prn (str ($ which "git")))
(prn (str ($ pwd)))
($ cd  "..")
(-> ($ ls) prn)

;; so far no errors

($ which foobar) ;; foobar should be reported unresolved

(defmacro form-env-macro [_]
  (list* 'clojure.core/+
         (list* 'clojure.core/+
                [(when (= 'form-env-macro (first &form)) "foo")
                 (when (contains? &env 'x) :foo)])))

(let [x 1]
  x
  ;; Expected number, received string
  ;; Expected number, received keyword
  (form-env-macro (inc x)))

(let [y 1]
  ;; Expected number, received nil (no x in scope)
  ;; Expected number, received keyword
  y
  (form-env-macro (inc x)))
