(ns clj-kondo.impl.types.clojure.string
  {:no-doc true})

(def clojure-string
  {;; 75
   'replace
   {:arities {3 {:args [:char-sequence #{:string :char :regex} #{:string :char :ifn}]
                 :ret :string}}}
   ;; 180
   'join
   {:arities {1 {:args [:seqable]
                 :ret :string}
              2 {:args [:any :seqable]
                 :ret :string}}}
   ;; 360
   'starts-with?
   {:arities {2 {:args [:char-sequence :string]
                 :ret :boolean}}}
   ;; 366
   'ends-with?
   {:arities {2 {:args [:char-sequence :string]
                 :ret :boolean}}}
   ;; 372
   'includes?
   {:arities {2 {:args [:char-sequence :char-sequence]
                 :ret :boolean}}}})
