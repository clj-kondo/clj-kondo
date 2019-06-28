(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.parser.keyword
  (:require
   [clj-kondo.impl.rewrite-clj.node :as node]
   [clj-kondo.impl.rewrite-clj.parser.utils :as u]
   [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader]
   [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader.edn :as edn]
   [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader.impl.commons :as rc]
   [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader.impl.errors :as err]
   [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader.impl.utils :as ru]
   [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader.reader-types :as r]))

(set! *warn-on-reflection* true)

;; modified version of tools.reader.edn/read-keyword, less strict
(defn read-keyword
  [reader]
  (let [ch (r/read-char reader)]
    (if-not (ru/whitespace? ch)
      (let [^String token (#'edn/read-token reader :keyword ch)
            s (rc/parse-symbol token)]
        (if (and s (not (zero? (.indexOf token "::"))))
          (let [^String ns (s 0)
                ^String name (s 1)]
            (if (identical? \: (nth token 0))
              (err/throw-invalid reader :keyword token) ; No ::kw in edn.
              (keyword ns name)))
          (err/throw-invalid reader :keyword token)))
      (err/throw-single-colon reader))))

(defn parse-keyword
  [reader]
  (u/ignore reader)
  (if-let [c (r/peek-char reader)]
    (if (= c \:)
      (do
        (r/read-char reader)
        (node/keyword-node
         (read-keyword reader)
         true))
      (node/keyword-node (read-keyword reader)))
    (u/throw-reader reader "unexpected EOF while reading keyword.")))

;;;; Scratch

(comment
  (parse-keyword (r/string-push-back-reader "::foo"))
  (parse-keyword (r/string-push-back-reader "::a/foo"))
  (parse-keyword (r/string-push-back-reader ":a/foo"))
  (parse-keyword (r/string-push-back-reader ":foo"))
  (parse-keyword (r/string-push-back-reader ":&::before"))
  )
