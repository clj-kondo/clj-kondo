# clj-kondo

A minimal and opinionated linter for Clojure code that sparks joy.

<img src="demo.png">

## Rationale

You use [inline
defs](https://blog.michielborkent.nl/2017/05/25/inline-def-debugging/) for
debugging but you would like to get rid of them before making your code
public. Also, unnecessary `do` and `let` nestings don't really add any value to
your life. Let clj-kondo help you tidy your code.

## Features

* inline def warnings
* obsolete do warnings
* obsolete let warnings
* basic arity errors
* private function call errors

## Status

Work in progress, but already useful. None of the code is meant to be exposed as a public API,
except the command line interface.

For new features I'd like to focus on things that
[joker](https://github.com/candid82/joker) doesn't support yet, so I recommend
enabling that one as well.

## Installation

(For running without GraalVM, look [here](#running-without-graalvm)).

Download [GraalVM](https://github.com/oracle/graal/releases) and set the
`GRAALVM_HOME` variable. E.g.:

    export GRAALVM_HOME=$HOME/Downloads/graalvm-ce-1.0.0-rc14/Contents/Home

Clone this repo, cd `clj-kondo` and build the native binary:

    clojure -A:native-image

Place the binary somewhere on your path.

## Usage

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
...
```

Lint a project classpath:

``` shellsession
$ clj-kondo --lint $(clj -Spath)
$ clj-kondo --lint $(lein classpath)
$ clj-kondo --lint $(boot with-cp -w -f -)
```

It is recommended to save the analysis results to a cache. This gives a better
experience when using `clj-kondo` in an editor. To do this, make a `.clj-kondo`
directory in the root of your project and use the `--cache` option:

    clj-kondo --cache --lint $(clj -Spath)

Next time you will lint a single namespace (e.g. using [editor
integration](#editor-integration)), the cache can be leveraged to detect more
errors:

``` shellsession
$ echo '(select-keys)' | clj-kondo --lang cljs --cache --lint -
<stdin>:1:1: error: Wrong number of args (0) passed to cljs.core/select-keys
```

### Running without GraalVM

Running with GraalVM is recommended for better startup time, but you can run this linter with a normal JVM as well.

#### leiningen

You can add `clj-kondo` to `~/.lein/profiles.clj` to make it available as a `lein` command:

``` clojure
{:user {:dependencies [[clj-kondo "0.0.1-SNAPSHOT"]]
        :aliases {"clj-kondo" ["run" "-m" "clj-kondo.main" "--lint"]}
```

``` shellsession
$ lein clj-kondo src 
```

#### tools.deps.alpha

Run `clj-kondo` as an ad-hoc command line dependency:

``` shellsession
$ clj -Sdeps '{:deps {clj-kondo {:git/url "https://github.com/borkdude/clj-kondo" :sha "<master/latest-sha>"}}}' -m clj-kondo.main --lint src
```

where `<master/latest-sha>` is the SHA of the latest commit on the master
branch.

Or add it as an alias to `~/.clojure/deps.edn`:

``` clojure
{:aliases
 {:clj-kondo
  {:extra-deps {clj-kondo {:git/url "https://github.com/borkdude/clj-kondo" :sha "<master/latest-sha>"}}
   :main-opts ["-m" "clj-kondo.main" "--lint"]}}}
```

``` shellsession
$ clj -A:clj-kondo src
```

## Editor integration

You can integrate with Emacs [`flycheck`](https://www.flycheck.org/en/latest/) as follows:

``` emacs-lisp
(flycheck-define-checker clj-kondo-clj
  "See https://github.com/borkdude/clj-kondo"
  :command ("clj-kondo" "--cache" "--lang" "clj" "--lint" "-")
  :standard-input t
  :error-patterns
  ((error line-start "<stdin>:" line ":" column ": " (0+ not-newline) (or "error: " "Exception: ") (message) line-end)
   (warning line-start "<stdin>:" line ":" column ": " (0+ not-newline) "warning: " (message) line-end))
  :modes (clojure-mode clojurec-mode)
  :predicate (lambda () (not (string= "edn" (file-name-extension (buffer-file-name)))))
  ;; use this when you also use the joker linter, recommended!
  :next-checkers ((error . clojure-joker) (warning . clojure-joker)))

(flycheck-define-checker clj-kondo-cljs
  "See https://github.com/borkdude/clj-kondo"
  :command ("clj-kondo" "--cache" "--lang" "cljs" "--lint" "-")
  :standard-input t
  :error-patterns
  ((error line-start "<stdin>:" line ":" column ": " (0+ not-newline) (or "error: " "Exception: ") (message) line-end)
   (warning line-start "<stdin>:" line ":" column ": " (0+ not-newline) "warning: " (message) line-end))
  :modes (clojurescript-mode)
  :predicate (lambda () (not (string= "edn" (file-name-extension (buffer-file-name)))))
  ;; use this when you also use the joker linter, recommended!
  :next-checkers ((error . clojurescript-joker) (warning . clojurescript-joker)))

(add-to-list 'flycheck-checkers 'clj-kondo-clj)
(add-to-list 'flycheck-checkers 'clj-kondo-cljs)
```

This code was adapted from [flycheck-joker](https://github.com/candid82/flycheck-joker).

## Credits

This project is inspired by [joker](https://github.com/candid82/joker). It uses
[clj.native-image](https://github.com/taylorwood/clj.native-image) for compiling
the project. The parsing of Clojure code relies on
[rewrite-clj](https://github.com/xsc/rewrite-clj).

## License

Copyright © 2019 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.
