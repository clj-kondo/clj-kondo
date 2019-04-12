(ns clj-kondo.impl.linters
  {:no-doc true}
  (:require
   [clj-kondo.impl.macroexpand :refer [expand-all]]
   [clj-kondo.impl.utils :refer [node->line parse-string
                                 parse-string-all some-call
                                 tag]]
   [clj-kondo.impl.vars :refer [analyze-arities]]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

;;;; inline def

(defn inline-def* [expr in-def?]
  (let [current-def? (some-call expr def defn defn- deftest defmacro)
        new-in-def? (and (not (contains? '#{:syntax-quote :quote}
                                         (tag expr)))
                         (or in-def? current-def?))]
    (if (and in-def? current-def?)
      [expr]
      (when (:children expr)
        (mapcat #(inline-def* % new-in-def?) (:children expr))))))

(defn inline-def [filename parsed-expressions]
  (map #(node->line filename % :warning :inline-def "inline def")
       (inline-def* parsed-expressions false)))

;;;; redundant let

(defn redundant-let* [{:keys [:children] :as expr}
                     parent-let?]
  (let [current-let? (some-call expr let)]
    (cond (and current-let? parent-let?)
          [expr]
          current-let?
          (let [;; skip let keywords and bindings
                children (nnext children)]
            (concat (redundant-let* (first children) current-let?)
                    (mapcat #(redundant-let* % false) (rest children))))
          :else (mapcat #(redundant-let* % false) children))))

(defn redundant-let [filename parsed-expressions]
  (map #(node->line filename % :warning :nested-let "redundant let")
       (redundant-let* parsed-expressions false)))

;;;; redundant do

(defn redundant-do* [{:keys [:children] :as expr}
                    parent-do?]
  (let [implicit-do? (some-call expr fn defn defn-
                            let loop binding with-open
                            doseq try)
        current-do? (some-call expr do)]
    (cond (and current-do? (or parent-do?
                               (and (not= :unquote-splicing
                                          (tag (second children)))
                                    (<= (count children) 2))))
          [expr]
          :else (mapcat #(redundant-do* % (or implicit-do? current-do?)) children))))

(defn redundant-do [filename parsed-expressions]
  (map #(node->line filename % :warning :redundant-do "redundant do")
       (redundant-do* parsed-expressions false)))

;;;; processing of string input

(defn process-input
  [filename input lang config]
  (try
    (let [;; workaround for https://github.com/xsc/rewrite-clj/issues/75
          input (-> input
                    (str/replace "##Inf" "::Inf")
                    (str/replace "##-Inf" "::-Inf")
                    (str/replace "##NaN" "::NaN")
                    ;; workaround for https://github.com/borkdude/clj-kondo/issues/11
                    (str/replace #_"#:a{#::a {:a b}}"
                                 #"#(::?)(.*?)\{" (fn [[_ colons name]]
                                                    (str "#_" colons name "{"))))
          parsed-expressions (parse-string-all input config)
          parsed-expressions (expand-all parsed-expressions)
          ids (inline-def filename parsed-expressions)
          nls (redundant-let filename parsed-expressions)
          ods (redundant-do filename parsed-expressions)]
      (cons {:findings (concat ids nls ods)
             :lang lang}
            (analyze-arities filename lang parsed-expressions (:debug? config))))
    (catch Exception e
      [{:findings [{:level :error
                    :filename filename
                    :col 0
                    :row 0
                    :message (str "Can't parse "
                                  filename ", "
                                  (.getMessage e))}]}])))

;;;; scratch

(comment
  )
