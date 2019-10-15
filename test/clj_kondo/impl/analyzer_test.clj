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

(deftest ->findings-test
  (testing "unexpected exceptions"
    (is (= [{:level :error
             :filename "file.clj"
             :col 0
             :row 0
             :type :syntax
             :message "can't parse file.clj, this is unexpected"}]
           (#'ana/->findings (Exception. "this is unexpected") "file.clj")))
    (testing "parse errors"
      (is (= [{:level :error
               :filename "core.clj"
               :col 0
               :row 0
               :type :syntax
               :message "can't parse core.clj, expected failure"}]
             (#'ana/->findings (ex-info "expected failure" {:row 7
                                                            :col 9
                                                            :type :syntax})
                               "core.clj"))))))

(deftest analyze-input-test
  (let [analyze (fn [^String source]
                  (ana/analyze-input {:config nil} "test.clj" source :clj false))]
    (testing "unmatched delimiters"
      (is (= {:findings [{:type :syntax
                          :level :error
                          :filename "test.clj"
                          :row 1
                          :col 1
                          :message "Mismatched bracket: found an opening ( and a closing } on line 1"}
                         {:type :syntax
                          :level :error
                          :filename "test.clj"
                          :row 1
                          :col 2
                          :message "Mismatched bracket: found an opening ( on line 1 and a closing }"}]}
             (analyze "(}"))))
    (testing "invalid tokens"
      (is (= {:findings [{:type :syntax
                          :level :error
                          :filename "test.clj"
                          :row 1
                          :col 4
                          :message "Invalid number: 1..1."}]}
           (analyze "1..1"))))))


(defn- compile-errors [^String source]
  (let  [result (ana/analyze-input {:filename "example.clj"
                                      :namespaces (atom {})
                                      :findings (atom [])
                                      :base-lang :clj
                                      :lang :clj
                                      :bindings {}} "example.clj" source :clj false)]
    (for [{:keys [row col message]} (:findings result)]
      [row col message])))

(deftest parser-reader-analyzer-errors
  ;; Some tests of the parser, reader, and analyzer when given invalid Clojure
  ;; code. Make sure that the results have valid line numbers and messages.
  (are [source messages] (= messages (compile-errors source))
    "(defn []" [[1 1 "Found an opening ( with no matching )"]
                [1 9 "Expected a ) to match ( from line 1"]]
    "(defn oops ())" [[0 0 "can't parse example.clj, No implementation of method: :tag of protocol: #'clj-kondo.impl.rewrite-clj.node.protocols/Node found for class: nil"]]))

(comment
  (t/run-tests)
  (analyze-ns-decl
   :clj
   (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))
  )
