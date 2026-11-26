// Kaynak (Android): app/src/main/java/com/example/app/AbacusFragment.kt
//   (onCreate + uploadLessonData, showCurrentOperation, controlButtonAnim, showResultPanel,
//    showQuestionPanel, showLessonResult / showLessonResultFalseOnly / showChestResult,
//    resolveWorstCupTimeFallback, showHint)
//
// iOS ders ekranı için ince köprü. Yeni kural içermez: var olan ortak mantığı
// ([UploadLessonDataLogic], [LessonOperations], [AbacusLessonSession], [AbacusLessonRules])
// Swift'ten yalın imzalarla (iç içe sınıf, sealed sınıf, null olabilen ilkel tip, Swift anahtar
// sözcüğüyle çakışan `operator` adı olmadan) çağrılabilir hâle getirir.
//
// Android'den bilinçli farklar (Kotlin istisnaları Swift'te yakalanamadığı için):
//  - Android'de çöken girdiler burada null döner: [AbakusDersKoprusu.dersGirdisi] başlangıç/bitiş
//    adımı eksikse, [AbakusDersKoprusu.akis] cevabı hesaplanamayan soru varsa.
package com.sorobit.shared.dersadim

import com.sorobit.shared.abakus.AbacusLessonOutcome
import com.sorobit.shared.abakus.AbacusLessonRules
import com.sorobit.shared.abakus.AbacusLessonSession
import com.sorobit.shared.abakus.AbacusOperation
import com.sorobit.shared.ders.GlobalLessonData
import com.sorobit.shared.ders.LessonItem

/** AbacusFragment'in dersi açarken [LessonItem]'dan okuduğu değerler (yalın). */
data class AbakusDersGirdisi(
    /** LessonItem.type (0 ders, 2 sandık). */
    val lessonType: Int,
    /** LessonItem.isBlinding == true. */
    val isBlinding: Boolean,
    /** Soru tablosuna sorulacak, çözümlenmiş kimlik (uploadLessonData'daki resolvedLessonId). */
    val lessonOperationsId: Int,
    /** Android'in `GlobalValues.lessonStep`'e yazdığı değer (startStepNumber). */
    val lessonStep: Int,
    /** resolveWorstCupTimeFallback sonucu (öğe → şablon → 240). */
    val worstCupTime: Int,
    /** LessonItem.lessonHint; yoksa boş. */
    val lessonHint: String,
    val title: String,
)

/** Kontrol düğmesinin sonucu ([AbacusLessonSession.ControlResult]'ın yalın karşılığı). */
data class AbakusKontrolSonucu(
    /** 0 = yok sayıldı (100 ms kuralı), 1 = çevrimdışı, 2 = cevap denetlendi. */
    val durum: Int,
    val dogru: Boolean,
    val sureDurdu: Boolean,
    /** İlerleme çubuğunun hedef ağırlığı (soru yoksa sonsuz olabilir; görünüm kırpmalı). */
    val ilerlemeHedefi: Float,
    val konfeti: Boolean,
    /** Yanlış panelindeki beyaz satır ("Senin cevabın N"). */
    val cevapMetni: String,
    /** Yanlış panelindeki kırmızı satır ("Doğru cevap N"). */
    val dogruCevapMetni: String,
) {
    companion object {
        const val YOK_SAYILDI = 0
        const val CEVRIMDISI = 1
        const val CEVAPLANDI = 2
    }
}

/** Ders bitince sonuç ekranlarına aktarılan değerler (Android: Bundle alanları). */
data class AbakusDersBitisi(
    /** 0 = LessonResult, 1 = LessonResultFalse, 2 = ChestResult, 3 = LessonResultFalse (isChestFailure). */
    val yonlendirme: Int,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val successRate: Float,
    val dersPuani: Int,
    /** "d:ss" biçiminde süre (sandık dışında hep "0:00"). */
    val time: String,
    val seconds: Int,
    val worstCupTime: Int,
    val carpan: Float,
    val toplamPuan: Int,
)

/** Swift'in hazır soru listesi kurması için (AbacusFragment'e "operations" argümanıyla gelen sorular). */
class AbakusSoruListesi {
    internal val sorular = mutableListOf<AbacusOperation>()

    val adet: Int get() = sorular.size

    /** İki sayılı soru; [islec] "+", "-", "x" ya da "/". */
    fun ekle(ilkSayi: Int, islec: String, ikinciSayi: Int) {
        sorular.add(AbacusOperation(ilkSayi, islec, ikinciSayi))
    }

    /** Tek sayılı soru (Android: iki sayı null, `operator` alanı sayının metni). */
    fun ekleTekSayi(sayi: Int) {
        sorular.add(AbacusOperation(null, sayi.toString(), null))
    }
}

/** [AbacusLessonSession] sarmalayıcısı. */
class AbakusDersAkisi internal constructor(private val oturum: AbacusLessonSession) {
    val soruSayisi: Int get() = oturum.totalQuestions
    val suankiIndeks: Int get() = oturum.currentIndex
    val dogruSayisi: Int get() = oturum.correctAnswer

    /** Sandık dersinde "Başla" düğmesi gösterilir. */
    val baslaDugmesiGerekli: Boolean get() = oturum.needsStartButton

    /** Süre göstergesi yalnızca sandık dersinde görünür. */
    val sureGorunur: Boolean get() = oturum.isTimerVisible
    val sureCalisiyor: Boolean get() = oturum.isTimerStarted
    val sureMetni: String get() = oturum.currentTime
    val basladi: Boolean get() = oturum.lessonStarted

    /** Zaten başladıysa false. */
    fun baslat(): Boolean = oturum.start()

    /**
     * showCurrentOperation: null dönen parça için Android metni DEĞİŞTİRMEZ (önceki sorunun
     * metni kalır); görünüm de null gelen parçayı olduğu gibi bırakmalıdır.
     */
    fun ilkSayiMetni(): String? = oturum.currentOperation()?.firstNumber?.toString()

    fun islecMetni(): String? = oturum.currentOperation()?.operator

    fun ikinciSayiMetni(): String? = oturum.currentOperation()?.secondNumber?.toString()

    /** Zamanlayıcının 1 sn'lik adımı (ilk adım başlar başlamaz atılır). */
    fun sureAdimi(): String = oturum.onTimerTick()

    fun sureyiDurdur() = oturum.stopTimer()

    fun kontrol(nowMs: Long, isOnline: Boolean, abacusValue: Int): AbakusKontrolSonucu =
        when (val s = oturum.onControlPressed(nowMs, isOnline, abacusValue)) {
            is AbacusLessonSession.ControlResult.Ignored ->
                AbakusKontrolSonucu(AbakusKontrolSonucu.YOK_SAYILDI, false, false, 0f, false, "", "")
            is AbacusLessonSession.ControlResult.Offline ->
                AbakusKontrolSonucu(AbakusKontrolSonucu.CEVRIMDISI, false, false, 0f, false, "", "")
            is AbacusLessonSession.ControlResult.Answered ->
                AbakusKontrolSonucu(
                    durum = AbakusKontrolSonucu.CEVAPLANDI,
                    dogru = s.isCorrect,
                    sureDurdu = s.timerStopped,
                    ilerlemeHedefi = s.progressEndWeight,
                    konfeti = s.playPerfectAnimation,
                    cevapMetni = if (s.isCorrect) "" else oturum.correctAnswerText,
                    dogruCevapMetni = if (s.isCorrect) "" else oturum.correctAnswerLabel,
                )
        }

    /** Sonuç panelindeki düğme: true → gösterilecek soru var, false → sorular bitti. */
    fun devam(): Boolean = oturum.onContinue()

    /** Ders sonu değerleri (Android: showQuestionPanel + sonuç dinleyicisi). Süreyi de durdurur. */
    fun bitis(worstCupTime: Int): AbakusDersBitisi {
        oturum.stopTimer()
        val basari = oturum.currentSuccessRate()
        val (carpan, toplamPuan) = AbacusLessonRules.calculateChestScore(basari, oturum.currentTime, worstCupTime)
        val yon = when (oturum.outcome(worstCupTime)) {
            AbacusLessonOutcome.LESSON_RESULT -> 0
            AbacusLessonOutcome.LESSON_RESULT_FALSE -> 1
            AbacusLessonOutcome.CHEST_RESULT -> 2
            AbacusLessonOutcome.CHEST_FAILURE -> 3
        }
        return AbakusDersBitisi(
            yonlendirme = yon,
            correctAnswers = oturum.correctAnswer,
            totalQuestions = oturum.totalQuestions,
            successRate = basari,
            dersPuani = oturum.dersPuani(),
            time = oturum.currentTime,
            seconds = oturum.seconds,
            worstCupTime = worstCupTime,
            carpan = carpan,
            toplamPuan = toplamPuan,
        )
    }
}

object AbakusDersKoprusu {
    /**
     * Dersin açılış değerleri. [partId] ve [mapIndex] yalnızca kupa süresi yedeği (şablon) için
     * kullanılır. Android'de çökecek öğe (startStepNumber null ya da bitmiş derste
     * finishStepNumber null) için null döner.
     */
    fun dersGirdisi(lessonItem: LessonItem, partId: Int, mapIndex: Int): AbakusDersGirdisi? {
        val baslangic = lessonItem.startStepNumber ?: return null
        if (lessonItem.stepIsFinish && lessonItem.finishStepNumber == null) return null
        val istenen = UploadLessonDataLogic.requestedLessonId(lessonItem)
        val cozulmus = LessonOperations.resolveLessonOperationsId(istenen, lessonItem.minLessonOperationsId())
        val sablonSure = GlobalLessonData.createLessonItems(partId).getOrNull(mapIndex)?.worstCupTime
        return AbakusDersGirdisi(
            lessonType = lessonItem.type,
            isBlinding = lessonItem.isBlinding == true,
            lessonOperationsId = cozulmus,
            lessonStep = baslangic,
            worstCupTime = AbacusLessonRules.resolveWorstCupTimeFallback(lessonItem.worstCupTime, sablonSure),
            lessonHint = lessonItem.lessonHint ?: "",
            title = lessonItem.title,
        )
    }

    /** `MapFragment.getLessonOperations(lessonOperationsId)` sorularıyla ders akışı. */
    fun akisDersKimliginden(lessonOperationsId: Int, lessonType: Int): AbakusDersAkisi? {
        val sorular = LessonOperations.getLessonOperations(lessonOperationsId)
            .map { AbacusOperation(it.firstNumber, it.operator, it.secondNumber) }
        return akis(sorular, lessonType)
    }

    /** Dışarıdan verilen sorularla (Android: "operations" argümanı) ders akışı. */
    fun akisSorulardan(sorular: AbakusSoruListesi, lessonType: Int): AbakusDersAkisi? =
        akis(sorular.sorular.toList(), lessonType)

    /** Ders ipucu balonunun metni (showHint: splitTextEqually). */
    fun ipucuMetni(lessonHint: String): String = AbacusLessonRules.splitTextEqually(lessonHint)

    /**
     * stepAnswerAlgorithm'in istisna atmadan cevap hesaplayabildiği soru mu:
     * iki sayı da null ise `operator` null ya da tam sayı olmalı; değilse iki sayı da dolu olmalı.
     */
    fun cevaplanabilir(soru: AbacusOperation): Boolean =
        if (soru.firstNumber == null && soru.secondNumber == null) {
            soru.operator == null || soru.operator.toIntOrNull() != null
        } else {
            soru.firstNumber != null && soru.secondNumber != null
        }

    private fun akis(sorular: List<AbacusOperation>, lessonType: Int): AbakusDersAkisi? {
        if (!sorular.all { cevaplanabilir(it) }) return null
        return AbakusDersAkisi(AbacusLessonSession(sorular, lessonType))
    }
}
