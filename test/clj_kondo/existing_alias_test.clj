(ns clj-kondo.existing-alias-test
  (:require [clj-kondo.test-utils :refer [lint!]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]))

(deftest single-alias-test
  (let [file (io/file "corpus" "existing_aliases" "single_alias.clj")]
    (is (= [{:file (str file),
             :row 4,
             :col 1,
             :level :warning,
             :message "An alias is defined for baz.qux: q"}]
           (lint! file {:linters {:existing-alias {:level :warning}}})))))

(deftest multiple-aliases-test
  (let [path (io/file "corpus" "existing_aliases" "multiple_aliases.clj")]
    (is (= [{:file (str path),
             :row 5,
             :col 1,
             :level :warning,
             :message "Multiple aliases are defined for baz.qux: q, qq"}]
           (lint! path
                  {:linters {:duplicate-require {:level :off}
                             :existing-alias {:level :warning}}})))))

(deftest excluded-alias-test
  (let [path (io/file "corpus" "existing_aliases" "excluded_alias.clj")]
    (is (empty?
          (lint! path
                 {:linters {:existing-alias {:level :warning
                                              :exclude ['clojure.string]}}})))))

(deftest dont-check-analyze-call-hook
  (is (empty? (lint!
                (str "(ns analyze-call-hook (:require [clojure.core :as cc]))\n"
                     "(defn new-> [x f] (f x))\n"
                     "(new-> 1 inc)")
                {:linters ^:replace {:existing-alias {:level :warning}}
                 :hooks
                 {:__dangerously-allow-string-hooks__ true
                  :analyze-call
                  {'analyze-call-hook/new->
                   (str "(require '[clj-kondo.hooks-api :as api])\n"
                        "(defn new-> [{:keys [node]}]\n"
                        "  (let [children (rest (:children node))\n"
                        "        node (list* (api/token-node 'clojure.core/->) children)]\n"
                        "    {:node (api/list-node node)}))")}}}))))

(deftest dont-check-macroexpansion
  (is (empty? (lint!
                (str "(ns macroexpansion (:require [clojure.core :as cc]))\n"
                     "(defn new-> [x f] (f x))\n"
                     "(new-> 1 inc)")
                {:linters ^:replace {:existing-alias {:level :warning}}
                 :hooks
                 {:__dangerously-allow-string-hooks__ true
                  :macroexpand
                  {'macroexpansion/new->
                   (str "(require '[clj-kondo.hooks-api :as api])\n"
                        "(defmacro new-> [x f]\n"
                        "  (list 'clojure.core/-> x f))")}}}))))
