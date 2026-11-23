# BR (Receita Federal CNPJ) 利用条件確認 — 条件確認1枚 (2026-11-24)

- 対象: `sources/catalog.edn` の `:id :br-cnpj`（`:status :planned`、`:terms-verified-on nil` のまま）。
- 本書は**調査のみ**。実装はしない。`sources/catalog.edn`・`src`・`test` には触らない（`:planned` のまま）。
- 追跡: Linear AWAI-3225（Done のまま、reopen しない）。
- 一次資料の確認日: **2026-11-24**（JST。GMT 2026-11-23 深夜 = JST 2026-11-24 朝に取得・計測）。
- データファイル本体（ZIP）のダウンロードは**していない**。サイズは Nextcloud 公開共有の WebDAV `PROPFIND` の `getcontentlength` で取得した値のみ（本体の取得はなし）。証跡は仕様書 PDF の sha256・サイズのみ。認証情報・API キーは本書にも repo にも書いていない。

## 結論（5点）

1. **一次資料**: RFB（Receita Federal）の公式ページが一次資料。Portal de Dados Abertos（dados.gov.br）は二次資料。ただし 2026-11-24 時点で RFB の「CNPJ」ページ（`gov.br/receitafederal/pt-br/acesso-a-informacao/dados-abertos/cadastros/cnpj`）は dados.gov.br のデータセットページへリダイレクトする。一次資料として確定したものは (a) RFB 公開リポジトリ `https://www.gov.br/receitafederal/dados`、(b) RFB ファイル共有 `https://arquivos.receitafederal.gov.br/index.php/s/YggdBLfdninEJX9`（SERPRO+）、(c) 仕様書 `cnpj-metadados.pdf`、(d) Nota Técnica RFB/COCAD 47/2024（86/2024 改正）、(e) Decreto 8.777/2016（Planalto）。データセットのメタデータ（ライセンス欄等）の読み方については下記「ライセンス」のとおり、両方の表記を並記し一方に寄せない。
2. **無料・キー不要（bulk）**: **確認済み**。Decreto 8.777/2016（Planalto）art. 3º II「garantia de acesso irrestrito… formato aberto」、art. 3º IV「permissão irrestrita de reuso」、art. 4º「de livre utilização」および art. 4º §1º（Decreto 9.903/2019 改正）「utilização gratuita」が一次資料の根拠。RFB の Nota Técnica 47/2024 item 10 は「dados públicos, de livre acesso a qualquer interessado」と明記。ファイル取得は RFB Nextcloud 公開共有（ログインなし、`PROPFIND`/`GET` でアカウントなしに一覧・サイズ取得可）で、登録・キー不要を 2026-11-24 に直接確認した。共有リンクのトークンは**公開リンク識別子**であり、個人アカウントの認証情報ではない（repo への秘匿情報の記載なし）。
3. **形態**: **bulk（CSV・ZIP、月次）** が第一候補。2026-11 フォルダは 37 ファイル・合計 **7.87 GB**（7.33 GiB）。内訳は Estabelecimentos 10 本（最大 `Estabelecimentos0.zip` 2.32 GB）、Empresas 10 本、Sócios 10 本、Simples 1 本、補助表 6 本（Cnaes/Motivos/Municipios/Naturezas/Paises/Qualificacoes、合計 68 KB）。全月アーカイブ `cnpj.tar.gz` は 63.95 GB（共有ルートに実在。取得しない）。単一照会の Web サービス（gov.br サービスカタログ「Consultar CNPJ」）は画面が JS シェルで無料文言を 2026-11-24 に live 確認できず**未確認**（下記）。
4. **ライセンス**: **両義を並記**（一覧で1行に圧縮しない）。Portal メタデータは「Licença: Creative Commons Attribution」（版の明示なし。二次資料）。RFB サイトのフッタは「Todo o conteúdo deste site está publicado sob a licença Creative Commons Atribuição-SemDerivações 3.0 Não Adaptada.」= **CC BY-ND 3.0** で、対象は「este site」の内容（live 2026-11-24 に `/dados-abertos/cadastros` と `/dados` の両方で確認）。ZIP ファイル自体のライセンス文言は RFB ページ・Nota・仕様書のいずれにも見当たらず**未確認**。ND（改変禁止）の下ではスキーマ変換（共通スキーマ化）が改変にあたる可能性があるため、**ND の改変（スキーマ変換）可否は Legal / owner 確認待ち**のままとする。
5. **個人データ**: CNPJ は法人番号だが、bulk には① Sócios の**氏名**（PF）、② Sócios の**CPF**（先頭3桁＋DV 隠蔽＝descaracterizado）、③ **代表者の氏名・CPF（同じく隠蔽）**、④ **Faixa Etária（CPF の生年月日から導出の年齢帯、0歳〜12歳〜80歳超）**、⑤ Estabelecimentos の**住所・電話・メール**、⑥ Simples の **MEI** フラグが含まれる（仕様書＋Nota 47/2024 item 5 一覧）。RFB はこれらを item 5 の一覧として「sigilo fiscal の保護を受けないデータ」とし、item 6 で CPF を隠蔽、item 9 で「他の個人・経済データ源と結合すると私生活・親密性を露出しうるため、保持者は慎重に扱うこと」と警告、item 10 で「dados públicos, de livre acesso a qualquer interessado」と分類した。**Legal の別判断**。初期スコープは法人の基本情報に限定し、役員名・個人事業主は初期スコープに入れない（catalog 方針の転記）。

## 一次資料一覧（確認日 2026-11-24）

| # | 一次資料 | URL | 区分 |
| --- | --- | --- | --- |
| P1 | RFB リポジトリ「Repositório de Dados Abertos da RFB」 | `https://www.gov.br/receitafederal/dados` | 一次（RFB 公式） |
| P2 | RFB ファイル共有「CNPJ」（SERPRO+） | `https://arquivos.receitafederal.gov.br/index.php/s/YggdBLfdninEJX9` | 一次（RFB 公式） |
| P3 | 仕様書「Novo Layout para os DADOS ABERTOS do CNPJ — CNPJ-Metadados」（6 pages、59,315 bytes、sha256 `0a52d6bdfb61a07425e352a0e692932306a5ec9ecad682f5f9e28059e1a5fce0`） | `https://www.gov.br/receitafederal/dados/cnpj-metadados.pdf/@@download/file` | 一次（RFB 公式） |
| P4 | Nota Técnica RFB/COCAD nº 47/2024（13/05/2024、197,508 bytes、sha256 `a317abd377881b97fb7c93078a02c591229f71544d9416d9729fa0da9938d498`） | `https://www.gov.br/receitafederal/dados/nota_cocad_no_47_2024.pdf` | 一次（RFB 公式・電子署名文書） |
| P5 | Nota Técnica RFB/COCAD nº 86/2024（04/07/2024、196,111 bytes、sha256 `c10d93aabb83d7d14e457689ca8aee5f17dc2a75db97198cf5baee94051e1667`）— 47/2024 の item 5・6 を `（NR）` で置換する改正文書 | `https://www.gov.br/receitafederal/dados/nota-cocad-rfb-86-2024.pdf` | 一次（RFB 公式・電子署名文書） |
| P6 | Decreto nº 8.777/2016（Planalto、Decreto 9.903/2019 改正後）art. 1º III・art. 3º I/II/IV・art. 4º caput・art. 4º §1º | `https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2016/decreto/D8777.htm` | 一次（連邦法令） |
| P7 | Conecta gov.br 連携カタログ「CNPJ – Consulta de Empresas」（v2.0.2、22/01/2021）— REST 照会の存在・認証（oauth2/jwt-token、機関 IP 登録、鍵発行）の確認。**bulk の第一候補ではない**（下記） | `https://www.gov.br/conecta/catalogo/apis/consulta-cnpj` | 一次（政府機関公式） |
| S1 | Portal de Dados Abertos（dados.gov.br）「Cadastro Nacional da Pessoa Jurídica - CNPJ」（RFB の CNPJ ページからのリダイレクト先。メタデータ: Licença=Creative Commons Attribution、更新頻度=Mensal、最終抽出 11/01/2026、Recursos 5） | `https://dados.gov.br/dados/conjuntos-dados/cadastro-nacional-da-pessoa-juridica---cnpj` | **二次**（タスク指定どおり一次資料と区別） |
| S2 | gov.br サービスカタログ「Consultar CNPJ」（REDESIM/cnpjreva 等の単一照会チャネル） | `https://www.gov.br/pt-br/servicos/consultar-cadastro-nacional-de-pessoas-juridicas` | **二次**の入口（サービスカタログ。無料文言は live で確認できず未確認扱い） |

注記:

- 2026-11-24 に RFB の CNPJ ページ（`…/dados-abertos/cadastros/cnpj`）は S1 へリダイレクトした（ブラウザで確認）。S1 の「Área técnica responsável: RFB」・連絡先（`demandas.dicaj.df@rfb.gov.br`）が示すとおり S1 は RFB のカタログ登録であるが、タスク指定に従い S1 は二次資料として扱い、一次資料側（P1–P6）と区別した。
- P1 のリポジトリは Plone 製で、ページ順覧（30 件/ページ、12 ページ超）に CNPJ の月次 ZIP は見当たらなかった。月次ファイルは P2 に実在する（2023-05〜2026-11 の AAAA-MM フォルダ。2026-11-16 に 2026-11 フォルダ更新）。P1 の「公開手順」（PBDA への catalog 登録）は確認したが、CNPJ ZIP の P1 内の場所は確認できていない（**未確認**）。
- S1 の「Recursos 5」: Nota Técnica 47/2024(PDF)、NT 86/2024(PDF)、Dicionário de dados do CNPJ(ZIP)、Inscrições no CNPJ(ZIP)、Regimes Tributários(ZIP)。このうち「Inscrições no CNPJ」は 2026-11-24 時点で S1 上「Data de atualização do arquivo: Indisponível (Erro na conexão)」と表示される（**可用性メモ**。P2 の 2026-11 フォルダは 37 ファイルを PROPFIND で直接確認した）。

## 確認表（項目ごとに一次資料で裏付け）

| 項目 | 結果 | 一次資料の根拠（条項・原文の要旨） | 確認日 |
| --- | --- | --- | --- |
| 無料（bulk） | **確認済み（無料）** | P6 art. 4º §1º: 「utilização gratuita das bases de dados…」（権原が Union の DB の無料利用を授権）。P6 art. 3º II（acesso irrestrito）・IV（permissão irrestrita de reuso）。P4 item 10: 「dados públicos, de livre acesso a qualquer interessado」。料金表・請求フローの存在は P2（公開共有）に見当たらない。 | 2026-11-24 |
| キー不要（bulk） | **確認済み（キー不要）** | P2 をアカウントなしに WebDAV PROPFIND で列挙・サイズ取得できた（2026-11-24）。登録・署名済み契約・購読申請の要求は P2/P4/P6 のいずれにもない。共有トークンは公開リンク識別子であり、個人認証情報として扱わない。 | 2026-11-24 |
| 形態 | **bulk（CSV・ZIP、月次・37 ファイル/月）**。CSV は `;` 区切り・RDBMS 想定の自動ロード形式（P3 §1）。単一照会の REST（P7: `api-cnpj-basica` / `api-cnpj-empresa` / `api-cnpj-qsa`）は存在を確認したが認証が必要で第一候補ではない（後述）。 | P3 §1（`ponto e vírgula` 区切り）；P7 Endpoints（PRODUÇÃO: `oauth2/jwt-token`、各 API）。 | 2026-11-24 |
| データサイズ（1月分） | 2026-10: 37 ファイル **7,808,752,942 bytes = 7.81 GB（7.27 GiB）**。2026-11: 37 ファイル **7,870,490,248 bytes = 7.87 GB（7.33 GiB）**。最大は `Estabelecimentos0.zip`（2026-10: 2.28 GB / 2026-11: 2.32 GB）。全月 `cnpj.tar.gz` は 63,954,782,749 bytes（63.95 GB）。取得はしないサイズのみ記録。 | P2 PROPFIND `getcontentlength`（2026-11-24 計測。ZIP 本体はダウンロードしていない）。 | 2026-11-24 |
| ライセンス（a: Portal メタデータ） | **CC BY（Creative Commons Attribution、版の明示なし）** — S1 に表示されるデータセットメタデータのライセンス欄。 | S1: 「Licença: Creative Commons Attribution」（live 2026-11-24）。RFB は「Área técnica responsável」だが、S1 は二次資料として区別する。 | 2026-11-24 |
| ライセンス（b: RFB サイト） | **CC BY-ND 3.0（Atribuição-SemDerivações 3.0 Não Adaptada）** — 対象は「Todo o conteúdo deste site」（サイトコンテンツ）。 | P1 および RFB `/dados-abertos/cadastros` のフッタ（live 2026-11-24 に両ページで同文を確認）。データ ZIP 自体への拡張は snapshot・一次資料のいずれでも確認できていない。 | 2026-11-24 |
| ライセンス（ZIP ファイル自体） | **未確認**。P1・P2・P3・P4 のいずれにもファイル個別のライセンス文言は見当たらなかった。P6（Decreto 8.777/2016）は政策として「livre utilização / reuso irrestrito」を定めるが、ファイル個別の CC 表記を代替するものではないため、P6 だけではファイルライセンスを確定させない。 | P6 art. 3º IV・art. 4º（政策レベルの根拠）。ファイルレベルの確定は未確認。 | 2026-11-24 |
| ND の改変（スキーマ変換）可否 | **未確認（Legal / owner 確認待ち）**。CC BY-ND 3.0 がファイルに適用される場合は改変（スキーマ変換・列加工）が禁じられ得る。適用範囲が未確認のため判断を埋めない。 | b に対する一次資料の射程限定（「este site」の内容）。ファイルへの適用は未確認。 | 2026-11-24 |
| 単一照会 Web サービス（cnpjreva / REDESIM） | **未確認**。S2 の live 表示は JS シェルで無料文言を取得できず（「gratuito」「Qualquer pessoa」はいずれも live テキストでヒットなし）。ログインは gov.br アカウント Prata/Ouro が必要と表示される（S2 live）。REDESIM チャネル（`consultacnpj.redesim.gov.br`）の利用条件は確認していない。**bulk が第一候補であり、単一照会は代替案として引き続き確認**（料金・障壁の判断はしない）。 | S2 live 2026-11-24（login: Prata/Ouro）。 | 2026-11-24 |
| Conecta REST API（P7） | **無料公開 API ではない（認証必要）**。Endpoints は `oauth2/jwt-token`（本番・検証環境とも）。カタログ文言: 「Caso seja a primeira integração… necessário previamente cadastrar os IPs do órgão para… firewall no Serpro」「Para geração das chaves de acesso, consulte… Gerenciador Conecta」。機関 IP 登録・鍵発行の政府間連携チャネルであり、無償・キー不要の公開経路として扱えない。レート制限の数値・3 サービスの相違の詳細は P7 で **未確認**（カタログの数値制限の記載なし）。 | P7 live 2026-11-24。 | 2026-11-24 |
| 個人データ（Sócios） | PF の**氏名**、CPF（**先頭3桁＋DV 隠蔽**）、代表者の氏名・CPF（同）、**Faixa Etária**（CPF 生年月日から導出、0–12 … 80超）を含む。P3 Sócios レイアウト＋規約 2（descaracterização、Lei 13.473/2017 LDO 2018 参照。P4 item 6 は CTN art. 198 / Lei 5.172/1966 を根拠に記載）。 | P3 §§2・4（Sócios・Faixa Etária 帯域）；P4 item 5 §2.1.1–2.1.10、item 6。 | 2026-11-24 |
| 個人データ（Empresas / Estabelecimentos） | Empresas: razão social/ nome empresarial、natureza jurídica、capital social、porte。Estabelecimentos: 住所一式（logradouro〜CEP）、電話・メール・ファックス、 situazione cadastral。Simples: Simples/MEI の option・exclusão 日付（MEI は個人事業主の制度）。**Empresas の razão social に個人事業主（Empresário Individual、Naturezas 213-5）の個人名が含まれるかはファイルの中身を未取得のため未確認。Naturezas.zip の中身は未開封（サイズのみ）。** | P3（Empresas/Estabelecimentos/Simples）；P4 item 5 §1・§3・§4。Empresário Individual の含否は未確認。 | 2026-11-24 |
| 個人データの扱い（方針） | RFB は P4 item 5 の一覧を「não protegidos por sigilo fiscal」とし、item 6 で CPF を隠蔽、**item 9** で「他の個人・経済データ源と結合して私生活・親密性が露出しうるため、保持者は cautela と zelo をもって扱うこと」と警告し、item 10 で「dados públicos, de livre acesso a qualquer interessado」と分類。→ **個人データはライセンスとは別判断**。初期スコープは法人の基本情報に限定し、役員名・個人事業主は初期スコープに入れない（catalog 方針の転記。結合利用の可否は Legal への未確認事項）。 | P4 items 5・6・9・10（P5 の NR 改正後）。 | 2026-11-24 |
| レート制限・robots（bulk） | **未確認**。P2 の公開共有・P4・P6 に数値制限の記載は見当たらず。決定した利用条件の数値制限がないため、取得計画に数値を置かない（計画側で別途確認する未確認事項）。 | P2/P4/P6（記載なし）。 | 2026-11-24 |
| 更新頻度・鮮度 | 月次（S1 メタデータ「Atualização: Mensal」「Data da última extração: 11/01/2026」（二次本文）。P2 のフォルダ更新: 2026-10 は 13/10/2026、2026-11 は 16/11/2026。**鮮度保証は RFB 側に明記がなく未確認**。P4 item 2: 「atualiza mensalmente」。 | P4 item 2（月次更新の言及）。 | 2026-11-24 |
| 利用用途の制限・再配布 | **未確認**。CC BY（版の明示なし）と CC BY-ND 3.0 の射程がファイル未確認のため、再配布・派生（スキーマ変換）・商用利用の可否を定めない。P6 art. 3º IV は政策として「reuso irrestrito」を定めるが、ファイル個別の可否とは別に記録する。再配布・第三者提供・マーケティング／リード獲得用途の可否はいずれも Legal / owner 確認待ち（部外の用途転用は行わない方針を維持する）。 | P6 art. 3º IV（政策）。用途自体の可否は未確認。 | 2026-11-24 |

## サイズ計測明細（PROPFIND、2026-11-24。ZIP 本体は取得していない）

- 2026-10 フォルダ: 37 ファイル合計 7,808,752,942 bytes（7.81 GB / 7.27 GiB）。
  - `Estabelecimentos0–9.zip`: 2.28 GB + 342 MB + 336 MB + 369 MB + 340 MB + 335 MB + 370 MB + 338 MB + 344 MB + 365 MB。
  - `Empresas0–9.zip`: 571 MB + 78 MB × 9 本（Empresas0 は 571 MB、他は 78–99 MB）。
  - `Socios0–9.zip`: 251 MB + 49 MB × 9 本（Socios0 は 251 MB、他は 49–50 MB）。
  - `Simples.zip`: 311 MB。補助表: Cnaes 22 KB / Municipios 43 KB / Motivos 1 KB / Paises 3 KB / Naturezas 2 KB / Qualificacoes 1 KB。
- 2026-11 フォルダ: 37 ファイル合計 7,870,490,248 bytes（7.87 GB / 7.33 GiB）。構成は 2026-10 と同一（Estabelecimentos0.zip 2.32 GB、Simples.zip 314 MB 等）。
- 全月: `cnpj.tar.gz` 63,954,782,749 bytes（63.95 GB）。取得しない。
- 仕様書 `cnpj-metadados.pdf`: 59,315 bytes（6 pages）、sha256 `0a52d6bdfb61a07425e352a0e692932306a5ec9ecad682f5f9e28059e1a5fce0`。
- 状態: S1 上「Inscrições no CNPJ (ZIP)」は接続エラー表示（2026-11-24）。P2 の 2026-11 フォルダは PROPFIND で 37 ファイルの存在を確認済みだが、**いずれの ZIP の中身も開いていない**（列・行の内容は不問）。

## 次の一手（本書のスコープ外）

- **実装はしない**（`:planned` のまま）。次のタスクで引き継ぐべき未解決の論点:
  1. **ファイルライセンスの確定**（CC BY と CC BY-ND 3.0 の食い違い）と **ND 下でのスキーマ変換可否**（Legal / owner 確認。成立しない場合はスキーマ変換を伴わない素朴な取り込みのみ可、等）。
  2. **用途の可否**（再配布・第三者提供・マーケティング／リード獲得。部外用途転用の禁止方針を維持しつつ、表示・加工の範囲を確定する）。
  3. **個人データの除外規則**（Empresário Individual / MEI の扱い、Sócios の PF 氏名・Faixa Etária・代表者氏名の取り込み可否、NT 47/2024 item 9 の cautela の実装側含意）。
  4. **単一照会の代替**（S2 の無料文言の再取得、REDESIM の利用条件。bulk 取得が採算しない場合のみ検討）。
  5. **鮮度・レート制限の数値**（月次更新の日、P2 の運用上限制共有、catalog `:access-conditions` への記録方法。取得側で別途確認）。
- catalog の `:status`・`:terms-verified-on`・`:blocked-reason` は**変更していない**。`:terms-verified-on` は条件全体の確定を意味するため、未確認項目（ファイルライセンス、ND、用途、単一照会無料文言、レート制限）が残る限り `nil` のままとする。
- 本書が `docs/sources/blocked-summary-20261123.md`（2026-11-23 記録。BR は Wayback snapshot の試行記録）の BR 節を置き換えるものではない。2026-11-23 snapshot（S1=2026-11-05 の CC BY 表示、RFB フッタ=2026-10-27 / 2026-11-17 の CC BY-ND 3.0）は当時記録として残る。本書は 2026-11-24 の live 確認であり、実行ファイルの本体はどちらでも取得していない。
