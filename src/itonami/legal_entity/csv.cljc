(ns itonami.legal-entity.csv
  "RFC 4180 の 1 行を読む最小の parser。改行を含む項目は扱わない（呼び出し側で 1 行ずつ渡す）。"
  (:require [clojure.string :as str]))

(def ^:private quote-char (first "\""))
(def ^:private comma-char (first ","))

(defn parse-line
  "1 行を項目のベクタにする。引用符が閉じていなければ nil。"
  ([line] (parse-line line \,))
  ([line ^char delim]
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

           (= c delim)
           (recur (inc i) [] (conj fields (apply str field)) false)

           (and (= c quote-char) (empty? field))
           (recur (inc i) field fields true)

           :else
           (recur (inc i) (conj field c) fields false)))))))

(defn rows
  "先頭行を見出しとして、残りの行を見出し→値の map にする。
  見出しの前後の空白と先頭の BOM は取り除く。列数が見出しと違う行は {:reject ...}。"
  ([lines] (rows lines \,))
  ([lines delim]
   (let [[header & body] lines
         bom-str (str (char 0xFEFF))
         strip-bom (fn [c] (if (str/starts-with? (str c) bom-str) (subs (str c) 1) (str c)))
         cols (when header (mapv (comp str/trim strip-bom) (parse-line header delim)))]
     (for [line body]
       (let [fields (parse-line line delim)]
         (cond
           (nil? cols) {:reject "見出し行がない"}
           (nil? fields) {:reject "引用符が閉じていない"}
           (not= (count cols) (count fields)) {:reject (str "列数が " (count fields) "（見出しは " (count cols) "）")}
           :else (zipmap cols fields)))))))
