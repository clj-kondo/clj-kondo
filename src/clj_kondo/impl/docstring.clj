(ns clj-kondo.impl.docstring
  (:require [clj-kondo.impl.findings :as findings]
            [clj-kondo.impl.utils :refer [node->line tag node->keyword string-from-token]]
            [clojure.string :as str]))

(defn docstring-messages
  "Return a sequence of linting messages for the given `docstring`."
  [docstring]
  (cond-> nil
    (str/blank? docstring)
    (conj {:message "Docstring should not be blank."
           :type :docstring-blank})

    (not (re-find #"^\s*[A-Z].*[.!?]\s*$" docstring))
    (conj {:message "First line of the docstring should be a capitalized sentence ending with punctuation."
           :type :docstring-no-summary})

    (or (re-find #"^\s" docstring)
        (re-find #"\s$" docstring))
    (conj {:message "Docstring should not have leading or trailing whitespace."
           :type :docstring-leading-trailing-whitespace})))

(defn lint-docstring!
  "Lint `docstring` for styling issues.

  `node` is the node reported when docstring has findings, so ideally
  it should be the text node for `docstring`."
  [{:keys [filename config] :as ctx} node docstring]
  (when (some? docstring)
    (doseq [{:keys [type message]} (docstring-messages docstring)]
      (when-not (identical? :off (-> config :linters type :level))
        (findings/reg-finding!
         ctx
         (node->line filename
                     node
                     type
                     message))))))

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
