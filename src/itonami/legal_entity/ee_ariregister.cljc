(ns itonami.legal-entity.ee-ariregister
  "エストニア e-Äriregister の基本データ（lihtandmed）の CSV 1 行を正規化する。
  項目はダウンロードページ https://avaandmed.ariregister.rik.ee/en/downloading-open-data
  で取得したファイルの見出しに従う（2026-11-23 のスナップショット、zip 18708394 bytes、
  sha256 5672ca914ace30154d0503468ceee9190c4614560191ce5900c7fb3d55220453 で保存）。
  見出し 17 列：nimi;ariregistri_kood;ettevotja_oiguslik_vorm;ettevotja_oigusliku_vormi_alaliik;
  kmkr_nr;ettevotja_staatus;ettevotja_staatus_tekstina;ettevotja_esmakande_kpv;ettevotja_aadress;
  asukoht_ettevotja_aadressis;asukoha_ehak_kood;asukoha_ehak_tekstina;indeks_ettevotja_aadressis;
  ads_adr_id;ads_ads_oid;ads_normaliseeritud_taisaadress;teabesysteemi_link。
  ライセンスは CC BY 4.0（ダウンロードページの License information リンクで 2026-11-23 確認）。
  区切りは ;（csv/rows の区切りで 2 引数）。状態は R=登録済み、L=清算中、N=破産中
  （lihtandmed は削除済みを含まない）。破産中は :bankrupt とし、原コードは
  :entity/status-detail に残す（活動停止 :inactive と破産を混同しない）。
  個人事業（FIE）は取り込まない。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Source: e-Business Register (Registrite ja Infosüsteemi Amet, RIK), CC BY 4.0. Modified by Awai Network.")

(def ^:private nat-person-form "Füüsilisest isikust ettevõtja")

(defn- field [m k]
  (let [v (get m k)]
    (when (and (string? v) (not (str/blank? v))) (str/trim v))))

(defn- dd-mm-yyyy->iso [s]
  (when-let [m (when s (re-matches #"(\d{2})\.(\d{2})\.(\d{4})" s))]
    (str (nth m 3) "-" (nth m 2) "-" (nth m 1))))

(defn- street-of
  "ettevotja_aadress は住所全体（番地まで含む）。カンマを含まず末尾に番地を伴う
  「街路名 番地」の形だけ街路と切り分けて「街路名」を返す。それ以外の住所は
  住所全体を :address/full として保持し、:street は載せない。"
  [s]
  (when-let [m (when s (re-matches #"^([^,]+?)\s+(\d+(?:-\d+)?[A-Za-z]?)$" s))]
    (str/trim (nth m 1))))

(defn normalize [m]
  (let [code (field m "ariregistri_kood")
        form (field m "ettevotja_oiguslik_vorm")
        st (field m "ettevotja_staatus")]
    (if (= nat-person-form form)
      {:skip "füüsilisest isikust ettevõtja（個人事業主）"}
      (let [r (cond-> {:entity/id (str "ee-ariregister:" code)
                       :entity/jurisdiction "EE"
                       :entity/name (field m "nimi")
                       :entity/identifiers [{:scheme :ee-registry-code :value code}]
                       :entity/status (case st "R" :active "L" :inactive "N" :bankrupt :unknown)
                       :source {:id :ee-ariregister :attribution attribution :modified? true}}
                st (assoc :entity/status-detail st)
                form (assoc :entity/legal-form {:scheme :ee-legal-form :code form :label form})
                (dd-mm-yyyy->iso (field m "ettevotja_esmakande_kpv"))
                (assoc :entity/registered-on (dd-mm-yyyy->iso (field m "ettevotja_esmakande_kpv")))
                (field m "ettevotja_aadress") (assoc :entity/address
                                                       (cond-> {:country "EE"}
                                                         (field m "ettevotja_aadress") (assoc :full (field m "ettevotja_aadress"))
                                                         (street-of (field m "ettevotja_aadress")) (assoc :street (street-of (field m "ettevotja_aadress")))
                                                         (field m "asukoht_ettevotja_aadressis") (assoc :locality (field m "asukoht_ettevotja_aadressis"))
                                                         (field m "indeks_ettevotja_aadressis") (assoc :postal-code (field m "indeks_ettevotja_aadressis")))))
            ps (schema/problems r)]
        (if (seq ps) {:reject (str/join "; " ps)} {:record r})))))
