(ns itonami.legal-entity.collect
  "ソースごとの parser を呼び、受理・除外・拒否を集計する。
  取得（ダウンロード）はここでは行わない。呼び出し側が取得済みの入力を渡す。"
  (:require [itonami.legal-entity.au-abr :as au-abr]
            [itonami.legal-entity.ca-ised :as ca-ised]
            [itonami.legal-entity.csv :as csv]
            [itonami.legal-entity.ee-ariregister :as ee-ariregister]
            [itonami.legal-entity.fi-prh :as fi-prh]
            [itonami.legal-entity.fr-sirene :as fr-sirene]
            [itonami.legal-entity.gb-companies-house :as gb-ch]
            [itonami.legal-entity.gleif :as gleif]
            [itonami.legal-entity.jp-nta :as jp-nta]
            [itonami.legal-entity.no-brreg :as no-brreg]
            [itonami.legal-entity.us-sec-edgar :as us-sec-edgar]))

(def parsers
  "実装済みのソース → {:input 入力の形, :parse 1 件を {:record}/{:skip}/{:reject} にする関数}。
  :lines は見出しのない CSV の行、:csv-with-header は先頭行が見出しの CSV の行（カンマ区切り）、
  :csv-semicolon は ; 区切り、:maps は JSON の要素。"
  {:gleif {:input :maps :parse gleif/normalize}
   :jp-nta {:input :lines :parse jp-nta/parse-line}
   :gb-companies-house {:input :csv-with-header :parse gb-ch/normalize}
   :fr-sirene {:input :csv-with-header :parse fr-sirene/normalize}
   :no-brreg {:input :maps :parse no-brreg/normalize}
   :fi-prh {:input :maps :parse fi-prh/normalize}
   :us-sec-edgar {:input :maps :parse us-sec-edgar/normalize}
   :ca-ised {:input :csv-with-header :parse ca-ised/normalize}
   :au-abr {:input :maps :parse au-abr/normalize}
   :ee-ariregister {:input :csv-semicolon :parse ee-ariregister/normalize}})

(defn collect
  "catalog はソースカタログ（sources/catalog.edn）、inputs は取得済みの入力の列。
  未知・未実装・利用条件未確認のソースは処理せず {:error ...} を返す。"
  [catalog source-id inputs]
  (let [entry (first (filter #(= source-id (:id %)) catalog))
        {:keys [input parse]} (parsers source-id)]
    (cond
      (nil? entry) {:source source-id :error "カタログにないソース"}
      (not= :implemented (:status entry)) {:source source-id :error "未実装のソース"}
      (nil? (:terms-verified-on entry)) {:source source-id :error "利用条件が未確認のソース"}
      (nil? parse) {:source source-id :error "parser がない"}
      :else
      (let [items (cond (= :csv-with-header input) (csv/rows inputs)
                        (= :csv-semicolon input) (csv/rows inputs \;)
                        :else inputs)
            results (map-indexed (fn [i x] (assoc (parse x) :index i)) items)]
        {:source source-id
         :records (vec (keep :record results))
         :skipped (vec (for [r results :when (:skip r)] (select-keys r [:index :skip])))
         :rejected (vec (for [r results :when (:reject r)] (select-keys r [:index :reject])))}))))
