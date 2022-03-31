(ns clj-kondo.non-arg-vec-return-type-hint-test
  (:require
   [clj-kondo.test-utils :refer [lint! assert-submaps]]
   [clojure.test :as t :refer [deftest is testing]]))

(def example-without-override
  "(ns test
     {:clj-kondo/config
       '{:lint-as {nedap.speced.def/defn clojure.core/defn}}}
     (:require [nedap.speced.def :as sd]))
   (defn ^String foo [])
   (defn ^:static ^String bar [])
   (sd/defn ^String baz [])")

(def example-with-override
  "(ns test
     {:clj-kondo/config
       '{:lint-as {nedap.speced.def/defn clojure.core/defn}
         :config-in-call {nedap.speced.def/defn {:linters {:misplaced-defn-return-type-hint {:level :off}}}}}}
     (:require [nedap.speced.def :as sd]))
   (defn ^String foo [])
   (sd/defn ^String bar [])")

(deftest misplaced-defn-type-hint
  (assert-submaps
   [{:file "<stdin>", :row 5, :col 11, :level :warning, :message "Prefer placing return type hint on arg vector: String"}
    {:file "<stdin>", :row 6, :col 20, :level :warning, :message "Prefer placing return type hint on arg vector: String"}
    {:file "<stdin>", :row 7, :col 14, :level :warning, :message "Prefer placing return type hint on arg vector: String"}]

   (lint! example-without-override))
  (assert-submaps
   [{:file "<stdin>", :row 6, :col 11, :level :warning, :message "Prefer placing return type hint on arg vector: String"}
    {:file "<stdin>", :row 7, :col 14, :level :warning, :message "Prefer placing return type hint on arg vector: String"}]
   (lint! example-with-override)))
