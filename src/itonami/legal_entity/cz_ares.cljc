(ns itonami.legal-entity.cz-ares
  "チェコ財務省 ARES の REST API（ekonomicke-subjekty、JSON をキーワード化した map）を正規化する。
  項目は OpenAPI 定義 https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/v3/api-docs に従う
  （2026-11-23 取得、sha256 822372bcc181740c1a0f43d0507c204780f575c7ddea837a65064a07a0819134）。EkonomickySubjekt の項目名は
  ico, obchodniJmeno, sidlo, pravniForma, datumVzniku, datumZaniku（EkonomickySubjektZaklad の properties）。
  運用条件は財務省の一次資料（https://data.mf.gov.cz/topics/ares 、
  https://mf.gov.cz/cs/ministerstvo/informacni-systemy/ares）で 2026-11-23 に確認。
  データは運用条件に従って無償で使える（利用登録・キー不要）。1 人あたり 1 分 500 件を超える
  照会や不正は遮断の理由になる。個人の事業者（fyzická osoba）は取り込まない。 "
  (:require [clojure.string :as str]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Source: Ministry of Finance of the Czech Republic, ARES (Administrative Register of Economic Subjects), used under the ARES operating conditions. Normalized by Awai Network.")

(def ^:private personal-forms
  "個人事業者（fyzická osoba を含む形態）。コード表 PravniForma
  （https://ares.gov.cz/.../ciselniky-nazevniky/vyhledat、kodCiselniku=PravniForma、2026-11-23 取得）。
  100-109 = Fyzická osoba podnikající 等、424 = Zahraniční fyzická osoba（外国人の自然人）、
  425 = その支店。000 = Zatím neurčeno（未判定）。いずれも初期スコープ外として取り込まない。"
  #{"100" "101" "102" "103" "104" "105" "106" "107" "108" "109" "424" "425" "000"})

(def ^:private form-labels
  "よく出るコードのラベル（チェコ語原文）。網羅でないので出たものに付与する。"
  {"111" "Veřejná obchodní společnost"
   "112" "Společnost s ručením omezeným"
   "113" "Společnost komanditní"
   "117" "Nadace"
   "118" "Nadační fond"
   "121" "Akciová společnost"
   "145" "Společenství vlastníků jednotek"
   "201" "Zemědělské družstvo"
   "205" "Družstvo"
   "701" "Spolek"
   "706" "Spolek"})

(defn- blank->nil [s] (when (and (string? s) (not (str/blank? s))) (str/trim s)))

(defn- any->str [v]
  (when (some? v)
    (let [s (str/trim (str v))]
      (when (seq s) s))))

(defn- postal [psc]
  (when-let [p (cond (integer? psc) (str psc)
                     (string? psc) (str/trim psc))]
    (when (seq p) p)))

(defn- address-map [a]
  (cond-> {:country (or (blank->nil (:kodStatu a)) "CZ")}
    (blank->nil (:nazevUlice a))
    (assoc :street
           (str/join " " (keep #(any->str (get a %))
                               [:nazevUlice :cisloDomovni :cisloOrientacni :cisloOrientacniPismeno])))
    (not (blank->nil (:nazevUlice a)))
    (assoc :street (blank->nil (:textovaAdresa a)))
    (blank->nil (:nazevObce a)) (assoc :locality (:nazevObce a))
    (blank->nil (:nazevKraje a)) (assoc :region (:nazevKraje a))
    (postal (:psc a)) (assoc :postal-code (postal (:psc a)))))

(defn normalize [m]
  (let [ico (blank->nil (:ico m))
        form (blank->nil (:pravniForma m))
        a (:sidlo m)]
    (if (personal-forms form)
      {:skip "fyzická osoba（個人事業主）"}
      (let [r (cond-> {:entity/id (str "cz-ares:" ico)
                       :entity/jurisdiction "CZ"
                       :entity/name (blank->nil (:obchodniJmeno m))
                       :entity/identifiers [{:scheme :cz-ico :value ico}]
                       :entity/status (if (blank->nil (:datumZaniku m)) :closed :active)
                       :source {:id :cz-ares :attribution attribution :modified? true
                                :updated-on (blank->nil (:datumAktualizace m))}}
                form (assoc :entity/legal-form {:scheme :cz-pravni-forma :code form
                                                :label (form-labels form)})
                (blank->nil (:datumVzniku m)) (assoc :entity/registered-on (:datumVzniku m))
                (blank->nil (:datumZaniku m)) (assoc :entity/closed-on (:datumZaniku m))
                a (assoc :entity/address (address-map a)))
            ps (schema/problems r)]
        (if (seq ps) {:reject (str/join "; " ps)} {:record r})))))
