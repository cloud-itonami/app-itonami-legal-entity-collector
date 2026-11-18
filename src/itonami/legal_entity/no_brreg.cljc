(ns itonami.legal-entity.no-brreg
  "Brønnøysundregistrene Enhetsregisteret の enhet（JSON をキーワード化した map）を正規化する。
  項目名は API 文書の enhet の例に従う（https://data.brreg.no/enhetsregisteret/api/dokumentasjon/no/index.html 、2026-11-18 確認）。
  データは NLOD 2.0。enkeltpersonforetak（organisasjonsform ENK）は個人の事業なので取り込まない。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Inneholder data under Norsk lisens for offentlige data (NLOD) 2.0 tilgjengeliggjort av Brønnøysundregistrene. Normalized by Awai Network.")

(defn- blank->nil [s] (when (and (string? s) (not (str/blank? s))) s))

(defn normalize [e]
  (let [form (get-in e [:organisasjonsform :kode])
        a (:forretningsadresse e)]
    (if (= "ENK" form)
      {:skip "enkeltpersonforetak（個人事業）"}
      (let [r (cond-> {:entity/id (str "no-brreg:" (:organisasjonsnummer e))
                       :entity/jurisdiction "NO"
                       :entity/name (:navn e)
                       :entity/identifiers [{:scheme :no-orgnr :value (:organisasjonsnummer e)}]
                       :entity/status (cond (blank->nil (:slettedato e)) :closed
                                            (or (:konkurs e) (:underAvvikling e)) :inactive
                                            :else :active)
                       :source {:id :no-brreg :attribution attribution :modified? true}}
                form (assoc :entity/legal-form {:scheme :no-organisasjonsform :code form
                                                :label (get-in e [:organisasjonsform :beskrivelse])})
                a (assoc :entity/address (cond-> {:country (or (:landkode a) "NO")}
                                           (seq (:adresse a)) (assoc :street (str/join ", " (:adresse a)))
                                           (:poststed a) (assoc :locality (:poststed a))
                                           (:kommune a) (assoc :region (:kommune a))
                                           (:postnummer a) (assoc :postal-code (:postnummer a))))
                (blank->nil (:stiftelsesdato e)) (assoc :entity/registered-on (:stiftelsesdato e))
                (blank->nil (:slettedato e)) (assoc :entity/closed-on (:slettedato e)))
            ps (schema/problems r)]
        (if (seq ps) {:reject (str/join "; " ps)} {:record r})))))
