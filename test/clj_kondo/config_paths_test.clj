(ns clj-kondo.config-paths-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest testing is]]))

(deftest re-frame-test
  (assert-submaps
   '({:file "corpus/hooks/re_frame.clj", :row 6, :col 12, :level :warning, :message #"keyword should be fully qualified!"})
   (lint! (io/file "corpus" "hooks" "re_frame.clj")
          {:linters {:unresolved-symbol {:level :error}
                     :invalid-arity {:level :error}}}
          ;; we are using the config dir .clj-kondo2 which refers to .clj-kondo as a config path
          ;; which contains the hook code needed to lint the above file
          "--config-dir" (.getPath (io/file "corpus" ".clj-kondo2")))))
