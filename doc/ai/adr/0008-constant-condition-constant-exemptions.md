# ADR 0008: Which constants `:constant-condition` exempts

## Status

Accepted, on the `constant-condition-nil-literal` branch, follow-up to #721.

## Context

`:constant-condition` warns when a condition decides the same branch on every
run. Some constants in condition position are intentional and must not warn.
The linter shipped exempting literal `true` and `false` (also through a var or
local) and the keyword `:always`. This ADR records why `nil` and every keyword
test in `cond->`/`cond->>` were added, so the boundary is not relitigated.

A survey of 4736 `cond->`/`cond->>` forms across the local `~/dev` corpus, with
balanced-paren extraction and a tokenizer to find the true test position, found
the following constants in test position:

    true      204   :always 54   :else 2   :default/:hack/:otherwise/:then/:finally/:alwyas  1 each

Every keyword found was an intentional "always run this step" marker. Zero were
bugs. The bug the narrow rule would catch, a bare keyword meaning a map lookup
such as `(cond-> m :some-flag (assoc ...))` for `(:some-flag m)`, did not occur
once and is not a natural slip: a lookup is written `(:some-flag m)`, never bare.

## Decision

### 1. A literal `nil` condition is exempt, like `false`

    (when nil 1)                    ;; silent
    (if nil 1 2)                    ;; silent
    (let [flag nil] (when flag 1))  ;; silent, nil-bound local
    (when (:k {}) 1)                ;; still warns, provably nil but not a literal

`nil` is the always-false mirror of the `false` dev toggle. `false` never warned
because a boolean literal is typed `:boolean`, which carries no verdict. `nil` is
typed `:nil`, which does, so it needed an explicit exemption.

A literal `nil` is detected by `utils/nil-token?`. A local bound to a literal
`nil` carries a `:nil-literal` meta marker set in `extract-bindings`, checked by
`analyzer/nil-literal-condition?`. A key lookup that is provably nil has the
same `:nil` tag but no marker, so it still warns: the exemption is for the
author writing `nil` on purpose, not for a value that inference proved nil.

### 2. Any keyword test in `cond->`/`cond->>` is exempt, not just `:always`

    (cond-> m :always (assoc :a 1))   ;; silent
    (cond-> m :else   (assoc :a 1))   ;; silent
    (cond-> m :hack   (assoc :a 1))   ;; silent
    (cond-> m inc     (assoc :a 1))   ;; still warns, a symbol is not a marker
    (cond-> m "s"     (assoc :a 1))   ;; still warns, only keywords
    (when :always 1)                  ;; still warns, cond-> only

`cond->` has no canonical marker the way `cond` has `:else`. `:always` is a
habit, not a rule. There is nothing to nudge authors toward, so any keyword is
treated as the intentional marker. Rarity of the other keywords argues for
exemption, not against: every rare keyword found was deliberate, so narrowing to
`:always` would only produce false positives on `:else`, `:hack` and friends.

The exemption is scoped to `cond->`/`cond->>` only. `expand-cond->` stamps the
user's test node with `:cond-arrow-test`, and `analyze-condition` exempts a
keyword test only when that marker is present. This replaces the old unscoped
`(not= :always (:k condition))`, which exempted `:always` in every condition
position. `cond->`/`cond->>` are the only threading macros with a test position,
so no other form needs the marker.

## Consequences

- `(cond-> m :true ...)` no longer warns. It was previously singled out as a
  warning because only `:always` was blessed. `:true` and `true` are identical
  in test position, so this is consistent.
- `(when :always 1)` now warns. `:always` only means "always run" inside
  `cond->`. Elsewhere it is an arbitrary truthy keyword.
- Symbols are never exempt. An always-true symbol such as `(if odd? :a :b)` is
  the linter's primary target, and a `def` toggle is already safe because its
  tag misses `set!` and `alter-var-root`, see `mask-var-usages`.
- Plain `cond` is unaffected. A non-`:else` catch-all still gets "use :else as
  the catch-all test expression", a separate style rule.
- `(assert false "msg")` was checked and does not warn: assert's condition is
  not routed through `:constant-condition`.
