(ns clj-kondo.clojure-data-xml-test
  (:require
    [clj-kondo.test-utils :refer [lint! #_assert-submaps]]
    [clojure.test :refer [deftest is testing]]))

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



