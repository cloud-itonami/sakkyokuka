#!/usr/bin/env nbb
;; Two-runtime gate: the same suite under nbb (ClojureScript on Node), not just
;; the JVM.
;;
;; この repo が両方で走らせる理由は日付と権利の比較にある。期間の包含判定は
;; ISO-8601 文字列の `compare` で書いてあり、地域の包含判定は集合演算で
;; 書いてある——どちらも JVM と ClojureScript で挙動が揃っていることを
;; 実際に確かめないと「片方だけ緑」になる種類のコード。
;;
;; Run:
;;   nbb --classpath "src:test" run-tests.cljs

(ns run-tests
  (:require [cljs.test :as t]
            [sakkyokuka.rights-test]
            [sakkyokuka.commission-test]))

;; cljs.test prints its own summary; this hook exists only so a failure becomes
;; a non-zero exit code, which is what a CI gate reads.
(defmethod t/report [::t/default :end-run-tests] [m]
  (when-not (t/successful? m)
    (set! (.-exitCode js/process) 1)))

(t/run-tests 'sakkyokuka.rights-test 'sakkyokuka.commission-test)
