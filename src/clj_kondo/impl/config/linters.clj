(ns clj-kondo.impl.config.linters)

(def config
  '{:bb.edn-cyclic-task-dependency
    {:name "Bb.edn cyclic task dependency"
     :default-level :error
     :description "Checks for cyclic dependencies in `bb.edn` files."
     :example-trigger
     "
     {:tasks {a {:depends [b]
                 :task (println \"a\")}
              b {:depends [a]}}}
     "
     :example-message "Cyclic task dependency: a -> b -> a"
     }

    :bb.edn-task-missing-docstring
    {:name "Bb.edn task docstring missing"
     :default-level :off
     :description "Checks for missing docstring for map tasks in `bb.edn` files."
     :example-trigger "{:tasks {a {:task (call-fn}]}"
     :example-message "Docstring missing for task: a"
     }

    :bb.edn-undefined-task
    {:name "Bb.edn dependency on undefined task"
     :default-level :error
     :description "Checks for undefined task dependencies in `bb.edn` files."
     :example-trigger "{:tasks {run {:depends [compile]}}}"
     :example-message "Depending on undefined task: compile"
     }

    :bb.edn-unexpected-key
    {:name "Bb.edn Unexpected key"
     :default-level :warning
     :description "Checks for unexpected keys in `bb.edn` files."
     :example-trigger "{:requires [[babashka.fs :as fs]]}"
     :example-message "Global :requires belong in the :tasks map."
     }

    :clj-kondo-config
    {:name "Clj-kondo config"
     :default-level :warning
     :description "Checks for common errors in `.clj-kondo/config` files."
     :example-trigger "{:linters {:foo 1}}"
     :example-message "Unexpected linter name: :foo"
     }

    :cond-else
    {:name "Cond-else"
     :default-level :warning
     :description "Prefer `:else` as `cond` default branch."
     :example-trigger "(cond (odd? (rand-int 10)) :foo :default :bar)"
     :example-message "use :else as the catch-all test expression in cond"
     }

    :conflicting-alias
    {:name "Conflicting-alias"
     :default-level :error
     :description "Checks for conflicting alias."
     :example-trigger
     "
     (require '[clojure.string :as s]
              '[clojure.spec.alpha :as s])
     "
     :example-message "Conflicting alias for clojure.spec.alpha"
     }

    :conflicting-fn-arity
    {:name "Conflicting arity"
     :default-level :error
     :description "Warns when an overloaded function defines multiple argument vectors with the same arity."
     :example-trigger "(fn ([x] x) ([y]) x)"
     :example-message "More than one function overload with arity 2."
     }

    :consistent-alias
    {:name "Consistent-alias"
     :default-level :warning
     :description "Checks namespace aliases against the provided map of namespace to alias."
     :example-trigger
     "
     (ns foo (:require [new.api :as api]))
     (ns bar (:require [old.api :as old-api]))
     (ns baz (:require [old.api :as api]))
     "
     :example-message "Inconsistent alias. Expected old-api instead of api."
     :config-spec {:aliases {symbol? symbol?}}
     :config-description
     "
     The consistent alias linter needs pre-configured aliases for namespaces that
     should have a consistent alias. This configuration:

     ``` clojure
     {:linters {:consistent-alias {:aliases {old.api old-api}}}}
     ```

     will produce this warning:

     ``` clojure
     Inconsistent alias. Expected old-api instead of api.
     ```
     "
     }

    :datalog-syntax
    {:name "Datalog syntax"
     :default-level :error
     :description "Checks for invalid datalog syntax. This linter is implemented using [io.lambdaforge/datalog-parser](https://github.com/lambdaforge/datalog-parser). Also see this [blog post](https://lambdaforge.io/2019/11/08/clj-kondo-datalog-support.html)."
     :example-trigger
     "
     (ns user (:require [datahike.api :refer [q]]))
     (q '[:find ?a :where [?b :foo _]] 42)
     "
     :example-message "Query for unknown vars: [?a]"
     }

    :deprecated-var
    {:name "Deprecated var"
     :default-level :warning
     :description "Avoid using deprecated vars."
     :example-trigger "(def ^:deprecated x) x"
     :example-message "#'user/x is deprecated"
     :config-spec {:exclude {symbol? {:namespaces [(s/or symbol? string?)]
                                      :defs [(s/or symbol? string?)]}}}
     :config-description
     "
     Say you have the following function:

     ``` clojure
     (ns app.foo)
     (defn foo {:deprecated \"1.9.0\"} [])
     ```

     and you still want to be able to call it without getting a warning, for example
     in function in the same namespace which is also deprecated:

     ``` clojure
     (defn bar {:deprecated \"1.9.0\"} []
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
     {:linters {:deprecated-var {:exclude {app.foo/foo {:namespaces [\".*-test$\"]}}}}}
     ```
     "
     }

    :deps.edn
    {:name "Deps.edn"
     :default-level :warning
     :description "Checks for common errors in `deps.edn` and `bb.edn` files."
     :example-trigger "{:deps {foo/bar \"2020.10.11\"}}"
     :example-message "Expected map, found: java.lang.String"
     }

    :discouraged-namespace
    {:name "Discouraged namespace"
     :default-level :warning
     :description "Avoid requiring or using provided namespaces."
     :example-trigger "(require '[clojure.java.jdbc :as jdbc])"
     :example-message "Use next.jdbc instead of clojure.java.jdbc"
     :config-spec {symbol? {:message string?}}
     :config-description
     "
     ```clojure
     {:linters {:discouraged-namespace {clojure.java.jdbc {:message \"Use next.jdbc instead of clojure.java.jdbc\"}}}}
     ```

     The matching namespace symbol may be given a group name using a regex pattern.

     ```clojure
     {:ns-groups [{:pattern \"clojure\\.java\\.jdbc.*\"
                   :name jdbc-legacy}]
      :linters {:discouraged-namespace {jdbc-legacy {:message \"Use next.jdbc instead of clojure.java.jdbc\"}}}}
     ```

     Add `:discouraged-namespace` linter into `:config-in-ns` to specify that specific namespaces are discouraged to be used in some namespace of ns-group.

     ```clojure
     {:config-in-ns {app.jdbc {:linters {:discouraged-namespace {clojure.java.jdbc {:message \"Use next.jdbc instead of clojure.java.jdbc\"}}}}}}
     ```
     "
     }

    :discouraged-var
    {:name "Discouraged var"
     :default-level :warning
     :description "Avoid using specified vars."
     :example-trigger "(read-string \"(+ 1 2 3)\")"
     :example-message "Use edn/read-string instead of read-string"
     :config-spec {symbol? {:message string?}}
     :config-description
     "
     ``` clojure
     {:linters {:discouraged-var {clojure.core/read-string {:message \"Use edn/read-string instead of read-string\"}}}}
     ```

     The matching namespace symbol may be given a group name using a regex pattern.
     "
     }

    :docstring-blank
    {:name "Docstring blank"
     :default-level :warning
     :description "Avoid blank docstring."
     :example-trigger "(defn foo \"\" [a b] 1)"
     :example-message "Docstring should not be blank."
     }

    :docstring-leading-trailing-whitespace
    {:name "Docstring leading trailing whitespace"
     :default-level :off
     :description "Checks docstring for leading or trailing whitespace."
     :example-trigger "(defn foo \"Has trailing whitespace. \" [a b] 1)"
     :example-message "Docstring should not have leading or trailing whitespace."
     }

    :docstring-no-summary
    {:name "Docstring no summary"
     :default-level :off
     :description
     "
     Warn when first _line_ of docstring is not a complete sentence. This linter is based on the community [style guide](https://guide.clojure.style/#docstring-summary).

     Explanation by Bozhidar Batsov:

     > The idea is simple - each docstring should start with a one-line
     > sentence. This minimizes the work tools have to do to extract some meaningful
     > summary of what a var does (and as a bonus - it plays great with the Emacs
     > minibuffer, that happens to have a height of 1 line).
     "
     :example-trigger "(defn foo \"not a sentence\" [a b] 1)"
     :example-message "First line of the docstring should be a capitalized sentence ending with punctuation."
     }

    :duplicate-case-test-constant
    {:name "Duplicate case test constant"
     :default-level :error
     :description "Checks for duplicate case test constants."
     :example-trigger "(case x :a 1 :b 2 :a 3)"
     :example-message "Duplicate case test constant: :a"
     }

    :duplicate-map-key
    {:name "Duplicate map key"
     :default-level :error
     :description "Checks for duplicate key in map literals."
     :example-trigger "{:a 1 :a 2}"
     :example-message "duplicate key :a"
     }

    :duplicate-require
    {:name "Duplicate require"
     :default-level :warning
     :description "Checks for namespace that has been required more than once within a namespace."
     :example-trigger
     "
     (ns foo
       (:require [clojure.string :as str]
                 [clojure.string :as str]))
     "
     :example-message "duplicate require of clojure.string"
     }

    :duplicate-set-key
    {:name "Duplicate set key"
     :default-level :error
     :description "Checks for duplicate values in set literals."
     :example-trigger "#{:a :a}"
     :example-message "duplicate set element :a"
     }

    :file
    {:name "File"
     :default-level :error
     :description "Checks that the target file exists."
     :example-trigger "clj-kondo --lint foo.clje"
     :example-message "file does not exist"
     }

    :format
    {:name "Format"
     :default-level :error
     :description "Checks for correct amount of arguments in `format`."
     :example-trigger "(format \"%s\" 1 2)"
     :example-message "Format string expects 1 arguments instead of 2."
     }

    :hook
    {:name "Hook"
     :default-level :error
     :description "Checks that clj-kondo custom hooks execute correctly."
     :example-trigger "(ns foo) (defn fixed-arity [{:keys [:node]}] {:a :sexpr 1})"
     :example-message "WARNING: error while trying to read hook for foo/fixed-arity: The map literal starting with :a contains 3 form(s)."
     }

    :inline-def
    {:name "Inline def"
     :default-level :warning
     :description "Checks for non-toplevel usage of `def` (and `defn`, etc.)."
     :example-trigger "(defn foo [] (def x 1))"
     :example-message "inline def"
     }

    :invalid-arity
    {:name "Invalid arity"
     :default-level :error
     :description "Checks that functions and macros are called with the correct amount of arguments."
     :example-trigger "(inc)"
     :example-message "clojure.core/inc is called with 0 args but expects 1"
     :config-spec {:skip-args [qualified-symbol?]}
     :config-description
     "
     Some macros rewrite their arguments and therefore can cause false positive arity errors. Imagine the following silly macro:

     ``` clojure
     (ns silly-macros)

     (defmacro with-map [m [fn & args]]
       `(~fn ~m ~@args))
     ```

     which you can call like:

     ``` clojure
     (silly-macros/with-map {:a 1 :d 2} (select-keys [:a :b :c])) ;;=> {:a 1}
     ```

     Normally a call to this macro will give an invalid arity error for `(select-keys [:a :b :c])`, but not when you use the following configuration:

     ``` clojure
     {:linters {:invalid-arity {:skip-args [silly-macros/with-map]}}}
     ```
     "
     }

    :keyword-binding
    {:name "Keyword in binding vector"
     :default-level :off
     :description "Checks for keywords in a `:keys` binding vector."
     :example-trigger "(let [{:keys [:a]} {:a 1}] a)"
     :example-message "Keyword binding should be a symbol: :a"
     }

    :line-length
    {:name "Line length"
     :default-level :warning
     :description "Check that lines are not longer than a configured length."
     :example-trigger "<An 81 character line with :max-line-length of 80>"
     :example-message "Line is longer than 80 characters."
     :config-description
     "
     `:max-line-length` is `nil` by default, which disables line length linting.

     The line length linter needs to know how long you are prepared to allow your lines to be. This configuration:

     ``` clojure
     {:linters {:line-length {:max-line-length 120}}}
     ```

     will produce this warning:

     ``` clojure
     Line is longer than 120 characters.
     ```
     "
     :config-default {:max-line-length nil}
     }

    :loop-without-recur
    {:name "Loop without recur"
     :default-level :warning
     :description "Checks that loop contains recur."
     :example-trigger "(loop [])"
     :example-message "Loop without recur."
     }

    :main-without-gen-class
    {:name "Main without gen-class"
     :default-level :off
     :description "Checks that `:gen-class` is present in namespaces that have a `-main` function."
     :example-trigger "(ns foo) (defn -main [& _args])"
     :example-message "Main function without gen-class."
     }

    :misplaced-docstring
    {:name "Misplaced docstring"
     :default-level :warning
     :description "Checks if docstring appears after argument vector instead of before."
     :example-trigger "(defn foo [] \"cool fn\" 1)"
     :example-message "Misplaced docstring."
     }

    :missing-body-in-when
    {:name "Missing body in when"
     :default-level :warning
     :description "Checks if `when` is called only with a condition."
     :example-trigger "(when true)"
     :example-message "Missing body in when."
     }

    :missing-clause-in-try
    {:name "Missing clause in try"
     :default-level :warning
     :description "Checks if `try` expression is missing `catch` or `finally` clause."
     :example-trigger "(try 1)"
     :example-message "Missing catch or finally in try."
     }

    :missing-docstring
    {:name "Missing docstring"
     :default-level :off
     :description "Checks if public var is missing docstring."
     :example-trigger "(defn foo [] 1)"
     :example-message "Missing docstring."
     }

    :missing-else-branch
    {:name "Missing else branch"
     :default-level :warning
     :description "Checks for missing else branch in `if` expressions."
     :example-trigger "(if :foo :bar)"
     :example-message "Missing else branch."
     }

    :missing-map-value
    {:name "Missing map value"
     :default-level :error
     :description "Checks for map literal with an uneven amount of elements, i.e. one of the keys is missing a value."
     :example-trigger "{:a 1 :b}"
     :example-message "Missing value for key :b"
     }

    :missing-test-assertion
    {:name "Missing test assertion"
     :default-level :warning
     :description "Checks that `deftest` expression has test assertion."
     :example-trigger "(require '[clojure.test :as test]) (test/deftest foo (pos? 1))"
     :example-message "Missing test assertion"
     }

    :namespace-name-mismatch
    {:name "Namespace name mismatch"
     :default-level :error
     :description "Checks that the namespace in the `ns` form corresponds with the file name of the file."
     :example-trigger
     "
     ;; file named `foo.clj`
     (ns bar)
     "
     :example-message "Namespace name does not match file name: bar"
     }

    :non-arg-vec-return-type-hint
    {:name "Non-arg vec return type hint"
     :default-level :warning
     :description
     "
     Checks that a return type type hint in `defn` is placed on the argument vector (CLJ only).

     Read [this](https://github.com/clj-kondo/clj-kondo/issues/1331) issue for more background information on this linter.
     "
     :example-trigger "(defn ^String foo [] \"cool fn\")"
     :example-message "Prefer placing return type hint on arg vector: String"
     :dialects #{:clj}
     }

    :not-a-function
    {:name "Not a function"
     :default-level :error
     :description "Avoid using booleans, strings, chars, or numbers in function position."
     :example-trigger "(true 1)"
     :example-message "A boolean is not a function"
     :config-spec {:skip-args [qualified-symbol?]}
     }

    :not-empty?
    {:name "Not empty?"
     :default-level :warning
     :description "Prefer `(seq ...)` over `(not (empty? ...))`."
     :example-trigger "(not (empty? []))"
     :example-message "use the idiom (seq x) rather than (not (empty? x))"
     }

    :private-call
    {:name "Private call"
     :default-level :error
     :description "Avoid using private vars."
     :example-trigger
     "
     (ns foo) (defn- f [])
     (ns bar (:require [foo]))
     (foo/f)
     "
     :example-message "#'foo/f is private"
     }

    :quoted-case-test-constant
    {:name "Quoted case test constant"
     :default-level :warning
     :description "Avoid quoted test case constants."
     :example-trigger "(case x 'a 1 :b 2)"
     :example-message "Case test is compile time constant and should not be quoted."
     }

    :redefined-var
    {:name "Redefined var"
     :default-level :warning
     :description "Avoid redefining vars."
     :example-trigger "(def x 1) (def x 2)"
     :example-message "redefined var #'user/x"
     }

    :reduce-without-init
    {:name "Reduce without initial value"
     :default-level :off
     :description
     "
     Avoid using `reduce` without an explicit initial value.

     Read [this article](https://purelyfunctional.tv/issues/purelyfunctional-tv-newsletter-313-always-use-the-3-argument-version-of-reduce/) why leaving it out can be problematic.
     "
     :example-trigger "(reduce max [])"
     :example-message "Reduce called without explicit initial value."
     :config-spec {:exclude [qualified-symbol?]}
     :config-description
     "
     To suppress the warning for a specific reducing function:

     ```clojure
     {:linters {:reduce-without-init {:exclude [clojure.core/max cljs.core/max]}}}
     ```
     "
     }

    :redundant-call
    {:name "Redundant call"
     :default-level :off
     :description
     "
     Avoid redundant calls.

     The warning arises when a single argument is passed to a known function or macro that that returns its arguments.

     `clojure.core` and `cljs.core` functions and macros that trigger this lint:
     * `->`, `->>`
     * `cond->`, `cond->>`
     * `some->`, `some->>`
     * `comp`, `partial`
     * `merge`
     "
     :example-trigger "(-> 1)"
     :example-message "Single arg use of -> always returns the arg itself."
     :config-spec {:exclude #{qualified-symbol?}
                   :include #{qualified-symbol?}}
     :config-description
     "
     Use `:exclude` to suppress warnings for the built-in list. Use `:include` to warn on additional vars.

     ```clojure
     {:linters {:redundant-call {:exclude #{clojure.core/->}
                                 :include #{clojure.core/conj!}}}}
     ```clojure
     "
     }

    :redundant-do
    {:name "Redundant do"
     :default-level :warning
     :description
     "
     Avoid redundant `do` calls.

     The warning usually arises because of an explicit or implicit do as the direct parent s-expression.
     "
     :example-trigger "(defn foo [] (do 1))"
     :example-message "redundant do"
     }

    :redundant-expression
    {:name "Redundant expression"
     :default-level :warning
     :description "Avoid redundant expressions."
     :example-trigger "(do 1 2)"
     :example-message "Redundant expression: 1"
     }

    :redundant-fn-wrapper
    {:name "Redundant fn wrapper"
     :default-level :off
     :description "Avoid redundant function wrappers."
     :example-trigger "#(inc %)"
     :example-message "Redundant fn wrapper"
     }

    :redundant-let
    {:name "Redundant let"
     :default-level :warning
     :description
     "
     Avoid redundant `let` calls.

     The warning usually arises because directly nested lets.
     "
     :example-trigger "(let [x 1] (let [y 2] (+ x y)))"
     :example-message "Redundant let expression."
     }

    :refer
    {:name "Refer"
     :default-level :off
     :description
     "
     Avoid using `:refer`.

     This can be used when one wants to enforce usage of aliases.
     "
     :example-trigger "(ns foo (:require [clojure.set :refer [union]]))"
     :example-message "require with :refer"
     :config-spec {:exclude [symbol?]}
     :config-description
     "
     Suppress warnings for given namespaces.

     Config example:
     ```clojure
     {:linters {:refer {:exclude [clojure.set]}}}
     ```
     "
     }

    :refer-all
    {:name "Refer all"
     :default-level :warning
     :description "Avoid using `:refer :all`."
     :example-trigger "(ns foo (:require [clojure.set :refer :all]))"
     :example-message "use alias or :refer"
     :config-spec {:exclude [symbol?]}
     :config-description
     "
     Suppress warnings for given namespaces.

     Config example:
     ```clojure
     {:linters {:refer {:exclude [clojure.set]}}}
     ```
     "
     }

    :shadowed-var
    {:name "Shadowed var"
     :default-level :off
     :description "Check if var is shadowed by local."
     :example-trigger "(def x 1) (let [x 2] x)"
     :example-message "Shadowed var: user/x."
     :config-spec {:exclude [symbol?]
                   :include [symbol?]
                   :suggestions {symbol? symbol?}}
     :config-description
     "
     Use `:exclude` to suppress warnings for specific binding names. Use `:include` to warn only for specific names.

     To avoid shadowing core vars you can also use `:refer-clojure` + `:exclude` in the `ns` form.
     "
     }

    :single-key-in
    {:name "Single key in"
     :default-level :off
     :description "Avoid using an associative path function with a single value path."
     :example-trigger "(get-in {:a 1} [:a])"
     :example-message "get-in with single key."
     }

    :single-logical-operand
    {:name "Single logical operand"
     :default-level :warning
     :description "Avoid calling `and` or `or` with only one argument."
     :example-trigger "(and 1)"
     :example-message "Single arg use of clojure.core/and always returns the arg itself."
     }

    :single-operand-comparison
    {:name "Single operand comparison"
     :default-level :warning
     :description "Avoid calling comparison function with only one argument."
     :example-trigger "(< 1)"
     :example-message "Single operand use of clojure.core/< is always true."
     }

    :syntax
    {:name "Syntax"
     :default-level :error
     :description "Reject invalid syntax."
     :example-trigger "#(#())"
     :example-message "Nested #()s are not allowed."
     }

    :type-mismatch
    {:name "Type mismatch"
     :default-level :error
     :description
     "
     Check for type mismatches.

     For example, passing a keyword where a number is expected.

     You can add or override type annotations. See [types.md](https://github.com/clj-kondo/clj-kondo/blob/master/doc/types.md).
     "
     :example-trigger "(inc :foo)"
     :example-message "Expected: number, received: keyword."
     }

    :unbound-destructuring-default
    {:name "Unbound destructuring default"
     :default-level :warning
     :description "Check if binding in `:or` occurs in destructuring."
     :example-trigger "(let [{:keys [:i] :or {i 2 j 3}} {}] i)"
     :example-message "j is not bound in this destructuring form."
     }

    :unexpected-recur
    {:name "Unexpected recur"
     :default-level :error
     :description "Reject using `recur` in a non-tail position."
     :example-trigger "(loop [] (recur) 1)"
     :example-message "Recur can only be used in tail position."
     }

    :unreachable-code
    {:name "Unreachable code"
     :default-level :warning
     :description "warn on unreachable code."
     :example-trigger "(cond :else 1 (odd? 1) 2)"
     :example-message "unreachable code"
     }

    :unresolved-namespace
    {:name "Unresolved namespace"
     :default-level :warning
     :description "Check if the namespace of a qualified symbol is required in the `ns` form."
     :example-trigger "foo.bar/baz"
     :example-message "Unresolved namespace foo.bar. Are you missing a require?"
     :config-spec {:exclude [symbol?]
                   :report-duplicates boolean?}
     :config-description
     "
     Use `:exclude [foo.bar]` to suppress warnings for the namespace `foo.bar`.

     Use `:report-duplicates true` to raise a lint warning for every occurence instead of the first in each file.
     "
     }

    :unresolved-symbol
    {:name "Unresolved symbol"
     :default-level :error
     :description "Reject unresolved symbols."
     :example-trigger "x"
     :example-message "Unresolved symbol: x"
     :config-spec {:exclude [(s/or (symbol?) (symbol? [symbol?]))]
                   :report-duplicates boolean?}
     :config-default {:exclude [(user/defproject)
                                (clojure.test/are [thrown? thrown-with-msg?])
                                (cljs.test/are [thrown? thrown-with-msg?])
                                (clojure.test/is [thrown? thrown-with-msg?])
                                (cljs.test/is [thrown? thrown-with-msg?])]
                      :report-duplicates false}
     :config-description
     "
     In the following code, `match?` is a test assert expression brought in by `matcher-combinators.test`. We don't want it to be reported as an unresolved symbol.

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
     (hugsql/def-db-fns \"select_things.sql\")

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
     (let-programs [clj-kondo \"./clj-kondo\"]
       ,,,)
     ```

     You can report duplicate warnings using:

     ``` clojure
     {:linters {:unresolved-symbol {:report-duplicates true}}}
     ```
     "
     }

    :unresolved-var
    {:name "Unresolved var"
     :default-level :warning
     :description "Checks for unresolved vars from other namespaces."
     :example-trigger "(require '[clojure.set :as set]) (set/onion)"
     :example-message "Unresolved var: set/onion"
     :config-spec {:exclude [symbol?]
                   :report-duplicates boolean?}
     :config-description
     "
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
     "
     }

    :unsorted-required-namespaces
    {:name "Unsorted required namespaces"
     :default-level :off
     :description "Checks if libspecs in `ns` and `require` forms are sorted alphabetically.."
     :example-trigger "(ns foo (:require b a))"
     :example-message "Unsorted namespace: a"
     }

    :unused-binding
    {:name "Unused binding"
     :default-level :warning
     :description "Check for unused bindings."
     :example-trigger "(let [x 1] (prn :foo))"
     :example-message "unused binding x"
     :config-default {:exclude-destructured-keys-in-fn-args false
                      :exclude-destructured-as false
                      :exclude-defmulti-args false}
     :config-spec {:exclude-destructured-keys-in-fn-args boolean?
                   :exclude-destructured-as boolean?
                   :exclude-defmulti-args boolean?}
     :config-description
     "
     To exclude unused bindings from being reported, start their names with
     underscores: `_x`.

     To exclude warnings about key-destructured function arguments, use:

     ``` clojure
     {:linters {:unused-binding {:exclude-destructured-keys-in-fn-args true}}}
     ```

     This will disable warnings for the following example:

     ``` clojure
     (defn f [{:keys [:a :b :c]} d])
     ```

     To disable warnings about `:as` bindings (which can be useful for
     documentation), use:

     ```clojure
     {:linters {:unused-binding {:exclude-destructured-as true}}}
     ```

     This will disable the warning in:

     ``` clojure
     (defn f [{:keys [a b c] :as g}] a b c)
     ```

     To exclude warnings about defmulti dispatch function arguments, use:

     ``` clojure
     {:linters {:unused-binding {:exclude-defmulti-args true}}}
     ```

     This will disable the warning in:

     ``` clojure
     (defmulti f (fn [a b] a))
     ```
     "
     }

    :unused-import
    {:name "Unused import"
     :default-level :warning
     :description "Check for unused imports."
     :example-trigger "(ns foo (:import [java.util UUID]))"
     :example-message "Unused import UUID."
     }

    :unused-namespace
    {:name "Unused namespace"
     :default-level :warning
     :description "Check for required but unused namespaces."
     :example-trigger "(ns foo (:require [bar :as b]))"
     :example-message "Namespace bar is required but never used."
     :config-spec {:exclude [(s/or symbol? string?)]
                   :simple-libspec boolean?}
     :config-default {:simple-libspec false}
     :config-description
     "
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
     {:linters {:unused-namespace {:exclude [\".*\\.specs$\"]}}}
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
     "
     }

    :unused-private-var
    {:name "Unused private var"
     :default-level :warning
     :description "warns on unused private vars."
     :example-trigger "(ns foo) (defn- f [])"
     :example-message "Unused private var foo/f"
     :config-spec {:exclude [qualified-symbol?]}
     :config-description
     "
     To suppress the warning:

     ``` clojure
     {:linters {:unused-private-var {:exclude [foo/f]}}}
     ```
     "
     }

    :unused-referred-var
    {:name "Unused referred var"
     :default-level :warning
     :description "warns about unused referred vars."
     :example-trigger "(ns foo (:require [clojure.set :refer [union]]))"
     :example-message "#'clojure.set/union is referred but never used"
     :config-spec {:exclude {symbol? [symbol?]}}
     :config-description
     "
     Imagine you want to have `taoensso.timbre/debug` available in all of your
     namespaces. Even when you don't use it, you don't want to get a warning about
     it. That can be done as follows:

     ``` clojure
     {:linters {:unused-referred-var {:exclude {taoensso.timbre [debug]}}}}
     ```
     "
     }

    :use
    {:name "Use"
     :default-level :warning
     :description "warns about `:use` or `use`."
     :example-trigger "(ns foo (:use [clojure.set]))"
     :example-message "use :require with alias or :refer"
     :config-description
     "
     This linter is closely tied to [Refer All](#refer-all). Namespaces configured to suppress the `:refer-all` warning will also suppress the `:use` warning.
     "
     }

    :used-underscored-binding
    {:name "Used underscored bindings"
     :default-level :off
     :description "warn when a underscored (ie marked as unused) binding is used."
     :example-trigger "(let [_x 0] _x)"
     :example-message "Using binding marked as unused: _x"
     }

    :warn-on-reflection
    {:name "Warn on reflection"
     :default-level :off
     :description "warns about not setting `*warn-on-reflection*` to true in Clojure namespaces. Defaults to only warning when doing interop."
     :example-trigger "(.length foo)"
     :example-message "Var *warn-on-reflection* is not set in this namespace."
     :config-spec {:warn-only-on-interop boolean?}
     :config-default {:warn-only-on-interop true}
     :config-description "The value of `:warn-only-on-interop` can be set to `false` to always warn in Clojure namespaces."
     }})
