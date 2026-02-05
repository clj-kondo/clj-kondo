Clj-kondo tries to parallize its analysis based on the number of cores.
When linting a big project like ~/dev/metabase, it currently takes 27s.

```
clojure -M:clj-kondo/dev --copy-configs --dependencies --lint  --parallel   188,05s  user 6,59s system 707% cpu 27,505 total
```

or the second time (clj-kondo recognizes already linted .jar files) and skips them.

```
clojure -M:clj-kondo/dev --copy-configs --dependencies --lint  --parallel   64,26s  user 2,72s system 381% cpu 17,543 total
```

There are probably two ways that we can speed up the parallel analysis.

1.
- Split up the classpath into more independent parts by first analyzing the namespace declarations of files and identify independent groups
- The more independent groups we have, the more cores we can utilize.

2. We don't have to keep all the analysis in memory, just writing it to disk after analyzing one group is already sufficient and will get the same results, provided that we have the configs in place (which is taken care of by `--copy-configs`. This will also release memory pressure. Probably more contention on the cache directory but could be worth it.

## Experiment: Option 2 - Incremental Cache Writing

**Status: Did not work out**

### Goal
Reduce memory pressure during parallel analysis by writing namespace var definitions to cache immediately after analysis, clearing them from memory.

### Implementation
We implemented incremental caching:
1. After analyzing each group, write `:vars` to cache immediately
2. Clear `:vars` from memory
3. When `namespaces->indexed` runs, load from cache if vars missing
4. Existing `update-defs` already skips namespaces with `:source :disk`

### Results

**Incremental caching alone (same group sizes):**
| Metric | Master | Incremental Cache |
|--------|--------|-------------------|
| Time | 18.1s | 18.5s |
| Memory | ~5.5 GB | ~5.0 GB |

Minimal improvement - the memory pressure comes from threads processing simultaneously, not from holding results after analysis.

**Incremental caching + smaller groups (for better parallelism):**
| Metric | Master | Smaller Groups |
|--------|--------|----------------|
| Time | 18.1s | 15.4s |
| CPU | 378% | 1064% |
| Memory | ~5.5 GB | ~16.3 GB |

Smaller groups improved parallelism (CPU 378% → 1064%) and speed (18s → 15s), but memory usage tripled.

### Memory Instrumentation

Added JVM memory tracking during parallel analysis to understand peak usage:

```
Max memory during analysis: 9804 MB   <- Peak during concurrent processing
Memory after analysis: 4903 MB        <- Before GC
Memory after GC: 1366 MB              <- Final working set
```

This confirms that:
- Peak memory (~10 GB) happens *during* concurrent analysis
- Half of that (~5 GB) is garbage collectible immediately after
- Final working set (~1.4 GB) is reasonable

The incremental cache writing happens *after* each group completes, but by then the peak has already occurred from multiple threads simultaneously parsing/analyzing files.

### Conclusion
The incremental caching approach doesn't help because:
1. Memory pressure comes from multiple threads processing simultaneously, not from accumulated results
2. Peak memory occurs during analysis, before we can write to cache and clear
3. Splitting into smaller groups improves parallelism but increases peak memory (more concurrent work)
4. The tradeoff (faster but 3x memory) is not acceptable

### Alternative approaches to consider
1. **Option 1 from original issue**: Analyze namespace declarations first to identify independent groups that can be processed in separate passes
2. **Limit concurrent threads**: Trade parallelism for memory
3. **Process dependencies and sources in separate phases**: Analyze all deps first (write to cache, discard), then analyze sources
4. **Streaming/lazy file processing**: Don't slurp all file contents into groups upfront
