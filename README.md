# sakkyokuka (作曲家)

**受注して作り、権利を定めて渡す商売の層。** 依頼1件を「作品 + 納品物 +
譲渡する権利 + 対価」として束ね、その約束が守れるかを受注前に構造的に
検査する。

ADR-2607023000 が creative `-ka` ごとに定める 3 層のうち、3 番目:

| layer | where | what it holds |
|---|---|---|
| 技芸 craft | [`kami-ongaku-notation`](https://github.com/kotoba-lang/kami-ongaku-notation) / [`-sequencer`](https://github.com/kotoba-lang/kami-ongaku-sequencer) / [`-project`](https://github.com/kotoba-lang/kami-ongaku-project)（人が書く経路）、[`composer`](https://github.com/kotoba-lang/composer)（生成する経路） | 譜面 IR・MIDI/SMF・DAW セッション・生成契約 |
| 職能 occupation | [`cloud-itonami-isco-2652`](https://github.com/cloud-itonami/cloud-itonami-isco-2652) | ISCO-08 2652 Musicians, Singers and Composers の blueprint |
| 商売 business | **this repo** | 受注・権利・納品・成立判定 |

## ongakuka との境界

`ongakuka` と分かれているのは語感の問題ではなく、**商売の力学が別**だから。

| | [`ongakuka`](https://github.com/cloud-itonami/ongakuka) | **sakkyokuka** |
|---|---|---|
| 原資 | 他人の既製音源（DOVA-SYNDROME / Incompetech 等） | 自分が作る |
| 権利 | 第三者ライセンスの遵守 | 原盤権・著作権・出版権を自分が持つ |
| 収益 | 使用許諾 / render 同梱 | 受注 + 二次利用 |
| 納品物 | asset path + credit text | master / stems / MIDI / 譜面 / セッション |
| craft | [`kotoba-lang/ongaku`](https://github.com/kotoba-lang/ongaku) | 上表のとおり |

職能（ISCO-08 2652）は**分けない** — ISCO は Musicians, Singers and Composers を
1 つの職業として正しく束ねている。`isco-2651`（painters / sculptors /
cartoonists）に対して `mangaka` だけが立っているのと同じ形で、-ka が ISCO より
細かいのは既存の形。

## 構造的に拒否すること

`kotoba-lang/ongaku` が license policy の違反を構造的に拒否するのと同じことを、
権利の譲渡側と納品の約束側で行う。

**1. 持っていない権利は売れない。** ongakuka のカタログ資産について studio が
持つのは非独占の使用許諾であって原盤権ではないので、それを素材にした受注で
`:master` を独占譲渡することは成立しない。

```clojure
(require '[sakkyokuka.rights :as rights])

(rights/validate-grant [(rights/right {:kind :sync :exclusive? false})]
                       (rights/right {:kind :master :exclusive? true}))
;; => [{:problem/type :not-held
;;      :problem/message "保有していない権利は譲渡できない: master(独占) worldwide" ...}]
```

非独占しか持っていない権利を独占で渡すこと、保有地域の外へ渡すこと、保有期間を
超えて渡すこと、既に出した独占譲渡と地域・期間が重なることも同じく拒否する。

**2. 作れない物は約束できない。** 生成パイプラインの出力は音源と stems だけで、
譜面も MIDI も DAW セッションも**そもそも存在しない**。純 AI 生成の作品に
「譜面つき」を売ることは守れない約束なので受注時に落とす。

```clojure
(require '[sakkyokuka.work :as work])

(work/producible-kinds :authored)
;; => #{:master-audio :stems :midi :score :session}
(work/producible-kinds :generated)
;; => #{:master-audio :stems}
```

**3. AI が関与した作品は、どのモデルが作ったかを記録せずに納品できない。**
`:generated` / `:hybrid` の作品は `:work/model-id` と `:work/disclosure` を
必ず持つ（`ai.gftd.ongakuka.track` の `modelId` と同じ要求）。

## 受注

```clojure
(require '[sakkyokuka.commission :as commission])

(commission/acceptable?
  (commission/commission
    {:id "c-0001" :client "株式会社ほげ" :brief "CM 30 秒"
     :work my-work :fee 300000 :currency "JPY" :deadline "2026-09-30"
     :grants [(rights/right {:kind :sync :territory #{"JP"} :exclusive? true
                             :term {:term/from "2026-10-01" :term/until "2027-10-01"}})]
     :deliverables [:master-audio :stems :midi :score :session]}))
;; => true
```

`validate` は問題が無ければ `nil`、あれば問題の vector を返す。真が返ったら
受注してはならない。

## 生成経路との関係

`ai.gftd.ongakuka.compose` の XRPC surface（`gftdcojp/apps-gftdcojp` の lexicon、
`etzhayyim/com-etzhayyim-app-ongakuka` にも同型のものがある）と、
`cloud-itonami/gftd-audio-actor`（persona リツ、network-isekai 向けの BGM/SFX
生成 actor）は、どちらもそのまま残る。**この repo はそれらを置き換えるのでは
なく、商売としての受注・権利・納品を1箇所に集約する。** lexicon の NSID は
wire identity なので改名しない。

## Test

```bash
nbb --classpath "src:test" run-tests.cljs   # ClojureScript on Node
clojure -M:test                              # JVM
```

期間の包含判定は ISO-8601 文字列の `compare`、地域の包含判定は集合演算で
書いてあるので、両ランタイムで実際に走らせて挙動が揃っていることを確かめる。

## Layout

```
src/sakkyokuka/rights.cljc       権利の保有・譲渡と構造的拒否
src/sakkyokuka/work.cljc         作品と provenance、納品可能な種別
src/sakkyokuka/commission.cljc   受注と成立判定
resources/craft-libraries.edn    技芸層の正本一覧（isco-2652 blueprint と一致させる）
```
