(ns itonami.legal-entity.collect
  "ソースごとの parser を呼び、受理・除外・拒否を集計する。
  取得（ダウンロード）はここでは行わない。呼び出し側が取得済みの入力を渡す。"
  (:require [itonami.legal-entity.gleif :as gleif]
            [itonami.legal-entity.jp-nta :as jp-nta]))

(def parsers
  "実装済みのソース → 入力 1 件を {:record}/{:skip}/{:reject} にする関数。"
  {:gleif gleif/normalize
   :jp-nta jp-nta/parse-line})

(defn collect
  "catalog はソースカタログ（sources/catalog.edn）、inputs は取得済みの入力の列。
  未知・未実装・利用条件未確認のソースは処理せず {:error ...} を返す。"
  [catalog source-id inputs]
  (let [entry (first (filter #(= source-id (:id %)) catalog))
        parse (parsers source-id)]
    (cond
      (nil? entry) {:source source-id :error "カタログにないソース"}
      (not= :implemented (:status entry)) {:source source-id :error "未実装のソース"}
      (nil? (:terms-verified-on entry)) {:source source-id :error "利用条件が未確認のソース"}
      (nil? parse) {:source source-id :error "parser がない"}
      :else
      (let [results (map-indexed (fn [i x] (assoc (parse x) :index i)) inputs)]
        {:source source-id
         :records (vec (keep :record results))
         :skipped (vec (for [r results :when (:skip r)] (select-keys r [:index :skip])))
         :rejected (vec (for [r results :when (:reject r)] (select-keys r [:index :reject])))}))))
