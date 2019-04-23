(ns clj-kondo.impl.schema
  (:require
   [rewrite-clj.zip :as z]
   [rewrite-clj.custom-zipper.core :as z*]
   [rewrite-clj.custom-zipper.utils :as zu]
   [rewrite-clj.zip.move :as m]
   [clj-kondo.impl.utils :refer [lift-meta parse-string]]))

(defn remove-schemas-from-seq [zloc recurse?]
  (if-let [inside-seq (z/down zloc)]
    (loop [zloc inside-seq]
      (let [v (z/sexpr zloc)
            zloc (if (= ':- v)
                   (-> zloc zu/remove-and-move-right zu/remove-and-move-left)
                   zloc)
            zloc (if (and recurse? (z/vector? zloc))
                   (remove-schemas-from-seq zloc recurse?)
                   zloc)
            right (z/right zloc)]
        (if (z/end? right) (z/up zloc)
            (recur right))))
    zloc))

(defn expand-schema-defn
  "Strips away schema type annotations so the expression can then be linted as a normal function"
  [expr]
  (try
    (z/root
     (loop [z (z/down (remove-schemas-from-seq (z/edn* expr) false))]
       (cond (z/vector? z)
             (remove-schemas-from-seq z true)
             (z/list? z)
             (let [stripped (-> z z/down (remove-schemas-from-seq true) z/up)
                   new-z (z/right stripped)]
               (if (z/end? new-z) stripped
                   (recur new-z)))
             :else (recur (z/right z)))))
    (catch Throwable e
      (println "could not parse" expr)
      (throw e))))

;;;; Scratch

(comment
  (expand-schema-defn (parse-string "(s/defn foo :- (s/maybe s/Int) [a :- Int])"))
  (expand-schema-defn (parse-string "(s/defn foo [[foo :- Baz]])"))
  (expand-schema-defn (parse-string "(s/defn foo [{:keys [a]} :- {:a s/Int}])"))
  )
