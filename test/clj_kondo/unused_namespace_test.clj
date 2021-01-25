(ns clj-kondo.unused-namespace-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [lint! assert-submaps assert-submap]]
   [clojure.java.io :as io]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest unused-namespace-test
  (testing "return ns and refer info"
    (assert-submaps
      '({:type :unused-namespace,
         :message "namespace clojure.core.async is required but never used",
         :level :warning,
         :row 1,
         :end-row 1,
         :end-col 38,
         :col 20,
         :filename "<stdin>",
         :ns clojure.core.async}
        {:type :unused-referred-var,
         :message "#'clojure.core.async/go-loop is referred but never used",
         :level :warning,
         :row 1,
         :end-row 1,
         :end-col 54,
         :col 47,
         :filename "<stdin>",
         :ns clojure.core.async
         :refer go-loop})
      (-> (with-in-str
            "(ns foo (:require [clojure.core.async :refer [go-loop]]))"
            (clj-kondo/run! {:lint ["-"]}))
          :findings)))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 20,
      :level :warning,
      :message "namespace clojure.core.async is required but never used"})
   (lint! "(ns foo (:require [clojure.core.async :refer [go-loop]]))"
          "--config" "^:replace {:linters {:unused-namespace {:level :warning}}}"))
  (assert-submaps
   '({:file "<stdin>",
      :row 2,
      :col 30,
      :level :warning,
      :message "namespace rewrite-clj.node is required but never used"}
     {:file "<stdin>",
      :row 2,
      :col 46,
      :level :warning,
      :message "namespace rewrite-clj.reader is required but never used"})
   (lint! "(ns rewrite-clj.parser
     (:require [rewrite-clj [node :as node] [reader :as reader]]))"))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 32,
      :level :warning,
      :message "namespace baz is required but never used"})
   (lint! "(ns foo (:require [bar :as b] [baz :as baz])) #::{:a #::bar{:a 1}}"))
  (assert-submap
   '({:file "<stdin>",
      :row 1,
      :col 18,
      :level :warning,
      :message "namespace clojure.string is required but never used"})
   (lint! "(ns f (:require [clojure.string :as s])) :s/foo"))
  (assert-submap
   '({:file "<stdin>",
      :row 1,
      :col 20,
      :level :warning,
      :message "namespace bar is required but never used"})
   (lint! "(ns foo (:require [bar :as b])) #:b{:a 1}"))
  (testing "simple libspecs (without as or refer) are not reported"
    (is (empty? (lint! "(ns foo (:require [foo.specs]))")))
    (is (empty? (lint! "(ns foo (:require foo.specs))")))
    (is (seq (lint! "(ns foo (:require [foo.specs]))"
                    '{:linters {:unused-namespace {:simple-libspec true}}}))))
  (assert-submaps
   '({:file "<stdin>",
      :row 1,
      :col 12,
      :level :warning,
      :message "namespace clojure.set is required but never used"})
   (lint! "(require '[clojure.set :refer [join]])"
          "--config" "^:replace {:linters {:unused-namespace {:level :warning}}}"))
  (is (empty?
       (lint! "(ns foo (:require [clojure.core.async :refer [go-loop]]))
         ,(ns bar)
         ,(in-ns 'foo)
         ,(go-loop [])")))
  (is (empty? (lint! "(ns foo (:require [clojure.set :as set]))
    (reduce! set/difference #{} [])")))
  (is (empty? (lint! "(ns foo (:require [clojure.set :as set :refer [difference]]))
    (reduce! difference #{} [])")))
  (is (empty? (lint! "(ns foo (:require [clojure.set :as set]))
    (defmacro foo [] `(set/difference #{} #{}))")))
  (is (empty? (lint! "(ns foo (:require [clojure.core.async :refer [go-loop]])) (go-loop [x 1] (recur 1))")))
  (is (empty? (lint! "(ns foo (:require bar)) ::bar/bar")))
  (is (empty? (lint! "(ns foo (:require [bar :as b])) ::b/bar")))
  (is (empty? (lint! "(ns foo (:require [bar :as b])) #::b{:a 1}")))
  (is (empty? (lint! "(ns foo (:require [bar :as b] baz)) #::baz{:a #::bar{:a 1}}")))
  (is (empty? (lint! "(ns foo (:require goog.math.Long)) (instance? goog.math.Long 1)")))
  (is (empty? (lint! "(ns foo (:require [schema.core :as s] [bar :as bar])) (s/defn foo :- bar/Schema [])")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str])) {str/join true}")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str])) {true str/join}")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str])) [str/join]")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str]))
                       (defn my-id [{:keys [:id] :or {id (str/lower-case \"HI\")}}] id)")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str]))
                       (fn [{:keys [:id] :or {id (str/lower-case \"HI\")}}] id)")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str]))
                       (let [{:keys [:id] :or {id (str/lower-case \"HI\")}} {:id \"hello\"}] id)")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str]))
                       (if-let [{:keys [:id] :or {id (str/lower-case \"HI\")}} {:id \"hello\"}] id :bar)")))
  (is (empty? (lint! "(ns foo (:require [clojure.string :as str]))
                       (loop [{:keys [:id] :or {id (str/lower-case \"HI\")}} {:id \"hello\"}])")))
  (is (empty? (lint! (io/file "corpus" "shadow_cljs" "default.cljs"))))
  (is (empty? (lint! "(ns foo (:require [bar])) (:id bar/x)")))
  (is (empty? (lint! (io/file "corpus" "no_unused_namespace.clj"))))
  (is (empty? (lint! "(ns foo (:require [bar :as b])) (let [{::b/keys [:baz]} nil] baz)")))
  (is (empty? (lint! "(require '[clojure.set :refer [join]]) join")))
  (is (empty? (lint! "(ns foo (:require [bar :as b])) (let [{:keys [::b/x]} {}] x)")))
  (is (empty? (lint! "(ns ^{:clj-kondo/config
                            '{:linters {:unused-namespace {:exclude [bar]}}}}
                          foo
                        (:require [bar :as b]))")))
  (is (empty? (lint! (io/file "corpus" "cljs_ns_as_as_object.cljs"))))
  (is (empty? (lint! "(ns c (:require [a :as b] b)) b/x")))
  (testing "disable linter via ns config"
    (is (empty? (lint! "
(ns ^{:clj-kondo/config '{:linters {:unused-namespace {:level :off}}}}
  foo
  (:require [bar :as b]))"))))
  (is (empty? (lint! "
(ns kondo
  (:require [df :as df]
            [fulcro :as fulcro]
            [ui.gift-list :as ui.gift-list]))

(fulcro/defsc Home1 [_this _]
  {:will-enter (fn [app _]
                 (df/load! app [:component/id :created-gift-lists]
                           ui.gift-list/CreatedGiftLists))}
  :foo)"
                     '{:linters {:unresolved-symbol {:level :error}}
                       :lint-as {fulcro/defsc clojure.core/defn}}))))
