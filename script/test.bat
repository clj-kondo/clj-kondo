@echo off

rem populate mvn cache for extract tests
clojure -Sdeps '{:deps {org.clojure/clojure {:mvn/version ""1.9.0""}}}' -Spath > NUL
clojure -Sdeps '{:deps {org.clojure/clojure {:mvn/version ""1.10.1""}}}' -Spath > NUL
clojure -Sdeps '{:deps {org.clojure/clojurescript {:mvn/version ""1.10.520""}}}' -Spath > NUL

if "%CLJ_KONDO_TEST_ENV%"=="native" (
  clojure -A:test
)
else (
  echo "Testing with Clojure 1.9.0"
  clojure -A:clojure-1.9.0:test
  lein with-profiles +clojure-1.9.0 do clean, test

  echo "Testing with Clojure 1.10.1"
  clojure -A:clojure-1.10.1:test
  lein with-profiles +clojure-1.10.1 do clean, test
)
