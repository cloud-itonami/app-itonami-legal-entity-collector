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
  "再試行の上限（初回に加えて最大 4 回まで。総試行数は最大 5）。"
  4)

(def backoff-schedule-ms
  "再試行待機の指数バックオフ（1s → 2s → 4s → 8s）。"
  [1000 2000 4000 8000])

(def max-total-wait-ms
  "バックオフ待機の総和の上限（30秒）。総和は 15000ms で上限内に収まる。"
  30000)

(def min-interval-ms
  "同一の request-fn への呼び出し間隔の下限（500件/分 → 120ms）。rate-window-ms と max-requests-per-minute から導く。"
  (quot rate-window-ms max-requests-per-minute))

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

(defn success-status?
  "2xx のみ true。数字以外の入力は false（副作用なしで安全に判定できるよう順序を保つ）。"
  [status]
  (and (integer? status) (<= 200 status 299)))

(defn interval-wait-ms
  "最後の試行時刻 last-attempt-ms から now-ms までに min-interval-ms の間隔を確保するための残り待機ミリ秒。最後の試行が無い・時刻が数字でない場合は 0。"
  [last-attempt-ms now-ms]
  (if (and (integer? last-attempt-ms) (integer? now-ms))
    (max 0 (- min-interval-ms (- now-ms last-attempt-ms)))
    0))

(defn new-request-state
  "request-with-retry の状態（送信時刻の履歴と照会キャッシュを持つ atom）。呼び出し側が保持して :state で渡す。"
  []
  (atom {:call-times [] :cache {}}))

(defn- invoke-with-retry
  "内部実装。state は確定済みで、キャッシュヒットは済んでいる（request-key の結果は呼び出し側で記録する）。"
  [state request-fn sleep-fn now-fn]
  (let [result (loop [attempt 1 waited 0 last-attempt nil]
                 (let [tw (throttle-wait-ms (now-fn) (:call-times @state))]
                   (when (pos? tw)
                     (sleep-fn tw))
                   (let [started (now-fn)]
                     (swap! state update :call-times conj started)
                     (let [resp (request-fn)
                           status (:status resp)]
                       (cond
                         (success-status? status) (:body resp)
                         (not (retryable-status? status)) {:status status :attempts attempt :waited-ms waited}
                         :else
                         (let [backoff (backoff-ms attempt)]
                           (if (nil? backoff)
                             {:status status :attempts attempt :waited-ms waited}
                             (let [wait (max backoff (interval-wait-ms last-attempt (now-fn)))]
                               (if (> (+ waited wait) max-total-wait-ms)
                                 {:status status :attempts attempt :waited-ms waited}
                                 (do (when (pos? wait)
                                       (sleep-fn wait))
                                     (recur (inc attempt)
                                            (+ waited wait)
                                            started)))))))))))]
    result))

(defn request-with-retry
  "request-fn（引数なしで {:status s :body b} を返す）を 429 と 5xx の間だけ最大 4 回再試行する。
  待機は指数バックオフ（1s → 2s → 4s → 8s）と、同 request-fn への次回呼び出しまでの
  120ms 間隔のうち長い方とし、総待機は 30000ms を超えたら再試行を止める。
  さらに trailing 60 秒で 500 件を超えないよう、送信前に window 内の件数を数え、
  500 以上なら最も古い試行が 60 秒窓から外れるまで sleep する（レート待機は上記の
  バックオフ総待機の上限とは別枠）。
  同一の照会（opts の :request-key。呼び出し側が正規化済みの対象 URL やパラメータ文字列を渡す）の
  反復はしない。:request-key の結果（成功の body / 失敗の {:status ...}）を :state に保持し、
  2回目以降は送らずにキャッシュを返す。:state を渡さなければ呼び出しごとに空の状態で動き、
  呼び出しをまたいだレート制限・重複防止は効かない。
  request-fn・sleep-fn（待機ミリ秒）・now-fn（現在時刻ミリ秒）はすべて引数で注入する。
  この namespace が直接ファイル・ネットワーク・時計に触れることはない。
  成功時（2xx）は body をそのまま返す。失敗は {:status :attempts :waited-ms} で返す。"
  [request-fn sleep-fn now-fn & [{:keys [request-key state]}]]
  (let [state (or state (new-request-state))]
    (if (and (some? request-key)
             (contains? (:cache @state) request-key))
      (get (:cache @state) request-key)
      (let [result (invoke-with-retry state request-fn sleep-fn now-fn)]
        (when (some? request-key)
          (swap! state assoc-in [:cache request-key] result))
        result))))
