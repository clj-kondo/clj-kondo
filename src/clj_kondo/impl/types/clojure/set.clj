(ns clj-kondo.impl.types.clojure.set
  {:no-doc true})

(def clojure-set
  {'union
   {:arities {:varargs {:min-arity 0
                        :args '[{:op :rest
                                 :spec :nilable/set}]
                        :ret :nilable/set}}}
   'intersection
   {:arities {:varargs {:args '[:nilable/set {:op :rest
                                              :spec :nilable/set}]
                        :ret :nilable/set}}}
   'difference
   {:arities {:varargs {:args '[:nilable/set {:op :rest
                                              :spec :nilable/set}]
                        :ret :nilable/set}}}})
