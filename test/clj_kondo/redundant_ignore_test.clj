(ns clj-kondo.redundant-ignore-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps2 with-temp-dir]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]))

(def config {:linters {:redundant-ignore {:level :warning}
                       :unresolved-protocol-method {:level :warning}
                       :missing-protocol-method {:level :warning}
                       :unresolved-symbol {:level :error}}})

(deftest redundant-ignore-test
  (assert-submaps2
   '({:file "<stdin>", :row 1, :col 3, :level :warning, :message "Redundant ignore"})
   (lint! "#_:clj-kondo/ignore (+ 1 2 3)" config)))

(deftest redundant-ignore-unused-private-var-test
  (assert-submaps2
   []
   (lint! "#_{:clj-kondo/ignore [:unused-private-var]}
(defn- -debug [& strs]
  (.println System/err
            (with-out-str
              (apply println strs))))"
          config)))

(deftest redundant-ignore-exclude-test
  (is (empty?
       (lint! "#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var :unused-binding]} (defn foo [])"
              (assoc-in config [:linters :redundant-ignore :exclude] [:clojure-lsp/unused-public-var])))))

(deftest redundant-ignore-on-missing-and-unresolved-protocol-test
  (is (empty? (lint! "(defprotocol IFoo
  (dude [_]))

(defrecord MyFoo []
  #_:clj-kondo/ignore IFoo
  (#_:clj-kondo/ignore dudex [_]))"
                     config))))

(deftest cljc-test
  (is (empty? (lint! "#_{:clj-kondo/ignore #?(:cljs [:unresolved-symbol])}
(defn foo []
  [#?(:cljs z)]) ;; x is only used in cljs, but unused is ignored for clj, so no warning
"
                     config "--filename" "foo.cljc"))))

(deftest issue-2818-redundant-ignore-cross-file-redefined-var-test
  (with-temp-dir [tmp "clj-kondo-issue-2818"]
    (let [dev-file (io/file tmp "src" "demo" "env_dev.clj")
          prod-file (io/file tmp "src" "demo" "env_prod.clj")]
      (.mkdirs (.getParentFile dev-file))
      (spit dev-file
            (str "(ns demo.env)\n\n"
                 "#_{:clj-kondo/ignore [:redefined-var]}\n"
                 "(def defaults {:mode :dev})\n"))
      (spit prod-file
            (str "(ns demo.env)\n\n"
                 "(def defaults {:mode :prod})\n"))
      (let [findings (lint! [dev-file prod-file] config)]
        (is (empty? (filter #(= "Redundant ignore" (:message %)) findings)))))))

(deftest issue-2818-redundant-ignore-same-file-redefined-var-test
  (is (empty?
       (filter #(= "Redundant ignore" (:message %))
               (lint! "(ns demo.same-file-control)

#_{:clj-kondo/ignore [:redefined-var]}
(def defaults {:mode :a})

#_{:clj-kondo/ignore [:redefined-var]}
(def defaults {:mode :b})"
                      config)))))

(deftest issue-2818-redundant-ignore-truly-redefined-var-test
  (assert-submaps2
   '({:file "<stdin>"
      :row 3
      :col 3
      :level :warning
      :message "Redundant ignore"})
   (lint! "(ns demo.truly-redundant)

#_{:clj-kondo/ignore [:redefined-var]}
(def defaults {:mode :only-once})"
          config)))
