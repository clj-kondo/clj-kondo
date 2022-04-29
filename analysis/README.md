# Analysis data and tools

Clj-kondo can provide data that was collected during linting, which enables
writing tools and linters that are not yet in clj-kondo itself. To get this
data, use the following configuration:

``` shellsession
{:analysis true}
```

When using clj-kondo from the command line, the analysis data will be exported
with `{:output {:format ...}}` set to `:json` or `:edn`.

## Extra analysis

Further analysis can be returned by providing `:analysis` with a map of options:

``` shellsession
{:analysis {... ...}
```

- `:locals`: when truthy return `:locals` and `:local-usages` described below
- `:keywords`: when truthy return `:keywords` described below
- `:arglists`: when truthy return `:arglists` on `:var-definitions`
- `:protocol-impls`: when truthy return `:protocol-impls` described below

### Metadata

Clj-kondo returns common metadata such as `:added` and `:deprecated`.
You can request that it return all, or a specific set, of user-coded namespace or var metadata via:

- `:namespace-definitions`
  - `:meta`: return user coded metadata under `:namespace-definitions` -> `:meta`, specify:
    - `true`: to return all
    - key sequence:  return metadata matching specified keys, ex. `[:skip-wiki :integration-test]`
- `:var-definitions`
  - `:meta`: return user coded metadata under `:var-definitions` -> `:meta`, options are the same as for namespaces.

### Context data

Built-in and custom hook can add arbitrary data to the analysis using a
`:context` map. This context map will currently appear in `:var-usages` and
`:keywords`. You can opt-in to the entire context map using `:context true` or
select certain keys using `:context [:re-frame.core]`.

## Limited analysis

Similarly, analysis can be limited. This is useful to quickly scan a file or
project. When using these expert options, you should not expect linters to
behave correctly. As such, consider using them with `:skip-lint true`.

- `:var-usages`: when falsy skip `:var-usages` described below
- `:var-definitions`
  - `:shallow true`: analyze `:var-definitions`, but skip their bodies

If you analyze var definitions shallowly, anything within the body of a form
listed below will be skipped. That means that if you define a var within another
`defn` (a [discouraged](https://guide.clojure.style/#dont-def-vars-inside-fns)
practice), it won't be analyzed.

- def
- defn
- defrecord
- defmethod
- plumatic schema: fn, def, defn, defmethod, defrecord
- protocol-impls
- fn
- letfn
- fn*
- bound-fn

# Data

The analysis output consists of a map with:

- `:namespace-definitions`, a list of maps with:
  - `:filename`, `:row`, `:col`
  - `:name`: the name of the namespace

  Optional:
  - `:lang`: if definition occurred in a `.cljc` file, the language in which the
    definition was done: `:clj` or `:cljs`
  - common metadata values: `:deprecated`, `:doc`, `:author`, `:added`, `:no-doc` (used by
    [codox](https://github.com/weavejester/codox)).
  - `:meta` map of requested metadata for namespace

 - `:namespace-usages`, a list of maps with:
   - `:filename`, `:row`, `:col`
   - `:from`: the namespace which uses
   - `:to`: the used namespace
   - `:alias`: the alias of namespace, if used

   Optional
   - `:lang`: if usage occurred in a `.cljc` file, the language in which it
     was resolved: `:clj` or `:cljs`

- `:var-definitions`, a list of maps with:
  - `:filename`, `:row`, `:col`, `end-row`, `end-col`
  - `:ns`: the namespace of the var
  - `:name`: the name of the var
  - `:defined-by`: the namespaced symbol which defined this var

  Optional:
  - `:fixed-arities`: a set of fixed arities
  - `:varargs-min-arity`: the minimal number of arguments of a varargs signature
  - common metadata values: `:private`, `:macro`, `:deprecated`, `:doc`, `:added`
  - `:meta` map of requested metadata for var
  - `:lang`: if definition occurred in a `.cljc` file, the language in which the
    definition was done: `:clj` or `:cljs`
  - `:arglists-str`: a list of each set of args as written
  - `:protocol-ns`, `:protocol-name`: the protocol namespace and name for a protocol method

- `:var-usages`, a list of maps with:
  - `:filename`
  - `:name`: the name of the used var
  - `:row`, `col`: the start position of this usage, the parenthesis start location if a function call
  - `:end-row`, `end-col`: the end position of this usage, the parenthesis end location if a function call
  - `:name-row`, `:name-col`: the start position of the name of this usage
  - `:name-end-row`, `:name-end-col`: the end position of the name of this usage
  - `:from`: the namespace from which the var was used
  - `:to`: the namespace of the used var
  - `:from-var`: the function name from which the var was used

  Optional:
  - `:arity`: if the usage was a function call, the amount of arguments passed
  - `:lang`: if usage occurred in a `.cljc` file, the language in which the call
    was resolved: `:clj` or `:cljs`
  - several attributes of the used var: `:private`, `:macro`, `:fixed-arities`,
    `:varargs-min-arity`, `:deprecated`.

- `:locals`, a list of maps with:
  - `:filename`, `:row`, `:col`, `:end-row`, `:end-col`
  - `:id`: an identification for this local, `:local-usages` will reference this
  - `:name`: the name of the used local
  - `:str`: the as written string of the local from the file and position
  - `:scope-end-row`: the row in which this local will go out of scope
  - `:scope-end-col`: the column in which this local will go out of scope

- `:local-usages`, a list of maps with:
  - `:filename`, `:row`, `:col`, `:end-row`, `:end-col`
  - `:name-row`, `:name-col`, `:name-end-row`, `:name-end-col` 
  - `:id`: an identification for this local, refers to the local declaration with the same `:id` in `:locals`
  - `:name`: the name of the used local

- `:keywords`, a list of maps with:
  - `:filename`, `:row`, `:col`, `:end-row`, `:end-col`
  - `:name`: the name of the used keyword as string
  - `:ns`: the namespace of the keyword.
    - `:kw` will have a `nil` ns.
    - `::kw` will have the current ns.
    - `:b/kw` will have `b` as a ns regardless of `require`ed namespaces.
    - `::b/kw` will be the aliased ns of `b` or `:clj-kondo/unknown-namespace` if `b` is not an alias.

    Keywords in namespaced maps:
    - `#:b{:kw 1}` will have `b` as ns.
    - `#:b{:_/kw 1}` will have no ns
    - `#:b{:c/kw 1}` will have `c` as ns.
    - `#:b{::kw 1}` will have the current ns.
  - `:alias`: the alias used by the keyword. Only present when a valid, external alias.
    - `::a/kw` would have an alias of `a`.
    - `::kw` does not have an alias.
  - `:auto-resolved`: if the keyword `:ns` is auto resolved, example: `::kw`
  - `:namespace-from-prefix`: if the keyword `:ns` is from the namespaced map, example: `::b{:kw 1}`
  - `:keys-destructuring`: if the keyword is within a `:keys` vector.
  - `:reg`: can be added by `:hooks` using `clj-kondo.hook-api/reg-keyword!` to indicate a registered definition location for the keyword.
    It should be the fully qualified call that registered it.

- `:protocol-impls`, a list of maps with:
  - `filename`
  - `:row`, `:col`, `:end-row`, `:end-col`, the range of the implementation method.
  - `:name-row`, `:name-col`, `:name-end-row`, `:name-end-col`, the range of the implementation method name.
  - `protocol-name`, the name of the protocol being implemented.
  - `protocol-ns`, the namespace of the protocol being implemented.
  - `impl-ns`, the namespace of the implementation of the protocol.
  - `method-name`, the method name of the implementation.
  - `defined-by`, the symbol that defines this, e.g. `clojure.core/defrecord`.

Example output after linting this code:

``` clojure
(ns foo
  "This is a useful namespace."
  {:deprecated "1.3"
   :author "Michiel Borkent"
   :no-doc true}
  (:require [clojure.set :as set]))

(defn- f [x]
  (inc x))

(defmacro g
  "No longer used."
  {:added "1.2"
   :deprecated "1.3"}
  [x & xs]
  `(comment ~x ~@xs))
```

``` clojure
$ clj-kondo --lint /tmp/foo.clj --config '{:output {:format :edn}, :analysis true}'
| jet --pretty --query ':analysis'

{:namespace-definitions [{:filename "/tmp/foo.clj",
                          :row 1,
                          :col 1,
                          :name foo,
                          :deprecated "1.3",
                          :doc "This is a useful namespace.",
                          :no-doc true,
                          :author "Michiel Borkent"}],
 :namespace-usages [{:filename "/tmp/foo.clj",
                     :row 6,
                     :col 14,
                     :from foo,
                     :to clojure.set,
                     :alias set}],
 :var-definitions [{:filename "/tmp/foo.clj",
                    :row 8,
                    :col 1,
                    :end-row 9,
                    :end-col 11,
                    :ns foo,
                    :name f,
                    :defined-by 'clojure.core/defn-
                    :private true,
                    :fixed-arities #{1}}
                   {:added "1.2",
                    :ns foo,
                    :name g,
                    :filename "/tmp/foo.clj",
                    :defined-by 'clojure.core/defmacro
                    :macro true,
                    :row 11,
                    :col 1,
                    :end-row 16,
                    :end-col 22,
                    :deprecated "1.3",
                    :varargs-min-arity 1,
                    :doc "No longer used."}],
 :var-usages [{:fixed-arities #{1},
               :name inc,
               :filename "/tmp/foo.clj",
               :from foo,
               :col 4,
               :from-var f,
               :arity 1,
               :row 9,
               :to clojure.core}
              {:name defn-,
               :filename "/tmp/foo.clj",
               :from foo,
               :macro true,
               :col 2,
               :arity 3,
               :varargs-min-arity 2,
               :row 8,
               :to clojure.core}
              {:name comment,
               :filename "/tmp/foo.clj",
               :from foo,
               :macro true,
               :col 5,
               :from-var g,
               :varargs-min-arity 0,
               :row 16,
               :to clojure.core}
              {:name defmacro,
               :filename "/tmp/foo.clj",
               :from foo,
               :macro true,
               :col 2,
               :arity 5,
               :varargs-min-arity 2,
               :row 11,
               :to clojure.core}]}
```

NOTE: breaking changes may occur as result of feedback in the next few weeks (2019-07-30).

## Example tools

These are examples of what you can do with the analysis data that clj-kondo
provides as a result of linting your sources.

To run the tools on your system you will need the Clojure [CLI
tool](https://clojure.org/guides/getting_started) version 1.10.1.466 or higher
and then use this repo as a git dep:

``` clojure
{:deps {clj-kondo/tools {:git/url "https://github.com/clj-kondo/clj-kondo"
                         :sha "1ed3b11025b7f3a582e6db099ba10a888fe0fc2c"
                         :deps/root "analysis"}}}
```

Replace the `:sha` with the latest SHA of this repo.

You can create an alias for a tool in your `~/.clojure/deps.edn`:

```
{
 :aliases {:unused-vars
           {:extra-deps {clj-kondo/tools {:git/url "https://github.com/clj-kondo/clj-kondo"
                                          :sha "1ed3b11025b7f3a582e6db099ba10a888fe0fc2c"
                                          :deps/root "analysis"}}
            :main-opts ["-m" "clj-kondo.tools.unused-vars"]}
 }
}
```

and then call it from anywhere in your system with:

```
$ clj -M:unused-vars src
```

### Unused vars

``` shellsession
$ clj -m clj-kondo.tools.unused-vars src
The following vars are unused:
clj-kondo.tools.namespace-graph/-main
clj-kondo.tools.unused-vars/-main
```

A [planck](https://planck-repl.org) port of this example is available in the
`script` directory. You can invoke it like this:

``` shellsession
script/unused_vars.cljs src
```

### Private vars

A variation on the above tool, which looks at private vars and reports unused
private vars or illegally accessed private vars.

Example code:

``` clojure
(ns foo)

(defn- foo [])
(defn- bar []) ;; unused

(ns bar (:require [foo :as f]))

(f/foo) ;; illegal call
```

``` shellsession
$ clj -m clj-kondo.tools.private-vars /tmp/private.clj
/tmp/private.clj:4:8 warning: foo/bar is private but never used
/tmp/private.clj:8:1 warning: foo/foo is private and cannot be accessed from namespace bar
```

A [planck](https://planck-repl.org) port of this example is available in the
`script` directory. You can invoke it like this:

``` shellsession
script/private_vars.cljs /tmp/private.clj
```

### Namespace graph

This example requires GraphViz. Install with e.g. `brew install graphviz`.

``` shellsession
$ clj -M -m clj-kondo.tools.namespace-graph src
```

<img src="assets/namespace-graph.png">

### Find var

``` shellsession
$ clj -m clj-kondo.tools.find-var clj-kondo.core/run! src ../src
clj-kondo.core/run! is defined at ../src/clj_kondo/core.clj:51:7
clj-kondo.core/run! is used at ../src/clj_kondo/core.clj:120:12
clj-kondo.core/run! is used at ../src/clj_kondo/main.clj:81:44
clj-kondo.core/run! is used at src/clj_kondo/tools/find_var_usages.clj:8:29
clj-kondo.core/run! is used at src/clj_kondo/tools/namespace_graph.clj:7:29
clj-kondo.core/run! is used at src/clj_kondo/tools/unused_vars.clj:9:31
```

### Popular vars

``` shellsession
$ clj -m clj-kondo.tools.popular-vars 10 ../src
clojure.core/let: 196
clojure.core/defn: 183
clojure.core/when: 115
clojure.core/=: 86
clojure.core/if: 86
clojure.core/recur: 79
clojure.core/assoc: 70
clojure.core/or: 68
clojure.core/->: 68
clojure.core/first: 62
```

### Missing docstrings

``` shellsession
$ clj -m clj-kondo.tools.missing-docstrings ../src
clj-kondo.impl.findings/reg-finding!: missing docstring
clj-kondo.impl.findings/reg-findings!: missing docstring
clj-kondo.impl.namespace/reg-var!: missing docstring
clj-kondo.impl.namespace/reg-var-usage!: missing docstring
clj-kondo.impl.namespace/reg-alias!: missing docstring
...
```

### Circular dependencies

Example code:

``` clojure
(ns a (:require b c))

(ns b (:require a)) ;; circular dependency

(ns c (:require a)) ;; circular dependency
```

```
$ clj -m clj-kondo.tools.circular-dependencies /tmp/circular.clj
/tmp/circular.clj:3:17: circular dependendy from namespace b to a
/tmp/circular.clj:5:17: circular dependendy from namespace c to a
```

### Finding unused and undefined re-frame subscriptions

See [this](https://github.com/yannvanhalewyn/analyze-re-frame-usage-with-clj-kondo) repo.

Also see this [gist](https://gist.github.com/roman01la/c6a2e4db8d74f89789292002794a7142).
