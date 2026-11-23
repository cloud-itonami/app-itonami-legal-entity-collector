(ns itonami.legal-entity.cz-ares
  "チェコ ARES（Administrativní registr ekonomických subjektů）の REST API
  GET /ekonomicke-subjekty/{ico} の応答（EkonomickySubjekt）を正規化する。
  項目は OpenAPI 3.0.0 定義 https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/v3/api-docs
  （ARES REST API v1.4.0、2026-11-23 取得、132815 bytes、
  sha256 822372bcc181740c1a0f43d0507c204780f575c7ddea837a65064a07a0819134）に従う。
  この parser は取得済みの 1 件（map）を受け取るだけで、ネットワークに触れない。
  取得時の照会は 500件/分を超えてはならない。財務省の運用条件
  （https://www.mfcr.cz/cs/ministerstvo/informacni-systemy/ares 、2026-11-23 確認）は
  500件/分超過・同一照会の反復・多数の同時自動照会・複数 IP での制限回避・
  ランダム探索・セキュリティ破壊の試みを受けてアクセス制限・遮断がありうるとする。
  個人事業主（pravniForma が 101/102/104/105/106/107/108 の fyzická osoba）は取り込まない。
  IČO が割当てられていない主体（ico が無く icoId のみ）は取り込まない。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Source: ARES (Administrativní registr ekonomických subjektů, Ministerstvo financí ČR). Modified by Awai Network.")

(def ^:private sole-trader-forms #{"101" "102" "104" "105" "106" "107" "108"})

(defn- field [m k]
  (let [v (get m k)]
    (when (and (string? v) (not (str/blank? v))) (str/trim v))))

(defn- num-str [v]
  (when (some? v) (str v)))

(defn- pad-left [s n]
  (if (< (count s) n)
    (str (apply str (repeat (- n (count s)) "0")) s)
    s))

(defn- street-of [a]
  (when-let [ulice (field a :nazevUlice)]
    (let [domovni (num-str (:cisloDomovni a))
          orient (num-str (:cisloOrientacni a))
          pismeno (or (field a :cisloOrientacniPismeno) "")]
      (str ulice
           (cond
             (and domovni orient) (str " " domovni "/" orient pismeno)
             domovni (str " " domovni)
             orient (str " /" orient pismeno)
             :else "")))))

(defn- postal-of [a]
  (if-let [psc (num-str (:psc a))]
    (pad-left psc 5)
    (field a :pscTxt)))

(defn normalize [m]
  (let [pf (field m :pravniForma)]
    (cond
      (sole-trader-forms pf) {:skip "fyzická osoba podnikající（個人事業主）"}
      :else
      (let [ico (or (field m :ico)
                    (let [id (field m :icoId)]
                      (when (and id (re-matches #"[0-9]{8}" id)) id)))]
        (if-not ico
          {:reject "IČO がない（ico が無く icoId のみ）"}
          (let [a (or (:sidlo m) {})
                zanik (field m :datumZaniku)
                vzniku (field m :datumVzniku)
                aktualizace (field m :datumAktualizace)
                addr (cond-> {:country (or (field a :kodStatu) "CZ")}
                       (street-of a) (assoc :street (street-of a))
                       (field a :nazevObce) (assoc :locality (field a :nazevObce))
                       (postal-of a) (assoc :postal-code (postal-of a))
                       (field a :textovaAdresa) (assoc :full (field a :textovaAdresa)))
                addr (when (> (count addr) 1) addr)
                r (cond-> {:entity/id (str "cz-ares:" ico)
                           :entity/jurisdiction "CZ"
                           :entity/name (field m :obchodniJmeno)
                           :entity/identifiers [{:scheme :cz-ico :value ico}]
                           :entity/status (if zanik :closed :active)
                           :source {:id :cz-ares :attribution attribution :modified? true}}
                      pf (assoc :entity/legal-form {:scheme :cz-pravni-forma :code pf})
                      addr (assoc :entity/address addr)
                      vzniku (assoc :entity/registered-on vzniku)
                      zanik (assoc :entity/closed-on zanik)
                      aktualizace (assoc-in [:source :updated-on] aktualizace))
                ps (schema/problems r)]
            (if (seq ps) {:reject (str/join "; " ps)} {:record r})))))))
