(ns clj-kondo.impl.analyzer-test
  (:require
   [clj-kondo.impl.analyzer :as ana]
   [clj-kondo.impl.analyzer.namespace :refer [analyze-ns-decl]]
   [clj-kondo.impl.metadata :as meta]
   [clj-kondo.impl.utils :refer [parse-string]]
   [clojure.test :as t :refer [deftest testing is are]]))

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


(deftest ->finding-test
  (testing "unexpected exceptions"
    (is (= {:level :error
            :filename "file.clj"
            :col 0
            :row 0
            :type :syntax
            :message "can't parse file.clj, this is unexpected"}
           (#'ana/->finding (Exception. "this is unexpected") "file.clj")))
    (testing "parse errors"
      (is (= {:level :error
              :filename "core.clj"
              :col 9
              :row 7
              :type :syntax
              :message "expected failure"}
             (#'ana/->finding (Exception. "expected failure [at line 7, column 9]") "core.clj"))))))

(comment
  (t/run-tests)
  (analyze-ns-decl
   :clj
   (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))
  )
