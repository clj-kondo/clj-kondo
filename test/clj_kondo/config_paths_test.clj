(ns clj-kondo.config-paths-test
  (:require
   [babashka.fs :as fs]
   [clj-kondo.test-utils :refer [lint! assert-submaps2 native?]]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest testing is]]))

(deftest re-frame-test
  (assert-submaps2
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
          home-dir (str (fs/create-temp-dir {:prefix "clj_kondo"}))
          project-cfg-dir (str (fs/create-temp-dir {:prefix "clj_kondo"}))
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
            (assert-submaps2
             '({:level :warning, :message "unused binding x"}
               {:level :warning, :message "unused binding y"})
             (lint! prog)))
          (testing "ignoring home dir config"
            (spit (io/file project-cfg-dir "config.edn") "{:config-paths ^:replace []}")
            (is (empty? (lint! prog "--config-dir" project-cfg-dir)))))
        (finally
          (System/setProperty "user.home" old-home))))))

(deftest symlinked-config-test
  (when-not native?
    (let [tmp-dir (str (fs/create-temp-dir {:prefix "clj_kondo"}))
          cfg-dir (io/file tmp-dir ".clj-kondo")
          ;; Create the actual config outside .clj-kondo
          real-lib-dir (io/file tmp-dir "real-configs" "acme" "lib")]
      (fs/create-dirs real-lib-dir)
      (fs/create-dirs cfg-dir)
      (spit (io/file real-lib-dir "config.edn")
            "{:linters {:unresolved-symbol {:exclude [(acme.lib.example/awful-macro [x y z])]}}}")
      ;; Symlink acme -> real config dir
      (fs/create-sym-link (fs/path cfg-dir "acme")
                          (fs/path tmp-dir "real-configs" "acme"))
      (testing "configs from symlinked dirs are loaded"
        ;; With the symlinked config, x/y/z are excluded for awful-macro, leaving only 'a' on row 7
        (let [results (lint! (io/file "corpus" "acme" "lib" "example.clj")
                             {:linters {:unresolved-symbol {:level :error}}}
                             "--config-dir" (.getPath cfg-dir))]
          (assert-submaps2
           '({:row 7, :level :error, :message "Unresolved symbol: a"})
           (filter #(= 7 (:row %)) results)))
        ;; Without the symlink, all symbols on row 7 are unresolved
        (fs/delete (io/file cfg-dir "acme"))
        (let [results (lint! (io/file "corpus" "acme" "lib" "example.clj")
                             {:linters {:unresolved-symbol {:level :error}}}
                             "--config-dir" (.getPath cfg-dir))]
          (is (< 1 (count (filter #(= 7 (:row %)) results)))))))))

(deftest auto-load-configs-test
  (let [config-file (io/file "corpus" ".clj-kondo" "config.edn")]
    (when-not native?
      (testing "auto-load-configs enabled by default, even with no config.edn"
        (is (not (fs/exists? config-file)))
        (assert-submaps2
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
