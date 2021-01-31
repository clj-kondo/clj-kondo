# Hooks

Hooks are a way to enhance linting via user provided code.

## API

Hooks are interpreted using the [Small Clojure Interpreter](https://github.com/borkdude/sci).

Hooks receive Clojure code as rewrite-clj nodes, not only for performance reasons, but
also because rewrite-clj nodes carry the line and row numbers for every Clojure element.
Note that when we refer to a "rewrite-clj node", we are referring to clj-kondo's version of rewrite-clj node.
Clj-kondo's version of [rewrite-clj](https://github.com/xsc/rewrite-clj) is catered to its use case,
includes some bug fixes, but most notably: strips away all whitespace.

A hook can leverage the `clj-kondo.hooks-api` namespace for transformation and analysis of rewrite-clj nodes.

API functions for producing nodes:

- `list-node`: produce a new list node from a seqable nodes.
- `vector-node`: produce a new vector node from a seqable of nodes.
<!-- - `map-node`: produce a new map node from a seqable of nodes-->
- `keyword-node`: produce a new keyword. Use `(api/keyword-node :foo)` for a
  normal keyword and `(api/keyword-node :foo true)` to produce a node for
  `::foo`.
- `string-node`: produce a new node for a single string or multiple strings (passed as seq)
- `token-node`: produce a new token node. Used for all remaining tokens (mainly used for symbols or nil).

Each producing function has a predicate counterpart for checking a type of node:

- `list-node?`: returns true if given node is a list node
- etc.

Other API functions:

- `sexpr`: turns a node into a Clojure s-expression. Useful for analyzing concrete values.
<!-- - `reg-keyword!`: indicates that a keyword's analysis should be mared as a definition. Expects the keyword node and either true or the fully-qualified call that registered it.-->
- `reg-finding!`: registers a finding. Expects a map with:
  - `:message`: the lint message
  - `:row` and `:col`: the location of the finding. These values can be derived from the metadata of a node.
  - `:type`: the type of lint warning. A level must be set for this type in the
    clj-kondo config under `:linters`. If the level is not set, the lint warning
    is ignored.

The namespaces `clojure.core`, `clojure.set` and `clojure.string` are also available.
Use `println` or `prn` for debugging and `time` to measure performance.

Hooks must be configured in clj-kondo's `config.edn` under `:hooks`, e.g.:

``` Clojure
{:hooks {:analyze-call {foo.weird-macro hooks.foo/weird-macro}}}
```

## analyze-call

The `analyze-call` hook offers a way to lint macros that are unrecognized by clj-kondo and cannot
be supported by [`:lint-as`](#lint-a-custom-macro-like-a-built-in-macro).

It receives Clojure macro (or function) call code as input in the form of a rewrite-clj node, and can:

- Transform the code to teach clj-kondo about its effect.
- Inspect call arguments and emit findings about them.

### Transformation

As an example, let's take this macro:

``` clojure
(ns mylib)
(defmacro with-bound [binding-vector & body] ,,,)
```

Users can call this macro like so:

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
`.clj-kondo/hooks/with_bound.clj` and the `with-bound` function defined in
it. Note that the file has to declare a namespace corresponding to its directory
structure and file name, just like in normal Clojure.

An analyze-call hook function receives a `:node` in its argument map. This is a
rewrite-clj node representing the hooked Clojure macro (or function) call code
clj-kondo has found in the source code it is linting. The hook uses the
`clj-kondo.hooks-api` namespace to validate then rewrite this node into a new
rewrite-clj node:

1. The `with-bound` hook function checks if the call has at least a `sym` and `val`
node. If not, it will throw an exception, which will result in a clj-kondo warning.

2. As a last step, the hook function constructs a new node using `api/list-node`,
`api/token-node` and `api/vector-node`. This new node is returned in a map under
the `:node` key.

Now clj-kondo fully understands the `my-lib/with-bound` macro and you will no
longer get false positives when using it. Moreover, it will report unused
bindings and will give warnings customized to this macro.

<p align="center">
  <img src="../screenshots/hooks-with-bound.png"/>
</p>

### Custom lint warnings

Analyze-call hooks can also be used to create custom lint warnings, without
transforming the original rewrite-clj node.

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

## Clojure code as rewrite-clj nodes

If you develop a hook you will likely need some familiarity with rewrite-clj node structure.
A couple of examples might help:

`(my-macro 1 2 3)` becomes:

- a list node with `:children`:
  - token node `my-macro`
  - token node `1`
  - token node `2`
  - token node `3`

`(my-lib/with-bound [a 1 {:with-bound/setting true}] (inc a))` becomes:

- a list node with `:children`
  - token node `my-lib/with-bound`
  - vector node with `:children`
    - token-node `a`
    - token-node `1`
    - map node with `:children`
      - keyword node `:with-bound/setting`
      - token node `true`
  - list node
    - token node `inc`
    - token node `a`

Clj-kondo uses a different approach to metadata than the original rewrite-clj
library. Metadata nodes are stored in the `:meta` key on nodes correponding to
the values carrying the metadata:

`^:foo ^:bar []` becomes:

- a vector node with `:meta`
  - a seq of nodes with:
    - keyword node `:foo`
    - keyword node `:bar`

## Example Hooks

- rewrite-cljc-playground: [import-vars-with-mod](https://github.com/lread/rewrite-cljc-playground/commit/09882e1244a8c12879ef8c1e6872724748e7914b)

More examples of hooks can be found in the [config](https://github.com/clj-kondo/config) project.

## Tips and tricks

Here are some tips and tricks for developing hooks.

### Debugging

For debugging the output of a hook function, you can use `println` or `prn`. To
get a sense of what a newly generated node looks like, you can use `(prn
(api/sexpr node))`.

### Performance

Less code to process will result in faster linting. If only one hook is used in
certain files and another hook is used in other files, divide them up into
multiple files and namespaces. If the hooks use common code, you can put that in
a library namespace and use `require` to load it from each hook's namespace.

To test performance of a hook, you can write code which triggers the hook and
repeat that expression `n` times (where `n` is a large number like
1000000). Then lint the file with `clj-kondo --lint` and measure
timing. The `time` macro is also available within hooks code.

## Clojurists Together

The initial work on hooks was sponsored by [Clojurists
Together](https://www.clojuriststogether.org/) as part of their [Summer of
Bugs](https://www.clojuriststogether.org/news/announcing-summer-of-bugs/)
program.
