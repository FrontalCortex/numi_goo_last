// KAYNAK (görünümden bağımsız kısımlar):
//  - app/src/main/java/com/example/app/AbacusFragment.kt → uploadLessonData()
//  - app/src/main/java/com/example/app/BlindingLessonFragment.kt → uploadLessonData()
//  - app/src/main/java/com/example/app/TutorialFragment.kt → öğretici bitince
//    `MapFragment.getLessonOperations(lessonStep)` çağrısı (iki yerde, aynı biçimde)
//
// Android'de bu fonksiyonlar Fragment alanlarına (operations) ve GlobalValues.lessonStep'e yazar;
// burada aynı hesap yan etkisiz yapılır ve sonuç [YuklenenDers] ile döner. Çağıran taraf
// `lessonStep` değerini (null değilse) global duruma kendisi yazmalıdır.
//
// Android'den bilinçli farklar: yok. `!!` kullanımları aynen korunur (startStepNumber / finishStepNumber
// null ise Android'de NullPointerException; burada da NullPointerException).
package com.sorobit.shared.dersadim

import com.sorobit.shared.ders.LessonItem
import com.sorobit.shared.matematik.MathOperation

object UploadLessonDataLogic {

    /**
     * uploadLessonData sonucu.
     * [lessonStep]: Android'in `GlobalValues.lessonStep`'e yazdığı değer; yazılmadıysa (sorular bundle'dan
     * geldiyse ya da kupa modunda) null.
     * [resolvedLessonId]: tabloya sorulan kimlik; tabloya hiç gidilmediyse null.
     */
    data class YuklenenDers<T>(
        val operations: List<T>,
        val lessonStep: Int?,
        val resolvedLessonId: Int?,
    )

    /**
     * İki Fragment'ta da aynı olan "istenen ders adımı" hesabı:
     * `lessonStep = startStepNumber!!`; 0 ya da negatifse 1 sayılır; ders bitmişse (stepIsFinish)
     * `finishStepNumber!!` kullanılır.
     * Not (Android'deki sıra korunur): startStepNumber null ise stepIsFinish true olsa bile çöker.
     */
    fun requestedLessonId(lessonItem: LessonItem): Int {
        val lessonStep = lessonItem.startStepNumber!!
        // lessonStep değerini kontrol et ve güvenli bir şekilde kullan
        val currentLessonStep = if (lessonStep > 0) lessonStep else 1
        return if (lessonItem.stepIsFinish) {
            lessonItem.finishStepNumber!!
        } else {
            currentLessonStep
        }
    }

    /**
     * AbacusFragment.uploadLessonData: [bundleOperations] (Android: arguments "operations"; yoksa boş liste)
     * doluysa aynen kullanılır; boşsa tablo sorulur.
     */
    fun abacusUploadLessonData(
        bundleOperations: List<MathOperation>,
        lessonItem: LessonItem,
    ): YuklenenDers<MathOperation> {
        if (bundleOperations.isEmpty()) {
            val lessonStep = lessonItem.startStepNumber!!
            val minLessonId = lessonItem.minLessonOperationsId()
            val requestedLessonId = requestedLessonId(lessonItem)
            val resolvedLessonId = LessonOperations.resolveLessonOperationsId(
                requestedLessonId,
                minLessonId,
            )
            return YuklenenDers(LessonOperations.getLessonOperations(resolvedLessonId), lessonStep, resolvedLessonId)
        }
        return YuklenenDers(bundleOperations, null, null)
    }

    /**
     * BlindingLessonFragment.uploadLessonData. Kupa modunda ([globalPartId] == 9) sayılar bundle'dan gelir,
     * ek işlem yapılmaz (bundle boş olsa bile).
     */
    fun blindingUploadLessonData(
        bundleOperations: List<Any>,
        lessonItem: LessonItem,
        globalPartId: Int,
    ): YuklenenDers<Any> {
        // Kupa modu: sayılar zaten bundle'dan geldi, ek işlem gerekmez
        if (globalPartId == 9) return YuklenenDers(bundleOperations, null, null)

        if (bundleOperations.isEmpty()) {
            val lessonStep = lessonItem.startStepNumber!!
            val minLessonId = lessonItem.minLessonOperationsId()
            val requestedLessonId = requestedLessonId(lessonItem)
            val resolvedLessonId = LessonOperations.resolveLessonOperationsBlindingId(
                requestedLessonId,
                minLessonId,
            )
            return YuklenenDers(
                LessonOperations.getLessonOperationsBlinding(resolvedLessonId),
                lessonStep,
                resolvedLessonId,
            )
        }
        return YuklenenDers(bundleOperations, null, null)
    }

    /**
     * TutorialFragment: öğretici bitince ders ekranına verilen sorular. Android burada çözümleme (resolve)
     * YAPMAZ ve isBlinding derslerde bile abaküs tablosunu sorar; sonuç bundle'a "operations" olarak
     * KONMAZ (yalnızca lessonItem konur), yani ders ekranı soruları kendi uploadLessonData'sı ile yeniden üretir.
     */
    fun tutorialFinishedOperations(lessonStep: Int): List<MathOperation> =
        LessonOperations.getLessonOperations(lessonStep)
}
