(ns itonami.legal-entity.jp-nta
  "国税庁法人番号公表サイトの全件・差分データ（CSV 形式、Unicode）を共通スキーマに正規化する。

  列の順序は「リソース定義書（ダウンロードファイル、Web-API）」4.1 版の項番 7〜36（30 列）。
  https://www.houjin-bangou.nta.go.jp/pc/download/images/k-resource-dl.pdf
  （2026-11-18 取得、sha256 9bc611fcf9d533502a34f287b6d8673564974b89fad23cba824cd681095ac8ba）"
  (:require [clojure.string :as str]
            [itonami.legal-entity.csv :as csv]
            [itonami.legal-entity.ids :as ids]
            [itonami.legal-entity.schema :as schema]))

(def columns
  [:sequence-number :corporate-number :process :correct :update-date :change-date
   :name :name-image-id :kind :prefecture-name :city-name :street-number
   :address-image-id :prefecture-code :city-code :post-code :address-outside
   :address-outside-image-id :close-date :close-cause :successor-corporate-number
   :change-cause :assignment-date :latest :en-name :en-prefecture-name
   :en-city-name :en-address-outside :furigana :hihyoji])

(def kinds
  "法人種別（リソース定義書 項番 15）。"
  {"101" "国の機関" "201" "地方公共団体" "301" "株式会社" "302" "有限会社"
   "303" "合名会社" "304" "合資会社" "305" "合同会社" "399" "その他の設立登記法人"
   "401" "外国会社等" "499" "その他"})

(def attribution
  "公共データ利用規約（第 1.0 版）に沿った出典と加工の表示。"
  "出典：国税庁法人番号公表サイト（国税庁）（https://www.houjin-bangou.nta.go.jp/）をもとに Awai Network が加工して作成")

(defn- blank->nil [s] (when-not (str/blank? s) s))

(defn row->map
  "CSV の 1 行を列名の map にする。列数が違えば {:reject ...}。"
  [line]
  (let [fields (csv/parse-line line)]
    (cond
      (nil? fields) {:reject "引用符が閉じていない"}
      (not= (count columns) (count fields)) {:reject (str "列数が " (count fields) "（期待 30）")}
      :else (zipmap columns fields))))

(defn normalize
  "列名の map を共通スキーマのレコードにする。
  戻り値は {:record r} / {:skip reason} / {:reject reason}。"
  [m]
  (let [cn (:corporate-number m)]
    (cond
      (:reject m) m
      (not (ids/jp-corporate-number? cn)) {:reject (str "法人番号が不正: " (pr-str cn))}
      ;; 検索対象除外（項番 36）は公表サイトの検索対象から外された情報。取り込まない。
      (= "1" (:hihyoji m)) {:skip "検索対象除外"}
      ;; 処理区分 99 は削除（公表の同意の撤回など）。取り込まない。
      (= "99" (:process m)) {:skip "処理区分 99（削除）"}
      (not (kinds (:kind m))) {:reject (str "法人種別が不正: " (pr-str (:kind m)))}
      :else
      (let [closed (blank->nil (:close-date m))
            r (cond-> {:entity/id (str "jp-nta:" cn)
                       :entity/jurisdiction "JP"
                       :entity/name (:name m)
                       :entity/identifiers [{:scheme :jp-corporate-number :value cn}]
                       :entity/legal-form {:scheme :jp-nta-kind :code (:kind m) :label (kinds (:kind m))}
                       :entity/status (if closed :closed :active)
                       :entity/address (cond-> {:country "JP"}
                                         (blank->nil (:prefecture-name m)) (assoc :region (:prefecture-name m))
                                         (blank->nil (:city-name m)) (assoc :locality (:city-name m))
                                         (blank->nil (:street-number m)) (assoc :street (:street-number m))
                                         (blank->nil (:post-code m)) (assoc :postal-code (:post-code m)))
                       :source {:id :jp-nta
                                :updated-on (:update-date m)
                                :process (:process m)
                                :latest? (= "1" (:latest m))
                                :attribution attribution
                                :modified? true}}
                (blank->nil (:en-name m)) (assoc :entity/name-en (:en-name m))
                (blank->nil (:furigana m)) (assoc :entity/name-kana (:furigana m))
                (blank->nil (:assignment-date m)) (assoc :entity/registered-on (:assignment-date m))
                closed (assoc :entity/closed-on closed))
            ps (schema/problems r)]
        (if (seq ps) {:reject (str/join "; " ps)} {:record r})))))

(defn parse-line [line] (normalize (row->map line)))
