// Kaynak (Android): app/src/main/java/com/example/app/CupScoreService.kt (YENİ) — kupa dersi sonucunu sunucuya
// bildiren `submitCupResult` çağrısının yükü, yanıt ayrıştırma ve ders → kupa alanı eşlemesi.
// Taşınmayanlar: Firebase Functions çağrısı, Log.
package com.sorobit.shared.kupa

/**
 * Kupa dersi sonucunu sunucuya bildirir (`submitCupResult`).
 *
 * Neden istemci artık kupa puanı yazmıyor: kupa puanı bir musluk (kupa yolu sandıkları ve kupa rozetleri ona
 * bakar). `cupWayProgress` istemciye açıkken "kupamı 999999 yap" demek bedava sandık demekti; koleksiyon artık
 * yazmaya kapalı ve puanı yalnızca sunucu değiştirir. İstemci DEĞİŞİM MİKTARINI da göndermez — yalnızca dersin
 * kazanılıp kazanılmadığını, zorluk kademesini ve modunu söyler; miktarı sunucu kendi tablosundan hesaplar.
 *
 * Akış (Android `BadgePrecalcHelper.executeCupDeltaUpdateAsync`): `won = delta > 0` ile bu çağrı yapılır, dönen
 * `newScore` ile ilgili hayvan rozetinin `sync*ProgressAndDetectLevelUp` senkronu çalışır ve oradan çıkan kademe
 * listesi kutlama için yayınlanır (rozet kademesi artık yerel "eski puan → yeni puan" ile hesaplanmaz: kupanın
 * GÖRDÜĞÜ EN YÜKSEK değer işlem içinde okunur).
 */
object CupScoreService {

    /** Callable adı. */
    const val CF_SUBMIT_CUP_RESULT = "submitCupResult"

    /** Zorluk kademesi sınırları (Android: `difficultyLevel.coerceIn(0, 4)`). */
    const val MIN_DIFFICULTY = 0
    const val MAX_DIFFICULTY = 4

    /**
     * `submitCupResult` istek gövdesi.
     *
     * @param dayId geçmiş grafiğinin günü, "yyyy-MM-dd" YEREL saat dilimiyle (sunucu UTC kullansaydı gece
     *   yarısından sonraki dersler bir önceki güne yazılırdı); varsayılan [CupHistoryRepository.todayId].
     */
    fun buildSubmitPayload(
        cupField: String,
        won: Boolean,
        difficultyLevel: Int,
        isMultiplication: Boolean,
        dayId: String = CupHistoryRepository.todayId(),
    ): Map<String, Any> = hashMapOf(
        "cupField" to cupField,
        "won" to won,
        "difficultyLevel" to difficultyLevel.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY),
        "isMultiplication" to isMultiplication,
        "dayId" to dayId,
    )

    /** Sunucu yanıtı: eski ve yeni puan. */
    data class CupScoreResult(val oldScore: Int, val newScore: Int)

    /**
     * Yanıtı ayrıştırır. `oldScore` ya da `newScore` eksik/sayı değilse null (Android: uyarı yazıp `onDone`
     * çağırmaz — arayüz eski değeri göstermeye devam eder).
     */
    fun parseResult(resultData: Any?): CupScoreResult? {
        val data = resultData as? Map<*, *> ?: return null
        val oldScore = (data["oldScore"] as? Number)?.toInt() ?: return null
        val newScore = (data["newScore"] as? Number)?.toInt() ?: return null
        return CupScoreResult(oldScore, newScore)
    }

    /**
     * Ders hangi kupa yoluna yazılacak — kartlardaki eşleşmenin aynısı (Android `cupFieldFor`;
     * bayraklar LessonItem'daki `Boolean?`, null = false).
     */
    fun cupFieldFor(isMultiplication: Boolean?, isExtraction: Boolean?, isBlinding: Boolean?): String = when {
        isMultiplication == true && isBlinding == true -> CupPathRewardRepository.FIELD_BLINDING_IMPACT
        isMultiplication == true -> CupPathRewardRepository.FIELD_IMPACT
        isExtraction == true && isBlinding == true -> CupPathRewardRepository.FIELD_BLINDING_EXTRACTION
        isExtraction == true -> CupPathRewardRepository.FIELD_EXTRACTION
        isBlinding == true -> CupPathRewardRepository.FIELD_BLINDING_ADDITION
        else -> CupPathRewardRepository.FIELD_ADDITION
    }
}
