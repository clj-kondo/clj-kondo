(ns clj-kondo.impl.vars-test
  (:require [clj-kondo.impl.vars :as vars]
            [clj-kondo.impl.utils :refer [parse-string parse-string-all]]
            [clojure.test :as t :refer [deftest is testing]]
            [rewrite-clj.parser :as p]
            [rewrite-clj.node.protocols :as node]))

(defn submap?
  "Is m1 a subset of m2? Taken from
  https://github.com/clojure/spec-alpha2, clojure.test-clojure.spec"
  [m1 m2]
  (if (and (map? m1) (map? m2))
    (every? (fn [[k v]] (and (contains? m2 k)
                             (submap? v (get m2 k))))
            m1)
    (= m1 m2)))

(deftest strip-meta-test
  (is (= "(defnchunk-buffer[capacity](clojure.lang.ChunkBuffer.capacity))"
         (str (vars/strip-meta (parse-string "(defn ^:static ^:foo chunk-buffer ^:bar [capacity]
  (clojure.lang.ChunkBuffer. capacity))"))))))

(deftest parse-defn-test
  (is (every? true?
              (map submap?
                   '[{:type :defn, :name chunk-buffer, :fixed-arities #{1}}
                     {:type :call, :name clojure.lang.ChunkBuffer., :arity 1, :row 2, :col 3}](vars/parse-defn :clj #{} (parse-string "(defn ^:static ^clojure.lang.ChunkBuffer chunk-buffer ^clojure.lang.ChunkBuffer [capacity]
  (clojure.lang.ChunkBuffer. capacity))"))))))

(deftest analyze-ns-test
  (is
   (= (vars/analyze-ns-decl
       (parse-string "(ns foo (:require [bar :as baz :refer [quux]])
                              (:refer-clojure :exclude [get assoc time]))"))
      '{:type :ns, :name foo,
        :qualify-var {quux {:namespace bar, :name bar/quux}}
        :qualify-ns {bar bar
                     baz bar}
        :clojure-excluded #{get assoc time}}))
  (testing "string namespaces should be allowed in require"
    (is (submap?
         '{:type :ns, :name foo
           :qualify-ns {bar bar
                        baz bar}}
         (vars/analyze-ns-decl
          (parse-string "(ns foo (:require [\"bar\" :as baz]))"))))))

(deftest qualify-name-test
  (let [ns (vars/analyze-ns-decl
            (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))]
    (is (= '{:namespace bar, :name bar/quux}
           (vars/qualify-name ns 'quux))))
  (let [ns (vars/analyze-ns-decl
            (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))]
    (is (= '{:namespace bar, :name bar/quux}
           (vars/qualify-name ns 'quux))))
  (let [ns (vars/analyze-ns-decl
            (parse-string "(ns clj-kondo.impl.utils {:no-doc true} (:require [rewrite-clj.parser :as p]))
"))]
    (is (= '{:namespace rewrite-clj.parser, :name rewrite-clj.parser/parse-string}
           (vars/qualify-name ns 'p/parse-string))))
  (testing "referring to unknown namespace alias"
    (let [ns (vars/analyze-ns-decl
              (parse-string "(ns clj-kondo.impl.utils {:no-doc true})
"))]
      (nil? (vars/qualify-name ns 'p/parse-string))))
  (testing "referring with full namespace"
    (let [ns (vars/analyze-ns-decl
              (parse-string "(ns clj-kondo.impl.utils (:require [clojure.core]))
(clojure.core/inc 1)
"))]
      (vars/qualify-name ns 'clojure.core/inc))))

(deftest analyze-arities-test
  (is (submap?
       '{:calls
         {bar
          [{:type :call,
            :name quux,
            :qname bar/quux,
            :arity 1,
            :row 4,
            :col 1,
            :ns foo,
            :filename "<stdin>",
            :lang :clj}]},
         :defns
         {bar
          #:bar{quux
                {:type :defn,
                 :name quux,
                 :qname bar/quux,
                 :fixed-arities #{3},
                 :ns bar,
                 :filename "<stdin>",
                 :lang :clj}}}}
       (first (vars/analyze-arities "<stdin>" :clj
                                    (parse-string-all "
#_1 (ns bar) (defn quux [a b c])
#_2 (ns foo (:require [bar :as baz :refer [quux]]))
(quux 1)
")))))
  (is (submap?
       '{:calls {rewrite-clj.parser
                 [{:type :call,
                   :name p/parse-string,
                   :qname rewrite-clj.parser/parse-string,
                   :arity 1, :row 5, :col 5,
                   :ns clj-kondo.impl.utils,
                   :filename "<stdin>", :lang :clj}]}
         :defns {}}
       (first (vars/analyze-arities "<stdin>" :clj
                                    (parse-string-all "
#_1 (ns clj-kondo.impl.utils
#_2  {:no-doc true}
#_3  (:require [rewrite-clj.parser :as p]))
#_4 (p/parse-string \"(+ 1 2 3)\")
")))))
  (testing "calling functions from own ns"
    (is (submap?
         '{:calls
           {clj-kondo.main
            [{:type :call,
              :name foo,
              :qname clj-kondo.main/foo,
              :arity 1,
              :row 3,
              :col 20,
              :ns clj-kondo.main,
              :filename "<stdin>",
              :lang :clj}]},
           :defns
           {clj-kondo.main
            #:clj-kondo.main{foo
                             {:type :defn,
                              :name foo,
                              :qname clj-kondo.main/foo,
                              :fixed-arities #{1},
                              :ns clj-kondo.main,
                              :filename "<stdin>",
                              :lang :clj}}}}
         (first (vars/analyze-arities "<stdin>" :clj
                                      (parse-string-all "
#_1 (ns clj-kondo.main)
#_2 (defn foo [x]) (foo 1)
"))))))
  (testing "calling functions from file without ns form"
    (is (submap?
         '{:calls
           {user [{:type :call, :name foo, :qname user/foo,
                   :arity 1, :row 2, :col 16, :ns user, :filename "<stdin>", :lang :clj}]},
           :defns
           {user #:user{foo {:type :defn, :name foo,
                             :qname user/foo, :fixed-arities #{1},
                             :ns user, :filename "<stdin>", :lang :clj}}}}
         (first (vars/analyze-arities "<stdin>" :clj
                                      (parse-string-all "
(defn foo [x]) (foo 1)
")))))))

(deftest analyze-arities-cljc-test
  (vars/analyze-arities "<stdin>" :clj
                        (parse-string-all "
#?(:cljs (defn foo []))
"))

  (vars/analyze-arities "<stdin>" :clj
                        (parse-string-all "
#?(:cljs (foo 1 2 3) :clj (bar 1 2 3))
")))
;;

(comment
  (t/run-tests)
  (vars/analyze-ns-decl
   (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))
  )

