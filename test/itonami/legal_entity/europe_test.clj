(ns itonami.legal-entity.europe-test
  "欧州・英国のソース（AWAI-3224）。架空データだけ。番号はチェックデジットだけ正しい架空の値。"
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [itonami.legal-entity.collect :as collect]
            [itonami.legal-entity.csv :as csv]
            [itonami.legal-entity.fi-prh :as fi]
            [itonami.legal-entity.fr-sirene :as fr]
            [itonami.legal-entity.gb-companies-house :as gb]
            [itonami.legal-entity.ids :as ids]
            [itonami.legal-entity.no-brreg :as no]
            [itonami.legal-entity.schema :as schema]))

(def catalog (edn/read-string (slurp "sources/catalog.edn")))

(defn- q [& fields] (str/join "," (map #(str "\"" % "\"") fields)))

;; --- 識別子 ---------------------------------------------------------------

(deftest identifiers
  (testing "PRH の OpenAPI 定義にある例で検算"
    (is (ids/fi-business-id? "0116297-6")))
  (testing "架空の値"
    (is (every? ids/fr-siren? ["999900004" "999900012" "999900020"]))
    (is (every? ids/no-orgnr? ["999000002" "999000010" "999000029"]))
    (is (every? ids/fi-business-id? ["9990000-8" "9990001-6" "9990002-4"]))
    (is (ids/gb-company-number? "ZZ000001"))
    (is (ids/ee-registry-code? "99999999")))
  (testing "負の対照"
    (is (not (ids/fr-siren? "999900005")))
    (is (not (ids/no-orgnr? "999000003")))
    (is (not (ids/fi-business-id? "9990000-7")))
    (is (not (ids/fi-business-id? "99900008")) "ハイフンなし")
    (is (not (ids/gb-company-number? "zz000001")))
    (is (not (ids/gb-company-number? "ZZ00001")) "7 文字")
    (is (not (ids/ee-registry-code? "9999999")) "7 桁")))

;; --- GB -------------------------------------------------------------------

(def gb-header
  "見出しの綴りは仕様書にないため、前後に空白がある形と RegAddress. 付きの形で試す。"
  (q "CompanyName" " CompanyNumber" "RegAddress.AddressLine1" " RegAddress.AddressLine2" "RegAddress.PostTown"
     "RegAddress.County" "RegAddress.PostCode" "CompanyCategory" "CompanyStatus" "DissolutionDate" "IncorporationDate"))

(def gb-lines
  [gb-header
   (q "FICTIONAL WIDGETS LIMITED" "ZZ000001" "1 Fake Street" "Nowhere" "TESTTOWN" "" "ZZ1 1ZZ"
      "Private Limited Company" "Active" "" "02/01/2020")
   (q "FICTIONAL OLD LTD" "ZZ000002" "" "" "" "" "" "Private Limited Company" "Dissolved" "31/10/2026" "2001-05-06")
   (q "FICTIONAL ODD DATE LTD" "ZZ000003" "" "" "" "" "" "PLC" "Liquidation" "" "2020/01/02")
   (q "BAD NUMBER LTD" "zz-1" "" "" "" "" "" "" "Active" "" "")
   (q "" "ZZ000004" "" "" "" "" "" "" "Active" "" "")
   (q "TOO FEW" "ZZ000005")])

(deftest gb-companies-house
  (let [res (collect/collect catalog :gb-companies-house gb-lines)
        [a b c] (:records res)]
    (is (= ["gb-companies-house:ZZ000001" "gb-companies-house:ZZ000002" "gb-companies-house:ZZ000003"]
           (map :entity/id (:records res))))
    (is (every? schema/valid? (:records res)))
    (is (= [:active "2020-01-02" "1 Fake Street, Nowhere" "TESTTOWN"]
           [(:entity/status a) (:entity/registered-on a) (get-in a [:entity/address :street]) (get-in a [:entity/address :locality])]))
    (is (= [:closed "2026-10-31" "2001-05-06"] [(:entity/status b) (:entity/closed-on b) (:entity/registered-on b)]))
    (testing "読めない日付は載せない"
      (is (= :inactive (:entity/status c)))
      (is (nil? (:entity/registered-on c))))
    (testing "負の対照: 不正な会社番号・空の名称・列数違い"
      (is (= 3 (count (:rejected res)))))))

(deftest gb-header-without-prefix
  (let [lines [(q "CompanyName" "CompanyNumber" "PostTown" "CompanyStatus")
               (q "FICTIONAL PLAIN LTD" "ZZ000006" "TESTTOWN" "Active - Proposal to Strike off")]
        r (first (:records (collect/collect catalog :gb-companies-house lines)))]
    (is (= ["TESTTOWN" :active] [(get-in r [:entity/address :locality]) (:entity/status r)]))))

;; --- FR -------------------------------------------------------------------

(def fr-header
  (q "siren" "statutDiffusionUniteLegale" "unitePurgeeUniteLegale" "dateCreationUniteLegale" "sigleUniteLegale"
     "prenom1UniteLegale" "nomUniteLegale" "denominationUniteLegale" "categorieJuridiqueUniteLegale"
     "etatAdministratifUniteLegale" "dateDernierTraitementUniteLegale"))

(def fr-lines
  [fr-header
   (q "999900012" "O" "" "2010-03-04" "FSA" "" "" "SOCIETE FICTIVE SA" "5599" "A" "2026-10-01T10:11:12.000")
   (q "999900004" "O" "" "1990-01-01" "" "" "" "SARL FICTIVE FERMEE" "5499" "C" "2026-09-01T00:00:00.000")
   (q "999900020" "O" "" "2015-01-01" "" "JEAN" "FICTIF" "" "1000" "A" "2026-10-01T00:00:00.000")
   (q "999900038" "P" "" "2015-01-01" "" "" "" "ASSOCIATION PARTIELLE" "9220" "A" "2026-10-01T00:00:00.000")
   (q "999900005" "O" "" "2015-01-01" "" "" "" "SIREN INVALIDE" "5710" "A" "2026-10-01T00:00:00.000")])

(deftest fr-sirene
  (let [res (collect/collect catalog :fr-sirene fr-lines)
        [a b] (:records res)]
    (is (= ["fr-sirene:999900012" "fr-sirene:999900004"] (map :entity/id (:records res))))
    (is (every? schema/valid? (:records res)))
    (is (= [:active "2010-03-04" "FSA" "2026-10-01" {:scheme :fr-categorie-juridique :code "5599"}]
           [(:entity/status a) (:entity/registered-on a) (:entity/short-name a)
            (get-in a [:source :updated-on]) (:entity/legal-form a)]))
    (is (= :closed (:entity/status b)))
    (testing "個人の情報は取り込まない"
      (is (= ["personne physique（個人事業主）" "diffusion partielle（部分公開）"] (map :skip (:skipped res))))
      (is (not-any? #(str/includes? (pr-str %) "JEAN") (:records res))))
    (testing "負の対照: Luhn 不一致"
      (is (= 1 (count (:rejected res)))))))

;; --- NO -------------------------------------------------------------------

(def no-enheter
  [{:organisasjonsnummer "999000002" :navn "FIKTIV HOLDING AS"
    :organisasjonsform {:kode "AS" :beskrivelse "Aksjeselskap"}
    :forretningsadresse {:land "Norge" :landkode "NO" :postnummer "0000" :poststed "OSLO"
                         :adresse ["Fiktivgata 1"] :kommune "OSLO" :kommunenummer "0301"}
    :stiftelsesdato "2001-02-03" :registreringsdatoEnhetsregisteret "2001-02-10"
    :konkurs false :underAvvikling false}
   {:organisasjonsnummer "999000010" :navn "FIKTIV KONKURS AS" :organisasjonsform {:kode "AS"}
    :konkurs true :underAvvikling false}
   {:organisasjonsnummer "999000029" :navn "OLA FIKTIV" :organisasjonsform {:kode "ENK"}}
   {:organisasjonsnummer "999000003" :navn "FEIL NUMMER AS" :organisasjonsform {:kode "AS"}}])

(deftest no-brreg
  (let [res (collect/collect catalog :no-brreg no-enheter)
        [a b] (:records res)]
    (is (= ["no-brreg:999000002" "no-brreg:999000010"] (map :entity/id (:records res))))
    (is (every? schema/valid? (:records res)))
    (is (= [:active "2001-02-03" "OSLO" "Fiktivgata 1"]
           [(:entity/status a) (:entity/registered-on a) (get-in a [:entity/address :locality]) (get-in a [:entity/address :street])]))
    (is (= :inactive (:entity/status b)))
    (is (= [{:index 2 :skip "enkeltpersonforetak（個人事業）"}] (:skipped res)))
    (testing "負の対照: MOD 11 不一致"
      (is (= [3] (map :index (:rejected res)))))))

;; --- FI -------------------------------------------------------------------

(defn- fi-company [bid form & {:as more}]
  (merge {:businessId {:value bid :source "3"}
          :names [{:name "Fiktiivinen Oy" :type "1" :version 1 :source "1"}
                  {:name "Vanha Nimi Oy" :type "1" :version 2 :endDate "2015-01-01" :source "1"}]
          :companyForms (when form [{:type form :version 1 :source "1"}])
          :addresses [{:type 1 :street "Kuvitteellinen katu" :buildingNumber "1" :postCode "00000"
                       :postOffices [{:city "HELSINKI" :languageCode "1"}] :source "0"}]
          :registeredEntries []
          :tradeRegisterStatus "1"
          :status "2"
          :registrationDate "2005-06-07"
          :lastModified "2026-11-01T12:00:00"}
         more))

(deftest fi-prh
  (let [res (collect/collect catalog :fi-prh
                             [(fi-company "9990000-8" "16")
                              (fi-company "9990001-6" "OY" :status "5" :endDate "2026-10-31")
                              (fi-company "9990002-4" nil)
                              (fi-company "9990003-2" "99")
                              (fi-company "9990000-7" "16")])
        [a b] (:records res)]
    (is (= ["fi-prh:9990000-8" "fi-prh:9990001-6"] (map :entity/id (:records res))))
    (is (every? schema/valid? (:records res)))
    (is (= ["Fiktiivinen Oy" :active "Limited company" "Kuvitteellinen katu 1" "HELSINKI"]
           [(:entity/name a) (:entity/status a) (get-in a [:entity/legal-form :label])
            (get-in a [:entity/address :street]) (get-in a [:entity/address :locality])]))
    (is (= [:closed "2026-10-31"] [(:entity/status b) (:entity/closed-on b)]))
    (testing "会社形態がない・一覧にないものは取り込まない"
      (is (= [2 3] (map :index (:skipped res)))))
    (testing "負の対照: チェックデジット不一致"
      (is (= [4] (map :index (:rejected res)))))))

;; --- EE -------------------------------------------------------------------
;; 17 列の見出しは 2026-11-23 に取得した lihtandmed.csv.zip の先頭行どおり。
;; 区切りは ;（csv/rows の 2 引数版）。架空データだけ。

(def ee-header
  (str/join ";" ["nimi" "ariregistri_kood" "ettevotja_oiguslik_vorm" "ettevotja_oigusliku_vormi_alaliik"
                 "kmkr_nr" "ettevotja_staatus" "ettevotja_staatus_tekstina" "ettevotja_esmakande_kpv"
                 "ettevotja_aadress" "asukoht_ettevotja_aadressis" "asukoha_ehak_kood" "asukoha_ehak_tekstina"
                 "indeks_ettevotja_aadressis" "ads_adr_id" "ads_ads_oid" "ads_normaliseeritud_taisaadress"
                 "teabesysteemi_link"]))

(defn- ee [& xs] (str/join ";" xs))

(def ee-lines
  [ee-header
   (ee "FICTIVE HOLDING OÜ" "99999999" "Osaühing" "" "" "R" "Registrisse kantud" "05.06.2023"
       "Regati pst 1" "TEST LINNA, Tallinn, Harju maakond" "0596" "Tallinn" "11911" "1" "2"
       "Harju maakond, Tallinn, TEST LINNA, Regati pst 1" "https://example.test/99999999")
   (ee "FICTIVE LIKVIDEERIMISEL AS" "99999998" "Aktsiaselts" "" "" "L" "Likvideerimisel" "01.02.2010"
       "" "Tartu linn, Tartu linn, Tartu maakond" "8151" "Tartu" "51003" "" "" "" "https://example.test/99999998")
   (ee "VANA FIKTIIV" "99999997" "Täisühing" "" "" "N" "Pankrotis" "03.03.2003"
       "" "" "" "" "" "" "" "" "https://example.test/99999997")
   (ee "FIKTIIV FIE" "99999996" "Füüsilisest isikust ettevõtja" "" "" "R" "Registrisse kantud" "04.04.2004"
       "" "" "" "" "" "" "" "" "https://example.test/99999996")
   (ee "BAD CODE OÜ" "123456" "Osaühing" "" "" "R" "Registrisse kantud" "04.04.2004"
       "" "" "" "" "" "" "" "" "https://example.test/123456")
   (ee "FICTIVE KOMMA OÜ" "99999995" "Osaühing" "" "" "R" "Registrisse kantud" "04.04.2004"
       "Tallinn, Regati pst 1" "TEST LINNA, Tallinn, Harju maakond" "0596" "Tallinn" "11911" "" ""
       "" "https://example.test/99999995")])

(deftest ee-ariregister
  (let [res (collect/collect catalog :ee-ariregister ee-lines)
        [a b c d] (:records res)]
    (is (= ["ee-ariregister:99999999" "ee-ariregister:99999998" "ee-ariregister:99999997"
            "ee-ariregister:99999995"]
           (map :entity/id (:records res))))
    (is (every? schema/valid? (:records res)))
    (is (= [:active "R" "2023-06-05" "Regati pst" "Regati pst 1" "11911" {:scheme :ee-legal-form :code "Osaühing" :label "Osaühing"}]
           [(:entity/status a) (:entity/status-detail a) (:entity/registered-on a)
            (get-in a [:entity/address :street]) (get-in a [:entity/address :full])
            (get-in a [:entity/address :postal-code]) (:entity/legal-form a)]))
    (is (= [:inactive "L" :bankrupt "N"]
           [(:entity/status b) (:entity/status-detail b) (:entity/status c) (:entity/status-detail c)]))
    (testing "住所全体は :full に保持し、分割できない住所には :street を載せない"
      (is ((fnil = nil) (get-in d [:entity/address :street])))
      (is (= "Tallinn, Regati pst 1" (get-in d [:entity/address :full]))))
    (testing "個人事業主は取り込まない"
      (is (= [{:index 3 :skip "füüsilisest isikust ettevõtja（個人事業主）"}] (:skipped res))))
    (testing "負の対照: 6 桁コードは拒否"
      (is (= [4] (map :index (:rejected res)))))))

;; --- カタログ -------------------------------------------------------------

(deftest europe-catalog
  (let [by-id (into {} (map (juxt :id identity) catalog))]
    (testing "利用条件と仕様を確認した 5 ソースだけ実装済み"
      (doseq [id [:gb-companies-house :fr-sirene :no-brreg :fi-prh :ee-ariregister]]
        (is (= :implemented (get-in by-id [id :status])) (name id))
        (is (every? (by-id id) [:license :url :terms-verified-on :parser]) (name id))))
    (testing "確認できなかったソースは受け付けない"
      (doseq [id [:dk-cvr :be-kbo :cz-ares :ch-zefix :nl-kvk]]
        (is (not= :implemented (get-in by-id [id :status])) (name id))
        (is (string? (get-in by-id [id :blocked-reason])) (name id))
        (is (:error (collect/collect catalog id [])) (name id))))))

(deftest csv-rows
  (is (= [{"a" "1" "b" "2"} {:reject "列数が 1（見出しは 2）"}] (csv/rows ["a, b" "1,2" "3"])))
  (is (= [] (csv/rows ["a,b"])))
  (testing "; 区切りと BOM"
    (is (= [{"a" "1" "b" "2"}] (csv/rows [(str (char 0xFEFF) "a;b") "1;2"] \;)))))
