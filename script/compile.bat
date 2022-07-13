@echo off

Rem set GRAALVM_HOME=C:\Users\IEUser\Downloads\graalvm\graalvm-ce-19.2.1
Rem set PATH=%PATH%;C:\Users\IEUser\bin

if "%GRAALVM_HOME%"=="" (
    echo Please set GRAALVM_HOME
    exit /b
)

set JAVA_HOME=%GRAALVM_HOME%\bin
set PATH=%GRAALVM_HOME%\bin;%PATH%

dir %GRAALVM_HOME%\bin

set /P CLJ_KONDO_VERSION=< resources\CLJ_KONDO_VERSION

echo Building clj-kondo %CLJ_KONDO_VERSION%

set CLJ_KONDO_NATIVE=true

call lein with-profiles +clojure-1.10.2 do clean, uberjar
if %errorlevel% neq 0 exit /b %errorlevel%

call %GRAALVM_HOME%\bin\gu.cmd install native-image

Rem the --no-server option is not supported in GraalVM Windows.
call %GRAALVM_HOME%\bin\native-image.cmd ^
  "-jar" "target/clj-kondo-%CLJ_KONDO_VERSION%-standalone.jar" ^
  "-H:+ReportExceptionStackTraces" ^
  "--no-fallback" ^
  "--verbose" ^
  "-J-Xmx3g"

if %errorlevel% neq 0 exit /b %errorlevel%
