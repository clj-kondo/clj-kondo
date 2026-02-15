# Changelog

For a list of breaking changes, check [here](#breaking-changes).

[Clj-kondo](https://github.com/clj-kondo/clj-kondo): static analyzer and linter for Clojure code that sparks joy ✨

<!-- Dev checklist: -->

<!-- - [ ] fill in empty doc links in changelogs -->
<!-- - [ ] script/bump_version release -->
<!-- - [ ] clj-kondo lsp: bb publish -->
<!-- - [ ] github release -->
<!-- - [ ] homebrew: ./update-clj-kondo -->
<!-- - [ ] clj-kondo pod -->
<!-- - [ ] script/bump_version post-release -->
<!-- - [ ] update clj-kondo-bb -->
<!-- - [ ] update lein-clj-kondo -->
<!-- - [ ] update carve -->

## Unreleased

- [#2768](https://github.com/clj-kondo/clj-kondo/issues/2768): NEW linter: `:redundant-declare` which warns when `declare` is used after a var is already defined ([@jramosg](https://github.com/jramosg))
- Performance improvement: refactor `lint-cond-constants!` to eliminate `sexpr` usage ([@jramosg](https://github.com/jramosg))
- [#2762](https://github.com/clj-kondo/clj-kondo/issues/2762): Fix false positive: `throw` with string in CLJS no longer warns about type mismatch ([@jramosg](https://github.com/jramosg))
- Add type support for `pmap` ([@jramosg](https://github.com/jramosg))
- Type system: Add type support for future-related functions (`future`, `future-call`, `future-done?`, `future-cancel`, `future-cancelled?`) ([@jramosg](https://github.com/jramosg))
- [#2773](https://github.com/clj-kondo/clj-kondo/issues/2773): Align executable path for images to be `/bin/clj-kondo` ([@harryzcy](https://github.com/harryzcy))

## 2026.01.19

- [#2735](https://github.com/clj-kondo/clj-kondo/issues/2735): NEW linter: `:duplicate-refer` which warns on duplicate entries in `:refer` of `:require`. ([@jramosg](https://github.com/jramosg))
- [#2734](https://github.com/clj-kondo/clj-kondo/issues/2734): NEW linter: `:aliased-referred-var`, which warns when a var is both referred and accessed via an alias in the same namespace. ([@jramosg](https://github.com/jramosg))
- [#2745](https://github.com/clj-kondo/clj-kondo/issues/2745): NEW linter: `:is-message-not-string` which warns when `clojure.test/is` receives a non-string message argument. This linter replaces the previous type-mismatch enforcement for `is` message arguments and can be disabled to allow non-string values. ([@jramosg](https://github.com/jramosg))
- [#2756](https://github.com/clj-kondo/clj-kondo/issues/2756): Fix: ensure `def + defmethod` triggers `:def-fn` warning with valid source location ([@jramosg](https://github.com/jramosg))
- `unused-excluded-var`: Add location metadata to excluded vars in `ns-unmap`. This fixes some findings with not location. ([@jramosg](https://github.com/jramosg))
- [#2747](https://github.com/clj-kondo/clj-kondo/issues/2747): Fix: Gensym bindings in nested syntax quotes are now correctly recognized ([@jramosg](https://github.com/jramosg))when throwing non-throwable values ([@jramosg](https://github.com/jramosg))
- [#2746](https://github.com/clj-kondo/clj-kondo/issues/2746): Fix regression: primitive array class syntax (e.g., `byte/1`, `int/2`) now correctly recognized as class literals in type checking ([@jramosg](https://github.com/jramosg))
- [#2739](https://github.com/clj-kondo/clj-kondo/issues/2739): Extend `:equals-expected-position` linter to also warn for `not=` when expected value is not first ([@jramosg](https://github.com/jramosg)) 
- [#2749](https://github.com/clj-kondo/clj-kondo/issues/2749): Fix false positive for throw in CLJS when throwing non-throwable values ([@jramosg](https://github.com/jramosg))
- [#2739](https://github.com/clj-kondo/clj-kondo/issues/2739): Extend `:equals-expected-position` linter to also warn for `not=` when expected value is not first ([@jramosg](https://github.com/jramosg))
- [#2732](https://github.com/clj-kondo/clj-kondo/issues/2732): `unreachable-code`: warn when `:default` does not come last in reader conditionals ([@jramosg](https://github.com/jramosg))
- Fix `str/replace` false positive and tighten comp ret type
- [#2729](https://github.com/clj-kondo/clj-kondo/issues/2729): Check for arity mismatch for bound vectors, sets & maps, not just literals ([@tomdl89](https://github.com/tomdl89))
- Add new type `inst` and type checking support for `inst-ms` and `inst-ms*` ([@jramosg](https://github.com/jramosg))

## 2026.01.12

- [#2712](https://github.com/clj-kondo/clj-kondo/issues/2712): NEW linter: `:redundant-format` to warn when format strings contain no format specifiers ([@jramosg](https://github.com/jramosg))
- [#2709](https://github.com/clj-kondo/clj-kondo/issues/2709): NEW linter: `:redundant-primitive-coercion` to warn when primitive coercion functions are applied to expressions already of that type ([@hugod](https://github.com/hugod))
- [#2600](https://github.com/clj-kondo/clj-kondo/issues/2600): NEW linter: `unused-excluded-var` to warn on unused vars in `:refer-clojure :exclude` ([@jramosg](https://github.com/jramosg))
- [#2459](https://github.com/clj-kondo/clj-kondo/issues/2459): NEW linter: `:destructured-or-always-evaluates` to warn on s-expressions in `:or` defaults in map destructuring ([@jramosg](https://github.com/jramosg))
- Change second arg for assoc-in, get-in and update-in to be sequential, not just seqable ([@tomdl89](https://github.com/tomdl89))
- `:condition-always-true` finding was adding full context into findigns ([@jramosg](https://github.com/jramosg))
- [#2340](https://github.com/clj-kondo/clj-kondo/issues/2340): Extend `:condition-always-true` linter to check first argument of `clojure.test/is` ([@jramosg](https://github.com/jramosg))
- [#2719](https://github.com/clj-kondo/clj-kondo/issues/2719): Fix: `comp` args are now properly type checked ([@tomdl89](https://github.com/tomdl89))
- [#2692](https://github.com/clj-kondo/clj-kondo/issues/2692): Lint quoted forms which are not functions ([@tomdl89](https://github.com/tomdl89))
- [#2691](https://github.com/clj-kondo/clj-kondo/issues/2691): Fix: `:refer-clojure :exclude` now properly ignores elements with `#_` (e.g. `#_:clj-kondo/ignore comp2`) [@jramosg](https://github.com/jramosg).
- [#2713](https://github.com/clj-kondo/clj-kondo/issues/2713): Fix regression: getting unused binding warning in `~'~` unquote expressions ([@jramosg](https://github.com/jramosg))
- [#2711](https://github.com/clj-kondo/clj-kondo/issues/2711): Unused value inside `defmethod` ([@jramosg](https://github.com/jramosg))
- Add type checking support for `sorted-map-by`, `sorted-set`, and `sorted-set-by` functions ([@jramosg](https://github.com/jramosg))
- Add new type `array` and type checking support for the next functions: `to-array`, `alength`, `aget`, `aset` and `aclone` ([@jramosg](https://github.com/jramosg))
- Add new type `class` and type checking support for class-related functions: `instance?`, `cast`, `class`, `make-array`, `bases` and `supers` ([@jramosg](https://github.com/jramosg))
- Add type checking support for clojure.test functions and macros ([@jramosg](https://github.com/jramosg))
- Fix [#2695](https://github.com/clj-kondo/clj-kondo/issues/2696): false positive `:unquote-not-syntax-quoted` in leiningen's `defproject`
- Leiningen's `defproject` behavior can now be configured using `leiningen.core.project/defproject`
- Fix [#2699](https://github.com/clj-kondo/clj-kondo/issues/2699): fix false positive unresolved string var with extend-type on CLJS
- Rename `:refer-clojure-exclude-unresolved-var` linter to `unresolved-excluded-var` for consistency
- Upgrade to GraalVM 25

## 2025.12.23

- [#2654](https://github.com/clj-kondo/clj-kondo/issues/2654): NEW linter: `redundant-let-binding`, defaults to `:off` ([@tomdl89](https://github.com/tomdl89))
- [#2653](https://github.com/clj-kondo/clj-kondo/issues/2653): NEW linter: `:unquote-not-syntax-quoted` to warn on `~` and `~@` usage outside syntax-quote (`` ` ``) ([@jramosg](https://github.com/jramosg))
- [#2613](https://github.com/clj-kondo/clj-kondo/issues/2613): NEW linter: `:refer-clojure-exclude-unresolved-var` to warn on non-existing vars in `:refer-clojure :exclude` ([@jramosg](https://github.com/jramosg))
- [#2668](https://github.com/clj-kondo/clj-kondo/issues/2668): Lint `&` syntax errors in let bindings and lint for trailing `&` ([@tomdl89](https://github.com/tomdl89))
- [#2590](https://github.com/clj-kondo/clj-kondo/issues/2590): `duplicate-key-in-assoc` changed to `duplicate-key-args`, and now lints `dissoc`, `assoc!` and `dissoc!` too ([@tomdl89](https://github.com/tomdl89))
- [#2651](https://github.com/clj-kondo/clj-kondo/issues/2651): resume linting after paren mismatches
- [clojure-lsp#2651](https://github.com/clojure-lsp/clojure-lsp/issues/2157): Fix inner class name for java-class-definitions.
- [clojure-lsp#2651](https://github.com/clojure-lsp/clojure-lsp/issues/2157): Include inner class java-class-definition analysis.
- Bump `babashka/fs`
- [#2532](https://github.com/clj-kondo/clj-kondo/issues/2532): Disable `:duplicate-require` in `require` + `:reload` / `:reload-all`
- [#2432](https://github.com/clj-kondo/clj-kondo/issues/2432): Don't warn for `:redundant-fn-wrapper` in case of inlined function
- [#2599](https://github.com/clj-kondo/clj-kondo/issues/2599): detect invalid arity for invoking collection as higher order function
- [#2661](https://github.com/clj-kondo/clj-kondo/issues/2661): Fix false positive `:unexpected-recur` when `recur` is used inside `clojure.core.match/match` ([@jramosg](https://github.com/jramosg))
- [#2617](https://github.com/clj-kondo/clj-kondo/issues/2617): Add types for `repeatedly` ([@jramosg](https://github.com/jramosg))
- Add `:ratio` type support for `numerator` and `denominator` functions ([@jramosg](https://github.com/jramosg))
- [#2676](https://github.com/clj-kondo/clj-kondo/issues/2676): Report unresolved namespace for namespaced maps with unknown aliases ([@jramosg](https://github.com/jramosg))
- [#2683](https://github.com/clj-kondo/clj-kondo/issues/2683): data argument of `ex-info` may be nil since clojure 1.12
- Bump built-in ClojureScript analysis info
- Fix [#2687](https://github.com/clj-kondo/clj-kondo/issues/2687): support new `:refer-global` and `:require-global` ns options in CLJS
- Fix [#2554](https://github.com/clj-kondo/clj-kondo/issues/2544): support inline configs in `.cljc` files

## 2025.10.23

- [#2590](https://github.com/clj-kondo/clj-kondo/issues/2590): NEW linter: `duplicate-key-in-assoc`, defaults to `:warning`
- [#2639](https://github.com/clj-kondo/clj-kondo/issues/2639): NEW `:equals-nil` linter to detect `(= nil x)` or `(= x nil)` patterns and suggest `(nil? x)` instead ([@conao3](https://github.com/conao3))
- [#2633](https://github.com/clj-kondo/clj-kondo/issues/2633): support new `defparkingop` macro in core.async alpha
- [#2635](https://github.com/clj-kondo/clj-kondo/pull/2635): Add `:interface` flag to `:flags` set in `:java-class-definitions` analysis output to distinguish Java interfaces from classes ([@hugoduncan](https://github.com/hugoduncan))
- [#2636](https://github.com/clj-kondo/clj-kondo/issues/2636): set global SCI context so hooks can use `requiring-resolve` etc.
- [#2641](https://github.com/clj-kondo/clj-kondo/issues/2641): fix linting of `def` body, no results due to laziness bug
- [#1743](https://github.com/clj-kondo/clj-kondo/issues/1743): change `:not-empty?` to only warn on objects that are already seqs
- Performance optimization for `:ns-groups` (thanks [@severeoverfl0w](https://github.com/severeoverfl0w))
- Flip `:self-requiring-namespace` level from `:off` to `:warning`

## 2025.09.22

- Remove `dbg` from `data_readers.clj` since this breaks when using together with CIDER

## 2025.09.19

- [#1894](https://github.com/clj-kondo/clj-kondo/issues/1894): support `destruct` syntax
- [#2624](https://github.com/clj-kondo/clj-kondo/issues/2624): lint argument types passed to `get` and `get-in` (especially to catch swapped arguments to get in threading macros) ([@borkdude](https://github.com/borkdude), [@Uthar](https://github.com/Uthar))
- [#2564](https://github.com/clj-kondo/clj-kondo/issues/2564): detect calling set with wrong number of arguments
- [#2603](https://github.com/clj-kondo/clj-kondo/issues/2603): warn on `:inline-def` with nested `deftest`
- [#2588](https://github.com/clj-kondo/clj-kondo/issues/2588): false positive type mismatch about `symbol` accepting var
- [#2615](https://github.com/clj-kondo/clj-kondo/issues/2615): emit `inline-configs` `config.edn` in a git-diff-friendly way ([@lread](https://github.com/lread))
- Require clojure `1.10.3` is the minimum clojure version
- [#2257](https://github.com/clj-kondo/clj-kondo/issues/2257): support ignore hint on invalid symbol
- Sort findings on filename, row, column and now additionally on message too
- [#2602](https://github.com/clj-kondo/clj-kondo/issues/2602): Sort auto-imported configs to avoid differences based on OS or file system
- [#2606](https://github.com/clj-kondo/clj-kondo/issues/2606): make it easy for users to know how inline-config files should be version controlled ([@lread](https://github.com/lread))
- [#2610](https://github.com/clj-kondo/clj-kondo/issues/2610): ignores may show up unordered due to macros
- [#2614](https://github.com/clj-kondo/clj-kondo/issues/2614): aliased-namespace-symbol doesn't work for CLJS call
- Regressions tests for specific open source codebases such as metabase, clerk, ...

## 2025.07.28

- [#2580](https://github.com/clj-kondo/clj-kondo/issues/2580): false positive type mismatch with quoted value
- Fix some `:locking-suspicious-lock` false positives
- [#2582](https://github.com/clj-kondo/clj-kondo/issues/2582): `:condition-always-true` false positives

## 2025.07.26

- [#2560](https://github.com/clj-kondo/clj-kondo/issues/2560): NEW linter: `:locking-suspicious-lock`: report when locking is used on a single arg, interned value or local object
- [#2519](https://github.com/clj-kondo/clj-kondo/issues/2519): NEW linter: `:unresolved-protocol-method`. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md) ([@emerson-matos](https://github.com/emerson-matos))
- [#2555](https://github.com/clj-kondo/clj-kondo/issues/2555): false positive with `clojure.string/replace` and `partial` as replacement fn
- [#2566](https://github.com/clj-kondo/clj-kondo/issues/2566): Expand `:condition-always-true` check. ([@NoahTheDuke](https://github.com/NoahTheDuke))
- [#2350](https://github.com/clj-kondo/clj-kondo/issues/2350): support `schema.core/defprotocol` ([@emerson-matos](https://github.com/emerson-matos))
- [#2571](https://github.com/clj-kondo/clj-kondo/issues/2571): false positive unresolved symbol when ignoring expression that goes through macroexpansion hook
- [#2575](https://github.com/clj-kondo/clj-kondo/issues/2575): false positive type mismatch with nested keyword call and `str`
- Bump SCI to `0.10.47`
- Drop memoization for hook fns and configuration, solves memory issue with Cursive + big projects like metabase
- Optimizations to compensate for dropping caching, performance should be similar (or better depending on the size of your project)
- [#2568](https://github.com/clj-kondo/clj-kondo/issues/2568): support `:deprecated-namespace` for `.cljc` namespaces

## 2025.06.05

- [#2541](https://github.com/clj-kondo/clj-kondo/issues/2541): NEW linter: `:discouraged-java-method`. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md)
- [#2522](https://github.com/clj-kondo/clj-kondo/issues/2522): support `:config-in-ns` on `:missing-protocol-method`
- [#2524](https://github.com/clj-kondo/clj-kondo/issues/2524): support `:redundant-ignore` on `:missing-protocol-method`
- [#2536](https://github.com/clj-kondo/clj-kondo/issues/2536): false positive with `format` and whitespace flag after percent
- [#2535](https://github.com/clj-kondo/clj-kondo/issues/2535): false positive `:missing-protocol-method` when using alias in method
- [#2534](https://github.com/clj-kondo/clj-kondo/issues/2534): make `:redundant-ignore` aware of `.cljc`
- [#2527](https://github.com/clj-kondo/clj-kondo/issues/2527): add test for using ns-group + config-in-ns for `:missing-protocol-method` linter
- [#2218](https://github.com/clj-kondo/clj-kondo/issues/2218): use `ReentrantLock` to coordinate writes to cache directory within same process
- [#2533](https://github.com/clj-kondo/clj-kondo/issues/2533): report inline def under fn and defmethod
- [#2521](https://github.com/clj-kondo/clj-kondo/issues/2521): support `:langs` option in `:discouraged-var` to narrow to specific language
- [#2529](https://github.com/clj-kondo/clj-kondo/issues/2529): add `:ns` to `&env` in `:macroexpand-hook` macros when executing in CLJS
- [#2547](https://github.com/clj-kondo/clj-kondo/issues/2547): make redundant-fn-wrapper report only for all cljc branches
- [#2531](https://github.com/clj-kondo/clj-kondo/issues/2531): add `:name` data to `:unresolved-namespace` finding for clojure-lsp

## 2025.04.07

- [#1292](https://github.com/clj-kondo/clj-kondo/issues/1292): NEW linter: `:missing-protocol-method`. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md)
- [#2512](https://github.com/clj-kondo/clj-kondo/issues/2512): support vars ending with `.`, e.g. `py.` according to clojure analyzer
- [#2516](https://github.com/clj-kondo/clj-kondo/issues/2516): add new `--repro` flag to ignore home configuration
- [#2493](https://github.com/clj-kondo/clj-kondo/issues/2493): reduce image size of native image
- [#2496](https://github.com/clj-kondo/clj-kondo/issues/2496): Malformed `deftype` form results in `NPE`
- [#2499](https://github.com/clj-kondo/clj-kondo/issues/2499): Fix `(alias)` bug ([@Noahtheduke](https://github.com/Noahtheduke))
- [#2492](https://github.com/clj-kondo/clj-kondo/issues/2492): Report unsupported escape characters in strings
- [#2502](https://github.com/clj-kondo/clj-kondo/issues/2502): add end locations to invalid symbol
- [#2511](https://github.com/clj-kondo/clj-kondo/issues/2511): fix multiple parse errors caused by incomplete forms
- document var-usages location info edge cases ([@sheluchin](https://github.com/sheluchin))
- Upgrade to GraalVM 24
- Bump datalog parser
- Bump built-in cache

## 2025.02.20

- [#2473](https://github.com/clj-kondo/clj-kondo/issues/2473): New linter: `:unknown-ns-options` will warn on malformed `(ns)` calls. The linter is `{:level :warning}` by default. ([@Noahtheduke](https://github.com/Noahtheduke))
- [#2475](https://github.com/clj-kondo/clj-kondo/issues/2475): add `:do-template` linter to check args & values counts ([@imrekoszo](https://github.com/imrekoszo))
- [#2465](https://github.com/clj-kondo/clj-kondo/issues/2465): fix `:discouraged-var` linter for fixed arities
- [#2277](https://github.com/clj-kondo/clj-kondo/issues/2277): prefer an array class symbol over `(Class/forName ...)` in `defprotocol` and `extend-type`
- [#2466](https://github.com/clj-kondo/clj-kondo/issues/2466): fix false positive with tagged literal in macroexpand hook
- [#2463](https://github.com/clj-kondo/clj-kondo/issues/2463): using `:min-clj-kondo-version` results in incorrect warning ([@imrekoszo](https://github.com/imrekoszo))
- [#2464](https://github.com/clj-kondo/clj-kondo/issues/2464): `:min-clj-kondo-version` warning/error should have a location in `config.edn` ([@imrekoszo](https://github.com/imrekoszo))
- [#2472](https://github.com/clj-kondo/clj-kondo/issues/2472) hooks `api/resolve` should return `nil` for unresolved symbols and locals
- [#2472](https://github.com/clj-kondo/clj-kondo/issues/2472): add `api/env` to determine if symbol is local
- [#2482](https://github.com/clj-kondo/clj-kondo/issues/2482): Upgrade to Oracle GraalVM 23
- [#2483](https://github.com/clj-kondo/clj-kondo/issues/2483): add `api/quote-node` and `api/quote-node?` to hooks API ([@camsaul](https://github.com/camsaul))
- [#2490](https://github.com/clj-kondo/clj-kondo/issues/2490): restore unofficial support for ignore hints via metadata

## 2025.01.16

- [#2457](https://github.com/clj-kondo/clj-kondo/issues/2457): NEW linter: `:equals-float`, warn on comparison of floating point numbers with `=`. This level of this linter is `:off` by default.
- [#2219](https://github.com/clj-kondo/clj-kondo/issues/2219): allow arity config for `:discouraged-var`
- [#2272](https://github.com/clj-kondo/clj-kondo/issues/2451): lint for `nil` return from `if`-like forms
- Add `printf` to vars linted by `analyze-format`. ([@tomdl89](https://github.com/tomdl89))
- [#2272](https://github.com/clj-kondo/clj-kondo/issues/2272): report var usage in `if-let` etc condition as always truthy
- [#2272](https://github.com/clj-kondo/clj-kondo/issues/2272): report var usage in `if-not` condition as always truthy
- [#2433](https://github.com/clj-kondo/clj-kondo/issues/2433): false positive redundant ignore with hook
- Document `:cljc` config option. ([@NoahTheDuke](https://github.com/NoahTheDuke))
- [#2439](https://github.com/clj-kondo/clj-kondo/issues/2439): uneval may apply to nnext form if reader conditional doesn't yield a form ([@NoahTheDuke](https://github.com/NoahTheDuke))
- [#2431](https://github.com/clj-kondo/clj-kondo/issues/2431): only apply redundant-nested-call linter for nested exprs
- Relax `:redundant-nested-call` for `comp`, `concat`, `every-pred` and `some-fn` since it may affect performance
- [#2446](https://github.com/clj-kondo/clj-kondo/issues/2446): false positive `:redundant-ignore`
- [#2448](https://github.com/clj-kondo/clj-kondo/issues/2448): redundant nested call in hook gen'ed code
- [#2424](https://github.com/clj-kondo/clj-kondo/issues/2424): fix combination of `:config-in-ns` and `:discouraged-namespace`

## 2024.11.14

- [#2212](https://github.com/clj-kondo/clj-kondo/issues/2212): NEW linter: `:redundant-nested-call` ([@tomdl89](https://github.com/tomdl89)), set to level `:info` by default
- Bump `:redundant-ignore`, `:redundant-str-call` linters to level `:info`
- [#1784](https://github.com/clj-kondo/clj-kondo/issues/1784): detect `:redundant-do` in `catch`
- [#2410](https://github.com/clj-kondo/clj-kondo/issues/2410): add `--report-level` flag
- [#2416](https://github.com/clj-kondo/clj-kondo/issues/2416): detect empty `require` and `:require` forms ([@NoahTheDuke](https://github.com/NoahTheDuke))
- [#1786](https://github.com/clj-kondo/clj-kondo/issues/1786): Support `gen-interface` (by suppressing unresolved symbols)
- [#2407](https://github.com/clj-kondo/clj-kondo/issues/2407): support ignore hint on called symbol
- [#2420](https://github.com/clj-kondo/clj-kondo/issues/2420): Detect uneven number of clauses in `cond->` and `cond->>` ([@tomdl89](https://github.com/tomdl89))
- [#2415](https://github.com/clj-kondo/clj-kondo/issues/2415): false positive type checking issue with `str/replace` and `^String` annotation

## 2024.09.27

- [#2404](https://github.com/clj-kondo/clj-kondo/issues/2404): fix regression with metadata on node in hook caused by `:redundant-ignore` linter

## 2024.09.26

- [#2366](https://github.com/clj-kondo/clj-kondo/issues/2366): new linter: `:redundant-ignore`. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md)
- [#2386](https://github.com/clj-kondo/clj-kondo/issues/2386): fix regression introduced in [#2364](https://github.com/clj-kondo/clj-kondo/issues/2364) in `letfn`
- [#2389](https://github.com/clj-kondo/clj-kondo/issues/2389): add new `hooks-api/callstack` function
- [#2392](https://github.com/clj-kondo/clj-kondo/issues/2392): don't skip jars that were analyzed with `--skip-lint`
- [#2395](https://github.com/clj-kondo/clj-kondo/issues/2395): enum constant call warnings
- [#2400](https://github.com/clj-kondo/clj-kondo/issues/2400): `deftype` and `defrecord` constructors can be used with `Type/new`
- [#2394](https://github.com/clj-kondo/clj-kondo/issues/2394): add `:sort` option to `:unsorted-required-namespaces` linter to enable case-sensitive sort to match other tools
- [#2384](https://github.com/clj-kondo/clj-kondo/issues/2384): recognize `gen/fmap` var in `cljs.spec.gen.alpha`

## 2024.08.29

- [#2303](https://github.com/clj-kondo/clj-kondo/issues/2303): Support array class notation of Clojure 1.12 (`byte/1`)
- [#916](https://github.com/clj-kondo/clj-kondo/issues/916): New linter: `:destructured-or-binding-of-same-map` which warns about
  `:or` defaults referring to bindings of same map, which is undefined and may result in broken
  behavior
- [#2362](https://github.com/clj-kondo/clj-kondo/issues/2362): turn min-version warning into lint warning
- [#1603](https://github.com/clj-kondo/clj-kondo/issues/1603): Support Java classes in `:analyze-call` hook
- [#2369](https://github.com/clj-kondo/clj-kondo/issues/2369): false positive unused value in quoted list
- [#2374](https://github.com/clj-kondo/clj-kondo/issues/2374): Detect misplaced return Schemas ([@frenchy64](https://github.com/frenchy64))
- [#2364](https://github.com/clj-kondo/clj-kondo/issues/2364): performance: code that analyzed fn arity is ran twice
- [#2355](https://github.com/clj-kondo/clj-kondo/issues/2355): support `:as-alias` with current namespace without warning about self-requiring namespace

## 2024.08.01

- [#2359](https://github.com/clj-kondo/clj-kondo/issues/2359): `@x` should warn with type error about `x` not being an IDeref, e.g. with `@inc`
- [#2345](https://github.com/clj-kondo/clj-kondo/issues/2345): Fix SARIF output and some enhancements ([@nxvipin](https://github.com/nxvipin))
- [#2335](https://github.com/clj-kondo/clj-kondo/issues/2335): read causes side effect, thus not an unused value
- [#2336](https://github.com/clj-kondo/clj-kondo/issues/2336): `do` and `doto` type checking ([@yuhan0](https://github.com/yuhan0))
- [#2322](https://github.com/clj-kondo/clj-kondo/issues/2322): report locations for more reader errors ([@yuhan0](https://github.com/yuhan0))
- [#2342](https://github.com/clj-kondo/clj-kondo/issues/2342): report unused maps, vectors, sets, regexes, functions as `:unused-value`
- [#2352](https://github.com/clj-kondo/clj-kondo/issues/2352): type mismatch error for `or` without arguments
- [#2344](https://github.com/clj-kondo/clj-kondo/issues/2344): copying configs and linting dependencies can now be done in one go with `--dependencies --copy-configs`
- [#2357](https://github.com/clj-kondo/clj-kondo/issues/2357): `:discouraged-namespace` can have `:level` per namespace

## 2024.05.24

- Imports were copied to `.clj-kondo/imports` but weren't pick up correctly. Thanks [@frenchy64](https://github.com/frenchy64) for reporting the bug.
- [#2333](https://github.com/clj-kondo/clj-kondo/issues/2333): Add location to invalid literal syntax errors

## 2024.05.22

- [#2323](https://github.com/clj-kondo/clj-kondo/issues/2323): New linter `:redundant-str-call` which detects unnecessary `str` calls. Off by default.
- [#2302](https://github.com/clj-kondo/clj-kondo/issues/2302): New linter: `:equals-expected-position` to enforce expected value to be in first (or last) position. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md)
- [#1035](https://github.com/clj-kondo/clj-kondo/issues/1035): Support SARIF output with `--config {:output {:format :sarif}}`
- [#2307](https://github.com/clj-kondo/clj-kondo/issues/2307): import configs to intermediate dir
- [#2309](https://github.com/clj-kondo/clj-kondo/issues/2309): Report unused `for` expression
- [#2315](https://github.com/clj-kondo/clj-kondo/issues/2315): Fix regression with unused JavaScript namespace
- [#2304](https://github.com/clj-kondo/clj-kondo/issues/2304): Report unused value in `defn` body
- [#2227](https://github.com/clj-kondo/clj-kondo/issues/2227): Allow `:flds` to be used in keys destructuring for ClojureDart
- [#2316](https://github.com/clj-kondo/clj-kondo/issues/2316): Handle ignore hint on protocol method
- [#2322](https://github.com/clj-kondo/clj-kondo/issues/2322): Add location to warning about invalid unicode character
- [#2319](https://github.com/clj-kondo/clj-kondo/issues/2319): Support `:discouraged-var` on global JS values, like `js/fetch`

## 2024.03.13

- Fix memory usage regression introduced in 2024.03.05
- [#2299](https://github.com/clj-kondo/clj-kondo/issues/2299): Add documentation for `:java-static-field-call`.

## 2024.03.05

- [#1732](https://github.com/clj-kondo/clj-kondo/issues/1732): new linter: `:shadowed-fn-param` which warns on using the same parameter name twice, as in `(fn [x x])`
- [#2276](https://github.com/clj-kondo/clj-kondo/issues/2276): New Clojure 1.12 array notation (`String*`) may occur outside of metadata
- [#2278](https://github.com/clj-kondo/clj-kondo/issues/2278): `bigint` in CLJS is a known symbol in `extend-type`
- [#2288](https://github.com/clj-kondo/clj-kondo/issues/2288): fix static method analysis and suppressing `:java-static-field-call` locally
- [#2293](https://github.com/clj-kondo/clj-kondo/issues/2293): fix false positive static field call for `(Thread/interrupted)`
- [#2296](https://github.com/clj-kondo/clj-kondo/issues/2296): publish multi-arch Docker images (including linux aarch64)
- [#2295](https://github.com/clj-kondo/clj-kondo/issues/2295): lint case test symbols in list

## 2024.02.12

- [#2274](https://github.com/clj-kondo/clj-kondo/issues/2274): Support clojure 1.12 new type hint notations
- [#2260](https://github.com/clj-kondo/clj-kondo/issues/2260): New linter `:java-static-field-call`: calling static _field_ as function should warn, e.g. `(System/err)`
- [#1917](https://github.com/clj-kondo/clj-kondo/issues/1917): detect string being called as function
- [#1923](https://github.com/clj-kondo/clj-kondo/issues/1923): Lint invalid fn name
- [#2256](https://github.com/clj-kondo/clj-kondo/issues/2256): enable `assert` in hooks
- [#2253](https://github.com/clj-kondo/clj-kondo/issues/2253): add support for `datomic-type-extensions` to datalog syntax checking
- [#2255](https://github.com/clj-kondo/clj-kondo/issues/2255): support `:exclude-files` in combination with linting from stdin + provided `--filename` argument
- [#2246](https://github.com/clj-kondo/clj-kondo/issues/2246): preserve metadata on symbol when going through `:macroexpand` hook
- [#2254](https://github.com/clj-kondo/clj-kondo/issues/2254): lint files in absence of config dir
- [#2251](https://github.com/clj-kondo/clj-kondo/issues/2251): support suppressing `:unused-value` using `:config-in-call`
- [#2266](https://github.com/clj-kondo/clj-kondo/issues/2266): suppress `:not-a-function` linter in reader tag
- [#2259](https://github.com/clj-kondo/clj-kondo/issues/2259): `ns-map` unmaps var defined prior in namespace
- [#2272](https://github.com/clj-kondo/clj-kondo/issues/2272): Report var usage in `if`/`when` condition as always truthy, e.g. `(when #'some-var 1)`

## 2023.12.15

- [#1990](https://github.com/clj-kondo/clj-kondo/issues/1990): Specify `:min-clj-kondo-version` in config.edn and warn when current version is too low ([@snasphysicist](https://github.com/snasphysicist))
- [#1753](https://github.com/clj-kondo/clj-kondo/issues/1753): New linter `:underscore-in-namespace` ([@cosineblast](https://github.com/cosineblast))
- [#2207](https://github.com/clj-kondo/clj-kondo/issues/2207): New `:condition-always-true` linter, see [docs](doc/linters.md)
- [#2235](https://github.com/clj-kondo/clj-kondo/issues/2235): New
  `:multiple-async-in-deftest` linter: warn on multiple async blocks in
  `cljs.test/deftest`, since only the first will run.
- [#2013](https://github.com/clj-kondo/clj-kondo/issues/2013): Fix NPE and similar errors when linting an import with an illegal token ([@cosineblast](https://github.com/cosineblast))
- [#2215](https://github.com/clj-kondo/clj-kondo/issues/2215): Passthrough hook should not affect linting
- [#2232](https://github.com/clj-kondo/clj-kondo/issues/2232): Bump analysis for clojure 1.12 (partitionv, etc)
- [#2223](https://github.com/clj-kondo/clj-kondo/issues/2223): Do not consider classes created with `deftype` a var that is referred with `:refer :all`
- [#2236](https://github.com/clj-kondo/clj-kondo/issues/2236): `:line-length` warnings cannot be `:clj-kondo/ignore`d
- [#2224](https://github.com/clj-kondo/clj-kondo/issues/2224): Give `#'foo/foo` and `(var foo/foo)` the same treatment with respect to private calls
- [#2239](https://github.com/clj-kondo/clj-kondo/issues/2239): Fix printing of unresolved var when going through `:macroexpand` hook

## 2023.10.20

- [#1804](https://github.com/clj-kondo/clj-kondo/issues/1804): new linter `:self-requiring-namespace`
- [#2065](https://github.com/clj-kondo/clj-kondo/issues/2065): new linter `:equals-false`, counterpart of `:equals-true` ([@svdo](https://github.com/svdo))
- [#2199](https://github.com/clj-kondo/clj-kondo/issues/2199): add `:syntax` check for var names starting or ending with dot (reserved by Clojure)
- [#2179](https://github.com/clj-kondo/clj-kondo/issues/2179): consider alias-as-object usage in CLJS for :unused-alias linter
- [#2183](https://github.com/clj-kondo/clj-kondo/issues/2183): respect `:level` in `:discouraged-var` config
- [#2184](https://github.com/clj-kondo/clj-kondo/issues/2184): Add missing documentation for `:single-logical-operand` linter ([@wtfleming](https://github.com/wtfleming))
- [#2187](https://github.com/clj-kondo/clj-kondo/issues/2187): Fix type annotation of argument of `clojure.core/parse-uuid` from `nilable/string` to string ([@dbunin](https://github.com/dbunin))
- [#2192](https://github.com/clj-kondo/clj-kondo/issues/2192): Support `:end-row` and `:end-col` in `:pattern` output format ([@joshgelbard](https://github.com/joshgelbard))
- [#2182](https://github.com/clj-kondo/clj-kondo/issues/2182): Namespace local configuration does not silence `:missing-else-branch`
- [#2186](https://github.com/clj-kondo/clj-kondo/issues/2186): Improve warning when `--copy-configs` is enabled but no config dir exists
- [#2190](https://github.com/clj-kondo/clj-kondo/issues/2190): false positive with `:unused-alias` and namespaced map
- [#2200](https://github.com/clj-kondo/clj-kondo/issues/2200): include optional `:callstack` in analysis

## 2023.09.07

- [#1332](https://github.com/clj-kondo/clj-kondo/issues/1332): New linter `:unused-alias`. See [docs](doc/linters.md).
- [#2143](https://github.com/clj-kondo/clj-kondo/issues/2143): false positive type warning for `clojure.set/project`
- [#2145](https://github.com/clj-kondo/clj-kondo/issues/2145): support ignore hint on multi-arity branch of function definition
- [#2147](https://github.com/clj-kondo/clj-kondo/issues/2147): use alternative solution as workaround for https://github.com/cognitect/transit-clj/issues/43
- [#2152](https://github.com/clj-kondo/clj-kondo/issues/2152): Fix false positive with used-underscored-binding with core.match
- [#2150](https://github.com/clj-kondo/clj-kondo/issues/2150): allow command line options = as in --fail-level=error
- [#2149](https://github.com/clj-kondo/clj-kondo/issues/2149): `:lint-as clojure.core/defmacro` should suppress `&env` as unresolved symbol
- [#2161](https://github.com/clj-kondo/clj-kondo/issues/2161): Fix type annotation for `clojure.core/zero?` to number -> boolean
- [#2165](https://github.com/clj-kondo/clj-kondo/issues/2165): Fix error when serializing type data to cache
- [#2167](https://github.com/clj-kondo/clj-kondo/issues/2167): Don't crash when `:unresolved-symbol` linter config contains unqualified symbol
- [#2170](https://github.com/clj-kondo/clj-kondo/issues/2170): `:keyword-binding` linter should ignore auto-resolved keywords
- [#2172](https://github.com/clj-kondo/clj-kondo/issues/2172): detect invalid amount of args and invalid argument type for `throw`
- [#2164](https://github.com/clj-kondo/clj-kondo/issues/2164): deftest inside let triggers :unused-value
- [#2154](https://github.com/clj-kondo/clj-kondo/issues/2154): add `:exclude` option to `:deprecated-namespace` linter
- [#2134](https://github.com/clj-kondo/clj-kondo/issues/2134): don't warn on usage of private var in `data_readers.clj(c)`
- [#2148](https://github.com/clj-kondo/clj-kondo/issues/2148): warn on configuration error in `:unused-refeferred-var` linter
- Expose more vars in `clj-kondo.hooks-api` interpreter namespace

## 2023.07.13

- [#2111](https://github.com/clj-kondo/clj-kondo/issues/2111): warn on symbol in case test using new opt-in linter `:case-symbol-test`
- Rename `:quoted-case-test-constant` to `:case-quoted-test`
- Rename `:duplicate-case-test-constant` to `:case-duplicate-test`
- [#1230](https://github.com/clj-kondo/clj-kondo/issues/1199): new linter, `:unsorted imports`
- [#1125](https://github.com/clj-kondo/clj-kondo/issues/1125): new `:deprecated-namespace` linter
- [#2097](https://github.com/clj-kondo/clj-kondo/issues/2097): analyze and act on `defprotocol` metadata ([@lread](https://github.com/lread))
- [#2105](https://github.com/clj-kondo/clj-kondo/issues/2105): Consider `.cljd` files when linting ([@ericdallo](https://github.com/ericdallo))
- [#2101](https://github.com/clj-kondo/clj-kondo/issues/2101): false positive with `if-some` + `recur`
- [#2109](https://github.com/clj-kondo/clj-kondo/issues/2109): `java.util.List` type hint corresponds to `:list` or nil
- [#2096](https://github.com/clj-kondo/clj-kondo/issues/2096): apply `:arglists` metadata to `:arglist-strs` for analysis data ([@lread](https://github.com/lread))
- [#256](https://github.com/clj-kondo/clj-kondo/issues/256): warn on reader conditional usage in non-cljc files
- [#2115](https://github.com/clj-kondo/clj-kondo/issues/2115): false positive `:redundant-fn-wrapper` in CLJS when passing keyword to JS
- [#1082](https://github.com/clj-kondo/clj-kondo/issues/1082): protocol methods do not support varargs
- [#2125](https://github.com/clj-kondo/clj-kondo/issues/2125): Setting `clj-kondo.hooks-api/*reload*` to true does not lint with the latest hook changes.
- [#2135](https://github.com/clj-kondo/clj-kondo/issues/2135): private vars starting with `_` should not be reported as unused
- [#1199](https://github.com/clj-kondo/clj-kondo/issues/1199): warn about reader conditional features that are not keywords, e.g. `#?(:clj 1 2)` (2 is not a keyword)
- [#2132](https://github.com/clj-kondo/clj-kondo/issues/2132): false negative unused value in clojure.test
- [#1294](https://github.com/clj-kondo/clj-kondo/issues/1294): redefined var comment edge case

## 2023.05.26

- [#2083](https://github.com/clj-kondo/clj-kondo/issues/2083): fix regression with `:missing-test-assertion` introduced in 2023.05.18
- [#2084](https://github.com/clj-kondo/clj-kondo/issues/2084): add `:refers` to `:refer-all` finding
- [#2086](https://github.com/clj-kondo/clj-kondo/issues/2086): false positive missing test assertion with `swap!`
- [#2087](https://github.com/clj-kondo/clj-kondo/issues/2087): honor :config-in-comment for :unused-referred-var

## 2023.05.18

- Linter `:uninitialized-var` moved from default `:level :off` to `:warning`
- [#2065](https://github.com/clj-kondo/clj-kondo/issues/2065): new linter `:equals-true`: suggest using `(true? x)` over `(= true x)` (defaults to `:level :off`).
- [#2066](https://github.com/clj-kondo/clj-kondo/issues/2066): new linters `:plus-one` and `:minus-one`: suggest using `(inc x)` over `(+ x 1)` (and similarly for `dec` and `-`, defaults to `:level :off`)
- [#2051](https://github.com/clj-kondo/clj-kondo/issues/2051): consider `:unresolved-namespace :exclude` as already required namespaces
- [#2056](https://github.com/clj-kondo/clj-kondo/issues/2056): validate collection nodes when constructing and `--debug` is true
- [#2058](https://github.com/clj-kondo/clj-kondo/issues/2058): warn about `#()` and `#""` in `.edn` files
- [#2064](https://github.com/clj-kondo/clj-kondo/issues/2064): False positive when using `:sha` instead of `:git/sha` in combination with git url in `deps.edn`
- [#2063](https://github.com/clj-kondo/clj-kondo/issues/2063): introduce new `:defined-by->lint-as` key which contains the `:lint-as` value for "defining" var, whereas `:defined-as` now always contains the name of the original "defining var". This is a **BREAKING** change.
- [#1983](https://github.com/clj-kondo/clj-kondo/issues/1983): produce java-member-definition analysis for `.java` files.
- [#2068](https://github.com/clj-kondo/clj-kondo/issues/2068): include `:or` default in `:local-usages analysis`
- [#2079](https://github.com/clj-kondo/clj-kondo/issues/2079): analysis for `data_readers.clj`
- [#2067](https://github.com/clj-kondo/clj-kondo/issues/2067): support `:ns-groups` to be used with `:analyze-call` and `:macroexpand` hooks
- [#1918](https://github.com/clj-kondo/clj-kondo/issues/1918): ignore keyword bindings with namespaced in `:keyword-binding` linter
- [#2073](https://github.com/clj-kondo/clj-kondo/issues/2073): `:lint-as clj-kondo.lint-as/def-catch-all` should ignore unresolved namespaces
- [#2078](https://github.com/clj-kondo/clj-kondo/issues/2078): detect more `:missing-test-assertion` cases, e.g. `(deftest foo (not (= 1 2)))`

## 2023.04.14

- [#1196](https://github.com/clj-kondo/clj-kondo/issues/1196): show language context in `.cljc` files with `:output {:langs true}`. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md#show-language-context-in-cljc-files).
- [#2026](https://github.com/clj-kondo/clj-kondo/issues/2026): coercing string did not create StringNode, but TokenNode, lead to false positive `Too many arguments to def`
- [#2030](https://github.com/clj-kondo/clj-kondo/issues/2030): Add a new `:discouraged-tag` linter for discouraged tag literals. See the [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#discouraged-tag).
- Add `:gen` support on `clojure.spec.alpha/keys`
- [#1647](https://github.com/clj-kondo/clj-kondo/issues/1647): support `:exclude-patterns` in `:unresolved-symbol` linter
- [#2036](https://github.com/clj-kondo/clj-kondo/pull/2036): False positive `:def-fn` on `def` + `reify`
- [#2024](https://github.com/clj-kondo/clj-kondo/issues/2024): CLJS allows interop in constructor position
- [#2025](https://github.com/clj-kondo/clj-kondo/issues/2025): support namespace groups with `:unresolved-namespace` linter
- [#2039](https://github.com/clj-kondo/clj-kondo/issues/2039): :analysis `:symbols` + `:aliased-namespace-symbol` linter gives false positive in quoted symbol
- [#2043](https://github.com/clj-kondo/clj-kondo/issues/2043): support ignore annotation on private calls
- Support `:exclude-pattern` in `:unused-binding`
- [#2046](https://github.com/clj-kondo/clj-kondo/issues/2046): clj-kondo stuck in loop with multiple :or in destructuring
- [#2048](https://github.com/clj-kondo/clj-kondo/issues/2048): better linting in `schema.core/defn` with invalid s-exprs

## 2023.03.17

- [#2010](https://github.com/clj-kondo/clj-kondo/issues/2010): Support inline macro configuration. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md#inline-macro-configuration)
- [#2010](https://github.com/clj-kondo/clj-kondo/issues/2010): Short syntax to disable linters: `{:ignore [:unresolved-symbol]}` or `{:ignore true}`, valid in ns-metadata, `:config-in-ns`, `:config-in-call`
- [#2009](https://github.com/clj-kondo/clj-kondo/issues/2009): new `:var-same-name-except-case` linter: warn when vars have names that differ only in case (important for AOT compilation and case-insensitive filesystems) ([@emlyn](https://github.com/emlyn)).
- [#1269](https://github.com/clj-kondo/clj-kondo/issues/1269): warn on `:jvm-opts` in top level of `deps.edn`
- [#2003](https://github.com/clj-kondo/clj-kondo/issues/2003): detect invalid arity call for function passed to `update`, `update-in`, `swap!`, `swap-vals!`, `send`, `send-off`, and `send-via` ([@jakemcc](https://github.com/jakemcc)).
- [#1983](https://github.com/clj-kondo/clj-kondo/issues/1983): add support for java member analysis, via new `java-member-definitions` bucket ([@ericdallo](https://github.com/ericdallo)).
- [#1999](https://github.com/clj-kondo/clj-kondo/issues/1999): add `hooks-api/set-node` and `hooks-api/set-node?` ([@sritchie](https://github.com/sritchie)).
- [#1997](https://github.com/clj-kondo/clj-kondo/issues/1997): False positive on `clojure.core/aget` with more than two args
- [#2011](https://github.com/clj-kondo/clj-kondo/issues/2011): push images to GHCR ([@lispyclouds](https://github.com/lispyclouds))
- [#2001](https://github.com/clj-kondo/clj-kondo/issues/2001): false positive `:misplaced-docstring` in `clojure.test/deftest`

## 2023.02.17

- [#1976](https://github.com/clj-kondo/clj-kondo/issues/1976): warn about using multiple bindings after varargs (`&`) symbol in fn syntax
- Add arity checks for core `def`
- [#1954](https://github.com/clj-kondo/clj-kondo/issues/1954): new `:uninitialized-var` linter. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#uninitialized-var).
- [#1996](https://github.com/clj-kondo/clj-kondo/issues/1996): expose `hooks-api/resolve`. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/hooks.md#api).
- [#1971](https://github.com/clj-kondo/clj-kondo/issues/1971): false positive `:redundant-fn-wrapper` with syntax-quoted body
- [#1984](https://github.com/clj-kondo/clj-kondo/issues/1984): lint java constructor calls as unresolved-symbol when using dot notation.
- [#1970](https://github.com/clj-kondo/clj-kondo/issues/1970): `:dynamic-var-not-earmuffed` should be opt-in
- [#1972](https://github.com/clj-kondo/clj-kondo/issues/1972): type hint aliases should not result in unresolved symbol
- [#1951](https://github.com/clj-kondo/clj-kondo/issues/1951): include end locations in `:line-length` linter
- [#1987](https://github.com/clj-kondo/clj-kondo/issues/1987): Fix escaping of regex literal string in `:macroexpand`
- [#1980](https://github.com/clj-kondo/clj-kondo/issues/1980): make support for ignoring warnings in generated hooks explicit
- [#1979](https://github.com/clj-kondo/clj-kondo/issues/1979): allow `:level :off` in `:discouraged-var` config on var level
- [#1995](https://github.com/clj-kondo/clj-kondo/issues/1995): `clj-kondo.lint-as/def-catch-all` doesn't emit locations, fixes navigation for lsp
- [#1978](https://github.com/clj-kondo/clj-kondo/issues/1978): false positive type error with symbol argument + varargs
- [#1989](https://github.com/clj-kondo/clj-kondo/issues/1989): don't analyze location metadata coming from `:macroexpand hook` (performance optimization)

## 2023.01.20

- [#1956](https://github.com/clj-kondo/clj-kondo/issues/1956): enable printing to `*err*` in hooks
- [#1943](https://github.com/clj-kondo/clj-kondo/issues/1943): allow `:discouraged-namespace` to be suppressed with `#_:clj-kondo/ignore`
- [#1942](https://github.com/clj-kondo/clj-kondo/issues/1942): prioritize specific namespace over ns-groups for `:discouraged-namespace` linter
- [#1959](https://github.com/clj-kondo/clj-kondo/issues/1959): analyze custom `defn` properly
- [#1961](https://github.com/clj-kondo/clj-kondo/issues/1961): be lenient with unexpected type
- [#1945](https://github.com/clj-kondo/clj-kondo/issues/1945): support merging of multiple ns-group configs
- [#1962](https://github.com/clj-kondo/clj-kondo/issues/1962): don't emit warning for aliased namespace var usage in syntax-quote
- [#1952](https://github.com/clj-kondo/clj-kondo/issues/1952): add `:exclude-urls` and `:exclude-pattern`

## 2023.01.16

- [#1920](https://github.com/clj-kondo/clj-kondo/issues/1920): new linter `:def-fn`: warn when using `fn` inside `def`, or `fn` inside `let` inside `def` ([@andreyorst](https://github.com/andreyorst)).
- [#1949](https://github.com/clj-kondo/clj-kondo/issues/1949): `:aliased-namespace-var-usage` gives erroneous output for keywords
- Add test for [#1944](https://github.com/clj-kondo/clj-kondo/issues/1944) (already worked)
- Don't reload SCI namespace on every hook usage

## 2023.01.12

- [#1742](https://github.com/clj-kondo/clj-kondo/issues/1742): new linter `:aliased-namespace-var-usage`: warn on var usage from namespaces that were used with `:as-alias`. See [demo](https://twitter.com/borkdude/status/1613524896625340417/photo/1).
- [#1914](https://github.com/clj-kondo/clj-kondo/issues/1914): Don't warn about single arg use when there's a second arg in a reader conditional ([@mk](https://github.com/mk))
- [#1912](https://github.com/clj-kondo/clj-kondo/issues/1912): Allow forward references in `comment` forms ([@mk](https://github.com/mk)). See [demo](https://twitter.com/borkdude/status/1603028023565062145).
- [#1926](https://github.com/clj-kondo/clj-kondo/issues/1926): Add keyword analysis for edn files.
- [#1922](https://github.com/clj-kondo/clj-kondo/issues/1922): don't crash on invalid type specification
- [#1902](https://github.com/clj-kondo/clj-kondo/issues/1902): provide `:symbols` analysis for navigation to symbols in quoted forms or EDN files. See [demo](https://twitter.com/borkdude/status/1612773780589355008).
- [#1939](https://github.com/clj-kondo/clj-kondo/issues/1939): no longer warn on unused namespace that was only used with `:as-alias`
- [#1911](https://github.com/clj-kondo/clj-kondo/issues/1911): missing test assertion linter doesn't work in CLJS
- [#1891](https://github.com/clj-kondo/clj-kondo/issues/1891): support `CLJ_KONDO_EXTRA_CONFIG_DIR` environment variable to enable extra linters after project config

## 2022.12.10

- [#1909](https://github.com/clj-kondo/clj-kondo/issues/1909): lower requirement on `glibc` in dynamic linux binary to 2.31 by using fixed version of CircleCI image

## 2022.12.08

- [#609](https://github.com/clj-kondo/clj-kondo/issues/609): typecheck var usage, e.g. `(def x :foo) (inc x)` will now give a warning
- [#1867](https://github.com/clj-kondo/clj-kondo/issues/1867): add name metadata to class usage
- [#1875](https://github.com/clj-kondo/clj-kondo/issues/1875): add `:duplicate-field-name` linter for deftype and defrecord definitions.
- [#1883](https://github.com/clj-kondo/clj-kondo/issues/1883): constructor usage should have name-col in analysis
- [#1874](https://github.com/clj-kondo/clj-kondo/issues/1874): fix name of fully qualified class usage
- [#1876](https://github.com/clj-kondo/clj-kondo/issues/1876): suppress . analysis from .. macroexpansion
- [#1877](https://github.com/clj-kondo/clj-kondo/issues/1877): suppress `new` analysis from `(String. x)` expansion
- [#1888](https://github.com/clj-kondo/clj-kondo/issues/1888): use `namespace-munge` for resolving hook files rather than `munge`
- [#1896](https://github.com/clj-kondo/clj-kondo/issues/1896): don't consider `**`, `***` etc. to be a dynamic vars
- [#1899](https://github.com/clj-kondo/clj-kondo/issues/1899): treat var or local reference as unused value when not in tail position
- [#1903](https://github.com/clj-kondo/clj-kondo/issues/1903): int can be cast to double
- [#1871](https://github.com/clj-kondo/clj-kondo/issues/1871): clj-kondo marks args in `definterface` as unused
- [#1879](https://github.com/clj-kondo/clj-kondo/issues/1879): analyze `definterface` more similarly to `defprotocol` for lsp-navigation
- [#1907](https://github.com/clj-kondo/clj-kondo/issues/1907): add `hooks-api/generated-node?` function to check if a node was generated
- [#1887](https://github.com/clj-kondo/clj-kondo/issues/1887), use `re-find` for ns groups rather than re-matches

## 2022.11.02

- [#1846](https://github.com/clj-kondo/clj-kondo/issues/1846): new linters: `:earmuffed-var-not-dynamic` and `:dynamic-var-not-earmuffed`. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#dynamic-vars).
- [#1842](https://github.com/clj-kondo/clj-kondo/issues/1842): Add `:exclude` option to `:used-underscored-binding` ([@staifa](https://github.com/staifa))
- [#1840](https://github.com/clj-kondo/clj-kondo/issues/1840): Fix warning in `.cljs` and `.cljc` for `:aliased-namespace-symbol` in interop calls. ([@NoahTheDuke](https://github.com/NoahTheDuke))
- [#1845](https://github.com/clj-kondo/clj-kondo/issues/1845): add `:derived-location` to analysis when location is derived from parent node
- [#1853](https://github.com/clj-kondo/clj-kondo/issues/1853): fix `:level :off` not being respected in `:discouraged-var` configs that are merged in.
- [#1855](https://github.com/clj-kondo/clj-kondo/issues/1855): accept symbol in addition to keyword in `clojure.spec.alpha/def` name position
- [#1844](https://github.com/clj-kondo/clj-kondo/issues/1844): support extra schema in `schema.core/defrecord`
- [#1720](https://github.com/clj-kondo/clj-kondo/issues/1720): prevent parse error on defmulti without args
- [#1863](https://github.com/clj-kondo/clj-kondo/issues/1863): Added `clj-kondo-docker` pre-commit hook.

## 2022.10.14

- [#1831](https://github.com/clj-kondo/clj-kondo/issues/1831): Add `:redundant-fn-wrapper` support for keyword and binding calls ([@NoahTheDuke](https://github.com/NoahTheDuke))
- [#1830](https://github.com/clj-kondo/clj-kondo/issues/1830): Fix warning on `:include-macros` in `.cljs` and `.cljc` for `:unknown-require-option` linter. ([@NoahTheDuke](https://github.com/NoahTheDuke))
- [#1238](https://github.com/clj-kondo/clj-kondo/issues/1238): Build a linux/aarch64 executable in CI ([cap10morgan](https://github.com/cap10morgan))
- Add `:exclude` option to `:unknown-require-option`
- Enable `:unused-value` by default
- Publish `.sha256` files along with released artifacts

## 2022.10.05

- [#611](https://github.com/clj-kondo/clj-kondo/issues/611): New linter: `:unused-value` - see [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#unused-value). Also see issue [#1258](https://github.com/clj-kondo/clj-kondo/issues/1258).
- [#1794](https://github.com/clj-kondo/clj-kondo/issues/1794): New linter: `:line-length` - see [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#line-length) ([@ourkwest](https://github.com/ourkwest))
- [#1460](https://github.com/clj-kondo/clj-kondo/issues/1460): New linter: `:unknown-require-option` - see [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#unknown-require-option). ([@NoahTheDuke](https://github.com/NoahTheDuke))
- [#1800](https://github.com/clj-kondo/clj-kondo/issues/1800): New linter: `:aliased-namespace-symbol` - see [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#aliased-namespace-symbol). ([@NoahTheDuke](https://github.com/NoahTheDuke))
- [#1758](https://github.com/clj-kondo/clj-kondo/issues/1758): Overrides don't get applied to var-definitions during analysis ([@sheluchin](https://github.com/sheluchin))
- [#1807](https://github.com/clj-kondo/clj-kondo/issues/1807): False positive with map transducer in cljs
- [#1806](https://github.com/clj-kondo/clj-kondo/issues/1806): False positive recur mismatch with letfn
- [#1810](https://github.com/clj-kondo/clj-kondo/issues/1810): Fix printing error map as additional error ([@NoahTheDuke](https://github.com/NoahTheDuke))
- [#1812](https://github.com/clj-kondo/clj-kondo/issues/1812): Inconsistent handling of location metadata sometimes produces `nil` values ([@sheluchin](https://github.com/sheluchin))
- [#1805](https://github.com/clj-kondo/clj-kondo/issues/1805): Ignore hints not being considered on protocol vars
- [#1819](https://github.com/clj-kondo/clj-kondo/issues/1819): Fix "Too many open files" in java class definition analysis caused by files not being closed ([@rsauex](https://github.com/rsauex))
- [#1821](https://github.com/clj-kondo/clj-kondo/issues/1821): Include vectors in `:unused-binding` config `:exclude-destructured-as` flag. ([@NoahTheDuke](https://github.com/NoahTheDuke))
- [#1818](https://github.com/clj-kondo/clj-kondo/issues/1818): unresolved var when using interop on var in CLJS
- [#1817](https://github.com/clj-kondo/clj-kondo/issues/1817): improve warning with invalid require libspec ([@benjamin-asdf](https://github.com/benjamin-asdf))
- [#1801](https://github.com/clj-kondo/clj-kondo/issues/1801): Missing docstring fix for `deftype` in CLJS
- Bump GraalVM to 22.2.0 for native-image
- Bump SCI to 0.4.33

## 2022.09.08

- `:config-in-call` - see [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md#config-in-call)
- [#1127](https://github.com/clj-kondo/clj-kondo/issues/1127): `:config-in-tag` - see [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md#config-in-tag)
- [#1791](https://github.com/clj-kondo/clj-kondo/issues/1791): Fix issue with namespace-name-mismatch on namespaces with + sign (and others)
- [#1782](https://github.com/clj-kondo/clj-kondo/issues/1782): Fix issue with jar URI missreporting to `file-analyzed-fn`, bump babashka/fs to 0.1.11
- [#1780](https://github.com/clj-kondo/clj-kondo/issues/1780): Can not use NPM dependency namespaces beginning with "@" in consistent-linter alias
- [#1771](https://github.com/clj-kondo/clj-kondo/issues/1771): don't crash on empty ns clauses: `(require '[])` and `(import '())`
- [#1774](https://github.com/clj-kondo/clj-kondo/issues/1774): Add support for sourcehut inferred git dep urls for the Clojure CLI
- [#1768](https://github.com/clj-kondo/clj-kondo/issues/1768): Expose a `tag` function in `clj-kondo.hooks-api`
- [#1790](https://github.com/clj-kondo/clj-kondo/issues/1790): Add support for `:filename-pattern` in `:ns-group`
- [#1773](https://github.com/clj-kondo/clj-kondo/issues/1773): false positive type mismatch warning with hook
- [#1779](https://github.com/clj-kondo/clj-kondo/issues/1779): lazy-seq should be coerced a list node
- [#1764](https://github.com/clj-kondo/clj-kondo/issues/1764): store overrides in cache and don't run them at runtime

## 2022.08.03

- [#1755](https://github.com/clj-kondo/clj-kondo/issues/1755): Fix false positive invalid-arity when using mapcat transducer in sequence with multiple collections
- [#1749](https://github.com/clj-kondo/clj-kondo/issues/1749): expose `clojure.pprint/pprint` to the hooks API
- [#698](https://github.com/clj-kondo/clj-kondo/issues/698): output rule name with new output option `:show-rule-name-in-message true`. See example in [config guide](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md#show-rule-name-in-message).
- [#1735](https://github.com/clj-kondo/clj-kondo/issues/1735) Add support for nilable map type specs
- [#1744](https://github.com/clj-kondo/clj-kondo/issues/1744) Expose `:imported-ns` in analysis of vars imported by potemkin
- [#1746](https://github.com/clj-kondo/clj-kondo/issues/1746) Printing deps.edn error to stdout
- [#1716](https://github.com/clj-kondo/clj-kondo/issues/1716) Include dispatch-val in analysis of defmethod
- [#1760](https://github.com/clj-kondo/clj-kondo/issues/1760) Add `:arglist-strs` support for functions defined with fn
- [#1731](https://github.com/clj-kondo/clj-kondo/issues/1731): prioritize special form in name resolving
- [#1739](https://github.com/clj-kondo/clj-kondo/issues/1739): Namespaced map type check fix
- Fix [#1737](https://github.com/clj-kondo/clj-kondo/issues/1737): config-in-ns for specific namespace + ns-group override
- Fix [#1741](https://github.com/clj-kondo/clj-kondo/issues/1741): Ignore redundant-call when single call is made in .cljc

## 2022.06.22

- [#1721](https://github.com/clj-kondo/clj-kondo/issues/1721): new `:discouraged-namespace` linter. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#discouraged-namespace).
- [#1728](https://github.com/clj-kondo/clj-kondo/issues/1728) New macOS aarch64 (M1-compatible) binary
- [#1726](https://github.com/clj-kondo/clj-kondo/issues/1726): fix invalid arity warning for `sequence` with `map` multi-arity transducer
- [#1715](https://github.com/clj-kondo/clj-kondo/issues/1715): fix false positive warning for `recur` not in tail position with `core.async alt!!`
- [#1714](https://github.com/clj-kondo/clj-kondo/issues/1714): fix recur arity for `defrecord`, `deftype` and `definterface`
- [#1718](https://github.com/clj-kondo/clj-kondo/issues/1718): make unsorted namespaces linter case-insensitive
- [#1722](https://github.com/clj-kondo/clj-kondo/issues/1722): suppress redundant do in `.cljc` for just one language

## 2022.05.31

- Ensure every node has a location when returning from `:macroexpand`

## 2022.05.29

- Support `:instance-invocations` analysis bucket
- Copy `.clj_kondo` files from configs

## 2022.05.28

- Fix false positive redundant do's from `:macroexpand` hooks (regression in 2022.05.27)

## 2022.05.27

- [#686](https://github.com/clj-kondo/clj-kondo/issues/686): new `:warn-on-reflection` linter. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#warn-on-reflection).
- [#1692](https://github.com/clj-kondo/clj-kondo/pull/1692): new linter `:redundant-call` - warns when a function or macro call with 1 given argument returns the argument. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#redundant-call).
- All new JVM `clj-kondo.hooks-api` API ns for REPL usage. See
  [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/hooks.md#developing-hooks-in-the-repl).
- [#1674](https://github.com/clj-kondo/clj-kondo/pull/1674): config options to limit analysis of var-usages and bodies of var-definitions. Can be used to get a quick overview of a project's namespaces and vars, without analyzing their details.
- [#1676](https://github.com/clj-kondo/clj-kondo/pull/1676): Add support for custom function to be called for progress update, `:file-analyzed-fn`.
- [#1697](https://github.com/clj-kondo/clj-kondo/issues/1697): update docs and messaging around importing configs ([@lread](https://github.com/lread))
- [#1700](https://github.com/clj-kondo/clj-kondo/issues/1700): allow discouraged var on non-analyzed (closed source) vars
- [#1703](https://github.com/clj-kondo/clj-kondo/issues/1703): update built-in cache with newest CLJ (1.11.1) and CLJS (1.11.54) versions
- [#1704](https://github.com/clj-kondo/clj-kondo/issues/1704): fix re-frame analysis bug
- [#1705](https://github.com/clj-kondo/clj-kondo/issues/1705): add pre-commit utility support via `.pre-commit-hooks.yaml`
- [#1701](https://github.com/clj-kondo/clj-kondo/issues/1701): preserve locations in seqs and symbols in `:macroexpand` hook
- [#1685](https://github.com/clj-kondo/clj-kondo/issues/1685): Support `.clj_kondo` hook extension
- [#1670](https://github.com/clj-kondo/clj-kondo/issues/1670): parse error on auto-resolved keyword for current ns
- [#1672](https://github.com/clj-kondo/clj-kondo/issues/1672): support `clojure.test/deftest-`
- [#1678](https://github.com/clj-kondo/clj-kondo/issues/1678): support `with-precision`

## 2022.04.25

- [#1669](https://github.com/clj-kondo/clj-kondo/issues/1669): fix re-frame analysis problem causing file to be not parsed

## 2022.04.23

- [#1653](https://github.com/clj-kondo/clj-kondo/issues/1653): new linter `:keyword-binding` - warns when a keyword
is used in a `:keys` binding vector. This linter is `:off` by default. See [docs](doc/linters.md#keyword-in-binding-vector).
- [#996](https://github.com/clj-kondo/clj-kondo/issues/996): new linter `:discouraged-var`. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#discouraged-var).
- [#1618](https://github.com/clj-kondo/clj-kondo/issues/1618): new `:config-in-ns` configuration option. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md#config-in-ns).
- Support `:ns-groups` configuration option. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md#namespace-groups)
- [#1657](https://github.com/clj-kondo/clj-kondo/issues/1657): support bindings with same name in `clojure.core.match`
- [#1659](https://github.com/clj-kondo/clj-kondo/issues/1659): fix false positive unused import
- [#1649](https://github.com/clj-kondo/clj-kondo/issues/1649): dot (`.`) should
  be unresolved when not in fn position

## 2022.04.08

- [#1331](https://github.com/clj-kondo/clj-kondo/issues/1331): new linter `:non-arg-vec-return-type-hint` that warns when a return type hint is not placed on the arg vector (CLJ only). See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#non-arg-vec-return-type-hint).
- Enable `:namespace-name-mismatch` by default
- [#1611](https://github.com/clj-kondo/clj-kondo/pull/1611): support `^:replace` override for nested config values
- [#1625](https://github.com/clj-kondo/clj-kondo/issues/1625): Add option `--skip-lint`, to skip linting while still executing other tasks like copying configuration with `--copy-configs`.
- [#1620](https://github.com/clj-kondo/clj-kondo/issues/1620): return type too narrow for `re-find`

Analysis:

- [#1623](https://github.com/clj-kondo/clj-kondo/issues/1623): Implement analysis for Java classes: `:java-class-definitions` and `:java-class-usages`. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/analysis/README.md).
- [#1635](https://github.com/clj-kondo/clj-kondo/pull/1635): add `:end-row` and `end-col` to analyze data for `:namespace-definitions`
- [#1651](https://github.com/clj-kondo/clj-kondo/issues/1651): Improvements for `:protocol-impls`
- [#1612](https://github.com/clj-kondo/clj-kondo/issues/1612): Improve analysis for `deftype`
- [#1613](https://github.com/clj-kondo/clj-kondo/issues/1613): Improve analysis for `reify`
- [#1609](https://github.com/clj-kondo/clj-kondo/issues/1609): keyword analysis for `ns` + `require`

## 2022.03.09

- [#1607](https://github.com/clj-kondo/clj-kondo/issues/1607): disable `:namespace-name-mismatch` until further notice due to problems on Windows
- [#1570](https://github.com/clj-kondo/clj-kondo/issues/1570): add `:defmethod true` to defmethod `var-usages` analysis.

## 2022.03.08

### New

- [#1602](https://github.com/clj-kondo/clj-kondo/issues/1602): analysis data now includes `:protocol-ns` and `:protocol-name` on protocol methods ([@lread](https://github.com/lread))

### Fixed

- [#1605](https://github.com/clj-kondo/clj-kondo/issues/1605): error while determining namespace mismatch on Windows

## 2022.03.04

### New

- [#1240](https://github.com/clj-kondo/clj-kondo/issues/1240): Add linter `:namespace-name-mismatch` to detect when namespace name does not match file name. ([@svdo](https://github.com/svdo))

### Fixed

- [#1598](https://github.com/clj-kondo/clj-kondo/issues/1598): `:scope-end-row` is missing on multi-arity fn args ([@mainej](https://github.com/mainej))
- [#1588](https://github.com/clj-kondo/clj-kondo/issues/1588): analyze type hint in reified method
- [#1581](https://github.com/clj-kondo/clj-kondo/issues/1581): redundant fn wrapper false positive when using pre-post-map
- [#1582](https://github.com/clj-kondo/clj-kondo/issues/1582): False positive Insufficient input when using symbol call
- [#1579](https://github.com/clj-kondo/clj-kondo/issues/1579): relax linting in tagged literal forms
- [#1578](https://github.com/clj-kondo/clj-kondo/issues/1578): allow `:deprecated-var` config in ns form metadata
- [#892](https://github.com/clj-kondo/clj-kondo/issues/892): suppress unresolved namespaces in data readers config
- [#1594](https://github.com/clj-kondo/clj-kondo/issues/1594): lint clojure.test.check.properties/for-all as let

## 2022.02.09

### New

- [#1549](https://github.com/clj-kondo/clj-kondo/issues/1549): detect and warn on cyclic task dependencies in `bb.edn` ([@mknoszlig](https://github.com/mknoszlig))
- [#1547](https://github.com/clj-kondo/clj-kondo/issues/1547): catch undefined tasks present in `:depends`. ([@mknoszlig](https://github.com/mknoszlig))
- [#783](https://github.com/clj-kondo/clj-kondo/issues/783): `:keys` can be used in `:ret` position, also fixes types return map call as input for another typed map function. ([@pfeodrippe](https://github.com/pfeodrippe))
- [#1526](https://github.com/clj-kondo/clj-kondo/issues/1526): detect
  redundant fn wrappers, like `#(inc %)`. See
  [docs](doc/linters.md#redundant-fn-wrapper). This linter of `:off` by default
  but may be enabled by default in future versions after more testing.
- [#1560](https://github.com/clj-kondo/clj-kondo/issues/1560): lint task definition keys in `bb.edn` ([@mknoszlig](https://github.com/mknoszlig))
- [#1484](https://github.com/clj-kondo/clj-kondo/issues/1484): Add analysis information about protocol implementations. ([@ericdallo](https://github.com/ericdallo))

### Fixed

- [#1563](https://github.com/clj-kondo/clj-kondo/issues/1563): vector inside list should not be linted as function call when inside tagged literal.
- [#1540](https://github.com/clj-kondo/clj-kondo/issues/1540): imported class flagged as unused when it only appears in annotation metadata.
- [#1571](https://github.com/clj-kondo/clj-kondo/issues/1571): ignore spliced reader conditionals wrt. namespace sorting.
- [#1574](https://github.com/clj-kondo/clj-kondo/issues/1574): def usage context contains reference of the re-frame reg-sub it is used in. ([@benedekfazekas](https://github.com/benedekfazekas))

## 2022.01.15

- Fix [#1537](https://github.com/clj-kondo/clj-kondo/issues/1537): stackoverflow with potemkin import vars with cyclic references
- Fix [#1545](https://github.com/clj-kondo/clj-kondo/issues/1545): `recur` in `cond->` gives warning about `recur` not in tail position.
- Fix [#1535](https://github.com/clj-kondo/clj-kondo/issues/1535): support CLJS vars / protocols references via dot rather than slash.

## 2022.01.13

- Add linter `:conflicting-fn-arity`: warn when an arity occurs more than once in a function that overloads on arity. [#1136](https://github.com/clj-kondo/clj-kondo/issues/1136) ([@mknoszlig](https://github.com/mknoszlig))
- Add linter `:clj-kondo-config` which provides linting for `.clj-kondo/config.edn`. [#1527](https://github.com/clj-kondo/clj-kondo/issues/1527)
- Relax `:reduce-without-init` for functions known to be safe [#1519](https://github.com/clj-kondo/clj-kondo/issues/1519)
- Symbol arg to `fdef` can be arbitrary namespace [#1532](https://github.com/clj-kondo/clj-kondo/issues/1532)
- Improve potemkin generated var-definition analysis [#1521](https://github.com/clj-kondo/clj-kondo/issues/1521) ([@ericdallo](https://github.com/ericdallo))
-  Stabilize cache version independent from kondo version [#1520](https://github.com/clj-kondo/clj-kondo/issues/1520). This allows you to re-use the cache over multiple kondo versions.
- `:output {:progress true}` should print to stderr [#1523](https://github.com/clj-kondo/clj-kondo/issues/1523)
- Only print informative messages when `--debug` is enabled. [#1514](https://github.com/clj-kondo/clj-kondo/issues/1514)
- Add Sublime Text instructions [#827](https://github.com/clj-kondo/clj-kondo/issues/827) ([@KyleErhabor](https://github.com/KyleErhabor))
- Fix end location in anonyous function body [#1533](https://github.com/clj-kondo/clj-kondo/issues/1533)
- Bump datalog-parser to 0.1.9: allows symbol constants in datalog expression

## 2021.12.19

### New

- Add linter `:reduce-without-init`: warn against two argument version of
  reduce. Disabled by
  default. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#reduce-without-initial-value). [#1064](https://github.com/clj-kondo/clj-kondo/issues/1064) ([@mknoszlig](https://github.com/mknoszlig))
- Add linter `:quoted-case-test-constant`: warn on quoted test constants in case. [#1496](https://github.com/clj-kondo/clj-kondo/issues/1496) ([@mknoszlig](https://github.com/mknoszlig))

### Enhanced

- Fix false positive unused binding in re-frame subscribe [#1504](https://github.com/clj-kondo/clj-kondo/issues/1504)
- Fix exclude-defmulti-args for CLJS [#1503](https://github.com/clj-kondo/clj-kondo/issues/1503)
- Fix warning location of namespaced map [#1475](https://github.com/clj-kondo/clj-kondo/issues/1475)
- False positive :docstring-no-summary on multiline docstrings [#1507](https://github.com/clj-kondo/clj-kondo/issues/1507)


## 2021.12.16

### New

- Automatically load configurations from `.clj-kondo/*/*/config.edn`. This can be disabled with `:auto-load-configs false`. [#1492](https://github.com/clj-kondo/clj-kondo/issues/1492)
- Add linter `:duplicate-case-test-constant`: detect duplicate case test constants. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#duplicate-case-test-constant). [#587](https://github.com/clj-kondo/clj-kondo/issues/587) ([@mknoszlig](https://github.com/mknoszlig))
- Add linter `:unexpected-recur`: warn on `recur` in unexpected (non-tail) position. [#1126](https://github.com/clj-kondo/clj-kondo/issues/1126)
- Add linter `:used-underscored-binding`: warn on used bindings that start with underscore. Disabled by default. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#used-underscored-bindings). [#1149](https://github.com/clj-kondo/clj-kondo/issues/1149) ([@mknoszlig](https://github.com/mknoszlig))
- Add linter `:docstring-blank` for checking empty docstring. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#docstring-blank). [#805](https://github.com/clj-kondo/clj-kondo/issues/805) ([@joodie](https://github.com/joodie))
- Add linter `:docstring-leading-trailing-whitespace` for checking leading and trailing whitespace in docstring. Disabled by default. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#docstring-leading-trailing-whitespace). [#805](https://github.com/clj-kondo/clj-kondo/issues/805) ([@joodie](https://github.com/joodie))
- Add linter `:docstring-no-summary` for checking the absence of summary of args in docstring. Disabled by default. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#docstring-no-summary). [#805](https://github.com/clj-kondo/clj-kondo/issues/805) ([@joodie](https://github.com/joodie))
- Add `:exclude-defmulti-args` option for `:unused-bindings` linter. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md). [#1188](https://github.com/clj-kondo/clj-kondo/issues/1188) ([@mknoszlig](https://github.com/mknoszlig))
- Support `:config-in-comment` [#1473](https://github.com/clj-kondo/clj-kondo/issues/1473). See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md#override-config-in-comment-forms).

### Enhanced

- Bump built-in cache for clojure 1.11.0-alpha3 and `clojure.data.json`
- Reword `:refer` suggestion so you can copy paste it [#1293](https://github.com/clj-kondo/clj-kondo/issues/1293) ([@vemv](https://github.com/vemv))
- Add re-frame analysis output [#1465](https://github.com/clj-kondo/clj-kondo/issues/1465) ([@benedekfazekas](https://github.com/benedekfazekas))
- Qualified map causes too many arguments in type checker [#1474](https://github.com/clj-kondo/clj-kondo/issues/1474)
- Handle reader conditional with unknown language [#970](https://github.com/clj-kondo/clj-kondo/issues/970)

## 2021.12.01

- Improve linting in `extend-protocol`, `extend-type`, `reify`, `specify!` [#1333](https://github.com/clj-kondo/clj-kondo/issues/1333), [#1447](https://github.com/clj-kondo/clj-kondo/issues/1447)
- Support `:context` in nodes in hooks for adding context to analysis [#1211](https://github.com/clj-kondo/clj-kondo/issues/1211)
- `goog.object`, `goog.string` etc must be required before use in newer releases
  of CLJS [#1422](https://github.com/clj-kondo/clj-kondo/issues/1422)
- Resume linting after invalid keyword [#1451](https://github.com/clj-kondo/clj-kondo/issues/1451)
- Fix install script for relative dir opts [#1444](https://github.com/clj-kondo/clj-kondo/issues/1444)
- Fix type mismatch error with auto-qualified keyword [#1467](https://github.com/clj-kondo/clj-kondo/issues/1467)
- String type hint causes false error report [#1455](https://github.com/clj-kondo/clj-kondo/issues/1455)
- Fix false positive with cljs/specify! [#1450](https://github.com/clj-kondo/clj-kondo/issues/1450)
- Improve analysis for ns-modifying destructuring key [#1441](https://github.com/clj-kondo/clj-kondo/issues/1441)
- CLJS `(exists? foo.bar/az)` complains about require [#1472](https://github.com/clj-kondo/clj-kondo/issues/1472)

## 2021.10.19

### New

- New optional linter: warn on missing `gen-class` if namespace has `-main` fn
  [#1417](https://github.com/clj-kondo/clj-kondo/issues/1417). See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#main-without-gen-class).
- Detect arity mismatches for functions defined with `def` [#1408](https://github.com/clj-kondo/clj-kondo/issues/1408)
- Type inference improvements for `def` + `fn` combination
  [#1410](https://github.com/clj-kondo/clj-kondo/issues/1410)
- Local `fn` type inference
  [#1412](https://github.com/clj-kondo/clj-kondo/issues/1412)
- Analysis: allow user to request all or specific metadata be returned [#1280](https://github.com/clj-kondo/clj-kondo/issues/1280) ([@lread](https://github.com/lread))
-  `rseq` called on other type than vector or sorted-map now gives type error [#1432](https://github.com/clj-kondo/clj-kondo/issues/1432)

### Enhanced / fixed

- Fix false positive with ns-unmap [#1393](https://github.com/clj-kondo/clj-kondo/issues/1393)
- Support custom-lint-fn with `.cljc` [#1403](https://github.com/clj-kondo/clj-kondo/issues/1403)
- Allow reader conditional in metadata [#1414](https://github.com/clj-kondo/clj-kondo/issues/1414)
- Analysis: add `:from-var` in higher order call [#1404](https://github.com/clj-kondo/clj-kondo/issues/1404)
- Dedupe linted files [#1395](https://github.com/clj-kondo/clj-kondo/issues/1395) ([@ericdallo](https://github.com/ericdallo))
- Add `:duplicate-ns` to duplicate-require linter output [#1421](https://github.com/clj-kondo/clj-kondo/issues/1421) ([@ericdallo](https://github.com/ericdallo))
- `if-let` / `if-some` with invalid arity no longer warn [#1426](https://github.com/clj-kondo/clj-kondo/issues/1426)
- Analysis: spport for defn 2nd attr-map, :doc derivation fixes ([@lread](https://github.com/lread))
- Fix parsing of trailing metdata map [#1433](https://github.com/clj-kondo/clj-kondo/issues/1433) ([@lread](https://github.com/lread))

## 2021.09.25

- Update built-in cache to clojure 1.11.0-alpha2 [#1382](https://github.com/clj-kondo/clj-kondo/issues/1382)
- Take into account aliases in `import-vars` [#1385](https://github.com/clj-kondo/clj-kondo/issues/1385)
- Consider var as used in CLJS `case` to avoid false positives for constants [#1388](https://github.com/clj-kondo/clj-kondo/issues/1388)
- Understand `ns-unmap` pattern [#1384](https://github.com/clj-kondo/clj-kondo/issues/1384)
- Expose config functions in core API namespace [#1389](https://github.com/clj-kondo/clj-kondo/issues/1389)
- Fix false positives when using quoted collection in function position [#1390](https://github.com/clj-kondo/clj-kondo/issues/1390)

### Analysis

- Add `:end-row`, `:end-col` to `:var-usages` analysis element [#1387](https://github.com/clj-kondo/clj-kondo/pull/1387)
- BREAKING: Change `:row` and `:col` for `:var-usages` to use the start location of the call instead of the name location [#1387](https://github.com/clj-kondo/clj-kondo/issues/1387)

## 2021.09.15

- Support `:as-alias` (new feature in Clojure 1.11) [#1378](https://github.com/clj-kondo/clj-kondo/issues/1378)
- Improve `:loop-without-recuir` wrt/ `fn` and other constructs that introduce a `recur` target [#1376](https://github.com/clj-kondo/clj-kondo/issues/1376)

## 2021.09.14

- Add `:loop-without-recur` linter. [#426](https://github.com/clj-kondo/clj-kondo/issues/426)
- Lint `deps.edn` and `bb.edn` `:paths` [#1353](https://github.com/clj-kondo/clj-kondo/issues/1353) ([@lread](https://github.com/lread))
- Fix unresolved-symbol for all-lowercase class name [#1362](https://github.com/clj-kondo/clj-kondo/issues/1362)
- Add `:refer` to var-usages when inside a require [#1364](https://github.com/clj-kondo/clj-kondo/issues/1364) ([@ericdallo](https://github.com/ericdallo))
- musl fix [#1365](https://github.com/clj-kondo/clj-kondo/issues/1365) ([@thiagokokada](https://github.com/thiagokokada))
- Fix incorrectly reported filename [#1366](https://github.com/clj-kondo/clj-kondo/issues/1366)
- Self-referring private-var should be reported unused if not used elsewhere [#1367](https://github.com/clj-kondo/clj-kondo/issues/1367)
- Support options map in `babashka.process/$` [#1368](https://github.com/clj-kondo/clj-kondo/issues/1368)
- Analyze metadata map of `defmulti` [#1310](https://github.com/clj-kondo/clj-kondo/issues/1310)
- Add support for potemkin full qualified symbols [#1371](https://github.com/clj-kondo/clj-kondo/issues/1371) ([@ericdallo](https://github.com/ericdallo))

## 2021.08.06

- Expose `ns-analysis` fn in hooks API [#1349](https://github.com/clj-kondo/clj-kondo/issues/1349) ([@hugoduncan](https://github.com/hugoduncan))
- Fix for Windows when analyzing deps

## 2021.08.03

### Enhanced / fixed

- Fix conflicts between application code and hook config code in cache [#1340](https://github.com/clj-kondo/clj-kondo/issues/1340)
- Allow overriding level in `reg-finding!` [#1344](https://github.com/clj-kondo/clj-kondo/issues/1344) ([@ericdallo](https://github.com/ericdallo))
- Fix `declare` name positions in analysis [#1343](https://github.com/clj-kondo/clj-kondo/issues/1343) ([@ericdallo](https://github.com/ericdallo))
- Updated rules for `deps.edn` to match Clojure CLI `1.10.1.933` ([@dpassen](https://github.com/dpassen))

## 2021.07.28

### New

- `:macroexpand` hook. This allows linting using the same or similar macros from
  your code. See
  [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/hooks.md#macroexpand).

### Enhanced / fixed

- Add types for `ex-info` [#1314](https://github.com/clj-kondo/clj-kondo/issues/1314)
- Bump SCI to v0.2.6
- Fix EDN/JSON serialization of findings for NPM string namespace [#1319](https://github.com/clj-kondo/clj-kondo/issues/1319)
- Support fully qualified symbol in def referring to current namespace [#1326](https://github.com/clj-kondo/clj-kondo/issues/1326)
- Fix false positive redundant expression in pre-post map [#1335](https://github.com/clj-kondo/clj-kondo/issues/1335)

## 2021.06.18

### New

- Lint arities of fn arguments to higher order functions (`map`, `filter`, `reduce`, etc.) [#1297](https://github.com/clj-kondo/clj-kondo/issues/1297)
- Add `map-node` and `map-node?` to hooks API [#1270](https://github.com/clj-kondo/clj-kondo/issues/1270)

### Enhanced / fixed

- Disable redefined-var warning in comment [#1294](https://github.com/clj-kondo/clj-kondo/issues/1294)
- `:skip-comments false` doesn't override `:skip-comments true` in namespace config [#1295](https://github.com/clj-kondo/clj-kondo/issues/1295)
- False positive duplicate element set for symbols/classes [#1296](https://github.com/clj-kondo/clj-kondo/issues/1296)

## v2021.06.01

- False positive unused namespace with `clojure.spec/keys` [#1289](https://github.com/clj-kondo/clj-kondo/issues/1289)

## v2021.05.31

### New

- Lint `clojure.spec.alpha/keys` [#1272](https://github.com/clj-kondo/clj-kondo/issues/1272) ([@daveduthie](https://github.com/daveduthie))
- Macroexpand `clojure.template/do-template` [#603](https://github.com/clj-kondo/clj-kondo/issues/603)
- Proper macroexpansion for `clojure.test/are` [#1284](https://github.com/clj-kondo/clj-kondo/issues/1284)
- Resolve vars in `clojure.data.xml` imported via macro [#1274](https://github.com/clj-kondo/clj-kondo/issues/1274)
- Lint `([])` as invalid call to vector [#1276](https://github.com/clj-kondo/clj-kondo/issues/1276)

### Enhanced / fixed

- Improve keyword reg support for re-frame [#1159](https://github.com/clj-kondo/clj-kondo/issues/1159) ([@ericdallo](https://github.com/ericdallo))
- Refine messaging around importing configs [#1256](https://github.com/clj-kondo/clj-kondo/issues/1256) ([@lread](https://github.com/lread))
- Static linux binary is now compiled with musl
- Recognize `:doc` from attr-map in `defn` [#1265](https://github.com/clj-kondo/clj-kondo/issues/1265)
- Don't skip linting `.jar` files with `--dependencies` when config(s) have changed [#1285](https://github.com/clj-kondo/clj-kondo/issues/1285)

## 2021.04.23

### New

- `--fail-level` flag to specify the minimum severity for a non-zero exit code [#1259](https://github.com/clj-kondo/clj-kondo/issues/1259) ([@RickMoynihan](https://github.com/RickMoynihan))

### Enhanced / fixed

- Support `core.async` `defblockingop` macro [#1244](https://github.com/clj-kondo/clj-kondo/issues/1244)
- Add error message when keywords are passed in `:or` map [#1242](https://github.com/clj-kondo/clj-kondo/issues/1242)
- False positive unused default when analyzing locals [#1246](https://github.com/clj-kondo/clj-kondo/issues/1246)
- False positive when destructuring depends on previous arg [#782](https://github.com/clj-kondo/clj-kondo/issues/782)
- Keyword analysis for namespaced maps [#1251](https://github.com/clj-kondo/clj-kondo/issues/1251) ([@ericdallo](https://github.com/ericdallo))
- Report reader errors at the start of token [#1255](https://github.com/clj-kondo/clj-kondo/issues/1255) ([@yuhan0](https://github.com/yuhan0))
- Fix recur arity for lazy-seq and lazy-cat ([@yuhan0](https://github.com/yuhan0))
- Prioritize aliases over object access in CLJS [#1248](https://github.com/clj-kondo/clj-kondo/issues/1248) ([@jahson](https://github.com/jahson))

## 2021.03.31

### Enhanced / fixed

- `:defined-by` contains raw node for sgen fns [#1231](https://github.com/clj-kondo/clj-kondo/issues/1231)
- Fix wrong order of unresolved symbols [#1237](https://github.com/clj-kondo/clj-kondo/issues/1237)
- Remove generated nodes from analysis [#1239](https://github.com/clj-kondo/clj-kondo/issues/1239) ([@ericdallo](https://github.com/ericdallo))
- Add `:report-duplicates` linter config for several linters. [#1232](https://github.com/clj-kondo/clj-kondo/issues/1232) ([@snoe](https://github.com/snoe))

## 2021.03.22

### New

- `--copy-configs` flag to indicate copy configs from dependencies while linting. This replaces `--no-warnings`.
- `--dependencies` flag to indicate skipping already linted jars for performance. This replaces `--no-warnings`.

### Enhanced / fixed

- Support js property access syntax [#1189](https://github.com/clj-kondo/clj-kondo/issues/1189)
- Fix linting `user.clj` [#1190](https://github.com/clj-kondo/clj-kondo/issues/1190)
- Add linting for `sgen/lazy-prims` [#1192](https://github.com/clj-kondo/clj-kondo/issues/1192)
- NullPointerException when ignoring :deprecated-var [#1195](https://github.com/clj-kondo/clj-kondo/issues/1195)
- Fix `:lint-as` with `cond->` [#1205](https://github.com/clj-kondo/clj-kondo/issues/1205)
- Expose config to hook fns [#1208](https://github.com/clj-kondo/clj-kondo/issues/1208) ([@not-in-stock](https://github.com/not-in-stock))
- Fix crash with :clj-kondo/ignore in combination with :rename [#1210](https://github.com/clj-kondo/clj-kondo/issues/1210)
- Fix false positive unresolved symbol in CLJS type hint [#1212](https://github.com/clj-kondo/clj-kondo/issues/1212)
- Fix invalid namespace in clojure.data.xml analysis [#1202](https://github.com/clj-kondo/clj-kondo/issues/1202)
- Fix analysis of `clojure.core.reducers/defcurried` [#1217](https://github.com/clj-kondo/clj-kondo/issues/1217)
- Add `:defined-by` on missing var definitions [#1219](https://github.com/clj-kondo/clj-kondo/issues/1219) ([@ericdallo](https://github.com/ericdallo))
- Add name positions to local-usage analysis [#1220](https://github.com/clj-kondo/clj-kondo/issues/1220) ([@ericdallo](https://github.com/ericdallo))
- False positive `:unused-private-var` warning for deftype `^:private` [#1222](https://github.com/clj-kondo/clj-kondo/issues/1222)
- Correct escaping for docstrings in analysis [#1224](https://github.com/clj-kondo/clj-kondo/issues/1224) ([@lread](https://github.com/lread))

## 2021.03.03

### Enhanced / fixed

- Redundant expression false positive [#1183](https://github.com/clj-kondo/clj-kondo/issues/1183)
- Redundant expression false positive [#1185](https://github.com/clj-kondo/clj-kondo/issues/1185)
- Regression in unresolved symbol config [#1187](https://github.com/clj-kondo/clj-kondo/issues/1187)

## 2021.02.28

### New

- Lint nested function literal [#636](https://github.com/clj-kondo/clj-kondo/issues/636)
- Redundant expression linter [#298](https://github.com/clj-kondo/clj-kondo/issues/298)
- Add `:exclude` config to :refer linter [#1172](https://github.com/clj-kondo/clj-kondo/issues/1172)
- Warn on non-existent var in `:refer` [#546](https://github.com/clj-kondo/clj-kondo/issues/546)
- Support `clojure.data.xml/alias-uri`[#1180](https://github.com/clj-kondo/clj-kondo/issues/1180)

### Enhanced / fixed

- Fix schema.core/defmethod linting for vectors dispatch-values [#1175](https://github.com/clj-kondo/clj-kondo/pull/1175) ([@leoiacovini](https://github.com/leoiacovini))
- Continue analyzing on invalid symbol [#1146](https://github.com/clj-kondo/clj-kondo/issues/1146)
- Standalone require should be emitted to analysis [#1177](https://github.com/clj-kondo/clj-kondo/issues/1177)
- Upgrade sci to 0.2.4

## 2021.02.13

Thanks to [@snoe](https://github.com/snoe) and [@ericdallo](https://github.com/ericdallo) for contributing to this release. Thanks to the
sponsors on [Github](https://github.com/sponsors/borkdude),
[OpenCollective](https://opencollective.com/clj-kondo) and [Clojurists
Together](https://www.clojuriststogether.org/) for making this release possible.

### New

- Core.match support [#496](https://github.com/clj-kondo/clj-kondo/issues/496)
- Keyword analysis [#1129](https://github.com/clj-kondo/clj-kondo/issues/1129) ([@snoe](https://github.com/snoe)). See [analysis docs](https://github.com/clj-kondo/clj-kondo/blob/master/analysis/README.md).

### Enhanced / fixed

- BREAKING: Don't use lint-as for hooks [#1170](https://github.com/clj-kondo/clj-kondo/issues/1170)
- Fix crash when linting kitchen-async [#1148](https://github.com/clj-kondo/clj-kondo/issues/1148)
- Memory optimizations for clojure-lsp [commit](https://github.com/clj-kondo/clj-kondo/commit/175c48839299c445f6684fa15e5692b03c9bcb5a0)
- Upgrade to GraalVM 21.0.0 [#1163](https://github.com/clj-kondo/clj-kondo/issues/1163)
- Fix analysis of case dispatch vals [#1169](https://github.com/clj-kondo/clj-kondo/issues/1169)
- Potemkin improvement with regards to unresolved var [#1167](https://github.com/clj-kondo/clj-kondo/issues/1167)
- Exported config fix for git deps [#1171](https://github.com/clj-kondo/clj-kondo/issues/1171)
- Add `:aliases` to ns ctx and `:alias` to var-usages [#1133](https://github.com/clj-kondo/clj-kondo/issues/1133) ([@snoe](https://github.com/snoe))
- Add `:end-row` and `:end-col` to var-definitions bucket on analysis [#1147](https://github.com/clj-kondo/clj-kondo/issues/1147) ([@ericdallo](https://github.com/ericdallo))
- Fix unresolved var `clojure.spec.gen.alpha/fmap` [#1157](https://github.com/clj-kondo/clj-kondo/issues/1157)

## 2021.01.20

Thanks to [@SevereOverfl0w](https://github.com/SevereOverfl0w),
[@jysandy](https://github.com/jysandy), [@tomdl89](https://github.com/tomdl89),
[@snoe](https://github.com/snoe), [@audriu](https://github.com/audriu), and
[@ericdallo](https://github.com/ericdallo) for contributing to this release.

### New

- New linter: `:unresolved-var`. This detects unresolved vars in other namespaces, like `set/onion`. See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md#unresolved-var). [#635](https://github.com/clj-kondo/clj-kondo/issues/635)
- Alpine Docker build [#1111](https://github.com/clj-kondo/clj-kondo/issues/1111)
- Add locals to analysis [#1109](https://github.com/clj-kondo/clj-kondo/issues/1109) ([@snoe](https://github.com/snoe))
- Add analysis for arglists [#1123](https://github.com/clj-kondo/clj-kondo/issues/1123) ([@snoe](https://github.com/snoe))

### Enhanced / fixed

- Fix finding without location info [#1101](https://github.com/clj-kondo/clj-kondo/issues/1101)
- Detect duplicate key in '{[1 2] 3, (1 2) 4} [#1056](https://github.com/clj-kondo/clj-kondo/issues/1056) ([@jysandy](https://github.com/jysandy))
- Add cljs.core cases in `lint-specific-calls!` [#1116](https://github.com/clj-kondo/clj-kondo/issues/1116) ([@tomdl89](https://github.com/tomdl89))
- [#1099] Add :single-operand-logical linter for `and` and `or` [#1122](https://github.com/clj-kondo/clj-kondo/issues/1122) ([@tomdl89](https://github.com/tomdl89))
- Add `:ns` to `:unused-namespace` findings ([@ericdallo](https://github.com/ericdallo))
- Derive config dir from only file path linted [#1135](https://github.com/clj-kondo/clj-kondo/issues/1135)
- Support name in defmethod fn-tail [#1115](https://github.com/clj-kondo/clj-kondo/issues/1115)
- Avoid crash when using `:refer-clojure` + `:only` [#957](https://github.com/clj-kondo/clj-kondo/issues/957)

## v2020.12.12

### New

- Documentation: a list of all available [linters](https://github.com/clj-kondo/clj-kondo/blob/master/doc/linters.md) [#936](https://github.com/clj-kondo/clj-kondo/issues/936)
- Lint protocol and interface implementations in `deftype` and `defrecord` [#140](https://github.com/clj-kondo/clj-kondo/issues/140)
- Upgrade to GraalVM 20.3.0 [#1085](https://github.com/clj-kondo/clj-kondo/issues/1085)
- Support `cljs.core/simple-benchmark` syntax [#1079](https://github.com/clj-kondo/clj-kondo/issues/1079)
- Support `babashka.process/$` macro syntax [#1089](https://github.com/clj-kondo/clj-kondo/issues/1089)

### Enhanced / fixed

- Fix recur arity in doysync [#1081](https://github.com/clj-kondo/clj-kondo/issues/1081)
- Alias linter doesn't recognize (quote alias) form [#1074](https://github.com/clj-kondo/clj-kondo/issues/1074)
- Fix retries for `refer :all` when linting in parallel [#1068](https://github.com/clj-kondo/clj-kondo/issues/1068)
- Improve analyzing syntax of `amap` [#1069](https://github.com/clj-kondo/clj-kondo/issues/1069)
- Namespaced map in `deps.edn` causes false positive [#1093](https://github.com/clj-kondo/clj-kondo/issues/1093)
- Support ignore hints in `deps.edn` [#1094](https://github.com/clj-kondo/clj-kondo/issues/1094)
- Fix unsorted namespaces linter for nested libspecs [#1097](https://github.com/clj-kondo/clj-kondo/issues/1097)
- Fix reported ns name in analysis for nested libspecs [#1100](https://github.com/clj-kondo/clj-kondo/issues/1100)

## v2020.11.07

Thanks [@bennyandresen](https://github.com/bennyandresen),
[@jaihindhreddy](https://github.com/jaihindhreddy),
[@mharju](https://github.com/mharju), [@pepijn](https://github.com/pepijn),
[@slipset](https://github.com/slipset) and
[@nvuillam](https://github.com/nvuillam) for contributing to this
release. Thanks to [Clojurists Together](https://www.clojuriststogether.org/)
for sponsoring this release.

### New

- Lint deps.edn [#945](https://github.com/clj-kondo/clj-kondo/issues/945)
- `--filename` option to set filename when linting from stdin. This should be
  used for editor plugins to enable `deps.edn` linting.
- Export and import config via classpath [#559](https://github.com/clj-kondo/clj-kondo/issues/559), [clj-kondo/config#1](https://github.com/clj-kondo/config/issues/1)
- `--no-warnings` flag to indicate linting is used to populate cache.
- Skip already linted jars [#705](https://github.com/clj-kondo/clj-kondo/issues/705)
- Implement `:include` option for shadowed-var linter [#1040](https://github.com/clj-kondo/clj-kondo/issues/1040)
- Return `:files` count in summary [#1046](https://github.com/clj-kondo/clj-kondo/issues/1046)

### Enhanced

- Better resolving of vars referred with `:all`
  [#1010](https://github.com/clj-kondo/clj-kondo/issues/1010)
- Fix false positive with `format` [#1044](https://github.com/clj-kondo/clj-kondo/issues/1044)
- Fix index out of bounds exception
  [clj-kondo.lsp#11](https://github.com/clj-kondo/clj-kondo.lsp/issues/11)
- More robust marking of generated nodes to avoid redundant dos and lets despite
  location metadata [#1059](https://github.com/clj-kondo/clj-kondo/issues/1059)

## v2020.10.10

Thanks [@zilti](https://github.com/zilti), [@dharrigan](https://github.com/dharrigan) and [@sogaiu](https://github.com/sogaiu) for contributing to this release. Thanks to [Clojurists Together](https://www.clojuriststogether.org/) for sponsoring this release.

### New

- Shadowed var linter [#646](https://github.com/clj-kondo/clj-kondo/issues/646). See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md#shadowed-var).
- Config for ignoring unused `:as` binding
  [clj-kondo#1016](https://github.com/clj-kondo/clj-kondo/issues/1016) ([@dharrigan](https://github.com/dharrigan))
- Type warning for `contains?` [#1021](https://github.com/clj-kondo/clj-kondo/issues/1021)
- Predicate functions for hooks api [#1006](https://github.com/clj-kondo/clj-kondo/issues/1006). See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/hooks.md#api).
- Support reader conditionals in ignore hint [#1022](https://github.com/clj-kondo/clj-kondo/issues/1022). See [docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md#ignore-warnings-in-an-expression).
- Support ignore hint on unused binding [#1017](https://github.com/clj-kondo/clj-kondo/issues/1017)
- Support ignore hint in ns form [#1031](https://github.com/clj-kondo/clj-kondo/issues/1031)
- Linux [packages](https://github.com/clj-kondo/clj-kondo/blob/master/doc/install.md#linux-packages) ([@zilti](https://github.com/zilti))

### Fixed / enhanced

- Fix memory leak in long running process [#1036](https://github.com/clj-kondo/clj-kondo/issues/1036)
- Claypoole config enhancements [clj-kondo/config#7](https://github.com/clj-kondo/config/pull/7)
- Don't warn about redundant `let` and `do` in hook-generated code [#1038](https://github.com/clj-kondo/clj-kondo/issues/1038)
- Fix format string false positive [#1007](https://github.com/clj-kondo/clj-kondo/issues/1007)
- Parse failure in `(or)` [#1023](https://github.com/clj-kondo/clj-kondo/issues/1023)
- Analyze require in top-level do [#1018](https://github.com/clj-kondo/clj-kondo/issues/1018)
- Analyze quote in require [#1019](https://github.com/clj-kondo/clj-kondo/issues/1019)
- Base Docker image on Ubuntu latest [#1026](https://github.com/clj-kondo/clj-kondo/issues/1026)

## v2020.09.09

Thanks to [@cldwalker](https://github.com/cldwalker), [@bfontaine](https://github.com/bfontaine), [@snoe](https://github.com/snoe), [@andreyorst](https://github.com/andreyorst), [@jeroenvandijk](https://github.com/jeroenvandijk),
[@jaihindhreddy](https://github.com/jaihindhreddy), [@sittim](https://github.com/sittim) and [@sogaiu](https://github.com/sogaiu) for contributing to this release. Thanks to the people who helped designing the new features in Github issue conversations.  Thanks to [Clojurists Together](https://www.clojuriststogether.org/) for sponsoring this release.

### New

- Add `--parallel` option to lint sources in parallel. This will speed up
  linting an entire classpath. [#632](https://github.com/clj-kondo/clj-kondo/issues/632), [#972](https://github.com/clj-kondo/clj-kondo/issues/972)
- Detect error when calling a local that's not a function. [#948](https://github.com/clj-kondo/clj-kondo/issues/948)

    ``` clojure
    (let [inc "foo"]
      (inc 1))
      ^--- String cannot be called as a function
    ```

- Support ignore hints [#872](https://github.com/clj-kondo/clj-kondo/issues/872):

    ``` clojure
    (inc 1 2 3)
    ^--- clojure.core/inc is called with 3 args but expects 1

    #_:clj-kondo/ignore
    (inc 1 2 3)
    ^--- arity warning ignored

    #_{:clj-kondo/ignore[:invalid-arity]}
    (do (inc 1 2 3))
    ^--- only redundant do is reported, but invalid arity is ignored
    ```

  Also see [config.md](doc/config.md).

- Merge config from `$HOME/.config/clj-kondo`, respecting `XDG_CONFIG_HOME`. See
  [config.md](doc/config.md) for details. [#992](https://github.com/clj-kondo/clj-kondo/issues/992)

- New `:config-paths` option in `<project>/.clj-kondo/config.edn`. This allows
  extra configuration directories to be merged in. See
  [config.md](doc/config.md) for details. [#992](https://github.com/clj-kondo/clj-kondo/issues/992)

- [Config tool](https://github.com/clj-kondo/config) that can spit out library
  specific configurations that can be added via `:config-paths`. Contributions
  for libraries are welcome.

- Experimental [spec inspection tool](https://github.com/clj-kondo/inspector) that attempts to extract type information for linting. Also uses the new `:config-paths` feature.

- Allow pinned version in installation script [#946](https://github.com/clj-kondo/clj-kondo/issues/946) ([@cldwalker](https://github.com/cldwalker))

### Fixed

- Fix docstring in Rum `defc` hook [#960](https://github.com/clj-kondo/clj-kondo/issues/960)
- Format string checking improvements [#942](https://github.com/clj-kondo/clj-kondo/issues/942), [#949](https://github.com/clj-kondo/clj-kondo/issues/949)
- False positive with `into` and `transducer` [#952](https://github.com/clj-kondo/clj-kondo/issues/952)
- Alias usage not detected in keywords when in quoted form [#981](https://github.com/clj-kondo/clj-kondo/issues/981)
- Fully qualified class name incorrectly assumed to be var [#950](https://github.com/clj-kondo/clj-kondo/issues/950)
- Backup existing clj-kondo binary when installing [#963](https://github.com/clj-kondo/clj-kondo/issues/963) ([@bfontaine](https://github.com/bfontaine))
- Various documentation fixes and improvements ([@jeroenvandijk](https://github.com/jeroenvandijk), [@sittim](https://github.com/sittim), [@sogaiu](https://github.com/sogaiu))

### Misc

- Update to GraalVM 20.2.0 for `native-image` build [@jaihindhreddy](https://github.com/jaihindhreddy)

## Prior to v2020.09.09

Details about releases prior to v2020.09.09 can be found
[here](https://github.com/clj-kondo/clj-kondo/releases).

## Breaking changes

### Unreleased

- [#2063](https://github.com/clj-kondo/clj-kondo/issues/2063): introduce new `:defined-by->lint-as` key which contains the `:lint-as` value for "defining" var, whereas `:defined-as` now always contains the name of the original "defining var". This is a **BREAKING** change.

### 2021.09.25

- Change `:row` and `:col` for `:var-usages` to use the start location of the call instead of the name location [#1387](https://github.com/clj-kondo/clj-kondo/issues/1387)

### 2020.10.10

- Base Docker image on Ubuntu latest instead of Alpine [#1026](https://github.com/clj-kondo/clj-kondo/issues/1026)
- Don't use lint-as for hooks [#1170](https://github.com/clj-kondo/clj-kondo/issues/1170)
