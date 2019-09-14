(ns foo
  (:require [reagent.core :as r]
            ["apsl-react-native-button" :as NativeButton]))

(def button (r/adapt-react-class NativeButton))
