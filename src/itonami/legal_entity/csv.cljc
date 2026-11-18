(ns itonami.legal-entity.csv
  "RFC 4180 の 1 行を読む最小の parser。改行を含む項目は扱わない（呼び出し側で 1 行ずつ渡す）。"
  (:require [clojure.string :as str]))

(def ^:private quote-char (first "\""))
(def ^:private comma-char (first ","))

(defn parse-line
  "1 行を項目のベクタにする。引用符が閉じていなければ nil。"
  [line]
  (let [n (count line)
        at (fn [i] (when (< i n) (nth line i)))]
    (loop [i 0, field [], fields [], quoted? false]
      (let [c (at i)]
        (cond
          (nil? c)
          (when-not quoted? (conj fields (apply str field)))

          quoted?
          (if (= c quote-char)
            (if (= (at (inc i)) quote-char)
              (recur (+ i 2) (conj field quote-char) fields true)
              (recur (inc i) field fields false))
            (recur (inc i) (conj field c) fields true))

          (= c comma-char)
          (recur (inc i) [] (conj fields (apply str field)) false)

          (and (= c quote-char) (empty? field))
          (recur (inc i) field fields true)

          :else
          (recur (inc i) (conj field c) fields false))))))

(defn rows
  "先頭行を見出しとして、残りの行を見出し→値の map にする。
  見出しの前後の空白は取り除く。列数が見出しと違う行は {:reject ...}。"
  [lines]
  (let [[header & body] lines
        cols (some->> header parse-line (mapv str/trim))]
    (for [line body]
      (let [fields (parse-line line)]
        (cond
          (nil? cols) {:reject "見出し行がない"}
          (nil? fields) {:reject "引用符が閉じていない"}
          (not= (count cols) (count fields)) {:reject (str "列数が " (count fields) "（見出しは " (count cols) "）")}
          :else (zipmap cols fields))))))
