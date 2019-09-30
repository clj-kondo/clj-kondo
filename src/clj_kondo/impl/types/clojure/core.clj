(ns clj-kondo.impl.types.clojure.core
  {:no-doc true})

;; sorted in order of appearance in
;; https://github.com/clojure/clojure/blob/master/src/clj/clojure/core.clj

;; a lot of this work was already figured out here:
;; https://github.com/borkdude/speculative/blob/master/src/speculative/core.cljc

(def seqable->seqable {:arities {1 {:args [:seqable]
                                    :ret :seqable-out}}})

(def seqable->any {:arities {1 {:args [:seqable]
                                :ret :any}}})

(def any->boolean {:arities {1 {:args [:any]
                                :ret :boolean}}})

(def number->number {:arities {1 {:args [:number]
                                  :ret :number}}})

(def number*->number {:arities {:varargs {:args [{:op :rest :spec :number}]
                                          :ret :number}}})

(def number+->number {:arities {:varargs {:args [:number {:op :rest :spec :number}]
                                          :ret :number}}})

(def number->boolean {:arities {:varargs {:args [:number]
                                          :ret :boolean}}})

(def compare-numbers {:arities {:varargs {:args [{:op :rest
                                                  :spec :number}]
                                          :ret :boolean}}})

(def clojure-core
  {;; 16
   'list {:arities {:varargs {:ret :list}}}
   ;; 22
   'cons {:arities {2 {:args [:any :seqable]}}}
   ;; 49
   'first seqable->any
   ;; 57
   'next seqable->seqable
   ;; 66
   'rest seqable->seqable
   ;; 75
   'conj {:arities {0 {:args [:coll]
                       :ret :coll}
                    :varargs {:args [:coll {:op :rest :spec :any}]
                              :ret :coll}}}
   ;; 126
   'seq {:arities {1 {:args [:seqable]
                      :ret :seq}}}
   ;; 181
   'assoc {:arities {3 {:args [:nilable/associative :any :any]}
                     :varargs {:min-arity 3
                               :args '[:nilable/associative :any :any
                                       {:op :rest
                                        :spec [:any :any]}]}}
           :fn (fn [args]
                 (let [t (:tag (first args))]
                   (if (identical? :any t)
                     :associative
                     t)))}
   ;; 262
   'last seqable->any
   ;; 353
   'vector {:arities {:varargs {:ret :vector}}}
   ;; 367
   'vec {:arities {1 {:ret :vector}}}
   ;; 379
   'hash-map {:arities {:varargs {:args [{:op :rest :spec [:any :any]}]
                                  :ret :map}}}
   ;; 389
   'hash-set {:arities {:varargs {:args [{:op :rest :spec [:any :any]}]
                                  :ret :set}}}
   ;; 436
   'nil? any->boolean
   ;; 524
   'not any->boolean
   ;; 531
   'some? any->boolean
   ;; 544
   'str {:arities {:varargs {:args [{:op :rest
                                     :spec :any}]
                             :ret :string}}}
   ;; 648
   'list* {:arities {:varargs {:args [{:op :rest
                                       :spec :any
                                       :last :seqable}]
                               :ret :list}}}
   ;; 660
   'apply {:arities {:varargs {:args [:ifn {:op :rest
                                            :spec :any
                                            :last :seqable}]
                               :ret :any}}}
   ;; 718
   'concat {:arities {:varargs {:args [{:op :rest :spec :seqable}]
                                :ret :seqable}}}
   ;; 783
   '= {:arities {:varargs {:args [:any {:op :rest :spec :any}]
                           :ret :boolean}}}
   ;; 874
   'count {:arities {1 {:args [:seqable]
                        :ret :number}}}
   ;; 889
   'nth {:arities {2 {:args [:seqable :int]
                      :ret :any}
                   3 {:args [:seqable :int :any]
                      :ret :any}}}
   ;; 900
   '< compare-numbers
   ;; 922
   'inc number->number
   ;; 947
   'reverse {:arities {1 {:args [:seqable]}}
             :ret :seqable-out}
   ;; 984
   '+ number*->number
   ;; 1008
   '* number*->number
   ;; 1020
   '/ number+->number
   ;; 1043
   '<= compare-numbers
   '- number+->number
   ;; 1070
   '> compare-numbers
   ;; 1085
   '>= compare-numbers
   ;; 1100
   '== compare-numbers
   ;; 1142
   'dec number->number
   ;; 1115
   'max number+->number
   ;; 1125
   'min number+->number
   ;; 1247
   'pos? number->boolean
   ;; 1254
   'neg? number->boolean
   ;; 1459
   'peek {:arities {1 {:args [:vector]
                       :ret :any}}}
   ;; 1467
   'pop {:arities {1 {:args [:vector]
                      :ret :vector}}}
   ;; 1504
   'dissoc {:arities {:varargs {:args [:map {:op :rest :spec :any}]
                                :ret :map}}}
   ;; 1534
   'find {:arities {2 {:args [:associative :any]}}}
   ;; 1540
   'select-keys {:arities {2 {:args [:associative :seqable]
                              :ret :map}}}
   ;; 1555
   'keys {:arities {1 {:args [:seqable]
                       :ret :seqable}}}
   ;; 1561
   'vals {:arities {1 {:args [:seqable]
                       :ret :seqable}}}
   ;; 1589
   ;;'name TODO
   ;; 2327
   'atom {:ret :atom}
   ;; 2345
   'swap! {:arities {:varargs {:args [:atom :ifn [{:op :rest
                                                   :spec :any}]]
                               :ret :any}}}
   ;; 2376
   'reset! {:arities {2 {:args [:atom :any]
                         :ret :any}}}
   ;; 2557
   'comp {:arities {:varargs [{:op :rest
                               :spec :ifn}]
                    :ret :ifn}}
   ;; 2576
   'juxt {:arities {:varargs {:args [:ifn {:op :rest
                                           :spec :ifn}]
                              :ret :ifn}}}
   ;; 2672
   'every? {:arities {2 {:args [:ifn :seqable]
                         :ret :boolean}}}
   ;; 2684
   'not-every? {:arities {2 {:args [:ifn :seqable]
                             :ret :boolean}}}
   ;; 2614
   'partial {:arities {:varargs {:args [:ifn {:op :rest :spec :any}]
                                 :ret :ifn}}}
   ;; 2693
   'some {:arities {2 {:args [:ifn :seqable]
                       :ret :any}}}
   ;; 2703
   'not-any? {:arities {2 {:args [:ifn :seqable]
                           :ret :boolean}}}
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
   ;; 3019
   'range {:arities {0 {:ret :seqable}
                     1 {:args [:number]
                        :ret :seqable}
                     2 {:args [:number :number]
                        :ret :seqable}
                     3 {:args [:number :number :number]
                        :ret :seqable}}}
   ;; 3041
   'merge {:arities {:varargs {:args [{:op :rest
                                       :spec :seqable}]
                               :ret :nilable/map}}}
   ;; 3051
   'merge-with {:arities {:varargs {:args [:ifn {:op :rest
                                                 :spec :seqable}]
                                    :ret :nilable/map}}}
   ;; 3071
   'zipmap {:arities {2 {:args [:seqable :seqable]
                         :ret :map}}}
   ;; 3184
   'partition {:arities {2 {:args [:int :seqable]
                            :ret :seqable}
                         3 {:args [:int :int :seqable]
                            :ret :seqable}
                         4 {:args [:int :int :seqable :seqable]
                            :ret :seqable}}}
   ;; 4105
   'set {:ret :set}
   ;; 4288
   'take-nth {:arities {1 {:args [:int]
                           :ret :transducer}
                        2 {:args [:int :seqable]
                           :ret :seqable}}}
   ;; 4839
   're-pattern {:arities {1 {:args [#{:string :regex}] ;; arg can also be a regex...
                             :ret :regex}}}
   ;; 4874
   're-seq {:arities {2 {:args [:regex :string]
                         :ret :seqable}}}
   ;; 4886
   're-matches {:arities {2 {:args [:regex :string]
                             :ret :seqable}}}
   ;; 4898
   're-find {:arities {1 {:args [:any] ;; matcher
                          :ret :seqable}
                       2 {:args [:regex :string]
                          :ret :seqable}}}
   ;; 4981
   'subs {:arities {2 {:args [:string :nat-int]
                       :ret :string}
                    3 {:args [:string :nat-int :nat-int]
                       :ret :string}}}
   ;; 4898
   'max-key {:arities {:varargs {:args [:ifn :any {:op :rest :spec :any}]
                                 :ret :any}}}
   ;; 5009
   'min-key {:arities {:varargs {:args [:ifn :any {:op :rest :spec :any}]
                                 :ret :any}}}
   ;; 5029
   'distinct {:arities {0 {:args []
                           :ret :transducer}
                        1 {:args [:seqable]
                           :ret :seqable}}}
   ;; 5206
   'interpose {:arities {1 {:args [:any]
                            :ret :transducer}
                         2 {:args [:any :seqable]
                            :ret :seqable}}}
   ;; 6142
   'get-in {:arities {2 {:args [:nilable/associative :seqable]
                         :ret :any}
                      3 {:args [:nilable/associative :seqable :any]
                         :ret :any}}}
   ;; 6152
   'assoc-in {:arities {3 {:args [:nilable/associative :seqable :any]
                           :ret :associative}}}
   ;; 6172
   'update-in {:arities {:varargs {:args [:nilable/associative :seqable :ifn {:op :rest :spec :any}]
                                   :ret :associative}}}
   ;; 6188
   'update {:arities {:varargs {:args [:nilable/associative :any :ifn {:op :rest :spec :any}]
                                :ret :associative}}}
   ;; 6536
   'fnil {:arities {2 {:args [:ifn :any]
                       :ret :ifn}
                    3 {:args [:ifn :any :any]
                       :ret :ifn}
                    4 {:args [:ifn :any :any :any]
                       :ret :ifn}}}
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
          :fn (fn [args]
                (let [t (:tag (first args))]
                  (if (identical? :any t)
                    :coll
                    t)))}
   ;; 6903
   'mapv {:arities {1 {:args [:ifn]
                       :ret :transducer}
                    :varargs {:args '[:ifn :seqable {:op :rest
                                                     :spec :seqable}]
                              :ret :vector}}}
   ;; 6921
   'filterv {:arities {2 {:args [:ifn :seqable]
                          :ret :vector}}}
   ;; 7136
   'flatten {:arities {1 {:args [:nilable/sequential]
                          :ret :sequential}}}
   ;; 7146
   'group-by {:arities {2 {:args [:ifn :seqable]
                           :ret :map}}}
   ;; 7160
   'partition-by {:arities {1 {:args [:ifn]
                               :ret :transducer}
                            2 {:args [:ifn :seqable]
                               :ret :seqable}}}
   ;; 7203
   'frequencies {:arities {1 {:args [:seqable]
                              :ret :map}}}
   ;; 7240
   'partition-all {:arities {1 {:args [:int]
                                :ret :transducer}
                             2 {:args [:int :seqable]
                                :ret :seqable}
                             3 {:args [:int :int :seqable]
                                :ret :seqable}}}
   ;; 7274
   'shuffle {:arities {1 {:args [:coll]
                          :ret :coll}}}
   ;; 7283
   'map-indexed {:arities {1 {:args [:ifn]
                              :ret :transducer}
                           2 {:args [:ifn :seqable]
                              :ret :seqable}}}
   ;; 7313
   'keep {:arities {1 {:args [:ifn]
                       :ret :transducer}
                    2 {:args [:ifn :seqable]
                       :ret :seqable-out}}}
   ;; 7283
   'keep-indexed {:arities {1 {:args [:ifn]
                               :ret :transducer}
                            2 {:args [:ifn :seqable]
                               :ret :seqable}}}
   ;; 7396
   'every-pred {:arities {:varargs {:args [:ifn {:op :rest
                                                 :spec :ifn}]
                                    :ret :ifn}}}
   ;; 7436
   'some-fn {:arities {:varargs {:args [:ifn {:op :rest
                                              :spec :ifn}]
                                 :ret :ifn}}}
   ;; 7655
   'dedupe {:arities {0 {:args []
                         :ret :transducer}
                      1 {:args [:seqable]
                         :ret :seqable}}}})
