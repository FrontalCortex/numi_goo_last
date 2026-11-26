// Kaynak (Android): app/src/main/java/com/example/app/BlindingLessonFragment.kt
//   onCreate (lessonItem + uploadLessonData), showCurrentOperation, controlButtonAnim,
//   showResultPanel, setupQuitButton, showQuestionPanel, finishLessonAfterLastQuestion,
//   showLessonResultFalse, showChestResult, resolveWorstCupTimeFallback, resetClickListener,
//   setupSkipStepButton.
//
// Android karşılığı tek bir sınıf değildir: Fragment'ın yukarıdaki fonksiyonlarında dağınık duran
// ÇAĞRI SIRASINI, zaten taşınmış olan saf kurallara ([BlindingLessonSession],
// [BlindingSequencePlayback], [BlindingLessonRules], [AbacusLessonRules], [UploadLessonDataLogic])
// bağlar. Yeni kural içermez. Amaç, SwiftUI ekranının (iosApp/KorDers) ortak kodu yalnızca ilkel
// tiplerle (Int, Boolean, String) çağırabilmesidir: varsayılan parametreler ve kutulu nullable
// tipler Swift'e taşınmadığı için kurucu "ayarla" fonksiyonlarıyla doldurulur.
//
// Android'den bilinçli farklar:
//  - `operations` içindeki tek bir Int öğesi (Android: listenin tamamını dizi sayar) [BlindingOperation.Other]
//    olur ve gösterilmez; bugünkü tablolarda böyle bir liste yok.
//  - Çarpım sorusunda `secondNumber` null ise Android çöker; burada cevap yanlış sayılır
//    (Kotlin istisnası Swift'te yakalanamadığı için).
//  - `startStepNumber` yoksa ya da ders bitmiş olup `finishStepNumber` yoksa Android çöker;
//    burada soru listesi boş kalır.
package com.sorobit.shared.baglanti

import com.sorobit.shared.abakus.AbacusLessonRules
import com.sorobit.shared.abakus.BlindingLessonFlags
import com.sorobit.shared.abakus.BlindingLessonRules
import com.sorobit.shared.abakus.BlindingLessonSession
import com.sorobit.shared.abakus.BlindingOperation
import com.sorobit.shared.abakus.BlindingSequencePlayback
import com.sorobit.shared.ders.GlobalLessonData
import com.sorobit.shared.ders.LessonItem
import com.sorobit.shared.dersadim.UploadLessonDataLogic
import com.sorobit.shared.matematik.MathOperation

/** Android `operations: List<Any>` → [BlindingOperation] listesi (showCurrentOperation'daki `when`). */
fun korDersIslemlerineCevir(operations: List<Any?>): List<BlindingOperation> = operations.map { oge ->
    when (oge) {
        is MathOperation -> BlindingOperation.Math(oge.firstNumber, oge.operator, oge.secondNumber)
        is List<*> -> BlindingOperation.Sequence(oge.filterIsInstance<Int>())
        else -> BlindingOperation.Other
    }
}

/**
 * Kör ders oturumunu ilkel değerlerle kurar.
 * [globalPartId] = Android `GlobalLessonData.globalPartId`; [lessonType] = `LessonItem.type`;
 * [mapFragmentStepIndex] = `GlobalValues.mapFragmentStepIndex` (kupa süresi yedeği için).
 */
class KorDersKurucu(
    private val globalPartId: Int,
    private val lessonType: Int,
    private val isDailyQuestionMode: Boolean,
    private val mapFragmentStepIndex: Int,
) {
    private var isBlinding: Boolean? = null
    private var isMultiplication: Boolean? = null
    private var blindingMultiplication: Boolean? = null
    private var isExtraction: Boolean? = null
    private var raceBusyLevel: Int? = null
    private var timePeriod: Long? = null
    private var cupWinDelta: Int? = null
    private var cupLossDelta: Int? = null
    private var worstCupTime: Int? = null

    private val operations = mutableListOf<BlindingOperation>()
    private var acikDizi: MutableList<Int>? = null
    private var lessonStep: Int? = null

    fun korAyarla(deger: Boolean) { isBlinding = deger }
    fun carpmaAyarla(deger: Boolean) { isMultiplication = deger }
    fun korCarpmaAyarla(deger: Boolean) { blindingMultiplication = deger }
    fun cikarmaAyarla(deger: Boolean) { isExtraction = deger }
    fun yarisYogunluguAyarla(deger: Int) { raceBusyLevel = deger }
    fun sayiSuresiAyarla(ms: Long) { timePeriod = ms }
    fun kupaKazancAyarla(deger: Int) { cupWinDelta = deger }
    fun kupaKayipAyarla(deger: Int) { cupLossDelta = deger }
    fun enKotuKupaSuresiAyarla(deger: Int) { worstCupTime = deger }

    /** Yeni bir sayı dizisi sorusu açar; sayılar [diziyeEkle] ile verilir. */
    fun diziBaslat() {
        diziKapat()
        acikDizi = mutableListOf()
    }

    fun diziyeEkle(sayi: Int) {
        val dizi = acikDizi ?: mutableListOf<Int>().also { acikDizi = it }
        dizi.add(sayi)
    }

    /** Çarpım (MathOperation) sorusu ekler. */
    fun islemEkle(birinci: Int, islec: String, ikinci: Int) {
        diziKapat()
        operations.add(BlindingOperation.Math(birinci, islec, ikinci))
    }

    private fun diziKapat() {
        acikDizi?.let { operations.add(BlindingOperation.Sequence(it.toList())) }
        acikDizi = null
    }

    /**
     * Android: uploadLessonData. Sorular dışarıdan verilmediyse ve kupa modu değilse ders adımı
     * tablosundan ([UploadLessonDataLogic.blindingUploadLessonData]) yüklenir.
     */
    fun tablodanYukle(
        baslangicAdimi: Int,
        bitisAdimi: Int,
        bitisAdimiVar: Boolean,
        adimSayisi: Int,
        adimBitti: Boolean,
    ) {
        diziKapat()
        if (globalPartId == BlindingLessonRules.CUP_PART_ID) return
        if (operations.isNotEmpty()) return
        if (adimBitti && !bitisAdimiVar) return
        val item = LessonItem(
            type = lessonType,
            title = "",
            offset = 0,
            isCompleted = false,
            stepCount = adimSayisi.coerceAtLeast(0),
            startStepNumber = baslangicAdimi,
            finishStepNumber = if (bitisAdimiVar) bitisAdimi else null,
            stepIsFinish = adimBitti,
        )
        val yuklenen = UploadLessonDataLogic.blindingUploadLessonData(emptyList(), item, globalPartId)
        operations.addAll(korDersIslemlerineCevir(yuklenen.operations))
        lessonStep = yuklenen.lessonStep
    }

    fun kur(): KorDersOturumu {
        diziKapat()
        val flags = BlindingLessonFlags(
            type = lessonType,
            isBlinding = isBlinding,
            isMultiplication = isMultiplication,
            blindingMultiplication = blindingMultiplication,
            isExtraction = isExtraction,
            raceBusyLevel = raceBusyLevel,
            timePeriod = timePeriod,
            cupWinDelta = cupWinDelta,
            cupLossDelta = cupLossDelta,
        )
        // Android: resolveWorstCupTimeFallback — önce güncel item, sonra şablon, sonra 240.
        val sablondan = try {
            GlobalLessonData.createLessonItems(globalPartId).getOrNull(mapFragmentStepIndex)?.worstCupTime
        } catch (e: Exception) {
            null
        }
        return KorDersOturumu(
            globalPartId = globalPartId,
            isDailyQuestionMode = isDailyQuestionMode,
            flags = flags,
            operations = operations.toList(),
            enKotuKupaSuresi = AbacusLessonRules.resolveWorstCupTimeFallback(worstCupTime, sablondan),
            // Android: showQuestionPanel güncel item'daki değeri verir, yoksa 0.
            soruPaneliKupaSuresi = worstCupTime ?: 0,
            yuklenenDersAdimi = lessonStep ?: -1,
        )
    }
}

/**
 * [BlindingSequencePlayback.Step]'in ilkel alanlı karşılığı (Swift'te iç içe tür ve enum adı
 * kullanmamak için). [zamanlayici]: 0 = yok, 1 = sıradaki sayıya geç (ekranı boşalt), 2 = sayıyı göster.
 */
class KorDersAdimi internal constructor(
    /** Tahtaya yazılacak metin; null = dokunma. */
    val metin: String?,
    val zamanlayici: Int,
    val gecikmeMs: Long,
    /** Gösterim bitti: kontrol düğmesi etkinleşir. */
    val bitti: Boolean,
    /** Gösterim başladı: kontrol düğmesi devre dışı kalır. */
    val kontrolKapat: Boolean,
) {
    companion object {
        const val ZAMANLAYICI_YOK = 0
        const val ZAMANLAYICI_SONRAKI_SAYI = 1
        const val ZAMANLAYICI_SAYI_GOSTER = 2
    }
}

private fun BlindingSequencePlayback.Step.adima(): KorDersAdimi = KorDersAdimi(
    metin = display,
    zamanlayici = when (schedule) {
        BlindingSequencePlayback.Timer.SHOW_NEXT_NUMBER -> KorDersAdimi.ZAMANLAYICI_SONRAKI_SAYI
        BlindingSequencePlayback.Timer.SEQUENCE_REVEAL -> KorDersAdimi.ZAMANLAYICI_SAYI_GOSTER
        null -> KorDersAdimi.ZAMANLAYICI_YOK
    },
    gecikmeMs = delayMs,
    bitti = finished,
    kontrolKapat = disableControl,
)

/** Kör ders ekranının görünümden bağımsız durumu. Ana iş parçacığından kullanılır. */
class KorDersOturumu internal constructor(
    val globalPartId: Int,
    val isDailyQuestionMode: Boolean,
    private val flags: BlindingLessonFlags,
    operations: List<BlindingOperation>,
    /** ChestResult'a aktarılan `worstCupTime` (yedekli). */
    val enKotuKupaSuresi: Int,
    /** QuestionPanelFragment'a aktarılan `worstCupTime` (yedeksiz; yoksa 0). */
    val soruPaneliKupaSuresi: Int,
    /** Android'in `GlobalValues.lessonStep`'e yazdığı değer; yazılmadıysa -1. */
    val yuklenenDersAdimi: Int,
) {
    private val session = BlindingLessonSession(operations, flags)
    private val playback = BlindingSequencePlayback(flags.timePeriod)
    private val islemler = operations

    /** calculateChestScore sonucu; [bitis] çağrılınca dolar. */
    var carpan: Float = 1f
        private set
    var toplamPuan: Int = 0
        private set

    val soruSayisi: Int get() = session.totalQuestions
    val suankiIndeks: Int get() = session.currentIndex
    val dogruSayisi: Int get() = session.correctAnswer

    // --- Görünüm bayrakları ---

    val kupaModu: Boolean get() = globalPartId == BlindingLessonRules.CUP_PART_ID

    /** Android: blindingOrRace — `isBlinding == true` ise sayı kutusu, değilse abaküs görünür. */
    val klavyeyleCevap: Boolean get() = flags.isBlinding == true

    /** Cevabın abaküsten okunduğu ders (`isBlinding == null`). */
    val abakusleCevap: Boolean get() = BlindingLessonRules.shouldResetAbacusAfterAnswer(flags)

    /** Android: showCurrentOperation'ın çarpım dalı (iki sayı + işleç yan yana). */
    val carpmaGosterimi: Boolean get() = flags.blindingMultiplication == true || flags.isMultiplication == true

    val sureGorunur: Boolean get() = BlindingLessonRules.isTimerVisible(flags.type, globalPartId)

    /** Android: cupWay + onViewCreated (günlük soruda da gizli). */
    val ilerlemeGizli: Boolean get() = BlindingLessonRules.isProgressBarHidden(globalPartId) || isDailyQuestionMode
    val ipucuGizli: Boolean get() = BlindingLessonRules.isHintHidden(globalPartId) || isDailyQuestionMode

    /** Android: initializeGuideSystem — 6. bölümde ve çarpım dersinde adım atlama düğmesi yok. */
    val adimAtlaGizli: Boolean get() = globalPartId == 6 || flags.isMultiplication == true

    /** Android: initializeGuideSystem — 6. bölümde sıfırla düğmesi yok. */
    val sifirlaGizli: Boolean get() = globalPartId == 6

    val yarisDersi: Boolean get() = BlindingLessonRules.isRacePanelLesson(flags)

    /** Android: setupAbacusController — boncuk animasyon süresi (ms). */
    val boncukAnimasyonMs: Long get() = AbacusLessonRules.beadAnimationDurationMs(flags.type)

    // --- Süre ---

    fun sureBaslatilmali(sureBasladi: Boolean): Boolean =
        BlindingLessonRules.shouldStartTimer(flags.type, sureBasladi)

    fun sureMetni(saniye: Int): String = AbacusLessonRules.formatTimer(saniye)

    // --- Soru gösterimi ---

    private fun suanki(): BlindingOperation? = islemler.getOrNull(session.currentIndex)

    /** Çarpım gösteriminde tahtaya yazılacaklar; null = önceki metin kalır (Android `?.let`). */
    fun birinciSayiMetni(): String? = (suanki() as? BlindingOperation.Math)?.firstNumber?.toString()
    fun islecMetni(): String? = (suanki() as? BlindingOperation.Math)?.operator
    fun ikinciSayiMetni(): String? = (suanki() as? BlindingOperation.Math)?.secondNumber?.toString()

    /**
     * Android: showCurrentOperation'ın dizi dalı. Şu anki soru bir sayı dizisiyse gösterimi başlatır;
     * değilse (çarpım gösterimi dahil) null döner.
     */
    fun diziBaslat(): KorDersAdimi? {
        if (carpmaGosterimi) return null
        val dizi = suanki() as? BlindingOperation.Sequence ?: return null
        return playback.start(dizi.numbers).adima()
    }

    /** Çarpım dışı gösterimde tek MathOperation: yalnızca ilk sayı yazılır. */
    fun tekSayiMetni(): String? {
        if (carpmaGosterimi) return null
        return (suanki() as? BlindingOperation.Math)?.firstNumber?.toString()
    }

    val diziGosteriliyor: Boolean get() = playback.isShowingSequence
    val diziVar: Boolean get() = playback.currentSequence.isNotEmpty()

    /** Android: onShowNextNumberStep ([KorDersAdimi.ZAMANLAYICI_SONRAKI_SAYI] dolunca çağrılır). */
    fun sonrakiSayiAdimi(): KorDersAdimi = playback.onShowNextNumberStep().adima()

    /** Android: onSequenceRevealStep ([KorDersAdimi.ZAMANLAYICI_SAYI_GOSTER] dolunca ya da adım atlanınca). */
    fun sayiGosterAdimi(): KorDersAdimi = playback.onSequenceRevealStep().adima()

    fun diziDurdur() {
        playback.stop()
    }

    /** Android: onRestartSequenceAfterReset. */
    fun diziYenidenBaslat(): KorDersAdimi = playback.start(playback.currentSequence).adima()

    // --- Cevap ---

    /** Android: stepAnswerAlgorithm. */
    fun kontrolEt(girdi: String, abakusDegeri: Int): Boolean = try {
        session.stepAnswerAlgorithm(girdi, abakusDegeri, playback.currentSequenceIndex)
    } catch (e: NullPointerException) {
        false
    }

    /** Android: updateProgressBar'ın hedef ağırlığı; soru yoksa 0. */
    fun ilerlemeBitisAgirligi(): Float =
        if (islemler.isEmpty()) 0f else AbacusLessonRules.progressEndWeight(session.currentIndex, islemler.size)

    fun kupaDeltasi(dogru: Boolean): Int = BlindingLessonRules.cupDelta(flags, dogru)

    /** Deltanın yazılacağı bekleyen alanın adı ([com.sorobit.shared.abakus.CupDeltaTarget]). */
    fun kupaDeltaHedefi(): String = BlindingLessonRules.cupDeltaTarget(flags).name

    /** Android: showResultPanel(true) başı — doğru sayısı artar; dönüş: konfeti oynatılsın mı. */
    fun dogruIslendi(): Boolean {
        session.correctAnswer++
        return AbacusLessonRules.shouldPlayPerfectAnimation(
            session.currentIndex, islemler.size, session.correctAnswer, session.totalQuestions,
        )
    }

    val dogruCevapEtiketi: String get() = session.correctAnswerLabel
    val seninCevabinMetni: String get() = session.correctAnswerText

    /** Kontrol akışının sonu ve sıfırla düğmesi: `controlNumber = 0`. */
    fun kontrolSifirla() {
        session.clearControlNumber()
    }

    /** `currentIndex++`; dönüş: gösterilecek soru kaldı mı. */
    fun sonrakiSoru(): Boolean {
        session.currentIndex++
        return session.currentIndex <= islemler.size - 1
    }

    /** [com.sorobit.shared.abakus.BlindingWrongAnswerNext] adı. */
    fun yanlisSonrasi(): String =
        BlindingLessonRules.wrongAnswerNext(globalPartId, isDailyQuestionMode, flags).name

    /** [BlindingLessonRules.ResultFalseAction] adı. */
    fun yanlisSonucEylemi(): String = BlindingLessonRules.lessonResultFalseAction(globalPartId, flags).name

    // --- Bitiş ---

    fun basariOrani(): Float = AbacusLessonRules.successRate(session.correctAnswer, session.totalQuestions)
    fun dersPuani(): Int = AbacusLessonRules.dersPuani(basariOrani())

    /**
     * Android: finishLessonAfterLastQuestion. [sureMetni] = süre göstergesindeki metin ("d:ss").
     * Dönüş: [com.sorobit.shared.abakus.BlindingLessonOutcome] adı; [carpan] ve [toplamPuan] dolar.
     */
    fun bitis(sureMetni: String): String {
        val puan = AbacusLessonRules.calculateChestScore(basariOrani(), sureMetni, enKotuKupaSuresi)
        carpan = puan.first
        toplamPuan = puan.second
        return BlindingLessonRules.finishLessonAfterLastQuestion(
            isDailyQuestionMode = isDailyQuestionMode,
            globalPartId = globalPartId,
            flags = flags,
            correctAnswer = session.correctAnswer,
            totalQuestions = session.totalQuestions,
            toplamPuan = toplamPuan,
        ).name
    }

    /** Android: fubHintClickListener → splitTextEqually. */
    fun ipucuMetni(ipucu: String): String = AbacusLessonRules.splitTextEqually(ipucu)
}
