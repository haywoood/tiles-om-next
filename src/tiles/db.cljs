(ns tiles.db
  (:require [cljs-uuid-utils.core :as uuid]))

(def colors [{:background-color "#444"    :color "white"}
             {:background-color "blue"    :color "white"}
             {:background-color "cyan"    :color "blue"}
             {:background-color "red"     :color "w`hite"}
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

(def default-tile {:color "red" :background-color "white"})

(defn make-tile [acc opts]
  (let [id (uuid/uuid-string (uuid/make-random-uuid))
        tile (merge opts {:id id})]
    (assoc acc id tile)))

(defn gen-board [x y]
  (reduce
   (fn [acc _] (make-tile acc default-tile))
   {}
   (take (* x y) (range))))

(def default-board (gen-board 23 17))
