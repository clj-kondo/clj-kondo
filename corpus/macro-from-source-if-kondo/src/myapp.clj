(ns myapp)

(declare -not-in-kondo)

(defmacro if-kondo
  "Picks `kondo-form` at expand time when running inside clj-kondo's SCI
  (where `-not-in-kondo` is unresolved), and `runtime-form` otherwise."
  {:clj-kondo/macroexpand-hook true}
  [kondo-form runtime-form]
  (if-not (resolve '-not-in-kondo)
    kondo-form
    runtime-form))

(defmacro embed-config
  "Inlines a config map at compile time. SCI takes the kondo branch and
  returns a stub map so the call sites still type-check; at runtime the
  slurp branch would read the real file."
  {:clj-kondo/macroexpand-hook true}
  []
  (if-kondo
    {:host "localhost" :port 8080}
    ;; The runtime branch deliberately refers to a symbol SCI cannot
    ;; resolve. If `if-kondo` were broken and SCI took this branch,
    ;; the macroexpand would error out and clj-kondo would emit a
    ;; :hook :error finding. Clean lint proves the kondo branch fires.
    (sci-cannot-resolve-this)))

(:host (embed-config))
(:port (embed-config))
