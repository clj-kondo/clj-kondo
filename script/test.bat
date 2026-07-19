rem force download of clojure 1.11.4 for the test run
call lein with-profiles +clojure-1.11.4 deps > NUL

echo "CLJ_KONDO_TEST_ENV: %CLJ_KONDO_TEST_ENV%"

IF "%CLJ_KONDO_TEST_ENV%"=="native" (
  lein do clean, test
) ELSE (
  rem else branch
  echo "Testing with Clojure 1.11.4"
  call lein with-profiles +clojure-1.11.4 do clean, test
)
