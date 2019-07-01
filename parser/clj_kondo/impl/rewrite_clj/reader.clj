(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.reader
  (:refer-clojure :exclude [peek next])
  (:require [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader
             [edn :as edn]
             [reader-types :as r]]
            [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

;; ## Exception

(defn throw-reader
  "Throw reader exception, including line/column."
  [reader fmt & data]
  (let [c (r/get-column-number reader)
        l (r/get-line-number reader)]
    (throw
      (Exception.
        (str (apply format fmt data)
             " [at line " l ", column " c "]")))))

;; ## Decisions

(defn boundary?
  [c]
  "Check whether a given char is a token boundary."
  (contains?
    #{\" \: \; \' \@ \^ \` \~
      \( \) \[ \] \{ \} \\ nil}
    c))

(defn comma?
  [^java.lang.Character c]
  (= c \,))

(defn whitespace?
  [^java.lang.Character c]
  (and c
       (or (comma? c)
           (Character/isWhitespace c))))

(defn linebreak?
  [^java.lang.Character c]
  (contains? #{\newline \return} c))

(defn space?
  [^java.lang.Character c]
  (and c
       (Character/isWhitespace c)
       (not (contains? #{\newline \return \,} c))))

(defn whitespace-or-boundary?
  [c]
  (or (whitespace? c) (boundary? c)))

;; ## Helpers

(defn read-while
  "Read while the chars fulfill the given condition. Ignores
   the unmatching char."
  [reader p? & [eof?]]
  (let [buf (StringBuilder.)
        eof? (if (nil? eof?)
               (not (p? nil))
               eof?)]
    (loop []
      (if-let [c (r/read-char reader)]
        (if (p? c)
          (do
            (.append buf c)
            (recur))
          (do
            (r/unread reader c)
            (str buf)))
        (if eof?
          (str buf)
          (throw-reader reader "Unexpected EOF."))))))

(defn read-until
  "Read until a char fulfills the given condition. Ignores the
   matching char."
  [reader p?]
  (read-while
    reader
    (complement p?)
    (p? nil)))

(defn read-include-linebreak
  "Read until linebreak and include it."
  [reader]
  (str
    (read-until
      reader
      #(or (nil? %) (linebreak? %)))
    (r/read-char reader)))

(defn string->edn
  "Convert string to EDN value."
  [^String s]
  (edn/read-string s))

(defn ignore
  "Ignore the next character."
  [reader]
  (r/read-char reader)
  nil)

(defn next
  "Read next char."
  [reader]
  (r/read-char reader))

(defn unread
  "Unreads a char. Puts the char back on the reader."
  [reader ch]
  (r/unread reader ch))

(defn peek
  "Peek next char."
  [reader]
  (r/peek-char reader))

(defn position
  "Create map of `row-k` and `col-k` representing the current reader position."
  [reader row-k col-k]
  {row-k (r/get-line-number reader)
   col-k (r/get-column-number reader)})

(defn read-with-meta
  "Use the given function to read value, then attach row/col metadata."
  [reader read-fn]
  (loop [start-position (position reader :row :col)]
    (when-let [entry (read-fn reader)]
      (if (identical? reader entry)
        (recur (position reader :row :col))
        (let [end-position (position reader :end-row :end-col)
              new-meta (merge start-position end-position (meta entry))]
          (with-meta entry new-meta))))))

(defn read-repeatedly
  "Call the given function on the given reader until it returns
   a non-truthy value."
  [reader read-fn]
  (->> (repeatedly #(read-fn reader))
       (take-while identity)
       (doall)))

(defn read-n
  "Call the given function on the given reader until `n` values matching `p?` have been
   collected."
  [reader node-tag read-fn p? n]
  {:pre [(pos? n)]}
  (loop [c 0
         vs []]
    (if (< c n)
      (if-let [v (read-fn reader)]
        (recur
          (if (p? v) (inc c) c)
          (conj vs v))
        (throw-reader
          reader
          "%s node expects %d value%s."
          node-tag
          n
          (if (= n 1) "" "s")))
      vs)))

;; ## Reader Types

(defn string-reader
  "Create reader for strings."
  ^clj_kondo.impl.toolsreader.v1v2v2.clojure.tools.reader.reader_types.IndexingPushbackReader
  [s]
  (r/indexing-push-back-reader
    (r/string-push-back-reader s)))

(defn file-reader
  "Create reader for files."
  ^clj_kondo.impl.toolsreader.v1v2v2.clojure.tools.reader.reader_types.IndexingPushbackReader
  [f]
  (-> (io/file f)
      (io/reader)
      (PushbackReader. 2)
      (r/indexing-push-back-reader 2)))
