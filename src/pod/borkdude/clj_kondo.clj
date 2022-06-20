(ns pod.borkdude.clj-kondo
  {:no-doc true}
  (:refer-clojure :exclude [read read-string])
  (:require [bencode.core :as bencode]
            [clj-kondo.core :as clj-kondo]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [java.io PushbackInputStream])
  (:gen-class))

(set! *warn-on-reflection* true)

(def debug? false)
(defn debug [& args]
  (when debug?
    (binding [*out* (io/writer "/tmp/debug.log" :append true)]
      (apply println args))))

(def stdin (PushbackInputStream. System/in))

(defn write [v]
  (bencode/write-bencode System/out v)
  (.flush System/out))

(defn read-string [^"[B" v]
  (String. v))

(defn read []
  (bencode/read-bencode stdin))

(defn print* [& args]
  (with-out-str
    (apply clj-kondo/print! args)))

(def lookup
  {'pod.borkdude.clj-kondo/merge-configs clj-kondo/merge-configs
   'clj-kondo.core/merge-configs clj-kondo/merge-configs
   'pod.borkdude.clj-kondo/print* print*
   'clj-kondo.core/print* print*
   'pod.borkdude.clj-kondo/run! clj-kondo/run!
   'clj-kondo.core/run! clj-kondo/run!})

(defn pod-ns [name]
  {"name" name
   "vars" [{"name" "merge-configs"}
           {"name" "print*"}
           {"name" "print!"
            "code" "
(defn print! [run-output]
  (print (print* run-output))
  (flush))"}
           {"name" "run!"}]})

(defn run-pod []
  (loop []
    (let [message (try (read)
                       (catch java.io.EOFException _
                         ::EOF))]
      (when-not (identical? ::EOF message)
        (let [op (get message "op")
              op (read-string op)
              op (keyword op)
              id (some-> (get message "id")
                         read-string)
              id (or id "unknown")]
          (case op
            :describe (do (write {"format" "edn"
                                  "namespaces" [(pod-ns "pod.borkdude.clj-kondo")
                                                (pod-ns "clj-kondo.core")]
                                  "id" id})
                          (recur))
            :invoke (do (try
                          (let [var (-> (get message "var")
                                        read-string
                                        symbol)
                                args (get message "args")
                                args (read-string args)
                                args (edn/read-string args)]
                            (if-let [f (lookup var)]
                              (let [value (pr-str (apply f args))
                                    reply {"value" value
                                           "id" id
                                           "status" ["done"]}]
                                (write reply))
                              (throw (ex-info (str "Var not found: " var) {}))))
                          (catch Throwable e
                            (binding [*out* *err*]
                              (println e))
                            (let [reply {"ex-message" (.getMessage e)
                                         "ex-data" (pr-str
                                                    (assoc (ex-data e)
                                                           :type (class e)))
                                         "id" id
                                         "status" ["done" "error"]}]
                              (write reply))))
                        (recur))
            (do
              (write {"err" (str "unknown op:" (name op))})
              (recur))))))))
