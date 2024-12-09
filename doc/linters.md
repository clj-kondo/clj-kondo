## Linters

This page contains an overview of all available linters and their corresponding
configuration. For general configurations options, go [here](config.md).

<!-- markdown-toc start - Don't edit this section. Run M-x markdown-toc-refresh-toc -->
**Table of Contents**

- [Linters](#linters)
    - [Aliased namespace symbol](#aliased-namespace-symbol)
    - [Aliased namespace var usage](#aliased-namespace-var-usage)
    - [Case](#case)
    - [Case duplicate test](#case-duplicate-test)
        - [Case quoted test](#case-quoted-test)
        - [Case symbol test constant](#case-symbol-test-constant)
    - [Clj-kondo config](#clj-kondo-config)
    - [Cond-else](#cond-else)
    - [Condition always true](#condition-always-true)
    - [Conflicting-alias](#conflicting-alias)
    - [Consistent-alias](#consistent-alias)
    - [Datalog syntax](#datalog-syntax)
    - [Deprecated var](#deprecated-var)
    - [Deprecated namespace](#deprecated-namespace)
    - [Deps.edn](#depsedn)
    - [Bb.edn](#bbedn)
        - [Bb.edn dependency on undefined task](#bbedn-dependency-on-undefined-task)
        - [Bb.edn cyclic task dependency](#bbedn-cyclic-task-dependency)
        - [Bb.edn Unexpected key](#bbedn-unexpected-key)
        - [Bb.edn task docstring missing](#bbedn-task-docstring-missing)
    - [Discouraged var](#discouraged-var)
    - [Discouraged namespace](#discouraged-namespace)
    - [Discouraged tag](#discouraged-tag)
    - [Docstring blank](#docstring-blank)
    - [Docstring no summary](#docstring-no-summary)
    - [Docstring leading trailing whitespace](#docstring-leading-trailing-whitespace)
    - [Duplicate map key](#duplicate-map-key)
    - [Duplicate require](#duplicate-require)
    - [Duplicate set key](#duplicate-set-key)
    - [Duplicate field name](#duplicate-field-name)
    - [Dynamic vars](#dynamic-vars)
        - [Dynamic var not earmuffed](#dynamic-var-not-earmuffed)
        - [Earmuffed var not dynamic](#earmuffed-var-not-dynamic)
    - [Quoted case test constant](#quoted-case-test-constant)
    - [Equals expected position](#equals-expected-position)
    - [Equals false](#equals-false)
    - [Equals true](#equals-true)
    - [File](#file)
    - [Format](#format)
    - [Def + fn instead of defn](#def--fn-instead-of-defn)
    - [Destructured or binding of same map](#destructured-or-binding-of-same-map)
    - [Inline def](#inline-def)
    - [Invalid arity](#invalid-arity)
    - [Conflicting arity](#conflicting-arity)
    - [Reduce without initial value](#reduce-without-initial-value)
    - [Loop without recur](#loop-without-recur)
    - [Line length](#line-length)
    - [Keyword in binding vector](#keyword-in-binding-vector)
    - [Main without gen-class](#main-without-gen-class)
    - [Minus one](#minus-one)
    - [Misplaced docstring](#misplaced-docstring)
    - [Missing body in when](#missing-body-in-when)
    - [Missing clause in try](#missing-clause-in-try)
    - [Missing docstring](#missing-docstring)
    - [Missing else branch](#missing-else-branch)
    - [Missing map value](#missing-map-value)
    - [Missing test assertion](#missing-test-assertion)
    - [Namespace name mismatch](#namespace-name-mismatch)
    - [Nil return from if-like forms](#nil-return-from-if-like-forms)
    - [Non-arg vec return type hint](#non-arg-vec-return-type-hint)
    - [Not empty?](#not-empty)
    - [Plus one](#plus-one)
    - [Private call](#private-call)
    - [Protocol method varargs](#protocol-method-varargs)
    - [Redefined var](#redefined-var)
    - [Var same name except case](#var-same-name-except-case)
    - [Redundant do](#redundant-do)
    - [Redundant call](#redundant-call)
    - [Redundant fn wrapper](#redundant-fn-wrapper)
    - [Redundant ignore](#redundant-ignore)
    - [Redundant nested call](#redundant-nested-call)
    - [Redundant let](#redundant-let)
    - [Redundant str call](#redundant-str-call)
    - [Refer](#refer)
    - [Refer all](#refer-all)
    - [Schema misplaced return](#schema-misplaced-return)
    - [Self-requiring namespace](#self-requiring-namespace)
    - [Single key in](#single-key-in)
    - [Single logical operand](#single-logical-operand)
    - [Single operand comparison](#single-operand-comparison)
    - [Shadowed fn param](#shadowed-fn-param)
    - [Shadowed var](#shadowed-var)
    - [Static field call](#static-field-call)
    - [Syntax](#syntax)
    - [Type mismatch](#type-mismatch)
    - [Unbound destructuring default](#unbound-destructuring-default)
    - [Uninitialized var](#uninitialized-var)
    - [Unused alias](#unused-alias)
    - [Unused binding](#unused-binding)
    - [Unused value](#unused-value)
    - [Used underscored bindings](#used-underscored-bindings)
    - [Unknown :require option](#unknown-require-option)
    - [Unreachable code](#unreachable-code)
    - [Unused import](#unused-import)
    - [Unresolved namespace](#unresolved-namespace)
    - [Unresolved symbol](#unresolved-symbol)
        - [:exclude-patterns](#exclude-patterns)
    - [Unresolved var](#unresolved-var)
    - [Unsorted imports](#unsorted-imports)
    - [Unsorted required namespaces](#unsorted-required-namespaces)
    - [Unused namespace](#unused-namespace)
    - [Unused private var](#unused-private-var)
    - [Unused referred var](#unused-referred-var)
    - [Use](#use)
    - [Warn on reflection](#warn-on-reflection)
    - [Underscore in namespace](#underscore-in-namespace)

<!-- markdown-toc end -->

### Aliased namespace symbol

*Keyword:* `:aliased-namespace-symbol`.

*Description:* warn when the namespace of a qualified symbol has a defined alias.

*Default level:* `:off`.

*Example trigger:*

```
(ns foo
  (:require [clojure.string :as str]))

(clojure.string/join ", " (range 10))
```

*Example message:* `An alias is defined for clojure.string: str`.

*Config:* to suppress the above warning:

```clojure
{:linters {:aliased-namespace-symbol {:exclude [clojure.string]}}}
```

### Aliased namespace var usage

*Keyword:* `:aliased-namespace-var-usage`.

*Description:* warn when a var from a namespace that was used with `:as-alias` is used.

*Default level:* `:warning`.

*Example trigger:*

```
(ns foo
  (:require [clojure.data.xml :as-alias xml]))

(xml/parse-str "<foo/>")
```

*Example message:* `Namespace only aliased but wasn't loaded: clojure.data.xml`

### Case

### Case duplicate test

*Keyword:* `:case-duplicate-test`.

*Description:* identify duplicate case test constants.

*Default level:* `:error`.

*Example trigger:* `(case x :a 1 :b 2 :a 3)`

*Example message:* `Duplicate case test constant: :a`.

#### Case quoted test

*Keyword:* `:case-quoted-test`

*Description:* Warn on quoted test constants in `case`, a common mistake when
users don't yet understand that test constants in case are not evaluated.

*Default level:* `:warning`

*Example trigger:*

``` clojure
(case 'x
  'x 1)
```

*Example message:* `Case test is compile time constant and should not be quoted.`

#### Case symbol test constant

*Keyword:* `:case-symbol-test`

*Description:* Warn on symbol test constants in `case`. Sometimes this is
intentional, but often users expect the symbol to be evaluated. To avoid this
confusion, enable this opt-in linter. Another reason to enable it might this
extra corner case in
[CLJS-2209](https://clojure.atlassian.net/browse/CLJS-2209). To opt out after
enabling this linter, you can prepend the `case` expression with
`#_{:clj-kondo/ignore [:case-symbol-test]}`.

*Default level:* `:off`

*Example trigger:*

``` clojure
(let [x 1]
  (case x
    x 1))
```

*Example message:* `Case test symbol is compile time constant and is never evaluated.`

### Clj-kondo config

*Keyword:* `:clj-kondo-config`

*Description:* warn on common errors in `.clj-kondo/config` files

*Default level:* `:warning`

*Example trigger:*

`.clj-kondo/config.edn`:

``` clojure
{:linters {:foo 1}}
```

*Example message:* `Unexpected linter name: :foo`.

### Cond-else

*Keyword:* `:cond-else`.

*Description:* warn on `cond` with a different constant for the else branch than `:else`.

*Default level:* `:warning`.

*Example trigger:* `(cond (odd? (rand-int 10)) :foo :default :bar)`.

*Example message:* `use :else as the catch-all test expression in cond`.

### Condition always true

*Keyword:* `:condition-always-true`.

*Description:* warn on a condition that evaluates to an always truthy constant,
like when passing a function instead of calling it. This linter intentionally
doesn't check for literally `true` values of vars since this is often a dev/production setting.

*Default level:* `:off` (will be `:warning` in a future release).

*Example trigger:* `(if odd? :odd :even)`.

*Example message:* `Condition always true`.

### Conflicting-alias

*Keyword:* `:conflicting-alias`.

*Description:* warn on conflicting alias.

*Default level:* `:error`.

*Example trigger:*

``` clojure
(require '[clojure.string :as s]
         '[clojure.spec.alpha :as s])
```

*Example message:* `Conflicting alias for clojure.spec.alpha`.

### Consistent-alias

*Keyword:* `:consistent-alias`

*Description:* Sometimes it's desirable to have a consistent alias for certain
namespaces in a project. E.g. in the below code it could be desirable if every
alias for `old.api` was `old-api`:

*Default level:* `:warning`.

*Example trigger:*

``` clojure
(ns foo (:require [new.api :as api]))
(ns bar (:require [old.api :as old-api]))
(ns baz (:require [old.api :as api]))
```

*Config:*

The consistent alias linter needs pre-configured aliases for namespaces that
should have a consistent alias. This configuration:

``` clojure
{:linters {:consistent-alias {:aliases {old.api old-api}}}}
```

will produce this warning:

``` clojure
Inconsistent alias. Expected old-api instead of api.
```

### Datalog syntax

*Keyword:* `:datalog-syntax`.

*Description:* warn on invalid datalog syntax. This linter is implemented using
[io.lambdaforge/datalog-parser](https://github.com/lambdaforge/datalog-parser). Also
see this [blog
post](https://lambdaforge.io/2019/11/08/clj-kondo-datalog-support.html).

*Default level:* `:error`.

*Example trigger:*

``` clojure
(ns user (:require [datahike.api :refer [q]]))

(q '[:find ?a :where [?b :foo _]] 42)
```

*Example message:* `Query for unknown vars: [?a]`.

### Deprecated var

*Keyword:* `:deprecated-var`.

*Description:* warn on usage of var that is deprecated.

*Default level:* `:warning`.

*Example trigger:* `(def ^:deprecated x) x`

Example warning: `#'user/x is deprecated`.

*Config:*

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

### Deprecated namespace

*Keyword:* `:deprecated-namespace`.

*Description:* warn on usage of namespace that is deprecated.

*Default level:* `:warning`.

*Example trigger:*

``` clojure
(ns foo {:deprecated true})
(def x 1)

(ns bar (:require [foo]))
```

Example warning: `Namespace foo is deprecated.`.

*Config:*

To exclude warnings about specific namespaces, use:

``` clojure
{:linters {:deprecated-namespace {:exclude [the-deprecated.namespace]}}}
```

### Deps.edn

*Keyword:* `:deps.edn`

*Description:* warn on common errors in `deps.edn` and `bb.edn` files.

*Default level:* `:warning`

*Example trigger:*

`deps.edn`:

``` clojure
{:deps {foo/bar "2020.10.11"}}
```

*Example message:*

```
Expected map, found: java.lang.String
```

### Bb.edn

#### Bb.edn dependency on undefined task

*Keyword:* `:bb.edn-undefined-task`

*Description:* warn on taks undefined task dependencies in `bb.edn` files.

*Default level:* `:error`

*Example trigger:*

`bb.edn`:

``` clojure
{:tasks {run {:depends [compile]}}}
```

*Example message:*

```
Depending on undefined task: compile
```

#### Bb.edn cyclic task dependency

*Keyword:* `:bb.edn-cyclic-task-dependency`

*Description:* warn on cyclic dependencies `bb.edn` files.

*Default level:* `:error`

*Example trigger:*

`bb.edn`:

``` clojure
{:tasks {a {:depends [b]
            :task (println "a")}
         b {:depends [a]}}}
```

*Example message:*

```
Cyclic task dependency: a -> b -> a
```

#### Bb.edn Unexpected key

*Keyword:* `:bb.edn-unexpected-key`

*Description:* warn on unexpected keys in bb.edn

*Default level:* `:warning`

*Example trigger:*

`bb.edn`:

``` clojure
{:requires [[babashka.fs :as fs]]}
```

*Example message:*

```
Global :requires belong in the :tasks map.
```

#### Bb.edn task docstring missing

*Keyword:* `:bb.edn-task-missing-docstring`

*Description:* warn on missing docstring for map tasks.

*Default level:* `:off`

*Example trigger:*

`bb.edn`:

``` clojure
{:tasks {a {:task (call-fn}]}
```

*Example message:*

```
Docstring missing for task: a
```

### Discouraged var

*Keyword*: `:discouraged-var`

*Description:* warn on the usage of a var that is discouraged to be used.

*Default level:* `:warning`

*Config:*

``` clojure
{:linters {:discouraged-var {clojure.core/read-string {:message "Use edn/read-string instead of read-string"}}}}
```

The matching namespace symbol may be given a group name using a regex
pattern. The warning can be made undone on the namespace level (e.g. via
`:config-in-ns` or ns metadata) by providing `:level` on the var level:

``` clojure
{:linters {:discouraged-var {clojure.core/read-string {:level :off}}}}
```

*Example trigger:*

With the configuration above:

``` clojure
(read-string "(+ 1 2 3)")
```

*Example message:*

```
Use edn/read-string instead of read-string
```

### Discouraged namespace

*Keyword*: `:discouraged-namespace`

*Description:* warn on the require or usage of a namespace that is discouraged to be used.

*Default level:* `:warning`

*Config:*

```clojure
{:linters {:discouraged-namespace {clojure.java.jdbc {:message "Use next.jdbc instead of clojure.java.jdbc"}}}}
```

The matching namespace symbol may be given a group name using a regex pattern.

```clojure
{:ns-groups [{:pattern "clojure\\.java\\.jdbc.*"
              :name jdbc-legacy}]
 :linters {:discouraged-namespace {jdbc-legacy {:message "Use next.jdbc instead of clojure.java.jdbc"}}}}
```

Add `:discouraged-namespace` linter into `:config-in-ns` to specify that specific namespaces are discouraged to be used in some namespace of ns-group.

```clojure
{:config-in-ns {app.jdbc {:linters {:discouraged-namespace {clojure.java.jdbc {:message "Use next.jdbc instead of clojure.java.jdbc"}}}}}}
```

*Example trigger:*

With the configuration above:

```clojure
(require '[clojure.java.jdbc :as j])
```

### Discouraged tag

*Keyword:* `:discouraged-tag`

*Description:* warn on the usage of a tagged literal that is discouraged to be used.

*Default level:* `:warning`

*Config:*

```clojure
{:linters {:discouraged-tag {inst {:message "Prefer #java-time/instant" }}}}
```

*Example trigger:*

Given the above configuration:

```clojure
{:date #inst "2020"}
```

*Example message:*

```
Prefer #java-time/instant
```

### Docstring blank

*Keyword:* `:docstring-blank`.

*Description:* warn on blank docstring.

*Default level:* `:warning`.

*Example trigger:* `(defn foo "" [a b] 1)`

*Example message:* `Docstring should not be blank.`.

### Docstring no summary

*Keyword:* `:docstring-no-summary`.

*Description:* warn when first _line_ of docstring is not a complete
sentence. This linter is based on the community [style
guide](https://guide.clojure.style/#docstring-summary).

Explanation by Bozhidar Batsov:

> The idea is simple - each docstring should start with a one-line
> sentence. This minimizes the work tools have to do to extract some meaningful
> summary of what a var does (and as a bonus - it plays great with the Emacs
> minibuffer, that happens to have a height of 1 line).

*Default level:* `:off`.

*Example trigger:* `(defn foo "not a sentence" [a b] 1)`

*Example message:* `First line of the docstring should be a capitalized sentence ending with punctuation.`

### Docstring leading trailing whitespace

*Keyword:* `:docstring-leading-trailing-whitespace`.

*Description:* warn when docstring has leading or trailing whitespace

*Default level:* `:off`.

*Example trigger:* `(defn foo "Has trailing whitespace.\n" [a b] 1)`

*Example message:* `Docstring should not have leading or trailing whitespace.`

### Duplicate map key

*Keyword:* `:duplicate-map-key`.

*Description:* warn on duplicate key in map.

*Default level:* `:error`.

*Example trigger:* `{:a 1 :a 2}`

*Example message:* `duplicate key :a`.

### Duplicate require

*Keyword:* `:duplicate-require`.

*Description:* warns on namespace that has been required more than once within a namespace.

*Example trigger:*

``` clojure
(ns foo
  (:require [clojure.string :as str]
            [clojure.string :as str]))
```

*Example message:* `duplicate require of clojure.string`

### Duplicate set key

*Keyword:* `:duplicate-set-key`.

*Description:* similar to `:duplicate-map-key` but for sets.

*Example trigger:* `#{:a :a}`

*Example message:* `duplicate set element :a`.

### Duplicate field name

*Keyword:* `:duplicate-field-name`.

*Description:* identify duplicate fields in deftype/defrecord fields definition.

*Default level:* `:error`.

*Example trigger:* `(deftype T [x y z y])`

*Example message:* `Duplicate field name: y`.

### Dynamic vars

#### Dynamic var not earmuffed

*Keyword:* `:dynamic-var-not-earmuffed`

*Description:* warn when dynamic var doesn't have an earmuffed name.

*Default level:* `:off`.

*Example trigger:* `(def ^:dynamic foo)`

*Example message:* `"Var is declared dynamic but name is not earmuffed: foo"`

#### Earmuffed var not dynamic

*Keyword:* `:earmuffed-var-not-dynamic`

*Description:* warn when var with earmuffed name isn't declared dynamic.

*Default level:* `:warning`.

*Example trigger:* `(def *foo*)`

*Example message:* `"Var has earmuffed name but is not declared dynamic: *foo*"`

### Quoted case test constant

*Keyword:* `:quoted-case-test-constant`.

*Description:* warn when encountering quoted test case constants.

*Default level:* `:warning`.

*Example trigger:* `(case x 'a 1 :b 2)`

*Example message:* `Case test is compile time constant and should not be quoted.`

### Equals expected position

*Keyword:* `:equals-expected-position`

*Description:* warn on usage of `=` with the expected value, a constant, that is not in the expected (first by default) position

*Default level:* `:off`

*Example trigger:* `(= (+ 1 2 3) 6)`

*Example message:* `Write expected value first`

*Config:*

The default configuration for this linter is:

``` clojure
{:linters {:equals-expected-position {:level :off
                                      :position :first
                                      :only-in-test-assertion false}}}
```

Possible values for `:position` are `:first` and `:last`
The `:only-in-test-assertion` boolean activates the linter only in a test assertion context, e.g. `(clojure.test/is (= (+ 1 2 3) 1))`

### Equals false

*Keyword:* `:equals-false`

*Description:* warn on usage of `(= false x)` or `(= x false)` rather than `(false? x)`

*Default level:* `:off`

*Example trigger:* `(fn [x] (= false x))`

*Example message:* `Prefer (false? x) over (= false x)`.

### Equals true

*Keyword:* `:equals-true`

*Description:* warn on usage of `(= true x)` or `(= x true)` rather than `(true? x)`

*Default level:* `:off`

*Example trigger:* `(fn [x] (= true x))`

*Example message:* `Prefer (true? x) over (= true x)`.

### File

*Keyword:* `:file`.

*Description:* warn on error while reading file.

*Default level:* `:error`.

*Example trigger:* `clj-kondo --lint foo.clje`.

*Example message:* `file does not exist`.

### Format

*Keyword:* `:format`.

*Description:* warn on unexpected amount of arguments in `format`.

*Default level:* `:error`.

*Example trigger:* `(format "%s" 1 2)`.

*Example message:* `Format string expects 1 arguments instead of 2.`.

### Def + fn instead of defn

*Keyword:* `:def-fn`.

*Description:* tells about closures defined with the combination of
`def` and `fn` with optional `let` in-between. In almost all cases
`defn` can be used instead which has the benefit of adding `:arglists` metadata to vars.

The practice of using `defn` instead of `def` + `fn` has the following benefits:

- `:argslists*` metadata on the var
- more readable stacktraces

*Default level:* `:off`.

*Example triggers:*

- `(def f (fn [] nil))` which can be written as: `(defn f [] nil)`).
- `(def f (let [y 1] (fn [x] (+ x y))))`  which can be written as `(let [y 1] (defn f [x] (+ x y)))`.

*Example messages:*

- `Use defn instead of def + fn`

*Config:*

``` clojure
{:linters {:def-fn {:level :warning}}}
```

*More info:*

See [issue](https://github.com/clj-kondo/clj-kondo/issues/1920).

### Destructured or binding of same map

*Keyword:* `:destructured-or-binding-of-same-map`

*Description:* an `:or` default value refers to a destructured binding of the
same map. This may result in ambigious behavior since the order of bindings is
undefined. See [this](https://github.com/clj-kondo/clj-kondo/issues/916) issue
for more details and discussion.

*Default level:* `:warning`

*Example triggers:*

- `(fn [x {:keys [a b] :or {b a}}])`. Note that this works in Clojure, but
  relies on incidental behavior of how destructuring is implemented. If you
  reverse the order of `a` and `b` in `:keys` you will get an error from the
  Clojure compiler: `(fn [x {:keys [b a] :or {b a}}])` It is better to not rely
  on this behavior at all since adding bindings or changing the order of
  bindings will break it.

*Example messages:*

- `Destructured :or refers to binding of same map: a`

*Config:*

``` clojure
{:linters {:destructured-or-binding-of-same-map {:level :warning}}}
```

### Inline def

*Keyword:* `:inline-def`.

*Description:* warn on non-toplevel usage of `def` (and `defn`, etc.).

*Default level:* `:warning`.

*Example trigger:* `(defn foo [] (def x 1))`.

*Example message:* `inline def`.

### Invalid arity

**Keyword:** `:invalid-arity`.

*Description:* warn when a function (or macro) is called with an invalid amount of
arguments.

*Default level:* `:error`.

*Example trigger:* `(inc)`.

*Example message:* `clojure.core/inc is called with 0 args but expects 1`.

*Config:*

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

### Conflicting arity

**Keyword:** `:conflicting-fn-arity`.

*Description:* warn when an overloaded function defines multiple argument vectors with the same arity.

*Default level:* `:error`.

*Example trigger:* `(fn ([x] x) ([y]) x)`.

*Example message:* `More than one function overload with arity 2.`.

### Reduce without initial value

**Keyword:** `:reduce-without-init`.

*Description:* warn when reduce is called without an explicit initial
value. Read [this
article](https://purelyfunctional.tv/issues/purelyfunctional-tv-newsletter-313-always-use-the-3-argument-version-of-reduce/)
why this can be problematic.

*Default level:* `:off`.

*Example trigger:* `(reduce max [])`.

*Example message:* `Reduce called without explicit initial value.`

*Config:* to suppress the above warning:

```clojure
{:linters {:reduce-without-init {:exclude [clojure.core/max cljs.core/max]}}}
```

### Loop without recur

*Keyword:* `:loop-without-recur`.

*Description:* warn when loop does not contain recur.

*Default level:* `:warning`.

*Example trigger:* `(loop [])`.

*Example message:* `Loop without recur.`

### Line length

*Keyword:* `:line-length`.

*Description:* warn when lines are longer than a configured length.

*Default level:* `:warning`.

*Default line length:* `:max-line-length` is `nil` by default, which disables line length linting.

*Config:*

The line length linter needs to know how long you are prepared to allow your lines to be. This configuration:

``` clojure
{:linters {:line-length {:max-line-length 120}}}
```

will produce this warning:

``` clojure
Line is longer than 120 characters.
```

*Config:*

To exclude lines with URLs use: `:exclude-urls true`

```clojure
{:linters {:line-length {:max-line-length 120
                         :exclude-urls true}}}
```

To exclude lines that matches a pattern via `re-find`, use: `:exclude-pattern ";; :ll/ok"`:

```clojure
{:linters {:line-length {:max-line-length 120
                         :exclude-pattern ";; :ll/ok"}}}
```

### Keyword in binding vector

**Keyword:** `:keyword-binding`

*Description:* warn when a keyword is used in a `:keys` binding vector

*Default level:* `:off`.

*Example trigger:* `(let [{:keys [:a]} {:a 1}] a)`.

*Example message:* `Keyword binding should be a symbol: :a`

### Main without gen-class

*Keyword:* `:main-without-gen-class`.

*Description:* warn when -main function is present without corresponding `:gen-class`.

*Default level:* `:off`.

*Example trigger:* `(ns foo) (defn -main [& _args])`.

*Example message:* `Main function without gen-class.`

### Minus one

*Keyword:* `:minus-one`

*Description:* warn on usages of `-` that can be replaced with `dec`.

*Default level:* `:off`

*Example trigger:*

``` clojure
(def x 1)
(- x 1)
```

*Example message:* `Prefer (dec x) over (- x 1)`

Also see `:plus-one`.

### Misplaced docstring

*Keyword:* `:misplaced-docstring`.

*Description:* warn when docstring appears after argument vector instead of before.

*Default level:* `:warning`.

*Example trigger:* `(defn foo [] "cool fn" 1)`.

*Example message:* `Misplaced docstring.`

### Missing body in when

*Keyword:* `:missing-body-in-when`.

*Description:* warn when `when` is called only with a condition.

*Default level:* `:warning`.

*Example trigger:* `(when true)`.

*Example message:* `Missing body in when`.

### Missing clause in try

*Keyword:* `:missing-clause-in-try`.

*Description:* warn when `try` expression misses `catch` or `finally` clause.

*Default level:* `:warning`.

*Example trigger:* `(try 1)`.

*Example message:* `Missing catch or finally in try.`

### Missing docstring

*Keyword:* `:missing-docstring`.

*Description:* warn when public var misses docstring.

*Default level:* `:off`.

*Example trigger:* `(defn foo [] 1)`.

*Example message:* `Missing docstring.`

### Missing else branch

*Keyword:* `:missing-else-branch`.

*Description:* warns about missing else branch in `if` expression.

*Default level:* `:warning`.

*Example trigger:* `(if :foo :bar)`.

*Example message:* `Missing else branch..`

### Missing map value

*Keyword:* `:missing-map-value`.

*Description:* warn on key with uneven amount of elements, i.e. one of the keys
misses a value.

*Default level:* `:error`.

*Example trigger:* `{:a 1 :b}`

*Example message:* `missing value for key :b`.

### Missing test assertion

*Keyword:* `:missing-test-assertion`.

*Description:* warn on `deftest` expression without test assertion.

*Default level:* `:warning`.

*Example trigger:*

``` clojure
(require '[clojure.test :as test])
(test/deftest foo (pos? 1))
```

*Example message:* `missing test assertion`.

### Namespace name mismatch

*Keyword:* `:namespace-name-mismatch`.

*Description:* warn when the namespace in the `ns` form does not
correspond with the file name of the file.

*Default level:* `:error`.

*Example trigger:* a file named `foo.clj` containing a namespace `(ns bar)`.

*Example message:* `Namespace name does not match file name: bar`

*Example trigger:* a folder/file containing dashes instead of underscores, example: `example-namespace/foo.clj` containing a namespace `(ns example-namespace.foo)`.

*Example message:* `Namespace name does not match file name: example-namespace.foo`

### Nil return from if-like forms

*Keyword:* `:if-nil-return`.

*Description:* warn when if-like form explicitly returns nil from either
branch. It is idiomatic in Clojure to prefer when-like forms, which (implicitly)
return nil when the condition is not met.

*Default level:* `:off`.

*Example trigger:* `(if true 1 nil)`.

*Example message:* `For nil return, prefer when`.

### Non-arg vec return type hint

*Keyword:* `:non-arg-vec-return-type-hint`.

*Description:* warn when a return type in `defn` is not placed on the argument vector (CLJ only).

*Default level:* `:warning`.

*Example trigger:* `(defn ^String foo [] "cool fn")`.

*Example message:* `Prefer placing return type hint on arg vector: String`

Read [this](https://github.com/clj-kondo/clj-kondo/issues/1331) issue for more background information on this linter.

### Not empty?

*Keyword:* `:not-empty?`

*Description:* warn on `(not (empty? ...))` idiom. According to the docstring of `empty?` `seq` is prefered.

*Default level:* `:warning`.

*Example trigger:* `(not (empty? []))`

*Example message:* `use the idiom (seq x) rather than (not (empty? x))`.

### Plus one

*Keyword:* `:plus-one`

*Description:* warn on usages of `+` that can be replaced with `inc`.

*Default level:* `:off`

*Example trigger:*

``` clojure
(def x 1)
(+ x 1)
```

*Example message:* `Prefer (inc x) over (+ 1 x)`

Also see `:minus-one`.

### Private call

*Keyword*: `:private-call`.

*Description:* warn when private var is used. The name of this linter should be
renamed to "private usage" since it will warn on usage of private vars and not
only inside calls.

*Default level:* `:error`.

*Example trigger:*

``` clojure
(ns foo) (defn- f [])

(ns bar (:require [foo]))
(foo/f)
```

*Example message:* `#'foo/f is private`.

To suppress the above message, refer to `foo/f` using the var `#'foo/f` or write:

``` shellsession
#_{:clj-kondo/ignore [:private-call]}
(foo/f)
```

### Protocol method varargs

*Keyword:* `:protocol-method-varargs`.

*Description:* warn on definition of varargs protocol method.

*Default level:* `:error`.

*Example trigger:* `(defprotocol Foo (foo [x & xs]))`

*Example message:* `Protocol methods do not support varargs`.

### Redefined var

*Keyword:* `:redefined-var`.

*Description:* warn on redefined var.

*Default level:* `:warning`.

*Example trigger:* `(def x 1) (def x 2)`

*Example message:* `redefined var #'user/x`.

When redefining a var from another namespace that was referred, e.g. `inc`, the
message is: `inc already refers to #'clojure.core/inc`

### Var same name except case

*Keyword:* `:var-same-name-except-case`.

*Description:* warn on vars that share the same name with different case (only in Clojure mode) as these could cause clashing class file names on case insensitive filesystems.

*Default level:* `:warning`.

*Example trigger:* `(defmacro One [] 1) (defn one [] 1)`

*Example message:* `warning: Var name one differs only in case from: One`.

### Redundant do

*Keyword:* `:redundant-do`.

*Description:* warn on usage of do that is redundant. The warning usually arises
because of an explicit or implicit do as the direct parent s-expression.

*Default level:* `:warning`.

*Example trigger:* `(defn foo [] (do 1))`.

*Example message:* `redundant do`.

### Redundant call

*Keyword*: `:redundant-call`

*Description:* warn on redundant calls. The warning arises when a single argument
is passed to a function or macro that that returns its arguments.

*Default level:* `:off`.

`clojure.core` and `cljs.core` functions and macros that trigger this lint:
* `->`, `->>`
* `cond->`, `cond->>`
* `some->`, `some->>`
* `comp`, `partial`
* `merge`

*Config:*

``` clojure
{:linters {:redundant-call {:exclude #{clojure.core/->}
                            :include #{clojure.core/conj!}}}}
```

Use `:exclude` to suppress warnings for the built-in list. Use `:include` to
warn on additional vars.

*Example trigger:* `(-> 1)`.

*Example message:* `Single arg use of -> always returns the arg itself`.

### Redundant fn wrapper

*Keyword*: `:redundant-fn-wrapper`

*Description:* warn on redundant function wrapper.

*Default level:* `:off`.

*Example trigger:* `#(inc %)`.

*Example message:* `Redundant fn wrapper`.

### Redundant ignore

*Keyword*: `:redundant-ignore`

*Description:* warn on redundant ignore, i.e. when an ignored expression doesn't trigger any lint warning

*Default level:* `:info`.

*Example trigger:* `#_:clj-kondo/ignore (+ 1 2 3)`.

*Example message:* `Redundant ignore`.

### Redundant nested call

*Keyword*: `:redundant-nested-call`

*Description:* warn on redundant nested call of functions and macros

*Default level:* `:info`.

*Example trigger:* `(+ 1 2 (+ 1 2 3))`.

*Example message:* `Redundant nested call: +`.

### Redundant let

*Keyword:* `:redundant-let`.

*Description:* warn on usage of let that is redundant. The warning usually arises
because directly nested lets.

*Default level:* `:warning`.

*Example trigger:* `(let [x 1] (let [y 2] (+ x y)))`.

*Example message:* `Redundant let expression.`

### Redundant str call

*Keyword*: `:redundant-str-call`

*Description:* warn on redundant `str` calls. The warning arises when a single argument
is passed to a `str` that is already a string, which makes the `str` unnecessary.

*Default level:* `:info`.

*Example triggers:* `(str "foo")`, `(str (str 1))`.

*Example message:* `Single argument to str already is a string`.

### Refer

*Keyword:* `:refer`

*Description:* warns when `:refer` is used. This can be used when one wants to
enforce usage of aliases.

*Default level:* `:off`.

*Example trigger:* `(ns foo (:require [clojure.set :refer [union]]))`.

Example warning: `require with :refer`.

*Config:* to suppress the above warning:

```clojure
{:linters {:refer {:exclude [clojure.set]}}}
```

### Refer all

*Keyword:* `:refer-all`

*Description:* warns when `:refer :all` is used.

*Default level:* `:warning`.

*Example trigger:* `(ns foo (:require [clojure.set :refer :all]))`.

*Example message:* `use alias or :refer`.

*Config:* to suppress the above warning:

```clojure
{:linters {:refer-all {:exclude [clojure.set]}}}
```

### Schema misplaced return

*Keyword:* `:schema-misplaced-return`

*Description:* warn on a misplaced return Schema

*Default level:* `:warning`

*Example trigger:* `(require '[schema.core :as s]) (s/defn foo [] :- s/Str)`

*Example message:* `Return schema should go before vector.`

### Self-requiring namespace

*Keyword:* `:self-requiring-namespace`

*Description:* warn on a namespace that requires itself

*Default level:* `:off`

*Example trigger:* `(ns foo (:require [foo]))`

*Example message:* `Namespace is requiring itself: foo`

### Single key in

*Keyword:* `:single-key-in`.

*Description:* warn on associative path function with a single value path.

*Default level:* `:off`.

*Example trigger:* `(get-in {:a 1} [:a])`.

*Example message:* `get-in with single key.`

### Single logical operand

*Keyword:* `:single-logical-operand`.

*Description:* warn on single operand logical operators with always the same value.

*Default level:* `:warning`.

*Example trigger:* `(and 1)`.

*Example message:* `Single arg use of and always returns the arg itself.`

### Single operand comparison

*Keyword:* `:single-operand-comparison`.

*Description:* warn on comparison with only one argument.

*Default level:* `:warning`.

*Example trigger:* `(< 1)`.

*Example message:* `Single operand use of clojure.core/< is always true.`

### Shadowed fn param

*Keyword:* `:shadowed-fn-param`

*Description:* warn on fn param that has same name as previously defined one (in the same fn expression)

*Default level:* `:warning`.

*Example trigger:* `(fn [x x])`.

*Example message:* `Shadowed fn param: x`

### Shadowed var

*Keyword:* `:shadowed-var`.

*Description:* warn on var that is shadowed by local.

*Default level:* `:off`.

*Example trigger:* `(def x 1) (let [x 2] x)`.

*Example message:* `Shadowed var: user/x.`

*Config:*

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

### Static field call

*Keyword:* `:java-static-field-call`.

*Description:* warn when invoking a static field on a Java object.

*Default level:* `:warning`.

*Example trigger:* `(clojure.lang.PersistentQueue/EMPTY)`

*Example message*: `Static fields should be referenced without parens unless they are intended as function calls`

### Syntax

*Keyword:* `:syntax`.

*Description:* warn on invalid syntax.

*Default level:* `:warning`.

*Example trigger:* `[)`

*Example messages*:

```
Mismatched bracket: found an opening [ and a closing ) on line 1
Mismatched bracket: found an opening [ on line 1 and a closing )
```

### Type mismatch

*Keyword:* `:type-mismatch`.

*Description:* warn on type mismatches, e.g. passing a keyword where a number is expected.

*Default level:* `:error`.

*Example trigger:* `(inc :foo)`

*Example message:* `Expected: number, received: keyword.`

*Config:*

You can add or override type annotations. See
[types.md](https://github.com/clj-kondo/clj-kondo/blob/master/doc/types.md).

### Unbound destructuring default

*Keyword:* `:unbound-destructuring-default`.

*Description:* warn on binding in `:or` which does not occur in destructuring.

*Default level:* `:warning`.

*Example trigger:* `(let [{:keys [i] :or {i 2 j 3}} {}] i)`

*Example message:* `j is not bound in this destructuring form`.

### Uninitialized var

*Keyword:* `:uninitialized-var`

*Description:* warn on var without initial value

*Default level:* `:warning`

*Example trigger:* `(def x)`

*Example message:* `Uninitialized var`

### Unused alias

*Keyword:* `:unused-alias`.

*Description:* warn on unused alias introduced in ns form.

*Default level:* `:off`.

*Example trigger:* `(ns foo (:require [foo :as-alias bar]))`

*Example message:* `Unused alias: bar`.

### Unused binding

*Keyword:* `:unused-binding`.

*Description:* warn on unused binding.

*Default level:* `:warning`.

*Example trigger:* `(let [x 1] (prn :foo))`

*Example message:* `unused binding x`.

*Config:*

To exclude unused bindings from being reported, start their names with
underscores: `_x` or add regex patterns to `:exclude-patterns []`.

To exclude warnings about key-destructured function arguments, use:

``` clojure
{:linters {:unused-binding {:exclude-destructured-keys-in-fn-args true}}}
```

This will disable warnings for the following example:

``` clojure
(defn f [{:keys [a b c]} d])
```

To disable warnings about `:as` bindings (which can be useful for
documentation), use:

```clojure
{:linters {:unused-binding {:exclude-destructured-as true}}}
```

This will disable the warning in:

``` clojure
(defn f [{:keys [a b c] :as g}] a b c)

(defn g [[a :as b]] a)
```

To exclude warnings about defmulti dispatch function arguments, use:

``` clojure
{:linters {:unused-binding {:exclude-defmulti-args true}}}
```

This will disable the warning in:

``` clojure
(defmulti f (fn [a b] a))
```

To exclude bindings named "this" use:

``` clojure
{:linters {:unused-binding {:exclude-patterns ["^this"]}}}
```

Patterns are matched via `re-find`.

### Unused value

*Keyword*: `:unused-value`

*Description:* warn on unused value: constants, unrealized lazy values, pure functions and transient ops (`assoc!`, `conj!` etc).

*Default level:* `:warning`.

*Example triggers*:

- `(do 1 2)`
- `(do (map inc [1 2 3]) 2)`
- `(do (assoc {} :foo :bar) 2)`
- `(do (assoc! (transient {}) :foo :bar) 2)`

*Example message:* `Unused value: 1`.

### Used underscored bindings

*Keyword:* `:used-underscored-binding`.

*Description:* warn when a underscored (ie marked as unused) binding is used.

*Default level:* `:off`.

*Example trigger:* `(let [_x 0] _x)`.

*Example message:* `Using binding marked as unused: _x`

These warnings can be enabled by setting the level to `:warning` or
`:error` in your config.

``` clojure
{:linters {:used-underscored-binding {:level :warning}}}
```

To suppress the above warning:

``` clojure
{:linters {:used-underscored-binding {:level :warning
                                      :exclude [_x]}}}
```

A regex is also supported:

``` clojure
{:linters {:used-underscored-binding {:level :warning
                                      :exclude ["^_x.*$"]}}}
```

This will exclude all bindings starting with `_x`.

### Unknown :require option

*Keyword:* `:unknown-require-option`

*Description:* warn on unknown `:require` option pairs.

*Default level:* `:warning`.

*Example trigger:* `(ns foo (:require [bar :s b]))`.

*Example message:* `Unknown :require option: :s`.

*Config:* use `:exclude [:s]` to suppress the above warning.

### Unreachable code

*Keyword:* `:unreachable-code`.

*Description:* warn on unreachable code.

*Default level:* `:warning`.

*Example trigger:* `(cond :else 1 (odd? 1) 2)`.

*Example message:* `unreachable code`.

### Unused import

*Keyword:* `:unused-import`.

*Description:* warn on unused import.

*Default level:* `:warning`.

*Example trigger:* `(ns foo (:import [java.util UUID]))`.

*Example message:* `Unused import UUID.`

### Unresolved namespace

*Keyword:* `:unresolved-namespace`.

*Default level:* `:error`.

*Example trigger:* `foo.bar/baz`.

*Example message:* `Unresolved namespace foo.bar. Are you missing a require?`

*Config:* use `:exclude [foo.bar]` to suppress the above warning.

You can report duplicate warnings using:

``` clojure
{:linters {:unresolved-namespace {:report-duplicates true}}}
```

### Unresolved symbol

*Keyword:* `:unresolved-symbol`.

*Default level:* `:error`.

*Example trigger:* `x`.

*Example message:* `Unresolved symbol: x`.

*Config:*

In the following code, `match?` is a test assert expression brought in by `matcher-combinators.test`.
We don't want it to be reported as an unresolved symbol.

``` clojure
(ns foo
  (:require [clojure.test :refer [deftest is]]
            [matcher-combinators.test]))

(deftest my-test
  (is (match? [1 odd?] [1 3])))
```

The necessary config:

``` clojure
{:linters
  {:unresolved-symbol
    {:exclude [(clojure.test/is [match?])]}}}
```

If you want to exclude unresolved symbols from being reported:
- for all symbols under calls to `clojure.test/is`, omit the vector of symbols: `:exclude [(clojure.test/is)]`
- for symbol `match?` globally for your project, specify only the vector of symbols: `:exclude [match?]`

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

You can report duplicate warnings using:

``` clojure
{:linters {:unresolved-symbol {:report-duplicates true}}}
```

#### :exclude-patterns

Since v2023.04.14 you can use `:exclude-patterns` to suppress symbols by regex patterns (as strings, processed via `re-find`):

``` clojure
(ns scratch)

(defmacro match
  {:clj-kondo/config
   '{:linters {:unresolved-symbol {:exclude-patterns ["^\\?"]}}}}
  [& _xs])

(match {:foo ?foo} {:foo :bar}
       [?foo x])
```

In the above example, only `x` is reported as an unresolved symbol, while
symbols starting with a question mark are not reported.

### Unresolved var

*Keyword:* `:unresolved-var`.

*Description:* warns on unresolved var from other namespace.

*Default level:* `:warning`.

*Example trigger:* `(require '[clojure.set :as set]) (set/onion)`.

*Example message:* `Unresolved var: set/onion`.

*Config:*

Given this example:

``` clojure
(ns foo)
(defmacro gen-vars [& names]) (gen-vars x y z)

(ns bar (:require foo))
foo/x
(foo/y)
```

you can exclude warnings for all unresolved vars from namespace `foo` using:

``` clojure
{:linters {:unresolved-var {:exclude [foo]}}}
```

or exclude a selection of unresolved vars using qualified symbols:

``` clojure
{:linters {:unresolved-var {:exclude [foo/x]}}}
```

You can report duplicate warnings using:

``` clojure
{:linters {:unresolved-var {:report-duplicates true}}}
```

### Unsorted imports

*Keyword:* `:unsorted-imports`.

*Description:* warns on non-alphabetically sorted imports in `ns` and `require` forms.

*Default level:* `:off`.

*Example trigger:* `(ns foo (:import [foo A] [bar B]))`.

*Example message:* `Unsorted import: [bar B]`.

### Unsorted required namespaces

*Keyword:* `:unsorted-required-namespaces`.

*Description:* warns on non-alphabetically sorted libspecs in `ns` and `require` forms.

*Default level:* `:off`.

*Example trigger:* `(ns foo (:require b a))`.

*Example message:* `Unsorted namespace: a`.

*Config*:

The default sort will consider lower-cased namespace name. To enable
keeping the namespace name as is:

```clojure
{:linters {:unsorted-required-namespaces {:sort :case-sensitive}}}
```

Possible values for `:sort` are `:case-insensitive` (default) and `:case-sensitive`.

### Unused namespace

*Keyword:* `:unused-namespace`.

*Description:* warns on required but unused namespace.

*Default level:* `:warning`.

*Example trigger:* `(ns foo (:require [bar :as b]))`.

*Example message:* `namespace bar is required but never used`.

*Config:*

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

Namespaces without `:as` or `:refer` are assumed to be loaded for side effects,
e.g. for clojure.spec or defining a protocol or multi-method, so the following
will not trigger a warning:

``` clojure
(ns foo (:require [foo.specs]))
```

If you'd like to have namespaces without `:as` or `:refer` trigger
warnings, you can enable this by setting the `:simple-libspec` option

``` clojure
{:linters {:unused-namespace {:simple-libspec true}}}
```

### Unused private var

*Keyword:* `:unused-private-var`.

*Description:* warns on unused private vars.

*Default level:* `:warning`.

*Example trigger:* `(ns foo) (defn- f [])`

*Example message:* `Unused private var foo/f`

*Config:*

To suppress the above warning:

``` clojure
{:linters {:unused-private-var {:exclude [foo/f]}}}
```

When defining a private var with defonce just for side effects, you can start
the name with an underscore:

``` clojure
(defonce ^:private _dude (launch-missiles))
```

### Unused referred var

*Keyword:* `:unused-referred-var`.

*Description:* warns about unused referred vars.

*Default level:* `:warning`.

*Example trigger:* `(ns foo (:require [clojure.set :refer [union]]))`.

*Example message:* `#'clojure.set/union is referred but never used`.

*Config:*

Imagine you want to have `taoensso.timbre/debug` available in all of your
namespaces. Even when you don't use it, you don't want to get a warning about
it. That can be done as follows:

``` clojure
{:linters {:unused-referred-var {:exclude {taoensso.timbre [debug]}}}}
```

### Use

*Keyword:* `:use`.

*Description:* warns about `:use` or `use`.

*Default level:* `:warning`.

*Example trigger:* `(ns foo (:use [clojure.set]))`.

*Example message:* `use :require with alias or :refer`.

*Config:*

This linter is closely tied to [Refer All](#refer-all). Namespaces configured to
suppress the `:refer-all` warning will also suppress the `:use` warning.

### Warn on reflection

*Keyword:* `:warn-on-reflection`

*Description:* warns about not setting `*warn-on-reflection*` to true in Clojure
namespaces. Defaults to only warning when doing interop.

*Default level:* `:off`

*Example trigger:* `(.length "hello")`

*Example message:* `Var *warn-on-reflection* is not set in this namespace.`

*Config:*

``` clojure
:warn-on-reflection {:level :off
                     :warn-only-on-interop true}
```

The value of `:warn-only-on-interop` can be set to `false` to always warn in
Clojure namespaces.

### Underscore in namespace

*Keyword:* `:underscore-in-namespace`

*Description:* warns about the usage of the `_` character in the declaration of namespaces (as opposed to `-`).

*Default level:* `:warning`

*Example trigger:* `(ns special_files)`

*Example message:* `Avoid underscore in namespace name: special_files`
