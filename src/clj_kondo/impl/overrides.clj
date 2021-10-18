(ns clj-kondo.impl.overrides
  {:no-doc true})

;; TODO: we can do this more intelligently. E.g. only override when we actually
;; have linted clojure.core and ont he way to the cache override it, so the
;; transit will contain the right info and we don't have to do this at runtime.

(defn overrides
  "Overrides var information. The way this is done looks a bit verbose,
  but it's faster than merging one giant map."
  [idacs]
  (-> idacs
      ;; clojure.core
      (assoc-in '[:clj :defs clojure.core def] '{:ns clojure.core
                                                 :name def
                                                 :macro true
                                                 :fixed-arities #{1 2 3}})
      (assoc-in '[:clj :defs clojure.core defn] '{:ns clojure.core
                                                  :name defn
                                                  :macro true
                                                  :varargs-min-arity 2})
      (assoc-in '[:clj :defs clojure.core defn-] '{:ns clojure.core
                                                   :name defn-
                                                   :macro true
                                                   :varargs-min-arity 2})
      (assoc-in '[:clj :defs clojure.core defmacro] '{:ns clojure.core
                                                      :name defmacro
                                                      :macro true
                                                      :varargs-min-arity 2})
      (assoc-in '[:clj :defs clojure.core quote] '{:ns clojure.core
                                                   :name quote
                                                   :macro true
                                                   :fixed-arities #{1}})
      (assoc-in '[:clj :defs clojure.core var] '{:ns clojure.core
                                                 :name var
                                                 :macro true
                                                 :fixed-arities #{1}})
      (assoc-in '[:clj :defs clojure.core set!] '{:ns clojure.core
                                                  :name set!
                                                  :macro true
                                                  :fixed-arities #{2}})
      (update-in '[:clj :defs clojure.core if-some] (fn [var]
                                                      (-> var
                                                          (dissoc :varargs-min-arity)
                                                          (assoc :fixed-arities #{2 3}))))
      (update-in '[:clj :defs clojure.core if-let] (fn [var]
                                                     (-> var
                                                         (dissoc :varargs-min-arity)
                                                         (assoc :fixed-arities #{2 3}))))
      ;; cljs.core
      (assoc-in '[:cljs :defs cljs.core array :varargs-min-arity] 0)
      (assoc-in '[:cljc :defs cljs.core :clj def] '{:ns cljs.core
                                                    :name def
                                                    :macro true
                                                    :fixed-arities #{1 2 3}})
      (assoc-in '[:cljc :defs cljs.core :cljs def] '{:ns cljs.core
                                                     :name def
                                                     :macro true
                                                     :fixed-arities #{1 2 3}})
      (assoc-in '[:cljc :defs cljs.core :clj defn ] '{:ns cljs.core
                                                      :name defn
                                                      :macro true
                                                      :varargs-min-arity 2})
      (assoc-in '[:cljc :defs cljs.core :cljs defn] '{:ns cljs.core
                                                      :name defn
                                                      :macro true
                                                      :varargs-min-arity 2})
      (assoc-in '[:cljc :defs cljs.core :clj defn-] '{:ns cljs.core
                                                      :name defn-
                                                      :macro true
                                                      :varargs-min-arity 2})
      (assoc-in '[:cljc :defs cljs.core :cljs defn-] '{:ns cljs.core
                                                       :name defn-
                                                       :macro true
                                                       :varargs-min-arity 2})
      (assoc-in '[:cljc :defs cljs.core :clj defmacro] '{:ns cljs.core
                                                         :name defmacro
                                                         :macro true
                                                         :varargs-min-arity 2})
      (assoc-in '[:cljc :defs cljs.core :cljs defmacro] '{:ns cljs.core
                                                          :name defmacro
                                                          :macro true
                                                          :varargs-min-arity 2})
      (assoc-in '[:cljc :defs cljs.core :clj quote] '{:ns cljs.core
                                                      :name quote
                                                      :macro true
                                                      :fixed-arities #{1}})
      (assoc-in '[:cljc :defs cljs.core :cljs quote] '{:ns cljs.core
                                                       :name quote
                                                       :macro true
                                                       :fixed-arities #{1}})
      (assoc-in '[:cljc :defs cljs.core :cljs var] '{:ns cljs.core
                                                     :name var
                                                     :macro true
                                                     :fixed-arities #{1}})
      (assoc-in '[:cljc :defs cljs.core :clj var] '{:ns cljs.core
                                                    :name var
                                                    :macro true
                                                    :fixed-arities #{1}})
      (assoc-in '[:cljc :defs cljs.core :clj set!] '{:ns cljs.core
                                                     :name set!
                                                     :macro true
                                                     :fixed-arities #{2}})
      (assoc-in '[:cljc :defs cljs.core :cljs set!] '{:ns cljs.core
                                                      :name set!
                                                      :macro true
                                                      :fixed-arities #{2 3}})
      (update-in '[:cljc :defs cljs.core :clj if-some] (fn [var]
                                                         (-> var
                                                             (dissoc :varargs-min-arity)
                                                             (assoc :fixed-arities #{2 3}))))
      (update-in '[:cljc :defs cljs.core :cljs if-some] (fn [var]
                                                          (-> var
                                                              (dissoc :varargs-min-arity)
                                                              (assoc :fixed-arities #{2 3}))))
      (update-in '[:cljc :defs cljs.core :clj if-let] (fn [var]
                                                        (-> var
                                                            (dissoc :varargs-min-arity)
                                                            (assoc :fixed-arities #{2 3}))))
      (update-in '[:cljc :defs cljs.core :cljs if-let] (fn [var]
                                                         (-> var
                                                             (dissoc :varargs-min-arity)
                                                             (assoc :fixed-arities #{2 3}))))))
