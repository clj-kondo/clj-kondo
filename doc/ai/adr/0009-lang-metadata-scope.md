# ADR 0009: Scope of a `:clj-kondo/lang` form

## Status

Accepted, for #3062.

## Context

`^{:clj-kondo/lang :cljs}` on a form makes clj-kondo lint that form as CLJS,
also in a `.clj` file and in hook output. Hyper's `defc` and buzz's `host`
macro need it: their bodies run in the browser but live in `.clj` files.

clj-kondo must not report false positives. The marked form shares the lexical
scope and the namespace of the code around it, so anything that crosses the
boundary must count as it would without the marker.

Namespace state is keyed by `[base-lang lang ns-name]`, because a `.cljc`
file is analyzed once per language. A marked form is a region inside one
pass, not a pass of its own. A first version gave the form a separate,
empty cljs entry, which reported its aliases, refers and private vars as
unresolved or unused.

## Comparable constructs

| Model | Examples | Host locals in the embedded code |
|---|---|---|
| Separate virtual file | ESLint markdown processor, doctests | not visible |
| One pass per configuration | Rust `#[cfg]`, Haxe `#if`, Nim `when`, `.cljc` | unused on the other target |
| Shared lexical scope | Vue SFC templates, Svelte, Template Haskell antiquotes, inline `asm` operands, CLJS `js*` placeholders | a use counts |
| Binding tagged with its phase | Racket `for-syntax` | a cross-phase reference is an error |
| Syntax injection only | IntelliJ `@Language`, tree-sitter injections | not analyzed |

The shared lexical scope model is the only one without false positives for
`host` and `defc`.

## Decision

A marked form keeps the namespace entry of the code around it. `enter-lang`
sets `:ns-lang` to the language outside the form and `:lang` to the marked
language. `utils/ns-lang` returns `:ns-lang`, or `:lang` outside a marked
form, and every read and write of a namespace entry uses it: vars, var
usages, aliases, refers, imports, bindings and unresolved symbols. A var
usage made in the form carries `:ns-lang`, so its unresolved symbol lands in
the same entry at lint time.

`:lang` drives what is language specific: core var resolution and arity,
`js/` symbols, and `:qualify-ns` for `cljs.core`, which `resolve-name` and
`enter-lang` merge into the outer namespace's.

`lang-metadata-test` checks each case.

| Inside the marked form | Finding |
|---|---|
| a call to a var defined in the same namespace | none, arity is checked |
| a call through a `:require` alias | none |
| a call to a referred var | none |
| an alias or refer used only in the form | none |
| an alias used nowhere | unused namespace |
| a private var used only in the form | none |
| the alias case in a `.cljc` file | none |
| a `cljs.core/` call | none |
| a local bound outside, used only in the form | none |
| a local bound in the form and never used | unused binding |
| `:clj` in a `.cljs` file or in the cljs pass of a `.cljc` file | linted as Clojure |
| a value other than `:clj` or `:cljs` | ignored |

## Measurement

metabase src, JVM, in-process, min of 7 runs, master (a38978c2) and branch
interleaved (benchmarks/bench.clj):

| | round 1 | round 2 | allocation |
|---|---|---|---|
| master | 2652ms | 2651ms | 4182-4185MB |
| branch | 2744ms | 2656ms | 4182-4189MB |

Within noise. The marker check is one meta lookup per node, and allocation
does not change.

## Consequences

A reader conditional inside a marked form in a `.cljc` file is read per pass
as usual, and each branch the reader picks is linted as the marked language.
In the clj pass of `(fn [] #?(:clj (Thread/sleep 1)))` marked `:cljs`, the
`:clj` branch is linted as CLJS and reports `Thread` as an unresolved
namespace. A macro body in a `.cljc` file is read on the JVM, so that branch
is what the macro emits.

An `ns` or `in-ns` form inside a marked form registers its namespace under
the marked language, as before. This case is not supported.
