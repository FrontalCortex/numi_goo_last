// Kaynak (Android): app/src/main/java/com/example/app/StreakFreezeRules.kt — tamamı.
//
// Seri dondurmanın karar kuralı — yan etkisiz. İkizi sunucuda (functions/index.js →
// settleStreakFreeze); ikisi aynı girdiyle aynı sonucu vermek ZORUNDA.
//
// Dondurma kaçırılan İLK günü kapatır, seri kırılmaz. Kapatılan gün seriye EKLENMEZ (5 günlük seri
// 5 kalır, ertesi gün çalışılınca 6 olur). Kaçan gün "köprü" olarak serinin son günü yapılır ve donmuş
// günlere eklenir; böylece ardışıklık kontrolü değişmeden çalışır.
package com.sorobit.shared.seri

object StreakFreezeRules {

    /** Dondurma harcandı; [missedDay] kapatılan gün ve serinin yeni son günü. */
    data class Settled(val missedDay: String)

    /**
     * Eldeki dondurmanın harcanıp harcanmayacağı; harcanmıyorsa null.
     *
     * İki gün üst üste kaçtıysa dondurma YİNE harcanır ve seri yine kırılır (ürün kararı). Kırılmayı
     * bu fonksiyon yazmaz; köprüden sonra son gün hâlâ dünden eskiyse StreakRepository.refresh
     * içindeki kırılma dalı çalışır.
     *
     * @param freezeDay Dondurmanın satın alındığı gün; ondan ÖNCESİNİ kurtarmaz.
     * @param scanDays Son günün kaç gün geriye kadar aranacağı.
     * @param dayIdAt Bugüne göre kaydırılmış gün kimliği (0 = bugün, -1 = dün).
     */
    fun settle(
        current: Int,
        lastDay: String,
        freezes: Int,
        freezeDay: String,
        scanDays: Int,
        dayIdAt: (Int) -> String,
    ): Settled? {
        if (freezes <= 0 || current <= 0 || lastDay.isEmpty()) return null
        // Son gün dün ya da sonrasıysa arada tam bir gün yok: bugün henüz bitmedi.
        // yyyy-MM-dd biçiminde sözlük sırası tarih sırasıyla aynı.
        if (lastDay >= dayIdAt(-1)) return null
        val offset = (-2 downTo -scanDays).firstOrNull { dayIdAt(it) == lastDay } ?: return null
        val missedDay = dayIdAt(offset + 1)
        if (freezeDay.isNotEmpty() && missedDay < freezeDay) return null
        return Settled(missedDay)
    }
}
