(ns itonami.legal-entity.americas-test
  "米州・その他のソース（AWAI-3225）。架空データだけ。番号は検査数字だけ正しい架空の値。"
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [itonami.legal-entity.br-cnpj :as br-cnpj]
            [itonami.legal-entity.collect :as collect]
            [itonami.legal-entity.ids :as ids]
            [itonami.legal-entity.schema :as schema]))

(def catalog (edn/read-string (slurp "sources/catalog.edn")))

(defn- q [& fields] (str/join "," (map #(str "\"" % "\"") fields)))

;; --- 識別子 ---------------------------------------------------------------

(deftest identifiers
  (testing "公開されている例で検算（ATO の ABN、ASIC の ACN の例）"
    (is (ids/au-abn? "51824753556"))
    (is (ids/au-acn? "000000019"))
    (is (ids/au-acn? "010499966")))
  (testing "架空の値"
    (is (every? ids/au-abn? ["99900000180" "99900000261" "99900000293"]))
    (is (every? ids/au-acn? ["999000001" "999000010"]))
    (is (ids/us-sec-cik? "0009990001"))
    (is (every? ids/ca-corporation-number? ["9990001" "999000-2"]))
    (is (ids/ca-business-number? "999999998")))
  (testing "負の対照"
    (is (not (ids/au-abn? "99900000181")))
    (is (not (ids/au-acn? "999000002")))
    (is (not (ids/us-sec-cik? "9990001")) "0 埋めしていない")
    (is (not (ids/ca-corporation-number? "99A0001")))
    (is (not (ids/ca-business-number? "99999999")))))

;; --- US -------------------------------------------------------------------

(defn- edgar [overrides]
  (merge {:cik "0009990001" :entityType "operating" :name "Fictional Widgets Inc."
          :stateOfIncorporation "DE" :lei nil
          :addresses {:business {:street1 "1 FAKE WAY" :street2 nil :city "TESTVILLE" :stateOrCountry "CA"
                                 :zipCode "99999" :isForeignLocation 0}}}
         overrides))

(def edgar-maps
  [(edgar {})
   (edgar {:cik "9990002" :name "Fictional Holdings Ltd" :stateOfIncorporation "E9" :lei "FAKE00TESTLEI0000134"
           :addresses {:business {:street1 "1 FAKE ROAD" :city "NOWHERE" :stateOrCountry "E9" :isForeignLocation 1}}})
   (edgar {:cik "0009990003" :entityType "other" :name "DOE JANE"})
   (edgar {:cik "0009990004" :entityType nil :name "NO TYPE"})
   (edgar {:cik "0009990005" :name ""})
   (edgar {:cik "999A" :name "BAD CIK INC"})])

(deftest us-sec-edgar
  (let [res (collect/collect catalog :us-sec-edgar edgar-maps)
        [a b] (:records res)]
    (is (= ["us-sec-edgar:0009990001" "us-sec-edgar:0009990002"] (map :entity/id (:records res))))
    (is (every? schema/valid? (:records res)))
    (is (= [:unknown "US-DE" "1 FAKE WAY" "TESTVILLE" "99999"]
           [(:entity/status a) (:entity/jurisdiction-detail a)
            (get-in a [:entity/address :street]) (get-in a [:entity/address :locality])
            (get-in a [:entity/address :postal-code])]))
    (testing "州でない設立地は jurisdiction-detail にせず、国外の住所は載せない。LEI は検算が通れば載せる"
      (is (nil? (:entity/jurisdiction-detail b)))
      (is (nil? (:entity/address b)))
      (is (= [:us-sec-cik :lei] (map :scheme (:entity/identifiers b)))))
    (testing "個人の filer など operating でないものは除外"
      (is (= [2 3] (map :index (:skipped res)))))
    (testing "名前が空・CIK が不正なものは拒否"
      (is (= [4 5] (map :index (:rejected res)))))
    (is (str/includes? (get-in a [:source :attribution]) "Securities and Exchange Commission"))))

;; --- CA -------------------------------------------------------------------

(def ca-header
  (str (char 0xFEFF)
       (str/join "," ["Corporation number" "Business number (BN)" "Corporate name - form 1" "Corporate name - form 2"
                      "Governing legislation" "Status" "Status Detail" "Anniversary date" "Year of last annual filing"
                      "Date of last annual meeting" "Street" "Street 2" "City/town" "Province/territory" "Country"
                      "Postal code" "Minimum number of directors" "Maximum number of directors"])))

(defn- ca-line [num bn name status country]
  (q num bn name "" "Canada Business Corporations Act" status "" "01-01" "2025" "" "1 Fake Street" "Suite 9"
     "Testville" "Ontario" country "Z9Z 9Z9" "1" "10"))

(def ca-lines
  [ca-header
   (ca-line "9990001" "999999998RC0001" "FICTIONAL MAPLE INC." "Active" "Canada")
   (ca-line "999000-2" "" "FICTIONAL OLD CORP." "Dissolved for non-compliance" "Canada")
   (ca-line "9990003" "" "FICTIONAL AMALGAMATED LTD." "Inactive - Amalgamated into another corporation" "United States")
   (ca-line "ABC" "" "BAD NUMBER INC." "Active" "Canada")
   (ca-line "9990004" "" "" "Active" "Canada")
   (q "9990005" "TOO FEW")])

(deftest ca-ised
  (let [res (collect/collect catalog :ca-ised ca-lines)
        [a b c] (:records res)]
    (is (= ["ca-ised:9990001" "ca-ised:999000-2" "ca-ised:9990003"] (map :entity/id (:records res))))
    (is (every? schema/valid? (:records res)))
    (testing "先頭の BOM 付きの見出しでも法人番号を読める"
      (is (= "9990001" (-> a :entity/identifiers first :value))))
    (is (= [:active :closed :inactive] (map :entity/status [a b c])))
    (is (= [{:scheme :ca-corporation-number :value "9990001"} {:scheme :ca-business-number :value "999999998"}]
           (:entity/identifiers a)))
    (is (= {:country "CA" :street "1 Fake Street, Suite 9" :locality "Testville" :region "ON" :postal-code "Z9Z 9Z9"}
           (:entity/address a)))
    (testing "国外の住所は載せない。取締役の人数は取り込まない"
      (is (nil? (:entity/address c)))
      (is (not (str/includes? (pr-str (:records res)) "directors"))))
    (is (= [3 4 5] (map :index (:rejected res))))))

;; --- AU -------------------------------------------------------------------

(defn- abr [abn kind name & {:keys [status from acn legal-entity]}]
  (cond-> {:ABN {:value abn :status (or status "ACT") :ABNStatusFromDate (or from "20200102")}
           :EntityType {:EntityTypeInd kind :EntityTypeText (str kind " (fictional)")}
           :MainEntity {:NonIndividualName {:type "MN" :NonIndividualNameText name}
                        :BusinessAddress {:AddressDetails {:State "NSW" :Postcode "2999"}}}}
    acn (assoc :ASICNumber {:value acn :ASICNumberType "undetermined"})
    legal-entity (-> (dissoc :MainEntity)
                     (assoc :LegalEntity {:IndividualName {:type "LGL" :GivenName ["JANE"] :FamilyName "DOE"}}))))

(def abr-maps
  [(abr "99900000180" "PRV" "FICTIONAL WATTLE PTY LTD" :acn "999000001")
   (abr "99900000261" "PUB" "FICTIONAL GUM LIMITED" :status "CAN" :from "20261031" :acn "999000002")
   (abr "99900000293" "IND" nil :legal-entity true)
   (abr "99900000180" "DIT" "THE TRUSTEE FOR THE FICTIONAL FAMILY TRUST")
   (abr "99900000181" "PRV" "BAD CHECK PTY LTD")
   (abr "99900000180" "OIE" "")])

(deftest au-abr
  (let [res (collect/collect catalog :au-abr abr-maps)
        [a b] (:records res)]
    (is (= ["au-abr:99900000180" "au-abr:99900000261"] (map :entity/id (:records res))))
    (is (every? schema/valid? (:records res)))
    (is (= [:active :closed] (map :entity/status [a b])))
    (is (= "2026-10-31" (:entity/closed-on b)))
    (is (nil? (:entity/closed-on a)))
    (testing "ACN は検算が通るときだけ載せる"
      (is (= [:au-abn :au-acn] (map :scheme (:entity/identifiers a))))
      (is (= [:au-abn] (map :scheme (:entity/identifiers b)))))
    (is (= {:country "AU" :region "NSW" :postal-code "2999"} (:entity/address a)))
    (testing "個人と、家族名を含みうる信託は除外"
      (is (= [2 3] (map :index (:skipped res))))
      (is (not (str/includes? (pr-str (:records res)) "DOE"))))
    (is (= [4 5] (map :index (:rejected res))))))

;; --- 止めたソース ------------------------------------------------------------

(deftest blocked-sources
  (doseq [id [:br-cnpj :nz-nzbn :za-cipc :il-rasham]]
    (let [e (first (filter #(= id (:id %)) catalog))]
      (is (= :planned (:status e)))
      (is (string? (:blocked-reason e)) (str id " に止めた理由がない"))
      (is (= "未実装のソース" (:error (collect/collect catalog id [])))))))

;; --- BR CNPJ ----------------------------------------------------------------
;; 内部利用のみ（配布不可。:br-cnpj は :planned のまま）。架空データだけを使う。
;; CNPJ の架空値（チェックディジット正）は Receita MOD 11 で検算した合成値。
;; 実データの登記情報は fixture にしない。

(deftest br-cnpj-ids
  (testing "14 桁検証（正）"
    (is (every? ids/cnpj14? ["99900001000150" "99900002000102" "99900003000220" "12345678000195"])))
  (testing "14 桁検証（負）"
    (is (not (ids/cnpj14? "99900001000151")) "検査数字が違う")
    (is (not (ids/cnpj14? "99900001000140")) "検査数字が違う")
    (is (not (ids/cnpj14? "9990000100015")) "13 桁")
    (is (not (ids/cnpj14? "00000000000000")) "全桁同一")
    (is (not (ids/cnpj14? "9990000100015A")) "数字でない"))
  (testing "8 桁 base の形式"
    (is (ids/cnpj-base? "99900001"))
    (is (not (ids/cnpj-base? "9990000")))
    (is (not (ids/cnpj-base? "9990000A")))))

(defn- br-line [base razao nat qual capital porte ente]
  (str/join ";" (map (fn [f] (str "\"" f "\"")) [base razao nat qual capital porte ente])))

(def br-lines
  [(br-line "99900001" "FICTIONAL REGISTRO COMERCIO LTDA" "2062" "49" "1000000,00" "01" "")
   (br-line "99900002" "FICTIONAL HOLDINGS SA" "2038" "10" "120000000000,00" "05" "")
   (br-line "99900003" "FICTIONAL INDIVIDUAL" "2135" "50" "0,00" "01" "")
   (br-line "ABC" "BAD BASE INC" "2062" "49" "0,00" "05" "")
   (br-line "99900004" "" "2062" "49" "0,00" "05" "")
   "\"99900005\";\"TOO FEW\""])

(deftest br-cnpj-parse
  (testing "実ファイルと同じ形：; 区切り・引用符・見出し行なしで読む。capital のコンマは区切りにならない"
    (let [[a b] (map br-cnpj/parse-line (take 2 br-lines))]
      (is (= "FICTIONAL REGISTRO COMERCIO LTDA" (:entity/name (:record a))))
      (is (= ["br-cnpj:99900001" "br-cnpj:99900002"] (map (comp :entity/id :record) [a b])))
      (is (= {:scheme :br-cnpj-base :value "99900001"} (first (:entity/identifiers (:record a)))))
      (is (= {:scheme :br-natureza :code "2038"} (:entity/legal-form (:record b))))
      (is (= :unknown (:entity/status (:record a))) "Empresas には situation がない")
      (is (every? schema/valid? (keep :record [a b])))))
  (testing "latin1 の商号をデコード済みで読む（RAZÃO の Ã は ISO-8859-1 の 0xC3）"
    (let [name (String. (byte-array [82 65 90 195 79]) "ISO-8859-1")
          line (br-line "99900006" name "2062" "49" "0,00" "05" "")
          res (br-cnpj/parse-line line)]
      (is (= "RAZÃO" (:entity/name (:record res))) "latin1 デコード済みの Ã をそのまま保持")
      (is (= (str (char 195) "O") (subs (:entity/name (:record res)) 3 5)))))
  (testing "列数違いは拒否"
    (is (:reject (br-cnpj/parse-line (nth br-lines 5))))
    (is (:reject (br-cnpj/parse-line (nth br-lines 3))))
    (is (:reject (br-cnpj/parse-line (nth br-lines 4))))))

(deftest br-cnpj-personal-excluded
  (testing "個人データ列を落とす：入力 map に混入した個人キーも出力に現れない"
    (let [res (br-cnpj/normalize {:cnpj-base "99900009" :razao-social "FICTIONAL CO LTDA"
                                  :natureza-juridica "2062" :qualificacao-responsavel "49"
                                  :capital-social "0,00" :porte "05" :ente-federativo ""
                                  :telefone "11999999999" :email "doe@example.invalid"
                                  :nome-socio "DOE JANE" :cpf "00000000100"
                                  :nome-representante "DOE JOHN" :cpf-representante "00000000200"
                                  :faixa-etaria "5"})]
      (is (:record res))
      (is (not (contains? (:record res) :telefone)) "telefone のキーがない")
      (is (not (contains? (:record res) :email)) "email のキーがない")
      (is (not (str/includes? (pr-str (:record res)) "DOE")) "氏名・電話・メールが見出しも値もない")
      (is (not (str/includes? (pr-str (:record res)) "11999999999")) "電話の値がない")
      (is (schema/valid? (:record res)))))
  (testing "213-5（Empresário Individual）の行は取り込まない"
    (is (string? (:skip (br-cnpj/parse-line (nth br-lines 2)))) "2135 は skip")
    (is (string? (:skip (br-cnpj/normalize {:cnpj-base "99900007" :razao-social "FICTIONAL INDIVIDUAL"
                                            :natureza-juridica "213-5"}))) "213-5 表記も skip")))

(deftest br-cnpj-deterministic
  (testing "同じ入力で 2 回実行しても同じ結果"
    (let [line (first br-lines)
          m (br-cnpj/row->map line)]
      (is (= (br-cnpj/parse-line line) (br-cnpj/parse-line line)))
      (is (= (br-cnpj/normalize m) (br-cnpj/normalize m))))))
