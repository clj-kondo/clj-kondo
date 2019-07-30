# clj-kondo-tools

These are examples of what you can do with the analysis data that clj-kondo
provides as a result of linting your sources.

To run the examples on your system you will need the Clojure [CLI
tool](https://clojure.org/guides/getting_started) version 1.10.1.466 or higher
and then use this repo as a git dep:

``` clojure
{:deps {clj-kondo/tools {:git/url "https://github.com/borkdude/clj-kondo"
                         :sha "44d54415b584694ff0e2dbfcbe71fd304b3829dd"
                         :deps/root "tools"}}}
```

Replace the `:sha` with the latest SHA of this repo.

## Data

A look at the data available after linting this code:

``` clojure
(ns foo
  (:require [clojure.set]))

(defn- f [])

(defmacro g [x & xs]
  `(comment ~x ~@xs))
```

``` clojure
$ clj -m clj-kondo.tools.pprint /tmp/foo.clj
:namespace-definitions
|    :filename | :row | :col | :name |
|--------------+------+------+-------|
| /tmp/foo.clj |    1 |    1 |  user |
| /tmp/foo.clj |    1 |    1 |   foo |

:namespace-usages
|    :filename | :row | :col | :from |         :to |
|--------------+------+------+-------+-------------|
| /tmp/foo.clj |    2 |   14 |   foo | clojure.set |

:var-definitions
|    :filename | :row | :col | :ns | :name | :fixed-arities | :var-args-min-arity | :private | :macro |
|--------------+------+------+-----+-------+----------------+---------------------+----------+--------|
| /tmp/foo.clj |    4 |    1 | foo |     f |           #{0} |                     |     true |        |
| /tmp/foo.clj |    6 |    1 | foo |     g |                |                   1 |          |   true |

:var-usages
|    :filename | :row | :col | :from |          :to |    :name | :arity |
|--------------+------+------+-------+--------------+----------+--------|
| /tmp/foo.clj |    4 |    1 |   foo | clojure.core |    defn- |      2 |
| /tmp/foo.clj |    7 |    5 |   foo | clojure.core |  comment |        |
| /tmp/foo.clj |    6 |    1 |   foo | clojure.core | defmacro |      3 |
```

NOTE: breaking changes may occur as result of feedback in the next few weeks (2019-07-30).

## Unused vars

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

## Private vars

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

## Namespace graph

This example requires GraphViz. Install with e.g. `brew install graphviz`.

``` shellsession
$ clj -m clj-kondo.tools.namespace-graph src
```

<img src="assets/namespace-graph.png">

## Find var

``` shellsession
$ clj -m clj-kondo.tools.find-var clj-kondo.core/run! src ../src
clj-kondo.core/run! is defined at ../src/clj_kondo/core.clj:51:7
clj-kondo.core/run! is used at ../src/clj_kondo/core.clj:120:12
clj-kondo.core/run! is used at ../src/clj_kondo/main.clj:81:44
clj-kondo.core/run! is used at src/clj_kondo/tools/find_var_usages.clj:8:29
clj-kondo.core/run! is used at src/clj_kondo/tools/namespace_graph.clj:7:29
clj-kondo.core/run! is used at src/clj_kondo/tools/unused_vars.clj:9:31
```
