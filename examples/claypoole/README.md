# Claypoole

See the `.clj-kondo` directory for configuration for the [Claypoole](https://github.com/TheClimateCorporation/claypoole) library.

The example effectively remaps the following to their `clojure.core` counterparts:



| Original                                   | Remapped  |
|--------------------------------------------|-----------|
| `com.climate.claypoole/future`             | `future`  |
| `com.climate.claypoole/completable-future` | `future`  |
| `com.climate.claypoole/pdoseq`             | `doseq`   |
| `com.climate.claypoole/pmap`               | `map`     |
| `com.climate.claypoole/pvalues`            | `pvalues` |
| `com.climate.claypoole/upvalues`           | `pvalues` |
| `com.climate.claypoole/pfor`               | `for`     |
| `com.climate.claypoole/upfor`              | `for`     |

           