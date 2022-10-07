(ns clj-kondo.unknown-require-option-test
  (:require [clj-kondo.test-utils :refer [lint!]]
            [clojure.test :refer [deftest is]]))

(deftest unknown-require-option-test
  (is (= '({:file "<stdin>",
            :row 1,
            :col 24,
            :level :warning,
            :message "Unknown require option: :s"})
         (lint! "(ns foo (:require [bar :s b]))"
                {:linters {:unknown-require-option {:level :warning}}}))))

(deftest ignorable-test
  (is (= '()
         (lint! "(ns foo (:require #_:clj-kondo/ignore [bar :s b]))"
                {:linters {:unknown-require-option {:level :warning}}}))))

(deftest include-macros-cljs-test
  (is (= '()
         (lint! "(ns foo (:require [bar :include-macros true]))"
                {:linters {:unknown-require-option {:level :warning}}}
                "--lang" "cljs")))
  (is (= '({:file "<stdin>",
            :row 1,
            :col 24,
            :level :error,
            :message "require form is invalid: :invalid-macros only accepts true"})
         (lint! "(ns foo (:require [bar :include-macros :s]))"
                {:linters {:unknown-require-option {:level :warning}}}
                "--lang" "cljs"))))

(deftest include-macro-clojure-test
  (is (= '({:file "<stdin>",
            :row 1,
            :col 24,
            :level :warning,
            :message "Unknown require option: :include-macros"})
         (lint! "(ns foo (:require [bar :include-macros true]))"
                {:linters {:unknown-require-option {:level :warning}}}
                "--lang" "clj"))))

(deftest include-macros-cljc-test
  (is (= '()
         (lint! "(ns foo (:require [bar :include-macros true]))"
                {:linters {:unknown-require-option {:level :warning}}}
                "--lang" "cljc")))
  (is (= '({:file "<stdin>",
            :row 1,
            :col 24,
            :level :error,
            :message "require form is invalid: :invalid-macros only accepts true"})
         (lint! "(ns foo (:require [bar :include-macros :s]))"
                {:linters {:unknown-require-option {:level :warning}}}
                "--lang" "cljc"))))
