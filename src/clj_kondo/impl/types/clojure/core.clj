(ns clj-kondo.impl.types.clojure.core
  {:no-doc true}
  (:require [clj-kondo.impl.types.utils :as tu]))

;; sorted in order of appearance in
;; https://github.com/clojure/clojure/blob/master/src/clj/clojure/core.clj

;; a lot of this work was already figured out here:
;; https://github.com/borkdude/speculative/blob/master/src/speculative/core.cljc

(def seqable->seq {:arities {1 {:args [:seqable]
                                :ret :seq}}})

(def seqable->boolean {:arities {1 {:args [:seqable]
                                :ret :boolean}}})

(def seqable->any {:arities {1 {:args [:seqable]
                                :ret :any}}})

(def any->boolean {:arities {1 {:ret :boolean}}})


;; arity-1 function that returns the same type
(def a->a {:arities {1 {:args [:any]}}
           :fn #(:tag (first %))})

(def number->number {:arities {1 {:args [:number]
                                  :ret :number}}})

(def number->number->number {:arities {2 {:args [:number :number]
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

(def int->int {:arities {1 {:args [:int]
                            :ret :int}}})

(def int->int->int {:arities {2 {:args [:int :int]
                                 :ret :int}}})

(def clojure-core
  {'if {:fn (fn [[_ then else]]
              (tu/union-type then else))}
   'let {:fn last}
   ;; 16
   'list {:arities {:varargs {:ret :list}}}
   ;; 22
   'cons {:arities {2 {:args [:any :seqable]}}}
   ;; 49
   'first seqable->any
   ;; 57
   'next seqable->seq
   ;; 66
   'rest seqable->seq
   ;; 75
   'conj {:arities {0 {:args [:nilable/coll]
                       :ret :coll}
                    :varargs {:args [:nilable/coll {:op :rest :spec :any}]
                              :ret :coll}}}
   ;; 91
   'second seqable->any
   ;; 98
   'ffirst seqable->any
   ;; 105
   'nfirst seqable->seq
   ;; 112
   'fnext seqable->any
   ;; 119
   'nnext seqable->seq
   ;; 126
   'seq {:arities {1 {:args [:seqable]
                      :ret :seq}}}
   ;; 139
   'instance? any->boolean
   ;; 146
   'seq? any->boolean
   ;; 153
   'char? any->boolean
   ;; 160
   'string? any->boolean
   ;; 167
   'map? any->boolean
   ;; 181
   'assoc {:arities {3 {:args [:nilable/associative :any :any]}
                     :varargs {:min-arity 3
                               :args '[:nilable/associative :any :any
                                       {:op :rest
                                        :spec [:any :any]}]}}
           :fn (fn [args]
                 (let [farg (first args)]
                   (if-let [t (:tag farg)]
                     (case t
                       :map :map
                       :vector :vector
                       (if (and (map? t)
                                (identical? :map (:type t)))
                         t
                         :associative))
                     :associative)))}
   ;; 202
   'meta {:arities {1 {:ret :nilable/map}}}
   ;; 211
   'with-meta a->a
   ;; 262
   'last seqable->any
   ;; 272
   'butlast seqable->seq
   ;; 283 'defn
   ;; 338 'to-array
   ;; 346 'cast
   ;; 353
   'vector {:arities {:varargs {:ret :vector}}}
   ;; 367
   'vec {:arities {1 {:ret :vector}}}
   ;; 379
   'hash-map {:arities {:varargs {:args [{:op :rest :spec [:any :any]}]
                                  :ret :map}}}
   ;; 389
   'hash-set {:arities {:varargs {:ret :set}}}
   ;; 398
   'sorted-map {:arities {:varargs {:ret :sorted-map}}}
   ;; 407 'sorted-map-by
   ;; 417 'sorted-set
   ;; 425 'sorted-set-by
   ;; 436
   'nil? any->boolean
   ;; 444 'defmacro
   ;; 493 'when
   'when {:fn (fn [args]
                (tu/union-type :nil (last args)))}
   ;; 499 'when-not
   'when-not {:fn (fn [args]
                    (tu/union-type :nil (last args)))}
   ;; 505
   'false? any->boolean
   ;; 512
   'true? any->boolean
   ;; 519
   'boolean? any->boolean
   ;; 524
   'not any->boolean
   ;; 531
   'some? any->boolean
   ;; 538
   'any? any->boolean
   ;; 544
   'str {:arities {:varargs {:args [{:op :rest
                                     :spec :any}]
                             :ret :string}}}
   ;; 562
   'symbol? any->boolean
   ;; 568
   'keyword? any->boolean
   ;; 574 'cond
   'cond {:fn (fn [args]
                (loop [args (seq args)
                       last-cond nil
                       rets []]
                  (if args
                    (let [next-cond (first args)
                          args (rest args)
                          next-ret (first args)
                          args (next args)]
                      (recur args next-cond (conj rets next-ret)))
                    (if (identical? :keyword (:tag last-cond))
                      (reduce tu/union-type #{} rets)
                      (reduce tu/union-type :nil rets)))))}
   ;; 589
   'symbol {:arities {1 {:args [#{:symbol :string :keyword}]
                         :ret :symbol}
                      2 {:args [:nilable/string :string]
                         :ret :symbol}}}
   ;; 604 'gensym
   ;; 614 'keyword
   'keyword {:arities {1 {:args [#{:symbol :string :keyword}]
                          :ret :keyword}
                       2 {:args [:nilable/string :string]
                          :ret :keyword}}}
   ;; 625 'find-keyword
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
   ;; 675 'vary-meta
   'lazy-seq {:arities {:varargs {:ret :seq}}}
   ;; 692 'chunk-buffer
   ;; 695 'chunk-append
   ;; 698 'chunk
   ;; 701 'chunk-first
   ;; 704 'chunk-rest
   ;; 707 'chunk-next
   ;; 710 'chunk-cons
   ;; 715
   'chunked-seq? any->boolean
   ;; 718
   'concat {:arities {:varargs {:args [{:op :rest :spec :seqable}]
                                :ret :seq}}}
   ;; 746 'delay
   ;; 755
   'delay? any->boolean
   ;; 761 'force
   ;; 767 'if-not
   ;; 775
   'identical? {:arities {2 {:ret :boolean}}}
   ;; 783
   '= {:arities {:varargs {:ret :boolean}}}
   ;; 819
   'not= {:arities {:varargs {:ret :boolean}}}
   ;; 831 'compare
   'compare {:arities {2 {:ret :number}}}
   ;; 842 'and
   'and {:fn (fn [args]
               (reduce tu/union-type :nil args))}
   ;; 854 'or
   'or {:fn (fn [args]
              (reduce tu/union-type #{} args))}
   ;; 867
   'zero? any->boolean
   ;; 874
   'count {:arities {1 {:args [:seqable]
                        :ret :number}}}
   ;; 882
   'int {:arities {1 {:args [#{:number :char}]
                      :ret :int}}}
   ;; 889
   'nth {:arities {2 {:args [:seqable :int]
                      :ret :any}
                   3 {:args [:seqable :int :any]
                      :ret :any}}}
   ;; 900
   '< compare-numbers
   ;; 915
   'inc' number->number
   ;; 922
   'inc number->number
   ;; 947
   'reverse {:arities {1 {:args [:seqable]}}
             :ret :seq}
   ;; 972
   '+' number*->number
   ;; 984
   '+ number*->number
   ;; 996
   '*' number*->number
   ;; 1008
   '* number*->number
   ;; 1020
   '/ number+->number
   ;; 1031
   '-' number+->number
   ;; 1043
   '- number+->number
   ;; 1055
   '<= compare-numbers
   ;; 1070
   '> compare-numbers
   ;; 1085
   '>= compare-numbers
   ;; 1100
   '== compare-numbers
   ;; 1115
   'max number+->number
   ;; 1125
   'min number+->number
   ;; 1135
   'dec' number->number
   ;; 1142
   'dec number->number
   ;; 1149
   'unchecked-inc-int int->int
   ;; 1156
   'unchecked-inc number->number
   ;; 1163
   'unchecked-dec-int int->int
   ;; 1170
   'unchecked-dec number->number
   ;; 1177
   'unchecked-negate-int int->int
   ;; 1184
   'unchecked-negate number->number
   ;; 1191
   'unchecked-add-int int->int->int
   ;; 1198
   'unchecked-add number->number->number
   ;; 1205
   'unchecked-subtract-int int->int->int
   ;; 1212
   'unchecked-subtract number->number->number
   ;; 1219
   'unchecked-multiply-int int->int->int
   ;; 1226
   'unchecked-multiply number->number->number
   ;; 1233
   'unchecked-divide-int int->int->int
   ;; 1240
   'unchecked-remainder-int int->int->int
   ;; 1247
   'pos? number->boolean
   ;; 1254
   'neg? number->boolean
   ;; 1261
   'quot number->number->number
   ;; 1269
   'rem number->number->number
   ;; 1277
   'rationalize number->number
   ;; 1286 'bit-not
   ;; 1293 'bit-and
   ;; 1302 'bit-or
   ;; 1311 'bit-xor
   ;; 1320 'bit-and-not
   ;; 1331 'bit-clear
   ;; 1337 'bit-set
   ;; 1343 'bit-flip
   ;; 1349 'bit-test
   ;; 1356 'bit-shift-left
   ;; 1362 'bit-shift-right
   ;; 1368 'unsigned-bit-shift-right
   ;; 1374
   'integer? any->boolean
   ;; 1386
   'even? number->boolean
   ;; 1394
   'odd? number->boolean
   ;; 1400
   'int? any->boolean
   ;; 1408
   'pos-int? any->boolean
   ;; 1414
   'neg-int? any->boolean
   ;; 1420
   'nat-int? any->boolean
   ;; 1426
   'double? any->boolean
   ;; 1433
   'complement {:arities {1 {:args [:ifn]
                             :ret :fn}}}
   ;; 1445
   'constantly {:arities {1 {:ret :fn}}}
   ;; 1451
   'identity a->a
   ;; 1459
   'peek {:arities {1 {:args [:stack]
                       :ret :any}}}
   ;; 1467
   'pop {:arities {1 {:args [:stack]
                      :ret :stack}}}
   ;; 1478
   'map-entry? any->boolean
   ;; 1484 'contains?
   'contains? {:arities {2 {:args [#{:associative :set :string} :any]
                            :ret :boolean}}}
   ;; NOTE: get is an any->any function on any object that implements ILookup.
   ;; 1494 'get
   ;; 1504
   'dissoc {:arities {:varargs {:args [:nilable/map {:op :rest :spec :any}]
                                :ret :nilable/map}}}
   ;; 1518 'disj
   ;; 1534
   'find {:arities {2 {:args [:nilable/associative :any]}}}
   ;; 1540
   'select-keys {:arities {2 {:args [:nilable/associative :seqable]
                              :ret :map}}}
   ;; 1555
   ;; NOTE: keys and vals can be called on seqs of MapEntry's, hence not :associative.
   'keys {:arities {1 {:args [:seqable]
                       :ret :seq}}}
   ;; 1561
   'vals {:arities {1 {:args [:seqable]
                       :ret :seq}}}
   ;; 1567 'key
   ;; 1574 'val
   ;; 1581
   'rseq {:arities {1 {:args [#{:vector :sorted-map}]
                      :req :seq}}}
   ;; 1589 'name
   ;; 1597
   'namespace {:arities {1 {:ret :string}}}
   ;; 1605
   'boolean any->boolean
   ;; 1612
   'ident? any->boolean
   ;; 1617
   'simple-ident? any->boolean
   ;; 1622
   'qualified-ident? any->boolean
   ;; 1627
   'simple-symbol? any->boolean
   ;; 1632
   'qualified-symbol? any->boolean
   ;; 1637
   'simple-keyword? any->boolean
   ;; 1642
   'qualified-keyword? any->boolean
   ;; 1647 'locking
   ;; 1659 '..
   ;; 1677 '->
   ;; 1693 '->>
   ;; 1723 'global-hierarchy
   ;; 1725 'defmulti
   ;; 1783 'defmethod
   ;; 1789 'remove-all-methods
   ;; 1796 'remove-method
   ;; 1803 'prefer-method
   ;; 1811 'methods
   ;; 1817 'get-method
   ;; 1824 'prefers
   ;; 1841 'if-let
   'if-let {:fn (fn [[_ then else]]
                  (tu/union-type then else))}
   ;; 1861 'when-let
   'when-let {:fn (fn [args]
                    (tu/union-type :nil (last args)))}
   ;; 1876 'if-some
   ;; 1896 'when-some
   ;; 1913 'push-thread-bindings
   ;; 1931 'pop-thread-bindings
   ;; 1939 'get-thread-bindings
   ;; 1947 'binding
   ;; 1973 'with-bindings*
   ;; 1986 'with-bindings
   ;; 1994 'bound-fn*
   ;; 2006 'bound-fn
   ;; 2015 'find-var
   ;; 2054 'agent
   ;; 2089 'set-agent-send-executor!
   ;; 2095 'set-agent-send-off-executor!
   ;; 2101 'send-via
   ;; 2111 'send
   ;; 2122 'send-off
   ;; 2133 'release-pending-sends
   ;; 2144 'add-watch
   ;; 2162 'remove-watch
   ;; 2169 'agent-error
   ;; 2177 'restart-agent
   ;; 2194 'set-error-handler!
   ;; 2204 'error-handler
   ;; 2212 'set-error-mode!
   ;; 2229 'error-mode
   ;; 2236 'agent-errors
   ;; 2246 'clear-agent-errors
   ;; 2254 'shutdown-agents
   ;; 2262 'ref
   ;; 2306 'deref
   ;; 2327
   'atom {:ret :atom}
   ;; 2345
   'swap! {:arities {:varargs {:args [:atom :ifn [{:op :rest
                                                   :spec :any}]]
                               :ret :any}}}
   ;; 2357 'swap-vals!
   ;; 2368 'compare-and-set!
   ;; 2376
   'reset! {:arities {2 {:args [:atom :any]
                         :ret :any}}}
   ;; 2383 'reset-vals!
   ;; 2389 'set-validator!
   ;; 2400 'get-validator
   ;; 2406 'alter-meta!
   ;; 2416 'reset-meta!
   ;; 2422 'commute
   ;; 2443 'alter
   ;; 2455 'ref-set
   ;; 2463 'ref-history-count
   ;; 2470 'ref-min-history
   ;; 2479 'ref-max-history
   ;; 2488 'ensure
   ;; 2498 'sync
   ;; 2512 'io!
   ;; 2525 'volatile!
   ;; 2532 'vreset!
   ;; 2539 'vswap!
   ;; 2548
   'volatile? any->boolean
   ;; 2557
   'comp {:arities {:varargs [{:op :rest
                               :spec :ifn}]
                    :ret :ifn}}
   ;; 2576
   'juxt {:arities {:varargs {:args [:ifn {:op :rest
                                           :spec :ifn}]
                              :ret :ifn}}}
   ;; 2614
   'partial {:arities {:varargs {:args [:ifn {:op :rest :spec :any}]
                                 :ret :ifn}}}
   ;; 2647 'sequence
   ;; 2672
   'every? {:arities {2 {:args [:ifn :seqable]
                         :ret :boolean}}}
   ;; 2684
   'not-every? {:arities {2 {:args [:ifn :seqable]
                             :ret :boolean}}}
   ;; 2693
   'some {:arities {2 {:args [:ifn :seqable]
                       :ret :any}}}
   ;; 2703
   'not-any? {:arities {2 {:args [:ifn :seqable]
                           :ret :boolean}}}
   ;; 2712 'dotimes
   ;; 2727
   'map {:arities {1 {:args [:ifn]
                      :ret :transducer}
                   :varargs {:args '[:ifn :seqable [{:op :rest
                                                     :spec :seqable}]]
                             :ret :seq}}}
   ;; 2776 'declare
   ;; 2781 'cat
   ;; 2783
   'mapcat {:arities {1 {:args [:ifn]
                         :ret :transducer}
                      :varargs {:args '[:ifn :seqable [{:op :rest
                                                        :spec :seqable}]]
                                :ret :seq}}}
   ;; 2793
   'filter {:arities {1 {:args [:ifn]
                         :ret :transducer}
                      2 {:args [:ifn :seqable]
                         :ret :seq}}}
   ;; 2826
   'remove {:arities {1 {:args [:ifn]
                         :ret :transducer}
                      2 {:args [:ifn :seqable]
                         :ret :seq}}}
   ;; 2836 'reduced
   ;; 2842
   'reduced? any->boolean
   ;; 2849 'ensure-reduced
   ;; 2855 'unreduced
   ;; 2861
   'take {:arities {1 {:args [:nat-int]
                       :ret :transducer}
                    2 {:args [:nat-int :seqable]
                       :ret :seq}}}
   ;; 2888
   'take-while {:arities {1 {:args [:ifn]
                             :ret :transducer}
                          2 {:args [:ifn :seqable]
                             :ret :seq}}}
   ;; 2909
   'drop {:arities {1 {:args [:nat-int]
                       :ret :transducer}
                    2 {:args [:nat-int :seqable]
                       :ret :seq}}}
   ;; 2934
   'drop-last {:arities {1 {:args [:seqable]
                            :ret :seq}
                         2 {:args [:nat-int :seqable]
                            :ret :seq}}}
   ;; 2941
   'take-last {:arities {2 {:args [:nat-int :seqable]
                            :ret :seq}}}
   ;; 2952
   'drop-while {:arities {1 {:args [:ifn]
                             :ret :transducer}
                          2 {:args [:ifn :seqable]
                             :ret :seq}}}
   ;; 2979
   'cycle seqable->seq
   ;; 2985
   'split-at {:arities {2 {:args [:nat-int :seqable]
                           :ret :vector}}}
   ;; 2992
   'split-with {:arities {2 {:args [:ifn :seqable]
                             :ret :vector}}}
   ;; 2999
   'repeat {:arities {1 {:args [:any]
                         :ret :seq}
                      2 {:args [:nat-int :any]}}}
   ;; 3006 'replicate (deprecated)
   ;; 3013
   'iterate {:arities {2 {:args [:ifn :any]
                          :ret :seq}}}
   ;; 3019
   'range {:arities {0 {:ret :seq}
                     1 {:args [:number]
                        :ret :seq}
                     2 {:args [:number :number]
                        :ret :seq}
                     3 {:args [:number :number :number]
                        :ret :seq}}}
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
   ;; 3085 'line-seq
   ;; 3094 'comparator
   ;; 3102 'sort
   ;; 3119 'sort-by
   ;; 3133 'dorun
   ;; 3148 'doall
   ;; 3164 'nthnext
   ;; 3174 'nthrest
   ;; 3184
   'partition {:arities {2 {:args [:int :seqable]
                            :ret :seq}
                         3 {:args [:int :int :seqable]
                            :ret :seq}
                         4 {:args [:int :int :seqable :seqable]
                            :ret :seq}}}
   ;; 3210 'eval
   ;; 3216 'doseq
   ;; 3274 'await
   ;; 3291 'await1
   ;; 3296 'await-for
   ;; 3313 'dotimes
   ;; 3342 'transient
   ;; 3349 'persistent!
   ;; 3358 'conj!
   ;; 3368 'assoc!
   ;; 3381 'dissoc!
   ;; 3392 'pop!
   ;; 3400 'disj!
   ;; 3425 'import
   ;; 3443 'into-array
   ;; 3460 'class
   ;; 3466 'type
   ;; 3473 'num
   ;; 3480 'long
   ;; 3486 'float
   ;; 3492 'double
   ;; 3498 'short
   ;; 3504 'byte
   'byte {:arities {1 {:args [#{:byte :number :char}]
                       :ret :byte}}}
   ;; 3510 'char
   ;; 3516 'unchecked-byte
   ;; 3522 'unchecked-short
   ;; 3528 'unchecked-char
   ;; 3534 'unchecked-int
   ;; 3540 'unchecked-long
   ;; 3546 'unchecked-float
   ;; 3552 'unchecked-double
   ;; 3559
   'number? any->boolean
   ;; 3566 'mod
   ;; 3576
   'ratio? any->boolean
   ;; 3582 'numerator
   ;; 3590 'denominator
   ;; 3598
   'decimal? any->boolean
   ;; 3604
   'float? any->boolean
   ;; 3612
   'rational? any->boolean
   ;; 3619 'bigint
   ;; 3633 'biginteger
   ;; 3647 'bigdec
   ;; 3663 'print-method
   ;; 3666 'print-dup
   ;; 3677 'pr
   ;; 3697 'newline
   ;; 3705 'flush
   ;; 3714 'prn
   ;; 3724 'print
   ;; 3733 'println
   ;; 3741 'read
   ;; 3770 'read+string
   ;; 3796 'read-line
   ;; 3805 'read-string
   ;; 3818
   'subvec {:arities {2 {:args [:vector :nat-int]
                         :ret :vector}
                      3 {:args [:vector :nat-int :nat-int]
                         :ret :vector}}}
   ;; 3831 'with-open
   ;; 3852 'doto
   ;; 3871 'memfn
   ;; 3884 'time
   ;; 3898 'alength
   ;; 3905 'aclone
   ;; 3912 'aget
   ;; 3923 'aset
   ;; 3986 'make-array
   ;; 4003 'to-array-2d
   ;; 4018 'macroexpand-1
   ;; 4026 'macroexpand
   ;; 4038 'create-struct
   ;; 4045 'defstruct
   ;; 4052 'struct-map
   ;; 4062 'struct
   ;; 4071 'accessor
   ;; 4082 'load-reader
   ;; 4089 'load-string
   ;; 4099
   'set? any->boolean
   ;; 4105
   'set {:ret :set}
   ;; 4126 'find-ns
   ;; 4132 'create-ns
   ;; 4140 'remove-ns
   ;; 4147 'all-ns
   ;; 4153 'the-ns
   ;; 4164 'ns-name
   ;; 4171 'ns-map
   ;; 4178 'ns-unmap
   ;; 4189 'ns-publics
   ;; 4200 'ns-imports
   ;; 4207 'ns-interns
   ;; 4217 'refer
   ;; 4254 'ns-refers
   ;; 4264 'alias
   ;; 4274 'ns-aliases
   ;; 4281 'ns-unalias
   ;; 4288
   'take-nth {:arities {1 {:args [:int]
                           :ret :transducer}
                        2 {:args [:int :seqable]
                           :ret :seq}}}
   ;; 4309 'interleave
   ;; 4327 'var-get
   ;; 4333 'var-set
   ;; 4340 'with-local-vars
   ;; 4359 'ns-resolve
   ;; 4372 'resolve
   ;; 4379 'array-map
   ;; 4389 'destructure
   ;; 4481 'let
   ;; 4513 'fn
   'fn {:arities {:varargs {:ret :fn}}}
   ;; 4575 'loop
   ;; 4600 'when-first
   ;; 4614 'lazy-cat
   ;; 4624 'for
   'for {:arities {2 {:ret :seq}}}
   ;; 4711 'comment
   ;; 4716 'with-out-str
   ;; 4727 'with-in-str
   ;; 4736 'pr-str
   ;; 4745 'prn-str
   ;; 4754 'print-str
   ;; 4763 'println-str
   ;; 4794 'ex-info
   'ex-info {:arities {2 {:args [:nilable/string :map]
                          :ret :throwable}
                       3 {:args [:nilable/string :map :any]
                          :ret :throwable}}}
   ;; 4803 'ex-data
   ;; 4800 'ex-message
   ;; 4808 'ex-cause
   ;; 4816 'assert
   ;; 4829 'test
   ;; 4839
   're-pattern {:arities {1 {:args [#{:string :regex}] ;; arg can also be a regex...
                             :ret :regex}}}
   ;; 4849 're-matcher
   ;; 4858 're-groups
   ;; 4874
   're-seq {:arities {2 {:args [:regex :string]
                         :ret :seq}}}
   ;; 4886
   're-matches {:arities {2 {:args [:regex :string]
                             :ret #{:vector :string}}}}
   ;; 4898
   're-find {:arities {1 {:args [:any] ;; matcher
                          :ret #{:vector :string}}
                       2 {:args [:regex :string]
                          :ret #{:vector :string}}}}
   ;; 4911 'rand
   ;; 4919 'rand-int
   ;; 4925 'defn-
   ;; 4931 'tree-seq
   'tree-seq {:arities {3 {:args [:ifn :ifn :any]
                           :ret :seq}}}
   ;; 4948 'file-seq
   ;; 4958 'xml-seq
   ;; 4968
   'special-symbol? any->boolean
   ;; 4975
   'var? any->boolean
   ;; 4981
   'subs {:arities {2 {:args [:string :nat-int]
                       :ret :string}
                    3 {:args [:string :nat-int :nat-int]
                       :ret :string}}}
   ;; 4989
   'max-key {:arities {:varargs {:args [:ifn :any {:op :rest :spec :any}]
                                 :ret :any}}}
   ;; 5009
   'min-key {:arities {:varargs {:args [:ifn :any {:op :rest :spec :any}]
                                 :ret :any}}}
   ;; 5029
   'distinct {:arities {0 {:args []
                           :ret :transducer}
                        1 {:args [:seqable]
                           :ret :seq}}}
   ;; 5058 'replace
   ;; 5076 'dosync
   ;; 5086 'with-precision
   ;; 5109 'subseq
   ;; 5126 'rsubseq
   ;; 5143 'repeatedly
   ;; 5152 'add-classpath
   ;; 5165 'hash
   ;; 5175 'mix-collection-hash
   ;; 5186 'hash-ordered-coll
   ;; 5195 'hash-unordered-coll
   ;; 5206
   'interpose {:arities {1 {:args [:any]
                            :ret :transducer}
                         2 {:args [:any :seqable]
                            :ret :seq}}}
   ;; 5229 'definline
   ;; 5241 'empty
   ;; 5249 'amap
   ;; 5265 'areduce
   ;; 5277 'float-array
   ;; 5285 'boolean-array
   ;; 5293 'byte-array
   ;; 5301 'char-array
   ;; 5309 'short-array
   ;; 5317 'double-array
   ;; 5325 'object-array
   ;; 5332 'int-array
   ;; 5340 'long-array
   ;; 5348 'booleans
   ;; 5353 'bytes
   ;; 5358 'chars
   ;; 5363 'shorts
   ;; 5368 'floats
   ;; 5373 'ints
   ;; 5378 'doubles
   ;; 5383 'longs
   ;; 5388
   'bytes? any->boolean
   ;; 5397 'seque
   ;; 5443
   'class? any->boolean
   ;; 5505 'alter-var-root
   ;; 5512 'bound?
   ;; 5520 'thread-bound?
   ;; 5528 'make-hierarchy
   ;; 5537 'not-empty
   ;; 5543 'bases
   ;; 5553 'supers
   ;; 5564 'isa?
   ;; 5585 'parents
   ;; 5598 'ancestors
   ;; 5614 'descendants
   ;; 5626 'derive
   ;; 5662 'flatten
   ;; 5664 'underive
   ;; 5685 'distinct?
   ;; 5702 'resultset-seq
   ;; 5721 'iterator-seq
   ;; 5731 'enumeration-seq
   ;; 5738 'format
   'format {:arities {:varargs {:args [:string {:op :rest :spec :any}]
                                :ret :string}}}
   ;; 5746 'printf
   ;; 5753 'gen-class
   ;; 5755 'with-loading-context
   ;; 5764 'ns
   ;; 5822 'refer-clojure
   ;; 5828 'defonce
   ;; 6007 'require
   ;; 6082 'requiring-resolve
   ;; 6093 'use
   ;; 6104 'loaded-libs
   ;; 6109 'load
   ;; 6128 'compile
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
   ;; 6206 'empty?
   'empty? seqable->boolean
   ;; 6213
   'coll? any->boolean
   ;; 6219
   'list? any->boolean
   ;; 6225
   'seqable? any->boolean
   ;; 6230
   'ifn? any->boolean
   ;; 6237
   'fn? any->boolean
   ;; 6244
   'associative? any->boolean
   ;; 6250
   'sequential? any->boolean
   ;; 6256
   'sorted? any->boolean
   ;; 6262
   'counted? any->boolean
   ;; 6268
   'reversible? any->boolean
   ;; 6274
   'indexed? any->boolean
   ;; 6279 '*1
   ;; 6284 '*2
   ;; 6289 '*3
   ;; 6294 '*e
   ;; 6299 'trampoline
   ;; 6317 'intern
   ;; 6333 'while
   ;; 6343 'memoize
   ;; 6359 'condp
   ;; 6530
   'future? any->boolean
   ;; 6536 'future-done?
   ;; 6543 'letfn
   ;; 6556
   'fnil {:arities {2 {:args [:ifn :any]
                       :ret :ifn}
                    3 {:args [:ifn :any :any]
                       :ret :ifn}
                    4 {:args [:ifn :any :any :any]
                       :ret :ifn}}}
   ;; 6697 'case
   ;; 6780 'Inst
   ;; 6780 'inst-ms*
   ;; 6787 'inst-ms
   ;; 6793
   'inst? any->boolean
   ;; 6805
   'uuid? any->boolean
   ;; 6810
   'reduce {:arities {2 {:args [:ifn :seqable]
                         :ret :any}
                      3 {:args [:ifn :any :seqable]
                         :ret :any}}}
   ;; 6847 'reduce-kv
   ;; 6858 'completing
   ;; 6870 'transduce
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
   ;; 6942 'slurp
   ;; 6954 'spit
   ;; 6963 'future-call
   ;; 6990 'future
   ;; 7000 'future-cancel
   ;; 7006 'future-cancelled?
   ;; 7012 'pmap
   ;; 7037 'pcalls
   ;; 7044 'pvalues
   ;; 7069 '*clojure-version*
   ;; 7081 'clojure-version
   ;; 7096 'promise
   ;; 7127 'deliver
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
                               :ret :seq}}}
   ;; 7203
   'frequencies {:arities {1 {:args [:seqable]
                              :ret :map}}}
   ;; 7214 'reductions
   ;; 7231 'rand-nth
   ;; 7240
   'partition-all {:arities {1 {:args [:int]
                                :ret :transducer}
                             2 {:args [:int :seqable]
                                :ret :seq}
                             3 {:args [:int :int :seqable]
                                :ret :seq}}}
   ;; 7274
   'shuffle {:arities {1 {:args [:coll]
                          :ret :coll}}}
   ;; 7283
   'map-indexed {:arities {1 {:args [:ifn]
                              :ret :transducer}
                           2 {:args [:ifn :seqable]
                              :ret :seq}}}
   ;; 7313
   'keep {:arities {1 {:args [:ifn]
                       :ret :transducer}
                    2 {:args [:ifn :seqable]
                       :ret :seq}}}
   ;; 7283
   'keep-indexed {:arities {1 {:args [:ifn]
                               :ret :transducer}
                            2 {:args [:ifn :seqable]
                               :ret :seq}}}
   ;; 7384 'bounded-count
   ;; 7396
   'every-pred {:arities {:varargs {:args [:ifn {:op :rest
                                                 :spec :ifn}]
                                    :ret :ifn}}}
   ;; 7436
   'some-fn {:arities {:varargs {:args [:ifn {:op :rest
                                              :spec :ifn}]
                                 :ret :ifn}}}
   ;; 7498 'with-redefs-fn
   ;; 7518 'with-redefs
   ;; 7533 'realized?
   ;; 7538 'cond->
   ;; 7555 'cond->>
   ;; 7572 'as->
   ;; 7584 'some->
   ;; 7598 'some->>
   ;; 7619 'cat
   ;; 7631 'halt-when
   ;; 7655
   'dedupe {:arities {0 {:args []
                         :ret :transducer}
                      1 {:args [:seqable]
                         :ret :seq}}}
   ;; 7673 'random-sample
   ;; 7682 'Eduction
   ;; 7682 '->Eduction
   ;; 7694 'eduction
   ;; 7710 'run!
   ;; 7719
   'tagged-literal? any->boolean
   ;; 7725 'tagged-literal
   ;; 7732
   'reader-conditional? any->boolean
   ;; 7738 'reader-conditional
   ;; 7750 'default-data-readers
   ;; 7758 '*data-readers*
   ;; 7787 '*default-data-reader-fn*
   ;; 7845
   'uri? any->boolean
   ;; 7868 'add-tap
   ;; 7879 'remove-tap
   ;; 7886 'tap>
   })

(def cljs-core
  (assoc clojure-core
         'keyword {:arities {1 {:args [#{:string :keyword :symbol}]
                                :ret :keyword}
                             2 {:args [#{:nilable/string :keyword :symbol}
                                       #{:string :keyword :symbol}]
                                :ret :keyword}}}))
