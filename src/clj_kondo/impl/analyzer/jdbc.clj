(ns clj-kondo.impl.analyzer.jdbc
  {:no-doc true}
  (:require
   [clj-kondo.impl.analyzer.common :refer [analyze-expression**
                                           analyze-like-let]]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :as utils :refer [node->line]]))

(defn analyze-like-jdbc-with
  "clojure.java.jdbc/with-db-* and next.jdbc/with-transaction are almost
  like a let binding: they accept a binding vector which has a single symbol,
  an expression, and an optional third expression (the options to apply).

  We check there are 2 or 3 forms in the first argument (the binding vector).
  We analyze the 3rd form if it is present (the options).
  We analyze the whole expression as a let, after modifying the binding
  expression to only have two children."
  [{:keys [filename callstack] :as ctx} expr]
  (let [call (-> callstack first second)
        [f bindings & body] (:children expr)
        binding-forms (:children bindings)
        [sym db-expr opts] binding-forms]
    (when-not (<= 2 (count binding-forms) 3)
      (findings/reg-finding!
       ctx
       (node->line filename bindings :error :syntax
                   (format "%s binding form requires exactly 2 or 3 forms" call))))
    (when-not (utils/symbol-token? sym)
      (findings/reg-finding!
       ctx
       (node->line filename sym :error :syntax
                   (format "%s binding form requires a symbol" call))))
    (let [opts-analyzed (analyze-expression** ctx opts)
          ;; a normal binding vector with just these two forms:
          simple-binding (assoc bindings :children (list sym db-expr))
          ;; reconstruct the expression with the normal binding instead:
          expr-with-simple-binding
          (assoc expr :children (concat [f simple-binding] body))
          ;; now we can treat it like a regular let expression:
          analyzed (analyze-like-let ctx expr-with-simple-binding)]
      (concat opts-analyzed (doall analyzed)))))
