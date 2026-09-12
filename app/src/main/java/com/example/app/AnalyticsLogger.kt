package com.example.app

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

/**
 * Firebase Analytics ve Crashlytics için tek giriş noktası.
 *
 * ## Neden merkezi bir sarmalayıcı
 * Event isimleri ve parametre adları çağrı yerlerine dağılırsa, aynı olayın iki farklı
 * isimle ("chest_opened" / "chest_open") kaydedilmesi kaçınılmaz olur ve raporlar sessizce
 * bölünür. Buradaki tipli fonksiyonlar bunu derleme zamanında engeller.
 *
 * ## KİŞİSEL VERİ YASAK
 * Uygulamayı çocuklar kullanıyor. Analytics'e **hiçbir koşulda** şunlar yazılmaz:
 * e-posta, ad-soyad, Firebase uid, doğum tarihi, kullanıcının yazdığı serbest metin.
 * Google'ın politikası bunu yasaklıyor; ihlali uygulamanın Play'den kaldırılmasına yol açar.
 * Bu yüzden bu dosyada `String` alan tek parametre [sanitize]'dan geçer ve serbest metin
 * kabul eden bir fonksiyon **bilerek** tanımlanmamıştır — kullanıcının yazdığı metin
 * Firestore'da kalır (bkz. [QuestionPanelFragment]).
 *
 * Aynı sebeple [FirebaseAnalytics.setUserId] hiç çağrılmaz: retention zaten cihaz bazlı
 * hesaplanır, kimliğe ihtiyacı yoktur.
 *
 * ## Hata davranışı
 * Ölçüm hiçbir zaman uygulamayı çökertmemeli. Tüm çağrılar [safe] ile sarılıdır;
 * Analytics bir sebeple kullanılamazsa (Play Services yok, init edilmemiş) olay
 * sessizce düşer ve kullanıcı akışı etkilenmez.
 */
object AnalyticsLogger {

    private const val TAG = "AnalyticsLogger"

    // ── Event isimleri ──────────────────────────────────────────────────────
    // Firebase kuralı: <=40 karakter, harfle başlar, harf/rakam/alt çizgi.
    private const val EV_LESSON_STEP_PASS = "lesson_step_pass"
    private const val EV_LESSON_STEP_FAIL = "lesson_step_fail"
    private const val EV_LESSON_ITEM_FINISH = "lesson_item_finish"
    private const val EV_LESSON_ITEM_REPLAY = "lesson_item_replay"
    private const val EV_LESSON_QUESTION_ENTRY = "lesson_question_entry"
    private const val EV_LESSON_ABANDON_NO_ANSWER = "lesson_abandon_no_answer"
    private const val EV_ACQUISITION_SOURCE = "acquisition_source"
    private const val EV_SURVEY_CHOICE = "lesson_survey_choice"
    private const val EV_SURVEY_TEXT = "lesson_survey_text"
    private const val EV_CHEST_OPEN_START = "chest_open_start"
    private const val EV_CHEST_OPEN_COMPLETE = "chest_open_complete"
    private const val EV_CHEST_ABANDONED = "chest_abandoned"
    /**
     * DİKKAT: `app_background` KULLANILAMAZ — Firebase'in ayrılmış olay adlarından biridir ve
     * SDK onu sessizce düşürür (hata yok, DebugView'da hiç görünmez). Aynı tuzak `app_update`,
     * `app_remove`, `app_exception`, `first_open`, `session_start`, `screen_view` için de geçerli.
     */
    private const val EV_APP_EXIT_SCREEN = "app_exit_screen"
    private const val EV_TUTORIAL_STEP_REACHED = "tutorial_step_reached"
    private const val EV_TUTORIAL_STEP_ANSWER = "tutorial_step_answer"

    // ── Parametre isimleri ──────────────────────────────────────────────────
    private const val P_PART_ID = "part_id"
    private const val P_POSITION = "position"
    private const val P_STEP = "step"
    private const val P_FAIL_STREAK = "fail_streak"
    private const val P_CHEST_STARS = "chest_stars"
    private const val P_ELAPSED_MS = "elapsed_ms"
    private const val P_ANSWER_SUCCESS_RATE = "answer_success_rate"
    private const val P_SOURCE = "source"
    private const val P_SURVEY_TYPE = "survey_type"
    /**
     * Dersin kalıcı kimliği ([com.example.app.model.LessonItem.stableId]).
     *
     * [P_POSITION] ile birlikte gönderilir ama **kalıcı olan budur**: konum, müfredata araya
     * ders eklendiğinde kayar ve aynı numara farklı tarihlerde farklı dersi gösterir. Kimlik
     * kaymaz; ayrıca raporda numara yerine okunur bir ad verir.
     */
    private const val P_LESSON_ID = "lesson_id"
    private const val P_QUESTION_NO = "question_no"
    private const val P_CHOICE = "choice"
    private const val P_RARITY_START = "rarity_start"
    private const val P_RARITY_FINAL = "rarity_final"
    private const val P_REWARD_TYPE = "reward_type"
    private const val P_REWARD_AMOUNT = "reward_amount"
    private const val P_DURATION_MS = "duration_ms"
    private const val P_SERVER_WAIT_MS = "server_wait_ms"
    private const val P_ANIMATION_MS = "animation_ms"
    private const val P_AT_TAP = "at_tap"
    private const val P_CHEST_SOURCE = "chest_source"
    private const val P_EXIT_SCREEN = "exit_screen"
    private const val P_TUTORIAL_NO = "tutorial_no"
    private const val P_STEP_KEY = "step_key"
    private const val P_STEP_TITLE = "step_title"
    private const val P_STEP_KIND = "step_kind"
    private const val P_QUESTION_TEXT = "question_text"
    private const val P_ATTEMPT_NO = "attempt_no"
    private const val P_ATTEMPT_BUCKET = "attempt_bucket"
    private const val P_IS_CORRECT = "is_correct"

    /** [logSurveyChoice] / [logSurveyText] için anket türü. */
    const val SURVEY_LESSON = "lesson"
    const val SURVEY_TUTORIAL = "tutorial"

    // ── Öğretici soru adımı türleri ─────────────────────────────────────────
    /** Kullanıcı abaküse sayıyı yazıp "Kontrol Et"e basıyor (`abacusClickable = true`). */
    const val STEP_KIND_ABACUS = "abacus"
    /** Kullanıcı çoktan seçmeli şıklardan seçiyor (`options` + `correctOptionIndex` dolu). */
    const val STEP_KIND_OPTIONS = "options"

    // ── Sandık kaynakları ───────────────────────────────────────────────────
    // Uygulamadaki BÜTÜN sandık akışları tek bir ekrandan geçer ([NewChestFragment]),
    // dolayısıyla hepsi aynı olaylarla ölçülür. Ayırt edilebilmeleri için her çağrı yeri
    // kendi kaynağını bildirir; aksi halde "ders sandığı mı görev sandığı mı daha çok
    // terk ediliyor" sorusu sorulamaz.
    /** Ders sonu sandığı ([ChestFragment]). */
    const val CHEST_SOURCE_LESSON = "lesson"
    /** Günlük soru ödülü ([TasksFragment]). */
    const val CHEST_SOURCE_DAILY_QUESTION = "daily_question"
    /** Görev tamamlama ödülü ([MissionsFragment]). */
    const val CHEST_SOURCE_MISSION = "mission"
    /** Görev sandığı ([MissionChestRewardFragment]). */
    const val CHEST_SOURCE_MISSION_CHEST = "mission_chest"
    /** Mağazada reklam izleyerek açılan sandık ([ShopFragment]). */
    const val CHEST_SOURCE_SHOP_AD = "shop_ad"
    /** Bülten kartından açılan sandık ([TasksFragment]); prefetch YAPILMAZ. */
    const val CHEST_SOURCE_BULLETIN = "bulletin"

    /** Firebase kullanıcı özelliği: değer en fazla 36 karakter olabilir. */
    private const val USER_PROPERTY_MAX = 36

    /** Firebase string parametresi: değer en fazla 100 karakter olabilir. */
    private const val PARAM_VALUE_MAX = 100

    private val analytics: FirebaseAnalytics? by lazy {
        try {
            Firebase.analytics
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseAnalytics alınamadı; ölçüm devre dışı.", e)
            null
        }
    }

    /**
     * Analytics çağrılarını yutan koruma. Ölçüm bir ürün özelliği değil; başarısız
     * olduğunda kullanıcı akışını durdurmamalı.
     */
    private inline fun safe(block: (FirebaseAnalytics) -> Unit) {
        val fa = analytics ?: return
        try {
            block(fa)
        } catch (e: Throwable) {
            Log.w(TAG, "Analytics çağrısı başarısız", e)
        }
    }

    /**
     * Serbest metnin yanlışlıkla Analytics'e sızmasına karşı son savunma hattı.
     * Uzunluğu sınırlar ve satır sonlarını temizler. Kişisel veri taşıyabilecek bir değeri
     * buradan geçirmek onu güvenli YAPMAZ — çağıran taraf zaten göndermemelidir.
     */
    private fun sanitize(value: String): String =
        value.replace(Regex("\\s+"), " ").trim().take(PARAM_VALUE_MAX)

    // ── Ekran görüntüleme ───────────────────────────────────────────────────

    /**
     * Ekran görüntüleme kaydı.
     *
     * Firebase'in otomatik `screen_view` toplaması **Activity** adını kullanır. Bu uygulamada
     * 8 Activity'ye karşılık 63 Fragment var ve neredeyse tüm gezinme MainActivity içindeki
     * fragment değişimleriyle oluyor; otomatik kayıt bu yüzden yalnızca "MainActivity" der ve
     * uygulama içi akış tamamen görünmez kalır. [NumiGooApplication] her fragment görünür olduğunda
     * burayı çağırarak bu boşluğu kapatır.
     */
    fun logScreenView(screenName: String) = safe { fa ->
        fa.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, sanitize(screenName))
            param(FirebaseAnalytics.Param.SCREEN_CLASS, sanitize(screenName))
        }
    }

    // ── Ders başarı istatistikleri ──────────────────────────────────────────
    // Eskiden `successRate/lessonSuccessRate/**` altında küresel sayaç dokümanlarına
    // yazılıyordu; bkz. [LessonSuccessRateRepository] başlığındaki taşıma notu.

    /**
     * Kullanıcı bir adımı ilk kez geçti.
     *
     * @param failStreak İlk başarıya kadar kaç kez başarısız olundu (0 = ilk denemede geçti).
     *   Eski `passOnFirstTry` / `passOnSecondTry` / `passOnThirdPlusTry` sayaçları bu tek
     *   parametreden türetilir.
     * @param elapsedMs Soru ekranına girişten bitirmeye kadar geçen süre; bilinmiyorsa null.
     *   **Ham değer gönderilir, kovalanmaz** — eski `finishTime0to30Count` gibi sabit dilimler
     *   koda gömülüydü ve dilimleri değiştirmek uygulama güncellemesi gerektiriyordu. Analytics'te
     *   dilimleri konsolda (veya BigQuery'de) sonradan istediğin gibi tanımlarsın.
     */
    fun logLessonStepPass(
        partId: Int,
        position: Int,
        lessonId: String?,
        step: Int,
        failStreak: Long,
        elapsedMs: Long?,
    ) = safe { fa ->
        fa.logEvent(EV_LESSON_STEP_PASS) {
            param(P_PART_ID, partId.toLong())
            param(P_POSITION, position.toLong())
            if (lessonId != null) param(P_LESSON_ID, sanitize(lessonId))
            param(P_STEP, step.toLong())
            param(P_FAIL_STREAK, failStreak)
            if (elapsedMs != null) param(P_ELAPSED_MS, elapsedMs)
        }
    }

    /**
     * Kullanıcı adımı geçemeden dersten çıktı.
     *
     * @param answerSuccessRatePercent O başarısız denemedeki başarı oranı (0-100); bilinmiyorsa null.
     *   [elapsedMs][logLessonStepPass] ile aynı gerekçeyle ham gönderilir: eski
     *   `failRate100to80Count` gibi sabit dilimler yerine değerin kendisi.
     */
    fun logLessonStepFail(
        partId: Int,
        position: Int,
        lessonId: String?,
        step: Int,
        failStreak: Long,
        answerSuccessRatePercent: Float?,
    ) = safe { fa ->
        fa.logEvent(EV_LESSON_STEP_FAIL) {
            param(P_PART_ID, partId.toLong())
            param(P_POSITION, position.toLong())
            if (lessonId != null) param(P_LESSON_ID, sanitize(lessonId))
            param(P_STEP, step.toLong())
            param(P_FAIL_STREAK, failStreak)
            if (answerSuccessRatePercent != null) {
                param(P_ANSWER_SUCCESS_RATE, answerSuccessRatePercent.toDouble())
            }
        }
    }

    /**
     * Kullanıcı bir adımın soru ekranına girdi (tekrarlar dahil, tekilleştirme yok).
     * Eski `questionEntryCount` sayacının karşılığı.
     */
    fun logLessonQuestionEntry(partId: Int, position: Int, lessonId: String?, step: Int) = safe { fa ->
        fa.logEvent(EV_LESSON_QUESTION_ENTRY) {
            param(P_PART_ID, partId.toLong())
            param(P_POSITION, position.toLong())
            if (lessonId != null) param(P_LESSON_ID, sanitize(lessonId))
            param(P_STEP, step.toLong())
        }
    }

    /**
     * Kullanıcı soru ekranını hiç cevap vermeden terk etti.
     * Eski `abandonWithoutAnswerCount` sayacının karşılığı; [logLessonQuestionEntry] ile
     * oranlanarak "girilen sorunun yüzde kaçı cevapsız bırakılıyor" elde edilir.
     */
    fun logLessonAbandonWithoutAnswer(partId: Int, position: Int, lessonId: String?, step: Int) = safe { fa ->
        fa.logEvent(EV_LESSON_ABANDON_NO_ANSWER) {
            param(P_PART_ID, partId.toLong())
            param(P_POSITION, position.toLong())
            if (lessonId != null) param(P_LESSON_ID, sanitize(lessonId))
            param(P_STEP, step.toLong())
        }
    }

    /**
     * Ders/sandık item'ı (tüm adımlarıyla) ilk kez tamamlandı.
     * @param chestStars Yalnızca TYPE_CHEST için 1–3; ders item'lerinde null.
     */
    fun logLessonItemFinish(partId: Int, position: Int, lessonId: String?, chestStars: Int?) = safe { fa ->
        fa.logEvent(EV_LESSON_ITEM_FINISH) {
            param(P_PART_ID, partId.toLong())
            param(P_POSITION, position.toLong())
            if (lessonId != null) param(P_LESSON_ID, sanitize(lessonId))
            if (chestStars != null) param(P_CHEST_STARS, chestStars.toLong())
        }
    }

    /** Daha önce bitirilmiş bir item tekrar çözüldü. */
    fun logLessonItemReplay(partId: Int, position: Int, lessonId: String?) = safe { fa ->
        fa.logEvent(EV_LESSON_ITEM_REPLAY) {
            param(P_PART_ID, partId.toLong())
            param(P_POSITION, position.toLong())
            if (lessonId != null) param(P_LESSON_ID, sanitize(lessonId))
        }
    }

    // ── Edinim kaynağı ──────────────────────────────────────────────────────

    /**
     * Kullanıcının "bizi nereden duydunuz" cevabı.
     *
     * Hem event hem kullanıcı özelliği olarak yazılır: event "bu ay kaç kişi Instagram
     * dedi" sorusunu, kullanıcı özelliği ise "Instagram'dan gelenlerin retention'ı nedir"
     * sorusunu cevaplar — ikincisi için tüm raporlar bu boyuta göre kırılabilir hale gelir.
     */
    fun logAcquisitionSource(source: String) = safe { fa ->
        val clean = sanitize(source)
        fa.logEvent(EV_ACQUISITION_SOURCE) {
            param(P_SOURCE, clean)
        }
        fa.setUserProperty("acquisition_source", clean.take(USER_PROPERTY_MAX))
    }

    // ── Ders sonu anketi ────────────────────────────────────────────────────
    // Eskiden `questionPanel` / `questionPanelTutorial` altındaki choice{N} sayaçlarıydı.
    // Kullanıcının yazdığı SERBEST METİN buraya gelmez, Firestore'da kalır.

    /**
     * Ankette bir şık seçildi.
     * @param surveyType [SURVEY_LESSON] veya [SURVEY_TUTORIAL].
     */
    fun logSurveyChoice(
        surveyType: String,
        partId: Int,
        lessonId: String,
        questionNo: Int,
        choice: Int,
    ) = safe { fa ->
        fa.logEvent(EV_SURVEY_CHOICE) {
            param(P_SURVEY_TYPE, sanitize(surveyType))
            param(P_PART_ID, partId.toLong())
            param(P_LESSON_ID, sanitize(lessonId))
            param(P_QUESTION_NO, questionNo.toLong())
            param(P_CHOICE, choice.toLong())
        }
    }

    /**
     * Ankete serbest metin yazıldı — **metnin kendisi gönderilmez**, yalnızca "yazıldı"
     * bilgisi. Metnin içeriği Firestore'da `questionPanel/.../text/{uid}` altındadır ve
     * yalnızca Admin SDK ile okunur.
     */
    fun logSurveyText(
        surveyType: String,
        partId: Int,
        lessonId: String,
        questionNo: Int,
    ) = safe { fa ->
        fa.logEvent(EV_SURVEY_TEXT) {
            param(P_SURVEY_TYPE, sanitize(surveyType))
            param(P_PART_ID, partId.toLong())
            param(P_LESSON_ID, sanitize(lessonId))
            param(P_QUESTION_NO, questionNo.toLong())
        }
    }

    // ── Sandık akışı ────────────────────────────────────────────────────────

    /** Sandık ekranı açıldı ve sunucu sonucu hazır. */
    fun logChestOpenStart(startRarity: String, finalRarity: String, chestSource: String) = safe { fa ->
        fa.logEvent(EV_CHEST_OPEN_START) {
            param(P_RARITY_START, sanitize(startRarity))
            param(P_RARITY_FINAL, sanitize(finalRarity))
            param(P_CHEST_SOURCE, sanitize(chestSource))
        }
    }

    /**
     * Kullanıcı sandığı açıp ödülü gördü.
     *
     * Toplam süre iki parçaya ayrılır, çünkü ikisinin çaresi farklıdır:
     * - [serverWaitMs] uzunsa sorun Cloud Functions soğuk başlangıcındadır. Çağrı yerlerinin
     *   çoğu ekranı açmadan önce [ServerRewards.prefetchChest] çağırıyor; bu parametre o
     *   optimizasyonun gerçekten işe yarayıp yaramadığını ölçer (yarıyorsa ~0 olmalı).
     * - [animationMs] uzunsa sorun animasyon tasarımındadır.
     *
     * [durationMs] ikisinin toplamıdır; tek başına "kullanıcı ne kadar bekledi" sorusunu
     * cevapladığı için ayrıca gönderilir.
     */
    fun logChestOpenComplete(
        finalRarity: String,
        rewardType: String,
        rewardAmount: Int,
        durationMs: Long,
        serverWaitMs: Long,
        animationMs: Long,
        chestSource: String,
    ) = safe { fa ->
        fa.logEvent(EV_CHEST_OPEN_COMPLETE) {
            param(P_RARITY_FINAL, sanitize(finalRarity))
            param(P_REWARD_TYPE, sanitize(rewardType))
            param(P_REWARD_AMOUNT, rewardAmount.toLong())
            param(P_DURATION_MS, durationMs)
            param(P_SERVER_WAIT_MS, serverWaitMs)
            param(P_ANIMATION_MS, animationMs)
            param(P_CHEST_SOURCE, sanitize(chestSource))
        }
    }

    /**
     * Sandık ekranı, ödül görülmeden kapandı.
     *
     * @param atTap Kaçıncı dokunuşta bırakıldı (0 = hiç dokunmadan).
     * @param serverWaitMs Sunucu cevabının gelmesi ne kadar sürdü; **null ise cevap hiç
     *   gelmeden çıkıldı**. Bu ikisinin ayrımı kritik: `at_tap = 0` + `server_wait_ms` yok,
     *   "kullanıcı beklerken sıkıldı" demektir ve çaresi animasyon değil, prefetch'tir.
     */
    fun logChestAbandoned(
        atTap: Int,
        durationMs: Long,
        serverWaitMs: Long?,
        chestSource: String,
    ) = safe { fa ->
        fa.logEvent(EV_CHEST_ABANDONED) {
            param(P_AT_TAP, atTap.toLong())
            param(P_DURATION_MS, durationMs)
            if (serverWaitMs != null) param(P_SERVER_WAIT_MS, serverWaitMs)
            param(P_CHEST_SOURCE, sanitize(chestSource))
        }
    }

    // ── Uygulamadan çıkış ───────────────────────────────────────────────────

    /**
     * Uygulama arka plana geçti — **hangi ekran açıkken** ve o ekranda ne kadar kalındıktan sonra.
     *
     * ## Bu olay olmadan da türetilebilirdi, neden yine de var
     * Bir oturumun son `screen_view`'ı zaten çıkış ekranıdır ve BigQuery'de sorgulanabilir.
     * Ama GA4 arayüzü bunu yapamıyor (uygulama raporlarında "exit rate" diye bir şey yok) ve
     * BigQuery verisi günlük toplu geliyor. Açık bir olay DebugView'da anında, standart
     * raporlarda doğrudan görünür.
     *
     * @param screenName Arka plana geçildiği anda görünen son fragment.
     * @param durationMs O ekranda geçirilen süre. 3 saniyede bırakılan bir ders ile 5 dakika
     *   sonra bırakılan ders aynı şey değil; ekran adı tek başına bunu ayırt etmiyor.
     */
    fun logAppExitScreen(screenName: String, durationMs: Long) = safe { fa ->
        fa.logEvent(EV_APP_EXIT_SCREEN) {
            param(P_EXIT_SCREEN, sanitize(screenName))
            param(P_DURATION_MS, durationMs)
        }
    }

    // ── Öğretici soru adımları ──────────────────────────────────────────────

    /**
     * Kullanıcı bir öğretici soru adımına **ilk kez** ulaştı.
     *
     * [logTutorialStepAnswer]'ın paydası budur: adıma ulaşıp hiç doğru cevaplayamadan bırakan
     * kullanıcılar yalnızca bu iki olayın kullanıcı sayısı karşılaştırılarak görülebilir.
     * Yalnız cevap olayına bakmak, en çok zorlanan adımı sistematik olarak gizler — çünkü
     * orada takılıp uygulamayı kapatan çocuk hiçbir "doğru" olayı üretmez.
     *
     * "İlk kez" kararını [TutorialStepAnalytics.markReachedOnce] verir; geri tuşu ve dersi
     * tekrar oynama bu sayıyı şişirmez.
     */
    fun logTutorialStepReached(
        tutorialNumber: Int,
        stepKey: String,
        stepTitle: String,
        stepKind: String,
        lessonId: String?,
    ) = safe { fa ->
        fa.logEvent(EV_TUTORIAL_STEP_REACHED) {
            param(P_TUTORIAL_NO, tutorialNumber.toLong())
            param(P_STEP_KEY, sanitize(stepKey))
            param(P_STEP_TITLE, sanitize(stepTitle))
            param(P_STEP_KIND, sanitize(stepKind))
            if (!lessonId.isNullOrBlank()) param(P_LESSON_ID, sanitize(lessonId))
        }
    }

    /**
     * Öğretici soru adımında bir cevap gönderildi.
     *
     * Adım başına kullanıcı başına yalnızca **ilk geçişin** denemeleri gönderilir: adım bir kez
     * doğru cevaplandıktan sonra aynı adıma geri dönülüp verilen cevaplar hiç ölçülmez
     * (bkz. [TutorialStepAnalytics.recordAttempt]).
     *
     * ## Neden her deneme ayrı olay, neden tek bir "geçti" olayı değil
     * Tek bir "geçti (n denemede)" olayı, adımı hiç geçemeyenleri tamamen görünmez bırakırdı.
     * Her deneme ayrı gönderilince aynı veriden üç soru birden yanıtlanabiliyor:
     * `is_correct = true` süzgeciyle "kaçıncı denemede geçti" dağılımı, süzgeçsiz haliyle
     * "bu adımda toplam kaç yanlış verildi", [logTutorialStepReached] ile karşılaştırıldığında
     * da "kaç kişi hiç geçemeden bıraktı".
     *
     * @param attemptNo Kaçıncı deneme (1'den başlar); [TutorialStepAnalytics.recordAttempt] verir.
     * @param isCorrect Metin olarak gönderilir; GA4'te boole parametreler boyut olarak
     *   süzülemiyor, `"true"` / `"false"` ise doğrudan süzgeç değeri oluyor.
     */
    fun logTutorialStepAnswer(
        tutorialNumber: Int,
        stepKey: String,
        stepTitle: String,
        stepKind: String,
        questionText: String?,
        lessonId: String?,
        attemptNo: Int,
        isCorrect: Boolean,
    ) = safe { fa ->
        fa.logEvent(EV_TUTORIAL_STEP_ANSWER) {
            param(P_TUTORIAL_NO, tutorialNumber.toLong())
            param(P_STEP_KEY, sanitize(stepKey))
            param(P_STEP_TITLE, sanitize(stepTitle))
            param(P_STEP_KIND, sanitize(stepKind))
            param(P_ATTEMPT_NO, attemptNo.toLong())
            param(P_ATTEMPT_BUCKET, TutorialStepAnalytics.bucketOf(attemptNo))
            param(P_IS_CORRECT, if (isCorrect) "true" else "false")
            if (!questionText.isNullOrBlank()) param(P_QUESTION_TEXT, sanitize(questionText))
            if (!lessonId.isNullOrBlank()) param(P_LESSON_ID, sanitize(lessonId))
        }
    }

    // ── Crashlytics ─────────────────────────────────────────────────────────

    /**
     * Yakalanmış ama beklenmeyen bir hatayı Crashlytics'e "ölümcül olmayan" olarak bildirir.
     *
     * Kodda `addOnFailureListener { Log.w(...) }` ile yutulan hatalar üretimde tamamen
     * görünmez; bu fonksiyon onları görünür kılmak içindir.
     */
    fun recordNonFatal(where: String, error: Throwable) {
        try {
            Firebase.crashlytics.log(where)
            Firebase.crashlytics.recordException(error)
        } catch (e: Throwable) {
            Log.w(TAG, "Crashlytics'e yazılamadı", e)
        }
    }
}
