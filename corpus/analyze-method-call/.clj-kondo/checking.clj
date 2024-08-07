(ns checking)

(defn checking
  [_]
  (throw (ex-info "Use deterministic version" {})))
