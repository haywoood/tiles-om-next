(ns tiles.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [cljs.pprint :as pp]
            [om.dom :as dom]))

(enable-console-print!)

(declare reconciler)
(declare blank-grid)

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
          clickAction (om/get-computed this :clickAction)
          hoverAction (om/get-computed this :hoverAction)]
      (dom/div #js{:style       #js{:position        "relative" :width (:width p)
                                    :height          (:height p)
                                    :backgroundColor (:backgroundColor p)}
                   :onMouseOver #(hoverAction this)
                   :onMouseDown #(do
                                   (.preventDefault %)
                                   (swap! (om/app-state reconciler) assoc :grid/dragging true)
                                   (clickAction this))}
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
    (om/transact! this `[(row/select-tile {:index ~index})]))
  (handle-hover [this index]
    (om/transact! this `[(row/hover-tile {:index ~index})]))
  (render [this]
    (let [{:keys [tiles]} (om/props this)]
      (apply dom/div #js {:style #js {:display "flex"}}
        (mapv (fn [tile n] (tile-component (om/computed tile {:clickAction #(.handle-click this n)
                                                              :hoverAction #(.handle-hover this n)})))
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
      (apply dom/div #js {:style #js {:display "flex"
                                      :flexDirection "column"}}
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
      (dom/div #js {:style #js {:display "flex" :flexDirection "column" :alignItems "center" :marginTop 25}
                    :onMouseUp #(swap! (om/app-state reconciler) assoc :grid/dragging false)}
               (logo)
               (dom/div #js {:onClick #(swap! (om/app-state reconciler) assoc :tiles/grids (mapv blank-grid (range 4)))}
                        "")
               (apply dom/div #js {:style #js {:display "flex" :flexWrap "wrap" :width 342}}
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

(defn blank-grid []
  {:id (.-uuid (.-id (om/tempid)))
   :rows (mapv blank-row (range 7))})

(def initial-state
  {:tiles/legend   legend
   :tiles/grids    (mapv blank-grid (range 4))
   :tiles/selected (->> legend last make-tile-ref-str (conj [:tile/by-colors]))
   :grid/dragging  false})

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [state query]} key]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)}))

(defmulti mutate om/dispatch)

(defmethod mutate 'legend/select-tile
  [{:keys [state]} _ {:keys [tile-ref]}]
  {:action #(swap! state assoc :tiles/selected tile-ref)})

(defn select-tile [state ref index selected-tile]
  (fn []
    (swap! state assoc-in (conj ref :tiles index) selected-tile)))

(defmethod mutate 'row/select-tile
  [{:keys [state ref]} _ {:keys [index]}]
  (let [st @state
        selected-tile (get st :tiles/selected)]
    {:action (select-tile state ref index selected-tile)}))

(defmethod mutate 'row/hover-tile
  [{:keys [state ref]} _ {:keys [index]}]
  (let [st @state
        selected-tile (get st :tiles/selected)]
    (if (get st :grid/dragging)
      {:action (select-tile state ref index selected-tile)})))

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler (om/reconciler {:state initial-state
                                :parser parser}))

(om/add-root! reconciler TilesApp (gdom/getElement "app"))

;; saved drawings until local storage is hooked up
(comment
  (def drawing {:tiles/legend [[:tile/by-colors "#444/white"] [:tile/by-colors "blue/white"] [:tile/by-colors "cyan/blue"] [:tile/by-colors "red/white"] [:tile/by-colors "pink/white"] [:tile/by-colors "yellow/red"] [:tile/by-colors "#64c7cc/cyan"] [:tile/by-colors "#00a64d/#75f0c3"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#0469bd/#75d2fa"] [:tile/by-colors "#fcf000/#d60000"] [:tile/by-colors "#010103/#fa8e66"] [:tile/by-colors "#7a2c02/#fff3e6"] [:tile/by-colors "white/red"] [:tile/by-colors "#f5989c/#963e03"] [:tile/by-colors "#ed1c23/#fff780"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#e04696/#9c2c4b"]], :tiles/grids [[:grid/by-id "914a80fd-fae7-4ba3-990a-4ddf15da29bf"] [:grid/by-id "2c169d81-dc1d-4642-ad9a-e398198cdbfa"] [:grid/by-id "9e1fd896-2ec6-4abd-9525-70f285680ada"] [:grid/by-id "4071f876-6023-4e35-b70a-46af6420f28d"]], :tiles/selected [:tile/by-colors "#f7f7f7/#009e4c"], :grid/dragging false, :tile/by-colors {"pink/white" {:width 19, :height 30, :backgroundColor "pink", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "white"}}, "yellow/red" {:width 19, :height 30, :backgroundColor "yellow", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "red"}}, "#e04696/#9c2c4b" {:width 19, :height 30, :backgroundColor "#e04696", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "#9c2c4b"}}, "#ed1c23/#fff780" {:width 19, :height 30, :backgroundColor "#ed1c23", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "#fff780"}}, "#010103/#fa8e66" {:width 19, :height 30, :backgroundColor "#010103", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "#fa8e66"}}, "#f5008b/#ffdbbf" {:width 19, :height 30, :backgroundColor "#f5008b", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "#ffdbbf"}}, "#0469bd/#75d2fa" {:width 19, :height 30, :backgroundColor "#0469bd", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "#75d2fa"}}, "red/white" {:width 19, :height 30, :backgroundColor "red", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "white"}}, "#f7f7f7/#009e4c" {:width 19, :height 30, :backgroundColor "#f7f7f7", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "#009e4c"}}, "#fcf000/#d60000" {:width 19, :height 30, :backgroundColor "#fcf000", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "#d60000"}}, "white/red" {:width 19, :height 30, :backgroundColor "white", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "red"}}, "cyan/blue" {:width 19, :height 30, :backgroundColor "cyan", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "blue"}}, "#7a2c02/#fff3e6" {:width 19, :height 30, :backgroundColor "#7a2c02", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "#fff3e6"}}, "#64c7cc/cyan" {:width 19, :height 30, :backgroundColor "#64c7cc", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "cyan"}}, "blue/white" {:width 19, :height 30, :backgroundColor "blue", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "white"}}, "#00a64d/#75f0c3" {:width 19, :height 30, :backgroundColor "#00a64d", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "#75f0c3"}}, "#444/white" {:width 19, :height 30, :backgroundColor "#444", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "white"}}, "#f5989c/#963e03" {:width 19, :height 30, :backgroundColor "#f5989c", :dot {:top 18, :left 8, :width 3, :borderRadius 2, :height 3, :backgroundColor "#963e03"}}}, :row/by-id {"cd4698d3-01a0-4bcb-8a14-11f833575081" {:id "cd4698d3-01a0-4bcb-8a14-11f833575081", :tiles [[:tile/by-colors "red/white"] [:tile/by-colors "red/white"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "cyan/blue"] [:tile/by-colors "cyan/blue"] [:tile/by-colors "cyan/blue"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"]]}, "266a4cd6-948f-4274-ae10-5b60daa82c9e" {:id "266a4cd6-948f-4274-ae10-5b60daa82c9e", :tiles [[:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#ed1c23/#fff780"] [:tile/by-colors "#ed1c23/#fff780"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#64c7cc/cyan"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "1feb1ca6-00ef-45ac-8bec-54bfe3ec9daa" {:id "1feb1ca6-00ef-45ac-8bec-54bfe3ec9daa", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "ad983476-4d6f-4d72-a59d-d27b6dec751e" {:id "ad983476-4d6f-4d72-a59d-d27b6dec751e", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#010103/#fa8e66"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "29306b0e-b6d6-49f9-bff8-44f9e2c43abd" {:id "29306b0e-b6d6-49f9-bff8-44f9e2c43abd", :tiles [[:tile/by-colors "blue/white"] [:tile/by-colors "blue/white"] [:tile/by-colors "#e04696/#9c2c4b"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "e16b2866-d7ea-462b-b09c-4a5af74d13cc" {:id "e16b2866-d7ea-462b-b09c-4a5af74d13cc", :tiles [[:tile/by-colors "red/white"] [:tile/by-colors "red/white"] [:tile/by-colors "red/white"] [:tile/by-colors "red/white"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "red/white"]]}, "faaccea8-080b-4073-bbf2-97aec2998039" {:id "faaccea8-080b-4073-bbf2-97aec2998039", :tiles [[:tile/by-colors "yellow/red"] [:tile/by-colors "yellow/red"] [:tile/by-colors "yellow/red"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "4b4efbd2-a492-4b9c-949f-e6a22869bc85" {:id "4b4efbd2-a492-4b9c-949f-e6a22869bc85", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "2b530bff-169f-46a8-af26-96039aa9c238" {:id "2b530bff-169f-46a8-af26-96039aa9c238", :tiles [[:tile/by-colors "#0469bd/#75d2fa"] [:tile/by-colors "#0469bd/#75d2fa"] [:tile/by-colors "#0469bd/#75d2fa"] [:tile/by-colors "#0469bd/#75d2fa"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "c4b6a351-a144-41fb-9d5d-415311f4441f" {:id "c4b6a351-a144-41fb-9d5d-415311f4441f", :tiles [[:tile/by-colors "#0469bd/#75d2fa"] [:tile/by-colors "#0469bd/#75d2fa"] [:tile/by-colors "#0469bd/#75d2fa"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "fc97e4fd-f1bc-4349-84d0-f3c928ec2ca5" {:id "fc97e4fd-f1bc-4349-84d0-f3c928ec2ca5", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#ed1c23/#fff780"] [:tile/by-colors "#ed1c23/#fff780"]]}, "273f5136-6956-4d75-aaaa-c30a22dd94a1" {:id "273f5136-6956-4d75-aaaa-c30a22dd94a1", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#0469bd/#75d2fa"]]}, "ca5cdc51-929e-4582-b71d-581fb42e8c6f" {:id "ca5cdc51-929e-4582-b71d-581fb42e8c6f", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#0469bd/#75d2fa"] [:tile/by-colors "#0469bd/#75d2fa"]]}, "963ee20c-7180-4046-b059-a6aeb1a0d3d1" {:id "963ee20c-7180-4046-b059-a6aeb1a0d3d1", :tiles [[:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#64c7cc/cyan"] [:tile/by-colors "#64c7cc/cyan"] [:tile/by-colors "#64c7cc/cyan"] [:tile/by-colors "#64c7cc/cyan"] [:tile/by-colors "#64c7cc/cyan"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "yellow/red"]]}, "fae9ae5c-433c-45ad-b6a5-7a863f3e2c7d" {:id "fae9ae5c-433c-45ad-b6a5-7a863f3e2c7d", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#e04696/#9c2c4b"]]}, "96116265-2605-4e61-a716-577df8315a6f" {:id "96116265-2605-4e61-a716-577df8315a6f", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#010103/#fa8e66"] [:tile/by-colors "#010103/#fa8e66"] [:tile/by-colors "#010103/#fa8e66"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "04517338-e52c-42df-a4c5-c87251f03566" {:id "04517338-e52c-42df-a4c5-c87251f03566", :tiles [[:tile/by-colors "#010103/#fa8e66"] [:tile/by-colors "#010103/#fa8e66"] [:tile/by-colors "#010103/#fa8e66"] [:tile/by-colors "#f5989c/#963e03"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "6f571779-8eeb-43e9-ac13-be08e1ece425" {:id "6f571779-8eeb-43e9-ac13-be08e1ece425", :tiles [[:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "cyan/blue"] [:tile/by-colors "cyan/blue"] [:tile/by-colors "#f5008b/#ffdbbf"]]}, "c94e3ea7-4e1e-4219-b91b-fd7790774691" {:id "c94e3ea7-4e1e-4219-b91b-fd7790774691", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "blue/white"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "7da815e9-c489-4126-85c9-d65cc0f8eeb8" {:id "7da815e9-c489-4126-85c9-d65cc0f8eeb8", :tiles [[:tile/by-colors "blue/white"] [:tile/by-colors "blue/white"] [:tile/by-colors "blue/white"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "73627206-1323-4bd6-a183-cf51798f9a85" {:id "73627206-1323-4bd6-a183-cf51798f9a85", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "c5676598-8841-4277-94d4-cba86ad1fc89" {:id "c5676598-8841-4277-94d4-cba86ad1fc89", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#444/white"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#444/white"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#ed1c23/#fff780"] [:tile/by-colors "#f5008b/#ffdbbf"]]}, "aeddafbc-b480-447b-9af2-89f27edadbc8" {:id "aeddafbc-b480-447b-9af2-89f27edadbc8", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f5989c/#963e03"] [:tile/by-colors "#010103/#fa8e66"]]}, "a4930b01-4dd5-4600-8004-160239dbf40a" {:id "a4930b01-4dd5-4600-8004-160239dbf40a", :tiles [[:tile/by-colors "#0469bd/#75d2fa"] [:tile/by-colors "#0469bd/#75d2fa"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "cc3ca4e2-e179-4efe-966f-4283e63ccd0c" {:id "cc3ca4e2-e179-4efe-966f-4283e63ccd0c", :tiles [[:tile/by-colors "red/white"] [:tile/by-colors "red/white"] [:tile/by-colors "red/white"] [:tile/by-colors "red/white"] [:tile/by-colors "red/white"] [:tile/by-colors "red/white"] [:tile/by-colors "red/white"] [:tile/by-colors "red/white"] [:tile/by-colors "#f5008b/#ffdbbf"]]}, "f00ab90b-6e0c-4093-8ad4-46d42daf0d87" {:id "f00ab90b-6e0c-4093-8ad4-46d42daf0d87", :tiles [[:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#ed1c23/#fff780"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"]]}, "68a7e42b-434d-441d-b03f-5ebae65ca095" {:id "68a7e42b-434d-441d-b03f-5ebae65ca095", :tiles [[:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f7f7f7/#009e4c"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "yellow/red"]]}, "e055c7a2-d493-4612-9162-e4e3ab343e77" {:id "e055c7a2-d493-4612-9162-e4e3ab343e77", :tiles [[:tile/by-colors "yellow/red"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#f5008b/#ffdbbf"] [:tile/by-colors "#64c7cc/cyan"] [:tile/by-colors "#64c7cc/cyan"] [:tile/by-colors "#64c7cc/cyan"] [:tile/by-colors "#64c7cc/cyan"] [:tile/by-colors "#64c7cc/cyan"] [:tile/by-colors "#f5008b/#ffdbbf"]]}}, :grid/by-id {"914a80fd-fae7-4ba3-990a-4ddf15da29bf" {:id "914a80fd-fae7-4ba3-990a-4ddf15da29bf", :rows [[:row/by-id "ad983476-4d6f-4d72-a59d-d27b6dec751e"] [:row/by-id "96116265-2605-4e61-a716-577df8315a6f"] [:row/by-id "fc97e4fd-f1bc-4349-84d0-f3c928ec2ca5"] [:row/by-id "c5676598-8841-4277-94d4-cba86ad1fc89"] [:row/by-id "fae9ae5c-433c-45ad-b6a5-7a863f3e2c7d"] [:row/by-id "68a7e42b-434d-441d-b03f-5ebae65ca095"] [:row/by-id "aeddafbc-b480-447b-9af2-89f27edadbc8"]]}, "2c169d81-dc1d-4642-ad9a-e398198cdbfa" {:id "2c169d81-dc1d-4642-ad9a-e398198cdbfa", :rows [[:row/by-id "1feb1ca6-00ef-45ac-8bec-54bfe3ec9daa"] [:row/by-id "4b4efbd2-a492-4b9c-949f-e6a22869bc85"] [:row/by-id "266a4cd6-948f-4274-ae10-5b60daa82c9e"] [:row/by-id "f00ab90b-6e0c-4093-8ad4-46d42daf0d87"] [:row/by-id "29306b0e-b6d6-49f9-bff8-44f9e2c43abd"] [:row/by-id "faaccea8-080b-4073-bbf2-97aec2998039"] [:row/by-id "04517338-e52c-42df-a4c5-c87251f03566"]]}, "9e1fd896-2ec6-4abd-9525-70f285680ada" {:id "9e1fd896-2ec6-4abd-9525-70f285680ada", :rows [[:row/by-id "ca5cdc51-929e-4582-b71d-581fb42e8c6f"] [:row/by-id "273f5136-6956-4d75-aaaa-c30a22dd94a1"] [:row/by-id "c94e3ea7-4e1e-4219-b91b-fd7790774691"] [:row/by-id "7da815e9-c489-4126-85c9-d65cc0f8eeb8"] [:row/by-id "e16b2866-d7ea-462b-b09c-4a5af74d13cc"] [:row/by-id "cd4698d3-01a0-4bcb-8a14-11f833575081"] [:row/by-id "e055c7a2-d493-4612-9162-e4e3ab343e77"]]}, "4071f876-6023-4e35-b70a-46af6420f28d" {:id "4071f876-6023-4e35-b70a-46af6420f28d", :rows [[:row/by-id "2b530bff-169f-46a8-af26-96039aa9c238"] [:row/by-id "c4b6a351-a144-41fb-9d5d-415311f4441f"] [:row/by-id "a4930b01-4dd5-4600-8004-160239dbf40a"] [:row/by-id "73627206-1323-4bd6-a183-cf51798f9a85"] [:row/by-id "cc3ca4e2-e179-4efe-966f-4283e63ccd0c"] [:row/by-id "6f571779-8eeb-43e9-ac13-be08e1ece425"] [:row/by-id "963ee20c-7180-4046-b059-a6aeb1a0d3d1"]]}}})
  (reset! (om/app-state reconciler) drawing)
)