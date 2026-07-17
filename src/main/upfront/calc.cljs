(ns upfront.calc
  (:require [upfront.data :as data]))

(defn get-veerboot-tarief [n type]
  (if (= type "snel")
    (cond (>= n 51) 27.85
          (>= n 31) 28.97
          :else     30.01)
    (cond (>= n 51) 16.57
          (>= n 31) 17.69
          :else     18.73)))

(defn get-ehbo-requirements [aanwezigen]
  (let [base (cond
               (<= aanwezigen 50)   {:ehboers 0  :aed false :post false}
               (<= aanwezigen 250)  {:ehboers 2  :aed true  :post false}
               (<= aanwezigen 500)  {:ehboers (max 2 (js/Math.ceil (/ aanwezigen 250))) :aed true :post false}
               (<= aanwezigen 750)  {:ehboers (max 3 (js/Math.ceil (/ aanwezigen 200))) :aed true :post false}
               (<= aanwezigen 1000) {:ehboers (max 4 (js/Math.ceil (/ aanwezigen 200))) :aed true :post true}
               :else                {:ehboers (max 5 (js/Math.ceil (/ aanwezigen 150))) :aed true :post true})
        sport-min (when (> aanwezigen 250)
                    (+ 2 (js/Math.ceil (/ (- aanwezigen 250) 250))))]
    (if sport-min
      (update base :ehboers max sport-min)
      base)))

(defn calc-for-n [inputs n]
  (let [{:keys [ticketprijs crew-ratio ehbo-uurtarief ehbo-uren extra-dagen
                locatie-huur materiaal vergunning verzekering
                shirt-kosten startnummer prijzengeld weeztix-fee
                crew-org crew-cash crew-shoptegoed crew-eten crew-kleding
                veerboot-type yard-preset
                foodtruck-besteding foodtruck-freq foodtruck-standplaats foodtruck-pct
                shop-besteding shop-freq shop-marge-pct sponsoring]} inputs

        netto-ticket    (/ ticketprijs 1.09)
        ticket-inkomsten (* n netto-ticket)

        profile         (get data/yard-presets (or yard-preset :duv2025))
        drop-pct        (:drop-pct profile)
        max-yard        (inc (or (:winner-yard profile) 50))

        ;; Yard simulation
        sim (loop [y 1, active n, total-yards 0, breakdown [], last-yard 0]
              (if (or (> y max-yard) (<= active 1))
                {:total-yards   (+ total-yards (if (>= active 1) 1 0))
                 :breakdown     (if (>= active 1)
                                  (conj breakdown {:yard (inc last-yard) :active 1 :dropout 0})
                                  breakdown)
                 :last-yard     (if (>= active 1) (inc last-yard) last-yard)}
                (let [pct     (get drop-pct y 0)
                      dropout (js/Math.round (* n (/ pct 100)))
                      new-active (max 1 (- active dropout))]
                  (recur (inc y)
                         new-active
                         (+ total-yards active)
                         (conj breakdown {:yard y :active active :dropout dropout})
                         y))))

        total-yards      (:total-yards sim)
        yard-breakdown   (:breakdown sim)
        last-yard        (:last-yard sim)
        avg-yards        (if (pos? n) (/ total-yards n) 0)
        loopdagen        (js/Math.ceil (/ last-yard 24))
        dagen            (+ loopdagen extra-dagen)

        ;; Foodtruck
        crew-deeln       (js/Math.round (* n crew-ratio))
        aanwezigen       (+ n crew-deeln)
        support-crew-uren (* crew-deeln last-yard)
        persoon-uren     (+ total-yards support-crew-uren)
        ft-aankopen      (/ persoon-uren foodtruck-freq)
        ft-omzet         (* ft-aankopen foodtruck-besteding)
        ft-afdracht      (+ (* ft-omzet (/ foodtruck-pct 100)) foodtruck-standplaats)

        ;; Shop
        shop-aankopen    (/ total-yards shop-freq)
        shop-omzet       (* shop-aankopen shop-besteding)
        shop-marge       (* shop-omzet (/ shop-marge-pct 100))

        extra-inkomsten  (+ ft-afdracht shop-marge sponsoring)
        bruto            (+ ticket-inkomsten extra-inkomsten)

        ;; Veerboot
        veer-tarief      (get-veerboot-tarief n veerboot-type)
        kosten-veerboot  (* n veer-tarief)

        ;; EHBO
        ehbo             (get-ehbo-requirements aanwezigen)
        kosten-ehbo      (* (:ehboers ehbo) ehbo-uurtarief ehbo-uren dagen)
        kosten-aed       (if (:aed ehbo) (* 50 dagen) 0)
        kosten-ehbo-post (if (:post ehbo) (* 200 dagen) 0)

        ;; Per deelnemer
        kosten-shirts    (* n shirt-kosten)
        kosten-startnr   (* n startnummer)
        kosten-weeztix   (* n weeztix-fee)

        ;; Crew
        kosten-crew-cash      (* crew-org crew-cash)
        kosten-crew-shoptegoed (* crew-org crew-shoptegoed)
        kosten-crew-eten      (* crew-org crew-eten dagen)
        kosten-crew-kleding   (* crew-org crew-kleding)
        kosten-crew-totaal    (+ kosten-crew-cash kosten-crew-shoptegoed
                                 kosten-crew-eten kosten-crew-kleding)

        ;; Vast
        kosten-vast      (+ locatie-huur materiaal vergunning verzekering prijzengeld)

        totaal-kosten    (+ kosten-veerboot kosten-ehbo kosten-aed kosten-ehbo-post
                            kosten-shirts kosten-startnr kosten-weeztix
                            kosten-crew-totaal kosten-vast)
        netto            (- bruto totaal-kosten)
        marge            (if (pos? bruto) (* (/ netto bruto) 100) 0)]

    {:bruto bruto :ticket-inkomsten ticket-inkomsten :extra-inkomsten extra-inkomsten
     :totaal-kosten totaal-kosten :netto netto :marge marge
     :total-yards total-yards :last-yard last-yard :avg-yards avg-yards
     :yard-breakdown yard-breakdown :persoon-uren persoon-uren
     :loopdagen loopdagen :dagen dagen
     :profile-label (:label profile)
     :foodtruck-omzet ft-omzet :foodtruck-afdracht ft-afdracht :foodtruck-aankopen ft-aankopen
     :shop-omzet shop-omzet :shop-marge shop-marge :shop-aankopen shop-aankopen
     :sponsoring sponsoring
     :crew-deeln crew-deeln :aanwezigen aanwezigen :ehbo ehbo
     :netto-ticket netto-ticket :veer-tarief veer-tarief
     :kosten-veerboot kosten-veerboot :kosten-ehbo kosten-ehbo
     :kosten-aed kosten-aed :kosten-ehbo-post kosten-ehbo-post
     :kosten-shirts kosten-shirts :kosten-startnr kosten-startnr
     :kosten-weeztix kosten-weeztix
     :kosten-crew-cash kosten-crew-cash :kosten-crew-shoptegoed kosten-crew-shoptegoed
     :kosten-crew-eten kosten-crew-eten :kosten-crew-kleding kosten-crew-kleding
     :kosten-crew-totaal kosten-crew-totaal :kosten-vast kosten-vast
     :locatie locatie-huur :materiaal materiaal :vergunning vergunning
     :verzekering verzekering :prijzengeld prijzengeld}))

(defn find-breakeven [inputs]
  (loop [be 50]
    (if (> be 750)
      be
      (let [r (calc-for-n inputs be)]
        (if (>= (:netto r) 0)
          be
          (recur (+ be 5)))))))
