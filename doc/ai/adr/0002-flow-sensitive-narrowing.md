# ADR 0002: Flow-sensitive type narrowing

## Status

Positive narrowing merged for `if` and `when`. Union narrowing over an `or` of
predicates lives on branch `flow-narrow-or`, unmerged (see below). Negative
narrowing deferred.

## Context

The `:type-mismatch` checker is position-local. A type-predicate guard proves a
value's type inside a branch, but the checker did not use that fact:

    (defn f [x] (if (string? x) (inc x) 0))

`x` is a string in the then-branch, so `(inc x)` is a bug. clj-kondo did not
flag it.

## Decision

In the then branch of `(if (pred x) ..)` and the body of `(when (pred x) ..)`,
narrow `x` to the type `pred` proves.

- `types/predicate->tag` maps a core predicate to the type it proves. A value
  may be a set, for example `ident?` proves `#{:keyword :symbol}`.
- `narrowing-from-condition` recognizes `(pred local)` and returns `[sym tag]`.
  The predicate is resolved with `namespace/core-symbol-in-scope?`, so a
  shadowed, redefined, referred or excluded predicate does not narrow.
- `narrow-binding` stores the narrowed tag in the binding's metadata. Binding
  equality ignores metadata, so unused-binding detection is unaffected.
- `expr->tag` reads the narrowed tag before the declared tag.

A set-valued tag needs no new checking code. `match?` already has a set branch
that passes when any member could satisfy the expected type, so a union narrows
leniently.

## Or narrowing (unmerged, branch `flow-narrow-or`)

`(or (pred x) (pred x) ..)` on one local narrows `x` to the union of the
predicates' types, reusing the set-valued tag path. It is correct and free of
false positives, but it found no findings on the regression corpora (metabase,
clerk, clj-kondo deps), so its value is unproven. Kept on branch
`flow-narrow-or` rather than merged.

## Negative narrowing (deferred)

The else branch is the dual problem. There the guard proves the value is NOT the
predicate's type:

    (defn foo [x]
      (if (string? x)
        (inc x)          ;; then: x is a string, already flagged
        (parse-long x))) ;; else: x is not a string, parse-long wants a string

To flag the else branch:

1. Record `x` as excluded-from the predicate's type in the else branch, a
   `{:not #{:string}}` tag, the mirror of `narrow-binding`.
2. Add a `match?` rule: `{:not excluded}` fails a required type `T` only when
   `T` is entirely inside the excluded set, so every value satisfying `T` would
   be excluded. `:string` required against `not #{:string}` is a guaranteed
   failure. `:seqable` required against `not #{:string}` stays quiet, because a
   vector is seqable and not a string.

This is a new tag shape and a new `match?` branch. It is more false-positive
prone than positive narrowing, because a broad predicate produces a broad
exclusion, so it needs its own increment and tests.

`parse-long`, `parse-double` and `parse-uuid` have no specs (placeholders at
core.clj lines 8044-8068). The example above only fires once they carry a
`:string` argument spec. That spec is useful on its own, independent of
narrowing.

## Limitations

- `if` and `when` only. `cond` and `and` are not handled. `or` is on branch
  `flow-narrow-or`, unmerged.
- Then-branch (positive) only. Else-branch narrowing is the deferred work above.
- Predicates are resolved by name against the core set, so a qualified core
  predicate like `(clojure.core/string? x)` does not narrow.
