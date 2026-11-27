// Kaynak (Android): app/src/main/java/com/example/app/CupHistoryRepository.kt — saf kısım
// (yollar, todayId, mapSnapshot). Firestore okuma çağrıları taşınmadı.
// ANDROID (güncel): yalnızca OKUMA kaldı; geçmiş kaydını kupa puanıyla aynı transaction'da sunucu yazar
// (`submitCupResult`, günü istemci [todayId] ile bildirir). Android'de recordSnapshot ve todayId silindi;
// ortak kodda todayId `dayId` için, recordSnapshot yalnızca eski davranışın belgesi olarak durur.
package com.sorobit.shared.kupa

import kotlinx.datetime.TimeZone

/**
 * Kupa skorlarının günlük geçmişini `users/{uid}/cupHistory/{yyyy-MM-dd}` altında tutar.
 *
 * Her doküman, o günün son değerlerini `cupWayProgress/progress` ile aynı alan adlarıyla saklar.
 * Aynı gün içindeki tekrarlı güncellemeler merge ile üzerine yazılır.
 */
object CupHistoryRepository {

    const val COLLECTION = "users"
    const val SUBCOLLECTION = "cupHistory"

    /** Android: `SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())` (yerel saat dilimi). */
    fun todayId(
        nowMs: Long = kupaNowMs(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String = kupaDateId(daysAgo = 0, nowMs = nowMs, timeZone = timeZone)

    fun historyCollectionPath(uid: String): String = "$COLLECTION/$uid/$SUBCOLLECTION"

    /** Bugünün geçmiş dokümanının yolu (Android `recordSnapshot` içindeki `ref`). */
    fun historyDocumentPath(uid: String, dateId: String = todayId()): String =
        "${historyCollectionPath(uid)}/$dateId"

    /** Android `recordSnapshot`: mevcut transaction içinde merge ile yazılan harita. */
    fun recordSnapshot(field: String, value: Int): Map<String, Any> = mapOf(field to value)

    /**
     * Android `mapSnapshot`: (doküman id "yyyy-MM-dd", doküman verisi) çiftlerinden [field] değeri sayı olanları
     * (tarih, değer) olarak tarihe göre artan sırada döndürür.
     *
     * `fetchHistory` tüm koleksiyonu, `fetchHistorySince` ise `documentId() >= sinceDateId` aralığını okur;
     * okuma hatasında Android boş liste döndürür.
     */
    fun mapSnapshot(
        documents: List<Pair<String, Map<String, Any?>>>,
        field: String,
    ): List<Pair<String, Int>> =
        documents
            .mapNotNull { (id, data) -> (data[field] as? Number)?.toInt()?.let { id to it } }
            .sortedBy { it.first }
}
