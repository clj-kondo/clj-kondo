# ADR 0009: Scope of a `:clj-kondo/lang` form

## Status

Accepted, for #3062.

## Context

`^{:clj-kondo/lang :cljs}` on a form makes clj-kondo lint that form as CLJS,
also in a `.clj` file and in hook output. Hyper's `defc` and buzz's `host`
macro need it: their bodies run in the browser but live in `.clj` files.

Electric Clojure needs both directions in `.cljc` files. An `e/defn` body is
compiled by Clojure and by ClojureScript, `e/client` code runs only in the
browser and `e/server` code only on the JVM. Electric's own source calls
`js/document` in `e/client` and JVM classes in `e/server` without reader
conditionals, and requires per-platform namespaces with `#?(:clj ...)` and
`#?(:cljs ...)`. Without a marker, the clj pass reports the `js/` calls and the
cljs pass reports the JVM calls.

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
language. Analysis sets `:ns-lang` next to `:lang` for each file and pass.
`utils/ns-lang` returns `:ns-lang`, or `:lang` if absent, and every read and
write of a namespace entry uses it: vars, var
usages, aliases, refers, imports, bindings and unresolved symbols. A var
usage made in the form carries `:ns-lang` if it differs from `:lang`, so its
unresolved symbol lands in the same entry at lint time.

`:lang` drives what is language specific: core var resolution and arity,
`js/` symbols, and `:qualify-ns` for `cljs.core`, which `resolve-name` and
`enter-lang` merge into the outer namespace's.

In a `.cljc` file, only the pass of the marked language reports findings in
the form. The other pass analyzes it with all linters disabled by
`utils/ctx-with-linters-disabled` and marks its bindings used, so its usages
still count. A nested form of the pass's own language gets the outer config
back. A finding in a marked form is not merged across passes, so
`redundant-let` and the other linters that require both passes to agree still
report it.

`lang-metadata-test` and `lang-metadata-cljc-test` check each case.

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

| Inside the marked form, in a `.cljc` file | Finding |
|---|---|
| an alias required under `#?(:cljs ...)`, used in a `:cljs` form | none |
| an alias required under `#?(:clj ...)`, used in a `:clj` form | none |
| `#?(:cljs ... :clj ...)` | the branch of the marked language only |
| an arity error, unresolved symbol or unused binding | once, for the marked language |
| a redundant let | once, for the marked language |
| a used ignore | none |
| a `:clj` form nested in a `:cljs` form | linted as Clojure, with the outer locals |
| a marked form in one pass only | none |

## Electric

An `:analyze-call` hook marks the bodies:

```clojure
(defn- site [lang {:keys [node]}]
  {:node (vary-meta (api/list-node (list* (api/token-node 'do) (rest (:children node))))
                    assoc :clj-kondo/lang lang)})

(def client (partial site :cljs))
(def server (partial site :clj))
```

registered for `hyperfiddle.electric3/client` and `hyperfiddle.electric3/server`,
next to Electric's exported config.

Electric `src` and `src-dev` (bf40b50), master and branch with this hook,
findings compared without their language labels:

| | errors | warnings |
|---|---|---|
| master | 60 | 380 |
| branch | 43 | 373 |

The branch drops 27 false positives: `js/` and cljs-only vars in `e/client`
in the clj pass, and `ts/->node` in `e/server` with
`#?(:clj [dustingetz.triple-store :as ts])` in the cljs pass. It loses no
other finding. Three findings appear at a later position: `js/Object.`
outside `e/client` in `electric_forms5.cljc`, and the unresolved `with-cycle`
locals `p` and `c` of the clj pass in `electric3_contrib.cljc`. clj-kondo
reports these once per namespace and language, and their first occurrence is
now inside a form that the pass does not report.

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

A marked form in a `.cljc` file is linted with the reader conditionals and
the `ns` requires of the pass of its language. A marked form that exists in
one pass only, e.g. `#?(:clj ^{:clj-kondo/lang :cljs} (fn [] (inc 1 2)))`,
reports nothing.

An `ns` or `in-ns` form inside a marked form registers its namespace under
the marked language, as before. This case is not supported.
