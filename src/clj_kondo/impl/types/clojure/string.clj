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
   ;; 207
   'upper-case
   {:arities {1 {:args [:char-sequence]
                 :ret :string}}}
   ;; 213
   'lower-case
   {:arities {1 {:args [:char-sequence]
                 :ret :string}}}
   ;; 219
   'split
   {:arities {2 {:args [:char-sequence :regex]
                 :ret :vector}
              3 {:args [:char-sequence :regex :int]
                 :ret :vector}}}
   ;; 229
   'split-lines
   {:arities {1 {:args [:char-sequence]
                 :ret :vector}}}
   ;; 235
   'trim
   {:arities {1 {:args [:char-sequence]
                 :ret :string}}}
   ;; 252
   'triml
   {:arities {1 {:args [:char-sequence]
                 :ret :string}}}
   ;; 264
   'trimr
   {:arities {1 {:args [:char-sequence]
                 :ret :string}}}
   ;; 288
   'blank?
   {:arities {1 {:args [#{:nil :char-sequence}]
                 :ret :boolean}}}
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

(def cljs-string
  ;; JS String.split takes a string separator
  (-> clojure-string
      (assoc-in ['split :arities 2 :args 1] #{:regex :string})
      (assoc-in ['split :arities 3 :args 1] #{:regex :string})))
