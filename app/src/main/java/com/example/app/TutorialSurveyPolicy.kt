package com.example.app

import android.content.Context
import android.util.Log
import com.example.app.model.LessonItem

/**
 * Öğretici anketlerinin ne zaman çıkacağına karar verir (kullanıcı kararı 08.11.2026).
 *
 * ## Öğretici sonu anketi: yalnızca ünite başlarında
 * Eskiden her öğreticinin sonunda iki soruluk bir ekran çıkıyordu. 1 Ekim – 8 Kasım arasında
 * 19 kişiden gelen 58 cevabın %80'i 4–5'ti ve sonraki öğreticilerde cevaplar hızla düşüyordu:
 * bilgi az, sürtünme çok. Artık anket yalnızca her ünitenin İLK öğreticisinden sonra çıkıyor
 * (uygulamanın ilk öğreticisi de bunlardan biri) ve aynı öğretici için cihazda bir kez.
 * Bir ünite, haritadaki başlık satırından ([LessonItem.TYPE_HEADER]) sonraki derslerdir.
 *
 * ## "Takılınca sor"
 * Bir adımı ancak [STUCK_MIN_ATTEMPT]. denemede geçen çocuğa doğru panelinde "ne zorladı?"
 * diye sorulur. Neyi ölçtüğümüz zaten belli (adım, deneme sayısı); eksik olan nedeniydi.
 * Adım başına bir kez sorulur: [TutorialStepAnalytics.recordAttempt] adım bir kez doğru
 * geçildikten sonra yeni deneme saymıyor, dolayısıyla aynı adım ikinci kez buraya gelmez.
 */
object TutorialSurveyPolicy {

    private const val TAG = "TutorialSurveyPolicy"
    private const val PREFS = "tutorial_survey_policy"
    private const val PREFIX_SHOWN = "end_survey_shown_"

    /** Bu deneme numarasında (ya da sonrasında) doğru cevap gelirse "ne zorladı?" sorulur. */
    const val STUCK_MIN_ATTEMPT = 3

    /**
     * [stableId], listede bir başlıktan sonraki ilk ders mi? Listede hiç başlık yoksa ilk ders
     * sayılır. Saf fonksiyon; birim testi `TutorialSurveyPolicyTest`.
     *
     * @param items Bölümün haritadaki sırasıyla (tür, kimlik) çiftleri.
     */
    fun isUnitOpener(items: List<Pair<Int, String>>, stableId: String): Boolean {
        var waitingForFirstLesson = true
        for ((type, id) in items) {
            when (type) {
                LessonItem.TYPE_HEADER -> waitingForFirstLesson = true
                LessonItem.TYPE_LESSON -> {
                    if (id == stableId) return waitingForFirstLesson
                    waitingForFirstLesson = false
                }
            }
        }
        return false
    }

    /** Doğru cevap bu deneme numarasında geldiyse "ne zorladı?" sorulur. */
    fun shouldAskStuck(attemptNo: Int?): Boolean = attemptNo != null && attemptNo >= STUCK_MIN_ATTEMPT

    /**
     * Öğretici sonu anketi bu öğreticide gösterilsin mi? Göstermeye karar verirse cihazda
     * işaretler; aynı öğretici yeniden açılınca anket bir daha çıkmaz.
     */
    fun takeEndSurveySlot(context: Context, partId: Int, stableId: String): Boolean {
        if (stableId.isBlank()) return false
        val items = try {
            GlobalLessonData.createLessonItems(partId).map { it.type to it.stableId }
        } catch (e: Throwable) {
            Log.w(TAG, "Bölüm $partId listesi alınamadı", e)
            return false
        }
        if (!isUnitOpener(items, stableId)) return false
        return try {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val key = PREFIX_SHOWN + stableId
            if (p.getBoolean(key, false)) return false
            p.edit().putBoolean(key, true).apply()
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Anket defteri yazılamadı", e)
            false
        }
    }
}
