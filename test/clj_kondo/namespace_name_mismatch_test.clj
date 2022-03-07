(ns clj-kondo.namespace-name-mismatch-test
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]))

(deftest namespace-name-mismatch-test
  (assert-submaps
   '()
   (lint! (io/file "corpus" "namespace_name_mismatch" "correct_file.cljs")
          '{:linters {:namespace-name-mismatch {:level :error}}}))

  (assert-submaps
   '()
   (lint! (io/file "corpus" "namespace_name_mismatch" "correct_file.cljs")
          '{:linters {:namespace-name-mismatch {:level :error}}}))

  (assert-submaps
   '({:file "corpus/namespace_name_mismatch/wrong_file.clj",
      :row 1,
      :col 5,
      :level :error,
      :message "Namespace name does not match file name: namespace-name-mismatch.foo"})
   (lint! (io/file "corpus" "namespace_name_mismatch" "wrong_file.clj")
          '{:linters {:namespace-name-mismatch {:level :error}}}))

  (assert-submaps
   '({:file "corpus/namespace_name_mismatch/wrong_file.cljs",
      :row 1,
      :col 5,
      :level :error,
      :message "Namespace name does not match file name: namespace-name-mismatch.foo"})
   (lint! (io/file "corpus" "namespace_name_mismatch" "wrong_file.cljs")
          '{:linters {:namespace-name-mismatch {:level :error}}}))

  (assert-submaps
   '({:file "corpus/namespace_name_mismatch/wrong_file_but_substring.clj",
      :row 1,
      :col 5,
      :level :error,
      :message "Namespace name does not match file name: namespace-name-mismatch.wrong-file"})
   (lint! (io/file "corpus" "namespace_name_mismatch" "wrong_file_but_substring.clj")
          '{:linters {:namespace-name-mismatch {:level :error}}}))

  (assert-submaps
   '({:file "corpus/namespace_name_mismatch/wrong_folder/foo.clj",
      :row 1,
      :col 5,
      :level :error,
      :message "Namespace name does not match file name: namespace-name-mismatch.something.foo"})
   (lint! (io/file "corpus" "namespace_name_mismatch" "wrong_folder" "foo.clj")
          '{:linters {:namespace-name-mismatch {:level :error}}}))

  (assert-submaps
   '({:file "corpus/namespace_name_mismatch/file-with-dashes.clj",
      :row 1,
      :col 5,
      :level :error,
      :message "Namespace name does not match file name: namespace-name-mismatch.file-with-dashes"})
   (lint! (io/file "corpus" "namespace_name_mismatch" "file-with-dashes.clj")
          '{:linters {:namespace-name-mismatch {:level :error}}}))

  (assert-submaps
   '({:file "corpus/namespace_name_mismatch/folder-with-dashes/foo.clj",
      :row 1,
      :col 5,
      :level :error,
      :message "Namespace name does not match file name: namespace-name-mismatch.folder-with-dashes.foo"})
   (lint! (io/file "corpus" "namespace_name_mismatch" "folder-with-dashes" "foo.clj")
          '{:linters {:namespace-name-mismatch {:level :error}}}))

  (assert-submaps
   '()
   (lint! (io/file "corpus" "namespace_name_mismatch" "ignored.clj")
          '{:linters {:namespace-name-mismatch {:level :error}}})))

(deftest windows-test
  (testing "absolute path on Windows"
    (is (empty?
         (:findings (clj-kondo/run! {:lint [(-> (io/file "corpus" "namespace_name_mismatch" "correct_file.cljs")
                                                .toURI
                                                .toURL
                                                .getPath)]
                                     :config {:linters {:namespace-name-mismatch {:level :error}}}}))))))
