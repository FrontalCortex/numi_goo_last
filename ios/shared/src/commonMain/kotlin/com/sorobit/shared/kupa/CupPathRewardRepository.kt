// Kaynak (Android): app/src/main/java/com/example/app/CupPathRewardRepository.kt (YENİ — kupa yolu sandıkları /
// Trophy Road) ve CupPathRoadFragment.kt ile TasksFragment.kt'deki kupa yolu ödül çubuğunun saf kararları.
// Taşınmayanlar: Firestore okumaları (fetchState/fetchStates), görünüm, animasyon.
// Farklar: NewChestFragment.ChestRarity -> [ChestRarity] (oyun paketi); R.string.x -> Türkçe metin; RtlSupport.ltr
// kaldırıldı (iOS tarafı gerekirse yalıtır).
//
// Kupa yolu sandıkları: kullanıcı kupa biriktirdikçe belirli eşiklerde sandık kazanır. Burada yalnızca OKUMA
// mantığı var; sandığın verilmesi ve defterin (`cupPathRewards`) ilerletilmesi sunucuda (`claimCupPathChest`,
// bkz. ServerRewards.claimCupPathChest) tek transaction'da olur. Eşik matematiği sunucudakiyle birebir aynı olmalı.
package com.sorobit.shared.kupa

import com.sorobit.shared.oyun.ChestRarity

object CupPathRewardRepository {

    /** Kupa yollarının başlangıç puanı. Altı repository'nin DEFAULT_CUP_SCORE'u ile aynı. */
    const val START = 200

    /** Her kaç kupada bir sandık. */
    const val STEP = 100

    /**
     * Yolun son eşiği. Buradan sonra kupa artmaya devam eder ama sandık verilmez.
     * Sunucudaki `CUP_PATH_MAX` ile birebir aynı olmak zorunda.
     */
    const val MAX = 10_000

    /**
     * Bir eşiğin sandık nadirliği (yalnızca GÖRÜNTÜ — ödülü sunucu belirler, `cupPathChestRarity`):
     *   1000'in katları -> DESTANSI (EPIC); 500'ün katları -> ENDER (RARE); diğerleri -> SIRADAN (COMMON).
     * Sıra önemli: 1000 hem 500'ün hem 1000'in katı, destansı kazanmalı.
     */
    fun rarityOf(milestone: Int): ChestRarity = when {
        milestone % 1000 == 0 -> ChestRarity.EPIC
        milestone % 500 == 0 -> ChestRarity.RARE
        else -> ChestRarity.COMMON
    }

    /** Kupa yolu ekranında ileriye doğru kaç eşik gösterilsin. */
    const val FUTURE_MILESTONES = 20

    /** Listenin üst sınırı — bkz. [CupPathState.milestones]. */
    const val MAX_MILESTONES = 60

    // Firestore alan adları — sunucudaki CUP_PATH_FIELDS ile birebir aynı olmalı.
    const val FIELD_ADDITION = "addition_abacus_cup"
    const val FIELD_EXTRACTION = "extraction_abacus_cup"
    const val FIELD_IMPACT = "impact_abacus_cup"
    const val FIELD_BLINDING_ADDITION = "blinding_addition_abacus_cup"
    const val FIELD_BLINDING_EXTRACTION = "blinding_extraction_abacus_cup"
    const val FIELD_BLINDING_IMPACT = "blinding_impact_abacus_cup"

    val FIELDS: List<String> = listOf(
        FIELD_ADDITION,
        FIELD_EXTRACTION,
        FIELD_IMPACT,
        FIELD_BLINDING_ADDITION,
        FIELD_BLINDING_EXTRACTION,
        FIELD_BLINDING_IMPACT,
    )

    const val COL_USERS = "users"
    const val COL_CUP = "cupWayProgress"
    const val DOC_CUP = "progress"
    const val COL_LEDGER = "cupPathRewards"
    const val DOC_LEDGER = "progress"
    const val F_LAST_CLAIMED = "lastClaimed"
    const val F_CLAIMED = "claimed"

    /** `users/{uid}/cupPathRewards/progress` defter dokümanının yolu (istemciye yazmaya kapalı). */
    fun ledgerDocumentPath(uid: String): String = "$COL_USERS/$uid/$COL_LEDGER/$DOC_LEDGER"

    /** Tek bir kupa yolunun ödül durumu. */
    data class CupPathState(
        val cupField: String,
        /** Kullanıcının o yoldaki güncel kupa puanı. */
        val cupScore: Int,
        /** Bu değere kadarki BÜTÜN eşikler alınmış (su seviyesi); hiç alınmadıysa [START]. */
        val lastClaimed: Int,
        /**
         * Su seviyesinin üstünde, tek tek alınmış eşikler (biriken sandıklar sırayla alınmak zorunda değil).
         * Aradaki boşluk kapanınca sunucu seviyeyi yükseltip listeyi boşaltır.
         */
        val claimedAbove: Set<Int> = emptySet(),
    ) {
        /** Bir sonraki sandığın kupa eşiği. Başlangıç 200 olduğu için ilk eşik 300'dür. */
        val nextMilestone: Int
            get() = (waterLevel + STEP).coerceAtMost(MAX)

        /** Su seviyesi: bu değere kadarki bütün eşikler alınmış. */
        private val waterLevel: Int get() = if (lastClaimed > START) lastClaimed else START

        /** Yolun sonuna gelindi mi — son eşik ([MAX]) de alınmış. */
        val finished: Boolean get() = waterLevel >= MAX

        /** Sandık hak edildi mi (alınmayı bekliyor mu). */
        val claimable: Boolean get() = !finished && cupScore >= nextMilestone

        /** O eşiğin ödülü alınmış mı. */
        fun isClaimed(milestone: Int): Boolean =
            milestone <= lastClaimed || milestone in claimedAbove

        /** Bu eşiğin başladığı kupa puanı — ilerleme çubuğunun sol ucu. */
        val milestoneStart: Int get() = nextMilestone - STEP

        /**
         * Çubuğun doluluğu, 0f–1f. Kupa puanı düşebilir; çubuk o zaman geriler (kasıtlı). Alınmış ödüller geri
         * alınmaz.
         */
        val fraction: Float
            get() {
                if (cupScore <= milestoneStart) return 0f
                if (cupScore >= nextMilestone) return 1f
                return (cupScore - milestoneStart).toFloat() / STEP.toFloat()
            }

        /** Alınmayı bekleyen sandık sayısı (sunucu her çağrıda yalnızca sıradaki eşiği verdiği için birden fazla olabilir). */
        val pendingChests: Int
            get() {
                var count = 0
                var value = START + STEP
                val ceiling = minOf(cupScore, MAX)
                while (value <= ceiling) {
                    if (!isClaimed(value)) count++
                    value += STEP
                }
                return count
            }

        /**
         * Çubuğun üstünde yazan metin, ör. "250 / 300". Sandık hak edildiğinde sayı yazılmaz ("350 / 300" hata gibi
         * görünürdü).
         */
        val label: String
            get() = when {
                finished -> TEXT_FINISHED
                claimable -> TEXT_CHEST_READY
                else -> "$cupScore / $nextMilestone"
            }

        /**
         * Kupa yolu ekranında gösterilecek eşikler: geçilmiş olanlar ve ileriye doğru [future] tane. Liste her zaman
         * ilk taştan (300) başlayıp [STEP]'er artar; uzunluk [MAX_MILESTONES] ile sınırlı, dolunca BAŞTAN kırpılır.
         */
        fun milestones(future: Int = FUTURE_MILESTONES): List<Milestone> {
            // [MAX] son eşik: listenin ötesine taş konmaz.
            val end = (maxOf(nextMilestone, cupScore) + future * STEP).coerceAtMost(MAX)
            val all = ArrayList<Milestone>()
            var value = START + STEP
            while (value <= end) {
                val status = when {
                    isClaimed(value) -> MilestoneStatus.CLAIMED
                    value <= cupScore -> MilestoneStatus.CLAIMABLE
                    else -> MilestoneStatus.LOCKED
                }
                all.add(Milestone(value, status))
                value += STEP
            }
            if (all.size <= MAX_MILESTONES) return all
            return ArrayList(all.subList(all.size - MAX_MILESTONES, all.size))
        }

        /**
         * Karttaki sandığa dokunuldu (Android `onCupPathChestTapped`): hak edilmişse sıradaki eşik
         * (`claimCupPathChest(cupField, nextMilestone)`) açılır, değilse kupa yolu ekranı. Doğrulamayı sunucu yapar.
         */
        val tapOpensChest: Boolean get() = claimable
    }

    /** Kupa yolu ekranındaki tek bir eşik. */
    data class Milestone(val cupValue: Int, val status: MilestoneStatus)

    /** Bir eşiğin durumu; [CLAIMABLE] birden fazla olabilir (hangisine dokunulsa sıradaki alınır). */
    enum class MilestoneStatus { CLAIMED, CLAIMABLE, LOCKED }

    const val TEXT_FINISHED = "Tamamlandı"
    const val TEXT_CHEST_READY = "Sandık hazır!"
    const val TEXT_CLAIMED_CAPS = "ALINDI"
    const val TEXT_READY_CAPS = "HAZIR"
    const val TEXT_ALREADY_CLAIMED = "Bu sandığı zaten aldın"
    const val TEXT_TITLE_FALLBACK = "Kupa Yolu"

    /** Kilitli eşiğe dokunulunca ("%1$s kupaya ulaşınca açılır"). */
    fun lockedMilestoneText(cupValue: Int): String = "$cupValue kupaya ulaşınca açılır"

    /** Kartların başlıkları (panel_cup_path.xml ile birebir aynı). */
    fun titleOf(cupField: String): String = when (cupField) {
        FIELD_ADDITION -> "Toplama Kupa Yolu"
        FIELD_EXTRACTION -> "Çıkarma Kupa Yolu"
        FIELD_IMPACT -> "Çarpma Kupa Yolu"
        FIELD_BLINDING_ADDITION -> "Toplama Kupa Yolu - Körleme"
        FIELD_BLINDING_EXTRACTION -> "Çıkarma Kupa Yolu - Körleme"
        FIELD_BLINDING_IMPACT -> "Çarpma Kupa Yolu - Körleme"
        else -> TEXT_TITLE_FALLBACK
    }

    /** Okuma başarısızsa / belge yoksa varsayılan durum (puan [START], hiç ödül alınmamış). */
    fun defaultState(cupField: String): CupPathState = CupPathState(cupField, START, START, emptySet())

    /**
     * Android `build`: iki dokümandan (kupa puanları, defter) altı yolun durumu.
     *
     * @param cupDoc `cupWayProgress/progress` verisi (yoksa null)
     * @param ledgerDoc `cupPathRewards/progress` verisi (yoksa null); her alan `{lastClaimed, claimed[]}` haritasıdır.
     */
    fun buildStates(cupDoc: Map<String, Any?>?, ledgerDoc: Map<String, Any?>?): Map<String, CupPathState> =
        FIELDS.associateWith { field ->
            // Kartın kendisi puanı coerceAtLeast(0) ile gösteriyor; çubuktaki sayı ondan farklı çıkmasın.
            val score = ((cupDoc?.get(field) as? Number)?.toInt() ?: START).coerceAtLeast(0)
            val entry = ledgerDoc?.get(field) as? Map<*, *>
            val claimed = (entry?.get(F_LAST_CLAIMED) as? Number)?.toInt() ?: START
            val above = (entry?.get(F_CLAIMED) as? List<*>)
                ?.mapNotNull { (it as? Number)?.toInt() }
                ?.toSet()
                .orEmpty()
            CupPathState(
                cupField = field,
                cupScore = score,
                lastClaimed = claimed,
                claimedAbove = above,
            )
        }
}
