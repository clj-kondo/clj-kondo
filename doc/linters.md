# Linters

All linters have the option to disable them or set a different warning level. See [config.md](config.md).

## :invalid-arity

This linter will report function calls with an incorrect amount of arguments.

### Examples

example.clj
``` clojure
(inc 1 2)
^--- clojure.core/inc is called with 3 args but expects 1
```

### Config options

Some macros use function names in their DSL but give it a different meaning. To
turn off false positives in such macros, use this config::

```
{:linters {:invalid-arity {:skip-args [foo.bar/macro]}}}
```

such that `(foo.bar/weird-macro (inc))` doesn't give warnings.



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

### Exclude namespace in `:refer-all` linter

```clojure
{:linters {:refer-all {:exclude [alda.core]}}}
```

