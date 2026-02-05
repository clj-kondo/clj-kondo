# ADR 0001: Optimize Dependency Analysis

## Status

Proposed

## Context

When linting a project with `--dependencies`, clj-kondo analyzes dependency JARs to extract var definitions (names, arities, types, visibility) which are cached and used when linting source files.

### Current `--dependencies` behavior

The `--dependencies` flag currently enables:
1. Skip registering lint findings (`reg-finding!`)
2. Skip storing `:expr` nodes in var usages (memory optimization)
3. JAR skip-marker system - tracks analyzed JARs to avoid re-analyzing on subsequent runs
4. Full analysis still runs (body analysis, usage tracking, etc.)

### The problem

Significant work is still done that provides no benefit for dependency analysis:
- Tracking var usages (`:used-vars`) - used for unused-require detection
- Tracking binding usages - used for unused-binding detection
- These accumulate large data structures that consume memory

### Distinction: `--dependencies` vs `--skip-lint`

| Flag | Analysis | Cache | JAR skip markers | Analysis output |
|------|----------|-------|------------------|-----------------|
| `--dependencies` | Yes | Yes | **Yes** | No |
| `--skip-lint` (alone) | No | No | No | No |
| `--skip-lint` + `{:analysis {...}}` | Yes | Yes | No | Yes |

Key differences:
- `--dependencies`: CLI-oriented, for scanning classpath into cache with JAR tracking
- `--skip-lint` + analysis config: API-oriented, for getting analysis output without lint warnings

### How clojure-lsp handles this

clojure-lsp uses `--skip-lint` + analysis config (not `--dependencies`) for external paths:

```clojure
(-> config
    (assoc :skip-lint true)
    (assoc-in [:config :analysis]
      {:var-usages false                    ;; skip usage tracking
       :var-definitions {:shallow true}     ;; skip body analysis
       ...}))
```

clojure-lsp avoids `--dependencies` because:
1. It has its own caching system (`analysis-checksums`)
2. It doesn't want clj-kondo's JAR skip markers interfering
3. It needs analysis output, which `--dependencies` doesn't provide

This means clojure-lsp already gets optimized dependency analysis via explicit config. The optimization proposed here is for **CLI users** of `--dependencies`.

### Trade-off: Type inference

clojure-lsp uses `:var-definitions {:shallow true}` which skips body analysis. This means:
- Type hints (`^String [x]`) are preserved
- Inferred return types (from analyzing function bodies) are lost

For CLI `--dependencies` usage, we want to preserve type inference so users get type warnings when calling dependency functions.

## Decision Drivers

1. **Preserve type inference**: Keep full body analysis for return type inference
2. **Reduce memory**: Skip accumulating `:used-vars` and binding tracking
3. **CLI focus**: Optimize `--dependencies` flag, not `--skip-lint` + analysis path
4. **Consistency**: Follow existing pattern in `reg-finding!`

## Proposed Decision

**Skip var usage and binding tracking when `:dependencies` is true**

This provides memory reduction while preserving type inference. The optimization only affects `--dependencies`, not the `--skip-lint` + analysis path used by clojure-lsp.

### Implementation

```clojure
;; In namespace.clj

(defn reg-var-usage! [{:keys [dependencies] :as ctx} ns-sym usage]
  (when-not dependencies
    (when-not (:interop? usage)
      ;; existing implementation
      )))

(defn reg-used-binding! [{:keys [dependencies] :as ctx} ...]
  (when-not dependencies
    ;; existing implementation
    ))
```

### Why only `:dependencies`?

- `--skip-lint` alone skips analysis entirely
- `--skip-lint` + analysis config: User wants analysis output, may need usage tracking
- `--dependencies`: Populates cache for later linting, doesn't need usage tracking

## Consequences

### Positive
- Reduced memory during `--dependencies` analysis
- Faster dependency analysis
- Type inference preserved (unlike clojure-lsp's shallow approach)
- Consistent with existing `reg-finding!` pattern

### Negative
- Slightly more complex code with dependency checks

### Neutral
- No impact on clojure-lsp (uses different code path)
- Cache format unchanged
- API unchanged

## Related

- `issues/parallelize-analysis.md` - Previous experiment with incremental cache writing
- `src/clj_kondo/impl/findings.clj:67-79` - Existing pattern for skipping work in dependency mode
- clojure-lsp `kondo.clj:config-for-external-paths` - How clojure-lsp optimizes dependency analysis
