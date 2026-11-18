(ns itonami.legal-entity.core-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [itonami.legal-entity.collect :as collect]
            [itonami.legal-entity.csv :as csv]
            [itonami.legal-entity.fixtures :as fx]
            [itonami.legal-entity.gleif :as gleif]
            [itonami.legal-entity.ids :as ids]
            [itonami.legal-entity.jp-nta :as jp-nta]
            [itonami.legal-entity.schema :as schema]))

(def catalog (edn/read-string (slurp "sources/catalog.edn")))

(deftest identifiers
  (testing "実在の値で検算（国税庁の法人番号、GLEIF の LOU の LEI）"
    (is (ids/jp-corporate-number? "7000012050002"))
    (is (ids/lei? "636700184FM9OFFK8562")))
  (testing "架空の値"
    (is (ids/jp-corporate-number? "8990000000001"))
    (is (ids/lei? fx/lei-1))
    (is (ids/lei? fx/lei-2)))
  (testing "負の対照"
    (is (not (ids/jp-corporate-number? "1990000000001")) "チェックデジット不一致")
    (is (not (ids/jp-corporate-number? "899000000001")) "12 桁")
    (is (not (ids/jp-corporate-number? "899000000000a")))
    (is (not (ids/lei? "FAKE00TESTLEI0000199")) "検査数字不一致")
    (is (not (ids/lei? "fake00testlei0000134")) "小文字")
    (is (not (ids/lei? nil)))))

(deftest csv-line
  (is (= ["a" "b,c" "d\"e" ""] (csv/parse-line "\"a\",\"b,c\",\"d\"\"e\",\"\"")))
  (is (= ["a" "" "c"] (csv/parse-line "a,,c")))
  (is (nil? (csv/parse-line "\"a,b"))))

(deftest nta
  (let [p #(jp-nta/parse-line (get fx/nta-lines %))]
    (testing "通常の法人"
      (let [r (:record (p :active))]
        (is (schema/valid? r))
        (is (= "jp-nta:8990000000001" (:entity/id r)))
        (is (= :active (:entity/status r)))
        (is (= {:scheme :jp-nta-kind :code "301" :label "株式会社"} (:entity/legal-form r)))
        (is (= "Kaku Shoji Co., Ltd." (:entity/name-en r)))
        (is (= "2026-11-02" (:entity/registered-on r)))
        (is (re-find #"加工して作成" (get-in r [:source :attribution])))))
    (testing "閉鎖された法人"
      (let [r (:record (p :closed))]
        (is (= :closed (:entity/status r)))
        (is (= "2026-10-31" (:entity/closed-on r)))))
    (testing "名称の引用符とカンマ"
      (is (= "株式会社\"架空\",テスト" (:entity/name (:record (p :quoted-comma)))))))
  (testing "取り込まない"
    (is (= {:skip "検索対象除外"} (jp-nta/parse-line (:hihyoji fx/nta-lines))))
    (is (= {:skip "処理区分 99（削除）"} (jp-nta/parse-line (:deleted fx/nta-lines)))))
  (testing "負の対照"
    (doseq [k [:bad-check-digit :bad-kind :too-few-columns :unclosed-quote]]
      (is (:reject (jp-nta/parse-line (get fx/nta-lines k))) (name k)))))

(deftest gleif
  (let [r (:record (gleif/normalize (:active fx/gleif-records)))]
    (is (schema/valid? r))
    (is (= "lei:FAKE00TESTLEI0000134" (:entity/id r)))
    (is (= "SE" (:entity/jurisdiction r)))
    (is (= [{:scheme :lei :value fx/lei-1}
            {:scheme :registration-number :registry "RA000544" :value "5560000000"}]
           (:entity/identifiers r)))
    (is (= "1999-01-02" (:entity/registered-on r)))
    (is (= "2026-11-17" (get-in r [:source :updated-on]))))
  (let [r (:record (gleif/normalize (:us-subdivision fx/gleif-records)))]
    (is (= ["US" "US-DE" :inactive] ((juxt :entity/jurisdiction :entity/jurisdiction-detail :entity/status) r)))
    (is (nil? (:entity/legal-form r))))
  (testing "負の対照"
    (is (:reject (gleif/normalize (:bad-lei fx/gleif-records))))
    (is (:reject (gleif/normalize (:no-name fx/gleif-records))))))

(deftest schema-problems
  (is (seq (schema/problems {})))
  (is (some #(re-find #"未知の識別子" %)
            (schema/problems {:entity/id "x:1" :entity/jurisdiction "JP" :entity/name "a"
                              :entity/status :active :entity/identifiers [{:scheme :duns :value "1"}]
                              :source {:id :x :attribution "a"}})))
  (is (some #(re-find #"出典表示" %)
            (schema/problems {:entity/id "x:1" :entity/jurisdiction "JP" :entity/name "a"
                              :entity/status :active
                              :entity/identifiers [{:scheme :jp-corporate-number :value "8990000000001"}]
                              :source {:id :x}}))))

(deftest catalog-shape
  (is (= 19 (count catalog)) "GLEIF＋移植元の 18 法域")
  (is (apply distinct? (map :id catalog)))
  (is (= #{:gleif :jp-nta :gb-companies-house :fr-sirene :no-brreg :fi-prh}
         (set (map :id (filter #(= :implemented (:status %)) catalog)))))
  (doseq [e catalog :when (= :implemented (:status e))]
    (is (every? e [:license :url :terms-verified-on :parser]) (name (:id e)))))

(deftest collect-dispatch
  (let [res (collect/collect catalog :jp-nta (vals fx/nta-lines))]
    (is (= 3 (count (:records res))))
    (is (= 2 (count (:skipped res))))
    (is (= 4 (count (:rejected res)))))
  (testing "負の対照"
    (is (= "カタログにないソース" (:error (collect/collect catalog :dnb []))))
    (is (= "未実装のソース" (:error (collect/collect catalog :dk-cvr []))))
    (is (= "利用条件が未確認のソース"
           (:error (collect/collect (map #(assoc % :terms-verified-on nil) catalog) :gleif []))))))
