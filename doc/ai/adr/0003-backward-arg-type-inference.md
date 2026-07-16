# ADR 0003: Backward parameter-type inference

## Status

Implemented on branch `spike-backward-infer`, in review.

## Context

For a user fn the `:args` spec came only from `^type` hints on params. The body
already constrains the params:

    (defn slugify [s] (subs s 1))   ;; subs proves s is a string
    (slugify 42)                    ;; was not flagged

Return types were inferred from the body, argument types were not.

## Decision

While a fn body is analyzed, a param passed directly to a callee with a known
arg spec records that spec as a constraint on the param. After the body the
constraints are merged into the arity's `:args`, and existing call-site
checking does the rest.

Rules, in order of precedence:

1. A user config spec for the fn wins. This falls out of the existing lookup
   order in `lint-arg-types`: config, then built-in, then `:arities`.
2. A `^Type` hinted param is not inferred. A nilable hint (`^String` maps to
   `:nilable/string`) is upgraded to the non-nil tag when the body proves a
   non-nil use.
3. Constraints merge to the most specific provable tag, using `is-a-relations`
   only. Incomparable constraints prove nothing and the param stays untyped.
4. A conditionally guarded usage proves nothing. This has three mechanisms:
   a param passed to a core type predicate (`types/predicate->tag`, resolved
   via `core-symbol-in-scope?`) is marked `:poly` and excluded entirely, a
   usage on a flow-narrowed binding is skipped, and a usage inside a
   conditional branch (`if`, `if-not`, `when`, `when-not`, `cond`, `condp`,
   `case`, `and`, `or`, `if-let`, `when-let`, `if-some`, `when-some`) is
   skipped via an `:in-branch` flag. Only the body's unconditional spine
   constrains.
5. A `:char-sequence` constraint is not recorded when the lang is `:clj`: the
   char-sequence fns coerce via `.toString` on the JVM, so the requirement is
   not provable there. In CLJS they throw (or worse, `ends-with?` returns a
   wrong answer silently), so the constraint stands. Direct calls keep flagging
   in both langs. Open question with the core team: whether JVM toString
   coercion is a supported contract. If not, this rule can be dropped and the
   constraint propagates on `:clj` too.

## Transitive inference

`(defn foo [x] (bar x))` constrains `x` by whatever `bar` requires, including
when `bar`'s own params were inferred. A call to a spec-less resolved user fn
records a deferred `{:call {ns name arity arg-idx lang base-lang}}` constraint.
Constraint sets containing deferred entries are stored as
`{:infer constraints :hint h}` in `:args` and resolved in the linters phase by
`types/resolve-inferred-spec`: look up the callee's `:args` in idacs (possibly
itself inferred, so chains), guard cycles with a seen set, merge with
most-specific, fall back to the hint. This mirrors how deferred return tags
already resolve via `resolve-arg-type`.

The deferred shape is plain data and round-trips through the transit cache:
linting a single file against a warm cache still resolves cross-namespace
chains. This is the editor scenario.

## Performance

Metabase src, in-process, min of 7 runs, interleaved with master
(benchmarks/bench.clj, same methodology as ADR 0001):

| | min | median | alloc |
|---|---|---|---|
| master | 3323ms | 3383ms | 3598.6MB |
| this branch | 3302ms | 3348ms | 3648.4MB |

Wall clock within noise, +1.4% allocation. The hot path defers all spec and
predicate lookups until an argument is a bare param, so the common call costs a
token check per argument.

## Corpus results

Zero new findings and zero false positives on the three regression corpora
(clj-kondo classpath, metabase, clerk). Earlier iterations produced 76 findings
without rule 4 (dominated by guarded-polymorphic fns like metabase's `js=`) and
3 without rule 5 (all JVM toString idioms, e.g. a symbol into a callee's
`str/replace`). Committed code has survivorship bias against the bug class this
catches, unconditional param misuse crashes on first call, so the feature's
value is at write time in the editor.

## Caveats

- `cond`-style branch suppression is per-form. A conditional analyzed through
  the generic catch-all that is not in the rule 4 list would leak constraints.
- Predicate `:poly` marking keys on `types/predicate->tag`, so a new predicate
  added there tightens inference automatically (`nil?` already does: a
  nil-checked param is not inferred).
- The `{:infer ..}` entries enlarge cached `:arities` slightly.
