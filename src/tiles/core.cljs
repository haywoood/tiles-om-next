(ns tiles.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [cljs.pprint :as pp]
            [om.dom :as dom]))

(enable-console-print!)

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
     {:dot [:top :left :width :height :backgroundColor :borderRadius]}])
  Object
  (render [this]
    (let [p (om/props this)
          clickAction (om/get-computed this :clickAction)]
      (dom/div #js{:style #js{:position "relative" :width (:width p)
                              :height (:height p)
                              :backgroundColor (:backgroundColor p)}
                   :onClick #(om/transact! this `[(~clickAction) :tiles/selected])}
               (let [{:keys [width height backgroundColor top left borderRadius]} (:dot p)]
                 (dom/div #js {:style #js {:position "absolute" :width width
                                           :height height :top top :left left
                                           :borderRadius borderRadius
                                           :backgroundColor backgroundColor}}))))))

(def tile-component (om/factory Tile {:key-fn make-tile-ref-str}))

(defui Legend
  Object
  (render [this]
    (let [{:keys [tiles/legend]} (om/props this)]
      (apply dom/div #js {:style #js {:display "flex" :flexWrap "wrap"
                                      :width 75}}
             (map #(tile-component (om/computed % {:clickAction 'legend/select-tile}))
                  legend)))))

(def legend-component (om/factory Legend))

(defui TilesRow
  static om/Ident
  (ident [this {:keys [id]}]
    [:row/by-id id])
  static om/IQuery
  (query [this]
    [:id {:tiles (om/get-query Tile)}])
  Object
  (render [this]
    (let [{:keys [id tiles]} (om/props this)]
      (apply dom/div #js {:style #js {:display "flex"}}
        (mapv #(tile-component (om/computed % {:clickAction 'row/select-tile})) tiles)))))

(def tiles-row (om/factory TilesRow {:key-fn :id}))

(defui TilesApp
  static om/IQueryParams
  (params [_]
    {:tile (om/get-query Tile)})
  static om/IQuery
  (query [this]
    `[{:tiles/selected ~(om/get-query Tile)}
      {:tiles/grid [{:tiles/row-one ~(om/get-query TilesRow)}
                    {:tiles/row-two ~(om/get-query TilesRow)}
                    {:tiles/row-three ~(om/get-query TilesRow)}
                    {:tiles/row-four ~(om/get-query TilesRow)}
                    {:tiles/row-five ~(om/get-query TilesRow)}
                    {:tiles/row-six ~(om/get-query TilesRow)}
                    {:tiles/row-seven ~(om/get-query TilesRow)}]}
      {:tiles/legend ~(om/get-query Tile)}])

  Object
  (render [this]
    (let [{:keys [tiles/legend tiles/selected tiles/grid]} (om/props this)]
      (dom/div nil
        (tile-component selected)
        (legend-component {:tiles/legend legend})
        (dom/div #js {:style #js {:display "flex" :flexDirection "column"}}
          (tiles-row (:tiles/row-one grid))
          (tiles-row (:tiles/row-two grid))
          (tiles-row (:tiles/row-three grid))
          (tiles-row (:tiles/row-four grid))
          (tiles-row (:tiles/row-five grid))
          (tiles-row (:tiles/row-six grid))
          (tiles-row (:tiles/row-seven grid)))))))

(defn make-tile [{:keys [background-color color]}]
  {:width 10
   :height 17
   :backgroundColor background-color
   :dot {:top 12 :left 4
         :width 2 :borderRadius 1
         :height 2 :backgroundColor color}})

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

(def blank-tile (make-tile {:color "red" :background-color "white"}))

(defn blank-row []
  {:id (.-uuid (.-id (om/tempid)))
   :tiles (into [] (repeat 9 blank-tile))})

(def initial-state
  {:tiles/legend (conj legend blank-tile)
   :tiles/grid {:tiles/row-one   (blank-row)
                :tiles/row-two   (blank-row)
                :tiles/row-three (blank-row)
                :tiles/row-four  (blank-row)
                :tiles/row-five  (blank-row)
                :tiles/row-six   (blank-row)
                :tiles/row-seven (blank-row)}
   :tiles/selected nil})

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [state query]} key]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)}))

(defmulti mutate om/dispatch)

(defmethod mutate 'legend/select-tile
  [{:keys [state ref]} _ _]
  {:action (swap! state assoc :tiles/selected ref)})

(defmethod mutate 'row/select-tile
  [{:keys [state ref]} _ _]
  {:action #()})

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler (om/reconciler {:state initial-state
                                :parser parser}))

(om/add-root! reconciler TilesApp (gdom/getElement "app"))