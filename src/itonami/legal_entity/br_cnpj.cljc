(ns itonami.legal-entity.br-cnpj
  "Receita Federal CNPJ — Empresas ファイル（ZIP 内 CSV、; 区切り、latin1、見出し行なし）の 1 行を読む純粋関数。
  この namespace の関数はファイル・ネットワーク・時計に触れない。latin1 の明示的なデコードは呼び出し側
  （取得・復号の責務）が行い、デコード済みの文字列を渡す。

  片道は内部利用のみ（owner 2026-11-23 承認）：変換したデータは内部の保存と正規化までで、外部配布・公開・
  API 応答は弁護士確認とオーナー判断の後。catalog の :br-cnpj は :planned のまま（:implemented にしない）。

  個人データは取得しない・保持しない・出力に含めない：
   - Socios*.zip の列（nm_socios / cpf / nm_representante 等）は本パーサが知る限りのカラムではない。
     Empresas の 7 列だけを読む。余分なキー（例 :nome-socio :telefone :email）が入力 map に混入しても
     strip-personal が入口で落とす。
   - Estabelecimentos*.zip の telefone / email / fax は本パーサが読まない（Empresas 1 ファイルのみ）。
   - Natureza 2135（213-5、Empresário Individual）の行は入口で skip する。この行為の razao social は
     個人名そのものなので、レコード化する前に落とす。

  列順・区切り・エンコーディングは実データで確認した（推測していない）：
   - 取得ファイルは Empresas1.zip（2026-11、77906042 bytes、sha256 5aaa6308485098878c8f0da9a66a63af09b3d50aaf5fccb4eb70479fb12951b9）。
     ZIP 内の CSV は K3241.K03200Y1.D61114.EMPRECSV（325910903 bytes）。取得日 2026-11-23。
   - 先頭行から全行まで：見出し行なし、7 列、全列ダブルクォート囲み、区切りは ;、行区切りは LF。
     全行が 7 列（Empresas1.zip の全 4494860 行）であることを実ファイルで確認した。
     capital social は '120000000000,00' のようにクォート内にコンマ小数を含むため、自動ロード形式の
     クォート処理（csv/parse-line の ; 区切り）で読む。
   - ファイル全体は UTF-8 として不正（latin1 の単バイト 0xC3=Ã 等を含む）→ latin1（ISO-8859-1）。
   - CNPJ BÁSICO は 8 桁の数字、natureza はダッシュなし 4 桁（実ファイルの全行が該当）。2135 は
     専門家個別の個人店名が含まれるため除外対象（実ファイルでは 1859420 行＝約 41%）。

  CNPJ の 14 桁検証は CNPJ BÁSICO の 8 桁だけでは行えない（完全な 14 桁は Estabelecimentos の
  ordem 4 桁 + DV 2 桁との結合を要する）。本 namespace では normalize で base の形式（ids/cnpj-base?）を
  検証し、完全な 14 桁のチェックディジット検証は ids/cnpj14?（Receita MOD 11）として用意する。
  Estabelecimentos と結合する経路を足す際は normalize の前段で完全な 14 桁を組み立てて検証すること。"
  (:require [clojure.string :as str]
            [itonami.legal-entity.csv :as csv]
            [itonami.legal-entity.ids :as ids]
            [itonami.legal-entity.schema :as schema]))

(def columns
  "Empresas レイアウト 7 列（仕様書「Novo Layout para os DADOS ABERTOS do CNPJ — CNPJ-Metadados」の
  EMPRESAS 章の順序。見出し行なし）。"

  ;; 1 CNPJ BÁSICO（8 桁） 2 RAZÃO SOCIAL 3 NATUREZA JURÍDICA 4 QUALIFICAÇÃO DO RESPONSÁVEL
  ;; 5 CAPITAL SOCIAL（クォート内にコンマ小数） 6 PORTE 7 ENTE FEDERATIVO RESPONSÁVEL
  [:cnpj-base :razao-social :natureza-juridica :qualificacao-responsavel :capital-social :porte :ente-federativo])

(def nature-enterprise-individual
  "Empresário Individual（Naturezas のコード 213-5）の行は取り込まない。実ファイルの表記はダッシュなしの \"2135\"。

  ;; razao social は個人名であるため、レコード化の前に落とす。"
  "2135")

(def attribution
  "出典：Receita Federal（RFB）CNPJ Dados Abertos。内部利用のみに正規化したもの。配布は弁護士確認とオーナー判断の後（ライセンス未確定）。")

(def ^:private personal-keys
  "個人データとして入口で落とすキー。Sócios / Estabelecimentos に含まれる個人データ列の異名も含む
   （本パーサが意図的に読むのは Empresas の 7 列だけ）。この集合のキーが入力 map に混入しても
   出力レコードには含めない。"
  #{:nome-socio :nm-socios :socio :cpf :cpf-do-socio :nome-representante :nm-representante :representante
    :cpf-representante :telefone :telefone1 :telefone2 :email :correio-eletronico :fax :faixa-etaria
    :logradouro :endereco})

(defn strip-personal
  "個人データ列のキーを入力 map から入口で落とす。Empresas の 7 列だけを残す呼び出し側の防御。"
  [m]
  (apply dissoc m personal-keys))

(defn- blank->nil [s]
  (when (and (string? s) (not (str/blank? s))) (str/trim s)))

(defn- norm-natureza [v]
  (when-let [v (blank->nil v)]
    (str/replace (str/trim v) #"-" "")))

(defn row->map
  "Empresas の 1 行（; 区切り、引用符対応）を列名の map にする。列数が 7 でなければ {:reject ...}。
  個人データ列のキーは含まない（strip-personal は normalize の入口で効く）。"

  [line]
  (let [fields (csv/parse-line (str line) \;)]
    (cond
      (nil? fields) {:reject "引用符が閉じていない"}
      (not= (count columns) (count fields)) {:reject (str "列数が " (count fields) "（期待 7）")}
      :else (zipmap columns fields))))

(defn normalize
  "Empresas の行 map を共通スキーマのレコードにする。戻り値は {:record r} / {:skip reason} / {:reject reason}。
  個人データ列は入口（strip-personal）で落とし、Natureza 2135 の行は skip する（レコード化しない）。"
  [m]
  (let [m (strip-personal m)]
    (cond
      (:reject m) m
      (not (ids/cnpj-base? (:cnpj-base m))) {:reject (str "CNPJ BÁSICO が不正: " (pr-str (:cnpj-base m)))}
      (= nature-enterprise-individual (norm-natureza (:natureza-juridica m)))
      {:skip "natureza 2135（213-5、Empresário Individual）の行"}
      (nil? (blank->nil (:razao-social m))) {:reject "razão social が空"}
      :else
      (let [base (:cnpj-base m)
            nat (norm-natureza (:natureza-juridica m))
            r (cond-> {:entity/id (str "br-cnpj:" base)
                       :entity/jurisdiction "BR"
                       :entity/name (str/trim (:razao-social m))
                       :entity/identifiers [{:scheme :br-cnpj-base :value base}]
                       ;; Empresas には situation（situacao cadastral）がない（Estabelecimentos 側）。不明。
                       :entity/status :unknown
                       :source {:id :br-cnpj :attribution attribution :modified? true}}
                nat (assoc :entity/legal-form {:scheme :br-natureza :code nat}))
            ps (schema/problems r)]
        (if (seq ps) {:reject (str/join "; " ps)} {:record r})))))

(defn parse-line
  "Empresas の 1 行（; 区切り）を正規化する。見出し行は受け付けない（1 行目はデータ）。"

  [line]
  (normalize (row->map line)))
