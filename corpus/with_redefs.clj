(ns with-redefs
  (:require [environ.core :refer [env]]
            [foo :refer [some-constant]]
            [utils.kafka :refer [send-message]]
            [utils :refer [fn-1 fn-2 fn-3 fn-4]]))

(defn ^:private mock-fn
  [& args]
  args)

(defn with-updated-system
  [f]
  (let [updated-env (assoc env :flag true)]
    (with-redefs [fn-1 (constantly identity)
                  fn-2 (constantly identity)
                  fn-3 (constantly identity)
                  fn-4 (constantly identity)
                  send-message mock-fn
                  env updated-env
                  some-constant 100]
      (f))))
