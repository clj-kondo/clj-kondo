(ns clj-kondo.impl.analyzer-test
  (:require
   [clj-kondo.impl.analyzer :as ana]
   [clj-kondo.impl.analyzer.namespace :refer [analyze-ns-decl]]
   [clj-kondo.impl.metadata :as meta]
   [clj-kondo.impl.utils :refer [parse-string]]
   [clj-kondo.test-utils :refer [assert-submap]]
   [clojure.test :as t :refer [deftest testing is are]]))

(defn- lifted-meta
  ([s] (lifted-meta s nil))
  ([s cfg]
   (meta (meta/lift-meta-content2 (merge {:lang :clj
                                          :namespaces (atom {})}
                                         cfg)
                                  (parse-string s)))))

(deftest lift-meta-test
  (is (= {:private true
          :row 1
          :col 11
          :end-row 1
          :end-col 14
          :user-meta [{:private true}]}
         (lifted-meta "^:private [x]" {:analyze-meta? true})))

  (is (= {:private true
          :row 1
          :col 11
          :end-row 1
          :end-col 14}
         (lifted-meta "^:private [x]")))

  ;; no-clobber test
  (is (= {:row 1
          :col 149
          :end-row 1
          :end-col 152
          :uncommon :thing
          :user-meta [{:row :r :col :c :end-row :er :end-col :ec
                       :name-row :nr :name-col :nc :name-end-row :ner :name-end-col :nec
                       :user-meta :foo-bar
                       :uncommon :thing}]}
         (lifted-meta (str "^{:row :r :col :c :end-row :er :end-col :ec"
                           " :name-row :nr :name-col :nc :name-end-row :ner :name-end-col :nec"
                           " :user-meta :foo-bar"
                           " :uncommon :thing} [x]")
                      {:analyze-meta? true})))

  (is (= {:private true
          :row 1
          :col 13
          :end-row 1
          :end-col 16
          :user-meta [{:private true}]}
         (lifted-meta "#^ :private [x]" {:analyze-meta? true})))

  (is (= {:tag "[B"
          :row 1
          :col 7
          :end-row 1
          :end-col 11
          :user-meta [{:tag "[B"}]}
         (lifted-meta "^\"[B\" body" {:analyze-meta? true}))))

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
  (let [analyze (fn [^String source]
                  (let [ctx {:config {:linters {:syntax {:level :error}} :output {:format :edn} :analysis true}
                             :files (atom 0)
                             :filename "-"
                             :base-lang :clj
                             :lang :clj
                             :used-namespaces (atom {})
                             :findings (atom [])
                             :namespaces (atom {})
                             :ignores (atom {})
                             :bindings {}}]
                    (ana/analyze-input ctx "test.clj" "file:test.clj" source :clj false)
                    ctx))]
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
             @(:findings (analyze "(}")))))
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
             @(:findings (analyze "(defn []")))))

    (testing "invalid tokens"
      (is (= [{:type :syntax
               :level :error
               :filename "test.clj"
               :row 1
               :col 1
               :message "Invalid number: 1..1."}]
             @(:findings (analyze "1..1")))))
    (testing "multiple :as aliases and alias fn"
      (assert-submap
        '{:type :ns
          :name foo
          :qualify-ns {baz bar
                       bar bar
                       quux qux
                       qux qux
                       snafu clojure.set
                       clojure.core clojure.core}
          :aliases {baz bar quux qux snafu clojure.set}}
        (-> (analyze "(ns foo (:require [bar :as baz] [qux :as quux])) (alias 'snafu 'clojure.set)")
            :namespaces
            deref
            (get-in [:clj :clj 'foo]))))))

(comment
  (t/run-tests)
  (analyze-ns-decl
   :clj
   (parse-string "(ns foo (:require [bar :as baz :refer [quux]]))"))
  )
