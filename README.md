# app-itonami-legal-entity-collector

公開一次情報（各国の法人登記・GLEIF）から法人データを集め、出典つきの共通スキーマに正規化する Itonami profile です。D&B・帝国データバンク・Moody's の契約データに頼らない、Awai Network の自前の企業データ基盤の収集側にあたります（Linear AWAI-3186）。

## いまあるもの（2026-11-23 時点）

| 部分 | 状態 |
|---|---|
| 共通スキーマ `src/itonami/legal_entity/schema.cljc` | 実装・テスト済み |
| 識別子の検証（法人番号のチェックデジット、LEI の ISO 7064 MOD 97-10） | 実装・テスト済み（実在の値 2 件で検算） |
| GLEIF（API の lei-records 形式、CC0 1.0） | 実装・架空データでテスト済み |
| 国税庁 法人番号（全件・差分 CSV、リソース定義書 4.1 版の 30 列） | 実装・架空データでテスト済み |
| 英国 Companies House・フランス SIRENE・ノルウェー Brønnøysund・フィンランド PRH（AWAI-3224） | 実装・架空データでテスト済み。個人事業主・部分公開は取り込まない |
| エストニア e-Business Register（CC BY 4.0、lihtandmed CSV、;区切り） | 実装・架空データでテスト済み。個人事業主は取り込まない |
| デンマーク・ベルギー・チェコ・スイス・オランダ | 受け付けない。確認できなかった理由は `sources/catalog.edn` の `:blocked-reason` |
| 米国 SEC EDGAR・カナダ Corporations Canada・オーストラリア ABR（AWAI-3225） | 実装・架空データでテスト済み。個人の filer・個人事業主・信託は取り込まない。ABR の XML から map への変換は未実装 |
| ブラジル・ニュージーランド・南アフリカ・イスラエル | 受け付けない。確認できなかった理由は `sources/catalog.edn` の `:blocked-reason` |
| 取得（ダウンロード） | 未実装。parser は取得済みの入力を受け取るだけ |
| profile `hermes/profiles/legal-entity-collector/` | 定義のみ。runtime での実行は未検証。cron 無効 |

実データでの取得・件数の確認はまだしていません。

## 使い方

```bash
bb test            # 全テスト（ネットワークに出ない）
bb fixture-run     # 架空データで collector を通した記録を出す
bb check-profiles  # profile の権限定義を検査する
```

## 出典表示

- 国税庁: 出典：国税庁法人番号公表サイト（国税庁）（https://www.houjin-bangou.nta.go.jp/）をもとに Awai Network が加工して作成
- GLEIF: Contains LEI data from GLEIF (https://www.gleif.org/), CC0 1.0. Normalized by Awai Network.

## ライセンス

Apache-2.0。収集したデータには各ソースのライセンスが適用されます。
