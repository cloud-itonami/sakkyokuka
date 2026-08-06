(ns sakkyokuka.studio
  "この studio の作品台帳と、そこに受注を突き合わせる商売層。

  判定の中身は持たない —— 権利の保有・譲渡の検査（`ongaku.rights`）、
  provenance ごとの納品可能種別（`ongaku.work`）、受注1件のまとめ
  （`ongaku.commission`）はすべて技芸層の craft library にある
  （ADR-2607023000: コードは kotoba-lang、職能は cloud-itonami-isco、
  商売は -ka repo）。

  ここが持つのは craft library が原理的に知らないもの —— **この studio が
  どの作品を持ち、それぞれについて実際に何の権利を保有し、既にどこへ何を
  渡してあるか**。`ongaku.catalog` に対して `ongakuka/resources/catalog.edn`
  が立つのと同じ関係で、正本は `resources/works.edn`。

  典型的な使い方:

      (let [reg (studio/registry (studio/read-registry \"resources/works.edn\"))]
        (studio/evaluate reg {:id \"c-0001\" :client \"株式会社ほげ\"
                              :work-id \"w-0002\"
                              :grants [...] :deliverables [:master-audio]}))
      ;; => {:ok? false :problems [...] :work {...}}"
  (:require [ongaku.commission :as commission]
            [ongaku.work :as work]))

;; --- 台帳 ------------------------------------------------------------------

(defn registry
  "作品台帳を id → 作品エントリの map にする。"
  [{:keys [works]}]
  (into {} (map (juxt :id identity)) works))

(defn work-entry
  [reg work-id]
  (get reg work-id))

(defn held-rights
  "その作品について studio が実際に保有している権利。"
  [reg work-id]
  (vec (:held-rights (work-entry reg work-id))))

(defn existing-grants
  "その作品について既にどこかへ渡してある譲渡。独占の二重譲渡を防ぐために
  受注検査へ渡す。"
  [reg work-id]
  (vec (:granted (work-entry reg work-id))))

(defn- ->ongaku-work
  "台帳のエントリを `ongaku.work/work` の shape にする。"
  [entry]
  (work/work {:id (:id entry)
              :title (:title entry)
              :provenance (:provenance entry)
              :model-id (:model-id entry)
              :disclosure (:disclosure entry)
              :held-rights (:held-rights entry)}))

;; --- 受注の評価 ------------------------------------------------------------

(defn evaluate
  "受注1件を台帳に突き合わせて評価する。

  `request` は `:work-id` で台帳の作品を指し、あとは
  `ongaku.commission/commission` と同じ形（`:id` `:client` `:brief`
  `:grants` `:deliverables` `:fee` `:currency` `:deadline` `:status`）。

  返り値は `{:ok? bool :problems [...] :work entry}`。作品が台帳に無い場合は
  それ自体を問題として返す —— **台帳に無い作品の権利は定義上ひとつも保有して
  いない**ので、craft 層に渡す前にここで落とす。"
  [reg {:keys [work-id] :as request}]
  (if-let [entry (work-entry reg work-id)]
    (let [c (commission/commission
             (-> request
                 (dissoc :work-id)
                 (assoc :work (->ongaku-work entry))))
          problems (commission/validate c (existing-grants reg work-id))]
      {:ok? (nil? problems)
       :problems (vec problems)
       :work entry})
    {:ok? false
     :problems [{:problem/type :unknown-work
                 :problem/message (str "台帳に無い作品: " work-id)
                 :problem/work-id work-id}]
     :work nil}))

(defn acceptable?
  "この受注を受けてよいか。"
  [reg request]
  (:ok? (evaluate reg request)))
