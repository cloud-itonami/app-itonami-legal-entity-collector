# BR — Receita Federal CNPJ（Cadastro Nacional da Pessoa Jurídica）オープンデータ条件 (2026-11-23)

- 対象: `sources/catalog.edn` の `:id :br-cnpj`（`:status :planned`、`:terms-verified-on nil` 維持）。
- 本書は**条件確認のみ**。登録情報の取得（ダウンロード・照会）はしていない。クライアントも書いていない。認証情報は持っていない。
- 確認日: **2026-11-23**（JST）。全項目は一次資料を実際に開いて裏付けを取り、URL を添えた。推測で埋めていない。
- 関係: PR #19（`docs/sources/br-cnpj-terms-20261124.md`）が同じ調査テーマの条件確認1枚を出している。本書は同プログラムのこのタスクが求める形式（`br-cnpj.md`＋`catalog.edn` への1–2行追加）で書いたもの。結論の食い違いはない。

## 7項目（タスク指定の順に）

### 1. 一次資料とファイル構成

一次資料（RFB 公式）:

- リポジトリ入口「Repositório de Dados Abertos da RFB」: `https://www.gov.br/receitafederal/dados`
- ファイル実在場所（RFB の SERPRO+ 公開共有）: `https://arquivos.receitafederal.gov.br/index.php/s/YggdBLfdninEJX9`
- 仕様書「Novo Layout para os DADOS ABERTOS do CNPJ — CNPJ-Metadados」（6頁、59,315 bytes、sha256 `0a52d6bdfb61a07425e352a0e692932306a5ec9ecad682f5f9e28059e1a5fce0`）: `https://www.gov.br/receitafederal/dados/cnpj-metadados.pdf/@@download/file`
- 機密性評価「Nota Técnica RFB/COCAD nº 47/2024」（2024-05-13、86/2024 で item 5・6 を `（NR）` 置換）: `https://www.gov.br/receitafederal/dados/nota_cocad_no_47_2024.pdf`
- 連邦法令「Decreto nº 8.777/2016」（Dados Abertos 政策。9.903/2019 改正後）: `https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2016/decreto/D8777.htm`

二次資料として区別するもの:

- Portal Brasileiro de Dados Abertos（dados.gov.br）「Cadastro Nacional da Pessoa Jurídica - CNPJ」: `https://dados.gov.br/dados/conjuntos-dados/cadastro-nacional-da-pessoa-juridica---cnpj` — RFB が登録したカタログ情報であり、一次資料ではない。なお RFB の CNPJ ページ（`…/dados-abertos/cadastros/cnpj`）はこのページへリダイレクトする（2026-11-23 に curl で確認）。

月次フォルダ `AAAA-MM` に、2026-11 フォルダで 36 ファイル（WebDAV のフォルダ自身を除く）がある。仕様書の項目定義とファイル名から、各ファイルが持つもの:

| ファイル | 持つもの（仕様書の項目定義どおり） |
| --- | --- |
| `Empresas0-9.zip`（10本） | 法人単位: CNPJ básico（先頭8桁）、razão social / nome empresarial、natureza jurídica、qualificação do responsável、capital social、porte、ente federativo responsável（1XX のみ） |
| `Estabelecimentos0-9.zip`（10本） | 事業所単位: CNPJ 14桁、matriz/filial、nome fantasia、situação cadastral（01=nula 2=ativa 3=suspensa 4=inapta 08=baixada）＋日付・motivo、住所一式（logradouro〜CEP・UF・município・国）、telefone・e-mail・fax、situação especial、CNAE principal/secundárias |
| `Socios0-9.zip`（10本） | 株主・管理者単位: sócio の識別子（1=PJ 2=PF 3=estrangeiro）、氏名（PF）/razão social（PJ）、CNPJ/CPF、qualificação、国のコード、入社日 |
| `Simples.zip`（1本） | Simples Nacional / MEI の option・exclusão の指標と日付 |
| 補助表 `Cnaes.zip` / `Municipios.zip` / `Motivos.zip` / `Naturezas.zip` / `Paises.zip` / `Qualificacoes.zip`（6本、合計68KB未満） | ファイル名からコード表とみられるが、内容は ZIP を開いていないため**未確認** |

### 2. Credentials: 不要

公開共有リンクで、アカウントなしに一覧・サイズ取得できることを 2026-11-23 に直接確認した（WebDAV PROPFIND）。登録・署名済み契約・購読申請の要求は一次資料のいずれにもない。Decreto 8.777/2016 art. 3º II は「acesso irrestrito」（無制限アクセス）を政策原則としている。共有リンクのトークン（`YggdBLfdninEJX9`）は公開リンクの識別子であり、個人アカウントの認証情報ではないため repo に秘匿情報として記録しない。

### 3. コスト: 無料

一次資料の根拠:

- Decreto 8.777/2016 art. 4º §1º（9.903/2019 改正）: 「utilização gratuita das bases de dados… cujo detentor… seja a União」— 料金表・請求フローの存在は一次資料に見当たらない。
- Nota Técnica 47/2024 item 10: 「dados públicos, de livre acesso a qualquer interessado」。

### 4. Form: bulk（月次 ZIP）。全体取得は非推奨

- 形態は bulk が第一候補（API の存在・独立したキー不要の公開 API の有無は本確認では未確認。下記「確認できなかったこと」）。
- サイズ（2026-11 フォルダの WebDAV `getcontentlength` 合計。ZIP 本体は取得していない）: 36 ファイル **7,870,468,170 bytes ≒ 7.87 GB（7.33 GiB）**。内訳は `Estabelecimentos0.zip` が最大 **2.32 GB**、`Empresas0.zip` 0.58 GB、`Socios0.zip` 0.26 GB、`Simples.zip` 0.31 GB、その他は 1本あたり 50MB〜370MB。補助表6本は合計 68,978 bytes。
- 推奨: **全量の月次取得はしない**。取得する場合は照会ベース（単一事業所・少数件）か、分割ファイル1本（必要な表のみ）に留める。月次スナップショットは 7GB 級を毎月繰り返すため、鮮度要求が明確でない限り経済性がない。

### 5. ライセンス / 出典表示

- **no explicit license found**（ファイル自体のライセンス文言が一次資料になし）。ZIP ファイル自体のライセンスは RFB ページ・仕様書・Nota のいずれにも見当たらない。よって **terms not verified** として扱う。
- 並行して存在する表記（いずれもファイル単位の確定には使わない）:
  - Portal（二次）メタデータ: 「Licença: Creative Commons Attribution」（版の明示なし。`https://dados.gov.br/dados/conjuntos-dados/cadastro-nacional-da-pessoa-juridica---cnpj` で 2026-11-23 確認）。
  - RFB サイトフッタ（一次）: 「Todo o conteúdo deste site está publicado sob a licença Creative Commons Atribuição-SemDerivações 3.0 Não Adaptada。」= CC BY-ND 3.0。対象は "este site" の内容であり、ZIP ファイルへの拡張は一次資料で確認できていない。ND が適用される場合、スキーマ変換（共通スキーマ化）が改変にあたる可能性があるため、**ND 下での加工可否は Legal / owner 確認待ち**のまま。
- 出典表示: ライセンス確定まで、RFB が出典である旨の表示を catalog の `:attribution` に置く。表示義務の確定ではない（方針としての記録）。

### 6. 個人データ（出所: 仕様書＋Nota 47/2024 item 5）

CNPJ は法人番号だが、bulk には個人に結びつく情報が含まれる。RFB は item 5 の一覧を「sigilo fiscal の保護を受けないデータ」としつつ、item 9 で他の個人・経済データ源との結合で私生活露出しうるため保持者は慎重に扱うよう警告している（cuidado と zelo）。**個人データはライセンスとは別判断であり、初期スコープは法人の基本情報に限定する。**

初期スコープから外す個人結びつきフィールド:

- `Socios` の **nome do sócio pessoa física**（PF 株主の氏名）
- `Socios` の **CNPJ/CPF do sócio**（CPF は先頭3桁＋DV が不可読化（descaracterizado）されている。Nota item 6。ただし不可読化されても個人識別力の判断は Legal）
- `Socios` の **faixa etária do sócio/administrador**（CPF 生年月日から導出の年齢帯）
- `Socios` の **nome do responsável legal** と **CPF do responsável legal**、**qualificação do responsável legal**
- `Empresas` の **qualificação do responsável**（同上。代表者の属性）
- `Simples` の **MEI option/exclusão** に結びつく行（MEI は個人事業主の制度。MEI の行は初期スコープ入れない）
- `Empresas` に含まれる **Empresário Individual（個人事業主）の氏名** — ZIP の中身を開いていないため「含まれるか」は**未確認**。含まれる場合は法人名として扱わず個別対応を要する
- `Estabelecimentos` の **telefone・e-mail**（事業所連絡先。MEI・個人事業主では個人連絡先と重なる場合があるため初期スコープでは入れない）

### 7. 確認できなかったこと（一覧。推測しない）

1. ファイル単位ライセンス（CC BY / CC BY-ND 3.0 の射程、ND 下のスキーマ変換可否）
2. 再配布・第三者提供・商用利用・マーケティング/リード獲得用途の可否
3. CSV の区切り文字・文字コード（仕様書の抽出範囲では確認できていない）
4. 補助表6本（Cnaes/Municipios/Motivos/Naturezas/Paises/Qualificacoes）の中身
5. 単一照会チャネル（Consulta CNPJ の Web サービス・REST）の無料・キー不要の可否。API の存在は一次資料では本確認で未確認のため、bulk を第一候補、照会は代替案として再確認
6. レート制限・鮮度保証（RFB 側の数値・明記がないため取得計画に数値を置かない）
7. Empresas の razão social に Empresário Individual の個人名が含まれるか（ZIP 本体未取得）

## catalog 側の扱い

`catalog.edn` の `:br-cnpj` は `:status :planned` のまま、`:terms-verified-on nil`（条件全体の確定を意味するため、未確認項目が残る限り nil）。追加したキー（`:url` / `:terms-url` / `:attribution` / `:access-conditions`）は一次資料へのポインタであり、parser を追加する条件を満たしたことにはならない。`:blocked-reason` は未確認事項の名前で更新した。
