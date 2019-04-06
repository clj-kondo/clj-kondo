# Running on the JVM

Using the binary is recommended for better startup time, but you can run this
linter with as a regular Clojure program on the JVM as well.

## leiningen

You can add clj-kondo to `~/.lein/profiles.clj` to make it available as a `lein` command:

``` clojure
{:user {:dependencies [[clj-kondo "RELEASE"]]
        :aliases {"clj-kondo" ["run" "-m" "clj-kondo.main"]}
```

``` shellsession
$ lein clj-kondo --lint src
```

## tools.deps.alpha

Run clj-kondo as an ad-hoc command line dependency:

``` shellsession
$ clj -Sdeps '{:deps {clj-kondo {:mvn/version "RELEASE"}}}' -m clj-kondo.main --lint src
```

Or add it as an alias to `~/.clojure/deps.edn`:

``` clojure
{:aliases
 {:clj-kondo
  {:extra-deps {clj-kondo {:mvn/version "RELEASE"}}
   :main-opts ["-m" "clj-kondo.main"]}}}
```

``` shellsession
$ clj -A:clj-kondo --lint src
```
