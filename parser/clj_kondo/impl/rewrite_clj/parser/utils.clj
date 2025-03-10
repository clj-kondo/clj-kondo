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
  (let [f {:row (r/get-line-number reader)
           :col (r/get-column-number reader)
           :message (apply format fmt data)}]
    (throw (ex-info "Syntax error" {:findings [f]}))))

(defn read-eol
  [reader]
  (loop [char-seq []]
    (if-let [c (r/read-char reader)]
      (if (linebreak? c)
        (apply str (conj char-seq c))
        (recur (conj char-seq c)))
      (apply str char-seq))))

(defn flush-into
  "Flush buffer and add string to the given vector."
  [lines ^StringBuffer buf]
  (let [s (str buf)]
    (.setLength buf 0)
    (conj lines s)))

(def valid-escape-chars #{\t \b \n \r \f \' \" \\})

(defn read-string-data
  [reader]
  (ignore reader)
  (let [buf (StringBuffer.)]
    (loop [escape? false
           lines []]
      (if-let [c (r/read-char reader)]
        (do
          (when escape?
            (when-not (contains? valid-escape-chars c)
              (throw-reader reader "Unsupported escape character: %s" (pr-str c))))
          (cond (and (not escape?) (= \" c))
                (flush-into lines buf)

                (= \newline c)
                (recur escape? (flush-into lines buf))

                :else
                (do
                  (.append buf c)
                  (recur (and (not escape?) (= \\ c)) lines))))
        (throw-reader reader "Unexpected EOF while reading string.")))))
