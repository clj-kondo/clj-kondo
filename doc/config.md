# Configuration

NOTE: parts of the config may change as clj-kondo sees more usage. Please let me
know which parts you are using and find useful.

clj-kondo can be configured in three ways:

- by placing a `config.edn` file in the `.clj-kondo` directory (see [project setup](../README.md#project-setup))
- by providing a `--config` file argument from the command line
- by providing a `--config` EDN argument from the command line (see examples below)

The command line argument overrides a `config.edn`.

Look at the [default configuration](../src/clj_kondo/impl/config.clj) for all
available options.

## Examples

### Output

#### Print results in JSON format

``` shellsession
$ clj-kondo --lint corpus --config '{:output {:format :json}}' | jq '.findings[0]'
{
  "type": "invalid-arity",
  "filename": "corpus/nested_namespaced_maps.clj",
  "row": 9,
  "col": 1,
  "level": "error",
  "message": "wrong number of args (2) passed to nested-namespaced-maps/test-fn"
}
```

Printing in EDN format is also supported.

#### Include and exclude files from the output

``` shellsession
$ clj-kondo --lint "$(clj -Spath)" --config '{:output {:include-files ["^clojure/test"]}}'
clojure/test.clj:496:6: warning: redundant let
clojure/test/tap.clj:86:5: warning: redundant do
linting took 3289ms, errors: 0, warnings: 2

$ clj-kondo --lint "$(clj -Spath)" --config '{:output {:include-files ["^clojure/test"] :exclude-files ["tap"]}}'
clojure/test.clj:496:6: warning: redundant let
linting took 3226ms, errors: 0, warnings: 1
```

#### Show progress bar while linting

``` shellsession
$ clj-kondo --lint "$(clj -Spath)" --config '{:output {:progress true}}'
.................................................................................................................
cljs/tools/reader.cljs:527:9: warning: redundant do
(rest of the output omitted)
```

### Disable a linter

``` shellsession
$ echo '(select-keys [:a])' | clj-kondo --lint -
<stdin>:1:1: error: wrong number of args (1) passed to clojure.core/select-keys
linting took 10ms, errors: 1, warnings: 0

$ echo '(select-keys [:a])' | clj-kondo --lint - --config '{:linters {:invalid-arity {:level :off}}}'
linting took 10ms, errors: 0, warnings: 0
```

### Disable all linters but one

You can accomplish this by using `^:replace` metadata, which will override
instead of merge with other configurations:

``` shellsession
$ clj-kondo --lint corpus --config '^:replace {:linters {:redundant-let {:level :info}}}'
corpus/redundant_let.clj:4:3: info: redundant let
corpus/redundant_let.clj:8:3: info: redundant let
corpus/redundant_let.clj:12:3: info: redundant let
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

### Lint a custom macro like a built-in macro

In the following code the `my-defn` macro is defined, but clj-kondo doesn't know how to interpret it:

``` clojure
(ns foo)

(defmacro my-defn [name args & body]
  `(defn ~name ~args
     (do (println "hello!")
       ~@body)))

(my-defn foo [x])
```

Hence `(foo 1 2 3)` will not lead to an invalid arity error. However, the syntax
of `my-defn` is a subset of `clojure.core/defn`, so for detecting arity errors
we might have just linted it like that. That is what the following configuration accomplishes:

``` clojure
{:lint-as {foo/my-defn clojure.core/defn}}
```

### Exclude required but unused namespace from being reported

In the following code, the namespaces `foo.specs` and `bar.specs` are only loaded for the side effect of registering specs, so we don't like clj-kondo reporting those namespaces as required but unused.

``` clojure
(ns foo (:require [foo.specs] [bar.specs]))
(defn my-fn [x] x)
```

That can be done using this config:

``` clojure
{:linters {:unused-namespace {:exclude [foo.specs bar.specs]}}}
```

A regex is also supported:

``` clojure
{:linters {:unused-namespace {:exclude [".*\\.specs$"]}}}
```

This will exclude all namespaces ending with `.specs`.

### Exclude unresolved symbols from being reported

In the following code, 

``` clojure
(ns foo
  (:require [riemann.streams [streams]]))

(def email (mailer {:host "mail.relay"
                    :from "riemann@example.com"}))
(streams
  (where (and (= (:service event) “my-service”)
              (= (:level event) “ERROR”))
    ,,,))
```

`streams` is a macro that assigns a special meaning to the symbol `where`, so it should not be reported as an unresolved symbol. This is the config for it:

``` clojure
{:linters
  {:unresolved-symbol
    {:level :error
     :exclude [(riemann.streams/streams [where])]}}}
```

To exclude all symbols in calls to `riemann.streams/streams` write `:exclude [(riemann.streams/streams)]`, without the vector.

To exclude a symbol from being reported as unresolved globally in your project, e.g. `foo`, you can use `:exclude [foo]`.

Sometimes vars are introduced by executing macros, e.g. when using [HugSQL](https://github.com/layerware/hugsql)'s `def-db-fns`. You can suppress warnings about these vars by using `declare`. Example:

``` clojure
(ns hugsql-example
  (:require [hugsql.core :as hugsql]))

(declare select-things)

;; this will define a var #'select-things:
(hugsql/def-db-fns "select_things.sql")

(defn get-my-things [conn params]
  (select-things conn params))
```

Furthermore, the `:lint-as` option can help treating certain macros like built-in ones. This is in clj-kondo's own config:

``` clojure
:lint-as {me.raynes.conch/programs clojure.core/declare
          me.raynes.conch/let-programs clojure.core/let}
```

and helps preventing false positive unresolved symbols in this code:

``` clojure
(ns foo (:require [me.raynes.conch :refer [programs let-programs]]))

(programs rm mkdir echo mv)
(let-programs [clj-kondo "./clj-kondo"]
  ,,,)
```