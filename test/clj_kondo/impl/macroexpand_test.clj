(ns clj-kondo.impl.macroexpand-test
  (:require
   [clj-kondo.impl.macroexpand :as macroexpand]
   [clj-kondo.impl.utils :refer [parse-string tag sexpr]]
   [clojure.test :as t :refer [deftest is testing]]))

(defn location [node]
  (let [m (meta node)]
    (when (and (:row m) (:col m))
      m)))

(deftest expand->-test
  (testing "Expanded -> expression preserves location"
    (is (every? location
                (filter #(= :list (tag %))
                        (tree-seq :children :children
                                  (macroexpand/expand-> {}
                                   (parse-string "(-> 1 inc inc)")))))))
  (testing "with metadata"
    (is (= '(clojure.string/includes? (str "foo") "foo")
           (sexpr
            (macroexpand/expand-> {}
             (parse-string "(-> \"foo\" ^String str (clojure.string/includes? \"foo\"))")))))))

(deftest expand-fn-test
  (testing
      "Expanded function literals have a location for the function they call"
    (let [fn-body (macroexpand/expand-fn
                   (parse-string "#(valid? %)"))]
      (is
       (every? location
               (filter #(= :list (tag %))
                       (tree-seq :children :children
                                 fn-body)))))))


;;;; Scratch

(comment
  )
