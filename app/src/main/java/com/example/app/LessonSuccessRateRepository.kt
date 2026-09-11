package com.example.app

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Part 1-8'deki chest/lesson tipi [com.example.app.model.LessonItem]'lerin geçme/geçememe
 * istatistiklerini Firebase Analytics'e bildirir.
 *
 * ## Taşıma notu (önceki mimari)
 * Bu sayaçlar eskiden `successRate/lessonSuccessRate/part{partId}/{position}` ve altındaki
 * `steps/{step}` dokümanlarında KÜRESEL sayaç olarak tutuluyordu. Üç sebeple Analytics'e alındı:
 *
 * 1. **Hiçbir yerde okunmuyordu.** Uygulama bu koleksiyondan tek bir değer bile okumuyordu;
 *    veri yalnızca konsoldan bakmak içindi — yani tanımı gereği ölçüm verisiydi.
 * 2. **Sıcak nokta riski.** Her kullanıcı aynı dokümana yazıyordu. Uygulamayı ilk açan herkes
 *    `part1/1/steps/1`'e gittiği için, yeni kullanıcı akınında (kampanya, dönem başı) tek
 *    doküman üzerindeki transaction'lar çakışır. Üstelik başarısız transaction yalnızca
 *    loglanıyordu: en çok veri istenen anda veri sessizce kayboluyordu.
 * 3. **Manipülasyona açıktı.** Güvenlik kuralı giriş yapmış herkese yazma (ve okuma) izni
 *    veriyordu; herhangi bir kullanıcı sayaçları istediği değere çekebilirdi.
 *
 * ## Kovalar kalktı, ham değer gönderiliyor
 * Eski şemada `failRate100to80Count` ve `finishTime0to30Count` gibi dilimler KODA GÖMÜLÜYDÜ;
 * dilim sınırını değiştirmek uygulama güncellemesi gerektiriyor, geçmiş veri de eski sınırlarla
 * kalıyordu. Analytics'e artık ham değer gidiyor (`answer_success_rate`, `elapsed_ms`) ve
 * dilimler konsolda/BigQuery'de sonradan, kod değiştirmeden tanımlanıyor. Aynı şekilde
 * `passOnFirstTry`/`passOnSecondTry`/`passOnThirdPlusTry` üçlüsü tek bir `fail_streak`
 * parametresine indi; üçü de ondan türetilebiliyor.
 *
 * `attemptUserCount`, `passUserCount` gibi "kaç kullanıcı" sayaçlarına da gerek kalmadı:
 * Analytics her olay için olay sayısını ve BENZERSİZ KULLANICI sayısını zaten ayrı ayrı verir.
 *
 * ## Firestore'da KALAN tek şey: çift sayım koruması
 * `users/{uid}/lessonSuccessRateState/{partId}_{position}_{step}`:
 * - `attempted` : kullanıcı bu adımı en az bir kez denedi mi
 * - `passed`    : kullanıcı bu adımı en az bir kez geçti mi (geçtikten sonra artık olay üretilmez)
 * - `failStreak`: ilk başarıya kadar kaç kez başarısız olundu
 *
 * Bu doküman kullanıcıya özeldir (sıcak nokta yok), cihazlar arasında senkron olmalıdır ve
 * ileride "hedefli tekrar" modunun temeli olacaktır; bu yüzden Firestore'da kalır.
 * Hesap silme akışı onu zaten temizliyor (bkz. [AccountDeletionHelper]).
 *
 * Olay yalnızca transaction "bu gerçekten ilk kez" dediğinde gönderilir; transaction yeniden
 * denenebildiği için gönderim lambda'nın İÇİNDE değil, başarı callback'inde yapılır.
 *
 * Tüm fonksiyon imzaları korunmuştur; çağrı yerleri değişmedi.
 */
object LessonSuccessRateRepository {

    private const val TAG = "LessonSuccessRateRepo"

    private const val FIELD_ATTEMPTED = "attempted"
    private const val FIELD_PASSED = "passed"
    private const val FIELD_FAIL_STREAK = "failStreak"

    private fun stateCollection(uid: String) =
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("lessonSuccessRateState")

    /**
     * [position]'daki dersin değişmez kimliği. Şablon dışı bir konum verilirse null döner ve
     * çağıran taraf eski (sıra tabanlı) anahtarda kalır — veri kaybetmektense eski biçimde
     * tutmak yeğdir.
     */
    private fun stableIdOf(partId: Int, position: Int): String? =
        GlobalLessonData.createLessonItems(partId).getOrNull(position)?.stableId

    /** Yeni biçim: `{partId}_{stableId}_{step}`. */
    private fun userStepStateRef(uid: String, partId: Int, stableId: String, step: Int) =
        stateCollection(uid).document("${partId}_${stableId}_${step.coerceAtLeast(1)}")

    /**
     * Eski biçim: `{partId}_{position}_{step}`.
     *
     * Kimlik eskiden liste sırasıydı; müfredata araya ders eklendiğinde bu anahtarlar sessizce
     * yanlış derse bağlanır. Okuma sırasında hâlâ dikkate alınıyor ki geçmiş kaybolmasın —
     * dokunulan her adım ilk denemesinde yeni anahtara taşınır (bkz. [readStateMigrating]).
     */
    private fun legacyUserStepStateRef(uid: String, partId: Int, position: Int, step: Int) =
        stateCollection(uid).document("${partId}_${position}_${step.coerceAtLeast(1)}")

    /**
     * Kullanıcı bu adımı bitiremeden dersten çıktığında (quiz veya chest skoru yetersiz) çağrılır.
     *
     * [answerSuccessRatePercent] o başarısız denemenin başarı oranıdır (0-100). Eski davranışla
     * birebir aynı şekilde yalnızca part 1-6 için anlamlı kabul edilir ve yalnızca o aralıkta
     * gönderilir — 7-8'de çağıran taraf anlamlı bir oran üretmiyor.
     */
    fun recordFail(partId: Int, position: Int, step: Int, answerSuccessRatePercent: Float? = null) {
        if (partId !in 1..8) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val stableId = stableIdOf(partId, position)
        val targetRef = if (stableId != null) userStepStateRef(uid, partId, stableId, step)
                        else legacyUserStepStateRef(uid, partId, position, step)
        val legacyRef = if (stableId != null) legacyUserStepStateRef(uid, partId, position, step) else null

        FirebaseFirestore.getInstance().runTransaction { tx ->
            // Firestore kuralı: tüm okumalar yazmalardan önce.
            val targetSnap = tx.get(targetRef)
            val legacySnap = if (!targetSnap.exists() && legacyRef != null) tx.get(legacyRef) else null
            val userSnap = if (targetSnap.exists()) targetSnap else legacySnap ?: targetSnap

            // Zaten geçilmiş bir adım için başarısızlık sayılmaz (eski davranışla aynı).
            if (userSnap.getBoolean(FIELD_PASSED) == true) {
                return@runTransaction null
            }
            val newFailStreak = (userSnap.getLong(FIELD_FAIL_STREAK) ?: 0L) + 1

            tx.set(
                targetRef,
                mapOf(FIELD_ATTEMPTED to true, FIELD_FAIL_STREAK to newFailStreak),
                SetOptions.merge(),
            )
            // Eski anahtarlı kayıt bu adımın geçmişiydi; yeni anahtara taşındı, aslı silinir.
            if (legacySnap?.exists() == true && legacyRef != null) tx.delete(legacyRef)
            newFailStreak
        }.addOnSuccessListener { failStreak ->
            if (failStreak != null) {
                AnalyticsLogger.logLessonStepFail(
                    partId = partId,
                    position = position,
                    step = step,
                    failStreak = failStreak,
                    answerSuccessRatePercent = answerSuccessRatePercent.takeIf { partId in 1..6 },
                )
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "recordFail failed part=$partId position=$position step=$step", e)
            AnalyticsLogger.recordNonFatal(
                "LessonSuccessRateRepository.recordFail part=$partId position=$position step=$step",
                e,
            )
        }
    }

    /**
     * Kullanıcı bu adımı ilk kez başarıyla geçtiğinde (adım ilerlemesi veya item bitişi) çağrılır.
     * [elapsedMs] soru ekranına girişten bitirmeye kadar geçen süredir; eskiden süre dilimlerine
     * yuvarlanıyordu, artık ham gönderilir (bkz. sınıf başlığı).
     */
    fun recordPass(partId: Int, position: Int, step: Int, elapsedMs: Long? = null) {
        if (partId !in 1..8) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val stableId = stableIdOf(partId, position)
        val targetRef = if (stableId != null) userStepStateRef(uid, partId, stableId, step)
                        else legacyUserStepStateRef(uid, partId, position, step)
        val legacyRef = if (stableId != null) legacyUserStepStateRef(uid, partId, position, step) else null

        FirebaseFirestore.getInstance().runTransaction { tx ->
            // Firestore kuralı: tüm okumalar yazmalardan önce.
            val targetSnap = tx.get(targetRef)
            val legacySnap = if (!targetSnap.exists() && legacyRef != null) tx.get(legacyRef) else null
            val userSnap = if (targetSnap.exists()) targetSnap else legacySnap ?: targetSnap

            // İlk geçişten sonraki geçişler sayılmaz (eski davranışla aynı).
            if (userSnap.getBoolean(FIELD_PASSED) == true) {
                return@runTransaction null
            }
            val failStreak = userSnap.getLong(FIELD_FAIL_STREAK) ?: 0L

            tx.set(
                targetRef,
                mapOf(FIELD_ATTEMPTED to true, FIELD_PASSED to true),
                SetOptions.merge(),
            )
            if (legacySnap?.exists() == true && legacyRef != null) tx.delete(legacyRef)
            failStreak
        }.addOnSuccessListener { failStreak ->
            if (failStreak != null) {
                // failStreak == 0 → ilk denemede geçti.
                AnalyticsLogger.logLessonStepPass(
                    partId = partId,
                    position = position,
                    step = step,
                    failStreak = failStreak,
                    elapsedMs = elapsedMs,
                )
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "recordPass failed part=$partId position=$position step=$step", e)
            AnalyticsLogger.recordNonFatal(
                "LessonSuccessRateRepository.recordPass part=$partId position=$position step=$step",
                e,
            )
        }
    }

    /**
     * Kullanıcı bu adımın soru ekranına her girdiğinde (tekrarlar dahil) çağrılır.
     * Tekilleştirme yoktu, yok: her giriş bir olaydır.
     */
    fun recordQuestionEntry(partId: Int, position: Int, step: Int) {
        if (partId !in 1..8) return
        if (FirebaseAuth.getInstance().currentUser?.uid == null) return
        AnalyticsLogger.logLessonQuestionEntry(partId, position, step)
    }

    /** Kullanıcı bu adımın soru ekranını hiç cevap vermeden terk ettiğinde çağrılır. */
    fun recordAbandonWithoutAnswer(partId: Int, position: Int, step: Int) {
        if (partId !in 1..8) return
        if (FirebaseAuth.getInstance().currentUser?.uid == null) return
        AnalyticsLogger.logLessonAbandonWithoutAnswer(partId, position, step)
    }

    /**
     * Item (tüm adımları dahil) ilk kez tamamen bittiğinde (`stepIsFinish` false->true) çağrılır.
     * [chestStars] yalnızca TYPE_CHEST için verilir.
     *
     * Item seviyesinde çift sayım koruması gerekmez: çağıran taraf zaten
     * [com.example.app.model.LessonItem.stepIsFinish] alanına bakarak "ilk bitiş mi, tekrar mı"
     * ayrımını yapıp doğru fonksiyonu seçiyor. Bu yüzden burada Firestore'a hiç yazılmaz.
     */
    fun recordItemFirstFinish(partId: Int, position: Int, chestStars: Int? = null) {
        if (partId !in 1..8) return
        // Oturum açmamış kullanıcı eskiden de sayılmıyordu; veri karşılaştırılabilir kalsın diye korundu.
        if (FirebaseAuth.getInstance().currentUser?.uid == null) return
        AnalyticsLogger.logLessonItemFinish(partId, position, chestStars)
    }

    /**
     * Daha önce tamamen bitirilmiş (stepIsFinish=true) bir item kullanıcı tarafından tekrar
     * çözülüp yeniden bittiğinde çağrılır (chest'te tekrar oynama, lesson'da tekrar quiz çözme).
     *
     * Eski `avgReplayCount` alanı, Analytics'te bu olayın sayısının [recordItemFirstFinish]
     * olayının sayısına bölünmesiyle elde edilir.
     */
    fun recordItemReplay(partId: Int, position: Int) {
        if (partId !in 1..8) return
        if (FirebaseAuth.getInstance().currentUser?.uid == null) return
        AnalyticsLogger.logLessonItemReplay(partId, position)
    }
}
