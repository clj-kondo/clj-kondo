(ns clj-kondo.impl.types.clojure.core
  {:no-doc true})

(defmacro with-meta-fn [fn-expr]
  `(with-meta
     ~fn-expr
     {:form '~fn-expr}))

;; sorted in order of appearance in
;; https://github.com/clojure/clojure/blob/master/src/clj/clojure/core.clj

;; a lot of this work was already figured out here:
;; https://github.com/borkdude/speculative/blob/master/src/speculative/core.cljc

(def clojure-core
  {;; 16
   'list {:arities {:varargs {:ret :list}}}

   ;; 22
   'cons {:arities {2 {:args [:any :seqable]}}}
   ;; 49
   'first {:arities {1 {:args [:seqable]
                        :ret :any}}}
   ;; 181
   'assoc {:arities {3 {:args [:nilable/associative :any :any]
                        :ret :associative}
                     :varargs {:min-arity 3
                               :args '[:nilable/associative :any :any
                                       {:op :rest
                                        :spec [:any :any]}]
                               :ret :associative}}}
   ;; 544
   'str {:arities {:varargs {:args [{:op :rest
                                     :spec :any}]
                             :ret :string}}}
   ;; 922
   'inc {:arities {1 {:args [:number]}}
         :ret :number}
   ;; 947
   'reverse {:arities {1 {:args [:seqable]}}
             :ret :seqable-out}
   ;; 2327
   'atom {:ret :atom}
   ;; 2345
   'swap! {:arities {:varargs {:args '[:atom :ifn [{:op :rest
                                                    :spec :any}]]
                               :ret :any}}}
   ;; 2576
   'juxt {:arities {:varargs {:min-arity 0
                              :args [{:op :rest
                                      :spec :ifn}]
                              :ret :ifn}}}
   ;; 2727
   'map {:arities {1 {:args [:ifn]
                      :ret :transducer}
                   :varargs {:args '[:ifn :seqable [{:op :rest
                                                     :spec :seqable}]]
                             :ret :seqable-out}}}
   ;; 2793
   'filter {:arities {1 {:args [:ifn]
                         :ret :transducer}
                      2 {:args [:ifn :seqable]
                         :ret :seqable-out}}}
   ;; 2826
   'remove {:arities {1 {:args [:ifn]
                         :ret :transducer}
                      2 {:args [:ifn :seqable]
                         :ret :seqable-out}}}
   ;; 4105
   'set {:ret :set}
   ;; 4981
   'subs {:arities {2 {:args [:string :nat-int]
                       :ret :string}
                    3 {:args [:string :nat-int :nat-int]
                       :ret :string}}}
   ;; 6790
   'reduce {:arities {2 {:args [:ifn :seqable]
                         :ret :any}
                      3 {:args [:ifn :any :seqable]
                         :ret :any}}}
   ;; 6887
   'into {:arities {0 {:args []
                       :ret :coll}
                    1 {:args [:coll]}
                    2 {:args [:coll :seqable]}
                    3 {:args [:coll :transducer :seqable]}}
          :fn (with-meta-fn
                (fn [args]
                  (let [t (:tag (first args))]
                    (if (identical? :any t)
                      :coll
                      t))))}
   ;; 6903
   'mapv {:arities {1 {:args [:ifn]
                       :ret :transducer}
                    :varargs {:args '[:ifn :seqable {:op :rest
                                                     :spec :seqable}]
                              :ret :vector}}}
   ;; 7313
   'filterv {:arities {2 {:args [:ifn :seqable]
                          :ret :vector}}}
   ;; 7313
   'keep {:arities {1 {:args [:ifn]
                       :ret :transducer}
                    2 {:args [:ifn :seqable]
                       :ret :seqable-out}}}})
