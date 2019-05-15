(ns clj-kondo.impl.schema
  {:no-doc true}
  (:require
   #_[clj-kondo.impl.profiler :as profiler]
   #_[rewrite-clj.custom-zipper.utils :as zu]
   #_[rewrite-clj.zip :as z]
   [rewrite-clj.node.protocols :as node]))

#_(defn rightmost? [zloc]
  (nil? (z/right zloc)))

#_(defn remove-schemas-from-seq [zloc recurse?]
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


#_(defn expand-schema-defn
  "Strips away schema type annotations so the expression can then be linted as a normal function"
  [expr]
  (profiler/profile
   :expand-schema-defn
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
                    (recur (z/right z)))))))))

(defn remove-schemas-from-children [expr]
  (let [children (:children expr)
        {:keys [:new-children :schemas]}
        (loop [[fst-child & rest-children] children
               res {:new-children []
                    :schemas []}]
          (let [sexpr (when fst-child (node/sexpr fst-child))]
            (cond (not fst-child)
                  res
                  (= ':- (node/sexpr fst-child))
                  (recur (next rest-children)
                         (update res :schemas conj (first rest-children)))
                  (vector? sexpr)
                  (recur rest-children
                         (let [{:keys [:expr :schemas]} (remove-schemas-from-children fst-child)]
                           (-> res
                               (update :schemas into schemas)
                               (update :new-children conj expr))))
                  :else (recur rest-children
                               (update res :new-children conj fst-child)))))]
    {:expr (assoc expr :children new-children)
     :schemas schemas}))

(defn expand-schema-defn2
  [expr]
  (let [children (:children expr)
        {:keys [:new-children
                :schemas]}
        (loop [[fst-child & rest-children] children
               res {:new-children []
                    :schemas []}]
          (let [sexpr (when fst-child (node/sexpr fst-child))]
            (cond (not fst-child)
                  res
                  (= ':- sexpr)
                  (recur (next rest-children)
                         (update res :schemas conj (first rest-children)))
                  (vector? sexpr)
                  (let [{:keys [:expr :schemas]} (remove-schemas-from-children fst-child)]
                    (-> res
                        (update :schemas into schemas)
                        (update :new-children conj expr)
                        (update :new-children into rest-children)))
                  (list? sexpr)
                  (recur rest-children
                         (let [cchildren (:children fst-child)
                               {:keys [:expr :schemas]} (remove-schemas-from-children (first cchildren))
                               new-cchildren (cons expr (rest cchildren))
                               new-fst-child (assoc fst-child :children new-cchildren)]
                           (-> res
                               (update :schemas into schemas)
                               (update :new-children conj new-fst-child))))
                  :else (recur rest-children
                               (update res :new-children conj fst-child)))))]
    {:defn (assoc expr :children new-children)
     :schemas schemas}))

;;;; Scratch

(comment
  (require '[clj-kondo.impl.utils :refer [parse-string]])
  (expand-schema-defn (parse-string "(s/defn foo :- (s/maybe s/Int) [a :- Int])"))
  (expand-schema-defn2 (parse-string "(s/defn foo :- (s/maybe s/Int) [a :- Int])"))
  (expand-schema-defn (parse-string "(s/defn foo [[foo :- Baz]] foo)"))
  (expand-schema-defn2 (parse-string "(s/defn foo [[foo :- Baz]] foo)"))
  (expand-schema-defn (parse-string "(s/defn foo [{:keys [a]} :- {:a s/Int}])"))
  (expand-schema-defn2 (parse-string "(s/defn foo [{:keys [a]} :- {:a s/Int}])"))
  (expand-schema-defn2 (parse-string "(s/defn foo ([a :- s/Int]) ([b :- s/String]))"))

  )
