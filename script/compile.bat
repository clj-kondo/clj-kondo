@echo off

Rem set GRAALVM_HOME=C:\Users\IEUser\Downloads\graalvm\graalvm-ce-19.2.1
Rem set PATH=%PATH%;C:\Users\IEUser\bin

if "%GRAALVM_HOME%"=="" ( 
    echo Please set GRAALVM_HOME
    exit /b
)
set JAVA_HOME=%GRAALVM_HOME%\bin
set PATH=%PATH%;%GRAALVM_HOME%\bin

set /P CLJ_KONDO_VERSION=< resources\CLJ_KONDO_VERSION
echo Building clj-kondo %CLJ_KONDO_VERSION%

call lein do clean, uberjar
if %errorlevel% neq 0 exit /b %errorlevel%

Rem the --no-server option is not supported in GraalVM Windows.
call %GRAALVM_HOME%\bin\native-image.cmd ^
  "-jar" "target/clj-kondo-%CLJ_KONDO_VERSION%-standalone.jar" ^
  "-H:Name=clj-kondo" ^
  "-H:+ReportExceptionStackTraces" ^
  "-J-Dclojure.spec.skip-macros=true" ^
  "-J-Dclojure.compiler.direct-linking=true" ^
  "-H:IncludeResources=clj_kondo/impl/cache/built_in/.*" ^
  "-H:ReflectionConfigurationFiles=reflection.json" ^
  "--initialize-at-build-time"  ^
  "-H:Log=registerResource:" ^
  "--no-fallback" ^
  "--verbose" ^
  "-J-Xmx3g"

if %errorlevel% neq 0 exit /b %errorlevel%

echo Creating zip archive
jar -cMf clj-kondo-%CLJ_KONDO_VERSION%-windows-amd64.zip clj-kondo.exe

@echo on
echo (inc 1) | clj-kondo --lint -
