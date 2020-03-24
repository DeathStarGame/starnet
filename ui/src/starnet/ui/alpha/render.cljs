(ns starnet.ui.alpha.render
  (:require
   [clojure.repl :refer [doc]]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [clojure.core.async :as a :refer [<! >!  timeout chan alt! go
                                     alts!  take! put! mult tap untap
                                     pub sub sliding-buffer mix admix unmix]]
   [goog.string :as gstring]
   [goog.string.format]

   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as sgen]
   [clojure.spec.test.alpha :as stest]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]

   ["antd/lib/layout" :default AntLayout]
   ["antd/lib/menu" :default AntMenu]
   ["antd/lib/icon" :default AntIcon]
   ["antd/lib/button" :default AntButton]
   ["antd/lib/list" :default AntList]
   ["antd/lib/row" :default AntRow]
   ["antd/lib/col" :default AntCol]
   ["antd/lib/form" :default AntForm]
   ["antd/lib/input" :default AntInput]
   ["react" :as React]
   ["antd/lib/checkbox" :default AntCheckbox]


   ["antd/lib/divider" :default AntDivider]
   ["@ant-design/icons/SmileOutlined" :default AntSmileOutlined]))

(js/console.log )

(def ant-row (r/adapt-react-class AntRow))
(def ant-col (r/adapt-react-class AntCol))
(def ant-divider (r/adapt-react-class AntDivider))

(def ant-layout (r/adapt-react-class AntLayout))
(def ant-layout-content (r/adapt-react-class (.-Content AntLayout)))
(def ant-layout-header (r/adapt-react-class (.-Header AntLayout)))
(def ant-smile-outlined (r/adapt-react-class AntSmileOutlined))
(def ant-menu (r/adapt-react-class AntMenu))
(def ant-menu-item (r/adapt-react-class (.-Item AntMenu)))
(def ant-icon (r/adapt-react-class AntIcon))
(def ant-button (r/adapt-react-class AntButton))
(def ant-list (r/adapt-react-class AntList))
(def ant-input (r/adapt-react-class AntInput))
(def ant-input-password (r/adapt-react-class (.-Password AntInput)))
(def ant-checkbox (r/adapt-react-class AntCheckbox))
(def ant-form (r/adapt-react-class AntForm))
(def ant-form-item (r/adapt-react-class (.-Item AntForm)))

(defn menu
  [channels ratoms]
  (let [handler* (r/cursor (ratoms :state) [:router/handler])
        url* (r/cursor (ratoms :state) [:history/pushed :url])] ; for test, remove
    (fn [_ _]
      (let [handler @handler*
            url @url*]
        [ant-menu {:theme "light"
                   :mode "horizontal"
                   :size "small"
                   :style {:lineHeight "32px"}
                   :default-selected-keys ["home-panel"]
                   :selected-keys [handler]
                   :on-select (fn [x] (do))}
         [ant-menu-item {:key :page/events}
          [:a {:href "/events"} "events"]]
         [ant-menu-item {:key :page/games}
          [:a {:href "/games"} "games"]]
         [ant-menu-item {:key :page/user-games}
          [:a {:href "/u/games"} "u/games"]]
         [ant-menu-item {:key :page/settings}
          [:a {:href "/settings"} "settings"]]
         [ant-menu-item {:key :page/userid}
          [:a {:href (gstring/format "/u/%s" (gen/generate gen/string-alphanumeric))} "user/random"]]
         [ant-menu-item {:key :page/non-existing}
          [:a {:href (gstring/format "/non-existing" (gen/generate gen/string-alphanumeric))} "non-existing"]]
         [ant-menu-item {:key :page/sign-in}
          [:a {:href "/sign-in"} "sign-in"]]
         [ant-menu-item {:key :page/sign-up}
          [:a {:href "/sign-up"} "sign-up"]]
         
         ]))))

(defn rc-user-identity
  [channels ratoms]
  (let [user-data* (r/cursor (ratoms :state) [:ops/state :op/get-profile :http/response :body])]
    (fn [_ _]
      (let [{:keys [u/username u/fullname]} @user-data*]
        [:div {:style {:position "absolute" :top 2 :right 10}} username]))))

(defn layout
  [channels ratoms content]
  [ant-layout {:style {:min-height "100vh"}}
   [ant-layout-header
    {:style {:position "fixed"
             :z-index 1
             :lineHeight "32px"
             :height "32px"
             :padding 0
             :background "#000" #_"#001529"
             :width "100%"}}
    [:a {:href "/"
         :class "logo"}
     #_[:img {:class "logo-img" :src "./img/logo-4.png"}]
     [:div {:class "logo-name"} "starnet"]]
    [menu channels ratoms]
    [rc-user-identity channels ratoms]
    ]
   [ant-layout-content {:class "main-content"
                        :style {:margin-top "32px"
                                :padding "32px 32px 32px 32px"}}
    content]])

(defn layout-game
  [channels ratoms content]
  [ant-layout {:style {:min-height "100vh"}}
   [ant-layout-content {:class "main-content"
                        :style {:margin-top "32px"
                                :padding "32px 32px 32px 32px"}}
    content]])

(defn rc-form-signin
  [channels ratoms]
  (let [{:keys [ch-inputs]} channels
        form-ref (.createRef React)
        on-submit (fn []
                    (let [vs (.. form-ref -current getFieldsValue)]
                      (put! ch-inputs {:ch/topic :inputs/ops
                                       :ops/op :op/login
                                       :u/password (aget vs "password")
                                       :u/username (aget vs "username")})))]
    (fn [_ _]
      (let []
        [ant-form {:labelCol {:span 8} :wrapperCol {:span 16} :ref form-ref}
         [ant-form-item {:label nil
                         :name "username"
                         :wrapperCol {:offset 1 :span 16}
                         :rules [{:required true :message "username"}]}
          [ant-input {:placeholder "username"}]]
         [ant-form-item {:label nil
                         :name "password"
                         :wrapperCol {:offset 1 :span 16}
                         :rules [{:required true :message "password"}]}
          [ant-input-password {:placeholder "password"}]]
         [ant-form-item {:wrapperCol {:offset 1 :span 16}}
          [ant-button {:type "primary" :on-click on-submit} "sign in"]]]))))



(defn rc-page-events
  [channels ratoms]
  (let []
    (fn [_ _]
      (let []
        [layout channels ratoms
         [:<>
          [:div "rc-page-events"]]]))))

(defn rc-page-settings
  [channels ratoms]
  (let []
    (fn [_ _]
      (let []
        [layout channels ratoms
         [:<>
          [:div "rc-page-settings"]]]))))

(defn rc-page-games
  [channels ratoms]
  (let []
    (fn [_ _]
      (let []
        [layout channels ratoms
         [:<>
          [:div "rc-page-games"]]]))))

(defn rc-page-userid-games
  [channels ratoms]
  (let []
    (fn [_ _]
      (let []
        [layout channels ratoms
         [:<>
          [:div "rc-page-userid-games"]]]))))

(defn rc-page-userid
  [channels ratoms]
  (let []
    (fn [_ _]
      (let []
        [layout channels ratoms
         [:<>
          [:div "rc-page-userid"]]]))))

(defn rc-page-not-found
  [channels ratoms]
  (let []
    (fn [_ _]
      (let []
        [layout channels ratoms
         [:<>
          [:div "rc-page-not-found"]]]))))

(defn rc-page-sign-in
  [channels ratoms]
  (let []
    (fn [_ _]
      (let []
        [layout channels ratoms
         [:<>
          #_[ant-row {:gutter [16 24]}
             [ant-col "rc-page-sign-in"]]
          #_[ant-divider {:orientation "left"} "rc-page-sign-in"]
          [ant-row {:justify "center"
                    :align "middle"
                    :style {:height "85%"}
                    ;; :gutter [16 24]
                    }
           [ant-col {:span 12}
            [rc-form-signin channels ratoms]]]]]))))

(defn rc-page-sign-up
  [channels ratoms]
  (let []
    (fn [_ _]
      (let []
        [layout channels ratoms
         [:<>
          [:div "rc-page-sign-up"]]]))))

(defn rc-page-game
  [channels ratoms]
  (let []
    (fn [_ _]
      (let []
        [layout-game channels ratoms
         [:<>
          [:div "rc-page-game"]]]))))

(defn rc-page-user-games
  [channels ratoms]
  (let []
    (fn [_ _]
      (let []
        [layout channels ratoms
         [:<>
          [:div  "rc-page-user-games"]
          [ant-button {:value "button" :size "small"} "button"]
          [:div
           [:a {:target "_blank"
                :href (gstring/format "/game/%s" (str (gen/generate gen/uuid)))}
            [ant-button {:size "small"} "create game"]]]]
         ]))))

(defn rc-ui
  [channels ratoms]
  (let [handler* (r/cursor (ratoms :state) [:router/handler])]
    (fn [_ _]
      #_(println (gstring/format "ratoms :state %s" @(ratoms :state)))
      #_(let [{:keys [router/handler history/pushed]} @(ratoms :state)]
          (println (gstring/format "rendering %s" handler)))
      (let [handler @handler*]
        (println (gstring/format "rendering %s" handler))
        (condp = handler
          :page/events [rc-page-events channels ratoms]
          :page/games [rc-page-games channels ratoms]
          :page/settings [rc-page-settings channels ratoms]
          :page/user-games [rc-page-user-games channels ratoms]
          :page/userid-games [rc-page-userid-games channels ratoms]
          :page/userid [rc-page-userid channels ratoms]
          :page/sign-in [rc-page-sign-in channels ratoms]
          :page/sign-up [rc-page-sign-up channels ratoms]
          :page/game [rc-page-game channels ratoms]
          [rc-page-not-found channels ratoms])))))

(defn render-ui
  [channels ratoms]
  (rdom/render [rc-ui channels ratoms]  (.getElementById js/document "ui")))