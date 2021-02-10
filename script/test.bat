rem force download of clojure 1.10.1 for extraction test
call lein with-profiles +clojure-1.10.2 deps > NUL

echo "CLJ_KONDO_TEST_ENV: %CLJ_KONDO_TEST_ENV%"

IF "%CLJ_KONDO_TEST_ENV%"=="native" (
  lein do clean, test
) ELSE (
  rem else branch
  echo "Testing with Clojure 1.9.0"
  rem clojure -A:clojure-1.9.0:test
  call lein with-profiles +clojure-1.9.0 do clean, test

  echo "Testing with Clojure 1.10.2"
  rem clojure -A:clojure-1.10.1:test
  call lein with-profiles +clojure-1.10.2 do clean, test
)
