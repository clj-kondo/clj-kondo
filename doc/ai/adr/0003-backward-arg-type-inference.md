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
   constrains. A nested fn body is a separate spine: a usage there proves
   nothing about the outer fn's params (the fn may never run), and an outer
   conditional does not make the nested body conditional. `analyze-fn-body`
   drops both `:param-infer` and `:in-branch` on entry.
5. `:char-sequence` constraints propagate on both platforms. The JVM impls
   coerce via `.toString`, so a symbol into `str/replace` happens to work
   there, but the clojure.string ns docstring (design note 4) documents the
   contract as CharSequence, and the CLJS impls throw or, for `ends-with?`,
   return a wrong answer silently. Provable means provable by contract. An
   earlier revision skipped these constraints on `:clj` and was reverted.

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

Three new findings on the regression corpora (all metabase), each a contract
violation that works on the JVM by toString coercion only: a `^Character` into
ring's `url-encode` two hops through `str/replace`, and symbols into a helper
that forwards to `str/replace`. Zero false positives. An earlier iteration
without rule 4 produced 76 findings, dominated by guarded-polymorphic fns like
metabase's `js=`. Committed code has survivorship bias against the crashing
bug class this catches, unconditional param misuse fails on first call, so
much of the feature's value is at write time in the editor.

## Caveats

- `cond`-style branch suppression is per-form. A conditional analyzed through
  the generic catch-all that is not in the rule 4 list would leak constraints.
- Predicate `:poly` marking keys on `types/predicate->tag`, so a new predicate
  added there tightens inference automatically (`nil?` already does: a
  nil-checked param is not inferred).
- The `{:infer ..}` entries enlarge cached `:arities` slightly.
- Memoizing the lazy resolutions (ret tags, inferred args) was tried and
  dropped: -5% time and -6% alloc on a synthetic 5000-call-site hot fn, noise
  on metabase. Chains are a few map lookups deep, there is little to save.
  Revisit only if a real profile shows resolution.

## Future work

- Destructured params: `(defn foo [{:keys [a]}] (inc a))` can infer a map
  param spec `{:op :keys :opt {:a :number}}`. The pieces mostly exist:
  `lint-map!` checks such specs at call sites and `extract-bindings` already
  feeds `:keys-spec` into `:args` for the CLJ-2961 required-keys work. New
  part: collect destructured key bindings into param-infer with a
  [param-idx key] path and merge constraints into the keys spec. Start with
  `:opt` plus a type. An unconditional use also proves the key required
  (missing means nil and a crash), so promoting to `:req` is a candidate
  second step. Direct keyword constraints only at first, no deferred entries
  inside map specs.
