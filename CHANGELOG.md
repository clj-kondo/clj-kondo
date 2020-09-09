# Changelog

For a list of breaking changes, check [here](#breaking-changes)

## v2020.09.09

Thanks to [@cldwalker](https://github.com/cldwalker), [@bfontaine](https://github.com/bfontaine), [@snoe](https://github.com/snoe), [@andreyorst](https://github.com/andreyorst), [@jeroenvandijk](https://github.com/jeroenvandijk),
[@jaihindhreddy](https://github.com/jaihindhreddy), [@sittim](https://github.com/sittim) and [@sogaiu](https://github.com/sogaiu) for contributing to this release. Thanks to the people who helped designing the new features in Github issue conversations.  Thanks to [Clojurists Together](https://www.clojuriststogether.org/) for sponsoring this release.

### New

- Add `--parallel` option to lint sources in parallel. This will speed up
  linting an entire classpath. [#632](https://github.com/borkdude/clj-kondo/issues/632), [#972](https://github.com/borkdude/clj-kondo/issues/972)
- Detect error when calling a local that's not a function. [#948](https://github.com/borkdude/clj-kondo/issues/948)

    ``` clojure
    (let [inc "foo"]
      (inc 1))
      ^--- String cannot be called as a function
    ```

- Support ignore hints [#872](https://github.com/borkdude/clj-kondo/issues/872):

    ``` clojure
    (inc 1 2 3)
    ^--- clojure.core/inc is called with 3 args but expects 1

    #_:clj-kondo/ignore
    (inc 1 2 3)
    ^--- arity warning ignored

    #_:clj-kondo/ignore[:invalid-arity]
    (do (inc 1 2 3))
    ^--- only redundant do is reported, but invalid arity is ignored
    ```

  Also see [config.md](doc/config.md).

- Merge config from `$HOME/.config/clj-kondo`, respecting `XDG_CONFIG_HOME`. See
  [config.md](doc/config.md) for details. [#992](https://github.com/borkdude/clj-kondo/issues/992)

- New `:config-paths` option in `<project>/.clj-kondo/config.edn`. This allows
  extra configuration directories to be merged in. See
  [config.md](doc/config.md) for details.

- [Config tool](https://github.com/clj-kondo/config) that can spit out library
  specific configurations that can be added via `:config-paths`. Contributions
  for libraries are welcome.

- Allow pinned version in installation script [#946](https://github.com/borkdude/clj-kondo/issues/946) ([@cldwalker](https://github.com/cldwalker))

### Fixed

- Fix docstring in Rum `defc` hook [#960](https://github.com/borkdude/clj-kondo/issues/960)
- Format string checking improvements [#942](https://github.com/borkdude/clj-kondo/issues/942), [#949](https://github.com/borkdude/clj-kondo/issues/949)
- False positive with `into` and `transducer` [#952](https://github.com/borkdude/clj-kondo/issues/952)
- Alias usage not detected in keywords when in quoted form [#981](https://github.com/borkdude/clj-kondo/issues/981)
- Fully qualified class name incorrectly assumed to be var [#950](https://github.com/borkdude/clj-kondo/issues/950)
- Backup existing clj-kondo binary when installing [#963](https://github.com/borkdude/clj-kondo/issues/963) ([@bfontaine](https://github.com/bfontaine))
- Various documentation fixes and improvements ([@jeroenvandijk](https://github.com/jeroenvandijk), [@sittim](https://github.com/sittim), [@sogaiu](https://github.com/sogaiu))

## Misc

- Update to GraalVM 20.2.0 for `native-image` build [@jaihindhreddy](https://github.com/jaihindhreddy)

## Prior to v2020.09.09

Details about releases prior to v2020.09.09 can be found
[here](https://github.com/borkdude/clj-kondo/releases).

## Breaking changes

None yet.
