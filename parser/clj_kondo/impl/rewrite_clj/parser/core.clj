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

;; Record information about the pair of delimiter that are being parsed.
;; This keeps track when parsing matching pairs of `{}`, `[]`, `()`.
;; - The first item is the opening character, `{`, `[` or `(`.
;; - The second item is the closing character, `}`, `]` or `)`.
;; - The third item is the row on which the opening bracket was found.
;; - The fourth item is the column on which the opening bracket was found.
(defrecord ReaderContext [open-delim close-delim row col])

(defn- dispatch
  [c context]
  (cond (nil? c)                     :eof
        (reader/whitespace? c)       :whitespace
        (= c (:close-delim context)) :delimiter
        :else (get {\^ :meta      \# :sharp
                    \( :list      \[ :vector    \{ :map
                    \} :unmatched \] :unmatched \) :unmatched
                    \~ :unquote   \' :quote     \` :syntax-quote
                    \; :comment   \@ :deref     \" :string
                    \: :keyword}
                   c :token)))

(defmulti ^:private parse-next*
  (fn [reader context] (dispatch (reader/peek reader) context)))

(defn parse-next
  ([reader] (parse-next reader nil))
  ([reader context]
   (reader/read-with-meta reader parse-next* context)))

;; # Parser Helpers

(defn- parse-delim
  [reader open-delimiter close-delimiter]
  (let [{:keys [row col]} (reader/position reader :row :col)]
    (reader/ignore reader)
    (reader/read-repeatedly reader parse-next
                            (->ReaderContext open-delimiter close-delimiter
                                             row col))))

(defn- parse-printables
  [reader context node-tag n & [ignore?]]
  (when ignore?
    (reader/ignore reader))
  (reader/read-n
   reader
   node-tag
   parse-next
   context
   (complement node/printable-only?)
   n))

;; ## Parsers Functions

;; ### Base

(defmethod parse-next* :token
  [reader _]
  (parse-token reader))

(defmethod parse-next* :delimiter
  [reader _]
  (reader/ignore reader))

(def open->close
  {\( \)
   \[ \]
   \{ \}})

(defn- mismatched-paren [reader {:keys [open-delim row col] :as _context}]
  (let [{:keys [close-row close-col]} (reader/position reader :close-row :close-col)
        closer (r/read-char reader)
        open-message (format "Mismatched bracket: found an opening %s and a closing %s on line %d" open-delim closer close-row)
        close-message (format "Mismatched bracket: found an opening %s on line %d and a closing %s" open-delim row closer)
        opening {:row row
                 :col col
                 :message open-message}
        closing  {:row close-row
                  :col close-col
                  :message close-message}]
    (swap! reader/*reader-exceptions* conj (ex-info "Syntax error" {:findings [opening closing]}))
    (r/unread reader (get open->close open-delim))
    reader))

(defn- trailing-paren [reader]
  (let [{:keys [row col]} (reader/position reader :row :col)
        message (format "Unmatched bracket: unexpected %c" (r/read-char reader))]
    (swap! reader/*reader-exceptions* conj (ex-info "Syntax error" {:findings [{:row row
                                                                                :col col
                                                                                :message message}]}))
    reader))

(defmethod parse-next* :unmatched
  [reader context]
  (if context
    (mismatched-paren reader context)
    (trailing-paren reader)))

(defmethod parse-next* :eof
  [reader context]
  (when-let [{:keys [open-delim close-delim row col]} context]
    (let [opening {:row row
                   :col col
                   :message (format "Found an opening %s with no matching %s" open-delim close-delim)}
          closing (assoc (reader/position reader :row :col)
                         :message (format "Expected a %s to match %s from line %d" close-delim open-delim row))]
      (swap! reader/*reader-exceptions* conj (ex-info "Syntax error"
                                                      {:findings [opening closing]}))
      (r/unread reader close-delim)
      reader)))

;; ### Whitespace

(defmethod parse-next* :whitespace
  [reader _]
  (reader/read-while reader reader/whitespace?)
  reader)

(defmethod parse-next* :comment
  [reader _]
  (reader/read-include-linebreak reader)
  reader)

;; ### Special Values

(defmethod parse-next* :keyword
  [reader _]
  (parse-keyword reader))

(defmethod parse-next* :string
  [reader _]
  (parse-string reader))

;; ### Meta

(def lconj (fnil conj '()))

(defn parse-meta [reader context]
  (reader/ignore reader)
  (let [meta-node (parse-next reader context)
        value-node (parse-next reader context)]
    (update value-node :meta lconj meta-node)))

(defmethod parse-next* :meta
  [reader context]
  (parse-meta reader context))

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

(defn- read-with-ignore-hint [reader context]
  (let [[node] (parse-printables reader context :uneval 1 true)
        im (ignore-meta [node])]
    (cond im
          (vary-meta (parse-next reader context)
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
              (parse-next reader context)
              (vary-meta node assoc :clj-kondo/uneval (set (remove children features)))))
          :else
          (parse-next reader context))))

(defmethod parse-next* :sharp
  [reader context]
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
    \' (node/var-node (parse-printables reader context :var 1 true))
    \= (node/eval-node (parse-printables reader context :eval 1 true))
    \_ (read-with-ignore-hint reader context)
    ;; begin patch patch
    \: (nm/parse-namespaced-map reader #(parse-next % context))
    ;; end patch
    \? (do
         ;; we need to examine the next character, so consume one (known \?)
         (reader/next reader)
         ;; we will always have a reader-macro-node as the result
         (node/reader-macro-node
          (let [read1 (fn [] (parse-printables reader context :reader-macro 1))]
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
    (node/reader-macro-node (parse-printables reader context :reader-macro 2))))

(defmethod parse-next* :deref
  [reader context]
  (node/deref-node (parse-printables reader context :deref 1 true)))

;; ## Quotes

(defmethod parse-next* :quote
  [reader context]
  (node/quote-node (parse-printables reader context :quote 1 true)))

(defmethod parse-next* :syntax-quote
  [reader context]
  (node/syntax-quote-node (parse-printables reader context :syntax-quote 1 true)))

(defmethod parse-next* :unquote
  [reader context]
  (reader/ignore reader)
  (let [c (reader/peek reader)]
    (if (= \@ c)
      (node/unquote-splicing-node
       (parse-printables reader context :unquote 1 true))
      (node/unquote-node
       (parse-printables reader context :unquote 1)))))

;; ### Seqs

(defmethod parse-next* :list
  [reader _]
  (node/list-node (parse-delim reader \( \))))

(defmethod parse-next* :vector
  [reader _]
  (node/vector-node (parse-delim reader \[ \])))

(defmethod parse-next* :map
  [reader _]
  (node/map-node (parse-delim reader \{ \})))
