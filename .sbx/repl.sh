#!/usr/bin/env bash
set -euo pipefail

NAME=clj-kondo-repl

if ! sbx ls -q | grep -Fxq "$NAME"; then
  sbx create --debug shell \
    --name "$NAME" \
    --kit "$HOME/dev/clj-kondo/.sbx" \
    "$HOME/dev/clj-kondo" \
    "$HOME/.m2"
fi

sbx ports --debug "$NAME" --publish 7888:7888
sbx run --debug --name "$NAME"
