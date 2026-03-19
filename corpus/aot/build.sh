#!/usr/bin/env bash
# Builds the AOT-only test jar: aot-only.jar
# The jar contains only .class files (no .clj source).
set -euo pipefail

cd "$(dirname "$0")"

rm -rf classes
mkdir classes

# AOT-compile
clojure -Srepro -Sdeps '{:paths ["src" "classes"]}' \
  -M -e "(binding [*compile-path* \"classes\"] (compile 'aot-test.sample))"

# Package only .class files into jar (no source)
jar cf aot-only.jar -C classes .

rm -rf classes
echo "Built aot-only.jar"
