(ns clj-kondo.impl.core-test
  (:require
   [clj-kondo.impl.core :as impl-core]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]))

(def ^:private test-cases
  {["corpus/config-dir/foo.clj"]                         "corpus/config-dir/.clj-kondo"
   ["corpus/config-dir/subdir/bar.cljc"]                 "corpus/config-dir/.clj-kondo"
   ["corpus/config-dir/nested/foo.cljs"]                 "corpus/config-dir/nested/.clj-kondo"
   ["corpus/config-dir/nested/subdir/bar.clj"]           "corpus/config-dir/nested/.clj-kondo"
   []                                                    ".clj-kondo"
   ["corpus/config-dir/a.clj" "corpus/config-dir/b.clj"] ".clj-kondo"
   ["corpus/config-dir/c.jar"]                           ".clj-kondo"
   ["corpus/config-dir/subdir"]                          ".clj-kondo"
   ["/does-not-exist/foo.cljs"]                          ".clj-kondo"
   ["/bar.cljc"]                                         ".clj-kondo"})

(deftest closest-config-dir-test
  (doseq [[lint expected-dir] test-cases
          :let [string-result (impl-core/config-dir lint)
                lint-files (map io/file lint)
                file-result (impl-core/config-dir lint-files)]]
    (is (= (.getCanonicalPath (io/file expected-dir))
           (.getCanonicalPath string-result))
        (str "expected: (config-dir " (pr-str lint) ") ;=> " expected-dir
             "\ngot: " string-result))
    (is (= (.getCanonicalPath (io/file expected-dir))
           (.getCanonicalPath file-result))
        (str "expected: (config-dir " (pr-str lint-files) ") ;=> " expected-dir
             "\ngot: " file-result))))
