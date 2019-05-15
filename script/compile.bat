@echo off
if "%GRAALVM_HOME%"=="" ( 
    echo Please set GRAALVM_HOME
    exit /b
)
set %JAVA_HOME%=%GRAAL_VM_HOME%\bin
set %PATH%=%PATH%;%GRAAL_VM_HOME%\bin

set /P CLJ_KONDO_VERSION=< resources\CLJ_KONDO_VERSION
echo Building clj-kondo %CLJ_KONDO_VERSION%

call lein clean
call lein uberjar

call %GRAALVM_HOME%\bin\native-image.cmd ^
  -jar target/clj-kondo-%CLJ_KONDO_VERSION%-standalone.jar ^
  -H:Name=clj-kondo ^
  -H:+ReportExceptionStackTraces ^
  -J-Dclojure.spec.skip-macros=true ^
  -J-Dclojure.compiler.direct-linking=true ^
  "-H:IncludeResources=clj_kondo/impl/cache/built_in/.*" ^
  -H:ReflectionConfigurationFiles=reflection.json ^
  --initialize-at-build-time  ^
  -H:Log=registerResource: ^
  --verbose ^
  "-J-Xmx3g"

echo Creating zip archive

jar -cMf clj-kondo-%CLJ_KONDO_VERSION%-windows-amd64.zip clj-kondo.exe
