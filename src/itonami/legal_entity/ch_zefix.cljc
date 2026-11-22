(ns itonami.legal-entity.ch-zefix
  "スイス連邦 Zefix PublicREST API の CompanyFull（JSON をキーワード化した map）を正規化する。
  項目は OpenAPI 定義 https://www.zefix.admin.ch/ZefixPublicREST/v3/api-docs に従う
  （2026-11-23 取得、sha256 ff8e72380dcb0dbde910919154f5295051b0006a12513195afe7d8d110781450）。
  利用条件は同定義の license（OGD Open use. Must provide the source.、出典表示必須、商用可）。
  アクセスは zefix@bj.admin.ch への申請で発行される Basic 認証情報を使う（bj.admin.ch の案内）。
  個人事業（Einzelunternehmen、eCH-0097 の 0101）は取り込まない。役員・署名者は API に出ない。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Source: Federal Office of Justice (BJ/EHRA), Zefix, Open Government Data - free use with attribution. Normalized by Awai Network.")

(defn- blank->nil [s] (when (and (string? s) (not (str/blank? s))) (str/trim s)))

(defn- canon-uid [uid]
  (let [s (some-> uid str/trim)
        m (when s (re-matches #"CHE-?([0-9]{3})\.?([0-9]{3})\.?([0-9]{3})" s))]
    (when m (str "CHE-" (nth m 1) (nth m 2) (nth m 3)))))

(defn- dfie [x]
  (cond (string? x) (blank->nil x)
        (map? x) (or (blank->nil (:de x)) (blank->nil (:en x)) (blank->nil (:fr x))
                     (some blank->nil (vals x)))
        :else nil))

(defn- status [s]
  (case s "ACTIVE" :active "BEING_CANCELLED" :inactive "CANCELLED" :closed :unknown))

(defn normalize [m]
  (let [uid (canon-uid (:uid m))
        form (:legalForm m)
        form-uid (some-> form :uid str)]
    (if (= "0101" form-uid)
      {:skip "Einzelunternehmen（個人事業）"}
      (let [a (:address m)
            street (str/join " " (keep #(blank->nil (get a %)) [:street :houseNumber :addon]))
            r (cond-> {:entity/id (str "ch-zefix:" uid)
                       :entity/jurisdiction "CH"
                       :entity/name (blank->nil (:name m))
                       :entity/identifiers [{:scheme :ch-uid :value uid}]
                       :entity/status (status (:status m))
                       :source {:id :ch-zefix :attribution attribution :modified? true
                                :updated-on (blank->nil (:sogcDate m))}}
                form-uid (assoc :entity/legal-form {:scheme :ch-legal-form :code form-uid
                                                    :label (dfie (:name form))})
                (and (= "CANCELLED" (:status m)) (blank->nil (:deletionDate m)))
                (assoc :entity/closed-on (:deletionDate m))
                a (assoc :entity/address (cond-> {:country "CH"}
                                           (seq street) (assoc :street street)
                                           (blank->nil (:city a)) (assoc :locality (:city a))
                                           (blank->nil (:canton m)) (assoc :region (:canton m))
                                           (blank->nil (:swissZipCode a)) (assoc :postal-code (:swissZipCode a)))))
            ps (schema/problems r)]
        (if (seq ps) {:reject (str/join "; " ps)} {:record r})))))
