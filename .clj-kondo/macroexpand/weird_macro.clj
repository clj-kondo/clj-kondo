(fn weird-macro [{:keys [:sexpr]}]
  (let [[[sym val opts] & body] (rest sexpr)]
    (when-not (and sym val)
      (throw (ex-info "No sym and val provided" {})))
    {:sexpr `(let [~sym ~val] ~@(cons opts body))}))
