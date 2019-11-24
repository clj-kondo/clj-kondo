@echo off

rem clojure doesn't work yet
rem populate mvn cache for extract tests
rem clojure -Sdeps '{:deps {org.clojure/clojure {:mvn/version ""1.9.0""}}}' -Spath > NUL
rem clojure -Sdeps '{:deps {org.clojure/clojure {:mvn/version ""1.10.1""}}}' -Spath > NUL
rem clojure -Sdeps '{:deps {org.clojure/clojurescript {:mvn/version ""1.10.520""}}}' -Spath > NUL

if "%CLJ_KONDO_TEST_ENV%"=="native" (
  rem clojure -A:test
) else (
  echo "Testing with Clojure 1.9.0"
  rem clojure -A:clojure-1.9.0:test
  call lein with-profiles +clojure-1.9.0 do clean, test

  echo "Testing with Clojure 1.10.1"
  rem clojure -A:clojure-1.10.1:test
  call lein with-profiles +clojure-1.10.1 do clean, test
)
