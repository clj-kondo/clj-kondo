(ns clj-kondo.config-in-call-test
  (:require
   [clj-kondo.test-utils :refer [lint!]]
   [clojure.test :as t :refer [deftest is testing]]))

(deftest disable-unresolved-namespace-in-comment
  (is (seq (lint! "(comment (clojure.string/join [1 2 3]))"
                  '{:linters {:unresolved-symbol {:level :error}
                              :unresolved-namespace {:level :error}}})))
  (is (empty? (lint! "(comment (clojure.string/join [1 2 3]))"
                     '{:linters {:unresolved-symbol {:level :error}
                                 :unresolved-namespace {:level :error}}
                       ;; undocumented
                       :config-in-call {clojure.core/comment
                                        {:linters {:unresolved-namespace {:level :off}}}}})))
  (is (empty? (lint! "(comment (clojure.string/join [1 2 3]))"
                     '{:linters {:unresolved-symbol {:level :error}
                                 :unresolved-namespace {:level :error}}
                       ;; documented
                       :config-in-comment {:linters {:unresolved-namespace {:level :off}}}}))))
