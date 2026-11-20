# ADR 0009: Scope of a `:clj-kondo/lang` form

## Status

Proposed, on the `hook-lang` branch, for #3062.

## Context

`^{:clj-kondo/lang :cljs}` on a form makes clj-kondo lint that form as CLJS,
also in a `.clj` file and in hook output. Hyper's `defc` and buzz's `host`
macro need it: their bodies run in the browser but live in `.clj` files.

clj-kondo must not report false positives. The marked form shares the lexical
scope and the namespace of the code around it, so anything that crosses the
boundary must count as it would without the marker.

`enter-lang` switches `:lang` and `:ns` to a separate namespace entry at
`[base-lang lang ns-name]`. A `.cljc` file is analyzed once per language,
which is why namespace state is keyed by language. A marked form is a region
inside one pass, not a pass of its own.

## Comparable constructs

From memory, not checked against current docs. (?) marks the less certain
ones.

| Model | Examples | Host locals in the embedded code |
|---|---|---|
| Separate virtual file | ESLint markdown processor, doctests | not visible |
| One pass per configuration | Rust `#[cfg]`, Haxe `#if`, Nim `when`, `.cljc` | unused on the other target |
| Shared lexical scope | Vue SFC templates, Svelte, Kotlin/JS `js()` (?), Template Haskell antiquotes, inline `asm` operands, CLJS `js*` placeholders | a use counts |
| Binding tagged with its phase | Racket `for-syntax` | a cross-phase reference is an error |
| Syntax injection only | IntelliJ `@Language`, tree-sitter injections | not analyzed |

The shared lexical scope model is the only one without false positives for
`host` and `defc`.

## Decision

Locals follow the shared lexical scope model. `enter-lang` sets
`:bindings-lang` to the language of the enclosing pass, and
`reg-binding!`, `reg-destructuring-default!` and `reg-used-binding!` write to
that language's namespace entry. A local bound outside the form and used only
inside it counts as used. A local bound inside the form and never used is
reported.

## Open problem

The rest of the namespace state still goes to the separate entry, which
starts empty. These cases are expected to report false positives. None of
them has a test yet.

| Inside the marked form | Expected finding |
|---|---|
| a call to a var defined in the same namespace | unresolved symbol |
| a call through a `:require` alias | unresolved namespace |
| an alias or refer used only in the form | unused namespace or referred var |
| a private var used only in the form | unused private var |
| a squint core fn missing from `cljs.core` | unresolved symbol |

Proposed direction: keep the outer `:ns` and its entry, and switch only the
language specific parts: `:qualify-ns` to `cljs.core`, core var resolution
and arity, and `js/` symbols. `:bindings-lang` is then not needed. The squint
case needs a dialect value such as `:clj-kondo/lang :squint`, or no
unresolved-symbol for core names in the form.

Next step: a regression test per row in `hooks_test.clj`, then the change.
Also test a marked `:cljs` form in a `.cljc` file, where the clj pass writes
to the entry the cljs pass also uses.
