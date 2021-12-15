(ns clj-kondo.config-paths-test
  (:require
   [babashka.fs :as fs]
   [clj-kondo.test-utils :refer [lint! assert-submaps native?]]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest testing is]])
  (:import [java.nio.file Files]))

(deftest re-frame-test
  (assert-submaps
   '({:file "corpus/hooks/re_frame.clj", :row 6, :col 12, :level :warning, :message #"keyword should be fully qualified!"})
   (lint! (io/file "corpus" "hooks" "re_frame.clj")
          {:linters {:unresolved-symbol {:level :error}
                     :invalid-arity {:level :error}}}
          ;; we are using the config dir .clj-kondo2 which refers to .clj-kondo as a config path
          ;; which contains the hook code needed to lint the above file
          "--config-dir" (.getPath (io/file "corpus" ".clj-kondo2")))))

(deftest home-config-test
  (when-not native?
    (let [old-home (System/getProperty "user.home")
          home-dir (str (Files/createTempDirectory "clj_kondo" (into-array java.nio.file.attribute.FileAttribute [])))
          project-cfg-dir (str (Files/createTempDirectory "clj_kondo" (into-array java.nio.file.attribute.FileAttribute [])))
          prog "
(ns x {:clj-kondo/config '{:linters {:unused-binding {:level :warning}}}}
  (:require foo.foo))

(foo.foo/foo [x 1 y 2])"]
      (try
        (System/setProperty "user.home" home-dir)
        (let [cfg-file (io/file home-dir ".config" "clj-kondo" "config.edn")]
          (io/make-parents cfg-file)
          (spit cfg-file "{:lint-as {foo.foo/foo clojure.core/let}}")
          (testing "config from home dir is picked up"
            (assert-submaps
             '({:level :warning, :message "unused binding x"}
               {:level :warning, :message "unused binding y"})
             (lint! prog)))
          (testing "ignoring home dir config"
            (spit (io/file project-cfg-dir "config.edn") "{:config-paths ^:replace []}")
            (is (empty? (lint! prog "--config-dir" project-cfg-dir)))))
        (finally
          (System/setProperty "user.home" old-home))))))

(deftest auto-load-configs-test
  (let [config-file (io/file "corpus" ".clj-kondo" "config.edn")]
    (when-not native?
      (testing "auto-load-configs enabled by default, even with no config.edn"
        (is (not (fs/exists? config-file)))
        (assert-submaps
         '({:file "corpus/acme/lib/example.clj", :row 7, :col 21, :level :error, :message "Unresolved symbol: a"})
         (lint! (io/file "corpus" "acme" "lib" "example.clj")
                {:linters {:unresolved-symbol {:level :error}}}
                "--config-dir" (.getPath (io/file "corpus" ".clj-kondo")))))
      (testing "auto-load-configs disabled"
        (let [edn {:auto-load-configs false}]
          (try
            (is (not (fs/exists? config-file)))
            (spit config-file edn)
            (is (= 4
                   (count
                    (lint! (io/file "corpus" "acme" "lib" "example.clj")
                           {:linters {:unresolved-symbol {:level :error}}}
                           "--config-dir" (.getPath (io/file "corpus" ".clj-kondo"))
                           "--config" (pr-str '{:auto-load-configs false})))))
            (finally (fs/delete config-file))))))))
