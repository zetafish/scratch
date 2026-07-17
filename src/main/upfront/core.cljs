(ns upfront.core
  (:require [reagent.dom :as rdom]
            [upfront.views :as views]))

(defn mount-root []
  (rdom/render [views/app] (.getElementById js/document "app")))

(defn ^:export init []
  (mount-root))
