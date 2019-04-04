# clj-kondo
[![CircleCI](https://circleci.com/gh/borkdude/clj-kondo/tree/master.svg?style=svg)](https://circleci.com/gh/borkdude/clj-kondo/tree/master)
[![Clojars Project](https://img.shields.io/clojars/v/clj-kondo.svg)](https://clojars.org/clj-kondo)
[![cljdoc badge](https://cljdoc.org/badge/clj-kondo/clj-kondo)](https://cljdoc.org/d/clj-kondo/clj-kondo/CURRENT)

A minimal and opinionated linter for Clojure code that sparks joy.

<img src="screenshots/demo.png" width="50%" align="right">

## Rationale

You don't mind the occasional [inline
def](https://blog.michielborkent.nl/2017/05/25/inline-def-debugging/) for
debugging, but you would like to get rid of them before making your code
public. Also, unnecessary `do` and `let` nestings don't really add any value to
your life. Let clj-kondo help you tidy your code.

## Features

* inline def warnings
* obsolete do warnings
* obsolete let warnings
* arity errors across namespaces
* private function call errors

<img src="screenshots/wrong-arity.png" width="50%" align="right">

This linter is:

* compatible with .clj, .cljs and .cljc files
* build tool and editor agnostic

## Status

Under active development, but already useful. None of the code is meant to be exposed as a
public API, except the command line interface.

For new features I'd like to focus on things that
[joker](https://github.com/candid82/joker) doesn't support yet, so I recommend
enabling that one as well.

## Installation

### MacOS:

    brew install borkdude/brew/clj-kondo

### Linux

Install [Linuxbrew](http://linuxbrew.sh/). Then run:

    brew install borkdude/brew/clj-kondo

### Manual install

Pre-built binaries are available for linux and MacOS on the
[releases](https://github.com/borkdude/clj-kondo/releases) page.

### Running with the JVM

Using the binary is recommended for better startup time, but you can run this
linter with as a regular Clojure program on the JVM as well.

#### leiningen

You can add clj-kondo to `~/.lein/profiles.clj` to make it available as a `lein` command:

``` clojure
{:user {:dependencies [[clj-kondo "RELEASE"]]
        :aliases {"clj-kondo" ["run" "-m" "clj-kondo.main"]}
```

``` shellsession
$ lein clj-kondo --lint src
```

#### tools.deps.alpha

Run clj-kondo as an ad-hoc command line dependency:

``` shellsession
$ clj -Sdeps '{:deps {clj-kondo {:mvn/version "RELEASE"}}}' -m clj-kondo.main --lint src
```

Or add it as an alias to `~/.clojure/deps.edn`:

``` clojure
{:aliases
 {:clj-kondo
  {:extra-deps {clj-kondo {:mvn/version "RELEASE"}}
   :main-opts ["-m" "clj-kondo.main"]}}}
```

``` shellsession
$ clj -A:clj-kondo --lint src
```

### Building from source

To build a binary from source, download [GraalVM](https://github.com/oracle/graal/releases) and set the
`GRAALVM_HOME` variable. E.g.:

    export GRAALVM_HOME=$HOME/Downloads/graalvm-ce-1.0.0-rc14/Contents/Home

Then clone this repo, `cd clj-kondo` and build the native binary:

    clojure -A:native-image

Place the binary somewhere on your path.

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
src/clj_kondo/test.cljs:7:1: warning: obsolete do
src/clj_kondo/vars.clj:291:3: error: Wrong number of args (1) passed to clj-kondo.vars/analyze-arities
```

Lint a project classpath:

``` shellsession
$ clj-kondo --lint $(lein classpath)
```

### Project setup

To detect lint errors across namespaces in your project, a cache is needed. To
create one, make a `.clj-kondo` directory in the root of your project. A cache
will be created inside of it when you run `clj-kondo` with the `--cache` option.
Before linting inside your editor, it is recommended to lint the entire
classpath to teach `clj-kondo` about all the libraries you are using, including
Clojure and/or ClojureScript itself:

``` shellsession
$ clj-kondo --lint <classpath> --cache
```

Build tool specific ways to get a classpath:
- `lein classpath`
- `boot with-cp -w -f`
- `clj -Spath`

So for `lein` the entire command would be:

    $ clj-kondo --lint $(lein classpath) --cache

Now you are ready to lint single files using [editor
integration](#editor-integration). A simulation of what happens when you edit a
file in your editor:

``` shellsession
$ echo '(select-keys)' | clj-kondo --lang cljs --cache --lint -
<stdin>:1:1: error: Wrong number of args (0) passed to cljs.core/select-keys
```

Since clj-kondo now knows about your version of ClojureScript via the cache,
it detects that the number of arguments you passed to `select-keys` is
invalid. Each time you edit a file, the cache is incrementally updated, so
clj-kondo is informed about new functions you just wrote.

## Editor integration

For integrating with Emacs, see
[flycheck-clj-kondo](https://github.com/borkdude/flycheck-clj-kondo).

## Tests

    clj -A:test

## Credits

This project is inspired by [joker](https://github.com/candid82/joker). It uses
[clj.native-image](https://github.com/taylorwood/clj.native-image) for compiling
the project. The parsing of Clojure code relies on
[rewrite-clj](https://github.com/xsc/rewrite-clj).

## License

Copyright © 2019 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.
