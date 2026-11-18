(ns itonami.legal-entity.fr-sirene
  "INSEE Sirene の StockUniteLegale（CSV、先頭行が見出し）の 1 行を正規化する。
  見出しは「liste-csv-des-variables-du-dessin-du-fichier-stockunitelegale-311.csv」に従う。
  https://static.data.gouv.fr/resources/base-sirene-des-entreprises-et-de-leurs-etablissements-siren-siret/20260113-143438/stockunitelegale-311-dessin-de-fichier.csv
  （2026-11-18 取得、sha256 d96b3d9a6fbb4d4275571ba54aceb5dfef7cfc85bb0e07e023af4f610a023e89）

  個人の情報を取り込まないため、次の行は除外する。
  - categorieJuridiqueUniteLegale が 1 で始まる（personne physique、個人事業主）
  - statutDiffusionUniteLegale が P（本人の申し出による部分公開）
  住所は StockEtablissement 側にあるため、この parser では載せない。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Source: INSEE, base Sirene des entreprises et de leurs établissements (Licence Ouverte 2.0). Modified by Awai Network.")

(defn- blank->nil [s] (when-not (str/blank? s) (str/trim s)))

(defn normalize [m]
  (if (:reject m)
    m
    (let [siren (blank->nil (get m "siren"))
          cj (blank->nil (get m "categorieJuridiqueUniteLegale"))
          etat (blank->nil (get m "etatAdministratifUniteLegale"))]
      (cond
        (and cj (str/starts-with? cj "1")) {:skip "personne physique（個人事業主）"}
        (= "P" (blank->nil (get m "statutDiffusionUniteLegale"))) {:skip "diffusion partielle（部分公開）"}
        :else
        (let [r (cond-> {:entity/id (str "fr-sirene:" siren)
                         :entity/jurisdiction "FR"
                         :entity/name (blank->nil (get m "denominationUniteLegale"))
                         :entity/identifiers [{:scheme :fr-siren :value siren}]
                         :entity/status (case etat "A" :active "C" :closed :unknown)
                         :entity/address {:country "FR"}
                         :source {:id :fr-sirene
                                  :updated-on (some-> (blank->nil (get m "dateDernierTraitementUniteLegale")) (subs 0 10))
                                  :attribution attribution
                                  :modified? true}}
                  cj (assoc :entity/legal-form {:scheme :fr-categorie-juridique :code cj})
                  (blank->nil (get m "sigleUniteLegale")) (assoc :entity/short-name (blank->nil (get m "sigleUniteLegale")))
                  (blank->nil (get m "dateCreationUniteLegale")) (assoc :entity/registered-on (blank->nil (get m "dateCreationUniteLegale"))))
              ps (schema/problems r)]
          (if (seq ps) {:reject (str/join "; " ps)} {:record r}))))))
