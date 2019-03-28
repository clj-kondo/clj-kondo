(ns clj-kondo.main-test
  (:require
   [clj-kondo.main :as main :refer [-main]]
   [clojure.string :as str :refer [trim]]
   [clojure.test :as t :refer [deftest is testing]]
   [me.raynes.conch :refer [programs with-programs let-programs] :as sh]))

(defn parse-output [msg]
  (map (fn [[_ file row col level message]]
         {:file file
          :row (Integer. row)
          :col (Integer. col)
          :level (keyword level)
          :message message})
       (keep
        #(re-matches #"(.*):(.*):(.*): (.*): (.*)" %)
        (str/split-lines msg))))

(defn lint-jvm! [input]
  (let [res (with-out-str
              (with-in-str input (-main "--lint" "-")))]
    (parse-output res)))

(defn lint-native! [input]
  (let [res (let-programs [clj-kondo "./clj-kondo"]
              (clj-kondo "--lint" "-" {:in input}))]
    (parse-output res)))

(def lint!
  (case (System/getenv "CLJK_TEST_ENV")
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
(defn public-varargs [x y & zs])
;; 1: invalid call in own namespace to fixed arity
(public-fixed 1)
;; 2: invalid call in own namespace to  multi-arity
(public-multi-arity 1) ;; correct
(public-multi-arity 1 2) ;; correct
(public-multi-arity 1 2 3) ;; invalid
;; 3: invalid call in own namespace to varargs
(public-varargs 1)

(ns ns2 (:require [ns1 :as x :refer [public-fixed
                                     public-varargs
                                     public-multi-arity]]))
;; 4: invalid calls in other namespace to fixed arity
(public-fixed 1)
;; 5:
(x/public-fixed 1)
;; 6:
(ns1/public-fixed 1)
;; 7:
(public-multi-arity 1 2 3)
;; 8: invalid call in other namespace to varargs
(public-varargs 1)
")

(deftest invalid-arity-test
  (let [linted (lint! invalid-arity-examples)]
    (is (= 8 (count linted)))
    (is (every? #(str/includes? % "Wrong number of args")
                linted))
    (empty? (lint! "(defn foo [x]) (defn bar [foo] (foo))"))
    (empty? (lint! "(defn foo [x]) (let [foo (fn [])] (foo))"))
    (testing "macroexpansion of ->"
      (is (= 1 (count (lint! "(defn inc [x] (+ x 1)) (-> x inc (inc 1))")))))
    (testing "macroexpansion of fn literal"
      (is (= 1 (count (lint! "(defn inc [x] (+ x 1)) #(-> % inc (inc 1))")))))
    (empty? (lint! "(defn inc [x] (+ x 1)) (-> x inc inc)"))))

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
