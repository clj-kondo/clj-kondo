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
             :ignores (atom {})
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
    (is (= [{:filename "file.clj"
             :col 0
             :row 0
             :type :syntax
             :message "Can't parse file.clj, this is unexpected"}]
           (#'ana/->findings (Exception. "this is unexpected") "file.clj")))
    (testing "parse errors"
      (is (= [{:filename "core.clj"
               :col 0
               :row 0
               :type :syntax
               :message "Can't parse core.clj, expected failure"}]
             (#'ana/->findings (ex-info "expected failure" {:row 7
                                                            :col 9
                                                            :type :syntax})
                               "core.clj"))))))

(deftest analyze-input-test
  (let [findings (atom [])
        analyze (fn [^String source]
                  (ana/analyze-input {:config {:linters {:syntax {:level :error}
                                                         :duplicate-map-key {:level :error}}}
                                      :findings findings
                                      :ignores (atom {})
                                      :namespaces (atom {})
                                      :used-namespaces (atom {})} "test.clj" source :clj false))]
    (testing "unmatched delimiters"
      (is (= [{:type :syntax
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
               :message "Mismatched bracket: found an opening ( on line 1 and a closing }"}]
             (do
               (reset! findings [])
               (analyze "(}")
               @findings))))
    (testing "unclosed delimiter"
      (is (= [{:type :syntax
               :level :error
               :filename "test.clj"
               :row 1
               :col 1
               :message "Found an opening ( with no matching )"}
              {:type :syntax, :level :error, :filename "test.clj"
               :row 1
               :col 9
               :message "Expected a ) to match ( from line 1"}]
             (do
               (reset! findings [])
               (analyze "(defn []")
               @findings))))

    (testing "invalid tokens"
      (is (= [{:type :syntax
               :level :error
               :filename "test.clj"
               :row 1
               :col 4
               :message "Invalid number: 1..1."}]
             (do
               (reset! findings [])
               (analyze "1..1")
               @findings))))

    (testing "duplicate map keys"
      (testing "when the map keys are simple sequences"
        (is (= [{:type :duplicate-map-key
                 :level :error
                 :filename "test.clj"
                 :row 1
                 :col 14
                 :end-col 19
                 :end-row 1
                 :message "duplicate key (12)"}]
               (do
                 (reset! findings [])
                 (analyze "{[1 2] \"bar\" (1 2) 12}")
                 @findings))))
      (testing "when the map keys are more complex forms"
        (is (= [{:type :duplicate-map-key
                 :level :error
                 :filename "test.clj"
                 :row 1
                 :col 22
                 :end-col 35
                 :end-row 1
                 :message "duplicate key (let[x2]x)"}]
               (do
                 (reset! findings [])
                 (analyze "{(let [x 2] x) \"bar\" (let [x 2] x) 12}")
                 @findings))))
      (testing "when the map keys are simple tokens"
        (is (= [{:type :duplicate-map-key
                 :level :error
                 :filename "test.clj"
                 :row 1
                 :col 12
                 :end-col 15
                 :end-row 1
                 :message "duplicate key foo"}]
               (do
                 (reset! findings [])
                 (analyze "{foo \"bar\" foo 12}")
                 @findings)))))))





(comment
  (t/run-tests)
  (analyze-ns-decl
   :clj
   (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))
  )
