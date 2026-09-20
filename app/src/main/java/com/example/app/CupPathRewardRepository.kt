package com.example.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Kupa yolu sandıkları (Trophy Road).
 *
 * Kullanıcı kupa biriktirdikçe belirli eşiklerde sandık kazanır. Altı kupa yolunun her
 * birinin kendi kupa puanı ve kendi defteri var.
 *
 * ## Burada yalnızca OKUMA var
 * Sandığın verilmesi ve defterin ilerletilmesi sunucuda, `claimCupPathChest` içinde ve tek
 * bir transaction'da oluyor (bkz. functions/index.js). Defter (`cupPathRewards`) istemciye
 * yazmaya kapalı; burada sadece ilerleme çubuğunu çizmek için okunuyor.
 *
 * Eşik matematiği sunucudakiyle birebir aynı olmak zorunda: ekranda "hak ettin" yazıp
 * sunucunun reddetmesi ya da tersi, kullanıcı için anlaşılmaz olur.
 */
object CupPathRewardRepository {

    /** Kupa yollarının başlangıç puanı. Altı repository'nin DEFAULT_CUP_SCORE'u ile aynı. */
    const val START = 200

    /** Her kaç kupada bir sandık. */
    const val STEP = 100

    // Firestore alan adları — sunucudaki CUP_PATH_FIELDS ile birebir aynı olmalı.
    const val FIELD_ADDITION = "addition_abacus_cup"
    const val FIELD_EXTRACTION = "extraction_abacus_cup"
    const val FIELD_IMPACT = "impact_abacus_cup"
    const val FIELD_BLINDING_ADDITION = "blinding_addition_abacus_cup"
    const val FIELD_BLINDING_EXTRACTION = "blinding_extraction_abacus_cup"
    const val FIELD_BLINDING_IMPACT = "blinding_impact_abacus_cup"

    val FIELDS = listOf(
        FIELD_ADDITION,
        FIELD_EXTRACTION,
        FIELD_IMPACT,
        FIELD_BLINDING_ADDITION,
        FIELD_BLINDING_EXTRACTION,
        FIELD_BLINDING_IMPACT,
    )

    private const val COL_USERS = "users"
    private const val COL_CUP = "cupWayProgress"
    private const val DOC_CUP = "progress"
    private const val COL_LEDGER = "cupPathRewards"
    private const val DOC_LEDGER = "progress"
    private const val F_LAST_CLAIMED = "lastClaimed"

    /** Tek bir kupa yolunun ödül durumu. */
    data class CupPathState(
        val cupField: String,
        /** Kullanıcının o yoldaki güncel kupa puanı. */
        val cupScore: Int,
        /** O yolda ödülü alınmış son eşik; hiç alınmadıysa [START]. */
        val lastClaimed: Int,
    ) {
        /**
         * Bir sonraki sandığın kupa eşiği.
         *
         * Başlangıç 200 olduğu için ilk eşik 300'dür — 100 ve 200 hiç oynamadan geçilmiş
         * sayılırdı ve ilk açılışta iki bedava sandık demek olurdu.
         */
        val nextMilestone: Int
            get() = (if (lastClaimed > START) lastClaimed else START) + STEP

        /** Sandık hak edildi mi (alınmayı bekliyor mu). */
        val claimable: Boolean get() = cupScore >= nextMilestone

        /** Bu eşiğin başladığı kupa puanı — ilerleme çubuğunun sol ucu. */
        val milestoneStart: Int get() = nextMilestone - STEP

        /**
         * Çubuğun doluluğu, 0f–1f.
         *
         * Kupa puanı DÜŞEBİLİR (yanlış cevapta kaybediliyor). Çubuk o zaman geriler; bu
         * kasıtlı, kartta gösterilen puanla tutarlı olsun diye. Alınmış ödüller geri
         * alınmıyor, defter ilerlemiş olarak kalıyor.
         */
        val fraction: Float
            get() {
                if (cupScore <= milestoneStart) return 0f
                if (cupScore >= nextMilestone) return 1f
                return (cupScore - milestoneStart).toFloat() / STEP.toFloat()
            }

        /** Çubuğun üstünde yazan metin, ör. "250 / 300". */
        val label: String get() = "$cupScore / $nextMilestone"
    }

    /**
     * Altı yolun durumunu tek seferde okur.
     *
     * İki doküman okunuyor, altı değil: kupa puanlarının hepsi tek dokümanda, defter de
     * öyle. Panel her açıldığında çağrıldığı için bu fark önemli.
     *
     * Okuma başarısız olursa varsayılanlarla (puan [START], hiç ödül alınmamış) dönülür —
     * çubuk boş görünür ama ekran çalışmaya devam eder.
     */
    fun fetchStates(onResult: (Map<String, CupPathState>) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(build(null, null))
            return
        }
        val userRef = FirebaseFirestore.getInstance().collection(COL_USERS).document(uid)
        userRef.collection(COL_CUP).document(DOC_CUP).get()
            .addOnSuccessListener { cupDoc ->
                userRef.collection(COL_LEDGER).document(DOC_LEDGER).get()
                    .addOnSuccessListener { ledgerDoc -> onResult(build(cupDoc, ledgerDoc)) }
                    .addOnFailureListener { onResult(build(cupDoc, null)) }
            }
            .addOnFailureListener { onResult(build(null, null)) }
    }

    private fun build(cupDoc: DocumentSnapshot?, ledgerDoc: DocumentSnapshot?): Map<String, CupPathState> =
        FIELDS.associateWith { field ->
            // Kartın kendisi puanı `coerceAtLeast(0)` ile gösteriyor; çubuktaki sayı ondan
            // farklı çıkmasın diye burada da aynısı yapılıyor.
            val score = ((cupDoc?.get(field) as? Number)?.toInt() ?: START).coerceAtLeast(0)
            val entry = ledgerDoc?.get(field) as? Map<*, *>
            val claimed = (entry?.get(F_LAST_CLAIMED) as? Number)?.toInt() ?: START
            CupPathState(cupField = field, cupScore = score, lastClaimed = claimed)
        }
}
