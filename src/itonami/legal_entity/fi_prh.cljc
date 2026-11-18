(ns itonami.legal-entity.fi-prh
  "PRH（フィンランド特許登録庁）YTJ オープンデータ API v3 の Company（JSON をキーワード化した map）を正規化する。
  項目は OpenAPI 定義 https://avoindata.prh.fi/opendata-ytj-api/v3/schema?lang=en に従う
  （2026-11-18 取得、sha256 cac658d4d4cf5d2b5bd55cd7f41f2bd1741cbfc7323bccd03960f5d04181087e）。データは CC BY 4.0。

  会社形態（companyForms[].type）が下の一覧（コード表 YRMU）にないもの、および会社形態がないものは取り込まない。
  個人の事業者が混ざるのを避けるため。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Source: Finnish Patent and Registration Office (PRH), open data, CC BY 4.0. Modified by Awai Network.")

(def company-forms
  "コード表 YRMU（https://avoindata.prh.fi/opendata-ytj-api/v3/description?code=YRMU&lang=en 、2026-11-18 取得）。
  略号と数値コードのどちらでも引けるようにする。"
  (let [rows [["AOY" "2" "Housing company"] ["ASH" "3" "Resident-administered area"]
              ["ASY" "4" "Right-of-occupancy association"] ["AY" "5" "Partnership"]
              ["AYH" "6" "Non-profit association"] ["ETS" "7" "Finnish branch of a European economic interest grouping"]
              ["ETY" "8" "European economic interest grouping"] ["SCE" "83" "European co-operative society"]
              ["SCP" "84" "European co-operative bank"] ["HY" "9" "Mortgage society"]
              ["KOY" "10" "Limited liability joint-stock property company"] ["KVJ" "11" "Public mutual insurance company"]
              ["KVY" "12" "Mutual insurance company"] ["KY" "13" "Limited partnership"]
              ["OK" "14" "Co-operative"] ["OP" "15" "Co-operative bank"] ["OY" "16" "Limited company"]
              ["OYJ" "17" "Public limited company"] ["SE" "80" "European company"]
              ["SL" "19" "Branch of a foreign trader"] ["SP" "20" "Savings bank"] ["SÄÄ" "18" "Foundation"]
              ["TYH" "21" "Association for carrying on economic activity"] ["VOJ" "23" "Public limited insurance company"]
              ["VOY" "24" "Limited insurance company"] ["VY" "25" "Insurance association"]
              ["VALTLL" "22" "State-owned company"]]]
    (into {} (mapcat (fn [[abbr code label]] [[abbr label] [code label]]) rows))))

(defn- current [xs] (first (filter #(and (= 1 (:version %)) (nil? (:endDate %))) xs)))

(defn normalize [c]
  (let [bid (get-in c [:businessId :value])
        form (current (:companyForms c))
        form-code (some-> (:type form) str)
        nm (first (filter #(= "1" (str (:type %))) (filter #(and (= 1 (:version %)) (nil? (:endDate %))) (:names c))))
        street (first (filter #(= 1 (:type %)) (:addresses c)))]
    (if-not (company-forms form-code)
      {:skip (str "会社形態が対象外: " (pr-str form-code))}
      (let [r (cond-> {:entity/id (str "fi-prh:" bid)
                       :entity/jurisdiction "FI"
                       :entity/name (:name nm)
                       :entity/identifiers [{:scheme :fi-business-id :value bid}]
                       :entity/legal-form {:scheme :fi-yrmu :code form-code :label (company-forms form-code)}
                       :entity/status (cond (:endDate c) :closed
                                            (= "5" (str (:status c))) :closed
                                            (= "2" (str (:status c))) :active
                                            :else :unknown)
                       :source {:id :fi-prh
                                :updated-on (some-> (:lastModified c) (subs 0 10))
                                :trade-register-status (:tradeRegisterStatus c)
                                :attribution attribution
                                :modified? true}}
                street (assoc :entity/address (cond-> {:country "FI"}
                                                (:street street) (assoc :street (str/join " " (keep street [:street :buildingNumber])))
                                                (:city (first (:postOffices street))) (assoc :locality (:city (first (:postOffices street))))
                                                (:postCode street) (assoc :postal-code (:postCode street))))
                (:registrationDate c) (assoc :entity/registered-on (:registrationDate c))
                (:endDate c) (assoc :entity/closed-on (:endDate c)))
            ps (schema/problems r)]
        (if (seq ps) {:reject (str/join "; " ps)} {:record r})))))
