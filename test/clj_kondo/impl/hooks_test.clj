(ns clj-kondo.impl.hooks-test
  (:require
   [clj-kondo.hooks-api :as hooks-api]
   [clj-kondo.impl.core :as core]
   [clj-kondo.impl.utils :as utils :refer [parse-string *ctx*]]
   [clj-kondo.test-utils :refer [lint! make-dirs with-temp-dir]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest predicates-test
  (is (hooks-api/keyword-node? (parse-string ":foo")))
  (is (hooks-api/string-node? (parse-string "\"hello\"")))
  (is (hooks-api/string-node? (parse-string "\"hello
there
\"")))
  (is (hooks-api/token-node? (parse-string "foo")))
  (is (hooks-api/token-node? (parse-string "nil")))
  (is (hooks-api/token-node? (parse-string "1")))
  (is (hooks-api/vector-node? (parse-string "[1]")))
  (is (hooks-api/list-node? (parse-string "(+ 1 2 3)")))
  #_(is (hooks-api/map-node? (parse-string "{:a 1}"))))


(deftest ns-analysis-test
  (testing "ns-analysis is loaded from cache"
    (with-temp-dir [tmp-dir "ns-analysis-test"]
      (let [test-cache-dir  (.getPath (io/file tmp-dir "test-cache-dir"))
            test-source-dir (io/file tmp-dir "test-source-dir")
            foo-clj (io/file test-source-dir "foo.clj")
            bar-cljc (io/file test-source-dir "bar.cljc")
            baz-cljs (io/file test-source-dir (str "baz.cljs"))]
        (make-dirs test-cache-dir)
        (make-dirs test-source-dir)
        (io/copy
         "(ns foo) (defn foo [x]) (defn- foo-p [x & _]) (defmacro foo-m [x])"
         foo-clj)
        (io/copy
         "(ns bar) #?(:clj (defn bar-clj [])) #?(:cljs (defn bar-cljs []))"
         bar-cljc)
        (io/copy "(ns baz) (defn baz [x])" baz-cljs)

        ;; populate cache
        (lint! test-source-dir "--cache" "true" "--cache-dir" test-cache-dir)
        (let [full-cache-dir (io/file test-cache-dir core/cache-version)]
          (is (= {:clj {'foo {:ns   'foo
                              :name 'foo :fixed-arities #{1}}
                        'foo-p {:ns                'foo
                                :name              'foo-p
                                :varargs-min-arity 1
                                :private           true}
                        'foo-m {:ns            'foo :name 'foo-m
                                :fixed-arities #{1}
                                :macro         true}}}
                 (binding [*ctx* {:cache-dir full-cache-dir}]
                   (hooks-api/ns-analysis 'foo))
                 (binding [*ctx* {:cache-dir full-cache-dir}]
                   (hooks-api/ns-analysis 'foo {:lang :clj}))))
          (is (= {:clj  {'bar-clj {:ns            'bar
                                   :name          'bar-clj
                                   :fixed-arities #{0}}}
                  :cljs {'bar-cljs {:ns            'bar
                                    :name          'bar-cljs
                                    :fixed-arities #{0}}}}
                 (binding [*ctx* {:cache-dir full-cache-dir}]
                   (hooks-api/ns-analysis 'bar))
                 (binding [*ctx* {:cache-dir full-cache-dir}]
                   (hooks-api/ns-analysis 'bar {:lang :cljc}))))
          (is (= {:cljs {'baz {:ns 'baz, :name 'baz, :fixed-arities #{1}}}}
                 (binding [*ctx* {:cache-dir full-cache-dir}]
                   (hooks-api/ns-analysis 'baz))
                 (binding [*ctx* {:cache-dir full-cache-dir}]
                   (hooks-api/ns-analysis 'baz {:lang :cljs})))))))))

(deftest macroexpand-locations-test
  (let [node (parse-string "(foo.bar/baz 1 (2 3 x))")
        m (meta node)
        sexpr (hooks-api/sexpr node)
        sexpr `(do (let [~'x 1] ~sexpr))
        node (hooks-api/coerce sexpr)
        node (#'hooks-api/annotate node m)
        nodes (tree-seq :children :children node)]
    (is (every? (comp :row meta) nodes))))
