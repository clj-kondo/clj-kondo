(ns clj-kondo.clojure-data-xml-test
  (:require
    [babashka.fs :as fs]
    [clj-kondo.core :as clj-kondo]
    [clj-kondo.test-utils :refer [lint! #_assert-submaps]]
    [clojure.test :refer [deftest is testing]]
    [clojure.tools.deps.alpha :as deps]))

(deftest alias-uri-test
  (is (empty? (lint! "
(ns foo
  (:require [clojure.data.xml :refer [alias-uri]]))

(alias-uri 'xh \"http://xml.weather.yahoo.com/ns/rss/1.0\")

::xh/location

(alias-uri
 :U \"uri-u:\"
 :D \"DAV:\"
 'V \"uri-v:\"
 \"W\" \"uri-w:\")

::U/foo
::D/foo
::V/foo
::W/foo

"))))


(defn lint!! [cache-dir s]
  (with-in-str s
    (-> (clj-kondo/run! {:lint ["-"]
                         :cache cache-dir})
        :findings)))

(deftest unresolved-var-test
  (let [cache (str (fs/create-temp-dir))
        deps '{:deps {;; org.clojure/clojure {:mvn/version "1.9.0"}
                      org.clojure/data.xml {:mvn/version "0.2.0-alpha6"}}
               :mvn/repos {"central" {:url "https://repo1.maven.org/maven2/"}
                           "clojars" {:url "https://repo.clojars.org/"}}}
        jar (-> (deps/resolve-deps deps nil)
                (get-in ['org.clojure/data.xml :paths 0]))]
    (clj-kondo/run! {:lint [jar] :cache-dir cache})
    (is (empty? (lint!! cache "
(ns foo
  (:require [clojure.data.xml :refer [element]]))
element")))))
