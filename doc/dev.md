# Developer documentation

## Design principles

1) Linters should be designed to add value in both of these modes:

   1. a single file at a time, integrated in an editor (single-file for short)
   2. an entire directory, project or classpath

    These modes should work with or without a cache directory.

2) The cache directory should only be used to enhance linting, not as a reliance. There should be no false positives when the cache directory is missing or isn't fully populated for a project. As people edit their files, the cache gets incrementally more populated and the linting experience improves organically.

3) Linters should be designed with editor feedback in mind and thus should have the lowest latency possible. Reading from the cache takes time and should be done carefully, only selecting the required information (e.g. only load files for namespaces that were actually used in the file being linted).

4) Project-wide analysis should be done using the analysis export and probably not as a linter (see 1, 2 and 3).

5) Configuration should live in one place as much as possible. The `.clj-kondo/config.edn` is the preferred location for configuration. This has the following reasons:

   1. Configuration should be able to live in a project's source repository, so team members can work out the style which they agree on for this project. Supporting external configuration leads to contradicting styles. By committing to a single project configuration team members also benefit from each other's work.
   2. Configuration spread across multiple files is harder to debug.
   3. Single-file mode should be able to find the configuration it needs in a predicable location. Scattering configuration in metadata across libraries does not work well for this.


 6) Clj-kondo should be unobtrusive. Users of clj-kondo should not have to change their code only to make the linter happy. Team members who do not wish to use clj-kondo should not be confronted with clj-kondo-related annotations in their code.
 
## Adding a new linter
If you wish to add a new linter, do not forget to add the appropriate keyword in `clj-kondo.impl.config/default-config`, the map that defines the default configuration.

This is necessary because only the linters with a keyword in the default config appear in the report.  

## PR

### Linting diff

When implementing a new linter or changing an existing one, run the
`script/diff` to view changed linting output between your branch and the master
branch on Github. Please include the output in a PR. This step may be automated
in the future using a Github action.

### Force-push

Please do not use `git push --force` on your PR branch for the following
reasons:

- It makes it more difficult for others to contribute to your branch if needed.
- CircleCI doesn't play well with it: it might try to fetch a commit which
  doesn't exist anymore.
- Your PR will be squashed anyway.

## Invoking clj-kondo from the command line

```
$ clojure -A:clj-kondo --lint - <<< "(defn foo [x] (if-let [x 1] x x x))"
<stdin>:1:15: error: if-let body requires one or two forms
linting took 73ms, errors: 1, warnings: 0
```

## REPL

### lein

To get a REPL that also includes the test source directory, run:

    lein with-profiles +test repl

### tools.deps

This is how [borkdude](https://github.com/borkdude) starts his REPL using CIDER:

    clojure -A:test:cider-nrepl

The `test` alias includes sources in the `test` directory on the classpath.

Once started, he connect from Emacs using `cider-connect`. You may prefer to use `cider-jack-in` instead.

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
