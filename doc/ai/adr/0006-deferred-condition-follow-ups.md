# ADR 0006: Resolve conditions hidden by folds and spine evidence

## Status

Proposed, follow-up to #721, not in the `:constant-condition` branch.

## Context

`:constant-condition` judges a condition twice: inline while analyzing, and in
`lint-deferred-conditions!` once every namespace is analyzed. Deferral only
helps when the condition's tag still contains an unresolved marker. Two classes
of constant conditions lose their marker before the deferred pass runs. Both
fail silent.

### 1. Unresolved calls in non-final and/or operands

`fold-logic` runs while analyzing and reduces a non-final operand to its
truthiness part. An unresolved `{:call ..}` marker becomes `:truthy` or
`#{:nil :false}` and cannot be resolved later. Only the final operand is
unioned whole.

    (defn f [] nil)
    (when (or x (f)) 1)   ;; warns, marker survives in final position
    (when (or (f) x) 1)   ;; silent, marker folded away

### 2. Param conditions with spine evidence

Param usage evidence is spine-only: a usage under a conditional contributes
nothing, so the inferred `:args` of a fn is sound for every run that does not
throw on the unconditional top level of the body. The condition check never
sees it: a param binding has no tag while analyzing.

    (defn g [x] (inc x) (when x 1))   ;; silent, x must be a number

Evidence inside a guard must never testify about the guard itself:

    (defn g [x] (when x (inc x)))     ;; must stay silent, guard is the point

## Decision

Deferred, in dependency order:

1. When a fold operand is unresolved, return a symbolic
   `{:fold {:op or :args [..]}}` node instead of folding. Replay `fold-logic`
   in `lint-deferred-conditions!` after `resolve-arg-type` resolves the
   operands.
2. Tag param bindings with a `{:param ..}` marker. Defer like a call marker,
   resolve against the fn's inferred `:args`. The verdict standard matches the
   existing call-site checks. Params inside and/or also need 1.

## Consequences

- An arg-types entry is `{:tag X :row ..}`: markers appear only under `:tag`,
  never at the entry's top level (normalized on the #721 branch). New marker
  kinds from 1 and 2 are tag-level values, entry consumers stay untouched.
- Symbolic fold nodes flow into cached return types, so every consumer of
  `resolve-arg-type` must handle or resolve them, not only the condition pass.
- Verdicts from spine evidence claim "always true in every run that survives
  the spine", the same claim call-site arg checks already make.
- Var usages stay excluded either way: a def tag misses `set!` and
  `alter-var-root`, see `mask-var-usages`.
