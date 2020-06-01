(defn expand-catch [[_catch catchee & exprs :as expr]]
  (cond (vector? catchee)
        (let [[selector & exprs] exprs]
           ;; (debug \"meta\" (map (juxt identity (comp meta first)) exprs))
            `(catch Exception _e#
                        (let [~selector nil]
                          ~@exprs)))
        :else expr))

(fn try+ [{:keys [sexpr]}]
  ;; (prn \"expr\" sexpr)
  (let [body (rest sexpr)
        [body catches]
        (loop [body body
               body-exprs []
               catches []]
          (if (seq body)
            (let [f (first body)]
              (if (and (seq? f) (= 'catch (first f)))
                (recur (rest body)
                       body-exprs
                       (conj catches (expand-catch f)))
                (recur (rest body)
                       (conj body-exprs f)
                       catches)))
            [body-exprs catches]))]
    {:sexpr
      `(let [~'throw+ (fn [])
           ~'&throw-context nil]
       ~(with-meta `(try ~@body ~@catches)
          (meta sexpr)))}))
