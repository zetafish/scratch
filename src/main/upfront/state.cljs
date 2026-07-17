(ns upfront.state
  (:require [reagent.core :as r]))

(def app-state
  (r/atom
   {:deelnemers          200
    :ticketprijs         180
    :crew-ratio          0.5
    :foodtruck-besteding 8
    :foodtruck-freq      4
    :foodtruck-standplaats 250
    :foodtruck-pct       20
    :shop-besteding      4
    :shop-freq           10
    :shop-marge-pct      40
    :sponsoring          0
    :yard-preset         :duv2025
    :veerboot-type       "veer_winter"
    :ehbo-uurtarief      30
    :ehbo-uren           16
    :extra-dagen         1
    :locatie-huur        3000
    :materiaal           2500
    :vergunning          500
    :verzekering         1500
    :shirt-kosten        8
    :startnummer         3
    :prijzengeld         5000
    :weeztix-fee         2.50
    :crew-org            10
    :crew-cash           450
    :crew-shoptegoed     60
    :crew-eten           30
    :crew-kleding        20}))

(defn update-val! [k v]
  (swap! app-state assoc k v))
