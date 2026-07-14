# Baseline suppressions

Baseline suppressions let an existing project adopt clj-kondo without reporting
all current findings. Findings added after the baseline was generated remain
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

Remove entries or excess occurrence counts that no longer match current
findings:

``` shell
clj-kondo --lint src test --prune-suppressions
```

Pruning cannot be combined with either suppression generation option.

Use `--suppressions-location <file>` to read and write a different file.

## Matching

Each suppression records the filename, linter type, message and number of
occurrences. Filenames are stored relative to the project root with portable
separators. Rows and columns are intentionally omitted, so moving unchanged code
within a file does not invalidate the baseline.

Occurrences with the same filename, type and message are matched by count. If an
old occurrence is fixed while an identical new occurrence is added to the same
file, the new occurrence may remain suppressed until the baseline is pruned or
regenerated.

Baseline suppressions are applied after source metadata and
`:clj-kondo/ignore` forms. Existing ignore behavior is unchanged.
