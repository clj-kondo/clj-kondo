# Configuration

NOTE: parts of the config may change as clj-kondo sees more usage. Please let me
know which parts you are using and find useful. To be notified of breaking changes to the config, please add your Github name to the list at the end of this file via a PR.

clj-kondo can be configured in three ways:

- by placing a `config.edn` file in the `.clj-kondo` directory (see [project setup](../README.md#project-setup))
- by providing a `--config` file argument from the command line
- by providing a `--config` EDN argument from the command line (see examples below)

The command line argument overrides a `config.edn` file without merging.

Look into the [sample config file](../.clj-kondo/config.edn) for all the options.

## Examples

### Disable a linter

``` shellsession
$ echo '(select-keys [:a])' | clj-kondo --lint -
<stdin>:1:1: error: wrong number of args (1) passed to clojure.core/select-keys
linting took 10ms, errors: 1, warnings: 0

$ echo '(select-keys [:a])' | clj-kondo --lint - --config '{:linters {:invalid-arity {:level :off}}}'
linting took 10ms, errors: 0, warnings: 0
```

### Show progress bar while linting

``` shellsession
$ clj-kondo --lint "$(clj -Spath)" --config '{:output {:show-progress true}}'
.................................................................................................................
cljs/tools/reader.cljs:527:9: warning: redundant do
(rest of the output omitted)
```

### Include and exclude files from the output

``` shellsession
$ clj-kondo --lint "$(clj -Spath)" --config '{:output {:include-files ["^clojure/test"]}}'
clojure/test.clj:496:6: warning: redundant let
clojure/test/tap.clj:86:5: warning: redundant do
linting took 3289ms, errors: 0, warnings: 2

$ clj-kondo --lint "$(clj -Spath)" --config '{:output {:include-files ["^clojure/test"] :exclude-files ["tap"]}}'
clojure/test.clj:496:6: warning: redundant let
linting took 3226ms, errors: 0, warnings: 1
```

### Exclude arity linting inside a specific macro call

Some macros rewrite their arguments and therefore can cause false positive arity
errors. Imagine the following silly macro:

``` clojure
(ns silly-macros)

(defmacro with-map [m [fn & args]]
  `(~fn ~m ~@args))
```

which you can call like:

``` clojure
(silly-macros/with-map {:a 1 :d 2} (select-keys [:a :b :c])) ;;=> {:a 1}
```

Normally a call to this macro will give an invalid arity error for `(select-keys
[:a :b :c])`, but not when you use the following configuration:

``` clojure
{:linters {:invalid-arity {:skip-args [silly-macros/with-map]}}}
```

## Be notified of breaking config changes

- borkdude
