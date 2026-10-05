package com.example.app

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import android.util.Log

/**
 * Cihaz saatinin elle değiştirilmesinden etkilenmeyen "şimdi".
 *
 * ## Neden var
 * Can (enerji) sunucuda tutuluyor (`energy_full_time`: canın dolacağı an) ama "şu an kaç
 * can var" sorusunu istemci, o değeri CİHAZ saatiyle karşılaştırarak cevaplıyordu. Çocuk
 * saati ileri alınca uygulama canı dolu sanıyor, dersi başlatıyor; sunucu harcamayı
 * reddetse de ders çoktan açılmış oluyordu. Yani saatle oynayarak sınırsız ders — Pro'nun
 * satış noktası — bedavaya geçiliyordu.
 *
 * ## Nasıl çalışıyor
 * Sunucu saatini zaten öğreniyoruz ([SeasonClock.refreshFromServer], her açılışta ve
 * saatte bir). O an, cihazın MONOTON saatiyle (`SystemClock.elapsedRealtime`) birlikte
 * çapa olarak saklanıyor; "şimdi" = sunucu anı + o zamandan beri geçen gerçek süre.
 * Monoton saat duvar saatinden bağımsız akıyor, ayarlardan değiştirilemiyor.
 *
 * Çapa diske de yazılıyor: aynı açılışta uygulama yeniden başlasa bile sunucu cevabını
 * beklemeden geçerli. Cihaz yeniden başlatılınca geçersizleşiyor (monoton saat sıfırlanır);
 * yeni çapa ilk sunucu cevabıyla geliyor.
 *
 * ## Kapatamadığı
 * Çapa yokken (cihaz yeniden başlatıldı VE internet yok) duvar saatine dönülüyor. Yani
 * "saati ileri al, yeniden başlat, uçak modunda oyna" hâlâ mümkün. Tamamen kapatmak, ders
 * başlatmayı sunucu onayına bağlamak demek; o zaman internetsiz ders açılamaz.
 */
object TrustedClock {

    private const val TAG = "TrustedClock"
    private const val PREFS = "trusted_clock"
    private const val KEY_SERVER_NOW_MS = "server_now_ms"
    private const val KEY_ELAPSED_MS = "elapsed_ms"
    private const val KEY_BOOT_COUNT = "boot_count"

    /** Hiçbir gerçek açılış sayacıyla eşleşmeyen değer; bkz. [init]. */
    private const val UNKNOWN_BOOT = -1

    @Volatile private var anchor: TrustedClockRules.Anchor? = null
    @Volatile private var bootCount: Int = 0

    /** Ölçüme en son bildirilen sapma kovası; bkz. [reportSkew]. */
    @Volatile private var lastSkewBucket: String? = null
    private var prefs: SharedPreferences? = null

    /**
     * Çapa değişince haber verilecekler.
     *
     * Çapa uygulama açıldıktan bir iki saniye sonra geliyor; cihaz saati yanlışsa o ana
     * kadar gösterilen can sayısı da yanlış. Dinleyen ([EnergyManager]) göstergeyi hemen
     * tazeliyor, bir sonraki can dolumunu beklemiyor.
     */
    private val anchorListeners = java.util.concurrent.CopyOnWriteArrayList<() -> Unit>()

    fun addAnchorListener(listener: () -> Unit) {
        anchorListeners.add(listener)
    }

    fun removeAnchorListener(listener: () -> Unit) {
        anchorListeners.remove(listener)
    }

    /** Diskteki çapayı yükler; [NumiGooApplication.onCreate] çağırıyor. Ağ çağrısı yok. */
    fun init(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        // Açılış sayacı okunamazsa 0 kalıyor; o zaman yeniden başlatma yalnızca monoton
        // saatin geriye gitmesinden anlaşılıyor (bkz. TrustedClockRules.trustedNow).
        bootCount = runCatching {
            Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, 0)
        }.getOrDefault(0)
        if (p.contains(KEY_SERVER_NOW_MS)) {
            val savedBootCount = p.getInt(KEY_BOOT_COUNT, 0)
            // Açılış sayacı okunamıyorsa (0) diskteki çapanın BU açılışa ait olduğu
            // kanıtlanamaz: cihaz yeniden başlatılıp eskisinden uzun süre açık kalmışsa
            // monoton saat de ele vermez ve "şimdi" gerçeğin gerisinde kalır — can geç
            // dolar, internet gelene kadar da düzelmezdi. O durumda çapa hiçbir açılışla
            // eşleşmeyen bir sayaçla yükleniyor: yalnızca alt sınır olarak kullanılıyor,
            // tam değer ilk sunucu cevabıyla geliyor.
            val bootKnown = bootCount != 0 && savedBootCount != 0
            anchor = TrustedClockRules.Anchor(
                serverNowMs = p.getLong(KEY_SERVER_NOW_MS, 0L),
                elapsedMs = p.getLong(KEY_ELAPSED_MS, 0L),
                bootCount = if (bootKnown) savedBootCount else UNKNOWN_BOOT,
            )
        }
    }

    /** Sunucunun bildirdiği anı çapa yapar; [SeasonClock.onServerTime] çağırıyor. */
    fun onServerTime(serverNowMs: Long) {
        val elapsed = SystemClock.elapsedRealtime()
        anchor = TrustedClockRules.Anchor(serverNowMs, elapsed, bootCount)
        prefs?.edit()
            ?.putLong(KEY_SERVER_NOW_MS, serverNowMs)
            ?.putLong(KEY_ELAPSED_MS, elapsed)
            ?.putInt(KEY_BOOT_COUNT, bootCount)
            ?.apply()
        // Fark büyükse cihaz saati yanlış ya da oynanmış demektir; teşhiste işe yarıyor.
        val skewMs = System.currentTimeMillis() - serverNowMs
        Log.d(TAG, "capa kuruldu | cihazSaatiFarki=${skewMs}ms")
        reportSkew(skewMs)
        anchorListeners.forEach { runCatching { it() } }
    }

    /**
     * Belirgin sapmayı ölçüme bildirir.
     *
     * Bu fonksiyon her açılışta ve saatte bir çalışıyor ([SeasonClock.refreshFromServer]),
     * yani her çağrıda olay göndermek kullanıcı başına günde onlarca kayıt demekti. İki
     * süzgeç var:
     *
     * - 5 dakikanın altı hiç gönderilmiyor: o aralık ağ gecikmesi ve normal saat kayması.
     * - Aynı kova tekrar gönderilmiyor. Böylece saatlik tazeleme sessiz kalıyor ama kova
     *   DEĞİŞİRSE (çocuk uygulama açıkken saati ileri aldı, ya da saat düzeldi) yakalanıyor.
     *
     * Süreç başına durum: uygulama yeniden açılınca tekrar bir kez gönderilir, o da
     * "kaç kullanıcıda var" sorusu için doğru olan.
     */
    private fun reportSkew(skewMs: Long) {
        val bucket = AnalyticsLogger.skewBucket(skewMs)
        if (bucket == AnalyticsLogger.SKEW_UNDER_5M) {
            lastSkewBucket = bucket
            return
        }
        if (bucket == lastSkewBucket) return
        lastSkewBucket = bucket
        // Kova yönü de taşıyor (`ahead_1h_6h`); ayrı bir yön parametresi ikinci bir özel
        // boyut yuvası yerdi, bkz. [AnalyticsLogger.skewBucket].
        AnalyticsLogger.logDeviceClockSkew(bucket)
    }

    /** Duvar saati değiştirilse de kaymayan "şimdi" (Unix ms). */
    fun nowMs(): Long = TrustedClockRules.trustedNow(
        anchor = anchor,
        wallNowMs = System.currentTimeMillis(),
        elapsedNowMs = SystemClock.elapsedRealtime(),
        bootCount = bootCount,
    )
}
