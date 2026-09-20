package com.example.app

import com.google.firebase.auth.FirebaseAuth

object BadgePrecalcHelper {
    /**
     * Kupa farkını yazar ve rozet kademelerini hesaplar. [BlindingLessonFragment] cevap
     * verilir verilmez çağırır; kullanıcı Görevler'e dönene kadar iş bitmiş oluyor.
     *
     * ## Kademeler neden yerel olarak hesaplanmıyor
     * Rozet ilerlemesi kupa puanının kendisi değil, kupanın GÖRDÜĞÜ EN YÜKSEK değer
     * (`userDinoProgress` vb.) — düşmüyor. Eskiden liste "eski puan → yeni puan" ile yerel
     * hesaplanıyordu; 500'e çıkıp 480'e düşen, sonra yine 500'e çıkan kullanıcı aynı rozeti
     * ikinci kez kazanmış gibi kutlama görüyordu. Kademeleri artık o en yüksek değeri
     * transaction içinde okuyan senkron fonksiyonu üretiyor, yani kutlama gerçekten yeni bir
     * kademeye karşılık geliyor.
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

        // Önceki turdan kalmış bir liste varsa temizleniyor: ağ çok yavaşsa TasksFragment
        // beklemekten vazgeçip geçiyor ve liste sahipsiz kalabiliyor. Her tur buradan
        // temiz başlıyor, böylece eski bir kutlama yanlış derse yapışmıyor.
        GlobalValues.pendingCupBadgePayloads = null

        val updateFn: (Int, ((Int, Int) -> Unit)?) -> Unit = when {
            lessonItem.isMultiplication == true && lessonItem.isBlinding == true -> BlindingImpactCupRepository::updateCupScore
            lessonItem.isMultiplication == true -> ImpactCupRepository::updateCupScore
            lessonItem.isExtraction == true && lessonItem.isBlinding == true -> BlindingExtractionCupRepository::updateCupScore
            lessonItem.isExtraction == true -> ExtractionCupRepository::updateCupScore
            lessonItem.isBlinding == true -> BlindingAdditionCupRepository::updateCupScore
            else -> AbacusCupRepository::updateCupScore
        }

        // Kutlamayı tetikleyecek kademe listesi buradan geliyor. Ders biter bitmez
        // başlatıldığı için kullanıcı Görevler'e döndüğünde cevap çoktan gelmiş oluyor;
        // gelmemişse TasksFragment kısa bir süre bekliyor.
        val publish: (List<BadgeLevelUpPayload>) -> Unit = { payloads ->
            GlobalValues.pendingCupBadgePayloads = payloads
        }

        // Firestore güncellemesini arka planda sessizce yap
        updateFn(delta) { _, calculatedNewScore ->
            when {
                lessonItem.isMultiplication == true && lessonItem.isBlinding == true -> {
                    BadgeProgressFirestore.syncTurtleProgressAndDetectLevelUp(uid, calculatedNewScore, publish)
                }
                lessonItem.isMultiplication == true -> {
                    BadgeProgressFirestore.syncGoatProgressAndDetectLevelUp(uid, calculatedNewScore, publish)
                }
                lessonItem.isExtraction == true && lessonItem.isBlinding == true -> {
                    BadgeProgressFirestore.syncFlyProgressAndDetectLevelUp(uid, calculatedNewScore, publish)
                }
                lessonItem.isExtraction == true -> {
                    BadgeProgressFirestore.syncCrocodileProgressAndDetectLevelUp(uid, calculatedNewScore, publish)
                }
                lessonItem.isBlinding == true -> {
                    BadgeProgressFirestore.syncEagleProgressAndDetectLevelUp(uid, calculatedNewScore, publish)
                }
                else -> {
                    BadgeProgressFirestore.syncDinoProgressAndDetectLevelUp(uid, calculatedNewScore, publish)
                }
            }
        }
    }
}
