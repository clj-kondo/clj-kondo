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

;; TODO, create test:
[:div (map (fn [{:keys [x]}]
             [:ul x])
           [{:x 1}])]

;; TODO: create test
[:div {:x [{}]}]

;; TODO: create test
(concat [:body] [1 2 3])

;; TODO: create test
(defn Animate [])
[:div [:> Animate {}]]

;; TODO: create test
[:div
 [Animate "dude"
  {:class "w-full"}]]

;; TODO: false positive:
[:map [:success-handler {:optional true} [:=> [:cat :any :double :string] :any]
       :err-handler {:optional true} [:=> [:cat [:fn ::whatever]] :any]
       :pool-size {:optional true} number?
       :max-batch-messages {:optional true} number?
       :max-next-ms {:optional true} number?]]

(defn lint-hiccup [ctx children]
  (let [ctx (assoc ctx :hiccup true :arg-types (atom []))
        ctx (update ctx
                    :callstack #(cons [nil :vector] %))
        hiccup-tag (:k (first children))
        expected-attr-idx (if (= :> hiccup-tag) 2 1)]
    (loop [children (seq children)
           i 0]
      (when children
        (let [fst (first children)
              map-node? (= :map (utils/tag fst))]
          (when (and map-node?
                     hiccup-tag
                     (not= expected-attr-idx i))
            (findings/reg-finding! ctx (merge {:type :hiccup
                                               :message "Hiccup attribute map in wrong location"
                                               :filename (:filename ctx)}
                                              (meta fst))))
          (common/analyze-expression** (assoc ctx :hiccup (not map-node?)) fst))
        (recur (next children) (inc i))))))
