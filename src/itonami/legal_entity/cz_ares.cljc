(ns itonami.legal-entity.cz-ares
  "チェコ ARES（Administrativní registr ekonomických subjektů）の REST API
  GET /ekonomicke-subjekty/{ico} の応答（EkonomickySubjekt）を正規化する。
  項目は OpenAPI 3.0.0 定義 https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/v3/api-docs
  （ARES REST API v1.4.0、2026-11-23 取得、132815 bytes、
  sha256 822372bcc181740c1a0f43d0507c204780f575c7ddea837a65064a07a0819134）に従う。
  この parser は取得済みの 1 件（map）を受け取るだけで、ネットワークに触れない。
  取得時の照会は 500件/分を超えてはならない。財務省の運用条件
  （https://www.mfcr.cz/cs/ministerstvo/informacni-systemy/ares 、2026-11-23 確認）は
  500件/分超過・同一照会の反復・多数の同時自動照会・複数 IP での制限回避・
  ランダム探索・セキュリティ破壊の試みを受けてアクセス制限・遮断がありうるとする。
  個人事業主（pravniForma が 101/102/104/105/106/107/108 の fyzická osoba）は取り込まない。
  IČO が割当てられていない主体（ico が無く icoId のみ）は取り込まない。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.schema :as schema]))

(def attribution
  "Source: ARES (Administrativní registr ekonomických subjektů, Ministerstvo financí ČR). Modified by Awai Network.")

(def ^:private sole-trader-forms #{"101" "102" "104" "105" "106" "107" "108"})

(defn- field [m k]
  (let [v (get m k)]
    (when (and (string? v) (not (str/blank? v))) (str/trim v))))

(defn- num-str [v]
  (when (some? v) (str v)))

(defn- pad-left [s n]
  (if (< (count s) n)
    (str (apply str (repeat (- n (count s)) "0")) s)
    s))

(defn- street-of [a]
  (when-let [ulice (field a :nazevUlice)]
    (let [domovni (num-str (:cisloDomovni a))
          orient (num-str (:cisloOrientacni a))
          pismeno (or (field a :cisloOrientacniPismeno) "")]
      (str ulice
           (cond
             (and domovni orient) (str " " domovni "/" orient pismeno)
             domovni (str " " domovni)
             orient (str " /" orient pismeno)
             :else "")))))

(defn- postal-of [a]
  (if-let [psc (num-str (:psc a))]
    (pad-left psc 5)
    (field a :pscTxt)))

(defn normalize [m]
  (let [pf (field m :pravniForma)]
    (cond
      (sole-trader-forms pf) {:skip "fyzická osoba podnikající（個人事業主）"}
      :else
      (let [ico (or (field m :ico)
                    (let [id (field m :icoId)]
                      (when (and id (re-matches #"[0-9]{8}" id)) id)))]
        (if-not ico
          {:reject "IČO がない（ico が無く icoId のみ）"}
          (let [a (or (:sidlo m) {})
                zanik (field m :datumZaniku)
                vzniku (field m :datumVzniku)
                aktualizace (field m :datumAktualizace)
                addr (cond-> {:country (or (field a :kodStatu) "CZ")}
                       (street-of a) (assoc :street (street-of a))
                       (field a :nazevObce) (assoc :locality (field a :nazevObce))
                       (postal-of a) (assoc :postal-code (postal-of a))
                       (field a :textovaAdresa) (assoc :full (field a :textovaAdresa)))
                addr (when (> (count addr) 1) addr)
                r (cond-> {:entity/id (str "cz-ares:" ico)
                           :entity/jurisdiction "CZ"
                           :entity/name (field m :obchodniJmeno)
                           :entity/identifiers [{:scheme :cz-ico :value ico}]
                           :entity/status (if zanik :closed :active)
                           :source {:id :cz-ares :attribution attribution :modified? true}}
                      pf (assoc :entity/legal-form {:scheme :cz-pravni-forma :code pf})
                      addr (assoc :entity/address addr)
                      vzniku (assoc :entity/registered-on vzniku)
                      zanik (assoc :entity/closed-on zanik)
                      aktualizace (assoc-in [:source :updated-on] aktualizace))
                ps (schema/problems r)]
            (if (seq ps) {:reject (str/join "; " ps)} {:record r})))))))

;; --- 取得ポリシー（リトライとレート制限） --------------------------------
;; 純粋関数のみ。ファイル・ネットワーク・時計に触れない（時刻は引数で渡す）。
;; 根拠: 財務省（MFČR）の運用条件（https://www.mfcr.cz/cs/ministerstvo/informacni-systemy/ares 、
;; 2026-11-23 確認）は 500件/分超過を受けてアクセス制限・遮断がありうるとする。取得側は
;; 下記の関数の戻り値どおりに待機して 500件/分を守り、429 と 5xx のみ指数バックオフで再試行する。

(def max-requests-per-minute
  "運用条件の上限（500件/分）。"
  500)

(def rate-window-ms
  "レート制限の窓（60秒）。"
  60000)

(def max-retries
  "再試行の上限（初回に加えて最大 3 回まで。総試行数は最大 4）。"
  3)

(def backoff-schedule-ms
  "再試行待機の指数バックオフ（1s → 2s → 4s）。"
  [1000 2000 4000])

(defn retryable-status?
  "429 と 5xx のみ true（再試行の対象）。それ以外（200・400・404 等）は false。"
  [status]
  (and (integer? status)
       (or (= 429 status)
           (<= 500 status 599))))

(defn backoff-ms
  "attempt（これまでの試行数。初回は 1）に対し、次の再試行までの待機ミリ秒を返す。上限を超えたら nil。"
  [attempt]
  (when (and (integer? attempt) (<= 1 attempt max-retries))
    (nth backoff-schedule-ms (dec attempt))))

(defn retry-wait-ms
  "失敗応答 {:status s :attempt n} が再試行に値するか。値すれば待機ミリ秒、しなければ nil。"
  [{:keys [status attempt]}]
  (when (retryable-status? status)
    (backoff-ms attempt)))

(defn throttle-wait-ms
  "任意の 60 秒窓に 500 件を超えないための待機ミリ秒（スライディングウィンドウ）。
  now-ms は現在時刻（ミリ秒）、call-times は過去の呼び出し時刻の列。待つ必要がなければ 0。"
  [now-ms call-times]
  (let [recent (filter #(> % (- now-ms rate-window-ms)) call-times)]
    (if (>= (count recent) max-requests-per-minute)
      (max 0 (- (+ (apply min recent) rate-window-ms) now-ms))
      0)))

(defn next-wait-ms
  "次の呼び出しまでの待機ミリ秒。再試行しないなら nil、それ以外はバックオフとスロットルの大きい方。"
  [{:keys [status attempt now-ms call-times]}]
  (let [b (retry-wait-ms {:status status :attempt attempt})]
    (when (some? b)
      (max b (throttle-wait-ms now-ms (or call-times []))))))
