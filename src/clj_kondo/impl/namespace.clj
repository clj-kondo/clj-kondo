(ns clj-kondo.impl.namespace
  {:no-doc true}
  (:require
   [clj-kondo.impl.utils :refer [parse-string parse-string-all]]
   [clojure.java.io :as io]
   [rewrite-clj.node.protocols :as node]))

(def valid-ns-name? (some-fn symbol? string?))

(defn- prefix-spec?
  "Adapted from clojure.tools.namespace"
  [form]
  (and (sequential? form) ; should be a list, but often is not
       (symbol? (first form))
       (not-any? keyword? form)
       (< 1 (count form))))  ; not a bare vector like [foo]

(defn- option-spec?
  "Adapted from clojure.tools.namespace"
  [form]
  (and (sequential? form)  ; should be a vector, but often is not
       (valid-ns-name? (first form))
       (or (keyword? (second form))  ; vector like [foo :as f]
           (= 1 (count form)))))  ; bare vector like [foo]

(defn- normalize-libspec
  "Adapted from clojure.tools.namespace."
  [prefix form]
  (cond (prefix-spec? form)
        (mapcat (fn [f]
                  (normalize-libspec
                   (symbol (str (when prefix (str prefix "."))
                                (first form)))
                   f))
                (rest form))
        (option-spec? form)
        [(into (normalize-libspec prefix (first form)) (rest form))]
        (valid-ns-name? form)
        [(symbol (str (when prefix (str prefix ".")) form))]
        (keyword? form)  ; Some people write (:require ... :reload-all)
        nil
        :else
        (throw (ex-info "Unparsable namespace form"
                        {:reason ::unparsable-ns-form
                         :form form}))))

(defn analyze-libspec [lang libspec]
  (if (symbol? libspec)
    [{:type :require
      :ns libspec}]
    (let [[ns-name & options] libspec
          ns-name (symbol ns-name)
          ;; CLJS clojure namespace aliasing
          ns-name (if (= :cljs lang)
                    (case ns-name
                      clojure.test 'cljs.test
                      clojure.pprint 'cljs.pprint
                      ns-name)
                    ns-name)]
      (loop [children options
             {:keys [:as :refers :excluded
                     :refered-all :renamed] :as m}
             {:as nil
              :refers []
              :excluded #{}
              :refered-all false
              :renamed {}}]
        (if-let [child (first children)]
          (let [opt (fnext children)]
            (case child
              (:refer :refer-macros)
              (recur
               (nnext children)
               (cond (sequential? opt)
                     (update m :refers into opt)
                     (= :all opt)
                     (assoc m :refered-all true)
                     :else m))
              :as (recur
                   (nnext children)
                   (assoc m :as opt))
              :exclude
              (recur
               (nnext children)
               (update m :excluded into (set opt)))
              :rename
              (recur
               (nnext children)
               (-> m (update :renamed merge opt)
                   (update :excluded into (set (keys opt)))))
              (recur (nnext children)
                     m)))
          [{:type :require
            :ns ns-name
            :as as
            :excluded excluded
            :refers (concat (map (fn [refer]
                                   [refer {:ns ns-name
                                           :name refer}])
                                 refers)
                            (map (fn [[original-name new-name]]
                                   [new-name {:ns ns-name
                                              :name original-name}])
                                 renamed))
            :refered-all refered-all}])))))

(def default-java-imports
  (reduce (fn [acc [prefix sym]]
            (let [fq (symbol (str prefix sym))]
              (-> acc
                  (assoc fq fq)
                  (assoc sym fq))))
          {}
          (into (mapv vector (repeat "java.lang.") '[Boolean Byte CharSequence Character Double
                                                     Integer Long Math String System Thread])
                (mapv vector (repeat "java.math.") '[BigDecimal BigInteger]))))

(defn analyze-ns-decl [lang expr]
  ;; TODO: handle rename
  (let [sexpr (node/sexpr expr)
        subclauses
        (for [?require-clause (nnext sexpr)
              :when (contains? #{:require :require-macros} (first ?require-clause))
              libspec (rest ?require-clause)
              normalized-libspec (normalize-libspec nil libspec)
              analyzed (analyze-libspec lang normalized-libspec)]
          analyzed)
        refer-alls (reduce (fn [acc clause]
                             (if (:refered-all clause)
                               (assoc acc (:ns clause) (:excluded clause))
                               acc))
                           {}
                           subclauses)]
    (cond->
        {:type :ns
         :lang lang
         :name (or (second sexpr)
                   'user)
         :qualify-var (into {} (mapcat :refers subclauses))
         :qualify-ns (reduce (fn [acc sc]
                               (cond-> (assoc acc (:ns sc) (:ns sc))
                                 (:as sc)
                                 (assoc (:as sc) (:ns sc))))
                             {}
                             subclauses)
         :clojure-excluded (set (for [?refer-clojure (nnext sexpr)
                                      :when (= :refer-clojure (first ?refer-clojure))
                                      [k v] (partition 2 (rest ?refer-clojure))
                                      :when (= :exclude k)
                                      sym v]
                                  sym))
         :refer-alls refer-alls
         :loaded (into
                    (case lang
                      :clj '#{clojure.core}
                      :cljs '#{cljs.core})
                    (keys refer-alls))}
      (= :clj lang) (update :qualify-ns
                            #(assoc % 'clojure.core 'clojure.core))
      (= :cljs lang) (update :qualify-ns
                             #(assoc % 'cljs.core 'cljs.core))
      (contains? #{:clj :cljc} lang)
      (assoc :java-imports default-java-imports))))

(defn resolve-name
  [ns name-sym]
  (if-let [ns* (namespace name-sym)]
    (let [ns-sym (symbol ns*)]
      (if-let [ns* (get (:qualify-ns ns) ns-sym)]
        {:ns ns*
         :name (symbol (name name-sym))}
        (when-let [ns* (get (:java-imports ns) ns-sym)]
          {:java-interop? true
           :ns ns*
           :name (symbol (name name-sym))})))
    (or (get (:qualify-var ns)
             name-sym)
        (let [namespace (:name ns)]
          {:ns namespace
           :name name-sym
           :unqualified? true
           :clojure-excluded? (contains? (:clojure-excluded ns)
                                         name-sym)}))))

;;;; Scratch

(comment
  (keys (analyze-ns-decl :clj (parse-string (slurp "/tmp/nsform.clj"))))
  (:loaded (analyze-ns-decl :clj (parse-string (slurp "/tmp/nsform.clj"))))
  (:java-imports (analyze-ns-decl :clj (parse-string (slurp "/tmp/nsform.clj"))))
  (analyze-libspec :clj (node/sexpr (parse-string "[foo.core :refer :all :exclude [foo] :rename {old-name new-name}]")))
  
  )
