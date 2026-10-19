(ns app
  (:require [macros]))

(macros/direct 1)
(macros/via-hook 1)
(macros/via-plain 1)
(macros/via-hook-do 1)
(macros/via-hook (h/process 1)) ;; warning: user-written h is unresolved
