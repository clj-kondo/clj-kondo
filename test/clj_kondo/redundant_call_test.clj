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
