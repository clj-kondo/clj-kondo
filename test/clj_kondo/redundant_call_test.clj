(ns clj-kondo.redundant-call-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :refer [deftest is testing]]))

(def config
  ^:replace {:linters {:redundant-call {:level :warning}}})

(deftest redundant-call-test
  (doseq [sym `[-> ->> cond-> cond->> some-> some->> partial comp merge]]
    (is (empty? (lint! (format "(%s 1 identity)" sym) config)))
    (assert-submaps
     [{:level :warning :message (format "Single arg use of %s always returns the arg itself" sym)}]
     (lint! (format "(%s 1)" sym) config))
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
