package com.example.app

import android.os.SystemClock

/**
 * Oturum içinde başlatılan ders sayısını sayar.
 *
 * ## Neden var
 * Enerji duvarının zararlı olup olmadığı, duvara çarpan çocuğun o oturumda **kaç ders
 * yaptığına** bağlı. İki senaryo tamamen farklı:
 *
 * - 5 ders yapıp duvara çarpmak: beklenen davranış, enerji sistemi işini yapıyor.
 * - 1 ders yapıp duvara çarpmak: çocuk günün ikinci kez uygulamayı açmış, bar henüz
 *   dolmamış ve neredeyse kapıdan geri çevrilmiş. Asıl zararlı olan bu.
 *
 * Bu ayrımı GA4'te sonradan türetmek mümkün değil: Keşfet arayüzü "bir olaydan önce aynı
 * oturumda kaç kez başka bir olay olmuş" hesabını yapamıyor. Bu yüzden sayı, olayın içine
 * parametre olarak yazılır.
 *
 * ## Oturum tanımı
 * GA4'ün oturum tanımıyla aynı: uygulama **30 dakikadan uzun** arka planda kaldıysa yeni
 * oturum sayılır ve sayaç sıfırlanır. Böylece buradaki sayı ile GA4'ün oturum kırılımı
 * birbirini tutar.
 *
 * Sayaç yalnızca bellekte tutulur; kalıcı olması gerekmiyor, çünkü uygulama tamamen
 * kapandığında zaten yeni oturum başlıyor.
 */
object EnergySessionCounter {

    /** GA4'ün varsayılan oturum zaman aşımı. */
    private const val SESSION_GAP_MS = 30 * 60 * 1000L

    @Volatile
    private var lessonsStarted: Int = 0

    /** Uygulamanın en son arka plana geçtiği an; 0 = henüz hiç arka plana geçmedi. */
    @Volatile
    private var backgroundedAtMs: Long = 0L

    /**
     * Bir ders başlatıldı. Enerji harcansın harcanmasın sayılır.
     *
     * İki çağrı yeri var ve ikisi de şart: haritadan açılan ders (bölüm 1-8) ve kupa yarışı
     * (bölüm 9). Kupa tarafı eksik kalırsa yalnız kupa oynayan çocuk için sayaç hep 0'da kalır.
     */
    fun onLessonStarted() {
        lessonsStarted++
    }

    /**
     * Sayının GA4'te satır/sütun olarak kullanılabilir kova hâli.
     *
     * Ham sayı yerine kova gönderiliyor: GA4 Keşfet'te sayısal bir boyutu aralıklara
     * ayırmak mümkün değil, kovayı burada üretmek raporu tek tıkla kurulabilir yapıyor.
     */
    fun bucket(): String = when (val n = lessonsStarted) {
        0 -> "0"
        1, 2 -> "1-2"
        3, 4 -> "3-4"
        else -> if (n >= 5) "5+" else "0"
    }

    /** Uygulama tamamen arka plana geçti. */
    fun onAppBackgrounded() {
        backgroundedAtMs = SystemClock.elapsedRealtime()
    }

    /**
     * Uygulama öne döndü. Arada 30 dakikadan fazla geçtiyse yeni oturum sayılır.
     *
     * `elapsedRealtime` kullanılıyor, `uptimeMillis` değil: ikincisi cihaz derin uykudayken
     * durur ve gece boyu kapalı kalan bir telefonda "arada hiç zaman geçmemiş" gibi görünür.
     */
    fun onAppForegrounded() {
        if (backgroundedAtMs == 0L) return
        if (SystemClock.elapsedRealtime() - backgroundedAtMs >= SESSION_GAP_MS) {
            lessonsStarted = 0
        }
    }
}
