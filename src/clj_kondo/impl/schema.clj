(ns clj-kondo.impl.schema
  {:no-doc true}
  (:require
   [rewrite-clj.zip :as z]
   [rewrite-clj.custom-zipper.utils :as zu]))

(defn rightmost? [zloc]
  (nil? (z/right zloc)))

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
            last? (rightmost? zloc)]
        (if last? (z/up zloc)
            (recur (z/right zloc)))))
    zloc))


(defn expand-schema-defn
  "Strips away schema type annotations so the expression can then be linted as a normal function"
  [expr]
  (z/root
   (loop [z (z/down (z/edn* expr))]
     (let [last? (rightmost? z)]
       (cond
         (= ':- (z/sexpr z))
         (recur (-> z zu/remove-and-move-right zu/remove-and-move-left))
         (z/vector? z)
         (remove-schemas-from-seq z true)
         (z/list? z)
         (let [stripped (-> z z/down (remove-schemas-from-seq true) z/up)]
           (if last? stripped
               (recur (z/right stripped))))
         :else (if last? z
                   (recur (z/right z))))))))

;;;; Scratch

(comment
  (require '[clj-kondo.impl.utils :refer [lift-meta parse-string]])
  (expand-schema-defn (parse-string "(s/defn foo :- (s/maybe s/Int) [a :- Int])"))
  (expand-schema-defn (parse-string "(s/defn foo [[foo :- Baz]])"))
  (expand-schema-defn (parse-string "(s/defn foo [{:keys [a]} :- {:a s/Int}])"))
  )
