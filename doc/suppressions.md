# Baseline suppressions

Baseline suppressions let an existing project adopt clj-kondo without reporting
all current error findings. Warning and info findings are not suppressed. When
error findings for a linter in a file exceed the recorded count, they remain
visible and affect the exit code normally.

Generate a baseline for all current findings:

``` shell
clj-kondo --lint src test --suppress-all
```

This creates `.clj-kondo/suppressions.edn`. Commit this file so local runs and CI
use the same baseline. Later runs load it automatically:

``` shell
clj-kondo --lint src test
```

To baseline only selected linters, repeat `--suppress-rule`:

``` shell
clj-kondo --lint src test \
  --suppress-rule unresolved-symbol \
  --suppress-rule unused-binding
```

Generating selected rules replaces existing baseline entries for those rules and
preserves entries for other files and rules. `--suppress-all` and
`--suppress-rule` cannot be used together.

When a suppression count exceeds the current error count, clj-kondo exits with
code 2 and asks you to prune the unused suppression. To ignore unused
suppressions temporarily, use `--pass-on-unpruned-suppressions`.

Remove entries or excess occurrence counts that no longer match current error
findings:

``` shell
clj-kondo --lint src test --prune-suppressions
```

Pruning cannot be combined with either suppression generation option.

Use `--suppressions-location <path>` to read and write a different file. The
option must be passed on subsequent runs that should use that file. When the path
is a directory, clj-kondo creates a suppression file in that directory with a
name derived from the project location.

## Matching

Each suppression records the filename, linter type and number of occurrences.
Filenames are stored relative to the project root with portable separators.
Rows, columns and messages are intentionally omitted, so moving or editing
existing findings within a file does not invalidate the baseline.

Findings are counted by filename and linter type. When the current count is less
than or equal to the baseline count, all findings in that group are suppressed.
When the current count exceeds the baseline, all findings in that group are
reported because clj-kondo cannot reliably distinguish old findings from new
ones without using unstable source locations.

Baseline suppressions are applied after source metadata and
`:clj-kondo/ignore` forms. Existing ignore behavior is unchanged.

## Clojure API

Pass `:apply-suppressions true` to `clj-kondo.core/run!` to apply an existing
baseline. The result includes suppressed findings under `:suppressed-findings`
and stale entries under `:unused-suppressions`. Suppressions are not applied by
default when using the Clojure API. Generating and pruning suppressions is only
supported through the CLI.
