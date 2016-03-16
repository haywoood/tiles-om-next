(ns tiles.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [cljs.pprint :as ppp]
            [om.dom :as dom]))

(enable-console-print!)

(def pp ppp/pprint)
(declare legend)


;; -----------------------------------------------------------------
;; -----------------------------------------------------------------
;; State
;; -----------------------------------------------------------------
;; -----------------------------------------------------------------


(def initial-state
  {:tiles/index []
   :tiles/legend legend
   :boards []
   :current-board {}})


;; -----------------------------------------------------------------
;; -----------------------------------------------------------------
;; Components
;; -----------------------------------------------------------------
;; -----------------------------------------------------------------


(defui Tile
  static om/Ident
  (ident [_ props]
    (let [tile-color (:backgroundColor props)
          dot-color (get-in props [:dot :backgroundColor])
          ref-str (str tile-color "/" dot-color)]
      [:tile/by-colors ref-str]))
  static om/IQuery
  (query [this]
    [:width :height :backgroundColor])
  Object
  (render [this]))

(defui Legend
  static om/IQuery
  (query [this]
    {:tiles/legend (om/get-query Tile)})

  Object
  (render [this]))

(defui TilesApp
  static om/IQuery
  (query [this]
    [(om/get-query Legend)])

  Object
  (render [this]
    (dom/div nil "tiles")))


;; -----------------------------------------------------------------
;; -----------------------------------------------------------------
;; Reconciler
;; -----------------------------------------------------------------
;; -----------------------------------------------------------------


(defmulti read om/dispatch)

(defmethod read :default
  [env key params]
  {:value {}})

(defmulti mutate om/dispatch)

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler (om/reconciler {:state initial-state
                                :parser parser}))

(om/add-root! reconciler TilesApp (gdom/getElement "app"))


;; -----------------------------------------------------------------
;; -----------------------------------------------------------------
;; Helpers
;; -----------------------------------------------------------------
;; -----------------------------------------------------------------


(defn make-tile [{:keys [background-color color] :as params}]
  {:width 10
   :height 17
   :backgroundColor background-color
   :dot {:top 12
         :left 4
         :width 2
         :height 2
         :backgroundColor color}})

(def colors [
  {:background-color "#444" :color "white"}
  {:background-color "blue" :color "white"}
  {:background-color "cyan" :color "blue"}
  {:background-color "red" :color "white"}
  {:background-color "pink" :color "white"}
  {:background-color "yellow" :color "red"}
  {:background-color "#64c7cc" :color "cyan"}
  {:background-color "#00a64d" :color "#75f0c3"}
  {:background-color "#f5008b" :color "#ffdbbf"}
  {:background-color "#0469bd" :color "#75d2fa"}
  {:background-color "#fcf000" :color "#d60000"}
  {:background-color "#010103" :color "#fa8e66"}
  {:background-color "#7a2c02" :color "#fff3e6"}
  {:background-color "white" :color "red"}
  {:background-color "#f5989c" :color "#963e03"}
  {:background-color "#ed1c23" :color "#fff780"}
  {:background-color "#f7f7f7" :color "#009e4c"}
  {:background-color "#e04696" :color "#9c2c4b"}])

(def legend (mapv make-tile colors))