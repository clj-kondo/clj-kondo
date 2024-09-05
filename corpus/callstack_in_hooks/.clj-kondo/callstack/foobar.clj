(ns callstack.foobar
  (:require [clj-kondo.hooks-api :as h]))

(defn foobar [_]
  (let [cs (h/callstack)]
    (when (some #(and (= 'inc (:name %))
                      (= 'clojure.core (:ns %)))
                cs)
      (throw (ex-info "You shouldn't call foobar as an argument to inc!" {})))))
