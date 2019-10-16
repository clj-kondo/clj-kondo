(ns foo
  (:require [reagent.core :as r]
            ["apsl-react-native-button" :as NativeButton]
            ["constructor-export" :as ConstructorExport]))

(def button (r/adapt-react-class NativeButton))
(def obj (ConstructorExport.))
