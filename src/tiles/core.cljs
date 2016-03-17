(ns tiles.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(declare legend)


;; -----------------------------------------------------------------
;; -----------------------------------------------------------------
;; State
;; -----------------------------------------------------------------
;; -----------------------------------------------------------------


(def initial-state
  {:tiles/index []
   :tiles/legend legend
   :selected-tile nil
   :boards []
   :current-board {}})


;; -----------------------------------------------------------------
;; -----------------------------------------------------------------
;; Components
;; -----------------------------------------------------------------
;; -----------------------------------------------------------------


(defn make-tile-ref-str [props]
  (let [tile-color (:backgroundColor props)
        dot-color (get-in props [:dot :backgroundColor])]
    (str tile-color "/" dot-color)))

(defui Tile
  static om/Ident
  (ident [_ props]
    [:tile/by-colors (make-tile-ref-str props)])
  static om/IQuery
  (query [this]
    [:width :height :backgroundColor
     {:dot [:width :height :backgroundColor]}])
  Object
  (render [this]
    (let [p (om/props this)
          clickAction (om/get-computed this :clickAction)]
      (dom/div #js{:style #js{:width (:width p) :height (:height p)
                              :backgroundColor (:backgroundColor p)}
                   :onClick #(om/transact! this `[(~clickAction) :selected-tile])}))))

(def tile-component (om/factory Tile))

(defui Legend
  static om/IQuery
  (query [this]
    {:tiles/legend (om/get-query Tile)})

  Object
  (render [this]
    (let [{:keys [tiles/legend]} (om/props this)]
      (apply dom/div #js {:style #js {:display "flex" :flexWrap "wrap"
                                      :width 75}}
             (map #(tile-component (om/computed % {:clickAction 'select-legend-tile}))
                  legend)))))

(def legend-component (om/factory Legend))

(defui TilesApp
  static om/IQuery
  (query [this]
    [{:selected-tile (om/get-query Tile)}
     (om/get-query Legend)])

  Object
  (render [this]
    (let [{:keys [tiles/legend selected-tile]} (om/props this)]
      (dom/div nil
        (tile-component selected-tile)
        (legend-component {:tiles/legend legend})))))


;; -----------------------------------------------------------------
;; -----------------------------------------------------------------
;; Reconciler
;; -----------------------------------------------------------------
;; -----------------------------------------------------------------


(defmulti read om/dispatch)

(defmethod read :selected-tile
  [{:keys [state query]} key params]
  (let [st @state]
    (if-let [val (get st key)]
      {:value (get-in st val)}
      {:value nil})))

(defmethod read :default
  [{:keys [state query]} key params]
  (let [st @state]
    (if (get st key)
      {:value (om/db->tree query (get st key) st)}
      {:value nil})))

(defmulti mutate om/dispatch)

(defmethod mutate 'select-legend-tile
  [{:keys [state ref]} key params]
  {:action #(swap! state assoc :selected-tile ref)})

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler (om/reconciler {:state initial-state
                                :parser parser}))


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

;; REPL helpers
(comment
  ; inspect the state when normalized
  (def normalized-state (atom (om/tree->db TilesApp initial-state true)))

  ; query the state
  (parser {:state normalized-state} [:boards {:tiles/legend [:width]}])
)

(om/add-root! reconciler TilesApp (gdom/getElement "app"))
