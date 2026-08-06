#!/usr/bin/env nbb
;; Two-runtime gate: the same suite under nbb (ClojureScript on Node), not just
;; the JVM.
;;
;; 判定の中身（権利の包含・期間の比較・納品可能種別）は `kotoba-lang/ongaku`
;; にあり、そちらでも両ランタイムで走る。ここで確かめるのは台帳との
;; 突き合わせ —— 台帳に無い作品を craft 層へ渡す前に落とすこと、既発の譲渡が
;; 二重譲渡の検査に届いていること。
;;
;; nbb は deps.edn の git dep を解決しないので、ongaku のソースを classpath に
;; 直接足す（west checkout の sibling path）:
;;
;;   nbb --classpath "src:test:../../kotoba-lang/ongaku/src" run-tests.cljs

(ns run-tests
  (:require [cljs.test :as t]
            [sakkyokuka.studio-test]))

;; cljs.test prints its own summary; this hook exists only so a failure becomes
;; a non-zero exit code, which is what a CI gate reads.
(defmethod t/report [::t/default :end-run-tests] [m]
  (when-not (t/successful? m)
    (set! (.-exitCode js/process) 1)))

(t/run-tests 'sakkyokuka.studio-test)
