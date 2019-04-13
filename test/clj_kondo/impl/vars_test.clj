(ns clj-kondo.impl.vars-test
  (:require [clj-kondo.impl.vars :as vars]
            [clj-kondo.impl.utils :refer [parse-string parse-string-all]]
            [clj-kondo.test-utils :refer [submap?]]
            [clojure.test :as t :refer [deftest is testing]]
            [rewrite-clj.parser :as p]
            [rewrite-clj.node.protocols :as node]))

(deftest strip-meta-test
  (is (= "(defnchunk-buffer[capacity](clojure.lang.ChunkBuffer.capacity))"
         (str (vars/strip-meta (parse-string "(defn ^:static ^:foo chunk-buffer ^:bar [capacity]
  (clojure.lang.ChunkBuffer. capacity))"))))))

(deftest parse-defn-test
  (is (every? true?
              (map submap?
                   '[{:type :defn, :name chunk-buffer, :fixed-arities #{1}}
                     {:type :call, :name clojure.lang.ChunkBuffer., :arity 1, :row 2, :col 3}]
                   (vars/parse-defn :clj #{}
                                    (parse-string
                                     "(defn ^:static ^clojure.lang.ChunkBuffer chunk-buffer ^clojure.lang.ChunkBuffer [capacity]
  (clojure.lang.ChunkBuffer. capacity))")))))
  (is (= '({:type :defn,
            :name get-bytes,
            :row 1,
            :col 1,
            :lang :clj,
            :fixed-arities #{1}})
         (vars/parse-defn :clj #{}
                          (parse-string "(defn get-bytes #^bytes [part] part)")))))

(deftest analyze-ns-test
  (is
   (submap?
    '{:type :ns, :name foo,
      :qualify-var {quux {:namespace bar, :name bar/quux}}
      :qualify-ns {bar bar
                   baz bar}
      :clojure-excluded #{get assoc time}}
    (vars/analyze-ns-decl
     :clj
     (parse-string "(ns foo (:require [bar :as baz :refer [quux]])
                              (:refer-clojure :exclude [get assoc time]))"))))
  (testing "string namespaces should be allowed in require"
    (is (submap?
         '{:type :ns, :name foo
           :qualify-ns {bar bar
                        baz bar}}
         (vars/analyze-ns-decl
          :clj
          (parse-string "(ns foo (:require [\"bar\" :as baz]))"))))))

(deftest qualify-name-test
  (let [ns (vars/analyze-ns-decl
            :clj
            (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))]
    (is (= '{:namespace bar, :name bar/quux}
           (vars/qualify-name ns 'quux))))
  (let [ns (vars/analyze-ns-decl
            :clj
            (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))]
    (is (= '{:namespace bar, :name bar/quux}
           (vars/qualify-name ns 'quux))))
  (let [ns (vars/analyze-ns-decl
            :clj
            (parse-string "(ns clj-kondo.impl.utils {:no-doc true} (:require [rewrite-clj.parser :as p]))
"))]
    (is (= '{:namespace rewrite-clj.parser, :name rewrite-clj.parser/parse-string}
           (vars/qualify-name ns 'p/parse-string))))
  (testing "referring to unknown namespace alias"
    (let [ns (vars/analyze-ns-decl
              :clj
              (parse-string "(ns clj-kondo.impl.utils {:no-doc true})
"))]
      (nil? (vars/qualify-name ns 'p/parse-string))))
  (testing "referring with full namespace"
    (let [ns (vars/analyze-ns-decl
              :clj
              (parse-string "(ns clj-kondo.impl.utils (:require [clojure.core]))
(clojure.core/inc 1)
"))]
      (vars/qualify-name ns 'clojure.core/inc))))

(deftest analyze-arities-test
  (let [analyzed (first (vars/analyze-arities "<stdin>" :clj
                                              (parse-string-all "
#_1 (ns bar) (defn quux [a b c])
#_2 (ns foo (:require [bar :as baz :refer [quux]]))
(quux 1)
")))]
    (is (submap? '{:type :call,
                   :name quux,
                   :qname bar/quux,
                   :arity 1,
                   :row 4,
                   :col 1,
                   :ns foo,
                   :filename "<stdin>",
                   :lang :clj}
                 (get-in analyzed '[:calls bar 0])))
    (is (submap? '#:bar{quux
                        {:type :defn,
                         :name quux,
                         :qname bar/quux,
                         :fixed-arities #{3},
                         :ns bar,
                         :filename "<stdin>",
                         :lang :clj}}
                 (get-in analyzed '[:defs bar]))))
  (let [analyzed (first (vars/analyze-arities "<stdin>" :clj
                                              (parse-string-all "
#_1 (ns clj-kondo.impl.utils
#_2  {:no-doc true}
#_3  (:require [rewrite-clj.parser :as p]))
#_4 (p/parse-string \"(+ 1 2 3)\")
")))]
    (is (submap? '{:type :call,
                   :name p/parse-string,
                   :qname rewrite-clj.parser/parse-string,
                   :arity 1, :row 5, :col 5,
                   :ns clj-kondo.impl.utils,
                   :filename "<stdin>", :lang :clj}
                 (get-in analyzed '[:calls rewrite-clj.parser 0]))))
  (testing "calling functions from own ns"
    (let [analyzed (first (vars/analyze-arities "<stdin>" :clj
                                                (parse-string-all "
#_1 (ns clj-kondo.main)
#_2 (defn foo [x]) (foo 1)
")))]
      (is (submap? '{:type :call,
                     :name foo,
                     :qname clj-kondo.main/foo,
                     :arity 1,
                     :row 3,
                     :col 20,
                     :ns clj-kondo.main,
                     :filename "<stdin>",
                     :lang :clj}
                   (get-in analyzed '[:calls clj-kondo.main 0])))
      (is (submap? '#:clj-kondo.main{foo
                                     {:type :defn,
                                      :name foo,
                                      :qname clj-kondo.main/foo,
                                      :fixed-arities #{1},
                                      :ns clj-kondo.main,
                                      :filename "<stdin>",
                                      :lang :clj}}
                   (get-in analyzed '[:defs clj-kondo.main])))))
  (testing "calling functions from file without ns form"
    (let [analyzed (first (vars/analyze-arities "<stdin>" :clj
                                                (parse-string-all "
(defn foo [x]) (foo 1)
")))]
      (is (submap? '{:type :call, :name foo, :qname user/foo,
                     :arity 1, :row 2, :col 16, :ns user, :filename "<stdin>", :lang :clj}
                   (get-in analyzed '[:calls user 0])))
      (is (submap? '#:user{foo {:type :defn, :name foo,
                                :qname user/foo, :fixed-arities #{1},
                                :ns user, :filename "<stdin>", :lang :clj}}
                   (get-in analyzed '[:defs user]))))))

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
   :clj
   (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))
  )
