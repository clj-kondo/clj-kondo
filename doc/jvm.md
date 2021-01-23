# Running on the JVM

Using the binary is recommended for better startup time, but you can run
clj-kondo as a command line program on the JVM as well. Additionally, there is
an [API](#api) to use clj-kondo from other Clojure programs.

## leiningen

You can add clj-kondo to `~/.lein/profiles.clj` to make it available as a `lein` command:

``` clojure
{:user {:dependencies [[clj-kondo "RELEASE"]]
        :aliases {"clj-kondo" ["run" "-m" "clj-kondo.main"]}}}
```

``` shellsession
$ lein clj-kondo --lint src
```

## tools.deps.alpha

Run clj-kondo as an ad-hoc command line dependency:

``` shellsession
$ clj -Sdeps '{:deps {clj-kondo/clj-kondo {:mvn/version "RELEASE"}}}' -m clj-kondo.main --lint src
```

Or add it as an alias to `~/.clojure/deps.edn`:

``` clojure
{:aliases
 {:clj-kondo
  {:extra-deps {clj-kondo/clj-kondo {:mvn/version "RELEASE"}}
   :main-opts ["-m" "clj-kondo.main"]}}}
```

``` shellsession
$ clj -M:clj-kondo --lint src
```

## shadow-cljs

Run `clj-kondo.main` directly:

``` clojure
{:dependencies [[clj-kondo "RELEASE"]]}
```

``` shellsession
$ shadow-cljs run clj-kondo.main --lint src
```

Execution will be much faster if there's a background server (either `shadow-cljs server` or `shadow-cljs watch`) running.

## API

To use clj-kondo from other Clojure programs, use the API in
[`clj-kondo.core`](https://cljdoc.org/d/clj-kondo/clj-kondo/CURRENT/api/clj-kondo.core).

``` clojure
$ clj
Clojure 1.10.0
user=> (require '[clj-kondo.core :as clj-kondo])
nil
user=> (-> (clj-kondo/run! {:lint ["corpus"]}) :summary)
{:error 41, :warning 43, :info 0, :type :summary, :duration 139}
user=> (-> (clj-kondo/run! {:lint ["corpus"]}) clj-kondo/print!)
corpus/cljc/datascript.cljc:8:1: error: wrong number of args (2) passed to datascript.db/seqable?
corpus/cljc/test_cljc.cljc:3:26: warning: unused binding y
...
```
