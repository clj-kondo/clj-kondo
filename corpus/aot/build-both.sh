#!/usr/bin/env bash
# Builds a jar with BOTH .clj source and .class files: aot-with-source.jar
# This tests that source analysis takes precedence over AOT extraction.
set -euo pipefail

cd "$(dirname "$0")"

rm -rf classes-both
mkdir classes-both

# AOT-compile
clojure -Srepro -Sdeps '{:paths ["src" "classes-both"]}' \
  -M -e "(binding [*compile-path* \"classes-both\"] (compile 'aot-test.sample))"

# Package both .clj source AND .class files into jar
jar cf aot-with-source.jar -C src . -C classes-both .

rm -rf classes-both
echo "Built aot-with-source.jar"
