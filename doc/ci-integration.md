# CI integration

After correctly setting up `clj-kondo`, you might want to leverage its powers during the test / build phase your project.

## Using CI-specific configuration

Since `clj-kondo` merges configuration files and [gives precedence to config files provided explicitly](config.md), a useful thing to do when integrating it with your CI workflow is to specify a custom configuration file, such as `.clj-kondo/ci-config.edn` with CI-specific overrides.

Then put in your pipeline the execution of the tool as

```
clj-kondo --lint src --config .clj-kondo/ci-config.edn
```

In this way, you can keep your configuration in the standard `config.edn` file and that will continue to work during development, with the overrides only be used during CI.

## Pre-commit hook

You can use this pre-commit hook to run `clj-kondo` over files you changed before
committing them. Save it into `.git/hooks/pre-commit`.

``` shell
#!/bin/sh

# To enable this hook, rename this file to "pre-commit".

if git rev-parse --verify HEAD >/dev/null 2>&1
then
    against=HEAD
else
    # Initial commit: diff against an empty tree object
    against=$(git hash-object -t tree /dev/null)
fi

if !(git diff --cached --name-only --diff-filter=AM $against | grep -E '.clj[cs]?$' | xargs clj-kondo --lint)
then
    echo
    echo "Error: new clj-kondo errors found. Please fix them and retry the commit."
    exit 1
fi

exec git diff-index --check --cached $against --
```

Also check out these resources:

- [lein-githooks](https://github.com/gmorpheme/lein-githooks)
- [Clojureverse pre-commit hook discussion](https://clojureverse.org/t/what-is-the-preferred-way-to-add-a-pre-commit-hook-to-re-frame-project/5305/4)

## GitHub

A number of [GitHub Actions](https://github.com/features/actions) that use `clj-kondo` are available:

- [clojure-lint-action](https://github.com/marketplace/actions/clj-kondo-checks)
- [lint-clojure](https://github.com/marketplace/actions/clj-kondo)
- [Mega-Linter](https://github.com/marketplace/actions/mega-linter): 100% open-source linters aggregator
- [setup-clj-kondo](https://github.com/marketplace/actions/setup-clj-kondo)

### Linter Output Integration

Github Actions can integrate with clj-kondo output using a custom output pattern. To enable this, set `clj-kondo`'s config to: 

``` edn
{:output {:pattern "::{{level}} file={{filename}},line={{row}},col={{col}}::{{message}}"}}
```

Example configuration with action [setup-clj-kondo](https://github.com/marketplace/actions/setup-clj-kondo):

```
on: push
name: Push / PR Builder
jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
    - name: Setup clj-kondo
      uses: DeLaGuardo/setup-clj-kondo@822352b8aa37d5c94135e67f7b4e2f46c08008a8
      with:
        version: '2020.04.05'

    - uses: actions/checkout@v2

    - name: Lint
      run: clj-kondo --lint src --config '{:output {:pattern "::{{level}} file={{filename}},line={{row}},col={{col}}::{{message}}"}}'
```

For more information on Github Action Commands, see [Workflow Commands for Github Actions](https://help.github.com/en/actions/reference/workflow-commands-for-github-actions#setting-a-warning-message).

Also see
[this](https://rymndhng.github.io/2020/04/03/Integrate-clj-kondo-with-Github-Actions/)
blog article by Raymond Huang.

## Other resources
- [Using clj-kondo in a notebook for Nextjournal CI](https://nextjournal.com/blog/ci)
