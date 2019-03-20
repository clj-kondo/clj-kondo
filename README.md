# clj-kondo

A minimal and opinionated linter for code that sparks joy.

<img src="demo.png">

## Rationale

You use [inline
defs](https://blog.michielborkent.nl/2017/05/25/inline-def-debugging/) for
debugging but you would like to get rid of them before making your code
public. Also, unnessary `do` and `let` nestings don't really add any value to
your life. Let clj-kondo help you tidy your code.

## Status

This is a hobby project. Let me know if you find this
useful. I might add more features later.

## Installation

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

Lint multiple files, e.g. with `find`:

``` shellsession
$ find src -type f -name "*.clj*"  -exec clj-kondo --lint {} \;

src/clj/clojure/test.clj:496:6: warning: obsolete let
src/clj/clojure/pprint/cl_format.clj:1156:15: warning: obsolete let
src/clj/clojure/pprint/cl_format.clj:1373:4: warning: obsolete do
src/clj/clojure/stacktrace.clj:32:5: warning: obsolete let
src/clj/clojure/test/tap.clj:86:5: warning: obsolete do
src/clj/clojure/repl.clj:33:17: warning: obsolete do
src/clj/clojure/core.clj:7706:5: warning: obsolete do
```

## Editor integration

You can integrate with Emacs [`flycheck`](https://www.flycheck.org/en/latest/) as follows:

``` shellsession
(flycheck-define-checker clj-kondo
  ""
  :command ("clj-kondo" "--lint" "-")
  :standard-input t
  :error-patterns
  ((error line-start "<stdin>:" line ":" column ": " (0+ not-newline) (or "error: " "Exception: ") (message) line-end)
   (warning line-start "<stdin>:" line ":" column ": " (0+ not-newline) "warning: " (message) line-end))
  :modes (clojure-mode clojurec-mode clojurescript-mode)
  :predicate (lambda () (not (string= "edn" (file-name-extension (buffer-file-name))))))

(add-to-list 'flycheck-checkers 'clj-kondo)
```

To run multiple checkers, like [joker](https://github.com/candid82/joker)
(recommended!), you can add the option:

``` emacs-lisp
  :next-checkers ((warning . clojure-joker))
```

## Credits

This project is inspired by [joker](https://github.com/candid82/joker). It uses
[clj.native-image](https://github.com/taylorwood/clj.native-image) for compiling
the project. The parsing of Clojure code relies on
[rewrite-clj](https://github.com/xsc/rewrite-clj).

## License

Copyright © 2019 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.
