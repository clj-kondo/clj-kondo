(ns macroexpand2)

(defmacro $ [op & args]
  (list* (symbol (str "." op)) 'sh args))
