(ns clj-kondo.impl.hiccup
  (:require
   [clj-kondo.impl.analyzer.common :as common]
   [clj-kondo.impl.findings :as findings]
   [clj-kondo.impl.utils :as utils]))

(def element-names
  #{:a :abbr :address :area :article :aside :audio
    :b :base :bdi :bdo :blockquote :body :br :button
    :canvas :caption :cite :code :col :colgroup :data :datalist :dd :del :details :dfn :dialog :div :dl :dt
    :em :embed
    :fieldset :figcaption :figure :footer :form
    :h1 :h2 :h3 :h4 :h5 :h6 :head :header :hr :html
    :i :iframe :img :input :ins
    :kbd
    :label :legend :li :link
    :main :map :mark :menu :meta :meter
    :nav :noscript
    :object :ol :optgroup :option :output
    :p :param :picture :pre :progress
    :q
    :rp :rt :ruby
    :s :samp :script :section :select :slot :small :source :span :strong :style :sub :summary :sup
    :table :tbody :td :template :textarea :tfoot :th :thead :time :title :tr :track
    :u :ul
    :var :video
    :wbr
    :<>})

(defn hiccup? [children]
  (when-let [k (:k (first children))]
    (contains? element-names k)))

;; TODO:

[:div (map (fn [{:keys [x]}]
             [:ul x])
           [{:x 1}])]

(defn lint-hiccup [ctx children]
  (let [ctx (assoc ctx :hiccup true)]
    (loop [children (seq children)
           i 0]
      (when children
        (let [fst (first children)]
          (when (and (= :map (utils/tag fst))
                     (not= 1 i))
            (findings/reg-finding! ctx (merge {:type :hiccup
                                               :message "Hiccup attribute map in wrong location"
                                               :filename (:filename ctx)}
                                              (meta fst)))))
        (recur (next children) (inc i))))
    (common/analyze-children (update ctx
                                     :callstack #(cons [nil :vector] %))
                             children)))
