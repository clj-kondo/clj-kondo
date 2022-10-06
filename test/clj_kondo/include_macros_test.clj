(ns clj-kondo.include-macros-test
  (:require [clj-kondo.test-utils :refer [lint! assert-submaps]]
            [clojure.test :refer [deftest is]]))

(deftest include-macros-test
  (is (= '()
         (lint! "(ns foo (:require [bar :include-macros true]))"
                {:linters {:unknown-require-option {:level :warning}
                           :include-macros {:level :warning}}}
                "--lang" "cljs")))
  (is (= '({:file "<stdin>",
            :row 1,
            :col 24,
            :level :warning,
            :message ":invalid-macros only accepts true: :s"})
         (lint! "(ns foo (:require [bar :include-macros :s]))"
                {:linters {:unknown-require-option {:level :warning}
                           :include-macros {:level :warning}}}
                "--lang" "cljs"))))

(deftest include-macro-clojure-test
  (is (= '({:file "<stdin>",
            :row 1,
            :col 24,
            :level :warning,
            :message "Unknown require option: :include-macros"})
         (lint! "(ns foo (:require [bar :include-macros true]))"
                {:linters {:unknown-require-option {:level :warning}
                           :include-macros {:level :warning}}}
                "--lang" "clj")))
  (is (= '({:file "<stdin>",
              :row 1,
              :col 24,
              :level :warning,
              :message ":invalid-macros only accepts true: :s"})
           (lint! "(ns foo (:require [bar :include-macros :s]))"
                  {:linters {:unknown-require-option {:level :warning}
                             :include-macros {:level :warning
                                              :allow-clojure true}}}
                  "--lang" "clj")))
  (is (= '()
         (lint! "(ns foo (:require [bar :include-macros true]))"
                {:linters {:unknown-require-option {:level :warning}
                           :include-macros {:level :warning
                                            :allow-clojure true}}}
                "--lang" "clj"))))

(deftest include-macros-cljc-test
  (is (= '({:file "<stdin>",
            :row 1,
            :col 24,
            :level :warning,
            :message "Unknown require option: :include-macros"})
         (lint! "(ns foo (:require [bar :include-macros true]))"
                {:linters {:unknown-require-option {:level :warning}
                           :include-macros {:level :warning}}}
                "--lang" "cljc")))
  (assert-submaps
    '({:file "<stdin>",
       :row 1,
       :col 24,
       :level :warning,
       :message "Unknown require option: :include-macros"}
      {:file "<stdin>",
       :row 1,
       :col 24,
       :level :warning,
       :message ":invalid-macros only accepts true: :s"})
    (lint! "(ns foo (:require [bar :include-macros :s]))"
           {:linters {:unknown-require-option {:level :warning}
                      :include-macros {:level :warning}}}
           "--lang" "cljc"))
  (is (= '()
         (lint! "(ns foo (:require [bar :include-macros true]))"
                {:linters {:unknown-require-option {:level :warning}
                           :include-macros {:level :warning
                                            :allow-clojure true}}}
                "--lang" "cljc"))))
