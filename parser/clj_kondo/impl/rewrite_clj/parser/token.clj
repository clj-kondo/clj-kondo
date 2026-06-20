(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.parser.token
  (:require [clj-kondo.impl.rewrite-clj
             [node :as node]
             [reader :as r]]
            [clj-kondo.impl.rewrite-clj.parser.utils :as u]
            [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader.reader-types :as rt]))

(set! *warn-on-reflection* true)

;; Next two functions are extract to avoid allocating fn objects in refsites.

(defn- not-boundary? [c] (not (r/whitespace-or-boundary? c)))

(defn- not-boundary-allow-extra? [c]
  (or (#{\' \:} c)
      (not (r/whitespace-or-boundary? c))))

(defn- read-to-boundary
  [reader buf f]
  (r/read-into-buffer-while reader buf f true))

(defn- read-to-char-boundary
  [reader ^StringBuilder buf]
  (if-let [c (r/next reader)]
    (do (.append buf (char c))
        (when-not (= c \\)
          (read-to-boundary reader buf not-boundary?)))
    ;; At least one char must be present after \
    (u/throw-reader reader "Unexpected EOF")))

(defn- symbol-node
  "Symbols allow for certain boundary characters that have
   to be handled explicitly."
  [reader value value-string ^StringBuilder buf]
  (let [length-before (.length buf)
        _ (read-to-boundary reader buf not-boundary-allow-extra?)
        length-after (.length buf)]
    (if (= length-before length-after)
      (node/token-node value value-string)
      (let [s (str buf)]
        (node/token-node (r/read-symbol s) s)))))

(defn- number-literal?
  "Checks whether the reader is at the start of a number literal

  Cribbed and adapted from clojure.tools.reader.impl.commons"
  [^String s]
  (let [c1 (.charAt s 0)]
    (or (Character/isDigit c1)
        (and (> (.length s) 1)
             (or (identical? \+ c1) (identical? \- c1))
             (Character/isDigit (.charAt s 1))))))

(defn parse-token
  "Parse a single token."
  [reader]
  (let [token-row (rt/get-line-number reader)
        token-col (rt/get-column-number reader)]
    (try
      (let [buf (StringBuilder.)
            first-char (r/next reader)
            _ (.append buf (char first-char))
            _ (if (= \\ first-char)
                (read-to-char-boundary reader buf)
                (read-to-boundary reader buf not-boundary?))
            s (str buf)
            v (if (or (= first-char \\) ;; character like \n or \newline
                      (= first-char \#) ;; something like ##Inf, ##Nan
                      (number-literal? s))
                (r/string->edn s)
                (r/read-symbol s))]
        (if (symbol? v)
          (symbol-node reader v s buf)
          (node/token-node v s)))
      (catch Exception e
        (if (and r/*reader-exceptions*
                 (= :reader-exception (:type (ex-data e))))
          (let [msg (ex-message e)
                invalid-symbol (when msg (second (re-matches #"Invalid symbol: (.*)\." msg)))
                f {:row token-row
                   :col token-col
                   :end-row (rt/get-line-number reader)
                   :end-col (rt/get-column-number reader)
                   :message (ex-message e)}]
            (swap! r/*reader-exceptions* conj (ex-info "Syntax error" {:findings [f]}))
            (if invalid-symbol
              (node/token-node (symbol invalid-symbol) invalid-symbol)
              reader))
          (throw e))))))
