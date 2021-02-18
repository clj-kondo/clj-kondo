(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.parser.token
  (:require [clj-kondo.impl.rewrite-clj
             [node :as node]
             [reader :as r]]))

(def invalid-token-exceptions (atom []))

(defn- read-to-boundary
  [reader & [allowed]]
  (let [allowed? (set allowed)]
    (r/read-until
      reader
      #(and (not (allowed? %))
            (r/whitespace-or-boundary? %)))))

(defn- read-to-char-boundary
  [reader]
  (let [c (r/next reader)]
    (str c
         (if (not= c \\)
           (read-to-boundary reader)
           ""))))

(defn- symbol-node
  "Symbols allow for certain boundary characters that have
   to be handled explicitly."
  [reader value value-string]
  (let [suffix (read-to-boundary
                 reader
                 [\' \:])]
    (if (empty? suffix)
      (node/token-node value value-string)
      (let [s (str value-string suffix)]
        (node/token-node
          (r/string->edn s)
          s)))))

(defn parse-token
  "Parse a single token."
  [reader]
  (try
    (let [first-char (r/next reader)
          s (->> (if (= first-char \\)
                   (read-to-char-boundary reader)
                   (read-to-boundary reader))
                 (str first-char))
          v (r/string->edn s)]
      (if (symbol? v)
        (symbol-node reader v s)
        (node/token-node v s)))
    (catch Exception e (swap! invalid-token-exceptions conj e) reader)))
