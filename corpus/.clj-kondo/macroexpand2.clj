(ns macroexpand2)

(defmacro $ [op & args]
  (list* (symbol (str "." op)) 'sh args))

(defmacro form-env-macro [_]
  (list* 'clojure.core/+
         (list* 'clojure.core/+
                [(when (= 'form-env-macro (first &form)) "foo")
                 (when (contains? &env 'x) :foo)])))

(defmacro private-defn [sym]
  `(defn ~(with-meta sym {:private true}) []
     ;; redundant stuff that should not be reported :)
     (do (let [] (let [])))))
