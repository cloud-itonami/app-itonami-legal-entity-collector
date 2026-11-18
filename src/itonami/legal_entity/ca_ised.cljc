(ns itonami.legal-entity.ca-ised
  "Corporations Canada の Federal Corporations データセット（CSV、先頭行が見出し、英語版）の 1 行を正規化する。
  見出しは 2026-11-18 に corporations-active-cbca-en.csv と
  corporations-inactive-or-dissolved-non-cbca-en.csv の先頭行で確認した（両方同じ 18 列）。
  ライセンスは Open Government Licence - Canada（open.canada.ca のデータセット 0032ce54-…）。
  取締役の人数の列は取り込まない。Status の値は Active のほかを一次資料で確認できていないため、
  Active → :active、Dissolved で始まる → :closed、それ以外 → :inactive とする。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Contains information licensed under the Open Government Licence – Canada (Corporations Canada, Federal Corporations). Normalized by Awai Network.")

(def ^:private bom (str (char 0xFEFF)))

(defn- field [m k]
  (let [v (or (get m k) (get m (str bom k)))]
    (when (and (string? v) (not (str/blank? v))) (str/trim v))))

(defn status [s]
  (cond
    (nil? s) :unknown
    (= "Active" s) :active
    (str/starts-with? s "Dissolved") :closed
    :else :inactive))

(def ^:private provinces
  "州・準州の英語名 → ISO 3166-2:CA の下位コード。"
  {"Alberta" "AB" "British Columbia" "BC" "Manitoba" "MB" "New Brunswick" "NB"
   "Newfoundland and Labrador" "NL" "Nova Scotia" "NS" "Ontario" "ON" "Prince Edward Island" "PE"
   "Quebec" "QC" "Saskatchewan" "SK" "Northwest Territories" "NT" "Nunavut" "NU" "Yukon" "YT"})

(defn normalize [m]
  (let [num (field m "Corporation number")
        bn (field m "Business number (BN)")
        bn9 (when bn (re-find #"^[0-9]{9}" bn))
        st (field m "Status")
        country (field m "Country")
        r (cond-> {:entity/id (str "ca-ised:" num)
                   :entity/jurisdiction "CA"
                   :entity/name (field m "Corporate name - form 1")
                   :entity/identifiers (cond-> [{:scheme :ca-corporation-number :value num}]
                                         bn9 (conj {:scheme :ca-business-number :value bn9}))
                   :entity/status (status st)
                   :source {:id :ca-ised :attribution attribution :modified? true}}
            (field m "Governing legislation")
            (assoc :entity/legal-form {:scheme :ca-governing-legislation :label (field m "Governing legislation")})
            (or (nil? country) (= "Canada" country))
            (assoc :entity/address
                   (cond-> {:country "CA"}
                     (field m "Street") (assoc :street (str/join ", " (keep #(field m %) ["Street" "Street 2"])))
                     (field m "City/town") (assoc :locality (field m "City/town"))
                     (provinces (field m "Province/territory")) (assoc :region (provinces (field m "Province/territory")))
                     (field m "Postal code") (assoc :postal-code (field m "Postal code")))))
        ps (schema/problems r)]
    (if (seq ps) {:reject (str/join "; " ps)} {:record r})))
