(ns sakkyokuka.studio-test
  (:require [clojure.test :refer [deftest is testing]]
            [ongaku.rights :as rights]
            [sakkyokuka.studio :as studio]))

;; テストは resources/works.edn の実データを読まず、同じ shape の
;; リテラルを使う（ランタイムに依らず、台帳を編集しても壊れないため）。
(def reg
  (studio/registry
   {:works
    [{:id "w-authored" :title "遠雷" :provenance :authored
      :held-rights [(rights/right {:kind :master :exclusive? true})
                    (rights/right {:kind :sync :exclusive? true})]
      :granted [(rights/right {:kind :sync :territory #{"JP"} :exclusive? true
                               :term {:term/from "2026-04-01" :term/until "2027-04-01"}})]}
     {:id "w-generated" :title "night drive" :provenance :generated
      :model-id "diffrhythm-1.2-ja"
      :disclosure "本作は ai.gftd.ongakuka.compose により生成されました。"
      :held-rights [(rights/right {:kind :master :exclusive? true})]
      :granted []}
     {:id "w-catalog" :title "カタログ資産ベース" :provenance :authored
      :held-rights [(rights/right {:kind :sync :exclusive? false})]
      :granted []}]}))

(defn- req [work-id extra]
  (merge {:id "c-0001" :client "株式会社ほげ" :brief "CM 30 秒"
          :work-id work-id :fee 300000 :deadline "2026-09-30"
          :grants [] :deliverables [:master-audio]}
         extra))

(deftest registry-lookup
  (is (= "遠雷" (:title (studio/work-entry reg "w-authored"))))
  (is (= 2 (count (studio/held-rights reg "w-authored"))))
  (is (= 1 (count (studio/existing-grants reg "w-authored"))))
  (is (= [] (studio/existing-grants reg "w-generated"))))

(deftest unknown-work-is-refused-before-reaching-the-craft-layer
  (let [{:keys [ok? problems work]} (studio/evaluate reg (req "w-nope" {}))]
    (is (false? ok?))
    (is (nil? work))
    (is (= :unknown-work (:problem/type (first problems))))))

(deftest authored-work-accepts-a-non-overlapping-grant
  (testing "既発の独占譲渡（JP 2026-04〜2027-04）と重ならなければ通る"
    (is (studio/acceptable?
         reg (req "w-authored"
                  {:grants [(rights/right {:kind :sync :territory #{"JP"} :exclusive? true
                                           :term {:term/from "2027-04-01" :term/until "2028-04-01"}})]
                   :deliverables [:master-audio :stems :midi :score :session]})))))

(deftest existing-grant-in-the-registry-blocks-an-overlapping-exclusive
  (let [{:keys [ok? problems]}
        (studio/evaluate reg (req "w-authored"
                                  {:grants [(rights/right {:kind :sync :territory #{"JP"} :exclusive? true
                                                           :term {:term/from "2026-06-01" :term/until "2027-01-01"}})]}))]
    (is (false? ok?))
    (is (some #(= :exclusive-conflict (:problem/type %)) problems))))

(deftest catalog-backed-work-cannot-sell-master
  (let [{:keys [ok? problems]}
        (studio/evaluate reg (req "w-catalog"
                                  {:grants [(rights/right {:kind :master :exclusive? true})]}))]
    (is (false? ok?))
    (is (some #(= :not-held (:problem/type %)) problems))))

(deftest generated-work-cannot-promise-score
  (let [{:keys [ok? problems]}
        (studio/evaluate reg (req "w-generated" {:deliverables [:master-audio :stems :score]}))]
    (is (false? ok?))
    (is (some #(= :not-producible (:problem/type %)) problems)))
  (testing "音源と stems だけなら通る"
    (is (studio/acceptable? reg (req "w-generated" {:deliverables [:master-audio :stems]})))))
