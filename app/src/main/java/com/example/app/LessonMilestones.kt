package com.example.app

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.math.abs

/**
 * Haftalık ilerleme kartı için DERS KİLOMETRE TAŞLARI:
 *
 *     users/{uid}/lessonMilestones/{partId}_{stableId}
 *         partId, stableId
 *         finishedAtMs : dersin ilk bitişi (kart "bu hafta bitirdiği dersler")
 *         sampleOp     : o dersten doğru çözülmüş bir işlem (kart "Çözdüğü bir işlem: 48 + 37 = 85")
 *         chest        : sandık dersinde ilk ve en iyi süre (kart "1:20'de bitiriyordun, şimdi 0:52")
 *
 * ## Neden ayrı belge
 * Ders ilerlemesi (`lessonProgress`, [com.example.app.model.LessonItem], [LessonProgressMerge]) ve
 * ders sonu ekran zinciri uzun uğraşla kararlı hâle geldi. Kartın verisi oraya alan olarak eklenseydi
 * merge kurallarına, bulut anlık görüntüsüne ve sayfa geçişlerine dokunmak gerekirdi. Burada her şey
 * arka planda, ayrı bir belgeye yazılıyor; yazma başarısız olursa yalnızca kartın bir satırı eksik
 * kalır, ders akışı etkilenmez.
 *
 * ## Neden şimdi
 * Bugün sandık dersinde yalnızca en yüksek PUAN saklanıyor (puan = başarı × 5 × süre çarpanı); süre
 * puanın içinde kayboluyor. Ders bitişinin tarihi de yok (yalnızca `stepIsFinish`). Geçmiş geri
 * gelmeyeceği için kart ekranından önce birikmeye başlamalı.
 *
 * Eski veri: bu sürümden önce bitirilen derslerin tarihi ve örnek işlemi yok, uydurulmaz; sandık
 * süresi bu sürümden sonraki ilk geçerli koşuyla başlar.
 */
object LessonMilestones {

    private const val TAG = "LessonMilestones"
    private const val COLLECTION = "lessonMilestones"

    /**
     * Ders boyunca doğru çözülenlerin en zoru, bellekte. Anahtar `{partId}_{stableId}`.
     * Dersin son adımındaki işlem isteniyor (kullanıcı kararı 08.11.2026: "doğrulardan en zoru"),
     * bu yüzden daha ileri bir adıma geçilince öncekiler atılıyor.
     */
    private data class Pending(val step: Int, val op: LessonMilestoneRules.SampleOp)

    private val pending = HashMap<String, Pending>()

    private fun key(partId: Int, stableId: String) = "${partId}_$stableId"

    /**
     * Ders ekranlarının soru başı [SobiRuleStats] kaydıyla aynı yerden çağrılır (AbacusFragment,
     * BlindingLessonFragment → recordRuleStats). Yalnızca ders bölümleri (1..6): kupa yolu (9) ve
     * yarışların (7-8) "ders bitişi" yok.
     */
    fun noteCorrect(partId: Int, stableId: String?, step: Int, op: LessonMilestoneRules.SampleOp?) {
        if (partId !in 1..6 || stableId.isNullOrBlank() || op == null) return
        Log.d(TAG, "noteCorrect part=$partId id=$stableId adım=$step işlem=${op.text}")
        val k = key(partId, stableId)
        synchronized(pending) {
            val cur = pending[k]
            pending[k] = when {
                cur == null || step > cur.step -> Pending(step, op)
                step < cur.step -> cur
                else -> Pending(step, LessonMilestoneRules.pickHarder(cur.op, op))
            }
        }
    }

    /**
     * Ders ilk kez tamamen bittiğinde ([LessonSuccessRateRepository.recordItemFirstFinish]).
     * Bitiş tarihi ilk yazılanda kalır (başka cihazda yeniden bitirmek tarihi ileri atmasın).
     */
    fun recordFinish(partId: Int, stableId: String?) {
        if (partId !in 1..6 || stableId.isNullOrBlank()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val op = synchronized(pending) { pending.remove(key(partId, stableId))?.op }
        val ref = docRef(uid, partId, stableId)
        val now = System.currentTimeMillis()
        try {
            FirebaseFirestore.getInstance().runTransaction { tx ->
                val snap = tx.get(ref)
                val patch = HashMap<String, Any>()
                patch["partId"] = partId
                patch["stableId"] = stableId
                if (snap.getLong("finishedAtMs") == null) patch["finishedAtMs"] = now
                if (op != null && snap.get("sampleOp") == null) patch["sampleOp"] = op.toMap()
                tx.set(ref, patch, SetOptions.merge())
                patch.keys.toString()
            }.addOnSuccessListener { keys ->
                // Cihazda doğrulama için (adb logcat -s LessonMilestones); yazılan alanları gösterir.
                Log.d(TAG, "recordFinish yazıldı part=$partId id=$stableId alanlar=$keys örnek=${op?.text}")
                // Davet ödülü bu kaydı bekliyor (functions/referrals.js grantReferralRewardOnLesson). Davetle
                // gelen kişi "1 hafta Pro kazandın"ı 2. dersin hemen ardından görsün diye (09.11.2026 cihazda B'de
                // 30 dk'lık okuma aralığı yüzünden çıkmamıştı) ödül bildirimi birkaç saniye sonra yeniden okunuyor.
                if (partId == 1 && stableId == ReferralAttribution.REWARD_LESSON_ID) {
                    MainActivity.currentActivity?.requestReferralRewardCheck()
                }
            }.addOnFailureListener { e -> Log.w(TAG, "recordFinish part=$partId id=$stableId", e) }
        } catch (e: Exception) {
            Log.w(TAG, "recordFinish başlatılamadı", e)
        }
    }

    /**
     * Sandık dersinin bir koşusu: Part 1–3 ve 6'da [ChestResult.record] (yalnızca sandığı geçen
     * koşular o ekrana ulaşıyor), Part 4–5'te BlindingLessonFragment'ın geçiş dalı (sandık ekranı yok).
     * Başarı %[LessonMilestoneRules.CHEST_MIN_SUCCESS_PCT] altındaysa sayılmaz.
     */
    fun recordChestRun(partId: Int, stableId: String?, successPct: Float, timeSec: Int?) {
        if (partId !in 1..6 || stableId.isNullOrBlank() || timeSec == null) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Eşik altı koşu için ağ turu bile gerekmiyor.
        if (LessonMilestoneRules.chestUpdate(null, timeSec, successPct, 0L) == null) return
        val ref = docRef(uid, partId, stableId)
        val now = System.currentTimeMillis()
        try {
            FirebaseFirestore.getInstance().runTransaction { tx ->
                val snap = tx.get(ref)
                @Suppress("UNCHECKED_CAST")
                val prev = LessonMilestoneRules.ChestTimes.fromMap(snap.get("chest") as? Map<String, Any?>)
                val next = LessonMilestoneRules.chestUpdate(prev, timeSec, successPct, now)
                if (next != null) {
                    tx.set(
                        ref,
                        mapOf("partId" to partId, "stableId" to stableId, "chest" to next.toMap()),
                        SetOptions.merge(),
                    )
                }
                next?.toString() ?: "değişiklik yok (önceki: $prev)"
            }.addOnSuccessListener { result ->
                Log.d(TAG, "recordChestRun part=$partId id=$stableId süre=${timeSec}sn başarı=$successPct → $result")
            }.addOnFailureListener { e -> Log.w(TAG, "recordChestRun part=$partId id=$stableId", e) }
        } catch (e: Exception) {
            Log.w(TAG, "recordChestRun başlatılamadı", e)
        }
    }

    private fun docRef(uid: String, partId: Int, stableId: String) =
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection(COLLECTION).document("${partId}_$stableId")
}

/**
 * [LessonMilestones]'ın yan etkisiz kararları; birim testi Firebase'siz çalışsın diye ayrı.
 */
object LessonMilestoneRules {

    /** Sandık süresinin sayılması için en az başarı (kullanıcı kararı 08.11.2026). */
    const val CHEST_MIN_SUCCESS_PCT = 80f

    /**
     * Kartta gösterilecek işlem. [numbers] işaretli dizi (çıkarma negatif), çarpmada iki çarpan.
     */
    data class SampleOp(val numbers: List<Int>, val multiply: Boolean, val answer: Int) {
        /** "48 + 37 = 85", "60 - 25 + 7 = 42", "12 x 4 = 48" (gösterim biçimi kartın işi; bu sade). */
        val text: String
            get() = if (multiply) {
                "${numbers[0]} x ${numbers[1]} = $answer"
            } else {
                buildString {
                    append(numbers[0])
                    numbers.drop(1).forEach { n -> append(if (n < 0) " - " else " + ").append(abs(n)) }
                    append(" = ").append(answer)
                }
            }

        fun toMap(): Map<String, Any> = mapOf(
            "numbers" to numbers,
            "multiply" to multiply,
            "answer" to answer,
            "text" to text,
        )
    }

    /** Toplama/çıkarma dizisi (işaretli). En az iki sayı; değilse null. */
    fun sequence(numbers: List<Int>): SampleOp? =
        if (numbers.size < 2) null else SampleOp(numbers, multiply = false, answer = numbers.sum())

    /** İki sayı ve işaret ("+", "-", "x"); başka işaret null. */
    fun binary(first: Int, operator: String?, second: Int): SampleOp? = when (operator) {
        "+" -> SampleOp(listOf(first, second), false, first + second)
        "-" -> SampleOp(listOf(first, -second), false, first - second)
        "x" -> SampleOp(listOf(first, second), true, first * second)
        else -> null
    }

    private fun digits(n: Int): Int = abs(n).toString().length

    /**
     * Zorluk sırası: en çok basamak, sonra sayı adedi, sonra sonucun büyüklüğü. Basamak önce: veliye
     * "iki basamaklı sayılarla çalışıyor" adet farkından daha çok şey söylüyor.
     */
    private val difficulty = compareBy<SampleOp>(
        { op -> op.numbers.maxOf { digits(it) } },
        { op -> op.numbers.size },
        { op -> abs(op.answer) },
    )

    /** Eşitlikte sonuncusu (yeni gelen) — kullanıcı kararı. */
    fun pickHarder(current: SampleOp?, candidate: SampleOp): SampleOp =
        if (current == null || difficulty.compare(candidate, current) >= 0) candidate else current

    /** Sandık dersinin ilk ve en iyi süresi. */
    data class ChestTimes(val firstSec: Int, val firstAtMs: Long, val bestSec: Int, val bestAtMs: Long) {
        fun toMap(): Map<String, Any> = mapOf(
            "firstSec" to firstSec, "firstAtMs" to firstAtMs,
            "bestSec" to bestSec, "bestAtMs" to bestAtMs,
        )

        companion object {
            fun fromMap(m: Map<String, Any?>?): ChestTimes? {
                if (m == null) return null
                val firstSec = (m["firstSec"] as? Number)?.toInt() ?: return null
                val bestSec = (m["bestSec"] as? Number)?.toInt() ?: return null
                return ChestTimes(
                    firstSec, (m["firstAtMs"] as? Number)?.toLong() ?: 0L,
                    bestSec, (m["bestAtMs"] as? Number)?.toLong() ?: 0L,
                )
            }
        }
    }

    /**
     * Yeni koşudan sonraki kayıt; değişiklik yoksa null (yazma yapılmasın).
     * 0 sn ve altı geçersiz (süre okunamamış); eşik altı başarı sayılmaz.
     */
    fun chestUpdate(prev: ChestTimes?, timeSec: Int, successPct: Float, nowMs: Long): ChestTimes? {
        if (timeSec <= 0 || successPct < CHEST_MIN_SUCCESS_PCT) return null
        if (prev == null) return ChestTimes(timeSec, nowMs, timeSec, nowMs)
        if (timeSec >= prev.bestSec) return null
        return prev.copy(bestSec = timeSec, bestAtMs = nowMs)
    }
}
