(ns itonami.legal-entity.europe-test
  "欧州・英国のソース（AWAI-3224）。架空データだけ。番号はチェックデジットだけ正しい架空の値。"
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [itonami.legal-entity.collect :as collect]
            [itonami.legal-entity.csv :as csv]
            [itonami.legal-entity.cz-ares :as cz]
            [itonami.legal-entity.ee-ariregister :as ee]
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
    (is (ids/ee-registry-code? "99999999"))
    (is (every? ids/cz-ico? ["99900009" "99900017" "25596641"])))
  (testing "負の対照"
    (is (not (ids/fr-siren? "999900005")))
    (is (not (ids/no-orgnr? "999000003")))
    (is (not (ids/fi-business-id? "9990000-7")))
    (is (not (ids/fi-business-id? "99900008")) "ハイフンなし")
    (is (not (ids/gb-company-number? "zz000001")))
    (is (not (ids/gb-company-number? "ZZ00001")) "7 文字")
    (is (not (ids/ee-registry-code? "9999999")) "7 桁")
    (is (not (ids/cz-ico? "99900008")) "チェックデジット不一致")
    (is (not (ids/cz-ico? "9990000")) "7 桁")))

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

(deftest ee-empty-response
  "空のレスポンス（空文字列・空行のみ）では記録ができない。"
  (testing "空文字列は行を生まない（行数不足）"
    (is (= [] (vec (csv/rows [""] \;)))))
  (testing "空の map を normalize すると problems が拒否される"
    (let [r (ee/normalize {})]
      (is (string? (:reject r)) "空行は problems として検出される")
      (is (str/includes? (:reject r) "entity"))))
  (testing "見出しだけでは記録も拒否も生まない"
    (let [res (collect/collect catalog :ee-ariregister [ee-header])]
      (is (= [] (:records res)))
      (is (= [] (:rejected res)))))
  (testing "空行は collect で 1 件拒否（problems）になる"
    (let [res (collect/collect catalog :ee-ariregister [ee-header ""])]
      (is (= [] (:records res)))
      (is (= 1 (count (:rejected res))))
      (is (str/includes? (:reject (first (:rejected res))) "entity")))))

(deftest ee-column-mismatch
  "17 列でない行は列数不足として拒否される。"
  (is (= [{:reject "列数が 3（見出しは 17）"}]
         (vec (csv/rows [ee-header "a;b;c"] \;))))
  (let [res (collect/collect catalog :ee-ariregister [ee-header "a;b;c"])]
    (is (= [] (:records res)) "列数不一致の行は記録にならない")
    (is (= 1 (count (:rejected res))))) )

(deftest ee-bom-csv
  "BOM 付き CSV は最初の列名に BOM が混入せず正常にパースされる。"
  (let [rows (csv/rows [(str (char 0xFEFF) ee-header) (first (rest ee-lines))] \;)]
    (is (= 1 (count rows)))
    (is (= "FICTIVE HOLDING OÜ" (get (first rows) "nimi")) "BOM が列名に残らない"))
  (let [res (collect/collect catalog :ee-ariregister [(str (char 0xFEFF) ee-header) (first (rest ee-lines))])]
    (is (= ["ee-ariregister:99999999"] (map :entity/id (:records res))))))

(deftest ee-semicolon-rows
  "; 区切りの正例を csv/rows のレベルで確かめる（見出し 17 列に拆解される）。"
  (let [rows (csv/rows [ee-header (first (rest ee-lines))] \;)]
    (is (= 1 (count rows)))
    (let [m (first rows)]
      (is (= 17 (count (keys m))))
      (is (= "FICTIVE HOLDING OÜ" (get m "nimi")))
      (is (= "99999999" (get m "ariregistri_kood")))
      (is (= "Osaühing" (get m "ettevotja_oiguslik_vorm"))))))

;; --- CZ -------------------------------------------------------------------

;; 項目名は 2026-11-23 に取得した OpenAPI 定義（v1.4.0）の EconomickySubjekt / Adresa どおり。
;; 番号はチェックデジットだけ正しい架空の値（99900009 等）。実在の登記データは使わない。

(def cz-subjekty
  [{:ico "99900009" :obchodniJmeno "FIKČNÍ S.R.O." :pravniForma "112"
    :datumVzniku "2010-03-04" :datumAktualizace "2026-11-20"
    :sidlo {:kodStatu "CZ" :nazevUlice "Fikční ulice" :cisloDomovni 123
            :cisloOrientacni 4 :cisloOrientacniPismeno "a" :nazevObce "Testov"
            :psc 17000 :textovaAdresa "Fikční ulice 123/4a, Testov"}}
   {:ico "99900017" :obchodniJmeno "FIKČNÍ K.S." :pravniForma "113"
    :datumVzniku "2001-02-03" :datumZaniku "2026-10-31" :datumAktualizace "2026-10-31"}
   {:ico "99900025" :obchodniJmeno "FIKČNÍ ŽIVNOSTNÍK" :pravniForma "101"
    :datumVzniku "2015-01-01"}
   {:ico "99900008" :obchodniJmeno "BAD ICO SPOL. S.R.O." :pravniForma "112"}
   {:obchodniJmeno "NO ICO ID SPOL. S.R.O." :icoId "ARES_99900041" :pravniForma "112"}
   {:icoId "99900050" :obchodniJmeno "FIKČNÍ ICOID SPOL. S.R.O." :pravniForma "112"
    :datumVzniku "2020-05-06"}])

(deftest cz-ares
  (let [res (collect/collect catalog :cz-ares cz-subjekty)
        [a b f] (:records res)]
    (is (= ["cz-ares:99900009" "cz-ares:99900017" "cz-ares:99900050"]
           (map :entity/id (:records res))))
    (is (every? schema/valid? (:records res)))
    (is (= [:active "2010-03-04" "2026-11-20" {:scheme :cz-pravni-forma :code "112"}
            {:country "CZ" :street "Fikční ulice 123/4a" :locality "Testov"
             :postal-code "17000" :full "Fikční ulice 123/4a, Testov"}]
           [(:entity/status a) (:entity/registered-on a) (get-in a [:source :updated-on])
            (:entity/legal-form a) (:entity/address a)]))
    (is (= [:closed "2026-10-31" nil]
           [(:entity/status b) (:entity/closed-on b) (:entity/address b)]))
    (testing "ico が無く icoId（8桁）のみは ico として受け付ける"
      (is (= ["cz-ares:99900050" :active] [(:entity/id f) (:entity/status f)])))
    (testing "個人事業主は取り込まない"
      (is (= [{:index 2 :skip "fyzická osoba podnikající（個人事業主）"}] (:skipped res))))
    (testing "負の対照: チェックデジット不一致、IČO 未割当の icoId"
      (is (= [3 4] (map :index (:rejected res)))))))

(defn- simulate
  "失敗応答の並びを流し、retry-wait-ms の決定 [{:status :wait-ms|:done?}] を返す。"
  [statuses]
  (loop [att 1 [s & rs] statuses acc []]
    (if (nil? s) acc
      (let [w (cz/retry-wait-ms {:status s :attempt att})]
        (if (nil? w)
          (conj acc {:status s :done? true})
          (recur (inc att) rs (conj acc {:status s :wait-ms w})))))))

(deftest cz-ares-retry
  "リトライとレート制限（cz_ares.cljc の取得ポリシー、純粋関数）。時刻は注入する。"
  (testing "① 429 → 再試行 → 成功"
    (is (= [{:status 429 :wait-ms 1000} {:status 200 :done? true}] (simulate [429 200]))))
  (testing "② 5xx は指数バックオフで上限（再試行 4 回）まで再試行して止まる"
    (is (= [{:status 500 :wait-ms 1000} {:status 502 :wait-ms 2000} {:status 503 :wait-ms 4000}
            {:status 599 :wait-ms 8000} {:status 503 :done? true}]
           (simulate [500 502 503 599 503])))
    (is (= 8000 (cz/retry-wait-ms {:status 429 :attempt 4})))
    (is (nil? (cz/retry-wait-ms {:status 503 :attempt 5})) "上限で止まる"))
  (testing "③ バックオフの総待機は 30 秒を超えない"
    (let [ws (keep :wait-ms (simulate [500 502 503 599 503]))]
      (is (= 15000 (apply + ws)))
      (is (<= (apply + ws) cz/max-total-wait-ms))))
  (testing "④ 500 件までは待たず、501 件目は窓の外まで待つ。バーストでも 60 秒窓に 500 件を超えない"
    (is (= 0 (cz/throttle-wait-ms 0 (repeat 499 0))))
    (is (= 60000 (cz/throttle-wait-ms 0 (repeat 500 0))))
    (is (= 0 (cz/throttle-wait-ms 60000 (repeat 500 0))) "窓の外（境界）の呼び出しは数えない")
    (let [times (loop [now 0 times [] i 0]
                  (if (>= i 600) times
                    (let [now (+ now (cz/throttle-wait-ms now times))]
                      (recur now (conj times now) (inc i)))))]
      (is (= 60000 (last times)))
      (is (every? (fn [i]
                    (let [t (nth times i)]
                      (<= (count (filter #(< % (+ t cz/rate-window-ms)) (drop i times)))
                          cz/max-requests-per-minute)))
                  (range (count times)))))
    (is (= 60000 (cz/next-wait-ms {:status 429 :attempt 1 :now-ms 0 :call-times (vec (repeat 500 0))})))
    (is (= 1000 (cz/next-wait-ms {:status 429 :attempt 1 :now-ms 0 :call-times []}))))
  (testing "⑤ 429 以外の 4xx は再試行しない"
    (is (every? cz/retryable-status? [429 500 502 599]))
    (is (not-any? cz/retryable-status? [200 400 401 404 428 600 nil]))
    (is (nil? (cz/retry-wait-ms {:status 404 :attempt 1})))
    (is (nil? (cz/next-wait-ms {:status 404 :attempt 1 :now-ms 0 :call-times []})))))

;; リトライ実行関数（request-with-retry）。時刻・sleep は注入し、実時間では眠らない。
;; fake clock は sleep の分だけ進む。ステータスは架空のもののみ。

(defn- run-request
  "statuses を順に返す fake request-fn（無くなったら最後を繰り返す）で cz/request-with-retry を実行し、
  {:result r :sleeps [...] :calls n} を返す。実時間では眠らない。"
  [statuses body]
  (let [clock (atom 0) sleeps (atom []) calls (atom 0)
        rest (atom (seq statuses))]
    (let [result (cz/request-with-retry
                  (fn [] (swap! calls inc)
                        (let [s (first @rest)]
                          (when (next @rest) (swap! rest next))
                          {:status s :body body}))
                  (fn [ms] (swap! sleeps conj ms) (swap! clock + ms))
                  (fn [] @clock))]
      {:result result :sleeps @sleeps :calls @calls})))

(deftest cz-ares-retry-executor
  "request-with-retry（429 と 5xx のみ、1s-2s-4s-8s、上限 4 回、総待機 30 秒、120ms 間隔）。時刻は fake。 "
  (testing "① 429 → 再試行 → 成功"
    (let [{:keys [result sleeps calls]} (run-request [429 200] {:ok true})]
      (is (= {:ok true} result) "成功時は body をそのまま返す")
      (is (= [1000] sleeps))
      (is (= 2 calls)))
    (let [{:keys [result sleeps calls]} (run-request [503 200] {:ok true})]
      (is (= {:ok true} result) "5xx も再試行する")
      (is (= [1000] sleeps))
      (is (= 2 calls))))
  (testing "② 上限で停止（初発 + 再試行 4 回 = 5 試行）"
    (let [{:keys [result sleeps calls]} (run-request [429] nil)]
      (is (= [1000 2000 4000 8000] sleeps))
      (is (= 5 calls))
      (is (= {:status 429 :attempts 5 :waited-ms 15000} result))))
  (testing "③ 総待機が 30 秒を超えない"
    (let [{:keys [result sleeps]} (run-request [500 502 503 599 503] nil)]
      (is (<= (reduce + 0 sleeps) cz/max-total-wait-ms))
      (is (<= (:waited-ms result) cz/max-total-wait-ms))))
  (testing "④ レート遵守：同一 request-fn への試行間隔が 120ms 未満にならない"
    (is (= 120 cz/min-interval-ms))
    (is (= 70 (cz/interval-wait-ms 0 50)) "不足分を sleep で補う計算")
    (is (= 0 (cz/interval-wait-ms 0 120)) "境界は待たない")
    (is (= 0 (cz/interval-wait-ms 0 200)) "間隔が十分なら待たない")
    (is (= 0 (cz/interval-wait-ms nil 0)) "初回は待たない")
    (let [clock (atom 0) sleeps (atom []) times (atom []) n (atom 0)
          req (fn [] (swap! times conj @clock)
                    {:status (if (= 1 (swap! n inc)) 429 200) :body {:ok true}})
          body (cz/request-with-retry req
                                      (fn [ms] (swap! sleeps conj ms) (swap! clock + ms))
                                      (fn [] @clock))]
      (is (= {:ok true} body))
      (is (= [1000] @sleeps) "sleep で間隔が確保された")
      (is (every? #(<= 120 %) (map - (rest @times) @times)) "試行時刻の差が 120ms 以上")))
  (testing "⑤ 429 以外の 4xx は再試行しない"
    (doseq [s [400 403 404]]
      (let [{:keys [result sleeps calls]} (run-request [s] nil)]
        (is (= 1 calls) (str s))
        (is (= [] sleeps) (str s))
        (is (= {:status s :attempts 1 :waited-ms 0} result) (str s))))))

(deftest cz-ares-rate-window
  "trailing 60 秒で 500 件を超えない（fake clock は sleep の分だけ進み、送信回数を数える）。"
  (let [state (cz/new-request-state)
        clock (atom 0) sleeps (atom []) times (atom [])
        req (fn [] (swap! times conj @clock) {:status 200 :body {:ok true}})
        sleep (fn [ms] (swap! sleeps conj ms) (swap! clock + ms))
        now (fn [] @clock)]
    (dotimes [_ 501]
      (cz/request-with-retry req sleep now {:state state}))
    (is (= 501 (count @times)) "501 件すべて送る（止めずに待つ）")
    (is (= [60000] @sleeps) "500 件で window が満杯になり、古い試行が窓から外れるまで待つ")
    (is (= 60000 (last @times)) "501 件目は窓の外で送られる")
    (is (every? (fn [i]
                  (let [t (nth @times i)]
                    (<= (count (filter #(< % (+ t cz/rate-window-ms)) (drop i @times)))
                        cz/max-requests-per-minute)))
                (range (count @times)))
        "いずれの 60 秒窓でも 500 件を超えない")))

(deftest cz-ares-dedupe
  "同一照会の反復をしない（2回目は request-fn を呼ばずキャッシュを返す）。"
  (let [state (cz/new-request-state)
        clock (atom 0) sleeps (atom []) calls (atom 0)
        req (fn [] (swap! calls inc) {:status 200 :body {:ok true}})
        sleep (fn [ms] (swap! sleeps conj ms) (swap! clock + ms))
        now (fn [] @clock)
        r1 (cz/request-with-retry req sleep now {:state state :request-key "ico:99900009"})
        r2 (cz/request-with-retry req sleep now {:state state :request-key "ico:99900009"})
        r3 (cz/request-with-retry req sleep now {:state state :request-key "ico:99900017"})]
    (is (= {:ok true} r1))
    (is (= r1 r2) "2回目はキャッシュを返す")
    (is (= 2 @calls) "2回目は送らず、別の対象は送る")
    (is (= [] @sleeps) "レート待機なし")
    (is (= 2 (count (:call-times @state))) "キャッシュヒットは送信時刻に数えない")))

(deftest cz-ares-dedupe-after-retry
  "再試行を伴った同一照会の反復もしない（初回 429→再試行→成功の後、同じ IČO は送らない）。"
  (let [state (cz/new-request-state)
        clock (atom 0) sleeps (atom []) calls (atom 0)
        req (fn [] (swap! calls inc) {:status (if (= 1 @calls) 429 200) :body {:ok true}})
        sleep (fn [ms] (swap! sleeps conj ms) (swap! clock + ms))
        now (fn [] @clock)
        r1 (cz/request-with-retry req sleep now {:state state :request-key "ico:99900009"})
        calls-after-first @calls
        r2 (cz/request-with-retry req sleep now {:state state :request-key "ico:99900009"})
        calls-after-second @calls
        r3 (cz/request-with-retry req sleep now {:state state :request-key "ico:99900017"})]
    (is (= {:ok true} r1) "初回は 429 の再試行の後に成功する")
    (is (= 2 calls-after-first) "初回は初発と再試行で 2 回送る")
    (is (= [1000] @sleeps) "再試行の待機は 1s")
    (is (= r1 r2) "2回目はキャッシュを返す")
    (is (= calls-after-first calls-after-second) "同じ IČO を 2 回続けて照会しても 2 回目は送らない")
    (is (= 3 @calls) "別の IČO は送る")))

;; --- カタログ -------------------------------------------------------------

(deftest europe-catalog
  (let [by-id (into {} (map (juxt :id identity) catalog))]
    (testing "利用条件と仕様を確認した 6 ソースだけ実装済み"
      (doseq [id [:gb-companies-house :fr-sirene :no-brreg :fi-prh :ee-ariregister :cz-ares]]
        (is (= :implemented (get-in by-id [id :status])) (name id))
        (is (every? (by-id id) [:license :url :terms-verified-on :parser]) (name id))))
    (testing "確認できなかったソースは受け付けない"
      (doseq [id [:dk-cvr :be-kbo :ch-zefix :nl-kvk]]
        (is (not= :implemented (get-in by-id [id :status])) (name id))
        (is (string? (get-in by-id [id :blocked-reason])) (name id))
        (is (:error (collect/collect catalog id [])) (name id))))))

(deftest csv-rows
  (is (= [{"a" "1" "b" "2"} {:reject "列数が 1（見出しは 2）"}] (csv/rows ["a, b" "1,2" "3"])))
  (is (= [] (csv/rows ["a,b"])))
  (testing "; 区切りと BOM"
    (is (= [{"a" "1" "b" "2"}] (csv/rows [(str (char 0xFEFF) "a;b") "1;2"] \;)))))
