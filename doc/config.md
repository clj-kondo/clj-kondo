# Configuration

Table of contents:

- [Introduction](#introduction)
- [Libraries](#libraries)
- [Unrecognized macros](#unrecognized-macros)
- [Linters](#linters)
- [Hooks](#hooks)
- [Output](#output)
- [Example configurations](#example-configurations)
- [Exporting and importing configuration](#exporting-and-importing-configuration)
- [Deprecations](#deprecations)

## Introduction

Clj-kondo can be configured in five ways, by providing:

- home dir config in `~/.config/clj-kondo/config.edn` (respects `XDG_CONFIG_HOME`)
- project config: a `config.edn` file in the `.clj-kondo` directory (see
  [project setup](../README.md#project-setup))
- `:config-paths` in project `config.edn`: a list of directories that provide additional config
- command line `--config` file or EDN arguments
- namespace local config using `:clj-kondo/config` metadata in the namespace form

The configurations are merged in the following order, where a later config overrides an earlier config:

- home dir config
- `:config-paths` in project config
- project config
- command line config
- namespace local config

The `^:replace` metadata hint can be used to replace parts or all of the
configuration instead of merging with previous ones. The home dir config is
implicitly part of `:config-paths`. To opt out of merging with home dir config
use `:config-paths ^:replace []` in your project config.

Note that namespace local config must always be quoted: `{:clj-kondo/config
'{:linters ...}}` and quotes should not appear inside the config.

Look at the [default configuration](../src/clj_kondo/impl/config.clj) for all
available options.

## Examples

See the [config](https://github.com/clj-kondo/config) project for library-specific configurations.

## Unrecognized macros

Clj-kondo only expands a selected set of macros from clojure.core and some
popular community libraries. For unrecognized macros you can use these
configurations:

- [`:lint-as`](#lint-a-custom-macro-like-a-built-in-macro)
- [`:unresolved-symbol`](#exclude-unresolved-symbols-from-being-reported)
- [`:hooks`](#hooks)

## Linters

Each linter is identified by a keyword in the `:linters` config. We will start
with general linter configutation and will then enumerate all available linters
and corresponding config options.

### Enabling and disabling

#### Disable a linter

``` shellsession
$ echo '(select-keys [:a])' | clj-kondo --lint -
<stdin>:1:1: error: wrong number of args (1) passed to clojure.core/select-keys
linting took 10ms, errors: 1, warnings: 0

$ echo '(select-keys [:a])' | clj-kondo --lint - --config '{:linters {:invalid-arity {:level :off}}}'
linting took 10ms, errors: 0, warnings: 0
```

#### Enable optional linters

Some linters are not enabled by default. Right now these linters are:

- `:missing-docstring`: warn when public var doesn't have a docstring.
- `:unsorted-required-namespaces`: warn when namespaces in `:require` are not sorted.
- `:refer`: warn when there is **any** usage of `:refer` in your namespace requires.
- `:single-key-in`: warn when using assoc-in, update-in or get-in with single key.
- `:shadowed-var`: warn when a binding shadows a var.

You can enable these linters by setting the `:level`:

``` clojure
{:linters {:missing-docstring {:level :warning}}}
```

#### Disable all linters but one

You can accomplish this by using `^:replace` metadata, which will override
instead of merge with other configurations:

``` shellsession
$ clj-kondo --lint corpus --config '^:replace {:linters {:redundant-let {:level :info}}}'
corpus/redundant_let.clj:4:3: info: redundant let
corpus/redundant_let.clj:8:3: info: redundant let
corpus/redundant_let.clj:12:3: info: redundant let
```

### Ignore warnings in an expression

To ignore all warnings in an expression, place a hint before it. It uses reader
comments, so they won't end up in your runtime.

``` clojure
#_:clj-kondo/ignore
(inc 1 2 3)
```

To ignore warnings from just one or a few linters:

``` clojure
#_{:clj-kondo/ignore [:invalid-arity]}
(inc 1 2 3)
```

To ignore warnings for only one language in a reader conditional:

``` clojure
#_{:clj-kondo/ignore #?(:clj [:unused-binding] :cljs [])}
(defn foo [x]
  #?(:cljs x)) ;; x is only used in cljs, but unused is ignored for clj, so no warning
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

When you have custom `def` or `defn`-like macros and you can't find a supported macro that is like it, you can use:

``` clojure
{:lint-as {foo/my-defn clj-kondo.lint-as/def-catch-all}}
```

### Available linters

#### Cond-else

Keyword: `:cond-else`.

Description: warn on `cond` with a different constant for the else branch than `:else`.

Default level: `:warning`.

Example trigger: `(cond (odd? (rand-int 10)) :foo :default :bar)`.

Example message: `use :else as the catch-all test expression in cond`.

#### Conflicting-alias

Keyword: `:conflicting-alias`.

Description: warn on conflicting alias.

Default level: `:error`.

Example trigger:

``` clojure
(require '[clojure.string :as s]
         '[clojure.spec.alpha :as s])
```

Example message: `Conflicting alias for clojure.spec.alpha`.

#### Datalog syntax

Keyword: `:datalog-syntax`.

Description: warn on invalid datalog syntax. This linter is implemented using
[io.lambdaforge/datalog-parser](https://github.com/lambdaforge/datalog-parser). Also
see this [blog
post](https://lambdaforge.io/2019/11/08/clj-kondo-datalog-support.html).

Default level: `:error`.

Example trigger:

``` clojure
(ns user (:require [datahike.api :refer [q]]))

(q '[:find ?a :where [?b :foo _]] 42)
```

Example message: `Query for unknown vars: [?a]`.

#### Deprecated var

Keyword: `:deprecated-var`.

Description: warn on usage of var that is deprecated.

Default level: `:warning`.

Example trigger: `(def ^:deprecated x) x`

Example warning: `#'user/x is deprecated`.

Config:

Say you have the following function:

``` clojure
(ns app.foo)
(defn foo {:deprecated "1.9.0"} [])
```

and you still want to be able to call it without getting a warning, for example
in function in the same namespace which is also deprecated:

``` clojure
(defn bar {:deprecated "1.9.0"} []
  (foo))
```

or in test code:

``` clojure
(ns app.foo-test
  (:require
   [app.foo :refer [foo]]
   [clojure.test :refer [deftest is]]))

(deftest foo-test [] (is (nil? (foo))))
```

To achieve this, use this config:

``` clojure
{:linters
 {:deprecated-var
  {:exclude
   {app.foo/foo
    {:defs [app.foo/bar]
     :namespaces [app.foo-test]}}}}}
```

A regex is also permitted, e.g. to exclude all test namespaces:

``` clojure
{:linters {:deprecated-var {:exclude {app.foo/foo {:namespaces [".*-test$"]}}}}}
```

#### Duplicate map key

Keyword: `:duplicate-map-key`.

Description: warn on duplicate key in map.

Default level: `:error`.

Example trigger: `{:a 1 :a 2}`

Example message: `duplicate key :a`.

#### Duplicate require

Keyword: `:duplicate-require`.

Description: warns on namespace that has been required more than once within a namespace.

Example trigger:

``` clojure
(ns foo
  (:require [clojure.string :as str]
            [clojure.string :as str]))
```

Example message: `duplicate require of clojure.string`

#### Duplicate set key

Keyword: `:duplicate-set-key`.

Description: similar to `:duplicate-map-key` but for sets.

Example trigger: `#{:a :a}`

Example message: `duplicate set element :a`.

#### File

Keyword: `:file`.

Description: warn on error while reading file.

Default level: `:error`.

Example trigger: `clj-kondo --lint foo.clje`.

Example message: `file does not exist`.

#### Inline def

Keyword: `:inline-def`.

Description: warn on non-toplevel usage of `def` (and `defn`, etc.).

Default level: `:warning`.

Example trigger: `(defn foo [] (def x 1))`.

Example message: `inline def`.

#### Invalid arity

*Keyword:* `:invalid-arity`.

Description: warn when a function (or macro) is called with an invalid amount of
arguments.

Default level: `:error`.

Example trigger: `(inc)`.

Example message: `clojure.core/inc is called with 0 args but expects 1`.

Config:

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

#### Misplaced docstring

Keyword: `:misplaced-docstring`.

Description: warn when docstring appears after argument vector instead of before.

Default level: `:warning`.

Example trigger: `(defn foo [] "cool fn" 1)`.

Example message: `Misplaced docstring.`

#### Missing else branch

Keyword: `:missing-else-branch`.

Description: warns about missing else branch in `if` expression.

Default level: `:warning`.

Example trigger: `(if :foo :bar)`.

Example message: `Missing else branch..`

#### Missing map value

Keyword: `:missing-map-value`.

Description: warn on key with uneven amount of elements, i.e. one of the keys
misses a value.

Default level: `:error`.

Example trigger: `{:a 1 :b}`

Example message: `missing value for key :b`.

#### Missing test assertion

Keyword: `:missing-test-assertion`.

Description: warn on `deftest` expression without test assertion.

Default level: `:warning`.

Example trigger:

``` clojure
(require '[clojure.test :as test])
(test/deftest foo (pos? 1))
```

Example message: `missing test assertion`.

#### Not empty?

Keyword: `:not-empty?`

Description: warn on `(not (empty? ...))` idiom. According to the docstring of `empty?` `seq` is prefered.

Default level: `:warning`.

Example trigger: `(not (empty? []))`

Example message: `use the idiom (seq x) rather than (not (empty? x))`.

#### Private call

Keyword `:private-call`.

Description: warn when private var is used. The name of this linter should be
renamed to "private usage" since it will warn on usage of private vars and not
only inside calls.

Default level: `:error`.

Example trigger:

``` clojure
(ns foo) (defn- f [])

(ns bar (:require [foo]))
(foo/f)
```

Example message: `#'foo/f is private`.

To suppress the above message, refer to `foo/f` using the var `#'foo/f` or write:

``` shellsession
#_{:clj-kondo/ignore [:private-call]}
(foo/f)
```

#### Redefined var

Keyword: `:redefined-var`.

Description: warn on redefind var.

Default level: `:warning`.

Example trigger: `(def x 1) (def x 2)`

Example message: `redefined var #'user/x`.

#### Redundant do

Keyword: `:redundant-do`.

Description: warn on usage of do that is redundant. The warning usually arises
because of an explicit or implicit do as the direct parent s-expression.

Default level: `:warning`.

Example trigger: `(defn foo [] (do 1))`.

Example message: `redundant do`.

#### Redundant let

Keyword: `:redundant-let`.

Description: warn on usage of let that is redundant. The warning usually arises
because directly nested lets.

Default level: `:warning`.

Example trigger: `(let [x 1] (let [y 2] (+ x y)))`.

Example message: `Redundant let expression.`

#### Refer

Keyword: `:refer`

Description: warns when `:refer` is used. This can be used when one wants to
enforce usage of aliases.

Default level: `:off`.

Example trigger: `(ns foo (:require [clojure.set :refer [union]]))`.

Example warning: `require with :refer`.

#### Refer all

Keyword: `:refer-all`

Description: warns when `:refer :all` is used.

Default level: `:warning`.

Example trigger: `(ns foo (:require [clojure.set :refer :all]))`.

Example warning: `use alias or :refer`.

#### Syntax

Keyword: `:syntax`.

Description: warn on invalid syntax.

Default level: `:warning`.

Example trigger: `[)`.

Example messages:

```
Mismatched bracket: found an opening [ and a closing ) on line 1
Mismatched bracket: found an opening [ on line 1 and a closing )
```

#### Unbound destructuring default

Keyword: `:unbound-destructuring-default`.

Description: warn on binding in `:or` which does not occur in destructuring.

Default level: `:warning`.

Example trigger: `(let [{:keys [:i] :or {i 2 j 3}} {}] i)`

Example message: `j is not bound in this destructuring form`.

#### Unused binding

Keyword: `:unused-binding`.

Description: warn on unused binding.

Default level: `:warning`.

Example trigger: `(let [x 1] (prn :foo))`

Example message: `unused binding x`.

Config:

To exclude unused bindings from being reported, start their names with
underscores: `_x`.

To exclude warnings about key-destructured function arguments, use:

``` clojure
{:linters {:unused-binding {:exclude-destructured-keys-in-fn-args true}}}
```

This will disable warnings for the following example:

``` clojure
(defn f [{:keys [:a :b :c]} d])
```

To disable warnings about `:as` bindings (which can be useful for
documentation), use:

```clojure
{:linters {:unused-binding {:exclude-destructured-as true}}}
```

This will disable the warning in:

``` clojure
(defn f [{:keys [a b c] :as g}] a b c)
```

#### Unreachable code

Keyword: `:unreachable-code`.

Description: warn on unreachable code.

Default level: `:warning`.

Example trigger: `(cond :else 1 (odd? 1) 2)`.

Example message: `unreachable code`.

#### Unresolved symbol

Keyword: `:unresolved-symbol`.

Default level: `:error`.

Example trigger: `x`.

Example message: `unresolved symbol x`.

Config:

In the following code `streams` is a macro that assigns a special meaning to the
symbol `where`, so it should not be reported as an unresolved symbol:

``` clojure
(ns foo
  (:require [riemann.streams :refer [streams]]))

(def email (mailer {:host "mail.relay"
                    :from "riemann@example.com"}))
(streams
  (where (and (= (:service event) “my-service”)
              (= (:level event) “ERROR”))
    ,,,))
```

This is the config for it:

``` clojure
{:linters
  {:unresolved-symbol
    {:exclude [(riemann.streams/streams [where])]}}}
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

If the amount of symbols introduced by HugSQL becomes too unwieldy, consider
introducing a separate namespace in which HugSQL generates the vars:
`foo.db.hugsql`. You can then refer to this namespace from `foo.db` with
`(require '[foo.db.hugsql :as sql]) (sql/insert! ...)` and clj-kondo will not
complain about this.

Furthermore, the `:lint-as` option can help treating certain macros like
built-in ones. This is in clj-kondo's own config:

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

#### Unsorted required namespace

Keyword: `:unsorted-required-namespace`.

Description: warns on non-alphabetically sorted libspecs in `ns` and `require` forms.

Default level: `:off`.

Example trigger: `(ns foo (:require b a))`.

Example message: `Unsorted namespace: a`.

#### Unused namespace

Keyword: `:unused-namespace`.

Description: warns on required but unused namespace.

Default level: `:warning`.

Example trigger: `(ns foo (:require [bar :as b]))`.

Example message: `namespace bar is required but never used`.

Config:

Given this example:

``` clojure
(ns foo (:require [foo.specs :as specs]))
```

you will get a warning about `foo.specs` being unused.

To suppress this, you can either leave out the alias `specs` if it isn't used
anywhere in the namespace or use this config:

``` clojure
{:linters {:unused-namespace {:exclude [foo.specs]}}}
```

A regex is also supported:

``` clojure
{:linters {:unused-namespace {:exclude [".*\\.specs$"]}}}
```

This will exclude all namespaces ending with `.specs`.

### Unused private var

Keyword: `:unused-private-var`.

Description: warns on unused private vars.

Default level: `:warning`.

Example trigger: `(ns foo) (defn- f [])`

Example message: `Unused private var foo/f`

Config:

To suppress the above warning:

``` clojure
{:linters {:unused-private-var {:exclude [foo/f]}}}
```

#### Unused referred var

Keyword: `:unused-referred-var`.

Description: warns about unused referred vars.

Default level: `:warning`.

Example trigger: `(ns foo (:require [clojure.set :refer [union]]))`.

Example message: `#'clojure.set/union is referred but never used`.

Config:

Imagine you want to have `taoensso.timbre/debug` available in all of your
namespaces. Even when you don't use it, you don't want to get a warning about
it. That can be done as follows:

``` clojure
{:linters {:unused-referred-var {:exclude {taoensso.timbre [debug]}}}}
```

#### Use

Keyword: `:use`.

Description: warns about `:use` or `use`.

Default level: `:warning`.

Example trigger: `(ns foo (:use [clojure.set]))`.

Example message: `use :require with alias or :refer`.

### Exclude unresolved namespaces from being reported

``` clojure
(ns foo
  {:clj-kondo/config '{:linters {:unresolved-namespace {:exclude [criterium.core]}}}})

(criterium.core/quick-bench [])
```

### Alias consistency

Sometimes it's desirable to have a consistent alias for certain namespaces in a project. E.g. in the below code it could be desirable if every alias for `old.api` was `old-api`:

``` clojure
(ns foo (:require [new.api :as api]))
(ns bar (:require [old.api :as old-api]))
(ns baz (:require [old.api :as api]))
```

This configuration:

``` clojure
{:linters {:consistent-alias {:aliases {old.api old-api}}}}
```

will give this warning:

``` clojure
Inconsistent alias. Expected old-api instead of api.
```

### Ignore the contents of comment forms

If you prefer not to lint the contents of `(comment ...)` forms, use this configuration:

```clojure
{:skip-comments true}
```

### Exclude namespace in `:refer-all` linter

```clojure
{:linters {:refer-all {:exclude [alda.core]}}}
```

### Shadowed var

The `:shadowed-var` linter warns when a binding shadows a var.

Example config:

``` clojure
{:linters {:shadowed-var {:level :warning
                          :exclude [ns]
                          :suggest {name nom}}}}
```

``` clojure
(fn [name] name)
     ^--- Shadowed var: clojure.core/name. Suggestion: nom
```

Use `:exclude` to suppress warnings for specific binding names. Use `:include`
to warn only for specific names.

To avoid shadowing core vars you can also use `:refer-clojure` + `:exclude` in
the `ns` form.

## Hooks

See [hooks.md](hooks.md).

## Output

### Print results in JSON format


``` shell
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

### Print results with a custom format

``` shell
$ clj-kondo --lint corpus --config '{:output {:pattern "::{{level}} file={{filename}},line={{row}},col={{col}}::{{message}}"}}'
::warning file=corpus/compojure/core.clj,line=2,col=19::Unsorted namespace: foo
```

The custom pattern supports these template values:

| Template Variable | Notes                                                     |
|-------------------|-----------------------------------------------------------|
| `{{filename}}`    | File name                                                 |
| `{{row}}`         | Row where linter violation starts                         |
| `{{col}}`         | Column where linter violation starts                      |
| `{{level}}`       | Lowercase level of linter warning, one of info,warn,error |
| `{{LEVEL}}`       | Uppercase variant of `{{level}}`                          |
| `{{message}}`     | Linter message                                            |

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

### Show progress bar while linting

``` shellsession
$ clj-kondo --lint "$(clj -Spath)" --config '{:output {:progress true}}'
.................................................................................................................
cljs/tools/reader.cljs:527:9: warning: redundant do
(rest of the output omitted)
```

### Output canonical file paths

The config `'{:output {:canonical-paths true}}'` will output canonical file
paths (absolute file paths without `..`). This also shows the full path of a jar
file when you lint a classpath.

``` shellsession
$ clj-kondo --lint corpus --config '{:output {:canonical-paths true}}'
/Users/borkdude/dev/clj-kondo/corpus/cljc/datascript.cljc:8:1: error: datascript.db/seqable? is called with 2 args but expects 1
(rest of the output omitted)
```

## Example configurations

These are some example configurations used in real projects. Feel free to create a PR with yours too.

- [clj-kondo](https://github.com/borkdude/clj-kondo/blob/master/.clj-kondo/config.edn)
- [rewrite-cljc](https://github.com/lread/rewrite-cljs-playground/blob/master/.clj-kondo/config.edn)

Also see the [config](https://github.com/clj-kondo/config) project.

## Exporting and importing configuration

Libraries can export configuration on the classpath. When a users lints using a
project classpath, these configurations are automatically detected and imported
into the `.clj-kondo` directory. To export config, make sure there is a
directory in your library with the following structure:

``` shellsession
clj-kondo.exports/<your-org>/<your-libname>
```

The [clj-kondo/config](https://github.com/clj-kondo/config) repo has
configurations and hook code for several libraries:

``` shellsession
$ tree -d -L 3 resources
resources
└── clj-kondo.exports
    └── clj_kondo
        ├── claypoole
        ├── fulcro
        ├── mockery
        ├── rum
        └── slingshot
```

Note that this library uses the org `clj-kondo` to not conflict with
configurations that the orgs of the libraries themselves might use for exporting
configuration. If the `claypoole` library itself wanted to export config, the
structure might look like:

``` shellsession
resources
└── clj-kondo.exports
    └── com.climate
        └── claypoole
```

Suppose you would have [clj-kondo/config](https://github.com/clj-kondo/config)
on your classpath and linted like this:

``` shellsession
$ clj-kondo --no-warnings --lint "$(clojure -Spath -Sdeps '{:deps {clj-kondo/config {:git/url "https://github.com/clj-kondo/config" :sha "e2e156c53c6c228fee7242629b41013f3e55051d"}}}')"
Copied config to .clj-kondo/clj-kondo/claypoole. Consider adding clj-kondo/claypoole to :config-paths in .clj-kondo/config.edn.
...
```

When configurations are found, instructions are printed how to opt in to those,
namely by adding the imported configs to `:config-paths` in
`.clj-kondo/config.edn`, like so:

``` shellsession
{:config-paths ["clj-kondo/claypoole"]}
```

Imported configurations can be checked into source control, at your convenience.

Note: configuration is only copied when all of these requirements are met:

- There is a `.clj-kondo` directory that can be used as a target.
- The `--no-warnings` flag is used to indicate that clj-kondo is used to populate the cache.

## Deprecations

Some configuration keys have been renamed over time. The default configuration
is always up-to-date and we strive to mantain backwards compatibility. However,
for completeness, you can find a list of the renamed keys here.

- `:if -> :missing-else-branch`
