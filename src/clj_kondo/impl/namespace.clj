(ns clj-kondo.impl.namespace
  (:require
   [clj-kondo.impl.utils :refer [parse-string]]
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

(defn analyze-require-subclause* [lang libspec]
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
             as nil
             refers []]
        (if-let [child (first children)]
          (case child
            :refer
            (recur
             (nnext children)
             as
             (into refers (let [referred (fnext children)]
                            ;; referred could be :all
                            (when (sequential? referred)
                              referred))))
            :as
            (recur
             (nnext children)
             (fnext children)
             refers))
          [{:type :require
            :ns ns-name
            :as as
            :refers (map (fn [refer]
                           [refer {:ns ns-name
                                   :name refer}])
                         refers)}])))))

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
  (let [sexpr (node/sexpr expr)
        subclauses
        (for [?require-clause (nnext sexpr)
              :when (= :require (first ?require-clause))
              libspec (rest ?require-clause)
              normalized-libspec (normalize-libspec nil libspec)
              analyzed (analyze-require-subclause* lang normalized-libspec)]
          analyzed)]
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
                                  sym))}
      (= :clj lang) (update :qualify-ns
                            #(assoc % 'clojure.core 'clojure.core))
      (= :cljs lang) (update :qualify-ns
                             #(assoc % 'cljs.core 'cljs.core))
      (contains? #{:clj :cljc} lang)
      (assoc :java-imports default-java-imports))))

;;;; Scratch

(comment
  (analyze-ns-decl :clj (parse-string (slurp "/tmp/nsform.clj")))
  )
