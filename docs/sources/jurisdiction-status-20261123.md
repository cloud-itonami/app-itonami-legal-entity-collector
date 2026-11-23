# planned 8 法域の条件表（2026-11-23 時点）

`sources/catalog.edn` の `:status :planned` 8 法域について、**一次資料で確認できた条件だけ**を1行1法域で並べる。**推測で埋めた欄は無い**（確認できなければ「未確認」）。実装はまだしない。

この表の目的は、**「資格情報不要のものから着手する」**（オーナー方針 2026-11-23）を判断できるようにすること。

## 表

| 法域 | 一次資料 URL | 資格情報 | 費用 | 形態 | ライセンス・利用条件 | 個人データ | 判定 |
|---|---|---|---|---|---|---|---|
| **DK** CVR | `https://datacvr.virk.dk/artikel/system-til-system-adgang-til-cvr-data` | **要る**（Erhvervsstyrelsen への system-til-system 申請。書類・処理約3週。Basic 認証） | 無料 | API（system-til-system）。Elasticsearch の文書モデルはログイン後のみ | Vilkår v4（2015-03-10、無償・商用可・出典表示）。`reklamebeskyttelse`（広告保護）と `personkreds`（個人参加者）の除外は文書モデル確認が必要 | `personkreds` の除外フィールド未確認 | **`:planned` 据え置き**（申請しない） |
| **BE** KBO | `https://economie.fgov.be/nl/themas/ondernemingen/kruispuntbank-van/diensten-voor-iedereen/hergebruik-van-publieke/kruispuntbank-van-0` | **要る**（登録＋ライセンス同意。利用目的は登録時申告に限定 Art. 2.3） | 無料（Art. 7。**「年 30,000 EUR」は一次資料で確認できず**、年額の論点ではない） | API／ダウンロード（登録後） | 無償だが**データを変更しない・原形を保つ**（Art. 2.7）、**譲渡・下請負禁止**（Art. 11）、IP 権は付与されない。**正規化・共通スキーマ化に使えるかは Legal/owner 判断**（確認まで parser を足さない） | 自然人（個人事業主）を含む可能性は未確認 | **`:planned` 据え置き**（ライセンス同意が障壁） |
| **CH** Zefix | opendata.swiss の Zefix データセット／Zefix REST API | **要る**（Basic 認証が申請制） | 無料（OGD Open use、出典表示必須） | API（単一照会中心）／API 未提供のディレクトリもある | `opendata.swiss` には Zefix の一部しかなく、**REST API の利用条件（項目数・全件可否）は未確認** | 個人事業（Einzelunternehmen、eCH-0097 0101）は取り込まない | **`:planned` 据え置き**（認証が申請制） |
| **NL** KVK | `https://developers.kvk.nl` Open Dataset Basis Bedrijfsgegevens（CC BY 4.0、2026-11-18 確認） | 無償データは登録不要。**名称を含む API は有償**（2026-11-23 確認） | 無償データは無料／名称入り API は有償 | bulk（CSV）／API（有償） | 無償オープンデータ（CC BY 4.0）には**会社名と KVK 番号が含まれない**（共通スキーマの名称を満たせない） | 名称入り API は有償のため初期スコープに入れない | **`:planned` 据え置き**（有償化しない） |
| **BR** CNPJ | `https://dados.gov.br` の CNPJ データセット／`https://www.gov.br/receitafederal`（詳細は `docs/sources/br-cnpj-terms-20261124.md`） | **不要**（公開ダウンロード） | 無料 | **bulk**（巨大・全件、月次 37 ファイル **7.87 GB**／全月 **63.95 GB**） | **ライセンスが割れている**: dados.gov.br 側 `CC BY`（版の明示なし）／Receita サイトのフッタ `CC BY-ND 3.0`（改変不可）／**ZIP ファイル自体の文言は未確認**。**CC BY-ND はスキーマ変換・結合を禁じ得る** | Empresas に個人事業主（MEI 等）の氏名が入る → 除外規則が必要 | **資格情報不要の第一候補だが、ライセンス（ND と改変の解釈）が未解決のため実装しない**（Legal 待ち） |
| **NZ** NZBN | `https://portal.api.business.govt.nz/api/nzbn`／MBIE API Access Agreement（2022-11） | **要る**（署名済み Agreement の提出が購読承認の前提。RealMe ログイン＋購読申請、審査 1–2 営業日） | **料金なし**（「There is no fee for using this API」） | API（購読制） | Clause 4.1（Customer Site への表示に限定、non-exclusive / non-transferable / non-sub-licensable）、4.8＋Schedule 2（「Data sourced from the [Register name]」＋検索日時）、4.10(c)（他者への API 提供禁止）。**再配布の範囲は条文だけでは確定できない** | 役員等は初期スコープ外（法人基本情報のみ） | **NZBN API 経路のみ**（BDAA 保留）。**再配布が未確定のため取得しない** |
| **ZA** CIPC | `https://www.cipc.co.za` | **未確認** | **未確認**（二次情報では一括取得の制限・再販ライセンスが必要とされる） | 公式の一括データ・公開 API が見つからない | 一次資料で確認できず。2026-07 の報道で新オープンデータポータル準備中 | 未確認 | **`:planned` 据え置き**（一次資料待ち） |
| **IL** Rasham | `https://data.gov.il`（CKAN `package_show?id=ica_companies` → **HTTP 200**。`license_id: other-open` / `license_title: Other (Open)` / **`license_url: None`**） | **不要**（公開 CKAN。登録なしで `package_show` / `datastore_search` 可） | **未確認**（ライセンス欄が Other (Open) のため「無料と見られる」が、利用条件本文は未取得） | API（`datastore_search` で列名は確認済み） | **利用条件の本文が未取得**。実測（2026-11-23）: `https://data.gov.il/terms` → **403**、`/dataset/ica_companies?format=json` → **403**、`api/3/action/license?id=other-open` → **404**、**`license_url` が API に載っていない**。CKAN のライセンス欄は条件本文の裏付けにならない | 自然人について未確認 | **`:planned` 据え置き**（条件本文の確認が先。`license_url` が公開されるか、dataset ページの terms リンクが取得できるまで待つ） |

## Common Crawl の位置づけ（実測 2026-11-23）

```
curl -sfS https://index.commoncrawl.org/collinfo.json            → 最新 CC-MAIN-2026-43（取得日 2026-11-23）
curl -sfS "https://index.commoncrawl.org/CC-MAIN-2026-43-index?url=www.gov.br/*&output=json&limit=2"   → HTTP 504
curl -sfS "https://index.commoncrawl.org/CC-MAIN-2026-43-index?url=gov.br/*&output=json&limit=2"       → HTTP 504
curl -sfS "https://index.commoncrawl.org/CC-MAIN-2026-43-index?url=www.gov.br/receitafederal/pt-br&output=json&limit=2" → HTTP 504
（旧コレクション CC-MAIN-2026-42 でも同様に 504）
```

- **コレクション一覧は取れるが、ページレコードの index 照会は 504 が続いている**（＝現時点で帳票は取れない）。**取れていないものを「取れた」と書かない**
- CC は**公開クロールのコーパス**で、ライブのスクレイピングではない。**取得できる＝使ってよい、ではない**（元サイトの条件が別に存在する）
- **判定は一次資料で行う**。CC にしか無い情報を根拠にしない（古い・欠落）。使う場合は**コレクション ID と取得日**を記録する
- したがって **NZ の再配布可否・DK の申請条件・BE の同意条件・CH の認証条件**の判断に CC は使えない（元サイト条件との対応が取れない）

## 結論

- **資格情報不要で、かつライセンスが片付いている法域は、いま 0**。
- **BR CNPJ** が最有力（無料・キー不要・bulk）だが、**CC BY と CC BY-ND 3.0 の食い違い**と**ND 下でのスキーマ変換可否**が未解決。**Legal の判断待ち**（ライセンスの解釈を推測で埋めない）。
- **IL** は資格不要だが**利用条件本文が未取得**。**ZA** は一次資料が無い。
- **DK / BE / CH / NL / NZ** は**資格情報・契約**が要るので着手しない。

**次にやるなら**: Legal の結論が出た後の BR（分割ファイル1つ・照会ベース・4 GiB 安全弁）、または IL の利用条件本文の取得（CKAN の terms リンクを辿る）。
