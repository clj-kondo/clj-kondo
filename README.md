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

This is a hobby project. Let me know if you find this useful. More features
might be added later. For new features I would like to focus on things that
[joker](https://github.com/candid82/joker) does't support yet, so I recommend
enabling that one as well.

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
$ find ~/git/clojure/src -type f -name "*.clj*" | xargs clj-kondo --lint
/Users/Borkdude/git/clojure/src/clj/clojure/test.clj:496:6: warning: obsolete let
/Users/Borkdude/git/clojure/src/clj/clojure/pprint/cl_format.clj:1156:15: warning: obsolete let
/Users/Borkdude/git/clojure/src/clj/clojure/pprint/cl_format.clj:1373:4: warning: obsolete do
/Users/Borkdude/git/clojure/src/clj/clojure/stacktrace.clj:32:5: warning: obsolete let
/Users/Borkdude/git/clojure/src/clj/clojure/test/tap.clj:86:5: warning: obsolete do
/Users/Borkdude/git/clojure/src/clj/clojure/repl.clj:33:17: warning: obsolete do
/Users/Borkdude/git/clojure/src/clj/clojure/core_print.clj:233:7: warning: obsolete do
/Users/Borkdude/git/clojure/src/clj/clojure/core.clj:7706:5: warning: obsolete do
```

### Running without GraalVM

Running with GraalVM is recommended for better startup time. For the less GraalVM
inclined, it's also possible to run this linter with a normal JVM:

#### leiningen

You can add `clj-kondo` to `~/.lein/profiles.clj` to make it available as a `lein` command:

``` clojure
{:user {:dependencies [[clj-kondo "0.0.1-SNAPSHOT"]]
        :aliases {"clj-kondo" ["run" "-m" "clj-kondo.main" "--lint"]}
```

``` shellsession
$ find ~/git/clojure/src -type f -name "*.clj*" | xargs lein clj-kondo
...
```

#### tools.deps.alpha

Run `clj-kondo` as an ad-hoc command line dependency:

``` shellsession
$ find ~/git/clojure/src -type f -name "*.clj*" | xargs clj -Sdeps '{:deps {clj-kondo {:git/url "https://github.com/borkdude/clj-kondo" :sha "5cd4da7bfe3f11ba7ec5e0f36af1e659b28a1ce2"}}}' -m clj-kondo.main --lint
```

Or add it as an alias to `~/.clojure/deps.edn`:

``` clojure
{:aliases
 {:clj-kondo
  {:extra-deps {clj-kondo {:git/url "https://github.com/borkdude/clj-kondo" :sha "5cd4da7bfe3f11ba7ec5e0f36af1e659b28a1ce2"}}
   :main-opts ["-m" "clj-kondo.main" "--lint"]}}}
```

``` shellsession
$ find ~/git/clojure/src -type f -name "*.clj*" | xargs clj -A:clj-kondo
```

## Editor integration

You can integrate with Emacs [`flycheck`](https://www.flycheck.org/en/latest/) as follows:

``` emacs-lisp
(flycheck-define-checker clj-kondo
  "See `https://github.com/borkdude/clj-kondo'."
  :command ("clj-kondo" "--lint" "-")
  :standard-input t
  :error-patterns
  ((error line-start "<stdin>:" line ":" column ": " (0+ not-newline) (or "error: " "Exception: ") (message) line-end)
   (warning line-start "<stdin>:" line ":" column ": " (0+ not-newline) "warning: " (message) line-end))
  :modes (clojure-mode clojurec-mode clojurescript-mode)
  :predicate (lambda () (not (string= "edn" (file-name-extension (buffer-file-name)))))
  ;; Uncomment next line when you also use the joker linter. Recommended!
  ;; :next-checkers ((warning . clojure-joker) (warning . clojurescript-joker))
  )

(add-to-list 'flycheck-checkers 'clj-kondo)
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
