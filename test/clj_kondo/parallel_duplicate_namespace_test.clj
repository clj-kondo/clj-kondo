(ns clj-kondo.parallel-duplicate-namespace-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [with-temp-dir]]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]))

(deftest issue-2842-test
  (testing "vars remain resolvable when duplicate namespaces are linted in parallel"
    (with-temp-dir [dir "clj-kondo-2842"]
      (let [a (io/file dir "a.clj")
            b (io/file dir "b.clj")]
        (spit a "(ns duplicate.core)\n(defn helper [] true)\n(defmacro pause [] nil)\n(pause)\n(helper)\n")
        (spit b "(ns duplicate.core)\n(def other true)\n")
        (let [config {:hooks
                      {:analyze-call
                       {'duplicate.core/pause
                        (fn [{:keys [node]}]
                          (Thread/sleep 25)
                          {:node node})}}}
              findings (:findings
                        (clj-kondo/run! {:lint     [(.getPath a) (.getPath b)]
                                        :cache    false
                                        :config   config
                                        :parallel true}))]
          (is (empty? (filter #(= :unresolved-var (:type %)) findings))))))))
