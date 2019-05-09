(ns clj-kondo.impl.rewrite-clj-patch
  (:require
   [rewrite-clj.parser.core]
   [rewrite-clj.node.seq]
   [rewrite-clj.parser.keyword]))

(in-ns 'rewrite-clj.parser.core)

(defn parse-map-ns
  ;; parse keyword inside reader tag
  [reader]
  (reader/ignore reader)
  (reader/read-while reader (fn [c]
                              (= \: c)))
  (let [s (reader/read-until reader
                                  (fn [c]
                                    (= \{ c)))]
    (if (= "" s)
      'clj-kondo/current-ns
      (symbol s)
      )))

(defmethod parse-next* :sharp
  [reader]
  (reader/ignore reader)
  (case (reader/peek reader)
    nil (reader/throw-reader reader "Unexpected EOF.")
    \{ (node/set-node (parse-delim reader \}))
    \( (node/fn-node (parse-delim reader \)))
    \" (node/regex-node (parse-regex reader))
    \^ (node/raw-meta-node (parse-printables reader :meta 2 true))
    \' (node/var-node (parse-printables reader :var 1 true))
    \= (node/eval-node (parse-printables reader :eval 1 true))
    \_ (node/uneval-node (parse-printables reader :uneval 1 true))
    \: (node/namespaced-map-node (parse-map-ns reader)
                                 [(parse-next reader)])
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

(in-ns 'rewrite-clj.parser.keyword)

(defn parse-keyword
  [reader]
  (u/ignore reader)
  (if-let [c (r/peek-char reader)]
    (if (= c \:)
      (try
        (node/keyword-node
         (edn/read reader)
         true)
        (catch Exception e
          (if (re-find  #"Invalid keyword: \." (.getMessage e))
            (node/keyword-node :__current_ns__ true)
            (throw e))))
      (do
        (r/unread reader \:)
        (node/keyword-node (edn/read reader))))
    (u/throw-reader reader "unexpected EOF while reading keyword.")))

(in-ns 'rewrite-clj.node.seq)

(defn- assert-namespaced-map-children
  [children]
  )

(defrecord NamespacedMapNode [ns children]
  node/Node
  (tag [this]
    :namespaced-map)
  (printable-only? [_] false)
  (sexpr [this]
    (let [[nspace' [m]] [ns children]
          nspace nspace']
      (->> (for [[k v] m
                 :let [k' (cond (not (keyword? k)) k
                                (namespace k)      k
                                :else (keyword (str nspace) (name k)))]]
             [k' v])
           (into {}))))
  (length [_] 1
    (+ 1 (node/sum-lengths children)))
  (string [this]
    (str "#" (node/concat-strings children)))

  node/InnerNode
  (inner? [_] true)
  (children [_] children)
  (replace-children [this children']
    (assoc this :children children'))
  (leader-length [_]
    1)

  Object
  (toString [this]
    (node/string this)))

(defn namespaced-map-node
  "Create a node representing an EDN map namespace."
  [ns children]
  (->NamespacedMapNode ns children))
