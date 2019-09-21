(ns clj-kondo.impl.types.clojure.string)

(def clojure-string
  {'join
   {:arities {1 {:args [:seqable]
                 :ret :string}
              2 {:args [:any :seqable]
                 :ret :string}}}
   'starts-with?
   {:arities {2 {:args [:char-sequence :string]
                 :ret :boolean}}}
   'ends-with?
   {:arities {2 {:args [:char-sequence :string]
                 :ret :boolean}}}
   'includes?
   {:arities {2 {:args [:char-sequence :char-sequence]
                 :ret :boolean}}}})
