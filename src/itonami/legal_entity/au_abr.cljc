(ns itonami.legal-entity.au-abr
  "ABN Lookup Bulk Extract（XML）の ABR 要素 1 件を正規化する。入力は XML を次の形の map にしたもの
  （要素名はスキーマ bulkextract.xsd に従う。XML から map への変換はここでは行わない）。

    {:ABN {:value \"...\" :status \"ACT\"|\"CAN\" :ABNStatusFromDate \"YYYYMMDD\"}
     :EntityType {:EntityTypeInd \"PRV\" :EntityTypeText \"...\"}
     :MainEntity {:NonIndividualName {:type \"MN\" :NonIndividualNameText \"...\"}
                  :BusinessAddress {:AddressDetails {:State \"NSW\" :Postcode \"2000\"}}}
     :LegalEntity {...}            ; 個人。取り込まない
     :ASICNumber {:value \"...\" :ASICNumberType \"...\"}}

  スキーマ: data.gov.au の bulkextract.xsd（2026-11-18 取得、sha256 e19f5f991fdc3498e71faaf9f88574cab5d1d7d3cc23015c324e2f75ca45c670）。
  ライセンスは CC BY 3.0 AU（data.gov.au のデータセット abn-bulk-extract）。
  個人事業主（LegalEntity を持つ、または EntityTypeInd が IND）を取り込まない。信託・組合は家族名を
  含むことがあるため、会社と組合（co-operative）など法人の形態 `company-types` だけを受け付ける。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.ids :as ids]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Source: Australian Business Register (ABN Lookup Bulk Extract), licensed under CC BY 3.0 AU. Normalized by Awai Network.")

(def company-types
  "受け付ける EntityTypeInd。PRV: Australian Private Company、PUB: Australian Public Company、
  COP: Co-operative、OIE: Other Incorporated Entity。"
  #{"PRV" "PUB" "COP" "OIE"})

(defn- blank->nil [s] (when (and (string? s) (not (str/blank? s))) (str/trim s)))

(defn- ymd [s]
  (when-let [[_ y m d] (and (string? s) (re-matches #"(\d{4})(\d{2})(\d{2})" s))]
    (when (not= "00010101" s) (str y "-" m "-" d))))

(defn normalize [e]
  (let [kind (get-in e [:EntityType :EntityTypeInd])]
    (cond
      (or (:LegalEntity e) (= "IND" kind)) {:skip "個人（LegalEntity / IND）"}
      (not (company-types kind)) {:skip (str "法人の形態の対象外（" (pr-str kind) "）")}
      :else
      (let [abn (get-in e [:ABN :value])
            st (get-in e [:ABN :status])
            from (ymd (get-in e [:ABN :ABNStatusFromDate]))
            acn (blank->nil (get-in e [:ASICNumber :value]))
            ad (get-in e [:MainEntity :BusinessAddress :AddressDetails])
            r (cond-> {:entity/id (str "au-abr:" abn)
                       :entity/jurisdiction "AU"
                       :entity/name (blank->nil (get-in e [:MainEntity :NonIndividualName :NonIndividualNameText]))
                       :entity/identifiers (cond-> [{:scheme :au-abn :value abn}]
                                             (ids/au-acn? acn) (conj {:scheme :au-acn :value acn}))
                       :entity/status (case st "ACT" :active "CAN" :closed :unknown)
                       :entity/legal-form {:scheme :au-entity-type :code kind
                                           :label (get-in e [:EntityType :EntityTypeText])}
                       :source {:id :au-abr :attribution attribution :modified? true}}
                (and from (= "CAN" st)) (assoc :entity/closed-on from)
                ad (assoc :entity/address (cond-> {:country "AU"}
                                            (blank->nil (:State ad)) (assoc :region (str/trim (:State ad)))
                                            (blank->nil (:Postcode ad)) (assoc :postal-code (str/trim (:Postcode ad))))))
            ps (schema/problems r)]
        (if (seq ps) {:reject (str/join "; " ps)} {:record r})))))
