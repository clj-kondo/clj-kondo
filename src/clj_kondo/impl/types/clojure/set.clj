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
                        :ret :nilable/set}}}
   'select {:arities {2 {:args [:ifn :nilable/set]
                         :ret :nilable/set}}}
   'project {:arities {2 {:args [:seqable :sequential]
                          :ret :set}}}
   'rename-keys {:arities {2 {:args [:nilable/map :nilable/map]
                              :ret :nilable/map}}}
   'rename {:arities {2 {:args [:seqable :nilable/map]
                         :ret :set}}}
   'index {:arities {2 {:args [:seqable :seqable]
                        :ret :map}}}
   'map-invert {:arities {2 {:args [:seqable]
                             :ret :map}}}
   'join {:arities {2 {:args [:seqable :seqable]
                       :ret :set}
                    3 {:args [:seqable :seqable :nilable/map]
                       :ret :set}}}
   'subset? {:arities {2 {:args [:nilable/set :nilable/set]
                          :ret :boolean}}}
   'superset? {:arities {2 {:args [:nilable/set :nilable/set]
                            :ret :boolean}}}})
