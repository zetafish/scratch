(ns upfront.format)

(def ^:private fmt-0
  (js/Intl.NumberFormat. "nl-NL" #js {:minimumFractionDigits 0 :maximumFractionDigits 0}))

(def ^:private fmt-2
  (js/Intl.NumberFormat. "nl-NL" #js {:minimumFractionDigits 2 :maximumFractionDigits 2}))

(defn fmt [n]
  (str "€" (.format fmt-0 n)))

(defn fmt-dec [n]
  (str "€" (.format fmt-2 n)))

(defn fmt-nl [n]
  (.toLocaleString n "nl-NL"))
