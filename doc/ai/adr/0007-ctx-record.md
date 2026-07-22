# ADR 0007: ctx as a record

## Status

Parked. POC on branch `poc-ctx-record` (pushed), measured, not merged.

## Context

After the 2026-07 method-size splits (#2907-#2911) removed the JIT
interpretation tax, an allocation profile of a metabase lint attributed ~23%
of all allocation to hash-map internals (`BitmapIndexedNode`, `INode[]`,
`PersistentHashMap`), spread across the ctx-assoc'ing pipeline:
`analyze-expression**` 3.4%, `analyze-call` 2.0%, `lint-resolved-call!` 2.0%,
`analyze-children` 2.6%, `analyze-usages2` 1.5%, `reg-var-usage!` 2%. The ctx
map has ~40 root keys and is assoc'd on every expression. ADR 0001 (VarUsage
record, -13.5% alloc and 5-10% time) suggested the same move could pay here.

## Decision (POC)

`defrecord Ctx` in `clj-kondo.impl.utils` with the 20 keys that are assoc'd
per node or per call as basis fields: config, lang, base-lang, filename, ns,
callstack, bindings, arg-types, in-def, top-level?, quoted,
syntax-quote-level, recur-arity, seen-recur?, protocol-fn,
mark-bindings-used?, context, idx, fn-dupes, data-readers. The ~25 cold root
keys (atoms, analysis flags) stay in the extmap, which every basis assoc
shares by pointer. The root ctx literal in `clj-kondo.core/run!` is wrapped
in `map->Ctx`. 20 fields is also the ceiling: the positional factory fn hits
the 20-param limit past that.

Constraints that shaped the basis:

- `dissoc` of a basis field demotes a record to a plain map silently. The
  five `(dissoc ctx ...)` sites were converted to `(assoc ctx ... nil)` after
  checking all readers are truthiness-only.
- Basis fields are always present, so `(:k ctx default)` never falls back to
  the default. `:branch-count` and `:fn-depth` are read with `(:k ctx 0)` and
  therefore stay out of the basis.
- One conversion exposed a pre-existing bug in `analyze-loop`:
  `(-> (assoc ctx :seen-recur? seen-recur?) (dissoc ctx :protocol-fn))`
  dissoc'ed the ctx map as a key, a no-op. Fixed on the POC branch, still
  present on main.

## Results

Metabase lint, interleaved against main (cc0ffb79), min of 3 runs x 3 rounds:

| metric | main | ctx record | delta |
|---|---|---|---|
| alloc, default config | 4253 MB | 3997 MB | -6.0% |
| alloc, types off | 3949 MB | 3706 MB | -6.2% |
| time, default config | 2700 ms | 2713 ms | noise |
| time, types off | 2398 ms | 2407 ms | noise |

Findings byte-identical, unit suite green.

## Why time is flat

The 23% hash-map share did not translate. A record assoc still allocates a
full 20-field instance per assoc, so the saving per assoc is bytes, not an
allocation eliminated. And after the method-size splits, lint time is not
GC-bound, so a -6% allocation cut buys no wall clock. VarUsage was different:
a create-once leaf object whose map construction and hash reads were both on
the hot path.

## Consequences

Not merged. The record semantics (silent demotion on basis dissoc, basis keys
always present) are a standing trap for future ctx code, and -6% alloc with
flat time does not pay for that. Revisit only for a GC-constrained embedder,
the branch has the working implementation. The `analyze-loop` stray-ctx
dissoc fix should land on main separately.
