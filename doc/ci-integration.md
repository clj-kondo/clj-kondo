# CI integration

After correctly setting up `clj-kondo`, you might want to leverage its powers during the test / build phase your project.

## Using CI-specific configuration

Since `clj-kondo` merges configuration files and [gives precedence to config files provided explicitly](config.md), a useful thing to do when integrating it with your CI workflow is to specify a custom configuration file, such as `.clj-kondo/ci-config.edn` with CI-specific overrides.

Then put in your pipeline the execution of the tool as

```
clj-kondo --lint src --config .clj-kondo/ci-config.edn
```

In this way, you can keep your configuration in the standard `config.edn` file and that will continue to work during development, with the overrides only be used during CI.

## GitHub

A number of [GitHub Actions](https://github.com/features/actions) that use `clj-kondo` are available:

- [clojure-lint-action](https://github.com/marketplace/actions/clj-kondo-checks)
- [lint-clojure](https://github.com/marketplace/actions/clj-kondo)

