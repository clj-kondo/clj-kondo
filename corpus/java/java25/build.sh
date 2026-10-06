#!/usr/bin/env bash
set -eo pipefail
cd "$(dirname "$0")"
rm -rf classes
javac --release 25 -d classes src/java25/*.java
jar --create --file ../java25.jar -C classes . -C src java25/core.clj
rm -rf classes
