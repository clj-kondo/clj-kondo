(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.parser.core
  (:require
   [clj-kondo.impl.rewrite-clj.node :as node]
   [clj-kondo.impl.rewrite-clj.parser.keyword :refer [parse-keyword]]
   [clj-kondo.impl.rewrite-clj.parser.namespaced-map :as nm]
   [clj-kondo.impl.rewrite-clj.parser.string :refer [parse-regex parse-string]]
   [clj-kondo.impl.rewrite-clj.parser.token :refer [parse-token]]
   [clj-kondo.impl.rewrite-clj.parser.utils :as u]
   [clj-kondo.impl.rewrite-clj.reader :as reader]
   [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader.reader-types :as r]
   [clojure.string :as str]))

;; ## Base Parser

(def ^:dynamic ^:private *delimiter*
  "Record information about the pair of delimiter that are being parsed.
  This keeps track when parsing matching pairs of `{}`, `[]`, `()`.
  Store a tuple of 4 values: [open (char), close (char), row (int), col (int)]
  - The first item is the opening character, `{`, `[` or `(`.
  - The second item is the closing character, `}`, `]` or `)`.
  - The third item is the row on which the opening bracket was found.
  - The fourth item is the column on which the opening bracket was found."
  nil)

(defn- dispatch
  [c]
  (cond (nil? c)                   :eof
        (reader/whitespace? c)     :whitespace
        (= c (second *delimiter*)) :delimiter
        :else (get {\^ :meta      \# :sharp
                    \( :list      \[ :vector    \{ :map
                    \} :unmatched \] :unmatched \) :unmatched
                    \~ :unquote   \' :quote     \` :syntax-quote
                    \; :comment   \@ :deref     \" :string
                    \: :keyword}
                   c :token)))

(defmulti ^:private parse-next*
  (comp #'dispatch reader/peek))

(defn parse-next
  [reader]
  (reader/read-with-meta reader parse-next*))

;; # Parser Helpers

(defn- parse-delim
  [reader open-delimiter close-delimiter]
  (let [{:keys [row col]} (reader/position reader :row :col)]
    (reader/ignore reader)
    (->> #(binding [*delimiter* [open-delimiter close-delimiter row col]]
            (parse-next %))
         (reader/read-repeatedly reader))))

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

(defmethod parse-next* :token
  [reader]
  (parse-token reader))

(defmethod parse-next* :delimiter
  [reader]
  (reader/ignore reader))

(def open->close
  {\( \)
   \[ \]
   \{ \}})

(defn- mismatched-paren [[open _ row col] reader]
  (let [{:keys [close-row close-col]} (reader/position reader :close-row :close-col)
        closer (r/read-char reader)
        open-message (format "Mismatched bracket: found an opening %s and a closing %s on line %d" open closer close-row)
        close-message (format "Mismatched bracket: found an opening %s on line %d and a closing %s" open row closer)
        opening {:row row
                 :col col
                 :message open-message}
        closing  {:row close-row
                  :col close-col
                  :message close-message}]
    (swap! reader/*reader-exceptions* conj (ex-info "Syntax error" {:findings [opening closing]}))
    (r/unread reader (get open->close open))
    reader))

(defn- trailing-paren [reader]
  (let [{:keys [row col]} (reader/position reader :row :col)
        message (format "Unmatched bracket: unexpected %c" (r/read-char reader))]
    (swap! reader/*reader-exceptions* conj (ex-info "Syntax error" {:findings [{:row row
                                                                                :col col
                                                                                :message message}]}))
    reader))

(defmethod parse-next* :unmatched
  [reader]
  (if *delimiter*
    (mismatched-paren *delimiter* reader)
    (trailing-paren reader)))

(defmethod parse-next* :eof
  [reader]
  (when-let [[open close row col] *delimiter*]
    (let [opening {:row row
                   :col col
                   :message (format "Found an opening %s with no matching %s" open close)}
          closing (assoc (reader/position reader :row :col)
                         :message (format "Expected a %s to match %s from line %d" close open row))]
      (swap! reader/*reader-exceptions* conj (ex-info "Syntax error"
                                                      {:findings [opening closing]}))
      (r/unread reader close)
      reader)))

;; ### Whitespace

(defmethod parse-next* :whitespace
  [reader]
  (reader/read-while reader reader/whitespace?)
  reader)

(defmethod parse-next* :comment
  [reader]
  (reader/read-include-linebreak reader)
  reader)

;; ### Special Values

(defmethod parse-next* :keyword
  [reader]
  (parse-keyword reader))

(defmethod parse-next* :string
  [reader]
  (parse-string reader))

;; ### Meta

(def lconj (fnil conj '()))

(defn parse-meta [reader]
  (reader/ignore reader)
  (let [meta-node (parse-next reader)
        value-node (parse-next reader)]
    (update value-node :meta lconj meta-node)))

(defmethod parse-next* :meta
  [reader]
  (parse-meta reader))

;; ### Reader Specialities

(defn- read-symbolic-value [reader]
  (reader/unread reader \#)
  (parse-token reader))

(defn ignore-meta [uneval]
  (let [node (first uneval)]
    (some-> (if-let [k (:k node)]
             (when (identical? :clj-kondo/ignore k)
               {:clj-kondo/ignore (assoc (meta node)
                                         :linters :all)})
             (when (identical? :map (node/tag node))
               (let [[k v] (:children node)]
                 (when (identical? :clj-kondo/ignore (:k k))
                   ;; attach raw node, might need further processing for cljc
                   {:clj-kondo/ignore (assoc (meta node)
                                             :linters v)}))))
            (assoc :clj-kondo/ignore-id (keyword (gensym))))))

#_(defn spy [x]
  (prn x)
  x)

(defn- read-with-ignore-hint [reader]
  (let [[node] (parse-printables reader :uneval 1 true)
        im (ignore-meta [node])]
    (cond im
          (vary-meta (parse-next reader)
                     into im)
          (and node
               (= :reader-macro (node/tag node))
               (let [sv (-> node :children first :string-value)]
                 (str/starts-with? sv "?")))
          (let [features reader/*reader-features*
                children (when features
                           (->> node :children last :children
                                (take-nth 2)
                                (keep :k)
                                (into #{})))]
            ;; If the reader conditional contains all features or :default,
            ;; then it can be ignored.
            ;; Otherwise, add :clj-kondo/uneval metadata to discard later.
            (if (or (not features)
                    (every? children features)
                    (contains? children :default))
              (parse-next reader)
              (vary-meta node assoc :clj-kondo/uneval (set (remove children features)))))
          :else
          (parse-next reader))))

(defmethod parse-next* :sharp
  [reader]
  (reader/ignore reader)
  (case (reader/peek reader)
    nil (u/throw-reader reader "Unexpected EOF.")
    \# (read-symbolic-value reader)
    \! (do (reader/read-include-linebreak reader)
           reader)
    \{ (node/set-node (parse-delim reader \{ \}))
    \( (node/fn-node (parse-delim reader \( \)))
    \" (node/regex-node (parse-regex reader))
    \^ (parse-meta reader)
    \' (node/var-node (parse-printables reader :var 1 true))
    \= (node/eval-node (parse-printables reader :eval 1 true))
    \_ (read-with-ignore-hint reader)
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

(defmethod parse-next* :deref
  [reader]
  (node/deref-node (parse-printables reader :deref 1 true)))

;; ## Quotes

(defmethod parse-next* :quote
  [reader]
  (node/quote-node (parse-printables reader :quote 1 true)))

(defmethod parse-next* :syntax-quote
  [reader]
  (node/syntax-quote-node (parse-printables reader :syntax-quote 1 true)))

(defmethod parse-next* :unquote
  [reader]
  (reader/ignore reader)
  (let [c (reader/peek reader)]
    (if (= \@ c)
      (node/unquote-splicing-node
       (parse-printables reader :unquote 1 true))
      (node/unquote-node
       (parse-printables reader :unquote 1)))))

;; ### Seqs

(defmethod parse-next* :list
  [reader]
  (node/list-node (parse-delim reader \( \))))

(defmethod parse-next* :vector
  [reader]
  (node/vector-node (parse-delim reader \[ \])))

(defmethod parse-next* :map
  [reader]
  (node/map-node (parse-delim reader \{ \})))
