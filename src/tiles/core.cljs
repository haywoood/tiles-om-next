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

(defn logo []
  (dom/div nil
           (dom/div #js {:style #js {:position   "absolute" :bottom 25 :left "48%" :fontSize 18 :letterSpacing 1.5
                             :fontFamily "georgia" :color "#444" :fontStyle "italic"}}
            "tile")
           (dom/div #js {:style #js {:position   "absolute" :bottom 10 :left "48.8%" :fontSize 18 :letterSpacing 1.5
                                     :fontFamily "georgia" :color "#444" :fontStyle "italic"}}
                    "~")))

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
                   :onMouseMove #(clickAction this)}
               (let [{:keys [width height backgroundColor top left borderRadius]} (:dot p)]
                 (dom/div #js {:style #js {:position "absolute" :width width
                                           :height height :top top :left left
                                           :borderRadius borderRadius
                                           :backgroundColor backgroundColor}}))))))

(def tile-component (om/factory Tile {:key-fn make-tile-ref-str}))

(defui Legend
  Object
  (handle-click [_ x]
    (om/transact! x `[(legend/select-tile {:tile-ref ~(om/get-ident x)}) :tiles/selected]))

  (render [this]
    (let [{:keys [tiles/legend]} (om/props this)]
      (apply dom/div #js {:style #js {:display "flex" :flexWrap "wrap" :width 180 :marginTop 35 :marginLeft 13}}
             (map #(tile-component (om/computed % {:clickAction (fn [x]
                                                                  (.handle-click this x))}))
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
  (handle-click [this index]
    (let [row-ref (om/get-ident this)]
      (om/transact! this `[(row/select-tile {:index ~index}) ~row-ref])))
  (render [this]
    (let [{:keys [tiles]} (om/props this)]
      (apply dom/div #js {:style #js {:display "flex"}}
        (mapv (fn [tile n] (tile-component (om/computed tile {:clickAction #(.handle-click this n)})))
              tiles (range))))))

(def tiles-row (om/factory TilesRow {:key-fn :id}))

(defui Grid
  static om/Ident
  (ident [this {:keys [id]}]
    [:grid/by-id id])
  static om/IQueryParams
  (params [this]
    {:tile-row (om/get-query TilesRow)})
  static om/IQuery
  (query [this]
    '[:id {:rows ?tile-row}])
  Object
  (render [this]
    (let [{:keys [rows]} (om/props this)]
      (apply dom/div #js {:style #js {:display "flex" :flexDirection "column"}}
        (mapv tiles-row rows)))))

(def grid-component (om/factory Grid {:key-fn :id}))

(defui TilesApp
  static om/IQueryParams
  (params [_]
    {:tile (om/get-query Tile)
     :grid (om/get-query Grid)})
  static om/IQuery
  (query [_]
    '[{:tiles/legend ?tile}
      {:tiles/grids ?grid}])

  Object
  (render [this]
    (let [{:keys [tiles/legend tiles/grids]} (om/props this)]
      (println (pp/pprint grids))
      (dom/div #js {:style #js {:display "flex" :flexDirection "column" :alignItems "center" :marginTop 25}}
               (logo)
               (apply dom/div #js {:style #js {:display "flex"}}
                 (map grid-component grids))
               (legend-component {:tiles/legend legend})))))

(defn make-tile [{:keys [background-color color]}]
  {:width 19
   :height 30
   :backgroundColor background-color
   :dot {:top 18 :left 8
         :width 3 :borderRadius 2
         :height 3 :backgroundColor color}})

(def colors [{:background-color "#444"    :color "white"}
             {:background-color "blue"    :color "white"}
             {:background-color "cyan"    :color "blue"}
             {:background-color "red"     :color "white"}
             {:background-color "pink"    :color "white"}
             {:background-color "yellow"  :color "red"}
             {:background-color "#64c7cc" :color "cyan"}
             {:background-color "#00a64d" :color "#75f0c3"}
             {:background-color "#f5008b" :color "#ffdbbf"}
             {:background-color "#0469bd" :color "#75d2fa"}
             {:background-color "#fcf000" :color "#d60000"}
             {:background-color "#010103" :color "#fa8e66"}
             {:background-color "#7a2c02" :color "#fff3e6"}
             {:background-color "white"   :color "red"}
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
  {:tiles/legend   legend
   :tiles/grids    [{:id 1 :rows (into [] (repeat 7 (blank-row)))}
                    {:id 2 :rows (into [] (repeat 7 (blank-row)))}
                    {:id 3 :rows (into [] (repeat 7 (blank-row)))}]
   :tiles/selected nil})

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [state query]} key]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)}))

(defmulti mutate om/dispatch)

(defmethod mutate 'legend/select-tile
  [{:keys [state]} _ {:keys [tile-ref]}]
  {:action (swap! state assoc :tiles/selected tile-ref)})

(defmethod mutate 'row/select-tile
  [{:keys [state ref]} _ {:keys [index]}]
  (let [st @state
        selected-tile (get st :tiles/selected)]
    {:action #(swap! state assoc-in (conj ref :tiles index) selected-tile)}))

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler (om/reconciler {:state initial-state
                                :parser parser}))

(om/add-root! reconciler TilesApp (gdom/getElement "app"))
