(ns skip-args.comment
  (:refer-clojure :exclude [comment])
  (:require [cljs.core :as core]))

(core/comment
  (select-keys))


