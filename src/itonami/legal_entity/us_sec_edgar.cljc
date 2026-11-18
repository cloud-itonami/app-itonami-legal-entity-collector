(ns itonami.legal-entity.us-sec-edgar
  "SEC EDGAR の submissions JSON（data.sec.gov/submissions/CIK##########.json、または submissions.zip の
  各ファイル）をキーワード化した map を正規化する。項目名は 2026-11-18 に公開会社 1 社の応答で確認した
  （cik, entityType, name, stateOfIncorporation, addresses.business, lei）。
  SEC.gov の情報は SEC の許可なく複製・再配布できる（出典として SEC を示す）。取得時は User-Agent に
  名前と連絡先を申告し、全体で毎秒 10 件以下にする（SEC「Accessing EDGAR Data」）。取得はここでは行わない。
  個人の filer（役員・大株主など）を取り込まないため、entityType が \"operating\" のものだけを受け付ける。
  EDGAR は会社の存続状態を持たないので :entity/status は :unknown。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.ids :as ids]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Source: U.S. Securities and Exchange Commission (EDGAR). Normalized by Awai Network.")

(def us-states
  #{"AL" "AK" "AZ" "AR" "CA" "CO" "CT" "DE" "DC" "FL" "GA" "HI" "ID" "IL" "IN" "IA" "KS" "KY" "LA" "ME"
    "MD" "MA" "MI" "MN" "MS" "MO" "MT" "NE" "NV" "NH" "NJ" "NM" "NY" "NC" "ND" "OH" "OK" "OR" "PA" "RI"
    "SC" "SD" "TN" "TX" "UT" "VT" "VA" "WA" "WV" "WI" "WY"})

(defn- blank->nil [s] (when (and (string? s) (not (str/blank? s))) (str/trim s)))

(defn- pad-cik [c]
  (let [s (str c)]
    (if (re-matches #"[0-9]{1,10}" s) (str (apply str (repeat (- 10 (count s)) "0")) s) s)))

(defn normalize [s]
  (if (not= "operating" (:entityType s))
    {:skip (str "entityType が operating でない（" (pr-str (:entityType s)) "）")}
    (let [cik (pad-cik (:cik s))
          state (blank->nil (:stateOfIncorporation s))
          a (get-in s [:addresses :business])
          lei (blank->nil (:lei s))
          r (cond-> {:entity/id (str "us-sec-edgar:" cik)
                     :entity/jurisdiction "US"
                     :entity/name (blank->nil (:name s))
                     :entity/identifiers (cond-> [{:scheme :us-sec-cik :value cik}]
                                           (ids/lei? lei) (conj {:scheme :lei :value lei}))
                     :entity/status :unknown
                     :source {:id :us-sec-edgar :attribution attribution :modified? true}}
              (us-states state) (assoc :entity/jurisdiction-detail (str "US-" state))
              (and a (not= 1 (:isForeignLocation a)))
              (assoc :entity/address
                     (cond-> {:country "US"}
                       (blank->nil (:street1 a)) (assoc :street (str/join ", " (keep blank->nil [(:street1 a) (:street2 a)])))
                       (blank->nil (:city a)) (assoc :locality (str/trim (:city a)))
                       (blank->nil (:stateOrCountry a)) (assoc :region (str/trim (:stateOrCountry a)))
                       (blank->nil (:zipCode a)) (assoc :postal-code (str/trim (:zipCode a))))))
          ps (schema/problems r)]
      (if (seq ps) {:reject (str/join "; " ps)} {:record r}))))
