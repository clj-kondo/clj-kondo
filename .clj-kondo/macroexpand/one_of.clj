;; (one-of x [foo bar]), foo bar are literal symbols
(fn [{:keys [:sexpr]}]
  (let [[matchee matches] (rest sexpr)
        sexpr `(case ~matchee
                 ~(apply list matches) ~matchee
                 nil)]
    {:sexpr sexpr}))
