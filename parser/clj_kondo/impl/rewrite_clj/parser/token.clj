(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.parser.token
  (:require [clj-kondo.impl.rewrite-clj
             [node :as node]
             [reader :as r]]
            [clj-kondo.impl.toolsreader.v1v2v2.clojure.tools.reader.reader-types :as rt]))

(def ^:dynamic *invalid-token-exceptions* nil)

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
  (let [token-location ((juxt rt/get-line-number rt/get-column-number) reader)]
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
      (catch Exception e
        (let [{:keys [:type :ex-kind]} (ex-data e)]
          (when (and *invalid-token-exceptions*
                     (= :reader-exception type)
                     (= :reader-error ex-kind))
            (let [f {:row (token-location 0)
                     :col (token-location 1)
                     :message (.getMessage e)}]
              (swap! *invalid-token-exceptions* conj (ex-info "Syntax error" {:findings [f]})))))
        reader))))
