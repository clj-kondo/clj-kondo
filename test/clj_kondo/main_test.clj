(ns clj-kondo.main-test
  (:require
   [clj-kondo.main :as main :refer [-main]]
   [clojure.string :as str :refer [trim]]
   [clojure.test :as t :refer [deftest is testing]]
   [me.raynes.conch :refer [programs with-programs let-programs] :as sh]))

(defn parse-output [msg]
  (map (fn [[_ file row col level message]]
         {:file file
          :row (Integer/parseInt row)
          :col (Integer/parseInt col)
          :level (keyword level)
          :message message})
       (keep
        #(re-matches #"(.*):(.*):(.*): (.*): (.*)" %)
        (str/split-lines msg))))

(defn lint-jvm!
  ([input] (lint-jvm! input "clj"))
  ([input lang]
   (let [res (with-out-str
               (with-in-str input (-main "--lint" "-" "--lang" lang)))]
     (parse-output res))))

(defn lint-native!
  ([input] (lint-native! input "clj"))
  ([input lang]
   (let [res (let-programs [clj-kondo "./clj-kondo"]
               (clj-kondo "--lint" "-" "--lang" lang {:in input}))]
     (parse-output res))))

(def lint!
  (case (System/getenv "CLJ_KONDO_TEST_ENV")
    "jvm" lint-jvm!
    "native" lint-native!
    lint-jvm!))

(if (= lint! lint-jvm!)
  (println "==== Testing JVM version")
  (println "==== Testing native version"))

(def inline-def-examples "
(defn foo []
  (def x 1))

(defn- foo []
  (def x 1))

(def foo (def x 1))

(deftest foo (def x 1))

(defmacro foo [] (def x 1))
")

(deftest inline-def-test
  (let [linted (lint! inline-def-examples)]
    (is (= 5 (count linted)))
    (is (every? #(str/includes? % "inline def")
                linted)))
  (is (empty? (lint! "(defmacro foo [] `(def x 1))")))
  (is (empty? (lint! "(defn foo [] '(def x 3))")))
  (is (not-empty (lint! "(defmacro foo [] `(def x# (def x# 1)))"))))

(def obsolete-let-examples "
(let [x 1]
  (let [y 2]))

(let [x 1]
  #_(println \"hello\")
  (let [y 2]))

(let [x 1]
  ;; (println \"hello\")
  (let [y 2]))
")

(deftest obsolete-let-test
  (let [linted (lint! obsolete-let-examples)]
    (is (= 3 (count linted)))
    (is (every? #(str/includes? % "obsolete let")
                linted)))
  (is (empty? (lint! "(let [x 2] `(let [y# 3]))")))
  (is (empty? (lint! "(let [x 2] '(let [y 3]))"))))

(def obsolete-do-examples "
(do)
(do 1 (do 2))
(defn foo [] (do 1 2 3))
(fn [] (do 1 2))
(let [] 1 2 (do 1 2 3))
")

(deftest obsolete-do-test
  (let [linted (lint! obsolete-do-examples)]
    (is (= 5 (count linted)))
    (is (every? #(str/includes? % "obsolete do")
                linted)))
  (is (empty? (lint! "(do 1 `(do 1 2 3))")))
  (is (empty? (lint! "(do 1 '(do 1 2 3))"))))

(def invalid-arity-examples "
(ns ns1)
(defn public-fixed [x y z])
(defn public-multi-arity ([x] (public-multi-arity x false)) ([x y]))
#_5 (defn public-varargs [x y & zs])
;; 1: invalid call in own namespace to fixed arity
(public-fixed 1)
;; 2: invalid call in own namespace to  multi-arity
(public-multi-arity 1) ;; correct
#_10 (public-multi-arity 1 2) ;; correct
(public-multi-arity 1 2 3) ;; invalid
;; 3: invalid call in own namespace to varargs
(public-varargs 1)

#_15 (ns ns2 (:require [ns1 :as x :refer [public-fixed
                                          public-varargs
                                          public-multi-arity]]))
;; 4: invalid calls in other namespace to fixed arity
(public-fixed 1)
#_20 ;; 5:
(x/public-fixed 1)
;; 6:
(ns1/public-fixed 1) ;; this one is not detected
;; 7:
#_25 (public-multi-arity 1 2 3)
;; 8: invalid call in other namespace to varargs
(public-varargs 1)
")

(def invalid-core-function-call-example "
(ns clojure.core)
(defn inc [x])
(ns cljs.core)
(defn inc [x])

(ns myns)
(inc 1 2 3)
")

(def order-example "
;; call to def special form with docstring
(def x \"the number one\" 1)
(defmacro def [k spec-form])
;; valid call to macro
(def ::foo int?)
;; invalid call to macro
(def ::foo int? string?)
")

(deftest invalid-arity-test

  (let [linted (lint! invalid-arity-examples)]
    (is (= 8 (count linted)))
    (is (every? #(str/includes? % "Wrong number of args")
                linted)))

  (let [linted (lint! invalid-core-function-call-example)]
    (is (pos? (count linted)))
    (is (every? #(str/includes? % "Wrong number of args")
                linted)))

  (is (empty? (lint! "(defn foo [x]) (defn bar [foo] (foo))")))
  (is (empty? (lint! "(defn foo [x]) (let [foo (fn [])] (foo))")))

  (testing "macroexpansion of ->"
    (is (empty? (lint! "(defn inc [x] (+ x 1)) (-> x inc inc)")))
    (is (= 1 (count (lint! "(defn inc [x] (+ x 1)) (-> x inc (inc 1))")))))

  (testing "macroexpansion of fn literal"
    (is (= 1 (count (lint! "(defn inc [x] (+ x 1)) #(-> % inc (inc 1))")))))

  (testing "only invalid calls after (re-)definition are caught"
    (let [linted (lint! order-example)]
      (is (= 1 (count linted)))
      (is (= 8 (:row (first (lint! order-example))))))))

(def private-call-examples "
(ns ns1)
(defn- private [x y z])
;; this call is OK
(private 1 2 3)

(ns ns2 (:require [ns1 :as x :refer [private]]))

;; this call should be reported
(private 1 2 3)
")

(deftest private-call-test
  (let [linted (lint! private-call-examples)]
    (is (= 1 (count linted)))
    (is (= 10 (:row (first linted))))))

;;;; Scratch

(comment
  (inline-def-test)
  (obsolete-let-test)
  (obsolete-do-test)
  (invalid-arity-test)
  (t/run-tests)
  )
