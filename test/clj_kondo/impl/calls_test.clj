(ns clj-kondo.impl.calls-test
  (:require
   [clj-kondo.impl.namespace :refer [analyze-ns-decl]]
   [clj-kondo.impl.calls :as calls :refer [analyze-calls]]
   [clj-kondo.impl.utils :refer [parse-string parse-string-all]]
   [clj-kondo.test-utils :refer [assert-submap assert-some-submap assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest strip-meta-test
  (is (= "(defnchunk-buffer[capacity](clojure.lang.ChunkBuffer.capacity))"
         (str (calls/strip-meta (parse-string "(defn ^:static ^:foo chunk-buffer ^:bar [capacity]
  (clojure.lang.ChunkBuffer. capacity))"))))))

(deftest parse-defn-test
  (assert-submaps
   '[{:name chunk-buffer, :fixed-arities #{1}}
     {:type :call :name defn}
     {:type :call, :name clojure.lang.ChunkBuffer., :arity 1, :row 2, :col 3}]
   (calls/parse-defn :clj #{}
                    (parse-string
                     "(defn ^:static ^clojure.lang.ChunkBuffer chunk-buffer ^clojure.lang.ChunkBuffer [capacity]
  (clojure.lang.ChunkBuffer. capacity))")))
  (assert-submap '{:type :defn
                   :name get-bytes,
                   :row 1,
                   :col 1,
                   :lang :clj,
                   :fixed-arities #{1}}
                 (first (calls/parse-defn :clj #{}
                                         (parse-string "(defn get-bytes #^bytes [part] part)")))))

(deftest resolve-name-test
  (let [ns (analyze-ns-decl
            :clj
            (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))]
    (is (= '{:ns bar :name quux}
           (calls/resolve-name ns 'quux))))
  (let [ns (analyze-ns-decl
            :clj
            (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))]
    (is (= '{:ns bar :name quux}
           (calls/resolve-name ns 'quux))))
  (let [ns (analyze-ns-decl
            :clj
            (parse-string "(ns clj-kondo.impl.utils {:no-doc true} (:require [rewrite-clj.parser :as p]))
"))]
    (is (= '{:ns rewrite-clj.parser :name parse-string}
           (calls/resolve-name ns 'p/parse-string))))
  (testing "referring to unknown namespace alias"
    (let [ns (analyze-ns-decl
              :clj
              (parse-string "(ns clj-kondo.impl.utils {:no-doc true})
"))]
      (nil? (calls/resolve-name ns 'p/parse-string))))
  (testing "referring with full namespace"
    (let [ns (analyze-ns-decl
              :clj
              (parse-string "(ns clj-kondo.impl.utils (:require [clojure.core]))
(clojure.core/inc 1)
"))]
      ;; TODO: what's the test here?
      (is (=
           '{:ns clojure.core :name inc}
           (calls/resolve-name ns 'clojure.core/inc))))))

(deftest analyze-calls-test
  (let [analyzed (analyze-calls "<stdin>" :clj
                                  (parse-string-all "
#_1 (ns bar) (defn quux [a b c])
#_2 (ns foo (:require [bar :as baz :refer [quux]]))
(quux 1)
"))]
    (assert-some-submap '{:type :call,
                          :name quux,
                          :arity 1,
                          :row 4,
                          :col 1,
                          :ns foo,
                          :lang :clj}
                        (get-in analyzed '[:calls bar]))
    (assert-submap '{quux
                     {:name quux,
                      :fixed-arities #{3},
                      :ns bar
                      :lang :clj}}
                   (get-in analyzed '[:defs bar])))
  (let [analyzed (analyze-calls "<stdin>" :clj
                                  (parse-string-all "
#_1 (ns clj-kondo.impl.utils
#_2  {:no-doc true}
#_3  (:require [rewrite-clj.parser :as p]))
#_4 (p/parse-string \"(+ 1 2 3)\")
"))]
    analyzed
    (assert-submap '{:type :call,
                     :name parse-string ;;p/parse-string,
                     :arity 1, :row 5, :col 5,
                     :ns clj-kondo.impl.utils,
                     :lang :clj}
                   (get-in analyzed '[:calls rewrite-clj.parser 0])))
  (testing "calling functions from own ns"
    (let [analyzed (analyze-calls "<stdin>" :clj
                                    (parse-string-all "
#_1 (ns clj-kondo.main)
#_2 (defn foo [x]) (foo 1)
"))]
      (assert-some-submap '{:type :call,
                            :name foo,
                            :arity 1,
                            :row 3,
                            :col 20,
                            :ns clj-kondo.main,
                            :lang :clj}
                          (get-in analyzed '[:calls clj-kondo.main]))
      (assert-submap '{foo
                       {:name foo,
                        :fixed-arities #{1},
                        :ns clj-kondo.main,
                        :lang :clj}}
                     (get-in analyzed '[:defs clj-kondo.main]))))
  (testing "calling functions from file without ns form"
    (let [analyzed (analyze-calls "<stdin>" :clj
                                    (parse-string-all "
(defn foo [x]) (foo 1)
"))]
      (assert-some-submap '{:type :call, :name foo,
                            :arity 1, :row 2, :col 16, :ns user, :lang :clj}
                          (get-in analyzed '[:calls user]))
      (assert-submap '{foo {:name foo,
                            :fixed-arities #{1},
                            :ns user, :lang :clj}}
                     (get-in analyzed '[:defs user])))))

#_(deftest analyze-calls-cljc-test
    (analyze-calls "<stdin>" :clj
                     (parse-string-all "
#?(:cljs (defn foo []))
"))

    (analyze-calls "<stdin>" :clj
                     (parse-string-all "
#?(:cljs (foo 1 2 3) :clj (bar 1 2 3))
")))
;;

(comment
  (t/run-tests)
  (analyze-ns-decl
   :clj
   (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))
  )
