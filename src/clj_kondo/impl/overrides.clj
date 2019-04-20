(ns clj-kondo.impl.overrides
  {:no-doc true})

(defn overrides
  "Overrides var information."
  [idacs]
  (-> idacs
      (assoc-in '[:cljs :defs cljs.core array :var-args-min-arity] 0)
      (assoc-in '[:clj :defs clojure.core def] '{:ns clojure.core
                                                 :name def
                                                 :fixed-arities #{2 3}})
      (assoc-in '[:clj :defs clojure.core defn] '{:ns clojure.core
                                                  :name defn
                                                  :var-args-min-arity 2})
      (assoc-in '[:clj :defs clojure.core defn-] '{:ns clojure.core
                                                   :name defn-
                                                   :var-args-min-arity 2})
      (assoc-in '[:clj :defs clojure.core defmacro] '{:ns clojure.core
                                                      :name defmacro
                                                      :var-args-min-arity 2})))
