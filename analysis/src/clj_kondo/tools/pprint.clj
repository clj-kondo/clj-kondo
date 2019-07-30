(ns clj-kondo.tools.pprint
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.pprint :as p]))

(defn- private-fixed-arity [_x _y _z])

(defmacro macro-var-args-arity [_x & _xs])

(defn -main [& paths]
  (let [analysis (:analysis (clj-kondo/run! {:lint paths :config {:output {:analysis true}}}))
        {:keys [:namespace-definitions
                :namespace-usages
                :var-definitions
                :var-usages]} analysis]
    (print ":namespace-definitions")
    (p/print-table [:filename :row :col :name]
                   namespace-definitions)
    (println)
    (print ":namespace-usages")
    (p/print-table [:filename :row :col :from :to]
                   namespace-usages)
    (println)
    (print ":var-definitions")
    (p/print-table [:filename :row :col :ns :name :fixed-arities :var-args-min-arity :private :macro]
                   var-definitions)
    (println)
    (print ":var-usages")
    (p/print-table var-usages)))
