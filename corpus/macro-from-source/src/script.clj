(ns script
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [some.stub :as-alias stub]))

(defmacro my-let
  "No aliases. Body uses bindings."
  {:clj-kondo/macroexpand-hook true}
  [bnds & body]
  `(let [~@bnds] ~@body))

(defmacro shout
  "Alias inside syntax-quote -> :as-alias."
  {:clj-kondo/macroexpand-hook true}
  [s]
  `(str/upper-case ~s))

(defmacro joined
  "Alias used at expand time outside any quote -> :as."
  {:clj-kondo/macroexpand-hook true}
  [parts]
  (str/join "-" parts))

(defmacro tagged
  "Auto-resolved aliased keyword + alias declared :as-alias in source -> :as-alias."
  {:clj-kondo/macroexpand-hook true}
  [v]
  `{::stub/k ~v})

(defmacro mixed
  "Same alias used both at expand-time and inside syntax-quote -> :as wins."
  {:clj-kondo/macroexpand-hook true}
  [s]
  (let [up (str/upper-case s)]
    `(str/lower-case ~up)))

(defmacro literal
  "Alias appears only inside a regular quote: pure data, no require needed."
  {:clj-kondo/macroexpand-hook true}
  [_]
  '(str/never-called))

(defmacro setty
  "Alias from a regularly :as-required ns -> :as."
  {:clj-kondo/macroexpand-hook true}
  [a b]
  (set/union a b))

(defn ^{:clj-kondo/macroexpand-hook true} double-it
  "Helper defn marked for extraction. Called by `defdouble` at expand time."
  [n]
  (* 2 n))

(defmacro defdouble
  "Marker macro that calls a same-ns helper at expand time."
  {:clj-kondo/macroexpand-hook true}
  [sym n]
  `(def ~sym ~(double-it n)))

(def ^{:clj-kondo/macroexpand-hook true} k-default 42)

(defmacro defk
  "Marker macro that uses a same-ns marked def constant at expand time."
  {:clj-kondo/macroexpand-hook true}
  [sym]
  `(def ~sym ~k-default))

(my-let [x 1] (inc x))
