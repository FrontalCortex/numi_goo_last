package com.example.app

/**
 * Seri dondurmanın karar kuralı — yan etkisiz.
 *
 * ## Ne
 * Mağazadan altınla alınan tek kullanımlık koruma: kaçırılan İLK günü kapatır, seri
 * kırılmaz. Kapatılan gün seriye EKLENMEZ: 5 günlük seri 5 kalır, ertesi gün çalışılınca 6
 * olur. Eklenseydi altınla gün satın alınır, kilometre taşı ödülleri de onunla gelirdi.
 *
 * ## Nasıl temsil ediliyor
 * Kaçan gün "köprü" olarak serinin son günü yapılıyor ([StreakRepository] `last_goal_day`)
 * ve donmuş günlere ekleniyor. Böylece ardışıklık kontrolünün tek satırı değişmiyor: ertesi
 * gün hedef tutturulduğunda "son gün dün mü" sorusu kendiliğinden evet çıkıyor.
 *
 * ## Neden ayrı dosya
 * Kural iki yerde yaşıyor: burada ve sunucuda (`functions/index.js` → `settleStreakFreeze`).
 * İkisi aynı girdiyle aynı sonucu vermek ZORUNDA; yoksa çocuk çevrimdışıyken "serin
 * korundu" görüp eşitlemeden sonra kırık seriyle karşılaşır. SharedPreferences'tan ayrı
 * durunca kural cihaz olmadan test edilebiliyor (bkz. `StreakFreezeRulesTest`) — cihazda
 * denemek günlerce beklemek ya da saati oynatmak demek, ve saat oynatmak daha önce seriyi
 * kalıcı olarak dondurmuştu.
 */
internal object StreakFreezeRules {

    /** Dondurma harcandı; [missedDay] kapatılan gün ve serinin yeni son günü. */
    data class Settled(val missedDay: String)

    /**
     * Eldeki dondurmanın harcanıp harcanmayacağı; harcanmıyorsa null.
     *
     * İki gün üst üste kaçtıysa dondurma YİNE harcanıyor ve seri yine kırılıyor (ürün kararı:
     * dondurma kaçan ilk günün bedeli, serinin kurtulmasının garantisi değil). Kırılmayı bu
     * fonksiyon yazmıyor; köprüden sonra son gün hâlâ dünden eskiyse [StreakRepository.refresh]
     * içindeki mevcut kırılma dalı çalışıyor.
     *
     * @param freezeDay Dondurmanın satın alındığı gün. Dondurma ondan ÖNCESİNİ kurtarmaz;
     *   yoksa dün kırılan seri bugün dondurma alınarak "onarılabilirdi".
     * @param scanDays Son günün kaç gün geriye kadar aranacağı. Daha eskisi burada
     *   kapatılmıyor: o seri zaten çoktan kırık, dondurmanın akıbetini bir sonraki
     *   eşitlemede sunucu söylüyor.
     * @param dayIdAt Bugüne göre kaydırılmış gün kimliği (0 = bugün, -1 = dün). Tarih
     *   ayrıştırılmıyor: gün kimliğini üreten kodun aynısı hesabı da yapıyor
     *   (bkz. [StreakRepository] içindeki `daysSince`).
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
        // Son gün dün ya da sonrasıysa arada tam bir gün yok: bugün henüz bitmedi, kaçmış
        // sayılmaz. yyyy-MM-dd biçiminde sözlük sırası tarih sırasıyla aynı.
        if (lastDay >= dayIdAt(-1)) return null
        val offset = (-2 downTo -scanDays).firstOrNull { dayIdAt(it) == lastDay } ?: return null
        val missedDay = dayIdAt(offset + 1)
        if (freezeDay.isNotEmpty() && missedDay < freezeDay) return null
        return Settled(missedDay)
    }
}
