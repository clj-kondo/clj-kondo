# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

clj-kondo is a static analyzer and linter for Clojure, ClojureScript, and EDN. It performs analysis without executing code and can be compiled to a native binary using GraalVM.

## Common Commands

### Running clj-kondo from source
```bash
clojure -M:clj-kondo/dev --lint <path>
clojure -M:clj-kondo/dev --lint - <<< "(defn foo [x] x)"
```

### Testing
```bash
# Full test suite (uses Clojure 1.10.3)
script/test

# Test with native binary
CLJ_KONDO_TEST_ENV=native script/test

# Single namespace
clojure -M:test -n clj-kondo.impl.types-test

# Single test
clojure -M:test -v clj-kondo.impl.types-test/x-is-y-implies-y-could-be-x

# Regression tests (run automatically on PRs)
clojure -X:test:test-regression

# Show full stacktraces on exceptions
CLJ_KONDO_DEV=true clojure -M:test -n <namespace>
```

### REPL
```bash
clojure -M:test:cider-nrepl  # includes test sources
lein with-profiles +test repl
```

### Profiling
```bash
clojure -M:profiler --lint src test
# Flamegraph output: /tmp/clj-async-profiler/results
```

### Building
```bash
lein uberjar                    # Build uberjar
GRAALVM_HOME=/path/to/graalvm script/compile  # Native binary (requires GraalVM 24+)
```

## Architecture

### Entry Points
- `clj-kondo.core` - Public API (`run!`, `print!`, `merge-configs`)
- `clj-kondo.main` - CLI entry point
- `clj-kondo.hooks-api` - API for user-defined hooks

### Core Implementation (`src/clj_kondo/impl/`)
- `core.clj` - Orchestrates file processing (dirs, jars, classpath)
- `analyzer.clj` - Main analysis engine (~3700 lines), walks AST nodes
- `linters.clj` - ~130+ linting rules implementation
- `namespace.clj` - Namespace tracking and inter-file dependencies
- `config.clj` - Configuration resolution and defaults
- `cache.clj` - Transit-based cache for namespace info
- `types.clj` - Optional type checking system
- `macroexpand.clj` - Macro expansion handling

### Specialized Analyzers (`src/clj_kondo/impl/analyzer/`)
Library-specific analysis: core.async, re-frame, spec, potemkin, datalog, compojure, match, etc.

### Source Paths
- `src/` - Main source code
- `parser/` - Custom parser (based on rewrite-clj)
- `inlined/` - Inlined tools.reader code
- `resources/` - Built-in configuration and cache

## Key Design Principles

1. **Dual mode**: Works for single files (editor) AND entire projects
2. **Cache-enhanced but not required**: No false positives without cache
3. **Low latency**: Prioritize editor feedback speed
4. **Single config**: `.clj-kondo/config.edn` is the central configuration
5. **Non-intrusive**: Users shouldn't change code just for the linter

## Adding a New Linter

1. Add the linter implementation to `impl/linters.clj`
2. Add the linter keyword with default config to `impl/config.clj` in `default-config`
3. Add tests covering the new linter
4. Run `clojure -X:test:test-regression` to check for unintended changes

## Development Workflow

1. Create a GitHub issue first describing the problem and alternatives
2. Get agreement on approach before coding
3. Create PR with minimal changes
4. Tests are required for all changes
5. Don't force-push on PR branches (will be squashed on merge)

# Clojure REPL Evaluation

The command `clj-nrepl-eval` is installed on your path for evaluating Clojure code via nREPL.

**Discover nREPL servers:**

`clj-nrepl-eval --discover-ports`

**Evaluate code:**

`clj-nrepl-eval -p <port> "<clojure-code>"`

With timeout (milliseconds)

`clj-nrepl-eval -p <port> --timeout 5000 "<clojure-code>"`

The REPL session persists between evaluations - namespaces and state are maintained.
Always use `:reload` when requiring namespaces to pick up changes.
