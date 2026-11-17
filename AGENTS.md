# AGENTS.md

- 追跡: Linear AWAI-3186（親）、AWAI-3188。workspace 全体の `AGENTS.md`（Kotoba end-user apps の共有アーキテクチャ契約、Awai Otehon の規則）に従う。
- profile は `hermes/profiles/legal-entity-collector/` に置く Itonami profile（Hermes の配置を借りた定義）。`itonami.edn` と `yakuwari.edn` の capability は一致させ、`bb check-profiles` で確かめる。
- 公開・アップロード・送信・支払い・削除・cron・git 書き込みは `:blocked` のまま。取得は `:ask`。広げるのは owner の決定。
- ソースを足すときは、先に利用条件とデータ仕様を一次資料で確認し、`sources/catalog.edn` に URL・確認日・必要なら仕様書の sha256 を書く。確認していないソースの parser は足さない。
- テストは架空データだけを使う。実在の法人の登記データを fixture にしない。
- parser と正規化は `.cljc` の純粋関数にし、ファイル・ネットワーク・時計に触れない。`bb test` と `bb fixture-run` の差分なしを PR の条件にする。
