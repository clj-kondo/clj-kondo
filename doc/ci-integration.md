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

### pre-commit framework

`clj-kondo` is supported by the pre-commit framework for managing git commit hooks. To enable, add to your `.pre-commit-config.yaml`:

```yaml
- repo: https://github.com/clj-kondo/clj-kondo
    rev: v2022.04.25
    hooks:
      - id: clj-kondo
```

Check out [pre-commit](https://pre-commit.com/) for additional resources.

## GitHub

A number of [GitHub Actions](https://github.com/features/actions) that use `clj-kondo` are available:

- [setup-clj-kondo](https://github.com/marketplace/actions/setup-clj-kondo)
- [clojure-lint-action](https://github.com/marketplace/actions/clj-kondo-checks)

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
    - name: Install clj-kondo
      uses: DeLaGuardo/setup-clj-kondo@afc83dbbf4e7e32e04649e29dbf30668d30e9e3e
      with:
        version: '2022.01.15'

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
