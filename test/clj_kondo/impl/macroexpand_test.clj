(ns clj-kondo.impl.macroexpand-test
  (:require
   [clj-kondo.impl.macroexpand :as macroexpand]
   [clj-kondo.impl.utils :refer [parse-string]]
   [clojure.test :as t :refer [deftest is testing]]
   [rewrite-clj.node.protocols :as node :refer [tag]]))

(defn location [node]
  (let [m (meta node)]
    (when (and (:row m) (:col m))
      m)))

(deftest expand->-test
  (testing "Expanded -> expression preserves location"
    (is (every? location
                (filter #(= :list (tag %))
                        (tree-seq :children :children
                                  (macroexpand/expand-> "."
                                   (parse-string "(-> 1 inc inc)")))))))
  (testing "with metadata"
    (is (= '(clojure.string/includes? (str "foo") "foo")
           (node/sexpr
            (macroexpand/expand-> "."
             (parse-string "(-> \"foo\" ^String str (clojure.string/includes? \"foo\"))")))))))

(deftest expand-fn-test
  (testing
      "Expanded function literals have a location for the function they call"
    (let [fn-body (-> (macroexpand/expand-fn
                       (parse-string "#(valid? %)"))
                      :children
                      nnext
                      first
                      :children
                      nnext
                      first)]
      (is
       (every? location
               (filter #(= :list (tag %))
                       (tree-seq :children :children
                                 fn-body))))))
  (is (= '(fn [%1] (clojure.core/let [% %1] nil (println % %)))
         (node/sexpr
          (macroexpand/expand-fn
           (parse-string "#(println % %)")))))
  (is (= '(fn [%1 %2] (clojure.core/let [% %1] nil (println % %2)))
         (node/sexpr
          (macroexpand/expand-fn
           (parse-string "#(println % %2)")))))
  (is (= '(fn [%1 %2 & %&] (clojure.core/let [% %1] nil (apply println % %2 %&)))
         (node/sexpr
          (macroexpand/expand-fn
           (parse-string "#(apply println % %2 %&)"))))))


;;;; Scratch

(comment
  )
