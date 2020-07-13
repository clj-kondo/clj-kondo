(ns example
  (:require [rum.core :as rum]))

(rum/defc label [text] ;; text is recognized as a binding
  [:div {:class "label"} text])

(rum/defc label ;; redefined var
  [text] ;; unused binding
  [:div {:class "label"} text']) ;; unresolved binding text'

(rum/defcs time-label ;; defcs is linted as defc
  < { :will-mount (fn [x] ;; mixin is parsed correctly
                    (assoc x ::time (js/Date.))) }
  [state x] ;; unused
  [:div y ;; unresolved
   ": " (str (::time state))])

