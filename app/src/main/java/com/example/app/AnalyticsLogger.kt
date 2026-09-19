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
    private const val EV_ENERGY_BLOCKED = "energy_blocked"
    private const val EV_ENERGY_SPENT = "energy_spent"
    private const val EV_ENERGY_REFILL = "energy_refill"
    private const val EV_AD_SKIP_SHOWN = "ad_skip_shown"
    private const val EV_AD_SKIP_CLOSED = "ad_skip_closed"
    private const val EV_ASK_QUESTION_PROMO_SHOWN = "ask_question_promo_shown"
    private const val EV_ASK_QUESTION_PROMO_CLOSED = "ask_question_promo_closed"
    private const val EV_PRO_PANEL_SHOWN = "pro_panel_shown"
    private const val EV_PLAN_SHOWN = "plan_shown"
    private const val EV_SIGNUP_STEP = "signup_step"
    private const val EV_QUESTION_ASKED = "question_asked"
    private const val EV_QUESTION_ANSWERED = "question_answered"
    private const val EV_QUESTION_CHAT_OPENED = "question_chat_opened"
    private const val EV_DAILY_QUESTION_START = "daily_question_start"
    private const val EV_DAILY_QUESTION_RESULT = "daily_question_result"
    private const val EV_DAILY_QUESTION_CLAIM = "daily_question_claim"
    /**
     * DİKKAT: `purchase` GA4'ün STANDART olayıdır, ayrılmış adlardan biri değil. `value` ve
     * `currency` ile birlikte gönderildiğinde Para Kazanma raporlarını kendiliğinden doldurur;
     * bu yüzden özel bir ad uydurmuyoruz.
     */
    private const val EV_PURCHASE_STARTED = "purchase_started"

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
    private const val P_WRONG_ANSWER = "wrong_answer"
    private const val P_BLOCK_SOURCE = "block_source"
    private const val P_SPEND_SOURCE = "spend_source"
    private const val P_REFILL_SOURCE = "refill_source"
    private const val P_WAIT_SECONDS = "wait_seconds"
    private const val P_ENERGY_LEFT = "energy_left"
    private const val P_ENERGY_AFTER = "energy_after"
    private const val P_LESSONS_THIS_SESSION = "lessons_this_session"
    private const val P_VIEW_NO = "view_no"
    private const val P_DWELL_BUCKET = "dwell_bucket"
    private const val P_DWELL_MS = "dwell_ms"
    private const val P_OUTCOME = "outcome"
    private const val P_PRODUCT_ID = "product_id"
    private const val P_TRIGGER = "trigger"
    private const val P_PRO_ENTRY_POINT = "pro_entry_point"
    private const val P_WELCOME_CREDIT = "welcome_credit"
    private const val P_SIGNUP_STAGE = "signup_stage"
    private const val P_SIGNUP_ROLE = "signup_role"
    private const val P_MEDIA_TYPE = "media_type"
    private const val P_WAIT_HOURS = "wait_hours"
    private const val P_QUESTION_STATUS = "question_status"

    /** [logSurveyChoice] / [logSurveyText] için anket türü. */
    const val SURVEY_LESSON = "lesson"
    const val SURVEY_TUTORIAL = "tutorial"

    // ── Öğretici soru adımı türleri ─────────────────────────────────────────
    /** Kullanıcı abaküse sayıyı yazıp "Kontrol Et"e basıyor (`abacusClickable = true`). */
    const val STEP_KIND_ABACUS = "abacus"
    /** Kullanıcı çoktan seçmeli şıklardan seçiyor (`options` + `correctOptionIndex` dolu). */
    const val STEP_KIND_OPTIONS = "options"

    // ── Enerji (can) kaynakları ─────────────────────────────────────────────
    /** Haritadan ders başlatılırken can yetmedi ([LessonAdapter]). */
    const val ENERGY_BLOCK_LESSON = "lesson"
    /** Kupa yarışı başlatılırken can yetmedi ([TasksFragment]). */
    const val ENERGY_BLOCK_CUP = "cup"

    /** Ders başlatıldı, 1 can gitti. */
    const val ENERGY_SPEND_LESSON = "lesson"
    /** Kupa modunda yanlış cevap verildi. */
    const val ENERGY_SPEND_CUP_FAIL = "cup_fail"
    /** Kupa yarışı yarıda bırakıldı (çıkış veya geri tuşu). */
    const val ENERGY_SPEND_CUP_QUIT = "cup_quit"

    /** Mağazada reklam izlenerek can alındı. */
    const val ENERGY_REFILL_AD = "ad"
    /** Mağazada anahtar harcanarak can alındı. */
    const val ENERGY_REFILL_KEYS = "keys"

    // ── Reklam sonrası Pro paneli ([AdSkipFragment]) çıkış yolları ──────────
    /** "Ücretsiz dene" — huni [ProDiffirentFragment] ile sürüyor. */
    const val AD_SKIP_TRY_FREE = "try_free"
    /** "Hayır teşekkürler" düğmesi. */
    const val AD_SKIP_NO_THANKS = "no_thanks"
    /** Geri tuşu veya panel dışına dokunma. */
    const val AD_SKIP_DISMISSED = "dismissed"

    // ── Öğretmene sorma tanıtımı ([AskQuestionOpenFragment]) ────────────────
    // Ekran iki bambaşka durumda çıkıyor ve "hayır teşekkürler" ikisinde zıt şey demek:
    // otomatik tanıtımda reddetmek sıradan, kredisi bittiği için gelen kullanıcıda ise
    // istediği şeyi önüne koyduğun hâlde almaması. Birleştirilirse oran anlamsızlaşır.
    /** Ders dönüşü sayacı eşiğe ulaştı, ekran kendiliğinden açıldı. */
    const val PROMO_TRIGGER_AUTO = "auto_promo"
    /** Free kullanıcı "öğretmene sor"a bastı ama kredisi yoktu. */
    const val PROMO_TRIGGER_OUT_OF_CREDITS = "out_of_credits"
    /**
     * PRO üyesi "öğretmene sor"a bastı ama kredisi yoktu.
     *
     * Ayrı bir değer çünkü bu kullanıcıya satılacak şey abonelik değil kredi; aynı kovaya
     * konsaydı "tanıtımı görenlerin kaçı Pro'ya geçti" oranı, zaten Pro olanlarla sulanırdı.
     * Bu durum şimdiye kadar düz bir AlertDialog'du ve hiçbir yere kaydedilmiyordu.
     */
    const val PROMO_TRIGGER_PRO_OUT_OF_CREDITS = "pro_out_of_credits"

    /** "1 hafta ücretsiz dene" — huni [ProDiffirentFragment] ile sürüyor. */
    const val PROMO_TRY_FREE = "try_free"
    /** "Bunun yerine kredi al" — abonelik değil, tek seferlik kredi isteniyor. */
    const val PROMO_BUY_CREDITS = "buy_credits"
    /** "Hayır teşekkürler" düğmesi. */
    const val PROMO_NO_THANKS = "no_thanks"
    /** Geri tuşu veya panel dışına dokunma. */
    const val PROMO_DISMISSED = "dismissed"

    // ── Kayıt hunisinin adımları ───────────────────────────────────────────
    // Sıra: start -> age -> source -> email/teacher_form -> otp_sent -> completed.
    // otp_wrong huninin bir adımı DEĞİL, sürtünme sinyali: aynı kullanıcıda hem otp_wrong
    // hem completed olabilir.
    /** Giriş/kayıt seçim ekranı açıldı — huninin paydası. */
    const val SIGNUP_START = "start"
    /** Yaş soruldu. */
    const val SIGNUP_AGE = "age"
    /** "Bizi nereden duydun" soruldu. */
    const val SIGNUP_SOURCE = "source"
    /** Öğrenci e-posta formu göründü. */
    const val SIGNUP_EMAIL = "email"
    /** Öğretmen kayıt formu göründü. */
    const val SIGNUP_TEACHER_FORM = "teacher_form"
    /**
     * Doğrulama kodu gönderildi.
     *
     * Yalnızca YENİ kayıtta gönderilir. Zaten kayıtlı bir e-posta yazılırsa aynı ekran koda
     * geçer ama o bir GİRİŞ'tir; huniye katılsaydı payda şişer ve o kullanıcılar "completed"
     * olmadığı için sahte bir terk oranı üretirdi.
     */
    const val SIGNUP_OTP_SENT = "otp_sent"
    /** Yanlış kod girildi. Adım değil, sürtünme ölçüsü. */
    const val SIGNUP_OTP_WRONG = "otp_wrong"
    /** Hesap oluştu. */
    const val SIGNUP_COMPLETED = "completed"

    const val SIGNUP_ROLE_STUDENT = "student"
    const val SIGNUP_ROLE_TEACHER = "teacher"

    // ── Öğretmene soru akışı ───────────────────────────────────────────────
    const val QUESTION_MEDIA_IMAGE = "image"
    const val QUESTION_MEDIA_VIDEO = "video"

    // ── Pro akışının giriş kapıları ────────────────────────────────────────
    /** Reklam sonrası panel ([AdSkipFragment]). */
    const val PRO_ENTRY_AD_SKIP = "ad_skip"
    /** Öğretmene sorma tanıtımı ([AskQuestionOpenFragment]). */
    const val PRO_ENTRY_ASK_QUESTION = "ask_question"
    /** Mağaza ([ShopFragment]). */
    const val PRO_ENTRY_SHOP = "shop"
    /** Kapı belirlenemedi (süreç akışın ortasında yeniden başlamış olabilir). */
    const val PRO_ENTRY_UNKNOWN = "unknown"

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

    /**
     * Kullanıcı hayatında en az bir kez enerji duvarına çarptı. Bir kez "true" yazılır ve
     * bir daha değişmez; Elde Tutma raporunda kırılım boyutu olarak kullanılır.
     */
    private const val USER_PROPERTY_ENERGY_WALL_HIT = "energy_wall_hit"

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
     * @param wrongAnswer Kullanıcının verdiği cevabın kendisi. **Yalnızca yanlış cevapta**
     *   gönderilir: doğru cevapta bu değer zaten beklenen cevaba eşittir ve adımın kendisinden
     *   bilindiği için boyutu gereksiz yere şişirir. Yanlış cevap ise soruyu anlatır — abaküs
     *   adımında 4 yerine 40 yazılması çocuğun boncuğu yanlış çubuğa koyduğunu, şık adımında
     *   hangi çeldiricinin seçildiği ise hangi kavramın karıştığını söyler.
     *
     *   İki adım türü de aynı parametreyi kullanır; hangisi olduğu `step_kind`'dan okunur. Ayrı
     *   parametre açmak GA4'ün keşif başına 20 boyut bütçesinden ikinci bir slot yerdi ve
     *   "verilen yanlış cevap" tablosunu iki ayrı tabloya bölerdi.
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
        wrongAnswer: String?,
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
            if (!isCorrect && !wrongAnswer.isNullOrBlank()) param(P_WRONG_ANSWER, sanitize(wrongAnswer))
        }
    }

    // ── Enerji (can) sistemi ────────────────────────────────────────────────

    /**
     * Kullanıcı bir şey başlatmak istedi ama canı yetmedi — **duvara çarptı**.
     *
     * Enerji sisteminin kullanıcı kaybettirip kaybettirmediği sorusunun merkezinde bu olay
     * var. Bugüne kadar bu an hiçbir yere kaydedilmiyordu: [LessonAdapter.showEnergyWarning]
     * sessizce mağazayı açıyor ve çocuk ya reklam izliyor, ya anahtar harcıyor, ya da
     * çıkıp gidiyor. Üçüncüsü, yani asıl önemli olan, tamamen görünmezdi.
     *
     * Ayrıca `energy_wall_hit` kullanıcı özelliğini yazar. Bu, Elde Tutma raporunda
     * "duvara çarpanlar" ile "çarpmayanların" geri dönüş eğrilerini yan yana koymayı
     * mümkün kılıyor — enerji sisteminin gerçek maliyeti oradan okunur.
     *
     * @param waitSeconds Sıradaki canın gelmesine kaç saniye kaldığı. Ekran adı tek başına
     *   yetmiyor: 7 yaşındaki bir çocuk için 10 dakika beklemek ile 45 dakika beklemek
     *   aynı şey değil, ikincisinde o gün geri gelmiyor.
     * @param lessonsThisSession Duvara çarpmadan önce o oturumda kaç ders başlatmıştı
     *   (kova: "0", "1-2", "3-4", "5+"). Bkz. [EnergySessionCounter].
     */
    fun logEnergyBlocked(
        blockSource: String,
        waitSeconds: Long,
        lessonsThisSession: String,
        partId: Int?,
        lessonId: String?,
    ) = safe { fa ->
        fa.logEvent(EV_ENERGY_BLOCKED) {
            param(P_BLOCK_SOURCE, sanitize(blockSource))
            param(P_WAIT_SECONDS, waitSeconds.coerceAtLeast(0L))
            param(P_LESSONS_THIS_SESSION, sanitize(lessonsThisSession))
            if (partId != null) param(P_PART_ID, partId.toLong())
            if (!lessonId.isNullOrBlank()) param(P_LESSON_ID, sanitize(lessonId))
        }
        fa.setUserProperty(USER_PROPERTY_ENERGY_WALL_HIT, "true")
    }

    /**
     * Can harcandı.
     *
     * [logEnergyBlocked]'ın paydası: kaç harcamaya karşılık kaç blok düştüğü, duvarın ne
     * sıklıkta bağladığını söyler. Hiç blok yoksa enerji sistemi hiçbir şey yapmıyor
     * demektir — ne gelir getiriyor ne tempo kuruyor, sadece kod ve arayüz karmaşıklığı.
     *
     * @param spendSource [ENERGY_SPEND_LESSON], [ENERGY_SPEND_CUP_FAIL] veya
     *   [ENERGY_SPEND_CUP_QUIT]. Ayrımı önemli: canların çoğu `cup_fail`'den gidiyorsa
     *   sistem canı en kötü yerden alıyor — çocuk zaten yarışı kaybetmiş, üstüne bir de
     *   cezalandırılıyor.
     * @param energyLeft Harcamadan SONRA kalan can.
     */
    fun logEnergySpent(
        spendSource: String,
        energyLeft: Int,
        lessonsThisSession: String,
        partId: Int?,
        lessonId: String?,
    ) = safe { fa ->
        fa.logEvent(EV_ENERGY_SPENT) {
            param(P_SPEND_SOURCE, sanitize(spendSource))
            param(P_ENERGY_LEFT, energyLeft.toLong())
            param(P_LESSONS_THIS_SESSION, sanitize(lessonsThisSession))
            if (partId != null) param(P_PART_ID, partId.toLong())
            if (!lessonId.isNullOrBlank()) param(P_LESSON_ID, sanitize(lessonId))
        }
    }

    /**
     * Can kazanıldı (reklam veya anahtar).
     *
     * [logEnergyBlocked] ile birlikte huni kurar: duvara çarpanların yüzde kaçı can almaya
     * gitti? Bu oran duvarın gelir verimidir. Çok düşükse duvar para kazandırmıyor, sadece
     * kullanıcı kaçırıyor demektir.
     */
    fun logEnergyRefill(refillSource: String, energyAfter: Int) = safe { fa ->
        fa.logEvent(EV_ENERGY_REFILL) {
            param(P_REFILL_SOURCE, sanitize(refillSource))
            param(P_ENERGY_AFTER, energyAfter.toLong())
        }
    }

    // ── Reklam sonrası Pro paneli ──────────────────────────────────────────

    /**
     * Reklam kapandıktan sonra çıkan Pro paneli ([AdSkipFragment]) gösterildi.
     *
     * [logAdSkipClosed]'ın paydası. İkisinin kullanıcı sayısı arasındaki fark, paneli görüp
     * kapatmadan uygulamadan çıkanlardır — panel açıkken arka plana geçen kullanıcıda kapanış
     * olayı hiç tetiklenmez.
     *
     * @param viewNo Kullanıcının bu paneli kaçıncı görüşü, kova hâlinde ("01"…"09", "10+").
     *   Kalıcı sayaçtan gelir; bkz. [AdSkipStats].
     */
    fun logAdSkipShown(viewNo: String) = safe { fa ->
        fa.logEvent(EV_AD_SKIP_SHOWN) {
            param(P_VIEW_NO, sanitize(viewNo))
        }
    }

    /**
     * Pro paneli kullanıcı tarafından kapatıldı.
     *
     * ## Neden tek olay, neden ayrı "süre" ve "tıklama" olayları değil
     * Ekranda kalma süresi ile çıkış yolu aynı anın iki yüzü: "3 saniye bakıp Ücretsiz Dene'ye
     * bastı" ile "3 saniye bakıp kapattı" bambaşka şeyler. İki ayrı olay gönderilseydi GA4'te
     * bunları aynı görüşe ait diye birleştirmenin yolu olmazdı.
     *
     * ## Neden onDismiss değil de düğmeler + onCancel
     * `onDismiss` yapılandırma değişikliğinde ve uygulama öldürülürken de tetikleniyor; o
     * durumlarda sahte bir "kapattı" kaydı düşerdi. `onCancel` ise yalnızca geri tuşu ve panel
     * dışına dokunmada çalışır, yani gerçek kullanıcı hareketidir.
     *
     * @param dwellMs Panelin açık kaldığı ham süre; ortalama alınabilsin diye kovanın yanında
     *   ayrıca gönderilir (GA4 metin boyutunu ortalayamıyor).
     * @param outcome [AD_SKIP_TRY_FREE], [AD_SKIP_NO_THANKS] veya [AD_SKIP_DISMISSED].
     */
    fun logAdSkipClosed(
        viewNo: String,
        dwellBucket: String,
        dwellMs: Long,
        outcome: String,
    ) = safe { fa ->
        fa.logEvent(EV_AD_SKIP_CLOSED) {
            param(P_VIEW_NO, sanitize(viewNo))
            param(P_DWELL_BUCKET, sanitize(dwellBucket))
            param(P_DWELL_MS, dwellMs.coerceAtLeast(0L))
            param(P_OUTCOME, sanitize(outcome))
        }
    }

    // ── Satın alma ──────────────────────────────────────────────────────────

    /**
     * Günün sorularından biri başlatıldı.
     *
     * ## Neden gerekli
     * 24 saatte 3 soru, uygulamanın günlük geri dönüş mekanizması. Sunucuda bütün akış
     * (`recordQuestionResult`, `incrementSolvedCount`, `markRewardClaimed`) çalışıyordu ama tek
     * olay gönderilmiyordu: kaç çocuğun başladığını, kaçının üçünü de bitirdiğini, kaçının
     * ödülü aldığını bilmiyorduk. Yani özelliğin işe yarayıp yaramadığına dair hiçbir kanıt yoktu.
     *
     * @param questionNo Günün kaçıncı sorusu (1-3). `question_no` parametresi anket
     *   olaylarıyla PAYLAŞILIYOR — ikisi de "sıradaki kaçıncı soru" demek; raporda olay adıyla
     *   filtrelemek şart.
     */
    fun logDailyQuestionStart(questionNo: Int) = safe { fa ->
        fa.logEvent(EV_DAILY_QUESTION_START) {
            param(P_QUESTION_NO, questionNo.toLong())
        }
    }

    /**
     * Günün sorusu sonuçlandı.
     *
     * Başlangıç ile sonuç arasındaki fark, soruyu açıp bitirmeden bırakanları verir.
     * [isCorrect] false ise çocuk elmas harcayarak devam etme paneliyle karşılaşıyor.
     */
    fun logDailyQuestionResult(questionNo: Int, isCorrect: Boolean) = safe { fa ->
        fa.logEvent(EV_DAILY_QUESTION_RESULT) {
            param(P_QUESTION_NO, questionNo.toLong())
            param(P_IS_CORRECT, if (isCorrect) "true" else "false")
        }
    }

    /** Üç soru da bitti ve günün ödülü alındı — huninin son adımı. */
    fun logDailyQuestionClaim() = safe { fa ->
        fa.logEvent(EV_DAILY_QUESTION_CLAIM) {}
    }

    /**
     * Çocuk öğretmene soru gönderdi.
     *
     * ## Neden gerekli
     * Satın almayı ölçüyoruz ama o kredinin KULLANILDIĞINI ölçmüyorduk. Kredi alıp hiç
     * sormayan bir çocuk, parasını verip ürünü raftan indirmemiş demektir; bu sessiz kayıp
     * `purchase` sayısına bakarak fark edilmiyor.
     *
     * @param mediaType [QUESTION_MEDIA_IMAGE] veya [QUESTION_MEDIA_VIDEO].
     */
    fun logQuestionAsked(mediaType: String) = safe { fa ->
        fa.logEvent(EV_QUESTION_ASKED) {
            param(P_MEDIA_TYPE, sanitize(mediaType))
        }
    }

    /**
     * Öğretmen bir soruya İLK cevabını gönderdi.
     *
     * ## Neden öğretmen tarafından ölçülüyor
     * Gerçek cevap süresini yalnızca bu an biliyor: sorunun `createdAt`'i elde ve cevap şimdi
     * gidiyor. Çocuk tarafından ölçülseydi "öğretmen ne zaman cevapladı" değil "çocuk ne zaman
     * açtı" ölçülürdü — bambaşka bir soru.
     *
     * ## Neden önemli
     * Sorular 48 saat cevapsız kalırsa sunucu krediyi iade edip soruyu `expired` yapıyor
     * (bkz. `QUESTION_REFUND_AFTER_MS`). Kredi geri gelse de çocuk iki gün bekleyip eli boş
     * kalmış oluyor. Bekleme süresi uzadıkça kredi değersizleşir ve Pro yenilenmez;
     * bu olay olmadan durum ancak iptaller başlayınca fark edilir.
     *
     * @param waitHours [questionWaitBucket] ile üretilmiş kova.
     */
    fun logQuestionAnswered(waitHours: String) = safe { fa ->
        fa.logEvent(EV_QUESTION_ANSWERED) {
            param(P_WAIT_HOURS, sanitize(waitHours))
        }
    }

    /**
     * Çocuk bir soru sohbetini açtı.
     *
     * @param questionStatus Sunucudaki durum: `pending`, `claimed`, `resolved`, `expired`.
     *   `expired` satırındaki her kullanıcı, iki gün bekleyip cevap alamamış bir çocuktur —
     *   bu tablodaki en önemli satır odur.
     */
    fun logQuestionChatOpened(questionStatus: String) = safe { fa ->
        fa.logEvent(EV_QUESTION_CHAT_OPENED) {
            param(P_QUESTION_STATUS, sanitize(questionStatus))
        }
    }

    /**
     * Soru sorulduktan sonra geçen sürenin GA4 kovası.
     *
     * Başa sıfır konuyor: GA4 metin boyutlarını alfabetik sıralıyor, `"1-3"` ile `"12-24"`
     * yan yana yanlış diziliyor. `48+` sınırı sunucunun iade eşiğiyle aynı — o kovaya düşen
     * cevap, kredisi çoktan iade edilmiş bir soruya gelmiş demektir.
     */
    fun questionWaitBucket(waitMs: Long): String {
        val hours = waitMs / 3_600_000.0
        return when {
            waitMs < 0L -> "00-01"
            hours < 1 -> "00-01"
            hours < 3 -> "01-03"
            hours < 12 -> "03-12"
            hours < 24 -> "12-24"
            hours < 48 -> "24-48"
            else -> "48+"
        }
    }

    /**
     * Kayıt hunisinin bir adımına ulaşıldı.
     *
     * ## Neden gerekli
     * Ölçtüğümüz her şey — dersler, enerji, Pro hunisi — uygulamaya çoktan girmiş kullanıcıyı
     * anlatıyor. Kurulumların ne kadarının kapıdan geçemediği hiç görünmüyordu; o oran
     * bilinmeden diğer bütün metriklerin paydası sessizce bozuk kalıyor.
     *
     * ## Neden tek olay, adım parametresiyle
     * Her adım ayrı bir olay olsaydı GA4'te huni kurmak için altı ayrı olay adı gerekirdi ve
     * araya yeni bir adım eklemek her raporu elden geçirmeyi gerektirirdi. Tek olay + [stage]
     * ile huni adımları parametre koşuluyla seçiliyor.
     *
     * Adımlar kullanıcı bazında sayılır; ekran döndürme veya geri dönüp tekrar ilerleme aynı
     * kullanıcıyı ikinci kez huniye sokmaz.
     *
     * @param stage `SIGNUP_*` sabitlerinden biri.
     * @param role [SIGNUP_ROLE_STUDENT] veya [SIGNUP_ROLE_TEACHER]. Öğretmen kaydı ayrı bir
     *   akış ve çok daha az sayıda; ayrılmazsa öğrenci hunisini kirletir.
     */
    fun logSignupStep(stage: String, role: String) = safe { fa ->
        fa.logEvent(EV_SIGNUP_STEP) {
            param(P_SIGNUP_STAGE, sanitize(stage))
            param(P_SIGNUP_ROLE, sanitize(role))
        }
    }

    /**
     * Öğretmene sorma tanıtımı ([AskQuestionOpenFragment]) ekrana geldi.
     *
     * @param trigger [PROMO_TRIGGER_AUTO] veya [PROMO_TRIGGER_OUT_OF_CREDITS].
     * @param viewNo Bu tetikleyici için kaçıncı görüş; bkz. [AskQuestionPromoStats.viewBucket].
     * @param welcomeCreditAvailable Cihaz hediye krediyi hâlâ alabiliyor mu. Ekranın hangi
     *   düzenle açıldığını belirliyor (alabiliyorsa büyük düğme Pro, alamıyorsa kredi satın
     *   alma). Bu ayrım kaydedilmezse `buy_credits` payındaki değişimin kullanıcı tercihinden
     *   mi düzenden mi geldiği sonradan ayırt edilemez.
     */
    fun logAskQuestionPromoShown(
        trigger: String,
        viewNo: String,
        welcomeCreditAvailable: Boolean,
    ) = safe { fa ->
        fa.logEvent(EV_ASK_QUESTION_PROMO_SHOWN) {
            param(P_TRIGGER, sanitize(trigger))
            param(P_VIEW_NO, sanitize(viewNo))
            param(P_WELCOME_CREDIT, if (welcomeCreditAvailable) "true" else "false")
        }
    }

    /**
     * Tanıtım kapandı — hangi yoldan kapandığıyla birlikte.
     *
     * ## Neden üç çıkış yolu ayrı ayrı
     * [PROMO_BUY_CREDITS] ayrı bir ürün sinyali: "şeyi istiyorum, aboneliği değil". Ağırlık
     * oraya kayıyorsa ekranın "1 hafta ücretsiz dene" çerçevesi o an için yanlış demektir.
     * Tek bir "kapattı" kaydı bu farkı yok ederdi.
     *
     * ## Neden onDismiss değil
     * [AdSkipFragment] ile aynı gerekçe: `onDismiss` yapılandırma değişikliğinde ve uygulama
     * öldürülürken de çalışıyor, sahte "kapattı" kaydı düşerdi. Düğmeler kendi sonuçlarını
     * bildiriyor, `onCancel` ise yalnızca geri tuşu ve panel dışına dokunmada çalışıyor.
     *
     * @param dwellMs Ekranda kalınan ham süre. Bu ekran için kova üretilmiyor: [AdSkipFragment]
     *   bir kesintiydi ve orada süre "okudu mu, refleksle mi kapattı" sorusunu cevaplıyordu;
     *   burada [PROMO_TRIGGER_OUT_OF_CREDITS] durumunda kullanıcı ekrana bilerek geliyor, süre
     *   ilgiyi değil okuma hızını ölçer. Ham değer yine de gidiyor ki ortalaması alınabilsin.
     */
    fun logAskQuestionPromoClosed(
        trigger: String,
        viewNo: String,
        outcome: String,
        dwellMs: Long,
        welcomeCreditAvailable: Boolean,
    ) = safe { fa ->
        fa.logEvent(EV_ASK_QUESTION_PROMO_CLOSED) {
            param(P_TRIGGER, sanitize(trigger))
            param(P_VIEW_NO, sanitize(viewNo))
            param(P_OUTCOME, sanitize(outcome))
            param(P_DWELL_MS, dwellMs.coerceAtLeast(0L))
            param(P_WELCOME_CREDIT, if (welcomeCreditAvailable) "true" else "false")
        }
    }

    /**
     * Pro paneli ([ProDiffirentFragment]) açıldı — hunideki "kapı" adımı.
     *
     * ## Neden `screen_view` yetmiyor
     * Panel dört ayrı yerden açılıyor (reklam sonrası, öğretmene sorma tanıtımı, mağazadaki iki
     * düğme). Huni adımları ekran adıyla eşlendiğinden bu dördü tek havuzda toplanıyor ve
     * "reklam sonrası panel işe yarıyor mu" sorusu ölçülüyor gibi görünüp ölçülmüyordu.
     * Bu olay kapıyı taşıyor; aynı kapı [ProFlow] üzerinden satın alma olaylarına da gidiyor,
     * böylece gösterim → ödeme ekranı → satın alma zinciri tek tabloda kapıya göre bölünebiliyor.
     */
    fun logProPanelShown(entryPoint: String) = safe { fa ->
        fa.logEvent(EV_PRO_PANEL_SHOWN) {
            param(P_PRO_ENTRY_POINT, sanitize(entryPoint))
        }
    }

    /**
     * Plan seçme ekranı ([PlanFragment]) açıldı — hunideki panel ile ödeme arasındaki adım.
     *
     * ## Neden ayrı bir olay, `screen_view` dururken
     * Huninin kapıya göre bölünebilmesi için HER adımın `pro_entry_point` taşıması gerekiyor:
     * GA4 huni ayrıştırmasında bir adımı tamamlayan olay o boyutu taşımıyorsa o adım
     * `(not set)` görünür ve ayrıştırma zincirin ortasında kopar. `screen_view` bu parametreyi
     * taşıyamaz, çünkü merkezden ([NumiGooApplication]) gönderiliyor ve Pro akışını bilmiyor.
     *
     * Kapı [ProFlow]'dan okunuyor; [PlanFragment] yalnızca [ProDiffirentFragment]'tan
     * açıldığı için değer her zaman o akışa ait.
     */
    fun logPlanShown() = safe { fa ->
        fa.logEvent(EV_PLAN_SHOWN) {
            param(P_PRO_ENTRY_POINT, sanitize(ProFlow.entryPoint()))
        }
    }

    /**
     * Play'in ödeme ekranı açıldı.
     *
     * [logPurchase]'ın paydası: ödeme ekranına kadar gidip vazgeçenlerin oranı. Pro hunisinin
     * son boşluğu burası — `PlanFragment`'e ulaşmakla parayı ödemek arasındaki fark.
     *
     * Hem abonelikler hem altın/anahtar paketleri buradan geçer; ayrımı [productId] taşır.
     */
    fun logPurchaseStarted(productId: String, value: Double?, currency: String?) = safe { fa ->
        fa.logEvent(EV_PURCHASE_STARTED) {
            param(P_PRODUCT_ID, sanitize(productId))
            param(P_PRO_ENTRY_POINT, sanitize(ProFlow.entryPoint()))
            if (value != null) param(FirebaseAnalytics.Param.VALUE, value)
            if (!currency.isNullOrBlank()) param(FirebaseAnalytics.Param.CURRENCY, sanitize(currency))
        }
    }

    /**
     * Play satın almayı onayladı — asıl dönüşüm.
     *
     * ## Neden gerekli (Play Console zaten geliri gösteriyor)
     * Play Console "kaç abone, ne kadar gelir" der ama satın almayı kullanıcının yolculuğuna
     * bağlayamaz. Bu olay sayesinde "satın alanların kaçı enerji duvarına çarpmıştı", "kaçı Pro
     * panelini üçüncü kez görmüştü", "hangi kanaldan gelmişlerdi" soruları sorulabilir hâle gelir.
     *
     * ## Nereden çağrıldığı önemli
     * `BillingManager.onPurchasesUpdated` — Play'in "az önce bir satın alma işlemi bitti"
     * geri çağırımı. `processPurchase`'tan çağrılamaz: `refreshPurchases()` uygulama her
     * açıldığında Play'de duran AKTİF ABONELİĞİ tekrar oraya sokuyor, yani tek bir aboneden
     * ayda otuz satın alma kaydedilirdi. `queryPurchasesAsync` bu geri çağırımı tetiklemez.
     *
     * Bedeli: uygulama kapalıyken tamamlanan (nadir) bir satın alma yalnızca
     * `refreshPurchases` ile işlenir ve ölçüme girmez. Eksik saymak, otuz kat fazla saymaktan
     * iyidir.
     *
     * @param transactionId Play sipariş numarası. GA4 aynı numaralı satın almaları tekilleştirir;
     *   geri çağırım iki kez gelse bile gelir iki kez sayılmaz.
     */
    fun logPurchase(
        productId: String,
        transactionId: String?,
        value: Double?,
        currency: String?,
    ) = safe { fa ->
        fa.logEvent(FirebaseAnalytics.Event.PURCHASE) {
            param(P_PRODUCT_ID, sanitize(productId))
            param(P_PRO_ENTRY_POINT, sanitize(ProFlow.entryPoint()))
            if (!transactionId.isNullOrBlank()) {
                param(FirebaseAnalytics.Param.TRANSACTION_ID, sanitize(transactionId))
            }
            if (value != null) param(FirebaseAnalytics.Param.VALUE, value)
            if (!currency.isNullOrBlank()) param(FirebaseAnalytics.Param.CURRENCY, sanitize(currency))
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
