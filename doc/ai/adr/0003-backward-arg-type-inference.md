# ADR 0003: Backward parameter-type inference

## Status

Accepted, on branch `spike-backward-infer`, review complete.

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
2. A hard `^Type` hinted param is not inferred. A nilable hint is sugar for a
   union (`^String` means `#{:nil :string}`) and is recorded as an ordinary
   constraint, so the upgrade to the non-nil tag when the body proves a non-nil
   use, and the fallback to the declaration when nothing resolves, both emerge
   from rule 3's intersection. A hint conflicting with the body leaves callers
   unchecked, and the body itself is flagged at the contradiction by the
   ordinary call-site check.
3. Constraints intersect to the most specific union: keywords and union sets
   normalize to sets, `intersect` keeps the maximal named types implying both
   sides, an
   empty intersection is a conflict and proves nothing, a partial intersection is a set, which
   is a legal spec. So `(defn f [x] (symbol x) (contains? x 1))` infers exactly
   `#{:string}`, the intersection of the two unions. A param whose only
   constraint is a `{:op :keys ..}` map spec passes it through verbatim, so
   wrappers propagate required keys from config specs. Map-shaped constraints
   have no intersection, mixed with others they are parked in `{:op :and :specs ..}`
   and resolved when the cache is synced, where unresolvable members
   contribute nothing.
4. A conditionally guarded usage proves nothing: a usage inside a conditional
   branch
   (`if`, `if-not`, `when`, `when-not`, `cond`, `condp`, `case`, `and`, `or`,
   `if-let`, `when-let`, `if-some`, `when-some`) is skipped via a branch
   count on the ctx. What always evaluates stays on the spine: the first
   operand of `and` and `or`, the first `cond` test, and the `condp` pred and
   dispatch expr. `some->` and `some->>` expand honestly via
   `macroexpand/expand-some->` to `(let [g init] (when (some? g) (-> g ..)))`,
   so the initial expression is spine and the threaded forms are guarded, with
   no inference special case. An unresolved call could be a macro, so its args
   count as a conditional branch too, like a when body. A user config spec
   covering an arity suppresses inference for it, including coverage through
   the `:varargs` fallback, mirroring spec lookup at call sites. Only the body's unconditional
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
   runtime, so it is the contract, and :pre could even feed inference
   directly. Reachability is two ints on the ctx: `:branch-count`, bumped
   on entering conditionally evaluated code, and `:fn-depth`, bumped on
   entering a nested fn body, the fn may run conditionally or never. Each
   fn entry adds its params to the `:param-infers` map with both counters
   pinned as `:branch-mark` and `:fn-mark`, and a usage constrains a param
   directly only while both counters still equal their marks, meaning
   neither a conditional nor an fn boundary was crossed since that fn's
   entry on this descent path. So a
   nested fn's own params infer regardless of enclosing conditionals, only a
   fn's own unconditional spine constrains its params directly. A nested
   fn's constraints on enclosing params are not dropped though, they go
   dormant: fn-body entry allocates a pending sink, and
   `record-constraint!` routes three ways. Same branch count and same
   depth commits to the param, same count but deeper parks the spec in the
   innermost fn's sink, a map of binding to spec set, the dormant twin of
   the param-infer map, a differing count drops, the guard may be what
   makes the usage safe. The sink travels out on the fn's
   analysis meta next to `:arity`, into the local's `:arities` entry for a
   let-bound fn. A call site that proves the fn invoked drains it via
   `activate-pending!`, which replays every entry through the same
   routing: on the owner's spine it commits, inside another nested fn it
   re-pends on that fn, so chains of local fns compose, and in a branch it
   drops, which is exactly the reported false positive, `(cond-> d avg
   (out))` activates nothing. Drains do not consume, dedup absorbs a
   second site. Activation sites: calling a local fn binding
   (analyze-binding-call), and the fn argument of a known higher-order
   call (the analyze-hof set: map, filter, reduce, swap!, update and
   friends), whether written inline or passed by name. Strictly a seq hof
   runs its fn zero or more times, a caller could pass a wrong-typed arg
   plus an always-empty coll, accepted as far-fetched. A fn that is only
   returned or stored stays dormant forever. An earlier revision let every
   nested body constrain enclosing params directly, closing over a param
   is using it, chosen because corpus deltas were zero either way and it
   caught `(defn f [x] #(subs x 1)) (f 42)` at write time. Reverted on the
   first real-world report: a local fn whose body divides by a closed-over
   param was only invoked under `(cond-> d avg (out))`, callers passing
   nil were correct, corpus tests missed it because the pattern needs a
   nil argument at a call site in the same codebase. Immediately invoked
   fn forms `((fn [] ..))` and letfn bindings do not activate yet, rare
   enough to defer.
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
records a deferred `{:op :arg-spec-of :ns .. :name .. :arity .. :arg-idx ..}`
constraint. Constraints with deferred or map-shaped members are stored as
`{:op :and :specs [..]}` in `:args`, joining the existing spec operator family
(`:rest`, `:keys`). Constraints collect in a set, order is immaterial:
intersect is a commutative meet, and resolution intersects every member at
cache sync before anything is serialized, so cache bytes are stable
regardless of recording order, verified by byte-comparing caches under
permuted constraint orders. An earlier revision kept insertion-ordered
vectors with manual dedup, from when unresolved :and entries could reach
the cache verbatim. When the cache is synced,
`types/resolve-types` walks every var's arities once, resolving each
`{:op :and}` arg entry to a concrete spec via `types/resolve-inferred-spec`:
look up the callee's `:args` in idacs (possibly itself inferred, so chains),
guard cycles with a seen set, intersect the contributions. The same walk
flattens deferred `{:call ..}` return tags, and keeps an invariant: the
cached `:args`/`:ret` vocabulary is plain tags plus `:rest` and `:keys`, so
older versions read caches written by newer ones. An
earlier revision resolved lazily in the linters phase, which leaked
`{:op :and}` into the cache and made older binaries warn
"No matching clause: :and" per affected call. The spec op dispatch in
`lint-arg-types` now also skips unknown ops instead of throwing, so future
vocabulary extensions degrade to unchecked args.

Cross-namespace chains still resolve when linting a single file against a warm
cache, the editor scenario: the callee's cached spec is already concrete, the
caller's deferred constraint resolves against it at sync time. A cached spec
is a snapshot, it updates when its own namespace is relinted, same staleness
contract as cached return types and arities.

## Performance

Metabase src, in-process, min of 7 runs, interleaved with master
(benchmarks/bench.clj, same methodology as ADR 0001):

| | min | median | alloc |
|---|---|---|---|
| master | 3312ms | 3428ms | 3561.6MB |
| this branch | 3301ms | 3393ms | 3683.8MB |

Final numbers, measured on the finished branch: branch-count reachability,
cache-sync resolution, the honest some-> expansion and the 1.13 built-in
cache. Wall clock within noise, +3.4% allocation. Spec and predicate lookups
only run when a local actually appears as a direct argument.

## Corpus results

Fourteen findings on the regression corpora, all metabase, all contract
violations per the built-in specs, none crashing at runtime: three
char-sequence findings (a `^Character` into ring's `url-encode` two hops
through `str/replace`, symbols into a `str/replace` helper), four
keyword-family (`(keyword {})` and `(keyword nil)` one hop out), six
get-family (metabase's map-or-id idiom, ids into fns whose spine `get`s the
argument), one nil into an associative-strict op. The get-family findings
proved actionable at every site: callers over-narrowing maps they already held
via `u/the-id`, or a callee hiding type dispatch behind `get`'s totality.
Zero mechanical false positives, two `intersect` implementation bugs were
caught by 32 interim corpus findings and fixed with regression tests.
Earlier iterations without rule 4 produced 76 findings, dominated by
guarded-polymorphic fns like metabase's `js=`. Committed code has survivorship
bias against the crashing bug class this catches, unconditional param misuse
fails on first call, so much of the feature's value is at write time in the
editor.

## Spec strictness under propagation

Inference amplifies spec opinions to callers, which forces per-spec rulings.
The dividing principle: runtime tolerance earns a spec slot when core's own
code or the documented contract relies on it, implementation fall-through does
not. So `contains?` gained `:nil` (clojure.core nil-puns it in `ns-resolve`),
while `get` stays strict (numbers hit RT/get's return-nil-forever fall-through,
nothing in core relies on it) and the char-sequence fns stay strict (the
clojure.string ns docstring names CharSequence as the contract). Corpus
findings from strict specs proved actionable: callers over-narrowing values
they already held, or callees hiding a type dispatch behind a total function
(metabase's map-or-id fns dispatching via `get`'s nil return). The right home
for such declared polymorphism is a per-fn config spec, rule 1.

## Caveats

- `cond`-style branch suppression is per-form. Unresolved calls count as
  branches, but a resolved macro analyzed through the generic catch-all has its
  args treated as evaluated code, consistent with how call-site checking
  already treats macro args.
- The `{:op :and ..}` entries enlarge cached `:arities` slightly.
- The param-infer atoms key on binding records (`Binding`, see
  `impl/utils.clj`). Fine today: the same long-lived objects are looked up
  repeatedly, hasheq is cached after the first hash, and each holds a handful
  of fields. Keying on the record's int `:id` instead would be marginally
  cheaper, but `:id` is only set under `:analyze-locals?`.
- Memoizing the lazy resolutions (ret tags, inferred args) was tried and
  dropped: -5% time and -6% alloc on a synthetic 5000-call-site hot fn, noise
  on metabase. Chains are a few map lookups deep, there is little to save.
  Revisit only if a real profile shows resolution.

## Destructured params

`(defn foo [{:keys [a]}] (inc a))` infers `{:op :keys :nilable true :opt {:a
:number}}` for the param, so `(foo {:a "s"})` and `(foo 42)` warn while
`(foo {})` and `(foo nil)` stay quiet, destructuring nil-punts. The map
branch of `extract-bindings` collects [map-key binding] pairs, covering
`:keys`/`:syms`/`:strs` and their `!` variants via the existing
`destructuring-key` resolution (prefixes, `::auto`), plus `{sym :key}`
renames via `types/map-key`. The pairs travel as `:key-bindings` meta next
to `:keys-spec`, per param through the arg vector's `:keys-bindings`, into
`inferable-params`, which emits `{:idx :binding :constraints}` descriptor
maps, plus `:key` and `:defaulted` for a destructured binding. The recording
side is unchanged, bindings are bindings. At merge, a descriptor carrying a
`:key` has its constraints intersect into the value type of that key: under
`:req`
when the spec excludes nil and the key has no `:or` default, absence then
means nil and a crash, so the key is proven required and `(foo* {})` warns
with a missing required key. Otherwise under `:opt`, joining any `:req` keys
the CLJ-2961 work established. Any required key makes the spec non-nilable,
nil really is missing required keys. Keys specs also chain:
`resolve-deferred-arg-spec` passes a callee's `{:op :keys}` spec through, so
wrappers inherit it.

The forward direction also flows: a destructured binding gets the value type
of its key when the init's map type is known. `analyze-let-like-bindings`
computes the init tag for map binding forms too, and the map branch of
`extract-bindings` distributes it per key instead of letting `:tag` in opts
leak wholesale to children, the `:select` precedent. A concrete
`{:type :map :val ..}` init yields concrete key tags. A deferred `{:call ..}`
init, a user fn's return, defers per key via `:kw-calls`, the same shape
keyword access on a call records, so lint-time resolution is untouched. The
`:as` binding gets the whole init tag. An unknown init types nothing.
Keyword access on a tagged local chases a deferred call under the local's
:tag, so `(let [m (cfg)] (:port m))` and nested chains resolve like keyword
access on the call itself.

A second review round hardened the closed-map semantics: a dynamic map key,
an unquoted symbol or computed form, can evaluate to any key, so such a map
is open and the dynamic entries never land in `:val`. into can overwrite the
input map's entries, so its result keeps key presence but drops value facts, and
a dynamic assoc key does the same for earlier pairs while later known pairs
re-establish them. assoc'd entries keep their source positions for
diagnostics. A value-type finding points at the entry's own coordinates when it has
any, else at the argument, naming the key in the message. Cached return
maps have their entry positions stripped at cache sync, they have no
consumer there, the producer's coordinates could even name another file,
and serializing them only grows the cache. In-run flows keep positions:
a direct literal, or one reaching the call through a let binding, points
at the offending value in the same scope, while anything cross-fn passes
the sync walk and arrives stripped. A stripped entry cannot anchor nested
keys-spec findings either, those fall back to the argument. when-first binds an element, not the init, so it gets no tag.
A qualified `:keys` entry matches its `:or` default by local binding name.
A nil-testing conditional-let strips nil from the binding's eager tag, and a
provably nil init leaves the dead body unchecked, the dead-body warning
stays future work. Known gaps: deferred destructuring resolves keyword keys
only, `:strs`/`:syms` through a fn return are untyped, and literal value
tracking covers keyword and string tokens only.

## Future work

- Guarded evidence with call-site discharge. The dormant-constraint design
  generalizes: evidence plus a premise, believed when the premise is
  discharged. A sink's premise is "this fn runs on the spine", discharged
  at analysis time by an invocation site. A branch-guarded usage could
  record its evidence with the premise "this guard holds", discharged per
  call site from the argument tags: `(defn f [x b] (if b (subs x 1) (inc
  x)))` then `(f 42 true)` warns, a keyword or number arg proves the guard
  truthy, :nil proves it falsy, :boolean or a mixed union proves nothing.
  Guard language stays tiny: a bare param or a known predicate on a param.
  The wall is the cache: a cross-argument conditional spec is new
  vocabulary that cannot be resolved away at sync, so it would start
  in-memory only, cross-library callers see just the unconditional spec.
  The self-guarded case needs no vocabulary at all: "b truthy implies b is
  a number" collapses to the union of the evidence with the falsy tags,
  `#{:number :nil :boolean}`, an ordinary spec, and would catch a caller
  passing a string where today's design only stays quiet on nil.
  Prototyped on branch `self-guard-infer` (8f62b238), shelved on the
  evidence: sound, tests green, four corpora green, but zero finding
  deltas and zero new cached specs across the clojure and clojurescript
  core sources, the direct pattern barely occurs in library code. What
  does fire is the interplay with activation, a guarded call of a local fn
  whose closure constrains the guard param, the reported false positive's
  own shape with a wrong-typed caller. Revisit if the cross-argument
  version is ever built, the prototype's guard threading and routing arm
  are its foundation.

- Arg-type checking at local fn call sites: `(let [f (fn [i] (inc i))] (f
  "foo"))` is silent while the defn twin warns, only arity is checked. The
  data already flows, extract-arity-info keeps the inferred :args and :ret
  per arity and analyze-binding-call has both the local's arity entry and
  an :arg-types atom for the call's children. The work is an inline check
  at analysis time: args-spec-from-arities plus tag-matches?, skipping
  deferred {:call ..} argument tags and in-memory {:op :and} spec members,
  neither resolvable before cache sync. No cache involvement, locals never
  serialize. letfn likely falls out of the same path. Needs corpus
  validation, inferred local specs meeting real call sites is new false
  positive surface.

- Destructured params, second steps: constraints on the `:as` binding could
  constrain the param directly, deferred members inside key value types are
  currently dropped at merge, an `:or` default value could be checked
  against the key's inferred spec, and a definition-site style warning could
  suggest `:keys!` for a proven-required key once 1.13 adoption exists.
- Map literals the user wrote are closed: a missing key is provably nil, in
  direct keyword access, chains, destructuring and fn return maps, and
  keyword access on provable nil is nil. Only literals produce `:val` map
  types, so completeness is exact; maps built via merge or branching get no
  `:val` and stay open, and assoc is modeled precisely, see below. A generated literal, e.g. a hook's
  placeholder `{}` that metabase's defendpoint binds params against, is
  marked `:open` in `map->tag` and proves nothing by absence, detected by
  the generated flag or missing location, and `:or`-defaulted bindings get
  no tag from the init at all. The into fn spec marks a passed-through input
  map `:open`, it adds keys the input's `:val` does not list, and `lint-map!`
  skips required-key reporting for open args. assoc is modeled precisely:
  arg types carry the literal value of keyword and string token args, so
  its fn spec extends the input map's `:val` with each known assoc'd key and the
  result stays closed, opening only for a dynamic key. Corpus: two new
  metabase findings,
  both adjudicated true positives, an OSS defenterprise stub returning `{}`
  whose caller does `(pos? (:max-users ..))` unguarded, plus one ductile
  finding, comment scratch code passing a fn where its call result was
  meant. The ductile corpus also caught the into input-map FP, it needs
  GITHUB_DUCTILE_PAT and runs as a fourth regression test next to metabase,
  clerk and the clj-kondo classpath.
- Vector element types do not flow: `loop`, `doseq` and `for` destructuring
  are untyped, loop also because recur can rebind with other types. A vector
  binding form deliberately gets no init tag, the :vector branch of
  extract-bindings would leak it wholesale onto elements, which :select
  relies on.
- Conditional-let nil handling, remaining pieces: the eager path now strips
  nil from the binding tag and disables type-mismatch in a provably dead
  bound branch, but a deferred init that only resolves to `:nil` at lint
  time is not stripped or deadened. Seen on the metabase corpus:
  `airgap-check-user-count` when-lets over a provably nil deferred return,
  its body is a no-op in OSS, reported as "Expected: number, received: nil"
  at the usage, adjudicated as a true positive wearing the wrong message.
  A dedicated condition-always-false or dead-body warning at the binding
  remains future work, as do element types for `when-first`.
