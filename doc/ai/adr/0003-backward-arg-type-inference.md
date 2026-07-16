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
   order in `lint-arg-types`: config, then built-in, then `:arities`. A
   config-specced arity is also skipped at analysis time, so no inference work
   or cached `:args` for it at all. Matters for malli-style generated configs
   that spec whole codebases. Unspecced sibling arities still infer.
2. A `^Type` hinted param is not inferred. A nilable hint (`^String` maps to
   `:nilable/string`) is upgraded to the non-nil tag when the body proves a
   non-nil use.
3. Constraints merge to the most specific provable tag, using `is-a-relations`
   only. Incomparable constraints prove nothing and the param stays untyped.
   A param whose only constraint is a set spec (`symbol` takes
   `#{:symbol :string :keyword :var}`) or a `{:op :keys ..}` map spec passes it
   through verbatim, call-site checking handles both shapes, so wrappers around
   such fns propagate them, including required keys from config specs.
   `most-specific` has no meet for these shapes, so a set or keys spec mixed
   with other constraints falls back to the keyword fold via the deferred
   path, where non-keyword members contribute nothing.
4. A conditionally guarded usage proves nothing: a usage inside a conditional
   branch
   (`if`, `if-not`, `when`, `when-not`, `cond`, `condp`, `case`, `and`, `or`,
   `if-let`, `when-let`, `if-some`, `when-some`) is skipped via a per-level
   branched flag. An unresolved call could be a macro, so its args count as a
   conditional branch too, like a when body. Only the body's unconditional
   spine constrains, and a spine usage constrains even when the param is
   type-tested elsewhere, the use runs regardless:
   `(defn f [x] (when (nil? x) x) (subs x 1))` proves `x` is a string, so
   `(f nil)` is flagged, it throws. Type predicates need no special handling
   for inference, their arg spec is `:any` and records nothing. An earlier
   revision marked predicate-tested params `:poly` (never infer). That masked
   the case above and only protected type dispatch through unresolved macros,
   which the branch treatment of unresolved calls now covers structurally.
   Narrowed usages need no separate skip, narrowing only happens in branches.
   If spine narrowing ever exists (assert, :pre, guard clauses that throw), a
   narrowed spine usage should constrain: the guard enforces the type at
   runtime, so it is the contract, and :pre could even seed inference
   directly. A nested fn body is a new inference level pushed onto
   `:param-infers`: its own params start unbranched regardless of enclosing
   conditionals, and a usage of an enclosing fn's param in the nested body
   still constrains it, closing over a param is using it. A conditional marks
   every enclosing level as branched, so a fn created inside a branch, or a
   guarded usage inside the fn, proves nothing about the outer param. Settled
   empirically: both including and excluding nested-fn usages produced zero
   corpus deltas, so the version that catches
   `(defn f [x] #(subs x 1)) (f 42)` at write time won.
5. `:char-sequence` constraints propagate on both platforms. The JVM impls
   coerce via `.toString`, so a symbol into `str/replace` happens to work
   there, but the clojure.string ns docstring (design note 4) documents the
   contract as CharSequence, and the CLJS impls throw or, for `ends-with?`,
   return a wrong answer silently. Provable means provable by contract. An
   earlier revision skipped these constraints on `:clj` and was reverted.

## The trigger point

Inference triggers where a local usage is analyzed, not by scanning call
arguments. `analyze-call` attaches `::types/infer-call [ns name arity]` to the
metadata of the callstack entry it pushes, for resolved calls in fn bodies with
inferable params. At the single place where a token resolves to a binding
(usages.clj, next to `reg-used-binding!`), the binding is a direct argument of
an inference-eligible call exactly when the callstack head carries that key,
and the argument index is the current `:arg-types` count.
`types/infer-local-usage!` classifies the callee and records the constraint.
This keeps local recognition structural: `(var x)` and quoted forms never reach
the usage path, a local inside a collection argument sits under a `[nil
:vector]` head with no key, and there is no stored state to go stale.

One callstack wart surfaced: `analyze-hof` pushes the hof'd fn as a
pseudo-frame ((map inc xs) analyzes under a pretend `[clojure.core inc]`
frame), shadowing the hof call's own entry while the remaining args are still
positionally the hof's args. The pseudo-frame carries the previous head's
`::types/infer-call` forward. Longer term callstack entries could become maps,
making this key and the pseudo-frame marking plain data (queued as its own
refactor, 65 consumer sites, needs benching since entry matching is hot).

Two earlier revisions: a call-site hook scanning argument tokens syntactically,
and a ctx-level descriptor with a memoized classification. All three produced
identical behavior and corpus results. The head-meta version is the smallest
and structurally soundest, classification is a few map lookups so the memo was
not worth its volatile.

## Transitive inference

`(defn foo [x] (bar x))` constrains `x` by whatever `bar` requires, including
when `bar`'s own params were inferred. A call to a spec-less resolved user fn
records a deferred `{:call {ns name arity arg-idx lang base-lang}}` constraint.
Constraint sets containing deferred entries are stored as
`{:constraints .. :hint ..}` in `:args` and resolved in the linters phase by
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

Wall clock within noise, +1.4% allocation with the call-site trigger, +0.6%
more with the head-meta usage-point trigger (the meta map per eligible call).
Spec and predicate lookups only run when a local actually appears as a direct
argument.

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

- `cond`-style branch suppression is per-form. Unresolved calls count as
  branches, but a resolved macro analyzed through the generic catch-all has its
  args treated as evaluated code, consistent with how call-site checking
  already treats macro args.
- The `{:constraints ..}` entries enlarge cached `:arities` slightly.
- The param-infer atoms key on binding maps. Fine today: the same long-lived
  objects are looked up repeatedly, hasheq is cached after the first hash, and
  the maps hold a handful of entries. If bindings ever become a record, key on
  an int `:id` field instead (`:id-gen` exists but only under
  `:analyze-locals?`).
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
