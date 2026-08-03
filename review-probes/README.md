# Review probes for #2927 (:redefined-spec)

Throwaway scripts behind the review of PR #2927. Not for merge.

Run from the repo root, with a writable temp dir:

    clojure -J-Djava.io.tmpdir=$TMPDIR \
      -Sdeps '{:aliases {:probe {:extra-paths ["review-probes"]}}}' \
      -M:probe -m probe

- `probe.clj` - rename, delete, path spelling, symbol-keyed `s/def`, cache
  contents, cross-run reporting.
- `probe2.clj` - path spelling under `:canonical-paths`, plus cost on a corpus.
  Takes source dirs as arguments.
- `probe3.clj` - index scaling. Generates N files with M `s/def` each, then
  times single-file re-lints against the resulting index. Takes N and M.
- `probe5.clj` - `(comment ...)`, `:config-in-ns`, ignore hints.
- `probe6.clj` - the `:redefined-var` precedent for `(comment ...)`.
- `probe7.clj` - relative vs absolute path in one cache. The clearest repro of
  the stale index entry.
- `topo_bench.clj` - unrelated to this PR: cost of the topological source sort
  on branch `issue-2284`. Takes source dirs as arguments.

The `.out` files are the recorded results.
