(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.reader
  (:refer-clojure :exclude [peek next])
  (:require [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader
             [edn :as edn]
             [reader-types :as r]]
            [clj-kondo.impl.rewrite-clj.parser
              [utils :as u]]
            [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(def ^:dynamic *reader-exceptions* nil)
(def ^:dynamic *reader-features* nil)

;; ## Decisions

(defn boundary?
  "Check whether a given char is a token boundary."
  [c]
  ;; Note: indexOf here is more efficient that a hashset of characters.
  (or (nil? c) (> (.indexOf "\":;'@^`~()[]{}\\" (int c)) -1)))

(defn comma?
  [^java.lang.Character c]
  (identical? \, c))

(defn whitespace?
  [^java.lang.Character c]
  (cond (nil? c) false
        (identical? \, c) true
        :else (Character/isWhitespace c)))

(defn linebreak?
  [^java.lang.Character c]
  (or (identical? c \newline) (identical? c \return)))

(defn space?
  [^java.lang.Character c]
  (and c
       (Character/isWhitespace c)
       (not (identical? c \newline))
       (not (identical? c \return))
       (not (identical? c \,))))

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
            (.append buf (char c))
            (recur))
          (do
            (r/unread reader c)
            (str buf)))
        (if eof?
          (str buf)
          (u/throw-reader reader "Unexpected EOF."))))))

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
  [reader read-fn context]
  (loop []
    (let [start-row (r/get-line-number reader)
          start-col (r/get-column-number reader)]
      (when-let [entry (read-fn reader context)]
        (if (identical? reader entry)
          (recur)
          ;; conj is more efficient here than into because it doesn't perform
          ;; transient/persistent conversion if the second argument is nil.
          (let [new-meta (-> (conj {:row start-row
                                    :col start-col
                                    :end-row (r/get-line-number reader)
                                    :end-col (r/get-column-number reader)}
                                   (meta entry)))]
            (with-meta entry new-meta)))))))

(defn read-repeatedly
  "Call the given function on the given reader until it returns
   a non-truthy value."
  [reader read-fn context]
  (loop [acc []]
    (if-let [x (read-fn reader context)]
      (recur (conj acc x))
      acc)))

(defn read-n
  "Call the given function on the given reader until `n` values matching `p?` have been
   collected."
  [reader node-tag read-fn context p? n]
  {:pre [(pos? n)]}
  (loop [c 0
         vs []]
    (if (< c n)
      (if-let [v (read-fn reader context)]
        (recur
          (if (p? v) (inc c) c)
          (conj vs v))
        (u/throw-reader
          reader
          "%s node expects %d value%s."
          node-tag
          n
          (if (= 1 n) "" "s")))
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
