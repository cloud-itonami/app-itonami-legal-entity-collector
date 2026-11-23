(ns itonami.legal-entity.schema
  "法人レコードの共通スキーマ。全 collector はこの形に正規化して返す。

  {:entity/id           \"jp-nta:7000012050002\"   ; ソース名:ソース内の ID
   :entity/jurisdiction \"JP\"                     ; ISO 3166-1 alpha-2
   :entity/name         \"...\"
   :entity/identifiers  [{:scheme :jp-corporate-number :value \"...\"} ...]
   :entity/status       :active | :inactive | :closed | :unknown
   ;; 以下は任意
   :entity/jurisdiction-detail \"US-DE\"
   :entity/name-en  :entity/name-kana
   :entity/legal-form {:scheme kw :code str :label str}
   :entity/address {:country :region :locality :street :postal-code}
   :entity/registered-on \"YYYY-MM-DD\"  :entity/closed-on \"YYYY-MM-DD\"
   :source {:id kw :updated-on str :attribution str :modified? true}}

  NZ 由来レコードの設計制約（docs/adr/ADR-001-nz-bulkdata-usage-constraints.md）。
  NZ は BDAA 申請保留（owner 判断 2026-11-23）。設計前提は NZBN API 経路のみで、
  NZBN API の利用条件は未確認（未確認のまま。推測で埋めない）。実データ取得は
  NZBN API の条件が揃うまでしない。
  NZ 由来の :source は BDAA 条項（4.4(3) / 4.6 等の契約条項）を契約として継承しない
  （BDAA を用いない間は適用対象外）。ただし設計制約として、由来が NZ のレコードは
  マーケティング・リード獲得用途に用いないこと（禁止、既定）と、第三者に渡さないこと
  （非渡し方針）を維持する。

  出所表示・加工明示（:source {:attribution ... :modified? ...}）は契約要件ではなく、
  公的データ一般原則として行う（契約が根拠ではない）。

  個人の情報（氏名・住所・生年月日など）は入れない。個人情報保護法の条番号対応表は
  docs/adr/ADR-001-nz-bulkdata-usage-constraints.md を参照。"
  (:require [itonami.legal-entity.ids :as ids]))

(def statuses #{:active :inactive :closed :unknown})

(def identifier-schemes
  "scheme → 値の検証関数。未知の scheme は拒否する。"
  {:jp-corporate-number ids/jp-corporate-number?
   :lei ids/lei?
   :gb-company-number ids/gb-company-number?
   :fr-siren ids/fr-siren?
   :no-orgnr ids/no-orgnr?
   :fi-business-id ids/fi-business-id?
   :us-sec-cik ids/us-sec-cik?
   :au-abn ids/au-abn?
   :au-acn ids/au-acn?
   :ca-corporation-number ids/ca-corporation-number?
   :ca-business-number ids/ca-business-number?
   :registration-number #(and (string? %) (<= 1 (count %) 64))})

(defn identifier-problems [{:keys [scheme value]}]
  (if-let [ok? (identifier-schemes scheme)]
    (when-not (ok? value) [(str "識別子の値が不正: " (name scheme) " " (pr-str value))])
    [(str "未知の識別子 scheme: " (pr-str scheme))]))

(defn problems
  "レコードの問題点のベクタ。空なら正しい。"
  [r]
  (let [opt-date (fn [k] (when-let [d (get r k)]
                           (when-not (ids/iso-date? d) [(str (name k) " が YYYY-MM-DD でない")])))]
    (vec
     (concat
      (when-not (and (string? (:entity/id r)) (re-matches #"[a-z0-9-]+:.+" (:entity/id r)))
        ["entity/id が source:id の形でない"])
      (when-not (ids/country-code? (:entity/jurisdiction r)) ["entity/jurisdiction が ISO 3166-1 alpha-2 でない"])
      (when-not (and (string? (:entity/name r)) (seq (:entity/name r))) ["entity/name が空"])
      (when-not (statuses (:entity/status r)) ["entity/status が不正"])
      (let [ids (:entity/identifiers r)]
        (if (and (sequential? ids) (seq ids))
          (mapcat identifier-problems ids)
          ["entity/identifiers が空"]))
      (opt-date :entity/registered-on)
      (opt-date :entity/closed-on)
      (when-not (keyword? (get-in r [:source :id])) ["source/id がない"])
      (when-not (string? (get-in r [:source :attribution])) ["source/attribution（出典表示）がない"])))))

(defn valid? [r] (empty? (problems r)))
