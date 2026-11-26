// Kaynak: AppRatingManager.kt — `AppRatingManager.checkAndShowRatingPrompt` karar zinciri,
// `markAsRated` anahtarı; `RatingDialogFragment` içindeki kurallar (4+ yıldız → mağaza puanlaması,
// altı → geri bildirim; yıldız boyama; geri bildirim belgesi).
// Dışarıda bırakılan: diyalog görünümü, Play In-App Review çağrısı, Firestore yazımı.
// Farklar: Android'de `object` idi; burada sınıf (son deneme zamanı örnek durumunda — tek paylaşılan
// örnek kullanılmalı). SharedPreferences ("AppRatingPrefs") → [OyunKeyValueStore]; FragmentManager
// sorguları ve `GlobalValues.pendingCupPathRevealPartId` → parametre; Log çağrıları kaldırıldı.
// Android'deki hâliyle korunan notlar:
//  - Eşik "en az 1 tamamlanmış ders"tir (kaynakta "for testing" notuyla).
//  - Son 2 saniye içinde yeniden çağrılırsa, diyalog gösterilmese bile TRUE döner.
//  - 2 saniye kuralı, AdSkip / Kupa Yolu denetimlerinden ÖNCE zaman damgasını günceller; yani bu
//    denetimlere takılan bir deneme de sonraki 2 saniyeyi "gösterildi" sayar.
//  - "Hayır, teşekkürler" de hesabı "puanladı" olarak işaretler.
package com.sorobit.shared.oyun

/** [AppRatingManager.checkAndShowRatingPrompt] sonucu. */
data class RatingPromptDecision(
    /** Android fonksiyonunun dönüş değeri ("puanlama akışı devrede" anlamında). */
    val result: Boolean,
    /** true ise çağıran puanlama diyaloğunu ("RatingDialog" etiketiyle) göstermelidir. */
    val showDialog: Boolean,
)

class AppRatingManager(
    /** Android: "AppRatingPrefs" adlı SharedPreferences. */
    private val prefs: OyunKeyValueStore,
    private val uid: () -> String?,
    private val nowMs: () -> Long = ::oyunSimdiMs,
) {
    private var lastPromptAttemptTime = 0L

    private fun getHasRatedKey(): String = hasRatedKey(uid())

    /**
     * @param adSkipShowing "AdSkip" etiketli ekran şu an açık mı
     * @param cupPathRevealPending Kupa Yolu yönlendirmesi bekliyor mu (`pendingCupPathRevealPartId != null`)
     * @param ratingDialogShowing "RatingDialog" zaten açık mı
     */
    fun checkAndShowRatingPrompt(
        completedLessonCount: Int,
        adSkipShowing: Boolean,
        cupPathRevealPending: Boolean,
        ratingDialogShowing: Boolean,
    ): RatingPromptDecision {
        // 1. Bu hesap zaten puanladıysa bir daha gösterme
        if (prefs.getBoolean(getHasRatedKey(), false)) {
            return RatingPromptDecision(result = false, showDialog = false)
        }

        // 2. Yeterli ders tamamlanmış mı
        if (completedLessonCount < MIN_COMPLETED_LESSONS) {
            return RatingPromptDecision(result = false, showDialog = false)
        }

        val now = nowMs()
        if (now - lastPromptAttemptTime < PROMPT_DEBOUNCE_MS) {
            return RatingPromptDecision(result = true, showDialog = false) // Son 2 saniye içinde zaten tetiklendi
        }
        lastPromptAttemptTime = now

        // Reklam sonrası ekran açıkken gösterme
        if (adSkipShowing) {
            return RatingPromptDecision(result = false, showDialog = false)
        }

        // Kupa Yolu yönlendirmesi bekliyorsa gösterme
        if (cupPathRevealPending) {
            return RatingPromptDecision(result = false, showDialog = false)
        }

        // Zaten açıksa ikinci kez açma
        if (ratingDialogShowing) {
            return RatingPromptDecision(result = true, showDialog = false)
        }

        return RatingPromptDecision(result = true, showDialog = true)
    }

    fun markAsRated() {
        prefs.putBoolean(getHasRatedKey(), true)
    }

    companion object {
        const val PREFS_NAME = "AppRatingPrefs"
        const val TAG_RATING_DIALOG = "RatingDialog"
        const val TAG_AD_SKIP = "AdSkip"
        const val MIN_COMPLETED_LESSONS = 1
        const val PROMPT_DEBOUNCE_MS = 2000L

        /** Bu yıldız sayısı ve üzeri mağaza puanlamasına yönlendirilir; altı geri bildirim ister. */
        const val STORE_REVIEW_MIN_STARS = 4

        /** Yıldız seçildikten sonra karar uygulanmadan önceki kısa bekleme. */
        const val STAR_SELECT_DELAY_MS = 300L

        const val COLLECTION_RATING_FEEDBACK = "ratingFeedback"
        const val FIELD_UID = "uid"
        const val FIELD_STARS = "stars"
        const val FIELD_FEEDBACK = "feedback"

        /** Değeri sunucu zaman damgasıdır (`FieldValue.serverTimestamp()`); platform ekler. */
        const val FIELD_TIMESTAMP = "timestamp"
        const val ANONYMOUS_UID = "anonymous"

        const val TITLE_LOW_RATING = "Üzgünüz 😔"
        const val SUBTITLE_LOW_RATING = "Deneyimini geliştirmek için geri bildirimine ihtiyacımız var."
        const val TOAST_FEEDBACK_THANKS = "Geri bildiriminiz için teşekkürler!"
        const val TOAST_FEEDBACK_EMPTY = "Lütfen bir şeyler yazın."

        fun hasRatedKey(uid: String?): String = "has_rated_${uid ?: ANONYMOUS_UID}"

        /** Seçilen yıldıza göre: true → mağaza puanlama akışı, false → geri bildirim alanı. */
        fun shouldLaunchStoreReview(selectedRating: Int): Boolean = selectedRating >= STORE_REVIEW_MIN_STARS

        /** `updateStars`: [index] (0..4) numaralı yıldız dolu mu. */
        fun isStarOn(index: Int, rating: Int): Boolean = index < rating

        /** Yıldız görselinin kaynak adı. */
        fun starDrawable(index: Int, rating: Int): String = if (isStarOn(index, rating)) "star_on_ic" else "star_off_ic"

        /** Geri bildirim gönderilebilir mi (kırpılmış metin boş değilse). */
        fun canSubmitFeedback(feedbackText: String): Boolean = feedbackText.trim().isNotEmpty()

        /** Geri bildirim belgesinin kimliği (`ratingFeedback/{uid}`; oturum yoksa "anonymous"). */
        fun feedbackDocId(uid: String?): String = uid ?: ANONYMOUS_UID

        /**
         * `ratingFeedback/{uid}` belgesine merge ile yazılan alanlar ([FIELD_TIMESTAMP] hariç —
         * sunucu zaman damgasını platform ekler).
         */
        fun feedbackData(uid: String?, stars: Int, feedback: String): Map<String, Any> = hashMapOf(
            FIELD_UID to (uid ?: ANONYMOUS_UID),
            FIELD_STARS to stars,
            FIELD_FEEDBACK to feedback,
        )
    }
}
