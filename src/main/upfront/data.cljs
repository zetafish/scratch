(ns upfront.data)

(def cfg
  {:event-naam      "Can you Stand It?"
   :locatie         "Eiland X"
   :basecamp        "Locatie BC"
   :rederij         "Veerdienst"
   :ticket-platform "Ticketplatform"
   :veiligheidsregio "Veiligheidsregio"
   :gemeente        "Gemeente Eiland"
   :organisator     "Organisator"
   :maand           "Oktober 2026"})

(def yard-presets
  {:duv2025
   {:label      "BYU NL 2025 (Zeewolde, 188 deelnemers, winnaar 36 yards)"
    :drop-pct   {7 1.1, 8 9.0, 9 6.9, 10 9.0, 11 5.9, 12 8.5, 13 4.8, 14 1.1,
                 15 16.0, 16 7.4, 17 4.3, 18 4.8, 19 2.1, 20 1.6, 21 0.5, 22 0.5,
                 23 1.1, 24 5.3, 25 1.6, 26 3.2, 28 0.5, 30 1.6, 31 0.5, 32 1.1,
                 33 0.5, 35 0.5, 36 0.5}
    :winner-yard 36}
   :duv2026
   {:label      "BYU NL 2026 (Leersum, 189 deelnemers, winnaar 50 yards)"
    :drop-pct   {8 8.5, 9 4.8, 10 8.0, 11 5.3, 12 5.3, 13 3.7, 14 1.6,
                 15 10.1, 16 7.9, 17 6.9, 18 4.8, 19 1.6, 20 3.2, 21 2.6,
                 22 2.6, 23 0.5, 24 11.6, 25 3.2, 26 2.6, 27 1.1, 28 0.5,
                 30 0.5, 32 0.5, 34 0.5, 38 0.5, 40 0.5, 49 0.5, 50 0.5}
    :winner-yard 50}})

(def veerboot-opties
  [{:value "veer_winter" :label-fn #(str (:rederij %) " veerdienst (winter/okt: €16.57-18.73 enkel p.p.)")}
   {:value "snel"        :label-fn #(str (:rederij %) " sneldienst (€27.85-30.01 enkel p.p.)")}])

(def scenario-map
  {:klein 100
   :middel 200
   :groot 350
   :uitverkocht 500})

(def sensitivity-levels [50 100 150 200 250 300 350 400 500 750])

(def ehbo-display-levels [50 100 200 300 500 750 1000 1500])
