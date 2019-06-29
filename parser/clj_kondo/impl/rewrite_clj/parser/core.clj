(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.parser.core
  (:require
   [clj-kondo.impl.rewrite-clj.parser.namespaced-map :as nm]
   [clj-kondo.impl.rewrite-clj
    [node :as node]
    [reader :as reader]]
   [clj-kondo.impl.rewrite-clj.parser
    [keyword :refer [parse-keyword]]
    [string :refer [parse-string parse-regex]]
    [token :as token]]))

;; ## Base Parser

(def ^:dynamic ^:private *delimiter*
  nil)

(defn- dispatch
  [c]
  (cond (nil? c)               :eof
        (reader/whitespace? c) :whitespace
        (= c *delimiter*)      :delimiter
        :else (get {\^ :meta      \# :sharp
                    \( :list      \[ :vector    \{ :map
                    \} :unmatched \] :unmatched \) :unmatched
                    \~ :unquote   \' :quote     \` :syntax-quote
                    \; :comment   \@ :deref     \" :string
                    \: :keyword}
                   c :token)))

#_(defmulti ^:private parse-next*
    (comp #'dispatch reader/peek))

(declare parse-eof parse-whitespace parse-delimiter parse-meta parse-sharp
         parse-list parse-vector parse-map parse-unmatched parse-unquote
         parse-quote parse-syntax-quote parse-comment parse-deref parse-token)

(defn parse-next* [reader]
  (let [c (reader/peek reader)
        f (cond (nil? c)               parse-eof
                (reader/whitespace? c) parse-whitespace
                (= c *delimiter*)      parse-delimiter
                :else (get {\^ parse-meta      \# parse-sharp
                            \( parse-list      \[ parse-vector    \{ parse-map
                            \} parse-unmatched \] parse-unmatched \) parse-unmatched
                            \~ parse-unquote   \' parse-quote     \` parse-syntax-quote
                            \; parse-comment   \@ parse-deref     \" parse-string
                            \: parse-keyword}
                           c parse-token))]
    (f reader)))

(defn parse-next
  [reader]
  (reader/read-with-meta reader parse-next*))

;; # Parser Helpers

(defn- parse-delim
  [reader delimiter]
  (reader/ignore reader)
  (->> #(binding [*delimiter* delimiter]
          (parse-next %))
       (reader/read-repeatedly reader)))

(defn- parse-printables
  [reader node-tag n & [ignore?]]
  (when ignore?
    (reader/ignore reader))
  (reader/read-n
   reader
   node-tag
   parse-next
   (complement node/printable-only?)
   n))

;; ## Parsers Functions

;; ### Base

(defn parse-token
  [reader]
  (token/parse-token reader))

#_(defmethod parse-next* :token
  [reader]
  (parse-token reader))

(defn parse-delimiter [reader]
  (reader/ignore reader))

#_(defmethod parse-next* :delimiter
  [reader]
  (parse-delimiter reader))

(defn parse-unmatched [reader]
  (reader/throw-reader
   reader
   "Unmatched delimiter: %s"
   (reader/peek reader)))

#_(defmethod parse-next* :unmatched
  [reader]
  (parse-unmatched reader))

(defn parse-eof
  [reader]
  (when *delimiter*
    (reader/throw-reader reader "Unexpected EOF.")))

#_(defmethod parse-next* :eof
  [reader]
  (parse-eof reader))

;; ### Whitespace

(defn parse-whitespace
  [reader]
  (reader/read-while reader reader/whitespace?)
  reader)

#_(defmethod parse-next* :whitespace
  [reader]
  (parse-whitespace reader))

(defn parse-comment
  [reader]
  (reader/read-include-linebreak reader)
  reader)

#_(defmethod parse-next* :comment
  [reader]
  (parse-comment reader))

;; ### Special Values

#_(defmethod parse-next* :keyword
  [reader]
  (parse-keyword reader))

#_(defmethod parse-next* :string
  [reader]
  (parse-string reader))

;; ### Meta

(defn parse-meta [reader]
  (reader/ignore reader)
  (node/meta-node (parse-printables reader :meta 2)))

#_(defmethod parse-next* :meta
  [reader]
  (parse-meta reader))

;; ### Reader Specialities

(defn parse-sharp [reader]
  (reader/ignore reader)
  (case (reader/peek reader)
    nil (reader/throw-reader reader "Unexpected EOF.")
    \! (do (reader/read-include-linebreak reader)
           reader)
    \{ (node/set-node (parse-delim reader \}))
    \( (node/fn-node (parse-delim reader \)))
    \" (node/regex-node (parse-regex reader))
    \^ (node/raw-meta-node (parse-printables reader :meta 2 true))
    \' (node/var-node (parse-printables reader :var 1 true))
    \= (node/eval-node (parse-printables reader :eval 1 true))
    \_ (do (parse-printables reader :uneval 1 true)
           reader)
    ;; begin patch patch
    \: (nm/parse-namespaced-map reader parse-next)
    ;; end patch
    \? (do
         ;; we need to examine the next character, so consume one (known \?)
         (reader/next reader)
         ;; we will always have a reader-macro-node as the result
         (node/reader-macro-node
          (let [read1 (fn [] (parse-printables reader :reader-macro 1))]
            (cons (case (reader/peek reader)
                    ;; the easy case, just emit a token
                    \( (node/token-node (symbol "?"))

                    ;; the harder case, match \@, consume it and emit the token
                    \@ (do (reader/next reader)
                           (node/token-node (symbol "?@")))

                    ;; otherwise no idea what we're reading but its \? prefixed
                    (do (reader/unread reader \?)
                        (first (read1))))
                  (read1)))))
    (node/reader-macro-node (parse-printables reader :reader-macro 2))))

#_(defmethod parse-next* :sharp
  [reader]
  (parse-sharp reader))

(defn parse-deref
  [reader]
  (node/deref-node (parse-printables reader :deref 1 true)))

#_(defmethod parse-next* :deref
  [reader]
  (parse-deref reader))

;; ## Quotes

(defn parse-quote
  [reader]
  (node/quote-node (parse-printables reader :quote 1 true)))

#_(defmethod parse-next* :quote
  [reader]
  (parse-quote reader))

(defn parse-syntax-quote
  [reader]
  (node/syntax-quote-node (parse-printables reader :syntax-quote 1 true)))

#_(defmethod parse-next* :syntax-quote
  [reader]
  (parse-syntax-quote reader))

(defn parse-unquote
  [reader]
  (reader/ignore reader)
  (let [c (reader/peek reader)]
    (if (= c \@)
      (node/unquote-splicing-node
       (parse-printables reader :unquote 1 true))
      (node/unquote-node
       (parse-printables reader :unquote 1)))))

#_(defmethod parse-next* :unquote
  [reader]
  (parse-unquote reader))

;; ### Seqs

(defn parse-list [reader]
  (node/list-node (parse-delim reader \))))

#_(defmethod parse-next* :list
  [reader]
  (parse-list reader))

(defn parse-vector [reader]
  (node/vector-node (parse-delim reader \])))

#_(defmethod parse-next* :vector
  [reader]
  (parse-vector reader))

(defn parse-map
  [reader]
  (node/map-node (parse-delim reader \})))

#_(defmethod parse-next* :map
  [reader]
  (parse-map reader))
