(ns itonami.legal-entity.gb-companies-house
  "Companies House の Free Company Data Product（CSV、先頭行が見出し）の 1 行を正規化する。
  項目は「Free Data Product - Data Fields」に従う。
  https://resources.companieshouse.gov.uk/toolsToHelp/pdf/freeDataProductDataset.pdf
  （2026-11-18 取得、sha256 be0e2ac20bfd4793b1eecae87929d8016c00bdf8639fd708514ba3edbbd041bb）
  この仕様書は見出しの綴りを示していないため、登記住所の項目は `RegAddress.` 付きと無しの両方を受け付ける。
  日付は dd/mm/yyyy と yyyy-mm-dd を受け付け、どちらでもなければ載せない。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Source: Companies House (Free Company Data Product). Normalized by Awai Network.")

(defn- blank->nil [s] (when-not (str/blank? s) (str/trim s)))

(defn- field [m & ks] (some #(blank->nil (get m %)) ks))

(defn- addr [m k] (field m (str "RegAddress." k) k))

(defn- date [s]
  (when-let [s (blank->nil s)]
    (or (when-let [[_ d mo y] (re-matches #"(\d{2})/(\d{2})/(\d{4})" s)] (str y "-" mo "-" d))
        (when (re-matches #"\d{4}-\d{2}-\d{2}" s) s))))

(defn status [s]
  (cond
    (nil? s) :unknown
    (str/starts-with? s "Active") :active
    (= s "Dissolved") :closed
    :else :inactive))

(defn normalize
  "見出し→値の map を {:record}/{:reject} にする。"
  [m]
  (if (:reject m)
    m
    (let [number (field m "CompanyNumber")
          st (field m "CompanyStatus")
          r (cond-> {:entity/id (str "gb-companies-house:" number)
                     :entity/jurisdiction "GB"
                     :entity/name (field m "CompanyName")
                     :entity/identifiers [{:scheme :gb-company-number :value number}]
                     :entity/status (status st)
                     :entity/address (cond-> {:country "GB"}
                                       (addr m "AddressLine1") (assoc :street (str/join ", " (keep #(addr m %) ["AddressLine1" "AddressLine2"])))
                                       (addr m "PostTown") (assoc :locality (addr m "PostTown"))
                                       (addr m "County") (assoc :region (addr m "County"))
                                       (addr m "PostCode") (assoc :postal-code (addr m "PostCode")))
                     :source {:id :gb-companies-house
                              :company-status st
                              :attribution attribution
                              :modified? true}}
              (field m "CompanyCategory") (assoc :entity/legal-form {:scheme :gb-company-category
                                                                     :label (field m "CompanyCategory")})
              (date (get m "IncorporationDate")) (assoc :entity/registered-on (date (get m "IncorporationDate")))
              (date (get m "DissolutionDate")) (assoc :entity/closed-on (date (get m "DissolutionDate"))))
          ps (schema/problems r)]
      (if (seq ps) {:reject (str/join "; " ps)} {:record r}))))
