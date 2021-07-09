<img src="logo/logo-300dpi.png" width="400px">

[![Clojars Project](https://img.shields.io/clojars/v/clj-kondo.svg)](https://clojars.org/clj-kondo)
[![Financial Contributors on Open Collective](https://opencollective.com/clj-kondo/all/badge.svg?label=financial+contributors)](https://opencollective.com/clj-kondo)
[![CircleCI](https://circleci.com/gh/clj-kondo/clj-kondo/tree/master.svg?style=shield)](https://circleci.com/gh/clj-kondo/clj-kondo/tree/master)
[![Build status](https://ci.appveyor.com/api/projects/status/3jdro7mu17nv3rb3/branch/master?svg=true)](https://ci.appveyor.com/project/clj-kondo/clj-kondo/branch/master)
[![cljdoc badge](https://cljdoc.org/badge/clj-kondo/clj-kondo)](https://cljdoc.org/d/clj-kondo/clj-kondo/CURRENT)
[![project chat](https://img.shields.io/badge/slack-join_chat-brightgreen.svg)](https://clojurians.slack.com/messages/CHY97NXE2)
[![twitter](https://img.shields.io/badge/twitter-%23cljkondo-blue)](https://twitter.com/search?q=%23cljkondo&src=typed_query&f=live)

A linter for Clojure code that sparks joy.

<img src="screenshots/demo.png" width="50%" align="right">

<blockquote class="twitter-tweet" data-lang="en">
    <p lang="en" dir="ltr">Thanks a lot for clj-kondo. It is like a companion for me. It has made clojure fun again.</p>
    &mdash;
    <a href="https://github.com/geraldodev">@geraldodev</a> on Clojurians Slack
</blockquote>

## Rationale

Clj-kondo performs static analysis on Clojure, ClojureScript and EDN, without the need of a running REPL. It informs you about potential errors while you are typing.

## Features

Clj-kondo detects:

* inline `def` expressions
* redundant `do` and `let` wrappings
* arity errors:
  - within the same namespace and across namespaces
  - of static Java method calls
  - of local `let` and `letfn` binding calls
  - of recursive calls (including `recur`)
* unused private vars
* private and deprecated var usage
* required but unused namespaces
* unsorted required namespaces
* referred but unused vars
* duplicate requires
* unused function arguments and let bindings
* unused imports
* redefined vars
* unresolved symbols, vars and namespaces
* misplaced docstrings
* duplicate map keys and set elements
* missing map keys
* invalid number of forms in binding vectors
* missing assertions in `clojure.test/deftest`
* alias consistency
* [type checking](doc/types.md)
* Datalog syntax checking
* format string argument mismatches
* shadowed vars

before your form hits the REPL.

It suggests several style guide recommendations, such as:

* rules from Stuart Sierra's [how to ns](https://stuartsierra.com/2016/clojure-how-to-ns.html)
* use `:else` as the catch-all test expression in `cond` (see [Clojure style guide](https://github.com/bbatsov/clojure-style-guide#else-keyword-in-cond))
* use `seq` instead of `(not (empty? ,,,))` (see [Clojure style guide](https://github.com/bbatsov/clojure-style-guide#nil-punning))

<img src="screenshots/wrong-arity.png" width="50%" align="right">

It has support for syntax of commonly used macros like
`clojure.core.async/alt!!`, `schema.core/defn` and `potemkin/import-vars`.

It detects common errors in `deps.edn`

It provides [analysis data](analysis) so you build your own custom linters.

View all available linters [here](doc/linters.md).

This linter is:

* compatible with `.clj`, `.cljs`, `.cljc` and `.edn` files
* build tool and editor agnostic
* a static code analyzer
* compiled to native code using GraalVM

Try clj-kondo at the [interactive playground](https://clj-kondo.michielborkent.nl).

Watch the talk:

[![Clj-kondo at ClojuTRE 2019](https://img.youtube.com/vi/MB3vVREkI7s/0.jpg)](https://www.youtube.com/watch?v=MB3vVREkI7s)

## [Installation](doc/install.md)

## [Running on the JVM](doc/jvm.md)

## [Running with Docker](doc/docker.md)

## Usage

### Command line

Lint from stdin:

``` shellsession
$ echo '(def x (def x 1))' | clj-kondo --lint -
<stdin>:1:8: warning: inline def
```

Lint a file:

``` shellsession
$ echo '(def x (def x 1))' > /tmp/foo.clj
$ clj-kondo --lint /tmp/foo.clj
/tmp/foo.clj:1:8: warning: inline def
```

Lint a directory:

``` shellsession
$ clj-kondo --lint src
src/clj_kondo/test.cljs:7:1: warning: redundant do
src/clj_kondo/calls.clj:291:3: error: Wrong number of args (1) passed to clj-kondo.calls/analyze-calls
```

Lint a project classpath:

``` shellsession
$ clj-kondo --lint "$(lein classpath)"
```

## Project setup

To detect lint errors across namespaces in your project, a cache is needed. To
let clj-kondo know where to create one, make a `.clj-kondo` directory in the
root of your project, meaning on the same level as your `project.clj`,
`deps.edn` or `build.boot`. A cache will be created inside of it when you run
`clj-kondo`.  Before linting inside your editor, it is recommended to lint the
entire classpath to teach `clj-kondo` about all the libraries you are using,
including Clojure and/or ClojureScript itself:

``` shellsession
$ clj-kondo --lint "<classpath>" --dependencies --parallel --copy-configs
```

The `--dependencies` flag indicates that clj-kondo is used to analyze sources to
populate the cache. When enabled, clj-kondo will suppress warnings and skips over
already linted `.jar` files for performance.

The `--parallel` option will use multiple threads to lint your sources, going through them faster.

The `--copy-configs` flag will search and copy configurations from dependencies into the
`.clj-kondo` directory, while linting (see
[config.md](doc/config.md#exporting-and-importing-configuration)).

Build tool specific ways to get a classpath:
- `lein classpath`
- `boot with-cp -w -f -`
- `clojure -Spath`

So for `lein` the entire command would be:

    $ clj-kondo --lint "$(lein classpath)" --dependencies --parallel --copy-configs

Now you are ready to lint single files using [editor
integration](doc/editor-integration.md). A simulation of what happens when you
edit a file in your editor:

``` shellsession
$ echo '(select-keys)' | clj-kondo --lang cljs --lint -
<stdin>:1:1: error: Wrong number of args (0) passed to cljs.core/select-keys
```

Since clj-kondo now knows about your version of ClojureScript via the cache,
it detects that the number of arguments you passed to `select-keys` is
invalid. Each time you edit a file, the cache is incrementally updated, so
clj-kondo is informed about new functions you just wrote.

If you want to use a different directory to read and write the cache, use the
`--cache-dir` option. To disable the cache even if you have a `.clj-kondo`
directory, use `--cache false`.

## [Configuration](doc/config.md)

## [Editor integration](doc/editor-integration.md)

## Exit codes

Exit codes can be controlled by the `--fail-level <level>` option. The
default fail level is `warning` which returns exit codes as follows:

- `0`: no errors or warnings were found
- `2`: one or more warnings were found
- `3`: one or more errors were found

If `--fail-level error` is supplied, warnings do not lead to a non-zero exit code:

- `0`: no errors were found
- `0`: one or more warnings were found
- `3`: one or more errors were found

All exit codes other than `0`, `2` and `3` indicate an error because of a bug in
clj-kondo or some other unexpected error beyond the control of clj-kondo.

## [CI Integration](doc/ci-integration.md)
## [Analysis data](analysis)

## [Developer documentation](doc/dev.md)

## [Companies](doc/companies.md) using clj-kondo

## Babashka pod

Clj-kondo can be invoked as a [babashka
pod](https://github.com/babashka/babashka.pods).

``` clojure
#!/usr/bin/env bb
(ns script
  (:require [babashka.pods :as pods]))

(pods/load-pod "clj-kondo")
(require '[pod.borkdude.clj-kondo :as clj-kondo])

(clj-kondo/merge-configs
 '{:linters {:unresolved-symbol {:exclude [(foo1.bar)]}}}
 '{:linters {:unresolved-symbol {:exclude [(foo2.bar)]}}})
;;=> {:linters {:unresolved-symbol {:exclude [(foo1.bar) (foo2.bar)]}}}

(-> (clj-kondo/run! {:lint ["src"]})
    :summary)
;;=> {:error 0, :warning 0, :info 0, :type :summary, :duration 779}
```

## Podcast

+ [defnpodcast](https://soundcloud.com/defn-771544745)
+ [ClojureScript Podcast](https://clojurescriptpodcast.com/)

## Thanks to:

- [joker](https://github.com/candid82/joker) for inspiration
- [rewrite-clj](https://github.com/xsc/rewrite-clj) for the Clojure parser code
- [eastwood](https://github.com/jonase/eastwood) for `var-info.edn` and inspiration
- [contributors](https://github.com/clj-kondo/clj-kondo/graphs/contributors) and
  other users posting issues with bug reports and ideas
- [Nikita Prokopov](https://github.com/tonsky) for the logo
- [adgoji](https://www.adgoji.com/) for financial support
- [Clojurists Together](https://www.clojuriststogether.org/) for sponsoring work
  on hooks

## Contributors

### Code Contributors

This project exists thanks to all the people who contribute. [[Contribute](doc/dev.md)].

<a href="https://github.com/clj-kondo/clj-kondo/graphs/contributors"><img src="https://opencollective.com/clj-kondo/contributors.svg?width=890&button=false" /></a>

### Financial Contributors

#### Github Sponsors

- [Robert Stuttaford](https://github.com/robert-stuttaford)

#### OpenCollective
Become a financial contributor and help us sustain our community. [[Contribute](https://opencollective.com/clj-kondo/contribute)]

##### Individuals

<a href="https://opencollective.com/clj-kondo"><img src="https://opencollective.com/clj-kondo/individuals.svg?width=890"></a>

##### Organizations

Support this project with your organization. Your logo will show up here with a link to your website. [[Contribute](https://opencollective.com/clj-kondo/contribute)]

<a href="https://opencollective.com/clj-kondo/organization/0/website"><img src="https://opencollective.com/clj-kondo/organization/0/avatar.svg"></a>

## License

Copyright © 2019 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.

The directory `inlined` contains source from [`tools.reader`](https://github.com/clojure/tools.reader) which is licensed under the EPL license.

The directory `parser` contains modified source from [`rewrite-clj`](https://github.com/xsc/rewrite-clj) which is licensed under the MIT license.
