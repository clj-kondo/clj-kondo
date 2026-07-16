# Types

STATUS: WORK IN PROGRESS. THE CONFIGURATION FORMAT IS STILL IN FLUX, BREAKING
CHANGES WILL HAPPEN.

Clj-kondo provides rudimentary type checking using a few basic rules and type
annotations. The linter for this is called `:type-mismatch` in the
configuration. Clj-kondo knows the types of a selection of
[clojure](https://github.com/clj-kondo/clj-kondo/tree/master/src/clj_kondo/impl/types/clojure)
functions and is therefore able to warn about mismatches like:

``` clojure
$ clj-kondo --lint - <<< '(first (map inc))'
<stdin>:1:8: warning: Expected: seqable collection, received: transducer.
```

or

``` clojure
$ clj-kondo --lint - <<< '(inc (subs "foo" 1 2))'
<stdin>:1:6: warning: Expected: number, received: string.
```

The second way clj-kondo is informed about types is by inspecting type hints:

``` clojure
$ clj-kondo --lint - <<< '(defn foo [^String x] x) (foo 1)'
<stdin>:1:31: warning: Expected: string or nil, received: positive integer.
```

This provides a way for users to inform clj-kondo and get some type checking for
free. But type hints were never designed with type checking in mind. Therefore
clj-kondo lets users bring in their own type annotations in the configuration:

``` clojure
{:linters
 {:type-mismatch
  {:level :warning
   :namespaces {foo {foo {:arities {1 {:args [:string]
                                       :ret :string}}}}}}}}
```

``` clojure
$ clj-kondo --lint - <<< '(ns bar (:require [foo :refer [foo]])) (foo 1)'
<stdin>:1:45: warning: Expected: string, received: positive integer.
```

``` clojure
$ clj-kondo --lint - <<< '(ns bar (:require [foo :refer [foo]])) (inc (foo "foo"))
<stdin>:1:45: warning: Expected: number, received: string.'
```

The configuration is organized per namespace. Annotations are provided per arity.

``` clojure
{:linters
 {:type-mismatch
  {:level :warning
   :namespaces {foo {foo-1 {:arities {1 {:args [:string]
                                         :ret :string}
                                      2 {:args [:string :int]
                                         :ret :map}
                                      :varargs {:args [:string :int {:op :rest
                                                                     :spec :int}]
                                                :ret :int}}}
                     foo-2 ...}
                bar {bar-1 ...}}}}}
```

Available types and relations can be found
[here](https://github.com/clj-kondo/clj-kondo/blob/d9fca2705863e3e604e004ccb942e0b3d2e268ec/src/clj_kondo/impl/types.clj#L18-L51).

Special operators:

- `{:op :rest, :spec :int}`. This can be used to match remaining arguments in
  vararg signatures. This operation also supports `:last` which can be used if
  the last vararg has a different type than the others (like in
  `clojure.core/apply`). The spec may be wrapped in a vector, to match pair-wise
  arguments: `{:args [{:op :rest, :spec [:any :any]}]`. Trying to match this
  spec with an uneven number of arguments will fail.

- `{:op :keys, :req {:a :string} :opt {:b :int}}`. This can be used to match map
  literals and check for required and optional keys.

## How can I help?

- Provide a PR for missing
  [clojure](https://github.com/clj-kondo/clj-kondo/tree/master/src/clj_kondo/impl/types/clojure)
  annotations.
- Report false positives.
- Try out the configuration and report feedback.
