(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.parser.utils
  (:require [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader.reader-types :as r]))

(defn whitespace?
  "Check if a given character is a whitespace."
  [^java.lang.Character c]
  (and c (or (= c \,) (Character/isWhitespace c))))

(defn linebreak?
  "Check if a given character is a linebreak."
  [^java.lang.Character c]
  (and c (or (= c \newline) (= c \return))))

(defn space?
  "Check if a given character is a non-linebreak whitespace."
  [^java.lang.Character c]
  (and (not (linebreak? c)) (whitespace? c)))

(defn ignore
  "Ignore next character of Reader."
  [reader]
  (r/read-char reader)
  nil)

(defn throw-reader
  "Throw reader exception, including line/column."
  [reader fmt & data]
  (let [c (r/get-column-number reader)
        l (r/get-line-number reader)]
    (throw
     (Exception.
      (str (apply format fmt data)
           " [at line " l ", column " c "]")))))

(defn read-eol
  [reader]
  (loop [char-seq []]
    (if-let [c (r/read-char reader)]
      (if (linebreak? c)
        (apply str (conj char-seq c))
        (recur (conj char-seq c)))
      (apply str char-seq))))
