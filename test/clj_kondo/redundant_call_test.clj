(ns clj-kondo.redundant-call-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps assert-submaps2]]
   [clojure.test :refer [deftest is testing]]))

(def config
  ^:replace {:linters {:redundant-call {:level :warning}}})

(deftest redundant-call-test
  (doseq [sym `[-> ->> some-> some->> partial comp merge]]
    (is (empty? (lint! (format "(%s 1 identity)" sym) config))))
  (doseq [sym `[-> ->> cond-> cond->> some-> some->> partial comp merge]]
    (assert-submaps
     [{:level :warning :message (format "Single arg use of %s always returns the arg itself" sym)}]
     (lint! (format "(%s 1)" sym) config)))
  (doseq [sym `[-> ->> cond-> cond->> some-> some->> partial]]
    (assert-submaps
     [{:level :warning :message (format "Single arg use of %s always returns the arg itself" sym)}
      {:level :warning :message (format "Single arg use of %s always returns the arg itself" sym)}]
     (lint! (format "(%s (%s 1))" sym sym) config))))

(deftest redundant-call-config-test
  (testing "default level is :off"
    (is (empty? (lint! "(-> 1)"))))
  (testing ":include works"
    (let [cfg (assoc-in config [:linters :redundant-call :include] `#{inc})]
      (assert-submaps
       [{:level :warning :message "Single arg use of clojure.core/inc always returns the arg itself"}]
       (lint! "(inc 1)" cfg))))
  (testing ":exclude works"
    (let [cfg (assoc-in config [:linters :redundant-call :exclude] `#{->})]
      (is (empty? (lint! "(-> 1)" cfg)))))
  (testing ":include and :exclude work together"
    (assert-submaps
     [{:level :warning :message "Single arg use of clojure.core/inc always returns the arg itself"}]
     (let [cfg (-> config
                   (assoc-in [:linters :redundant-call :include] `#{inc})
                   (assoc-in [:linters :redundant-call :exclude] `#{->}))]
       (lint! "(-> (inc 1))" cfg)))
    ;; :include two new vars to lint, :exclude one of them and a built-in, should
    ;; only warn for the remaining new var
    (let [cfg (-> config
                  (assoc-in [:linters :redundant-call :include] `#{inc dec})
                  (assoc-in [:linters :redundant-call :exclude] `#{inc ->}))]
      (assert-submaps
       [{:level :warning :message "Single arg use of clojure.core/dec always returns the arg itself"}]
       (lint! "(inc 1) (dec 1) (-> 1)" cfg)))))

(deftest redundant-call-in-cljc-test
  (is (= 2 (count (lint! "(-> 1)"
                         config
                         "--lang" "cljc"))))
  (is (empty? (lint! "(-> 1 #?(:clj inc))"
                     config
                     "--lang" "cljc"))))

(deftest redundant-str-call-test
  (let [my-config {:linters {:redundant-str-call {:level :warning}
                             :type-mismatch {:level :error}}}]
    (assert-submaps2
     '({:file "<stdin>", :row 2, :col 1, :level :warning, :message "Single argument to str already is a string"})
     (lint! "
(str (format \"dude\"))
#_:clj-kondo/ignore (str (format \"dude\"))
(str 1)
(str \"foo\" \"bar\")
(require '[clojure.test :refer [are]])
(are [x y] (= x (str y))
  \"foo\" \"bar\"
  \"foo\" 1)
" my-config))
    (is (empty? (lint! "(str \"foo\")"
                       (assoc-in my-config [:linters :redundant-str-call :level] :off))))))

;; Test for :redundant-primitive-coercion linter
;; Checks that coercion functions warn when applied to expressions already of that type
(deftest redundant-primitive-coercion-test
  (let [cfg {:linters {:redundant-primitive-coercion {:level :warning}
                       :type-mismatch {:level :error}}}]
    (testing "redundant-primitive-coercion"
      (testing "warns on double coercion of double-returning fn"
        (assert-submaps
         [{:level :warning
           :message "Redundant double coercion - expression already has type double"}]
         (lint! "(defn foo ^double [] 1.0) (double (foo))" cfg)))
      (testing "warns on float coercion of float-returning fn"
        (assert-submaps
         [{:level :warning
           :message "Redundant float coercion - expression already has type float"}]
         (lint! "(defn foo ^float [] 1.0) (float (foo))" cfg)))
      (testing "warns on long coercion of long-returning fn"
        (assert-submaps
         [{:level :warning
           :message "Redundant long coercion - expression already has type long"}]
         (lint! "(defn foo ^long [] 1) (long (foo))" cfg)))
      (testing "warns on nested double coercions"
        (assert-submaps
         [{:level :warning
           :message "Redundant double coercion - expression already has type double"}]
         (lint! "(double (double 1))" cfg)))
      (testing "warns on nested float coercions"
        (assert-submaps
         [{:level :warning
           :message "Redundant float coercion - expression already has type float"}]
         (lint! "(float (float 1))" cfg)))
      (testing "warns on int coercion of int-returning fn"
        (assert-submaps
         [{:level :warning
           :message "Redundant int coercion - expression already has type int"}]
         (lint! "(int (int 1))" cfg)))
      (testing "warns on long coercion of long-returning fn"
        (assert-submaps
         [{:level :warning
           :message "Redundant long coercion - expression already has type long"}]
         (lint! "(long (long 1))" cfg)))
      (testing "warns on short coercion of short-returning fn"
        (assert-submaps
         [{:level :warning
           :message "Redundant short coercion - expression already has type short"}]
         (lint! "(short (short 1))" cfg)))
      (testing "warns on byte coercion of byte-returning fn"
        (assert-submaps
         [{:level :warning
           :message "Redundant byte coercion - expression already has type byte"}]
         (lint! "(byte (byte 1))" cfg)))
      (testing "warns on char coercion of char-returning fn"
        (assert-submaps
         [{:level :warning
           :message "Redundant char coercion - expression already has type char"}]
         (lint! "(char (char 65))" cfg)))
      (testing "warns on boolean coercion of boolean-returning fn"
        ;; Both inner and outer boolean calls are redundant since true is already boolean
        (assert-submaps
         [{:level :warning
           :message "Redundant boolean coercion - expression already has type boolean"}
          {:level :warning
           :message "Redundant boolean coercion - expression already has type boolean"}]
         (lint! "(boolean (boolean true))" cfg)))
      (testing "no warning when coercion changes type"
        (is (empty? (lint! "(double 1)" cfg)))
        (is (empty? (lint! "(float 1)" cfg)))
        (is (empty? (lint! "(int 1.0)" cfg)))
        (is (empty? (lint! "(long 1.0)" cfg)))
        ;; Cross-type integer coercions should not warn
        (is (empty? (lint! "(int (long 1))" cfg)))
        (is (empty? (lint! "(long (int 1))" cfg)))
        (is (empty? (lint! "(short (int 1))" cfg)))
        (is (empty? (lint! "(int (short 1))" cfg)))
        (is (empty? (lint! "(long (short 1))" cfg)))
        (is (empty? (lint! "(short (long 1))" cfg))))
      (testing "no warning when type is not known"
        (is (empty? (lint! "(defn foo [] 1) (double (foo))" cfg))))
      (testing "no warning when linter is off"
        (is (empty? (lint! "(double (double 1))"
                           (assoc-in cfg [:linters :redundant-primitive-coercion :level] :off)))))
      (testing "respects clj-kondo/ignore"
        (is (empty? (lint! "#_:clj-kondo/ignore (double (double 1))" cfg)))))))
