package com.example.app

/**
 * Sobi'ye giden adım bağlamı: çocuk bir şey anlatmak zorunda kalmadan Sobi nerede takıldığını
 * bilsin diye (docs/ANLAMADIM_YARDIM_PLANI.md, "Anlık bağlam"). Alan adları sunucudaki
 * `chat.sanitizeContext` ile aynı; sunucu bilinmeyen alanları atar, aralık dışı sayıları yok sayar.
 *
 * Soru adımında abaküs alanları (base/target/current/valueLog) dolu; anlatım adımında boş, onun
 * yerine Sobi dersin kendi metnine ([recentSteps], [stepText]) ve öğretilen kurallara dayanıyor.
 *
 * Kişisel bilgi (ad, e-posta, yaş) BURADA YOK ve olmamalı.
 */
data class SobiContext(
    val stepId: String?,
    val stepText: String?,
    val questionText: String?,
    /** Adımın başında abaküste yazan değer. */
    val base: Int?,
    /** Beklenen cevap; null ise anlatım adımı. */
    val target: Int?,
    val current: Int?,
    /** Bu adımda "Kontrol Et"e kaç kez basıldı. */
    val attempts: Int,
    /** Abaküsün bu adımdaki değerleri, sırayla (ilk kayıt adımın başı). */
    val valueLog: List<Int>,
    val lessonTitle: String? = null,
    /** Bu adımdan önceki birkaç adımın metni, eskiden yeniye. */
    val recentSteps: List<String> = emptyList(),
    /** Çocuğun o ana kadar öğrendiği kurallar ([SobiCurriculum]). */
    val taughtRules: List<String> = emptyList(),
) {
    fun toPayload(): Map<String, Any> = buildMap {
        stepId?.let { put("stepId", it) }
        stepText?.takeIf { it.isNotBlank() }?.let { put("stepText", it) }
        questionText?.takeIf { it.isNotBlank() }?.let { put("questionText", it) }
        base?.let { put("base", it) }
        target?.let { put("target", it) }
        current?.let { put("current", it) }
        put("attempts", attempts)
        put("valueLog", valueLog)
        // Öğretici alıştırma hazırlayamıyor: "Kontrol Et" yalnızca adımın kendi cevabını denetliyor.
        put("practice", false)
        lessonTitle?.takeIf { it.isNotBlank() }?.let { put("lessonTitle", it) }
        if (recentSteps.isNotEmpty()) put("recentSteps", recentSteps)
        if (taughtRules.isNotEmpty()) put("taughtRules", taughtRules)
    }
}

/**
 * Öğretici numarasından çocuğun o ana kadar öğrendiği kurallar. Sobi henüz öğretilmemiş bir kuralı
 * anlatıp kafa karıştırmasın diye (talimat metni, "Anlatım adımlarında"). Anahtarlar sunucudaki
 * `chat.RULE_LABELS` ile ve ödevdeki kural anahtarlarıyla aynı.
 *
 * Sıra derslerin sırası (strings_tutorial.xml'deki "Öğretici N" bölümleri): o öğreticinin kendi
 * kuralı da listede, çünkü ders onu anlatıyor.
 */
object SobiCurriculum {

    private val ADD_DIRECT = listOf("add_direct")
    private val ADD_FIVE = ADD_DIRECT + "add_five"
    private val ADD_TEN = ADD_FIVE + "add_ten"
    private val ADD_BEAD = ADD_TEN + "add_bead"
    private val SUB_DIRECT = ADD_BEAD + "sub_direct"
    private val SUB_FIVE = SUB_DIRECT + "sub_five"
    private val SUB_TEN = SUB_FIVE + "sub_ten"
    private val SUB_BEAD = SUB_TEN + "sub_bead"

    fun taughtRules(tutorialNumber: Int): List<String> = when (tutorialNumber) {
        1, 100, 101 -> emptyList()            // sayıları abaküse yazmak
        2, 102, 103 -> ADD_DIRECT
        3, 4 -> ADD_FIVE
        5, 6, 104, 7, 105 -> ADD_TEN
        8, 9 -> ADD_BEAD
        10 -> SUB_DIRECT
        11, 1100 -> SUB_FIVE
        12, 13, 14, 999, 15 -> SUB_TEN
        16, 17 -> SUB_BEAD
        in 18..26 -> SUB_BEAD                 // çarpma dersleri: toplama ve çıkarma kuralları biliniyor
        else -> emptyList()
    }
}

/**
 * Öğreticideki Sobi durumu. İki parçası var:
 *  - Adım durumu (başlangıç değeri, abaküs kaydı, deneme sayısı, son gösterim): her adımda
 *    [startStep] ile sıfırlanır.
 *  - Sohbet (oturum kimliği, mesajlar, öneri balonları): öğretici boyunca sürer; çocuk anlatımın
 *    her yerinde soru sorabiliyor, sohbet adım değişince kopmasın. Fragment kapanınca biter.
 *
 * Neden ayrı sınıf: TutorialFragment çok büyük ve hassas (devam notundaki anlaşma). Sobi'nin
 * durumu burada, fragment'ta yalnızca bağlantı noktaları var.
 */
class SobiStepTracker {

    data class Message(val fromChild: Boolean, val text: String)

    var stepIndex: Int = -1
        private set
    var base: Int? = null
        private set
    var attempts: Int = 0
        private set
    var sessionId: String? = null
    val messages = ArrayList<Message>()
    /** Son "abaküste göster" işi; "Bir daha göster" sunucuya gitmeden bunu oynatır. Adıma bağlı. */
    var lastShowAction: ServerSobi.Action? = null
    /** Son cevabın öneri balonları (panel yeniden açılınca aynıları görünsün). */
    var suggestions: List<String> = emptyList()
    /** Sobi gösterim yaparken kayıt durur: gösterim çocuğun hareketi değil. */
    var paused: Boolean = false

    private val log = ArrayList<Int>()

    /** Yeni adım: adım durumu sıfırlanır, sohbet sürer. */
    fun startStep(stepIndex: Int) {
        this.stepIndex = stepIndex
        base = null
        attempts = 0
        lastShowAction = null
        paused = false
        log.clear()
    }

    /** Adımın başlangıç değeri; adımın boncuk animasyonları bitince bir kez. */
    fun captureBase(value: Int) {
        if (base != null) return
        base = value
        log.clear()
        log += value
    }

    /** Abaküs her değiştiğinde. Başlangıç alınmadan gelen değerler adımın kurulumudur, sayılmaz. */
    fun record(value: Int) {
        if (paused || base == null) return
        if (log.lastOrNull() == value) return
        if (log.size >= MAX_LOG) log.removeAt(1) // ilk kayıt (adımın başı) kalsın
        log += value
    }

    fun onAnswerChecked() {
        attempts++
    }

    /** Soru adımının bağlamı: abaküs alanları dolu. */
    fun questionContext(
        stepId: String?, stepText: String?, questionText: String?, target: Int?, current: Int?,
        lessonTitle: String?, recentSteps: List<String>, taughtRules: List<String>,
    ) = SobiContext(
        stepId = stepId,
        stepText = stepText,
        questionText = questionText,
        base = base ?: log.firstOrNull(),
        target = target,
        current = current,
        attempts = attempts,
        valueLog = log.toList(),
        lessonTitle = lessonTitle,
        recentSteps = recentSteps,
        taughtRules = taughtRules,
    )

    /**
     * Anlatım adımının bağlamı: abaküs alanları boş. Abaküsün değeri bile gönderilmiyor: anlatımda
     * boncukları dersin kendi animasyonları oynatıyor ve denetleyicinin değeri o sırada geride
     * kalabiliyor; Sobi "abaküsünde 9 var" derken yanılmasın.
     */
    fun explanationContext(
        stepId: String?, stepText: String?, questionText: String?,
        lessonTitle: String?, recentSteps: List<String>, taughtRules: List<String>,
    ) = SobiContext(
        stepId = stepId,
        stepText = stepText,
        questionText = questionText,
        base = null,
        target = null,
        current = null,
        attempts = 0,
        valueLog = emptyList(),
        lessonTitle = lessonTitle,
        recentSteps = recentSteps,
        taughtRules = taughtRules,
    )

    private companion object {
        /** Sunucu da en çok 200 kayıt alıyor. */
        const val MAX_LOG = 200
    }
}
