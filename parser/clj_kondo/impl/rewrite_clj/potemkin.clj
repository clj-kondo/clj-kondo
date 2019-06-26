(ns ^{:no-doc true} clj-kondo.impl.rewrite-clj.potemkin)

;; --- copied from ztellman/potemkin
;;
;; Copyright (c) 2013 Zachary Tellman
;;
;; Permission is hereby granted, free of charge, to any person obtaining a copy
;; of this software and associated documentation files  (the  "Software"), to
;; deal in the Software without restriction, including without limitation the
;; rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
;; sell copies of the Software, and to permit persons to whom the Software is
;; furnished to do so, subject to the following conditions:
;;
;; The above copyright notice and this permission notice shall be included in
;; all copies or substantial portions of the Software.
;;
;; THE SOFTWARE IS PROVIDED  "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
;; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
;; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
;; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
;; FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
;; IN THE SOFTWARE.
;;
;; ---

;; --- potemkin.namespaces

(defn link-vars
  "Makes sure that all changes to `src` are reflected in `dst`."
  [src dst]
  (add-watch src dst
    (fn [_ src old new]
      (alter-var-root dst (constantly @src))
      (alter-meta! dst merge (dissoc (meta src) :name)))))

(defmacro import-fn
  "Given a function in another namespace, defines a function with the
   same name in the current namespace.  Argument lists, doc-strings,
   and original line-numbers are preserved."
  ([sym]
     `(import-fn ~sym nil))
  ([sym name]
     (let [vr (resolve sym)
           m (meta vr)
           n (or name (:name m))
           arglists (:arglists m)
           protocol (:protocol m)]
       (when-not vr
         (throw (IllegalArgumentException. (str "Don't recognize " sym))))
       (when (:macro m)
         (throw (IllegalArgumentException.
                  (str "Calling import-fn on a macro: " sym))))

       `(do
          (def ~(with-meta n {:protocol protocol}) (deref ~vr))
          (alter-meta! (var ~n) merge (dissoc (meta ~vr) :name))
          (link-vars ~vr (var ~n))
          ~vr))))

(defmacro import-macro
  "Given a macro in another namespace, defines a macro with the same
   name in the current namespace.  Argument lists, doc-strings, and
   original line-numbers are preserved."
  ([sym]
     `(import-macro ~sym nil))
  ([sym name]
     (let [vr (resolve sym)
           m (meta vr)
           n (or name (with-meta (:name m) {}))
           arglists (:arglists m)]
       (when-not vr
         (throw (IllegalArgumentException. (str "Don't recognize " sym))))
       (when-not (:macro m)
         (throw (IllegalArgumentException.
                  (str "Calling import-macro on a non-macro: " sym))))
       `(do
          (def ~n ~(resolve sym))
          (alter-meta! (var ~n) merge (dissoc (meta ~vr) :name))
          (.setMacro (var ~n))
          (link-vars ~vr (var ~n))
          ~vr))))

(defmacro import-def
  "Given a regular def'd var from another namespace, defined a new var with the
   same name in the current namespace."
  ([sym]
     `(import-def ~sym nil))
  ([sym name]
     (let [vr (resolve sym)
           m (meta vr)
           n (or name (:name m))
           n (with-meta n (if (:dynamic m) {:dynamic true} {}))
           nspace (:ns m)]
       (when-not vr
         (throw (IllegalArgumentException. (str "Don't recognize " sym))))
       `(do
          (def ~n @~vr)
          (alter-meta! (var ~n) merge (dissoc (meta ~vr) :name))
          (link-vars ~vr (var ~n))
          ~vr))))

(defmacro import-vars
  "Imports a list of vars from other namespaces."
  [& syms]
  (let [unravel (fn unravel [x]
                  (if (sequential? x)
                    (->> x
                         rest
                         (mapcat unravel)
                         (map
                          #(symbol
                            (str (first x)
                                 (when-let [n (namespace %)]
                                   (str "." n)))
                            (name %))))
                    [x]))
        syms (mapcat unravel syms)]
    `(do
       ~@(map
          (fn [sym]
            (let [vr (resolve sym)
                  m (meta vr)]
              (cond
               (:macro m) `(import-macro ~sym)
               (:arglists m) `(import-fn ~sym)
               :else `(import-def ~sym))))
          syms))))

;; --- potemkin.types

(defmacro defprotocol+
  "A simpler version of 'potemkin.types/defprotocol+'."
  [name & body]
  (let [prev-body (-> name resolve meta :potemkin/body)]
    (when-not (= prev-body body)
      `(let [p# (defprotocol ~name ~@body)]
         (alter-meta! (resolve p#) assoc :potemkin/body '~body)
         p#))))
