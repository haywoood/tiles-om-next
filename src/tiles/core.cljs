(ns tiles.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
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
     {:dot [:width :height :backgroundColor]}])
  Object
  (render [this]
    (let [p (om/props this)
          clickAction (om/get-computed this :clickAction)]
      (dom/div #js{:style #js{:width (:width p) :height (:height p)
                              :backgroundColor (:backgroundColor p)}
                   :onClick #(om/transact! this `[(~clickAction) :tiles/selected])}))))

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
    [:id {:tile (om/get-query Tile)}])
  Object
  (render [this]
    (let [{:keys [id tile]} (om/props this)]
      (tile-component tile))))

(def tiles-row (om/factory TilesRow {:key-fn :id}))

(defui TilesApp
  static om/IQueryParams
  (params [_]
    {:tile (om/get-query Tile)})
  static om/IQuery
  (query [this]
    `[{:tiles/selected ~(om/get-query Tile)}
      {:tiles/grid [{:tiles/row-one ~(om/get-query TilesRow)}]}
      {:tiles/legend ~(om/get-query Tile)}])

  Object
  (render [this]
    (let [{:keys [tiles/legend tiles/selected tiles/grid]} (om/props this)]
      (dom/div nil
        (tile-component selected)
        (legend-component {:tiles/legend legend})
        (mapv tiles-row (:tiles/row-one grid))))))

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

(def blank-tile (make-tile {:color "red" :background-color "white"}))

(def initial-state
  {:tiles/legend   (conj legend blank-tile)
   :tiles/grid     {:tiles/row-one [{:id 0 :tile blank-tile}
                                    {:id 1 :tile blank-tile}
                                    {:id 2 :tile blank-tile}
                                    {:id 3 :tile blank-tile}
                                    {:id 4 :tile blank-tile}
                                    {:id 5 :tile blank-tile}]}
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

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler (om/reconciler {:state initial-state
                                :parser parser}))

(om/add-root! reconciler TilesApp (gdom/getElement "app"))