(ns clj-kondo.impl.docstring
  (:require [clj-kondo.impl.findings :as findings]
            [clj-kondo.impl.utils :refer [node->line tag node->keyword string-from-token]]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn safe-char-at [^String s idx len]
  (when (and (nat-int? idx) (< idx len))
    (.charAt s idx)))

(defn lint-docstring!
  "Lint `docstring` for styling issues.

  `node` is the node reported when docstring has findings, so ideally
  it should be the text node for `docstring`."
  [{:keys [filename config] :as ctx} node docstring]
  (when docstring
    (doseq [[type f msg] [[:docstring-blank
                           str/blank?
                           "Docstring should not be blank."]
                          [:docstring-no-summary
                           (fn [docstring]
                             (let [first-line (first (str/split-lines docstring))
                                   first-line (str/trim first-line)
                                   len (.length first-line)
                                   first-char (safe-char-at first-line 0 len)
                                   last-char (safe-char-at first-line (dec len) len)]
                               (not
                                (and first-char (Character/isUpperCase ^char first-char)
                                     (re-matches #"[.!?]" (str last-char))))))
                           "First line of the docstring should be a capitalized sentence ending with punctuation."]
                          [:docstring-leading-trailing-whitespace
                           (fn [docstring]
                             (not= docstring (str/trim docstring)))
                           "Docstring should not have leading or trailing whitespace."]]]
      (when (and (not (identical? :off (some-> config :linters type :level)))
                 (f docstring))
        (findings/reg-finding!
         ctx
         (node->line filename
                     node
                     type
                     msg))))))

(defn docs-from-meta
  "Return a tuple of `[doc-node docstring]` given `meta-node`.

  If `meta-node` is not a map node or does not contain a :doc entry,
  return nil."
  [meta-node]
  (when meta-node
    (when-let [doc-node (when (= :map (tag meta-node))
                          (->> meta-node
                               :children
                               (partition 2)
                               (some (fn [[k v]]
                                       (when (= :doc (node->keyword k)) v)))))]
      (when-let [docstring (string-from-token doc-node)]
        [doc-node docstring]))))
