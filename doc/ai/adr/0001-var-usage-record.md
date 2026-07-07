# ADR 0001: VarUsage record instead of per-usage hash map

## Status

Implemented (`var-usage-record` branch).

## Context

Every var usage allocates a map with 25-30 keys (analyzer.clj `analyze-call`
x2, `analyze-hof`, usages.clj `analyze-usages2`, plus a rare site in
analyzer/namespace.clj for `:use` refers). With >16 entries these become
`PersistentHashMap`s: `RT.mapUniqueKeys` was the single largest allocation
site inside `analyze-call` (7% of all allocation on a metabase lint), and
`lint-var-usage` reads ~30 keys back from each map via hash lookups
(`PersistentHashMap.valAt` was the top hash-map CPU frame).

## Decision

`defrecord VarUsage` in `clj-kondo.impl.utils` with the union of all keys
(39 fields). A `var-usage` macro takes the existing literal map and emits a
positional constructor call at compile time, so the call sites keep their
readable map syntax and nothing is allocated but the record itself.

Details that mattered:

- The macro emits `(new clj_kondo.impl.utils.VarUsage ...)`, not
  `(->VarUsage ...)`: with 39 fields the positional factory fn goes through
  RestFn and allocates an args seq per call. Using the factory cost +400MB
  allocation on the metabase corpus.
- `assoc` on a 39-field record copies all fields, so per-usage assocs were
  removed: `reg-var-usage!` no longer re-assocs `:config` (all construction
  sites already set it from the same ctx) and only assocs
  `:unresolved-symbol-disabled?` when it flips to true; `analyze-call` folds
  `:id`/`:in-def` into the construction and keeps only the conditional
  `:ret` assoc.
- The `:use`-refer site in analyzer/namespace.clj still produces a plain map
  (it assocs onto node meta); consumers only do keyword access, so mixing
  records and maps in `:used-vars` is fine.
- `:used-vars` never reaches the transit cache (only `:vars` defs do), so no
  serialization concerns.
- Keys outside the field list keep working via the record's extmap; unknown
  keys in a `var-usage` literal fail at compile time.

## Measurement

metabase src, JVM, in-process, min of 7 runs, master and branch interleaved
(benchmarks/bench.clj):

| | time | allocation |
|---|---|---|
| master | 4397ms | 4828MB |
| this change | 4026-4420ms | 4170MB |

-13.5% allocation (per-thread allocated bytes, deterministic to ±0.1MB).
Wall clock is noisy on this machine; the branch won the interleaved pairings
by roughly 5-10%. Findings on metabase byte-identical (12177,
benchmarks/dump-findings.clj diff).

## Caveats

- Record `=` differs from map `=` (type-sensitive). No consumer compares
  usages for equality; keyword access, destructuring and assoc of declared
  fields all behave as before.
- `dissoc` of a declared field would degrade the record to a plain map;
  nothing does that today.
- If `:used-vars` ever gets cached, transit has no write handler for the
  record and will throw — a loud failure, but the cache write path would
  need a handler or a plain-map conversion at that point.
