# Table of contents
-  [`clj-kondo.core`](#clj-kondo.core) 
    -  [`config-hash`](#clj-kondo.core/config-hash) - Return the hash of the provided clj-kondo config.
    -  [`merge-configs`](#clj-kondo.core/merge-configs) - Returns the merged configuration of c1 with c2.
    -  [`print!`](#clj-kondo.core/print!) - Prints the result from <code>run!</code> to <code>*out*</code>.
    -  [`resolve-config`](#clj-kondo.core/resolve-config) - Returns the configuration for <code>cfg-dir</code> merged with home, clj-kondo default configs and optional <code>config</code> if provided.
    -  [`run!`](#clj-kondo.core/run!) - Takes a map with: - <code>:lint</code>: a seqable of files, directories and/or classpaths to lint.

-----
# <a name="clj-kondo.core">clj-kondo.core</a>






## <a name="clj-kondo.core/config-hash">`config-hash`</a> [:page_facing_up:](https://github.com/clj-kondo/clj-kondo/blob/master/src/clj_kondo/core.clj#L260-L265)
<a name="clj-kondo.core/config-hash"></a>
``` clojure

(config-hash config)
```


Return the hash of the provided clj-kondo config.

## <a name="clj-kondo.core/merge-configs">`merge-configs`</a> [:page_facing_up:](https://github.com/clj-kondo/clj-kondo/blob/master/src/clj_kondo/core.clj#L247-L250)
<a name="clj-kondo.core/merge-configs"></a>
``` clojure

(merge-configs & configs)
```


Returns the merged configuration of c1 with c2.

## <a name="clj-kondo.core/print!">`print!`</a> [:page_facing_up:](https://github.com/clj-kondo/clj-kondo/blob/master/src/clj_kondo/core.clj#L19-L53)
<a name="clj-kondo.core/print!"></a>
``` clojure

(print! {:keys [:config :findings :summary :analysis]})
```


Prints the result from [`run!`](#clj-kondo.core/run!) to `*out*`. Returns `nil`. Alpha,
  subject to change.

## <a name="clj-kondo.core/resolve-config">`resolve-config`</a> [:page_facing_up:](https://github.com/clj-kondo/clj-kondo/blob/master/src/clj_kondo/core.clj#L252-L258)
<a name="clj-kondo.core/resolve-config"></a>
``` clojure

(resolve-config cfg-dir)
(resolve-config cfg-dir config)
```


Returns the configuration for `cfg-dir` merged with home,
  clj-kondo default configs and optional `config` if provided.

## <a name="clj-kondo.core/run!">`run!`</a> [:page_facing_up:](https://github.com/clj-kondo/clj-kondo/blob/master/src/clj_kondo/core.clj#L55-L245)
<a name="clj-kondo.core/run!"></a>
``` clojure

(run!
 {:keys
  [:lint
   :lang
   :filename
   :cache
   :cache-dir
   :config
   :config-dir
   :parallel
   :no-warnings
   :dependencies
   :copy-configs
   :custom-lint-fn
   :file-analyzed-fn
   :skip-lint
   :debug],
  :or {cache true}})
```


Takes a map with:

  - `:lint`: a seqable of files, directories and/or classpaths to lint.

  - `:lang`: optional, defaults to `:clj`. Sets language for linting
  `*in*`. Supported values: `:clj`, `:cljs` and `:cljc`.

  - `:filename`: optional. In case stdin is used for linting, use this
  to set the reported filename.

  - `:cache-dir`: when this option is provided, the cache will be
  resolved to this directory. If `:cache` is `false` this option will
  be ignored.

  - `:cache`: if `false`, won't use cache. Otherwise, will try to resolve cache
  using `:cache-dir`. If `:cache-dir` is not set, cache is resolved using the
  nearest `.clj-kondo` directory in the current and parent directories.

  - `:config`: optional. A seqable of maps, a map or string
  representing the config as EDN, or a config file.

  In places where a file-like value is expected, either a path as string or a
  `java.io.File` may be passed, except for a classpath which must always be a string.

  - `:parallel`: optional. A boolean indicating if sources should be linted in parallel.`

  - `:copy-configs`: optional. A boolean indicating if scanned hooks should be copied to clj-kondo config dir.`

  - `:skip-lint`: optional. A boolean indicating if linting should be
  skipped. Other tasks like copying configs will still be done if `:copy-configs` is true.`

  - `:debug`: optional. Print debug info.

  Returns a map with `:findings`, a seqable of finding maps, a
  `:summary` of the findings and the `:config` that was used to
  produce those findings. This map can be passed to [`print!`](#clj-kondo.core/print!) to print
  to `*out*`. Alpha, subject to change.
  
