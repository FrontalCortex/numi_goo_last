package com.example.app

import com.google.firebase.auth.FirebaseAuth

object BadgePrecalcHelper {
    /**
     * Cup delta yazma ve rozet kontrol işlemlerini BlindingLessonFragment içinde
     * erken başlatarak (Optimistic UI mantığı) arayüz bekleme süresini yok eder.
     */
    fun executeCupDeltaUpdateAsync(lessonItem: com.example.app.model.LessonItem) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val delta = GlobalValues.pendingCupDelta
            ?: GlobalValues.pendingBlindingCupDelta
            ?: GlobalValues.pendingExtractionCupDelta
            ?: GlobalValues.pendingBlindingExtractionCupDelta
            ?: GlobalValues.pendingImpactCupDelta
            ?: GlobalValues.pendingBlindingImpactCupDelta
            ?: return

        // Yerel olarak rozetleri önceden hesapla
        val oldScore = GlobalValues.currentLessonOldCupScore ?: 0
        val newScore = oldScore + delta

        val payloadsLocally = when {
            lessonItem.isMultiplication == true && lessonItem.isBlinding == true -> BadgeProgressFirestore.resolveTurtleLevelUpChain(oldScore, newScore)
            lessonItem.isMultiplication == true -> BadgeProgressFirestore.resolveGoatLevelUpChain(oldScore, newScore)
            lessonItem.isExtraction == true && lessonItem.isBlinding == true -> BadgeProgressFirestore.resolveFlyLevelUpChain(oldScore, newScore)
            lessonItem.isExtraction == true -> BadgeProgressFirestore.resolveCrocodileLevelUpChain(oldScore, newScore)
            lessonItem.isBlinding == true -> BadgeProgressFirestore.resolveEagleLevelUpChain(oldScore, newScore)
            else -> BadgeProgressFirestore.resolveDinoLevelUpChain(oldScore, newScore)
        }

        // TasksFragment'in hiç beklemeden anında rozet ekranını (veya panel'i) açması için anında değeri ata.
        GlobalValues.pendingCupBadgePayloads = payloadsLocally

        val updateFn: (Int, ((Int, Int) -> Unit)?) -> Unit = when {
            lessonItem.isMultiplication == true && lessonItem.isBlinding == true -> BlindingImpactCupRepository::updateCupScore
            lessonItem.isMultiplication == true -> ImpactCupRepository::updateCupScore
            lessonItem.isExtraction == true && lessonItem.isBlinding == true -> BlindingExtractionCupRepository::updateCupScore
            lessonItem.isExtraction == true -> ExtractionCupRepository::updateCupScore
            lessonItem.isBlinding == true -> BlindingAdditionCupRepository::updateCupScore
            else -> AbacusCupRepository::updateCupScore
        }

        // Firestore güncellemesini arka planda sessizce yap
        updateFn(delta) { _, calculatedNewScore ->
            when {
                lessonItem.isMultiplication == true && lessonItem.isBlinding == true -> {
                    BadgeProgressFirestore.syncTurtleProgressAndDetectLevelUp(uid, calculatedNewScore) {}
                }
                lessonItem.isMultiplication == true -> {
                    BadgeProgressFirestore.syncGoatProgressAndDetectLevelUp(uid, calculatedNewScore) {}
                }
                lessonItem.isExtraction == true && lessonItem.isBlinding == true -> {
                    BadgeProgressFirestore.syncFlyProgressAndDetectLevelUp(uid, calculatedNewScore) {}
                }
                lessonItem.isExtraction == true -> {
                    BadgeProgressFirestore.syncCrocodileProgressAndDetectLevelUp(uid, calculatedNewScore) {}
                }
                lessonItem.isBlinding == true -> {
                    BadgeProgressFirestore.syncEagleProgressAndDetectLevelUp(uid, calculatedNewScore) {}
                }
                else -> {
                    BadgeProgressFirestore.syncDinoProgressAndDetectLevelUp(uid, calculatedNewScore) {}
                }
            }
        }
    }
}
