#!/usr/bin/env bash
set -eo pipefail
cd "$(dirname "$0")"
rm -rf classes unsupported
javac --release 25 -d classes src/java25/*.java
jar --create --file ../java25.jar -C classes . -C src java25/core.clj
mkdir -p unsupported/java25
cp classes/java25/Shape.class unsupported/java25/
printf '\x03\xe7' | dd of=unsupported/java25/Shape.class bs=1 seek=6 conv=notrunc 2>/dev/null
jar --create --file ../unsupported-class-version.jar -C unsupported . -C src unsupported/core.clj
rm -rf classes unsupported
