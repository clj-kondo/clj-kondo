Rem @echo off
if "%GRAALVM_HOME%"=="" ( 
    echo "Please set GRAALVM_HOME"
    exit /b
)
set %JAVA_HOME%=%GRAAL_VM_HOME%\bin
set %PATH%=%PATH%;%GRAAL_VM_HOME%\bin

call lein clean
call lein uberjar
set /P CLJ_KONDO_VERSION=< resources\CLJ_KONDO_VERSION 
echo "Building clj-kondo %CLJ_KONDO_VERSION"

call %GRAALVM_HOME%\bin\native-image.cmd ^
  -jar target/clj-kondo-%CLJ_KONDO_VERSION%-standalone.jar ^
  -H:Name=clj-kondo -H:ReflectionConfigurationFiles=reflection.json ^
  --initialize-at-build-time "-H:IncludeResources=clj_kondo/impl/cache/built_in/.*" ^
  -H:Log=registerResource:
