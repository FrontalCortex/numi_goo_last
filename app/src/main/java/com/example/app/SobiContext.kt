package com.example.app

/**
 * Sobi'ye giden adım bağlamı: çocuk bir şey anlatmak zorunda kalmadan Sobi nerede takıldığını
 * bilsin diye (docs/ANLAMADIM_YARDIM_PLANI.md, "Anlık bağlam"). Alan adları sunucudaki
 * `chat.sanitizeContext` ile aynı; sunucu bilinmeyen alanları atar, aralık dışı sayıları yok sayar.
 *
 * Kişisel bilgi (ad, e-posta, yaş) BURADA YOK ve olmamalı.
 */
data class SobiContext(
    val stepId: String?,
    val stepText: String?,
    val questionText: String?,
    /** Adımın başında abaküste yazan değer. */
    val base: Int?,
    /** Beklenen cevap. */
    val target: Int?,
    val current: Int?,
    /** Bu adımda "Kontrol Et"e kaç kez basıldı. */
    val attempts: Int,
    /** Abaküsün bu adımdaki değerleri, sırayla (ilk kayıt adımın başı). */
    val valueLog: List<Int>,
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
    }
}

/**
 * Bir öğretici adımındaki Sobi durumu: adımın başlangıç değeri, abaküs kaydı, deneme sayısı ve
 * sohbet (oturum kimliği, mesajlar). Adım değişince [reset] ile sıfırlanır; aynı adımda panel
 * kapatılıp açılınca sohbet kaldığı yerden sürer.
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
    /** Son "abaküste göster" işi; "Bir daha göster" sunucuya gitmeden bunu oynatır. */
    var lastShowAction: ServerSobi.Action? = null
    /** Son cevabın öneri balonları (panel yeniden açılınca aynıları görünsün). */
    var suggestions: List<String> = emptyList()
    /** Sobi gösterim yaparken kayıt durur: gösterim çocuğun hareketi değil. */
    var paused: Boolean = false

    private val log = ArrayList<Int>()

    /** Sohbet açıldı mı (ekrandaki "Sobi" düğmesi buna göre görünür). */
    val hasConversation: Boolean get() = messages.isNotEmpty()

    fun reset(stepIndex: Int) {
        this.stepIndex = stepIndex
        base = null
        attempts = 0
        sessionId = null
        messages.clear()
        lastShowAction = null
        suggestions = emptyList()
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

    fun context(stepId: String?, stepText: String?, questionText: String?, target: Int?, current: Int?) = SobiContext(
        stepId = stepId,
        stepText = stepText,
        questionText = questionText,
        base = base ?: log.firstOrNull(),
        target = target,
        current = current,
        attempts = attempts,
        valueLog = log.toList(),
    )

    private companion object {
        /** Sunucu da en çok 200 kayıt alıyor. */
        const val MAX_LOG = 200
    }
}
