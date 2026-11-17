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

(defn country-code?
  "ISO 3166-1 alpha-2 の形（大文字 2 文字）。実在の国かまでは見ない。"
  [s]
  (and (string? s) (boolean (re-matches #"[A-Z]{2}" s))))

(defn iso-date?
  [s]
  (and (string? s) (boolean (re-matches #"\d{4}-\d{2}-\d{2}" s))))
