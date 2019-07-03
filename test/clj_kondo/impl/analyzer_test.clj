(ns clj-kondo.impl.analyzer-test
  (:require
   [clj-kondo.impl.analyzer :as ana :refer [analyze-expressions]]
   [clj-kondo.impl.analyzer.namespace :refer [analyze-ns-decl]]
   [clj-kondo.impl.metadata :as meta]
   [clj-kondo.impl.utils :refer [parse-string parse-string-all]]
   [clj-kondo.test-utils :refer [assert-submap assert-some-submap assert-submaps]]
   [clojure.test :as t :refer [deftest is are testing]]))

(deftest lift-meta-test
  (is (:private (meta (meta/lift-meta-content2 {:lang :clj
                                               :namespaces (atom {})}
                                              (parse-string "^:private [x]")))))
  (is (:private (meta (meta/lift-meta-content2 {:lang :clj
                                               :namespaces (atom {})}
                                              (parse-string "#^ :private [x]")))))
  (is (= "[B" (:tag (meta (meta/lift-meta-content2 {:lang :clj
                                                    :namespaces (atom {})}
                                                   (parse-string "^\"[B\" body")))))))

(def ctx
  (let [ctx {:filename "-"
             :namespaces (atom {})
             :findings (atom [])
             :base-lang :clj
             :lang :clj
             :bindings {}}]
    (assoc ctx :ns (analyze-ns-decl ctx (parse-string "(ns user)")))))

(deftest analyze-defn-test
  (assert-submaps
   '[{:type :defn
      :name chunk-buffer, :fixed-arities #{1}}
     {:type :call, :name clojure.lang.ChunkBuffer., :arity 1, :row 2, :col 3}]
   (ana/analyze-defn ctx
                     (parse-string
                      "(defn ^:static ^clojure.lang.ChunkBuffer chunk-buffer ^clojure.lang.ChunkBuffer [capacity]
  (clojure.lang.ChunkBuffer. capacity))")))
  (assert-submap '{:type :defn
                   :name get-bytes,
                   :row 1,
                   :col 1,
                   :lang :clj,
                   :fixed-arities #{1}}
                 (first (ana/analyze-defn ctx
                                          (parse-string "(defn get-bytes #^bytes [part] part)")))))

(deftest analyze-expressions-test
  (let [analyzed (analyze-expressions {:filename "<stdin>" :base-lang :clj :lang :clj
                                       :namespaces (atom {})
                                       :expressions (:children (parse-string-all "
#_1 (ns bar) (defn quux [a b c])
#_2 (ns foo (:require [bar :as baz :refer [quux]]))
(quux 1)
"))})]
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
  (let [analyzed (analyze-expressions {:filename "<stdin>" :base-lang :clj :lang :clj
                                       :namespaces (atom {})
                                       :expressions
                                       (:children (parse-string-all "
#_1 (ns clj-kondo.impl.utils
#_2  {:no-doc true}
#_3  (:require [rewrite-clj.parser :as p]))
#_4 (p/parse-string \"(+ 1 2 3)\")
"))})]
    analyzed
    (assert-submap '{:type :call,
                     :name parse-string ;;p/parse-string,
                     :arity 1, :row 5, :col 5,
                     :ns clj-kondo.impl.utils,
                     :lang :clj}
                   (get-in analyzed '[:calls rewrite-clj.parser 0])))
  (testing "calling functions from own ns"
    (let [analyzed (analyze-expressions {:filename "<stdin>" :base-lang :clj :lang :clj
                                         :namespaces (atom {})
                                         :expressions (:children (parse-string-all "
#_1 (ns clj-kondo.main)
#_2 (defn foo [x]) (foo 1)
"))})]
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
    (let [analyzed (analyze-expressions {:filename "<stdin>" :base-lang :clj :lang :clj
                                         :namespaces (atom {})
                                         :expressions
                                         (:children (parse-string-all "
(defn foo [x]) (foo 1)
"))})]
      (assert-some-submap '{:type :call, :name foo,
                            :arity 1, :row 2, :col 16, :ns user, :lang :clj}
                          (get-in analyzed '[:calls user]))
      (assert-submap '{foo {:name foo,
                            :fixed-arities #{1},
                            :ns user, :lang :clj}}
                     (get-in analyzed '[:defs user])))))

(deftest extract-bindings-test
  (are [syms binding-form] (= syms (keys (ana/extract-bindings ctx
                                                               (parse-string (str binding-form)))))
    '[x y z] '[x y [z [x]]]
    '[x y zs xs] '[x y & zs :as xs]
    '[x foo :analyzed] '[x {foo :foo :or {foo 1}}]
    '[x foo] '[x {:keys [foo]}]
    '[x foo m] '[x {:keys [foo] :as m}]
    '[x foo] "[x {:person/keys [foo]}]"
    '[x foo] "[x #:person{:keys [foo]}]"
    '[x foo] '[x {:keys [::foo]}]
    '[str-foo str-bar] "{:strs [str-foo str-bar]}"
    '[sym-foo sym-bar] "{:syms [sym-foo sym-bar]}"))

(comment
  (t/run-tests)
  (analyze-ns-decl
   :clj
   (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))
  )
