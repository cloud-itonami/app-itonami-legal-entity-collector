(ns itonami.legal-entity.ids
  "識別子の検証。ファイル・ネットワーク・時計に触れない純粋関数。")

(defn- digit [c] #?(:clj (Character/digit ^char c 10) :cljs (js/parseInt (str c) 10)))

(defn- digits? [s n]
  (and (string? s) (= n (count s)) (boolean (re-matches #"[0-9]+" s))))

(defn jp-corporate-number-check-digit
  "法人番号（13 桁）の先頭のチェックデジットを、残り 12 桁から計算する。
  検査用数字 = 9 - (Σ Pn × Qn を 9 で割った余り)。Pn は下位から n 桁目、
  Qn は n が奇数なら 1、偶数なら 2（法人番号の指定等に関する省令 第 2 条）。"
  [base12]
  (let [ds (map digit (reverse base12))
        s (reduce + (map-indexed (fn [i d] (* d (if (even? i) 1 2))) ds))]
    (- 9 (mod s 9))))

(defn jp-corporate-number?
  "13 桁の数字で、チェックデジットが合うとき true。"
  [s]
  (and (digits? s 13)
       (= (digit (first s)) (jp-corporate-number-check-digit (subs s 1)))))

(defn- lei->digits
  "英字を 10..35 に置き換えた数字列。"
  [s]
  (apply str (map (fn [c]
                    (let [code #?(:clj (int c) :cljs (.charCodeAt c 0))]
                      (if (<= 48 code 57) (str c) (str (- code 55)))))
                  s)))

(defn- mod97
  "大きな数字列の 97 の余り（桁ごとに計算するので bigint を使わない）。"
  [ds]
  (reduce (fn [r c] (mod (+ (* r 10) (digit c)) 97)) 0 ds))

(defn lei?
  "LEI（ISO 17442）。英大文字と数字の 20 文字で、ISO 7064 MOD 97-10 の余りが 1。"
  [s]
  (and (string? s)
       (boolean (re-matches #"[0-9A-Z]{18}[0-9]{2}" s))
       (= 1 (mod97 (lei->digits s)))))

(defn- weighted-sum [weights ds] (reduce + (map * weights ds)))

(defn fr-siren?
  "SIREN（9 桁）。Luhn の検査を通るとき true。"
  [s]
  (and (digits? s 9)
       (let [ds (map digit (reverse s))
             total (reduce + (map-indexed (fn [i d] (if (odd? i) (let [x (* 2 d)] (if (> x 9) (- x 9) x)) d)) ds))]
         (zero? (mod total 10)))))

(defn no-orgnr?
  "ノルウェーの organisasjonsnummer（9 桁）。重み 3,2,7,6,5,4,3,2 の MOD 11。"
  [s]
  (and (digits? s 9)
       (let [ds (map digit s)
             r (- 11 (mod (weighted-sum [3 2 7 6 5 4 3 2] (take 8 ds)) 11))
             check (case r 11 0 10 nil r)]
         (= check (last ds)))))

(defn fi-business-id?
  "フィンランドの Y-tunnus（NNNNNNN-C）。重み 7,9,10,5,8,4,2 の MOD 11。余り 1 は無効。"
  [s]
  (and (string? s)
       (boolean (re-matches #"\d{7}-\d" s))
       (let [ds (map digit (subs s 0 7))
             r (mod (weighted-sum [7 9 10 5 8 4 2] ds) 11)
             check (case r 0 0 1 nil (- 11 r))]
         (= check (digit (last s))))))

(defn gb-company-number?
  "Companies House の会社番号（英大文字と数字の 8 文字。例 01234567、SC123456）。"
  [s]
  (and (string? s) (boolean (re-matches #"[A-Z0-9]{8}" s))))

(defn us-sec-cik?
  "SEC EDGAR の CIK。data.sec.gov の表記どおり、先頭を 0 で埋めた 10 桁。検査数字はない。"
  [s]
  (digits? s 10))

(defn au-abn?
  "オーストラリアの ABN（11 桁）。先頭の桁から 1 を引き、重み 10,1,3,5,7,9,11,13,15,17,19 の和が 89 で割り切れる。"
  [s]
  (and (digits? s 11)
       (let [[d & ds] (map digit s)]
         (zero? (mod (weighted-sum [10 1 3 5 7 9 11 13 15 17 19] (cons (dec d) ds)) 89)))))

(defn au-acn?
  "オーストラリアの ACN（9 桁）。先頭 8 桁の重み 8..1 の和を 10 で割った余りの補数（10 - r、10 なら 0）が末尾の桁。"
  [s]
  (and (digits? s 9)
       (let [ds (map digit s)
             r (mod (weighted-sum [8 7 6 5 4 3 2 1] (take 8 ds)) 10)]
         (= (mod (- 10 r) 10) (last ds)))))

(defn ca-corporation-number?
  "Corporations Canada の法人番号。7〜8 桁の数字か、末尾に -N の付く形（例 1234567、123456-7）。検査数字は確かめない。"
  [s]
  (and (string? s) (boolean (re-matches #"[0-9]{6,8}|[0-9]{6,7}-[0-9]" s))))

(defn ca-business-number?
  "カナダの事業者番号 BN の先頭 9 桁（BN9）。検査数字は確かめない。"
  [s]
  (digits? s 9))

(defn cz-ico?
  "チェコの IČO（8 桁）。先頭 7 桁に 8,7,6,5,4,3,2 を掛けた和を 11 で割る。
  余り 0 → 1、余り 1 → 0、それ以外 → 11 - 余り。
  例 00006947、00177041（財務省の IČO 案内に出た値）。架空値 99900017（fictional）も通る。"
  [s]
  (and (digits? s 8)
       (let [ds (map digit (take 7 s))
             r (mod (weighted-sum [8 7 6 5 4 3 2] ds) 11)
             check (case r 0 1 1 0 (- 11 r))]
         (= check (digit (nth s 7))))))

(defn ee-registry-code?
  "エストニアの äriregistri kood（8 桁の数字）。検査数字は確かめない。"
  [s]
  (digits? s 8))

(defn country-code?
  "ISO 3166-1 alpha-2 の形（大文字 2 文字）。実在の国かまでは見ない。"
  [s]
  (and (string? s) (boolean (re-matches #"[A-Z]{2}" s))))

(defn iso-date?
  [s]
  (and (string? s) (boolean (re-matches #"\d{4}-\d{2}-\d{2}" s))))
