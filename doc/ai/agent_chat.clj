(ns agent-chat
  (:import
   [java.util.concurrent LinkedBlockingQueue TimeUnit]))

(defonce ^LinkedBlockingQueue codex->claude
  (LinkedBlockingQueue.))

(defonce ^LinkedBlockingQueue claude->codex
  (LinkedBlockingQueue.))

(defn- send! [^LinkedBlockingQueue queue from body]
  (let [message {:id (random-uuid)
                 :from from
                 :body body}]
    (.put queue message)
    message))

(defn- wait! [^LinkedBlockingQueue queue]
  (.poll queue 10 TimeUnit/MINUTES))

(defn send-to-claude! [body]
  (send! codex->claude :codex body))

(defn send-to-codex! [body]
  (send! claude->codex :claude body))

(defn wait-for-claude! []
  (wait! claude->codex))

(defn wait-for-codex! []
  (wait! codex->claude))
