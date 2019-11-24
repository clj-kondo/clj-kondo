@echo off

rem force download of clojure 1.10.1 for extraction test
lein with-profiles +clojure-1.10.1 deps

if "%CLJ_KONDO_TEST_ENV%"=="native" (
  rem clojure -A:test
) else (
  echo "Testing with Clojure 1.9.0"
  rem clojure -A:clojure-1.9.0:test
  call lein with-profiles +test, +clojure-1.9.0 do clean, test

  echo "Testing with Clojure 1.10.1"
  rem clojure -A:clojure-1.10.1:test
  call lein with-profiles +clojure-1.10.1 do clean, test
)
