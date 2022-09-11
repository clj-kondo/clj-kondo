#!/usr/bin/env bb

(ns generate-linter-docs
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]))

(def linters-config
  (->> (slurp "resources/clj_kondo/config/defaults.edn")
      edn/read-string
      (sort-by first)))

(defn build-intro []
  "# Linters\n\nThis page contains an overview of all available linters and their corresponding configuration. For general configurations options, go [here](config.md).")

(defn build-toc [config]
  (format
    "%s\n\n%s\n%s"
    "**Table of Contents**"
    "- [Linters](#linters)"
    (->> (for [[_lint-kw lint] config
               :let [nom (:name lint)
                     link (-> nom
                              (str/lower-case)
                              (str/replace " " "-")
                              (str/replace "." "")
                              (str/replace "?" ""))]]
           (format "    - [%s](#%s)" nom link))
         (str/join "\n"))))

(defn strip-indentation [string]
  (if (= \newline (first string))
    (let [indentation (abs (- (count string) (inc (count (str/triml string)))))
          lines (->> string
                     (str/split-lines)
                     (rest)
                     (butlast)
                     (map #(when (seq %) (subs % indentation)))
                     (str/join "\n")
                     (str/trim))]
      lines)
    string))

(defn build-docs [config]
  (->> (for [[lint-kw lint] config
             :let [{:keys [description default-level example-trigger example-message
                           config-spec config-description config-default]} lint]]
         (->> [(format "## %s" (:name lint))
               (format "*Keyword:* `%s`" lint-kw)
               (format "*Description:* %s" (strip-indentation description))
               (format "*Default level:* `%s`" default-level)
               (when-not (str/blank? example-trigger)
                 (format "*Example trigger:*\n\n```clojure\n%s\n```"
                         (strip-indentation example-trigger)))
               (when-not (str/blank? example-message)
                 (format "*Example message:* `%s`"
                         (strip-indentation example-message)))
               (when-not (str/blank? config-description)
                 (format "*Config:*\n\n%s"
                         (strip-indentation config-description)))
               (when config-default
                 (format "*Config default:* `%s`" (pr-str config-default)))
               (when config-spec
                 (format "*Config \"spec\":* `%s`" (pr-str config-spec)))]
              (filter identity)
              (str/join "\n\n"))
         )
       (str/join "\n\n")))

(spit "doc/linters.md"
      (str/join "\n\n"
                [(build-intro)
                 (build-toc linters-config)
                 (build-docs linters-config)]))
