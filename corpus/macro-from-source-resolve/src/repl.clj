(ns repl)

(declare ^:clj-kondo/macroexpand-hook catch-all)

(defn ^:clj-kondo/macroexpand-hook -expand-catch-all
  [env [_ bind & body]]
  (assert (simple-symbol? bind))
  `(catch ~(if (:ns env) :default 'java.lang.Exception) ~bind
     ~@body))

(defn ^:clj-kondo/macroexpand-hook -catch-all?
  [form]
  (and
   (seq? form)
   (symbol? (first form))
   ;; throws instead of returning nil so a resolution regression fails loudly
   (if-let [v (resolve (first form))]
     (and
      (var? v)
      (= (symbol v) `catch-all))
     (throw (Exception. (str {:form (str form) :ns *ns* :catch-all `catch-all}))))))

(defmacro ^:clj-kondo/macroexpand-hook try+
  [& body]
  `(try ~@(for [f body]
            (if-not (-catch-all? f)
              f
              (-expand-catch-all &env f)))))
