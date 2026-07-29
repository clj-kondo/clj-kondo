(ns clj-kondo.redefined-spec-test
  (:require
   [babashka.fs :as fs]
   [clj-kondo.test-utils :refer [lint! assert-submaps2]]
   [clojure.test :as t :refer [deftest is testing]]))

(def spec-require "[clojure.spec.alpha :as s]")

(deftest single-namespace-test
  (testing "s/def registering the same keyword twice"
    (assert-submaps2
     '({:row 3 :col 8 :level :warning
        :message #"redefined spec :foo/x"})
     (lint! (str "(ns foo (:require " spec-require "))\n"
                 "(s/def ::x string?)\n"
                 "(s/def ::x int?)"))))
  (testing "no warning for a single registration"
    (is (empty? (lint! (str "(ns foo (:require " spec-require "))\n"
                            "(s/def ::x string?)")))))
  (testing "s/fdef registering the same symbol twice"
    (assert-submaps2
     '({:row 4 :col 9 :level :warning
        :message #"redefined spec foo/f"})
     (lint! (str "(ns foo (:require " spec-require "))\n"
                 "(defn f [x] x)\n"
                 "(s/fdef f :args (s/cat :x int?))\n"
                 "(s/fdef f :args (s/cat :y int?))"))))
  (testing "a keyword s/def and a symbol s/fdef of the same name do not clash:
            they occupy spec's two disjoint key spaces"
    (is (empty? (lint! (str "(ns foo (:require " spec-require "))\n"
                            "(defn f [x] x)\n"
                            "(s/fdef f :args (s/cat :x int?))\n"
                            "(s/def ::f int?)"))))))

(deftest edge-cases-test
  (testing "auto-resolved keywords in different namespaces are distinct"
    (is (empty? (lint! (str "(ns foo (:require " spec-require "))\n"
                            "(s/def ::x string?)\n"
                            "(ns bar (:require " spec-require "))\n"
                            "(s/def ::x string?)")))))
  (testing "aliased and fully-qualified keyword resolving to the same spec clash"
    (assert-submaps2
     '({:row 4 :col 8 :level :warning
        :message #"redefined spec :shared/x"})
     (lint! (str "(ns foo (:require " spec-require " [shared :as sh]))\n"
                 "(s/def ::sh/x string?)\n"
                 "(ns bar (:require " spec-require "))\n"
                 "(s/def :shared/x string?)")))))

(deftest whole-project-test
  (testing "duplicate specs are detected across files in a single run"
    (fs/with-temp-dir [tmp {}]
      (spit (fs/file tmp "a.clj")
            (str "(ns a (:require " spec-require " [shared :as sh]))\n"
                 "(s/def ::sh/x string?)"))
      (spit (fs/file tmp "b.clj")
            (str "(ns b (:require " spec-require "))\n"
                 "(s/def :shared/x int?)"))
      (spit (fs/file tmp "shared.clj") "(ns shared)")
      (assert-submaps2
       '({:file #"b.clj" :row 2 :col 8 :level :warning
          :message #"redefined spec :shared/x, first defined at"})
       (filter #(re-find #"redefined spec" (:message %))
               (lint! (fs/file tmp)))))))

(deftest cljc-test
  (testing "a single cljc registration does not warn"
    (is (empty? (filter #(re-find #"redefined spec" (:message %))
                        (lint! (str "(ns foo (:require " spec-require "))\n"
                                    "(s/def ::x string?)")
                               "--lang" "cljc")))))
  (testing "a duplicate cljc registration warns exactly once"
    (let [findings (filter #(re-find #"redefined spec" (:message %))
                           (lint! (str "(ns foo (:require " spec-require "))\n"
                                       "(s/def ::x string?)\n"
                                       "(s/def ::x int?)")
                                  "--lang" "cljc"))]
      (is (= 1 (count findings)))))
  (testing "same spec name in .clj and .cljs do not clash (disjoint runtimes)"
    (fs/with-temp-dir [tmp {}]
      (spit (fs/file tmp "p.clj")
            (str "(ns p (:require " spec-require "))\n(s/def ::x string?)"))
      (spit (fs/file tmp "p.cljs")
            (str "(ns p (:require " spec-require "))\n(s/def ::x string?)"))
      (is (empty? (filter #(re-find #"redefined spec" (:message %))
                          (lint! (fs/file tmp))))))))

(defn- redefined-spec-findings [findings]
  (filter #(re-find #"redefined spec" (:message %)) findings))

(deftest cross-run-cache-test
  (testing "a redefinition is detected across runs via the global spec index,
            even when the files don't require one another (both resolve to the
            same spec via an alias)"
    (fs/with-temp-dir [tmp {}]
      (let [cache (str (fs/file tmp ".cache"))]
        (spit (fs/file tmp "specs.clj") "(ns specs)")
        (spit (fs/file tmp "a.clj")
              (str "(ns a (:require " spec-require " [specs :as sp]))\n"
                   "(s/def ::sp/bar string?)"))
        (spit (fs/file tmp "b.clj")
              (str "(ns b (:require " spec-require " [specs :as sp]))\n"
                   "(s/def ::sp/bar int?)"))
        (lint! (fs/file tmp "a.clj") "--cache" cache)
        (assert-submaps2
         '({:file #"b.clj" :row 2 :col 8 :level :warning
            :message #"redefined spec :specs/bar, first defined at"})
         (redefined-spec-findings
          (lint! (fs/file tmp "b.clj") "--cache" cache))))))
  (testing "re-linting the same file does not report it against its own cached
            entry"
    (fs/with-temp-dir [tmp {}]
      (let [cache (str (fs/file tmp ".cache"))]
        (spit (fs/file tmp "a.clj")
              (str "(ns a (:require " spec-require "))\n"
                   "(s/def ::foo string?)"))
        (lint! (fs/file tmp "a.clj") "--cache" cache)
        (is (empty? (redefined-spec-findings
                     (lint! (fs/file tmp "a.clj") "--cache" cache))))))))

(deftest cross-run-staleness-test
  (testing "removing an s/def and re-linting clears it from the index"
    (fs/with-temp-dir [tmp {}]
      (let [cache (str (fs/file tmp ".cache"))]
        (spit (fs/file tmp "specs.clj") "(ns specs)")
        (spit (fs/file tmp "a.clj")
              (str "(ns a (:require " spec-require " [specs :as sp]))\n"
                   "(s/def ::sp/bar string?)"))
        (spit (fs/file tmp "b.clj")
              (str "(ns b (:require " spec-require " [specs :as sp]))\n"
                   "(s/def ::sp/bar int?)"))
        (lint! (fs/file tmp "a.clj") "--cache" cache)
        ;; b now redefines specs/bar
        (is (seq (redefined-spec-findings
                  (lint! (fs/file tmp "b.clj") "--cache" cache))))
        ;; remove the registration from a and re-lint it
        (spit (fs/file tmp "a.clj")
              (str "(ns a (:require " spec-require " [specs :as sp]))"))
        (lint! (fs/file tmp "a.clj") "--cache" cache)
        ;; b is no longer a redefinition
        (is (empty? (redefined-spec-findings
                     (lint! (fs/file tmp "b.clj") "--cache" cache))))))))

(deftest config-test
  (testing "the linter can be disabled"
    (is (empty? (lint! (str "(ns foo (:require " spec-require "))\n"
                            "(s/def ::x string?)\n"
                            "(s/def ::x int?)")
                       {:linters {:redefined-spec {:level :off}}}))))
  (testing "individual registrations can be ignored"
    (is (empty? (lint! (str "(ns foo (:require " spec-require "))\n"
                            "(s/def ::x string?)\n"
                            "#_{:clj-kondo/ignore [:redefined-spec]}\n"
                            "(s/def ::x int?)"))))))
