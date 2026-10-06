package com.example.app

import android.util.Log
import java.util.concurrent.atomic.AtomicInteger

/**
 * Günlük seri (streak) zincirinin teşhisi.
 *
 * Logcat filtresi: `StreakDiag`
 *
 * ## Neden var
 * Seri "tutturdum ama sayılmadı" diye bildirildiğinde zincirin NEREDE durduğu
 * görülemiyordu: [StudyTimeTracker]'da sıcak yolda hiç log yoktu,
 * [StreakRepository.refresh] hiç log basmıyordu ve [StreakSyncService] yalnızca
 * başarısızlıkta tek satır `Log.w` atıyordu. Zincirin beş halkası var ve hepsi
 * sessizce "hiçbir şey yapma" diyebiliyor:
 *
 * 1. Süre sayıldı mı — ekran çalışma ekranı mı, parça kapanınca kaç saniye yazıldı.
 * 2. [StreakRepository.refresh] hedefi tutturulmuş saydı mı, hangi dala girdi.
 * 3. Gün kuyruğa girdi mi.
 * 4. [StreakSyncService.syncPendingDays] gerçekten gitti mi, yoksa erken mi döndü.
 * 5. Sunucu ne döndü ve [StreakRepository.adoptServerState] onu benimsedi mi.
 *
 * Satır başındaki `#N` sırayı veriyor; akışı okurken ona bakın.
 *
 * ## Kapatmak
 * [ENABLED] false yapılır. Yayına çıkarken kapatılacak — bkz.
 * `docs/YAYIN_ONCESI_KONTROL.md`.
 */
object StreakDiag {
    const val LOG_TAG = "StreakDiag"

    /** Yayına çıkarken false. Teşhis satırları ucuz ama kullanıcıya faydası yok. */
    const val ENABLED = false

    private val seq = AtomicInteger(0)

    fun log(caller: String, message: String) {
        if (!ENABLED) return
        Log.i(LOG_TAG, "#${seq.incrementAndGet()} | $caller | $message")
    }
}
