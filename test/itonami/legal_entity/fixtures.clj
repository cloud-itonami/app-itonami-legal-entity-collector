(ns itonami.legal-entity.fixtures
  "架空データだけ。法人番号の 99 始まりと LEI の FAKE00TESTLEI は実在しない値として作った
  （チェックデジットだけ正しくしてある）。"
  (:require [clojure.string :as str]))

(defn nta-line
  "30 列の CSV 1 行。overrides は列名 → 値。"
  [overrides]
  (let [base {:sequence-number "1" :corporate-number "8990000000001" :process "01" :correct "0"
              :update-date "2026-11-02" :change-date "2026-11-02" :name "架空商事株式会社"
              :name-image-id "" :kind "301" :prefecture-name "東京都" :city-name "千代田区"
              :street-number "架空町１－２－３" :address-image-id "" :prefecture-code "13"
              :city-code "101" :post-code "1000000" :address-outside "" :address-outside-image-id ""
              :close-date "" :close-cause "" :successor-corporate-number "" :change-cause ""
              :assignment-date "2026-11-02" :latest "1" :en-name "Kaku Shoji Co., Ltd."
              :en-prefecture-name "Tokyo" :en-city-name "Chiyoda ku" :en-address-outside ""
              :furigana "カクウショウジ" :hihyoji "0"}
        cols [:sequence-number :corporate-number :process :correct :update-date :change-date
              :name :name-image-id :kind :prefecture-name :city-name :street-number
              :address-image-id :prefecture-code :city-code :post-code :address-outside
              :address-outside-image-id :close-date :close-cause :successor-corporate-number
              :change-cause :assignment-date :latest :en-name :en-prefecture-name
              :en-city-name :en-address-outside :furigana :hihyoji]
        m (merge base overrides)]
    (str/join "," (map #(str "\"" (str/replace (get m %) "\"" "\"\"") "\"") cols))))

(def nta-lines
  {:active (nta-line {})
   :closed (nta-line {:corporate-number "7990000000002" :name "架空精算合同会社" :kind "305"
                      :process "21" :close-date "2026-10-31" :close-cause "01"})
   :quoted-comma (nta-line {:corporate-number "5990000000004" :process "01"
                            :name "株式会社\"架空\",テスト"})
   :hihyoji (nta-line {:corporate-number "6990000000003" :hihyoji "1"})
   :deleted (nta-line {:corporate-number "6990000000003" :process "99"})
   :bad-check-digit (nta-line {:corporate-number "1990000000001"})
   :bad-kind (nta-line {:kind "999"})
   :too-few-columns "\"1\",\"8990000000001\",\"01\""
   :unclosed-quote "\"1\",\"8990000000001"})

(def lei-1 "FAKE00TESTLEI0000134")
(def lei-2 "FAKE00TESTLEI0000231")

(defn- deep-merge [& ms]
  (apply merge-with (fn [a b] (if (and (map? a) (map? b)) (deep-merge a b) b)) ms))

(defn gleif-record [overrides]
  (deep-merge
              {:attributes {:lei lei-1
                            :entity {:legalName {:name "Fictional Holdings AB" :language "sv"}
                                     :jurisdiction "SE"
                                     :status "ACTIVE"
                                     :creationDate "1999-01-02T00:00:00Z"
                                     :registeredAs "5560000000"
                                     :registeredAt {:id "RA000544"}
                                     :legalForm {:id "XJHM"}
                                     :legalAddress {:addressLines ["Fiktiv gatan 1"] :city "Stad"
                                                    :region "SE-M" :country "SE" :postalCode "000 00"}}
                            :registration {:status "ISSUED" :lastUpdateDate "2026-11-17T07:18:20Z"}}}
              overrides))

(def gleif-records
  {:active (gleif-record {})
   :us-subdivision (gleif-record {:attributes {:lei lei-2
                                               :entity {:legalName {:name "Fictional Corp"}
                                                        :jurisdiction "US-DE" :status "INACTIVE"
                                                        :registeredAs nil :legalForm nil
                                                        :legalAddress {:addressLines ["1 Fake St"] :city "Dover"
                                                                       :region "US-DE" :country "US" :postalCode "00000"}}}})
   :bad-lei (gleif-record {:attributes {:lei "FAKE00TESTLEI0000199"}})
   :no-name (gleif-record {:attributes {:entity {:legalName {:name ""}}}})})
