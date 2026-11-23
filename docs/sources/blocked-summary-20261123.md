# Blocked / Implemented Summary — 法域別サマリ (2026-11-23)

- 対象: `sources/catalog.edn` の 19 エントリ（GLEIF ＋ 18 法域）。
- 根拠の出どころ: 既存 `sources/catalog.edn` の各エントリ（`:status` / `:blocked-reason` / `:terms-verified-on` / `:notes`）、PR #5（closed、ブランチ `com-junkawasaki/awai-3224-remaining` `8ea87ce`、本文 2026-11-23）、Organizer 共有の Legal NZ 回答（2026-11-23）。新しい一次資料の裏付けは本文書のためには取りに行っていない（BR/IL のアーカイブ試行のみタスクとして実施）。
- 状態は既存 catalog の `:status` を変えていない。blocked の行は catalog と同じ理由を転記し、出典と日付を添えた。推測で埋めていない。

## 個人データ方針（Legal、2026-11-23）

個人データ（役員名・個人事業者・住所）はライセンスとは別判断であり、初期スコープは法人の基本情報に限定する。役員名・個人事業主は初期スコープに入れない。

## 表（法域ごとに1行、各行に根拠と日付）

| 法域 | 状態 | 根拠（出典） | 根拠の日付 | 次のアクション |
| --- | --- | --- | --- | --- |
| GLEIF | implemented | catalog `:status :implemented`、CC0 1.0、`:terms-verified-on`、API 応答（lei / entity / registration）確認済み | 2026-11-18（catalog） | なし（実装済み） |
| JP | implemented | catalog `:status :implemented`、公共データ利用規約（第 1.0 版）、項番 36 は `:notes` で除外記録 | 2026-11-18（catalog） | なし（実装済み） |
| GB | implemented | catalog `:status :implemented`、Companies House 公開登記情報は制約なし（GOV.UK 2019-08-09 更新）、`:notes` で「役員の情報は含まない」 | 2026-11-18（catalog） | なし（実装済み） |
| FR | implemented | catalog `:status :implemented`、Licence Ouverte 2.0、`:notes` で personne physique（1xxx）と部分公開（statutDiffusion P）を除外 | 2026-11-18（catalog） | なし（実装済み。CSV 版は 2027 年後半に Parquet のみになる告知あり） |
| NO | implemented | catalog `:status :implemented`、NLOD 2.0、`:notes` で ENK と roller を除外 | 2026-11-18（catalog） | なし（実装済み） |
| DK | blocked | catalog `:blocked-reason`（現行利用条件を一次資料で未確認。非公式の写しには機械的・体系的収集の禁止条項）＋ PR #5 本文: Vilkår v4 は無償・商用可・出典表示だが system-til-system の申請（書類・約3週・Basic auth）とログイン後の文書モデル確認待ち | 2026-11-23（PR #5 本文 / catalog） | 申請 → 文書モデル確認（reklamebeskyttelse / personkreds の除外確認）→ parser |
| FI | implemented | catalog `:status :implemented`、OpenAPI 定義の license が CC BY 4.0 | 2026-11-18（catalog） | なし（実装済み） |
| EE | blocked | catalog `:blocked-reason`（CC BY 4.0 確認済み。基本データ CSV の列名を一次資料で未確認。ダウンロードには同意が要る）＋ PR #5 本文: yhtandmed CSV の 17 列を実ファイル先頭行で確認、実装済みとあるが catalog の `:status` は `:planned` のまま（本 PR では状態を変えない） | 2026-11-18（catalog）/ 2026-11-23（PR #5 本文） | 列名の一次資料確認後、状態変更は別途判断（本 worker では変えない） |
| BE | blocked | catalog `:blocked-reason`（KBO Open Data は登録と同意が要り、商用再利用は有償とする二次情報あり。公式条件本文が未確認）＋ PR #5 本文: ライセンス無償だが登録必須・目的限定・変更禁止・譲渡禁止、費用は年 30,000 EUR ではなかった | 2026-11-23（PR #5 本文 / catalog） | owner 判断待ち（Organizer が item 20 で確認中）。ライセンス accept と利用目的の申告要否。手続きは開始しない |
| CZ | blocked | catalog `:blocked-reason`（財務省は「運用条件に従えば誰でも使える」とするが、運用条件本文とライセンスを未確認）＋ PR #5 本文: 無償・500 件/分・キー不要、OpenAPI v3 で項目名確認、実装済みとあるが catalog の `:status` は `:planned` のまま（本 PR では状態を変えない） | 2026-11-18（catalog）/ 2026-11-23（PR #5 本文） | 状態変更は別途判断（本 worker では変えない） |
| CH | blocked | catalog `:blocked-reason`（opendata.swiss には一部しかなく、Zefix REST API の利用条件を未確認）＋ PR #5 本文: OGD Open use・出典表示必須、申請制 Basic 認証（`:key-required? true`）、実装済みとあるが catalog の `:status` は `:planned` のまま（本 PR では状態を変えない） | 2026-11-18（catalog）/ 2026-11-23（PR #5 本文） | 状態変更は別途判断（本 worker では変えない） |
| NL | blocked | catalog `:blocked-reason`（無償のオープンデータは BV・NV の属性だけで会社名と KVK 番号を含まない。名称を含む API は有償）＋ PR #5 本文: 有償 API は勝手に使わない | 2026-11-23（PR #5 本文 / catalog） | owner 判断待ち（Organizer が item 20 で確認中）。有償 API の契約要否。勝手に契約しない |
| US | implemented | catalog `:status :implemented`、SEC.gov の公開情報（出典として SEC を示す。印章・ロゴ・EDGAR 商標は使わない）、`:notes` で個人 filer を除外 | 2026-11-18（catalog） | なし（実装済み） |
| CA | implemented | catalog `:status :implemented`、Open Government Licence - Canada（連邦法人だけ。英語版 4 ファイル）、取締役の人数の列は取り込まない | 2026-11-18（catalog） | なし（実装済み） |
| BR | blocked | catalog `:blocked-reason`（ライセンス記載が二次情報で食い違う。dados.gov.br と Receita Federal の一次資料を取得できなかった。Empresas には個人事業主（MEI など）の氏名が入る）＋ 2026-11-23 アーカイブ試行（下記） | 2026-11-23（catalog / 本タスク試行） | ファイル単位のライセンス分離を続け、ND の改変（スキーマ変換）可否を Legal / owner へ食確認。取得はしない |
| AU | implemented | catalog `:status :implemented`、CC BY 3.0 AU（data.gov.au の CKAN package_show で確認）、`:notes` で個人事業主（LegalEntity / IND）等を除外 | 2026-11-18（catalog） | なし（実装済み） |
| NZ | blocked | catalog `:blocked-reason`（BDAA は申請しない（owner 判断 2026-11-23）、NZBN API 経路のみ。利用条件は一次資料で部分確認済み（署名済み MBIE API Access Agreement と購読申請・審査が要る、料金なし、表示義務。再配布の範囲とレート制限は未確認。条件確定まで取得しない。マーケティング・リード獲得用途には用いない）＋ 下記「NZ の節」（MBIE API Access Agreement 条項引用、2026-11-23 確認） | 2026-11-23 | 取得は条件確定後（AWAI-3894）。申請・契約・支払い手続きは開始しない |
| ZA | blocked | catalog `:blocked-reason`（公式の一括データ・公開 API が見つからない。CIPC の利用条件が機械的な一括取得を禁じ、再販にはライセンスが要る。オープンデータポータル準備中の報道 2026-07、公開待ち） | 2026-11-23（catalog の記録日。本文中の報道は 2026-07） | 公開待ち。新しい一次資料が出ればその時点で再確認 |
| IL | blocked | catalog `:blocked-reason`（data.gov.il の利用条件の本文（出典の示し方・商用利用）を一次資料で取得できなかった（terms ページが 403/404）。列名は datastore_search で確認済み）＋ 2026-11-23 アーカイブ試行（下記） | 2026-11-23（catalog / 本タスク試行） | 利用条件本文の取得。取得・実装はしない |

## BR のアーカイブ試行結果（2026-11-23、条件ページの取得に限る。データセット本体の取得はしていない）

試行の順序（各法域1回ずつ）: 1) Wayback Machine、2) 公的経路1つ。試行日は 2026-11-23。snapshot の日付が古い可能性があり、snapshot は「snapshot」として日付付きで引用する（live ページとして扱わない）。

### BR: Wayback snapshot で取れた（ファイル単位で分離）

1. **dados.gov.br 側（CNPJ データセットのカタログページ）** — snapshot `https://web.archive.org/web/20261105231346/https://dados.gov.br/dados/conjuntos-dados/cadastro-nacional-da-pessoa-juridica---cnpj`（snapshot 日付 **2026-11-05**。ページ内の「Licença: Creative Commons Attribution」は snapshot 上の表示。stale の可能性あり）。意味: CNPJ データセットのメタデータ中のライセンス表示は CC BY（版の明示は snapshot 表示にはなし）。この1行だけを「CC BY」として引用する。
2. **Receita Federal 側（サイトのフッタ注記）** — snapshot `https://web.archive.org/web/20261027201907/https://www.gov.br/receitafederal/pt-br/assuntos/orientacao-tributaria/cadastros/cnpj`（snapshot 日付 **2026-10-27**）および `https://web.archive.org/web/20261117152443/https://www.gov.br/receitafederal/pt-br/acesso-a-informacao/dados-abertos`（snapshot 日付 **2026-11-17**）のフッタ: 「Todo o conteúdo deste site está publicado sob a licença Creative Commons Atribuição-SemDerivações 3.0 Não Adaptada.」＝ **CC BY-ND 3.0**。文言の対象は「este site」の内容であり、CNPJ データセットのファイル自体への拡張は snapshot だけでは確認できない。

**ファイル単位の分離（データセット全体を1つのライセンスとして扱わない）**:

| 対象（どのライセンスの下のどのファイルか） | ライセンス表示（出典） |
| --- | --- |
| dados.gov.br 上の CNPJ データセットのカタログメタデータ（`Licença: Creative Commons Attribution`） | CC BY（出典: snapshot 2026-11-05、版の明示なし） |
| www.gov.br/receitafederal のサイトコンテンツ（CNPJ ページ・Dados Abertos ページ本体） | CC BY-ND 3.0（出典: snapshot 2026-10-27 / 2026-11-17 のフッタ注記。対象は "Todo o conteúdo deste site"） |
| CNPJ の ZIP などのデータファイル自体のライセンス | snapshot だけでは確認できていない（未確認）。推測で埋めない |

**要確認（Legal / owner）**: ND（CC BY-ND 3.0）は改変（スキーマ変換・他データとの結合）を禁じるため、ND 下のデータが共通スキーマ化の対象になる場合、スキーマ変換そのものが条件違反になり得る。**ND 下のデータを採用する場合は「改変の可否（スキーマ変換可否）」を Legal / owner への未確認・要確認事項として扱う。** 本 worker ではこの判断を埋めていない。なお Ferreira…（Empresas の個人事業主（MEI など）氏名の除外規則も要る — 個人データ方針参照）。

## IL のアーカイブ試行結果（2026-11-23）

1. **Wayback の試行**: 利用条件ページ `https://data.gov.il/terms` の snapshot（availability API は available と返すが、curl 取得・ブラウザレンダリングとも本文は JS シェルで表示されず空だった。2026-06-06 snapshot 等）。sitemap の snapshot（`https://data.gov.il/dataset/ica_companies`、2026-08-09 snapshot）も同様に本文が取得できなかった。
2. **公的経路1つ（data.gov.il の CKAN API）**: `https://data.gov.il/api/3/action/package_show?id=ica_companies` を1回当てた。返ってきたライセンス欄は `license_id: other-open` / `license_title: Other (Open)`、`license_url` は `None`。org は `ministry_of_justice`。リソースは CSV（`רשימת החברות`）と Excel 用ヘルプ PDF。利用条件の**本文**（出典の示し方・商用利用の可否）は確認できなかった。

**確定**: IL の利用条件本文は **未取得（403/404、2026-11-23 試行）**。CKAN のライセンス欄（Other (Open)）が確認できたことは記録するが、条件本文の裏付けにはならない。ライセンス欄の追加確認が進まない間は、それ以上追わず、`:status` は `:planned` のまま。

## NZ の節: NZBN API 経路のみ（BDAA は申請しない。利用条件の確認結果）

当面は NZBN API 経路のみ。BDAA（Bulk Data Access Agreement）は申請しない（owner 判断 2026-11-23）。NZ 一括データ取得側の Bulk Data 経路（BDAA 条項の詳細：個別契約、non-sublicensable、直接マーケティング禁止、終了時削除、監査、組織名義の Authorised Person 要件）は本節では扱わず、設計制約の最新版は ADR-001（PR #8）を参照（重複は ADR 側に集約）。契約・登録・支払い手続きは開始していない。データセットの取得はしていない。

出典（一次資料、すべて 2026-11-23 確認）:

1. NZBN API 紹介ページ（live）: https://portal.api.business.govt.nz/api/nzbn
2. MBIE API Access Agreement（Version: November 2022）: https://support.api.business.govt.nz/s/article/terms-and-conditions の配布文書（Salesforce ContentDistribution、15/11/2022）
3. support 記事: `terms-and-conditions`（2023-08-20）、`cloud-subscriptions`（2022-05-19）
4. API 定義表示（公開分）: https://portal.api.business.govt.nz/api-details#api=nzbn

確認項目（2026-11-23 JST）:

- **登録要件：確認済み。** 署名済み MBIE API Access Agreement の提出が購読承認の前提（「A signed MBIE API Access Agreement is required before an API subscription request will be approved」）。ポータル登録＋ RealMe ログイン＋ NZBN API Product への購読申請。審査は「1-2 business days」（cloud-subscriptions）。追加情報の要求があり得る（NZBN ページ「Our API support team may request further information before the subscription can be approved」）。承認後、sandbox / production 別の subscription key を発行。公開情報の検索は subscription key のみで可。情報の更新・非公開データの参照には（3-legged）OAuth と権限確認が必要。料金はなし（NZBN ページ「There is no fee for using this API」）。
- **商用利用：確認済み（条文の範囲内）。** 商用利用の明示的禁止条項は確認できなかった。Clause 4.1 は Customer Site への表示に限る royalty-free ライセンス（non-exclusive / non-transferable / non-sub-licensable）。Clause 4.7 は Registers から無料で得られるデータへの顧客課金は「reasonable fee」に限るとし、合理的課金が想定されている。ただしライセンスの範囲は 4.1 の表示に限られる。マーケティング・リード獲得用途に NZ の抽出項目を用いない方針は BDAA を使わない場合も据え置く（設計制約）。
- **再配布：未確認（Legal 確認待ち）。** Clause 4.2 は「at your own risk」で MBIE Content への第三者アクセス提供を予定するが、4.1 のライセンス範囲は Customer Site 表示に限られ、API の他者提供は 4.10(c) で禁止され、第三者への義務の流れ込み（4.11）・第三者からの MBIE への直接報告（6.1(c)）が条件となる。データセットとしてのダウンロード・再配布の可否は条文だけでは確定できない。NZBN API 由来データの再配布可否は本節では未確認とする。
- **表示義務：確認済み。** Clause 4.8 ＋ Schedule 2: データセットに「Data sourced from the [Register name]」の形式の出所表示と検索日時を含める。Schedule 1 の Excluded Websites（7カテゴリ）での表示は禁止（4.4）。この義務は契約が根拠ではなく、出所表示は公的データ一般原則としても行う。
- **レート制限：未確認。** 同意書・NZBN API 案内・API 定義（公開分）には数値制限の記載がなかった。サブスクリプション画面のプラン確認はログインが必要で本タスクでは未実施のため、数値制限は未確認。監査・監視・停止の権限は確認済み（4.9、7.2–7.3、16.1、17.3–17.4）。
- **その他（参考）:** Clause 4.3 は法令・Privacy Act 2020 の遵守を求める。Clause 18.2: 終了時に MBIE Content の消去義務。Clause 5.1: 料金は API website 掲示で追加され得る（現状 NZBN は無料）。Clause 17.3: MBIE は 60 日通知で API を停止し得る。個人データ（役員等）は引き続き初期スコープ外（法人基本情報のみ）。

上記のとおり、利用条件の一部（登録要件、料金、表示義務、乃至商用利用における課金の合理性の制約）は一次資料で確認できたが、再配布の範囲とレート制限は未確認のため、catalog `:terms-verified-on` は `nil` のまま（EE / NL の前例と同じ扱い）。条件確定まで取得しない。NZ データの取得は AWAI-3894。

## NZ / BE / NL の留保（owner 判断待ち。いずれも手続きは開始していない）

- **NZ**: NZBN API 経路のみ（BDAA は申請しない。owner 判断 2026-11-23）。詳細は上記「NZ の節」。条件確定まで取得しない。手続きはしていない。
- **BE**: ライセンス accept と利用目的の申告要否。owner 判断待ち（Organizer が item 20 で確認中）。手続きはしていない。
- **NL**: 有償 API の契約要否。owner 判断待ち（Organizer が item 20 で確認中）。勝手に契約していない。

## 次の一手（AWAI-3894 への送り先）

- AWAI-3894（実データ取得）は Backlog のまま。本 worker はデータセットの取得・取得クライアントの実装をしていない。
- 次のタスクで引き継ぐべき未解決の論点: NZ の取得（NZBN API 経路のみ。詳細は「NZ の節」参照。AWAI-3894 で着手する際は再配布の範囲とレート制限の確認から。BDAA 申請は保留）、BE のライセンス受け入れ、NL の有償 API 契約、BR の CC BY-ND の改変（スキーマ変換）可否、IL の利用条件本文の未取得確定、EE/CZ/CH の catalog 状態と PR #5 本文の差異（本 worker では `:status` を変えていない）。
