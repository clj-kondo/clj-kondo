# Changelog

For a list of breaking changes, check [here](#breaking-changes).

## Unreleased

### New

- core.match support [#496](https://github.com/clj-kondo/clj-kondo/issues/496)

### Enhanced / fixed

- Fix crash when linting kitchen-async [#1148](https://github.com/clj-kondo/clj-kondo/issues/1148)

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

### 2020.10.10

- Base Docker image on Ubuntu latest instead of Alpine [#1026](https://github.com/clj-kondo/clj-kondo/issues/1026)
