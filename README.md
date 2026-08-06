# sakkyokuka (作曲家)

**受注して作り、権利を定めて渡す商売の層。** この studio がどの作品を持ち、
それぞれについて何の権利を実際に保有し、既にどこへ何を渡してあるかを台帳で
持ち、そこに受注を突き合わせる。

ADR-2607023000 が creative `-ka` ごとに定める 3 層のうち、3 番目:

| layer | where | what it holds |
|---|---|---|
| 技芸 craft | [`ongaku`](https://github.com/kotoba-lang/ongaku)（選曲・ライセンス gate と **権利/納品の判定**）、[`kami-ongaku-*`](https://github.com/kotoba-lang/kami-ongaku-project)（譜面・MIDI・DAW セッション）、[`composer`](https://github.com/kotoba-lang/composer)（生成契約） | 判定と IR |
| 職能 occupation | [`cloud-itonami-isco-2652`](https://github.com/cloud-itonami/cloud-itonami-isco-2652) | ISCO-08 2652 の blueprint と reference actor。**問題を hard reject にするか人間承認に回すかを決める** |
| 商売 business | **this repo** | 作品台帳（`resources/works.edn`）と受注の突き合わせ |

**判定の中身はここに無い。** `ongaku.rights` / `ongaku.work` /
`ongaku.commission` が「持っている権利と要求された譲渡」を受け取って判定し、
**何を持っているかは知らない** —— それを持つのがこの repo。`ongaku.catalog`
に対して `ongakuka/resources/catalog.edn` が立つのと同じ関係。

## ongakuka との境界

`ongakuka` と分かれているのは語感の問題ではなく、**商売の力学が別**だから。
職能（ISCO-08 2652）は**分けない** — ISCO は Musicians, Singers and Composers を
1 つの職業として正しく束ねている。`isco-2651`（painters / sculptors /
cartoonists）に対して `mangaka` だけが立っているのと同じ形。

**分かれ目は音源の出所ではなく商売の単位。** ongakuka も音を作る（AI
generation coscientist / MusicGen）—— 違うのは、作った音を**自分のカタログに
積む**のか、**依頼主に権利ごと渡す**のか。

| | [`ongakuka`](https://github.com/cloud-itonami/ongakuka) | **sakkyokuka** |
|---|---|---|
| 単位 | カタログの1曲を選んで使わせる | 依頼1件を受けて作り、権利を定めて渡す |
| 音源 | 既製の調達 + 自前生成 | 受注ごとに作る |
| 権利 | 資産ごとに違う（調達物は使用許諾のみ、自前生成は原盤を保有） | 作品ごとの保有権利を台帳に持ち、譲渡を検査する |
| 収益 | 使用許諾 / render 同梱 | 受注 + 二次利用 |
| 納品物 | asset path + credit text | master / stems / MIDI / 譜面 / セッション |
| 台帳 | `resources/catalog.edn` | `resources/works.edn` |

**保有権利は repo 単位ではなく作品単位の事実**なので、`works.edn` が作品ごとに
持つ。`w-0003` のように ongakuka 側の調達資産を素材にした作品は原盤権を持たず、
自前で書いた `w-0001` は持つ —— 同じ repo の中で両方が並ぶ。

## 使う

```clojure
(require '[sakkyokuka.studio :as studio]
         '[ongaku.rights :as rights])

(def reg (studio/registry (read-string (slurp "resources/works.edn"))))

(studio/evaluate reg
  {:id "c-0001" :client "株式会社ほげ" :brief "CM 30 秒"
   :work-id "w-0003"                         ; カタログ資産ベースの作品
   :grants [(rights/right {:kind :master :exclusive? true})]
   :deliverables [:master-audio] :fee 300000 :deadline "2026-09-30"})
;; => {:ok? false
;;     :problems [{:problem/type :not-held
;;                 :problem/message "保有していない権利は譲渡できない: master(独占) worldwide"}]
;;     :work {...}}
```

台帳が答えるのは3つ:

- **`held-rights`** — その作品について実際に保有している権利。`w-0003` は
  ongakuka 側のカタログ資産が素材なので非独占の同期使用許諾しか持たず、
  原盤権の譲渡は構造的に落ちる。
- **`granted`** — 既にどこかへ渡してある譲渡。独占の二重譲渡の検査に効く。
- **`provenance`** — `:authored` なら譜面も MIDI も DAW セッションも出せるが、
  `:generated` は音源と stems だけ。「譜面つき」を売る受注はここで落ちる。

台帳に無い作品は craft 層に渡す前に落とす（`:unknown-work`）——
**台帳に無い作品の権利は定義上ひとつも保有していない**ので。

## Test

```bash
nbb --classpath "src:test:../../kotoba-lang/ongaku/src" run-tests.cljs   # ClojureScript on Node
clojure -M:test                                                          # JVM
```

nbb は deps.edn の git dep を解決しないので、ongaku のソースを classpath に
直接足す（west checkout の sibling path）。

## Layout

```
src/sakkyokuka/studio.cljc      台帳と受注の突き合わせ
resources/works.edn             作品台帳（保有権利・既発譲渡・provenance）
resources/craft-libraries.edn   技芸層の正本一覧（isco-2652 blueprint と一致させる）
```
