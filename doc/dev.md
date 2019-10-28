# Developer documentation

## Design principles

1) Linters should be designed to work in these modes:

   1. a single file at a time, integrated in an editor (single-file for short)
   2. an entire directory, project or classpath

    These modes should work with or without a cache directory.

2) The cache directory should only be used to enhance linting, not as a reliance. There should be no false positives when the cache directory is missing or isn't fully populated for a project. As people edit their files, the cache gets incrementally more populated and the linting experience improves organically.

3) Linters should be designed with editor feedback in mind and thus should have the lowest latency possible. Reading from the cache takes time and should be done carefully, only selecting the required information (e.g. only load files for namespaces that were actually used in the file being linted).

4) Project-wide analysis should be done using the analysis export and probably not as a linter (see 1, 2 and 3).

5) Configuration should live in one place as much as possible. The `.clj-kondo/config.edn` is the preferred location for configuration. This has the following reasons:

   1. Single-file mode should be able to find the configuration it needs in a predicable location. Scattering configuration in metadata across libraries does not work well for this.
   2. Configuration should be able to live in a project's source repository, so team members can profit from each others additions.

## REPL

### lein

To get a REPL that also includes the test source directory, run:

    lein with-profiles +test repl

### tools.deps

This is how [borkdude](https://github.com/borkdude) starts his REPL using CIDER:

    clojure -A:test:cider-nrepl

The `test` alias includes sources in the `test` directory on the classpath.

Once started, I connect from Emacs using `cider-connect`.

The alias `cider-nrepl` is defined in his `~/.clojure/deps.edn`:

``` clojure
:cider-nrepl
{:extra-deps {nrepl/nrepl {:mvn/version "0.6.0"}
              refactor-nrepl {:mvn/version "2.5.0-SNAPSHOT"}
              cider/cider-nrepl {:mvn/version "0.22.0-beta4"}}
 :main-opts ["-m" "nrepl.cmdline" "--middleware"
             "[cider.nrepl/cider-middleware,refactor-nrepl.middleware/wrap-refactor]"]}
```

## Tests

To test clj-kondo on the JVM, run:

    script/test

To test the native binary of clj-kondo, run:

    CLJ_KONDO_TEST_ENV=native script/test

To test a single namespace:

    clojure -A:test -n clj-kondo.impl.types-test

or:

    lein test :only clj-kondo.impl.types-test

To run a single test:

    clojure -A:test -v clj-kondo.impl.types-test/x-is-y-implies-y-could-be-x

or:

    lein test :only clj-kondo.impl.types-test/x-is-y-implies-y-could-be-x

In case of an exception, you may want to prefix the above lines with `CLJ_KONDO_DEV=true` to see the entire stacktrace.

## Build

### Uberjar

    lein uberjar

### Native

Read [here](build.md) how to build the native binary.
