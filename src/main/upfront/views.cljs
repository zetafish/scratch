(ns upfront.views
  (:require [upfront.state :as state]
            [upfront.calc :as calc]
            [upfront.data :as data]
            [upfront.format :as f]))

;; --- Helpers ---

(defn- input-number [k label & {:keys [step min max]}]
  [:div.input-group
   [:label label]
   [:input {:type "number"
            :value (get @state/app-state k)
            :step (or step 1)
            :min min
            :max max
            :on-change #(state/update-val! k (js/parseFloat (.. % -target -value)))}]])

(defn- panel [title & children]
  (into [:div.panel [:h2 title]] children))

;; --- Input Panels ---

(defn deelnemers-panel []
  (let [s @state/app-state
        n (:deelnemers s)]
    [panel "Deelnemers & Tickets"
     [:div.scenario-btns.compact {:style {:margin-bottom ".5rem"}}
      [:button {:on-click #(state/update-val! :deelnemers 100)} "Klein (100)"]
      [:button {:on-click #(state/update-val! :deelnemers 200)} "Middel (200)"]
      [:button {:on-click #(state/update-val! :deelnemers 350)} "Groot (350)"]]
     [:div.input-group
      [:label "Aantal deelnemers: " [:span.slider-value n]]
      [:input {:type "range" :min 50 :max 500 :step 10 :value n
               :on-change #(state/update-val! :deelnemers (js/parseInt (.. % -target -value)))}]]
     [input-number :ticketprijs "Ticketprijs (incl. btw) €" :step 5]
     [input-number :crew-ratio "Support crew per deelnemer (max 2, eigen rekening)" :step 0.1 :min 0 :max 2]]))

(defn foodtruck-panel []
  (let [s @state/app-state
        r (calc/calc-for-n s (:deelnemers s))]
    [panel "Foodtruck (extern) - Runners & Support"
     [input-number :foodtruck-besteding "Gem. besteding per aankoop €"]
     [input-number :foodtruck-freq "1x kopen per X uur" :min 1]
     [input-number :foodtruck-standplaats "Standplaatsvergoeding €" :step 100]
     [input-number :foodtruck-pct "% omzet afdracht aan org" :min 0 :max 50]
     [:div.staffel-info
      (f/fmt-nl (:persoon-uren r)) " persoon-uren ÷ " (:foodtruck-freq s) "u = ~"
      (js/Math.round (:foodtruck-aankopen r)) " aankopen" [:br]
      "Omzet foodtruck: " (f/fmt (:foodtruck-omzet r)) " → "
      [:strong "inkomsten org: " (f/fmt (:foodtruck-afdracht r))] [:br]
      "(standplaats " (f/fmt (:foodtruck-standplaats s)) " + " (:foodtruck-pct s) "% omzet)"]]))

(defn shop-panel []
  (let [s @state/app-state
        r (calc/calc-for-n s (:deelnemers s))]
    [panel "Shop (eigen, sportvoeding & essentials) - Runners"
     [input-number :shop-besteding "Gem. besteding per aankoop €"]
     [input-number :shop-freq "1x kopen per X uur" :min 1]
     [input-number :shop-marge-pct "Netto marge %" :step 5 :min 0 :max 100]
     [:div.staffel-info
      (f/fmt-nl (:total-yards r)) " runner-uren ÷ " (:shop-freq s) "u = ~"
      (js/Math.round (:shop-aankopen r)) " aankopen" [:br]
      "Omzet: " (f/fmt (:shop-omzet r)) " → "
      [:strong "netto marge: " (f/fmt (:shop-marge r))]]]))

(defn overige-inkomsten-panel []
  [panel "Overige inkomsten"
   [input-number :sponsoring "Sponsoring / partnerships €" :step 500]])

(defn yards-panel []
  (let [s @state/app-state
        r (calc/calc-for-n s (:deelnemers s))
        preset (:yard-preset s)]
    [panel "Yards Model (uitvalpatroon)"
     [:div.scenario-btns {:style {:margin-bottom ".75rem"}}
      [:button {:class (when (= preset :duv2025) "active")
                :on-click #(state/update-val! :yard-preset :duv2025)}
       "BYU NL 2025"]
      [:button {:class (when (= preset :duv2026) "active")
                :on-click #(state/update-val! :yard-preset :duv2026)}
       "BYU NL 2026"]]
     [:div.staffel-info
      [:strong "Profiel: " (:profile-label r)] [:br]
      [:strong (f/fmt-nl (:total-yards r)) " totale yards"]
      " · " (:last-yard r) " rondes tot winnaar · "
      (.toFixed (:avg-yards r) 1) " gem. per deelnemer" [:br]
      "Eventduur: ~" (:last-yard r) " uur = " (:loopdagen r) " loopdagen + "
      (:extra-dagen s) " op/afbouw = " (:dagen r) " dagen totaal"]]))

(defn veerboot-panel []
  (let [rederij (:rederij data/cfg)]
    [panel (str rederij " (groepstarief)")
     [:div.input-group
      [:label "Dienst"]
      [:select {:value (:veerboot-type @state/app-state)
                :on-change #(state/update-val! :veerboot-type (.. % -target -value))}
       [:option {:value "veer_winter"}
        (str rederij " veerdienst (winter/okt: €16.57-18.73 enkel p.p.)")]
       [:option {:value "snel"}
        (str rederij " sneldienst (€27.85-30.01 enkel p.p.)")]]]
     [:div.staffel-info
      [:strong (str "Staffel " rederij " (enkel, winter):")] [:br]
      "15-30 pers: €18.73 · 31-50: €17.69 · 51+: €16.57" [:br]
      [:em "Alleen heenreis deelnemers vergoed door org."]]]))

(defn ehbo-panel []
  (let [s @state/app-state
        r (calc/calc-for-n s (:deelnemers s))]
    [panel "EHBO & Medische zorg"
     [input-number :ehbo-uurtarief "Uurtarief EHBO'er €" :step 5]
     [input-number :ehbo-uren "EHBO uren per dag (niet 24u, shifts)"]
     [:div.staffel-info
      [:strong "Bij " (:deelnemers s) " deelnemers + ~" (:crew-deeln r)
       " support crew = " (:aanwezigen r) " aanwezigen:"] [:br]
      "Min. " (get-in r [:ehbo :ehboers]) " EHBO'ers · AED: "
      (if (get-in r [:ehbo :aed]) "ja" "nee") " · Post: "
      (if (get-in r [:ehbo :post]) "ja" "nee") [:br]
      (:loopdagen r) " loopdagen + " (:extra-dagen s) " op/afbouw = "
      [:strong (:dagen r) " dagen"] " (" (:last-yard r) " uur → " (:loopdagen r) "d)" [:br]
      "→ " (f/fmt (+ (:kosten-ehbo r) (:kosten-aed r) (:kosten-ehbo-post r)))
      " totaal (" (:dagen r) "d × " (:ehbo-uren s) "u)"]]))

(defn locatie-panel []
  [panel "Locatie & Logistiek"
   [input-number :locatie-huur (str "Locatiehuur " (:basecamp data/cfg) " (totaal) €") :step 250]
   [input-number :materiaal "Materiaal & infra (hekken, timing, borden) €" :step 250]
   [input-number :vergunning "Vergunning & leges €" :step 50]
   [input-number :verzekering "Verzekering evenement €" :step 100]])

(defn merchandise-panel []
  [panel "Merchandise & Overig"
   [input-number :shirt-kosten "Kosten performance shirt p.p. €"]
   [input-number :startnummer "Startnummer + goodiebag p.p. €" :step 0.5]
   [input-number :prijzengeld "Prijzengeld €" :step 500]
   [input-number :weeztix-fee (str (:ticket-platform data/cfg) " commissie per ticket €") :step 0.25]])

(defn crew-panel []
  (let [s @state/app-state
        r (calc/calc-for-n s (:deelnemers s))
        crew-pp (/ (:kosten-crew-totaal r) (:crew-org s))]
    [panel "Organisatie Crew"
     [input-number :crew-org "Aantal org crew"]
     [input-number :crew-cash "Vergoeding cash per crew (totaal event) €" :step 50]
     [input-number :crew-shoptegoed "Shoptegoed per crew (inkoopwaarde) €" :step 10]
     [input-number :crew-eten "Eten & drinken per crew per dag €" :step 5]
     [input-number :crew-kleding "Crewkleding per persoon €" :step 5]
     [input-number :extra-dagen "Op/afbouw dagen (extra naast loopdagen)" :min 0 :max 3]
     [:div.staffel-info
      [:strong "Totaal per crew: " (f/fmt crew-pp)]
      " (cash + shoptegoed inkoop + eten + kleding)" [:br]
      [:strong "Totaal org crew: " (f/fmt (:kosten-crew-totaal r))]
      " (" (:crew-org s) " personen)" [:br]
      [:em "Shoptegoed €150 waarde → werkelijke kost = inkoopprijs (~"
       (f/fmt-dec (:crew-shoptegoed s)) ")"]]]))

;; --- Stat Cards ---

(defn stat-card [label value sub & {:keys [value-class]}]
  [:div.stat
   [:div.label label]
   [:div.value {:class value-class} value]
   [:div.sub sub]])

;; --- Tables ---

(defn inkomsten-tabel [s r]
  (let [n (:deelnemers s)
        rows [["Ticketverkoop" "Variabel"
               (str n " × " (f/fmt-dec (:netto-ticket r)) " (excl. btw)")
               (:ticket-inkomsten r)]
              ["Foodtruck (standplaats + afdracht)" "Vast+Var"
               (str (f/fmt (:foodtruck-standplaats s)) " + " (:foodtruck-pct s) "% van " (f/fmt (:foodtruck-omzet r)) " omzet")
               (:foodtruck-afdracht r)]
              ["Eigen shop netto marge" "Variabel"
               (str (js/Math.round (:shop-aankopen r)) " aankopen × " (f/fmt-dec (:shop-besteding s))
                    " × " (:shop-marge-pct s) "% → omzet " (f/fmt (:shop-omzet r)))
               (:shop-marge r)]
              ["Sponsoring / partnerships" "Vast" "" (:sponsoring r)]]]
    [:table
     [:thead [:tr [:th "Post"] [:th "Type"] [:th "Toelichting"] [:th.right "Bedrag"]]]
     [:tbody
      (for [[naam type toel bedrag] rows
            :when (pos? bedrag)]
        ^{:key naam}
        [:tr [:td naam] [:td type] [:td toel] [:td.right.positive (f/fmt bedrag)]])
      ^{:key "sub"}
      [:tr.row-subtotal
       [:td {:col-span 3} "Totale inkomsten"]
       [:td.right.positive (f/fmt (:bruto r))]]]]))

(defn kosten-tabel [s r]
  (let [n (:deelnemers s)
        rederij (:rederij data/cfg)
        tp (:ticket-platform data/cfg)
        bc (:basecamp data/cfg)
        gem (:gemeente data/cfg)
        var-sub (+ (:kosten-veerboot r) (:kosten-ehbo r) (:kosten-aed r) (:kosten-ehbo-post r)
                   (:kosten-shirts r) (:kosten-startnr r) (:kosten-weeztix r))
        vast-sub (+ (:kosten-vast r) (:kosten-crew-totaal r))
        rows [[:row (str rederij " deelnemers (heen)") "Variabel"
               (str n " × " (f/fmt-dec (:veer-tarief r)) " (staffel 51+/31-50/15-30)")
               (:kosten-veerboot r)]
              [:row "EHBO team" "Staffel"
               (str (get-in r [:ehbo :ehboers]) " pers × " (:ehbo-uren s) "u × "
                    (:dagen r) "d × " (f/fmt-dec (:ehbo-uurtarief s)))
               (:kosten-ehbo r)]
              [:row "AED huur" "Staffel"
               (if (get-in r [:ehbo :aed]) (str (:dagen r) " dagen") "Niet vereist")
               (:kosten-aed r)]
              [:row "EHBO-post" "Staffel"
               (if (get-in r [:ehbo :post]) (str (:dagen r) " dagen (750+ aanwezigen)") "Niet vereist (<750)")
               (:kosten-ehbo-post r)]
              [:row "Performance shirts" "Variabel"
               (str n " × " (f/fmt-dec (:shirt-kosten s))) (:kosten-shirts r)]
              [:row "Startnummer + goodiebag" "Variabel"
               (str n " × " (f/fmt-dec (:startnummer s))) (:kosten-startnr r)]
              [:row (str tp " commissie") "Variabel"
               (str n " × " (f/fmt-dec (:weeztix-fee s))) (:kosten-weeztix r)]
              [:sub "Variabele kosten" var-sub]
              [:row (str "Locatiehuur " bc) "Vast" (str (:dagen r) "+ dagen") (:locatie r)]
              [:row "Materiaal & infra" "Vast" "Hekken, timing, bewegwijzering" (:materiaal r)]
              [:row "Vergunning & leges" "Vast" (str gem " + omgevingsvergunning") (:vergunning r)]
              [:row "Verzekering" "Vast" "Evenementenverzekering" (:verzekering r)]
              [:row "Prijzengeld" "Vast" "" (:prijzengeld r)]
              [:row "Org crew cash vergoeding" "Vast"
               (str (:crew-org s) " × " (f/fmt-dec (:crew-cash s))) (:kosten-crew-cash r)]
              [:row "Org crew shoptegoed (inkoop)" "Vast"
               (str (:crew-org s) " × " (f/fmt-dec (:crew-shoptegoed s)) " (€150 waarde, ~40% inkoop)")
               (:kosten-crew-shoptegoed r)]
              [:row "Org crew eten & drinken" "Vast"
               (str (:crew-org s) " × " (f/fmt-dec (:crew-eten s)) " × " (:dagen r) "d")
               (:kosten-crew-eten r)]
              [:row "Org crew kleding" "Vast"
               (str (:crew-org s) " × " (f/fmt-dec (:crew-kleding s))) (:kosten-crew-kleding r)]
              [:sub "Vaste kosten" vast-sub]
              [:total (:totaal-kosten r)]]]
    [:table {:style {:margin-top "1rem"}}
     [:thead [:tr [:th "Post"] [:th "Type"] [:th "Staffel / Toelichting"] [:th.right "Bedrag"]]]
     [:tbody
      (map-indexed
       (fn [i [kind & args]]
         (case kind
           :row (let [[naam type toel bedrag] args]
                  ^{:key i}
                  [:tr [:td naam] [:td type] [:td toel] [:td.right (f/fmt bedrag)]])
           :sub (let [[label bedrag] args]
                  ^{:key i}
                  [:tr.row-subtotal [:td {:col-span 3} label] [:td.right (f/fmt bedrag)]])
           :total (let [[bedrag] args
                        netto (- (:bruto r) bedrag)
                        cls (if (>= netto 0) "positive" "negative")]
                    ^{:key i}
                    [:<>
                     [:tr.row-total [:td {:col-span 3} "Totale kosten"] [:td.right (f/fmt bedrag)]]
                     [:tr.row-total [:td {:col-span 3} "Netto resultaat"]
                      [:td.right {:class cls} (f/fmt netto)]]])))
       rows)]]))

(defn gevoeligheid-tabel [s _r]
  (let [n (:deelnemers s)]
    [:div.panel
     [:h2 "Gevoeligheidsanalyse — Netto bij verschillende deelnemersaantallen"]
     [:table
      [:thead [:tr [:th "Deelnemers"] [:th.right "Inkomsten"] [:th.right "Kosten"]
               [:th.right "Netto"] [:th.right "Marge"]]]
      [:tbody
       (for [sn data/sensitivity-levels]
         (let [sr (calc/calc-for-n s sn)
               cls (if (>= (:netto sr) 0) "positive" "negative")
               current? (= sn n)]
           ^{:key sn}
           [:tr {:style (when current? {:background "var(--surface2)"})}
            [:td (str sn (when current? " ◄"))]
            [:td.right (f/fmt (:bruto sr))]
            [:td.right (f/fmt (:totaal-kosten sr))]
            [:td.right {:class cls} (f/fmt (:netto sr))]
            [:td.right {:class cls} (str (.toFixed (:marge sr) 1) "%")]]))]]]))

(defn yards-tabel [r]
  (let [breakdown (:yard-breakdown r)
        cum-yards (reductions + (map :active breakdown))]
    [:div.panel
     [:h2 "Yards Verloop (uitvalcurve)"]
     [:table
      [:thead [:tr [:th "Yard"] [:th.right "Actieve lopers"] [:th.right "Uitval deze ronde"] [:th.right "Cum. yards"]]]
      [:tbody
       (map (fn [{:keys [yard active dropout]} cum]
              ^{:key yard}
              [:tr [:td yard]
               [:td.right active]
               [:td.right (when (pos? dropout) (str "-" dropout))]
               [:td.right (f/fmt-nl cum)]])
            breakdown cum-yards)
       ^{:key "total"}
       [:tr.row-total [:td "Totaal"] [:td] [:td] [:td.right (f/fmt-nl (:total-yards r))]]]]]))

(defn ehbo-staffel-tabel [s]
  [:div.panel
   [:h2 "EHBO Staffelvereisten"]
   [:table
    [:thead [:tr [:th "Aanwezigen (deeln+support crew)"]
             [:th.right "Min. EHBO'ers"] [:th.right "AED vereist"]
             [:th.right "EHBO-post"] [:th.right "Kosten/dag"]]]
    [:tbody
     (let [r (calc/calc-for-n s (:deelnemers s))]
       (for [a data/ehbo-display-levels]
         (let [e (calc/get-ehbo-requirements a)
               dag-kosten (+ (* (:ehboers e) (:ehbo-uurtarief s) (:ehbo-uren s))
                             (if (:aed e) 50 0)
                             (if (:post e) 200 0))
               current? (= a (:aanwezigen r))]
           ^{:key a}
           [:tr {:style (when current? {:background "var(--surface2)"})}
            [:td (str a (when current? " ◄"))]
            [:td.right (:ehboers e)]
            [:td.right (if (:aed e) "Ja" "Nee")]
            [:td.right (if (:post e) "Ja" "Nee")]
            [:td.right (f/fmt dag-kosten)]])))]]])

(defn veerboot-staffel-tabel [s r]
  (let [n (:deelnemers s)
        veer-type (:veerboot-type s)
        staffels [{:label "15-30 personen" :tarief (calc/get-veerboot-tarief 20 veer-type)}
                  {:label "31-50 personen" :tarief (calc/get-veerboot-tarief 40 veer-type)}
                  {:label "51+ personen"   :tarief (calc/get-veerboot-tarief 100 veer-type)}]]
    [:div.panel
     [:h2 "Veerboot Staffel"]
     [:table
      [:thead [:tr [:th "Groepsgrootte"] [:th.right "Tarief p.p. (enkel)"]
               [:th.right "Totaal bij huidig aantal"]]]
      [:tbody
       (for [{:keys [label tarief]} staffels]
         ^{:key label}
         [:tr [:td label] [:td.right (f/fmt-dec tarief)] [:td.right (f/fmt (* n tarief))]])
       ^{:key "current"}
       [:tr.row-subtotal
        [:td (str "Huidig (" n " pers, 51+)")]
        [:td.right (f/fmt-dec (:veer-tarief r))]
        [:td.right (f/fmt (:kosten-veerboot r))]]]]]))

;; --- Assumptions ---

(defn assumptions []
  [:div.assumptions
   [:strong "Aannames:"] [:br]
   "· Veertarieven: groepstarieven veerdienst 2026 (winterperiode okt, incl. 9% btw)" [:br]
   "· EHBO-richtlijnen: landelijke richtlijnen evenementenzorg + Veldnorm Evenementenzorg (VNEZ)" [:br]
   "· Vergunning: gemeentelijke leges + veiligheidsplan + omgevingsvergunning" [:br]
   "· Sportevenement = verhoogd risicoprofiel → extra EHBO-capaciteit vereist" [:br]
   "· Ticketprijs incl. btw → netto inkomst berekend excl. 9% btw" [:br]
   "· Support crew (van deelnemers) niet meegenomen als inkomst (eigen rekening), wel als aanwezigen voor EHBO-berekening" [:br]
   "· Org crew kosten (vergoeding + veerboot) zijn vaste kosten voor de organisatie"])

;; --- Main App ---

(defn app []
  (let [s @state/app-state
        n (:deelnemers s)
        r (calc/calc-for-n s n)
        be (calc/find-breakeven s)
        bar-pct (min 100 (* (/ n be) 100))]
    [:div
     [:h1 (str (:event-naam data/cfg) " " (:locatie data/cfg) " — Financieel Model")]
     [:p.subtitle (str "Organisatorperspectief · " (:maand data/cfg)
                       " · Alle bedragen excl. btw tenzij anders vermeld")]

     [:div.grid
      ;; Left column: controls
      [:div.controls
       [deelnemers-panel]
       [foodtruck-panel]
       [shop-panel]
       [overige-inkomsten-panel]
       [yards-panel]
       [veerboot-panel]
       [ehbo-panel]
       [locatie-panel]
       [merchandise-panel]
       [crew-panel]]

      ;; Right column: results
      [:div.results
       [:div.result-grid
        [stat-card "Bruto Inkomsten" (f/fmt (:bruto r))
         (str "tickets " (f/fmt (:ticket-inkomsten r)) " + extra " (f/fmt (:extra-inkomsten r)))]
        [stat-card "Totale Kosten" (f/fmt (:totaal-kosten r))
         (str (f/fmt-dec (/ (:totaal-kosten r) n)) " p.p.")]
        [stat-card "Netto Resultaat" (f/fmt (:netto r))
         (str (.toFixed (:marge r) 1) "% marge")
         :value-class (if (>= (:netto r) 0) "positive" "negative")]
        [stat-card "Totale Yards" (f/fmt-nl (:total-yards r))
         (str (:last-yard r) " rondes · gem. " (.toFixed (:avg-yards r) 1) " p.p.")]
        [:div.stat
         [:div.label "Break-even"]
         [:div.value be]
         [:div.sub "deelnemers nodig"]
         [:div.breakeven-bar
          [:div.breakeven-fill {:style {:width (str bar-pct "%")
                                        :background (if (>= n be) "var(--green)" "var(--red)")}}]]]]

       [:div.panel
        [:h2 "Inkomsten & Kostenopbouw"]
        [inkomsten-tabel s r]
        [kosten-tabel s r]]

       [gevoeligheid-tabel s r]
       [yards-tabel r]
       [ehbo-staffel-tabel s]
       [veerboot-staffel-tabel s r]
       [assumptions]]]]))
