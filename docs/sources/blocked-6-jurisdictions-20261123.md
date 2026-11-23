この文書は新規取得をしていない。既存の `sources/catalog.edn` と PR #5 branch の記録から写した。

正本: `cloud-itonami/app-itonami-legal-entity-collector` (repo ID `1461722115`)。`network-awai/...` は transfer redirect で同一 repo (ID と blob SHA の一致で確認)。

対象 repo `cloud-itonami/app-itonami-legal-entity-collector` (repo ID 1461722115、URL `https://github.com/cloud-itonami/app-itonami-legal-entity-collector`、main HEAD `54fc89fb2de13254456d3ea11c1c1b975fe19229`)。`network-awai/...` は同一 repo への GitHub transfer redirect で、API でも同一 repo ID を返します (重複 repo はない)。
`main` の `sources/catalog.edn` は6法域すべて `:planned` + `:blocked-reason` のまま。
PR #5 (https://github.com/cloud-itonami/app-itonami-legal-entity-collector/pull/5、head `8ea87ced70194abad90cb485fef32edca085235e`、CLOSED、マージなし、branch `com-junkawasaki/awai-3224-remaining` は残存) は once の一次資料確認でしたが close されており、再評価は前提が整ったときです。

| 法域 | main の状態 | blocked 根拠 (一次資料の日付つき要約) | 次にやること |
|---|---|---|---|
| DK CVR | blocked | 規約は可 (Vilkår v4、無償・商用可・出典表示) が、system-til-system は Erhvervsstyrelsen への申請 (書類、処理約3週間) と Basic 認証発行が必要。Elasticsearch の文書モデルはログイン後しか見られず、reklamebeskyttelse と personkreds の除外を一次資料で確認できない。 | 申請 → 文書モデル確認 → parser。鍵は repo に入れない。 |
| EE e-Business Register | blocked (候補実装は PR #5) | 規約可 CC BY 4.0 (2026-11-23 確認)。lihtandmed CSV の見出し 17 列を実ファイル先頭行で確認、`:spec-sha256` は下記コードブロック (2026-11-23 版)。区切り `;`、BOM 付き。 | 3894 の設計で再評価。個人事業主 (Füüsilisest isikust ettevõtja) は除外。 |
| BE KBO | blocked | 規約不可・owner 判断要。KBO は無償 (Art. 7、年 30,000 EUR の話は誤り) だが、登録とライセンス accept 必須、利用目的は登録時申告に限定 (Art. 2.3)、変更禁止・原形的保持 (Art. 2.7)、譲渡・下請負禁止 (Art. 11)、知的財産権は付与されない。 | 2026-11-23 owner 判断: 保留（ライセンス締結と用途申告は保留。無償分を先に仕上げる） |
| CZ ARES | blocked (候補実装は PR #5) | 規約可 (運用条件に従って無償、登録・キー不要、出典として ARES)。上限 500 件/分、ブロック条件は mf.gov.cz で確認。OpenAPI v3 (下記コードブロック、@version 1.4.0) で項目名確認。 | 3894 の設計で再評価。個人事業主 (100-109・424・425) は除外。 |
| CH Zefix | blocked (候補実装は PR #5) | 規約可 OGD Open use (商用可・出典表示必須)。ただし API は `zefix@bj.admin.ch` への申請で発行される Basic 認証、`key-required? true`。v3/api-docs 2.7.2.4 (下記コードブロック)。全件取得は提供されず単一照会中心。 | 3894 の設計で再評価。個人事業 (0101) は除外。申請と Basic 認証の扱いは owner 判断 + 認証を repo に書かない。 |
| NL KVK | blocked | 規約不可・owner 判断要。無償データ (CC BY 4.0) は会社名と KVK 番号を含まず、共通スキーマの名称を満たせない。名称を含む API は有償 (2026-11-23 確認)。 | 2026-11-23 owner 判断: 保留（有償 API の契約は保留） |
| NZ (NZBN) | blocked (検討中) | NZBN API 経路のみを使う。NZBN API の利用条件は未確認のため、条件が一次資料で確認できるまで実データ取得はしない。 | 3894 の前提が整った後、NZBN API の利用条件を一次資料で確認 → catalog の `:blocked-reason` 更新 |

`:spec-sha256` (EE):

```
5672ca914ace30154d0503468ceee9190c4614560191ce5900c7fb3d55220453
```

`:spec-sha256` (CZ):

```
822372bcc181740c1a0f43d0507c204780f575c7ddea837a65064a07a0819134
```

`:spec-sha256` (CH):

```
ff8e72380dcb0dbde910919154f5295051b0006a12513195afe7d8d110781450
```

補足:
NZ: BDAA は申請しない（owner 判断 2026-11-23）。NZBN API 経路のみを使う。API の利用条件は未確認のため、条件が一次資料で確認できるまで実データ取得は行わない。
NZ の抽出項目はマーケティング・リード獲得に用いない（BDAA 4.4(3) 由来の方針として据え置き）。
