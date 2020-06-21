# Configuration

Table of contents:

- [Introduction](#introduction)
- [Libraries](#libraries)
- [Unrecognized macros](#unrecognized-macros)
- [Linters](#linters)
- [Hooks](#hooks)
- [Output](#output)
- [Example configurations](#example-configurations)
- [Deprecations](#deprecations)

## Introduction

Clj-kondo can be configured in four ways, by providing:

- a `config.edn` file in the `.clj-kondo` directory (see [project setup](../README.md#project-setup))
- a `--config` file argument from the command line
- a `--config` EDN argument from the command line (see examples below)
- a namespace local configuration using `:clj-kondo/config` metadata in the namespace form

<!-- <img src="../screenshots/compojure-config.png"> -->

Config takes precedence in the order of namespace, command line,
`.clj-kondo/config.edn`. Note that not all linters are currently supported in
namespace local configuration. Also note that namespace local config must always be quoted.

Look at the [default configuration](../src/clj_kondo/impl/config.clj) for all
available options.

## Libraries

See [libraries](../libraries) for library-specific configurations.

## Unrecognized macros

Clj-kondo only expands a selected set of macros from clojure.core and some
popular community libraries. For unrecognized macros you can use these
configurations:

- [`:lint-as`](#lint-a-custom-macro-like-a-built-in-macro)
- [`:unresolved-symbol`](#exclude-unresolved-symbols-from-being-reported)
- [`:hooks`](#hooks)

## Linters

### Disable a linter

``` shellsession
$ echo '(select-keys [:a])' | clj-kondo --lint -
<stdin>:1:1: error: wrong number of args (1) passed to clojure.core/select-keys
linting took 10ms, errors: 1, warnings: 0

$ echo '(select-keys [:a])' | clj-kondo --lint - --config '{:linters {:invalid-arity {:level :off}}}'
linting took 10ms, errors: 0, warnings: 0
```

### Enable optional linters

Some linters are not enabled by default. Right now these linters are:

- `:missing-docstring`: warn when public var doesn't have a docstring.
- `:unsorted-required-namespaces`: warn when namespaces in `:require` are not sorted.
- `:refer`: warn when there is **any** usage of `:refer` in your namespace requires.
- `:single-key-in`: warn when using assoc-in, update-in or get-in with single key

You can enable these linters by setting the `:level`:

``` clojure
{:linters {:missing-docstring {:level :warning}}}
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

### Exclude unresolved symbols from being reported

In the following code `streams` is a macro that assigns a special meaning to the symbol `where`, so it should not be reported as an unresolved symbol:

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

### Exclude unresolved namespaces from being reported

``` clojure
(ns foo
  {:clj-kondo/config '{:linters {:unresolved-namespace {:exclude [criterium.core]}}}})

(criterium.core/quick-bench [])
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

### Exclude unused referred vars from being reported.

Imagine you want to have `taoensso.timbre/debug` available in all of your
namespaces. Even when you don't use it, you don't want to get a warning about
it. That can be done as follows:

``` clojure
{:linters {:unused-referred-var {:exclude {taoensso.timbre [debug]}}}}
```

### Exclude deprecated var usage from being reported

Say you have the following function:

``` clojure
(ns app.foo)
(defn foo {:deprecated "1.9.0"} [])
```

and you still want to be able to call it without getting a warning, for example in function in the same namespace which is also deprecated:

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

### Exclude unused bindings from being reported

To exclude unused bindings from being reported, start their names with
underscores: `_x`. To exclude warnings about key-destructured function arguments, use:

``` clojure
{:linters {:unused-binding {:exclude-destructured-keys-in-fn-args true}}}
```

Examples:

``` clojure
$ echo '(defn f [{:keys [:a :b :c]} d])' | clj-kondo --lint -
<stdin>:1:18: warning: unused binding a
<stdin>:1:21: warning: unused binding b
<stdin>:1:24: warning: unused binding c
<stdin>:1:29: warning: unused binding d
linting took 8ms, errors: 0, warnings: 4
```

``` clojure
$ echo '(defn f [{:keys [:a :b :c]} _d])' | clj-kondo --lint - --config \
  '{:linters {:unused-binding {:exclude-destructured-keys-in-fn-args true}}}'
linting took 8ms, errors: 0, warnings: 0
```

### Exclude unused private vars from being reported

Example code:

``` clojure
(ns foo) (defn- f [])
```

Example config:

``` clojure
{:linters {:unused-private-var {:exclude [foo/f]}}}
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

## Hooks

Hooks are a way to enhance linting with user provided code. The following hooks are available:

- [`:analyze-call`](#analyze-call)

### analyze-call

The `analyze-call` hook can be used for:

- Transforming a node that represents a macro or function call. This is useful
  for teaching clj-kondo about custom macros.
- Inspecting arguments and emitting findings about them.

#### Transformation

As an example, let's take this macro:

``` clojure
(ns mylib)
(defmacro with-bound [binding-vector & body] ,,,)
```

Users can call this macro like:

``` clojure
(require '[my-lib])
(my-lib/with-bound [a 1 {:with-bound/setting true}] (inc a))
```

Clj-kondo does not recognize this syntax and will report the symbol `a` as
unresolved. If the macro didn't expect an option map in the third position of
the binding vector, we could have used `:lint-as {my-lib.with-bound
clojure.core/let}`, but unfortunately that doesn't work for this macro. We will
now write a hook that transforms the call into:

``` clojure
(let [a 1] {:with-bound/setting true} (inc a))
```

It is not important that the code is rewritten exactly according to the
macroexpansion. What counts is that the transformation rewrites into code that
clj-kondo can understand.

This is the code for the hook:

``` clojure
(ns hooks.with-bound
  (:require [clj-kondo.hooks-api :as api]))

(defn with-bound [{:keys [:node]}]
  (let [[binding-vec & body] (rest (:children node))
        [sym val opts] (:children binding-vec)]
    (when-not (and sym val)
      (throw (ex-info "No sym and val provided" {})))
    (let [new-node (api/list-node
                    (list*
                     (api/token-node 'let)
                     (api/vector-node [sym val])
                     opts
                     body))]
      {:node new-node})))
```

This code will be placed in a file `hooks/with_bound.clj` in your `.clj-kondo`
directory.

To register the hook, use this configuration:

``` clojure
{:hooks {:analyze-call {my-lib/with-bound hooks.with-bound/with-bound}}}
```

The symbol `hooks.with-bound/with-bound` corresponds to the file
`.clj-kondo/hooks/with-bound.clj` and the `with-bound` function defined in
it. Note that the file has to declare a namespace corresponding to its directory
structure and file name, just like in normal Clojure.

An analyze-call hook function receives a node in its argument map. It will use
the `clj-kondo.hooks-api` namespace to rewrite this node into a new node.  The
node data structures and related functions are based on the
[rewrite-clj](https://github.com/xsc/rewrite-clj) library. Clj-kondo has a
slightly modified version of rewrite-clj which strips away all whitespace,
because whitespace is not something clj-kondo looks at.

The `with-bound` hook function checks if the call has at least a `sym` and `val`
node. If not, it will throw an exception, which will result into a clj-kondo warning.

As a last step, the hook function constructs a new node using `api/list-node`,
`api/token-node` and `api/vector-node`. This new node is returned in a map under
the `:node` key.

Now clj-kondo fully understands the `my-lib/with-bound` macro and you will no
longer get false positives when using it. Moreover, it will report unused
bindings and will give warnings customized to this macro.

<p align="center">
  <img src="../screenshots/hooks-with-bound.png"/>
</p>

#### Custom lint warnings

Analyze-call hooks can also be used to create custom lint warnings, without
transforming the original node.

This is an example for re-frame's `dispatch` function which checks if the
dispatched event used a qualified keyword.

``` clojure
(ns hooks.re-frame
  (:require [clj-kondo.hooks-api :as api]))

(defn dispatch [{:keys [:node]}]
  (let [sexpr (api/sexpr node)
        event (second sexpr)
        kw (first event)]
    (when (and (vector? event)
               (keyword? kw)
               (not (qualified-keyword? kw)))
      (let [{:keys [:row :col]} (some-> node :children second :children first meta)]
        (api/reg-finding! {:message "keyword should be fully qualified!"
                           :type :re-frame/keyword
                           :row row
                           :col col})))))
```

The hook uses the `api/sexpr` function to convert the rewrite-clj node into a
Clojure s-expression, which is easier to analyze. In case of an unqualified
keyword we register a finding with `api/reg-finding!` which has a `:message`,
and `:type`. The `:type` should also occur in the clj-kondo configuration with a
level set to `:info`, `:warning` or `:error` in order to appear in the output:

``` clojure
{:linters {:re-frame/keyword {:level :warning}}
 :hooks {:analyze-call {re-frame.core/dispatch hooks.re-frame/dispatch}}}
```

Additionally, the finding has `:row` and `:col`,
derived from the node's metadata to show the finding at the appropriate
location.

<img src="../screenshots/re-frame-hook.png"/>

More examples of hooks can be found in the [libraries](../libraries)
directory. Take a look at the [Rum](../libraries/rum) and [Slingshot](../libraries/slingshot) configuration.

The initial work on hooks was sponsored by [Clojurists
Together](https://www.clojuriststogether.org/) as part of their [Summer of
Bugs](https://www.clojuriststogether.org/news/announcing-summer-of-bugs/)
program.

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

## Deprecations

Some configuration keys have been renamed over time. The default configuration is always up-to-date and we strive to mantain backwards compatibility. However, for completeness, you can find a list of the renamed keys here. 

- `:if -> :missing-else-branch`
