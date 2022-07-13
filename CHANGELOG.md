# Changelog

For a list of breaking changes, check [here](#breaking-changes).

## Unreleased

- [#1749](https://github.com/clj-kondo/clj-kondo/issues/1749): expose `clojure.pprint/pprint` to the hooks API
- [#698](https://github.com/clj-kondo/clj-kondo/issues/698): output rule name with new output option `:show-rule-name-in-message true`. See example in [config guide](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md#show-rule-name-in-message).
- [#1735](https://github.com/clj-kondo/clj-kondo/issues/1735) Add support for nilable map type specs
- [#1744](https://github.com/clj-kondo/clj-kondo/issues/1744) Expose `:imported-ns` in analysis of vars imported by potemkin
- [#1746](https://github.com/clj-kondo/clj-kondo/issues/1746) Printing deps.edn error to stdout

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
- [#1676](https://github.com/clj-kondo/clj-kondo/pull/1674): Add support for custom function to be called for progress update, `:file-analyzed-fn`.
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
- [#1549](https://github.com/clj-kondo/clj-kondo/issues/1560): lint task definition keys in `bb.edn` ([@mknoszlig](https://github.com/mknoszlig))
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

### 2021.09.25

- Change `:row` and `:col` for `:var-usages` to use the start location of the call instead of the name location [#1387](https://github.com/clj-kondo/clj-kondo/issues/1387)

### 2020.10.10

- Base Docker image on Ubuntu latest instead of Alpine [#1026](https://github.com/clj-kondo/clj-kondo/issues/1026)
- Don't use lint-as for hooks [#1170](https://github.com/clj-kondo/clj-kondo/issues/1170)
