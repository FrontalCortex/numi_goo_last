// Güncel Android'e (claude/soban-gamified-education-prb70m) eşitleme sırasında eklenen kupa kurallarının testleri:
// CupScoreService (submitCupResult), CupPathRewardRepository (kupa yolu sandıkları), kart kilidi, rozet eşiği tablosu.
// Beklenen değerler Android kaynak kodundan elle türetilmiştir.
package com.sorobit.shared.kupa

import com.sorobit.shared.kupa.CupPathRewardRepository.CupPathState
import com.sorobit.shared.kupa.CupPathRewardRepository.MilestoneStatus
import com.sorobit.shared.kupa.rozet.BadgeAnimMode
import com.sorobit.shared.kupa.rozet.BadgeKind
import com.sorobit.shared.kupa.rozet.BadgeLevelUpPayload
import com.sorobit.shared.kupa.rozet.BadgeProgressEngine
import com.sorobit.shared.kupa.rozet.BadgeProgressFirestore
import com.sorobit.shared.oyun.ChestRarity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CupYeniKurallarTest {

    private val f = CupPathRewardRepository.FIELD_ADDITION

    // ---------------------------------------------------------------- CupScoreService

    @Test
    fun submitPayloadVeZorlukKirpma() {
        assertEquals(
            mapOf<String, Any>(
                "cupField" to "addition_abacus_cup", "won" to true, "difficultyLevel" to 2,
                "isMultiplication" to false, "dayId" to "2026-11-27",
            ),
            CupScoreService.buildSubmitPayload("addition_abacus_cup", true, 2, false, "2026-11-27"),
        )
        assertEquals(4, CupScoreService.buildSubmitPayload("x", false, 9, true, "d")["difficultyLevel"])
        assertEquals(0, CupScoreService.buildSubmitPayload("x", false, -3, true, "d")["difficultyLevel"])
        // Değişim miktarı GÖNDERİLMEZ
        assertFalse(CupScoreService.buildSubmitPayload("x", true, 1, false, "d").containsKey("delta"))
        assertEquals("submitCupResult", CupScoreService.CF_SUBMIT_CUP_RESULT)
    }

    @Test
    fun submitSonucuAyristirma() {
        assertEquals(
            CupScoreService.CupScoreResult(200, 224),
            CupScoreService.parseResult(mapOf("oldScore" to 200, "newScore" to 224.0)),
        )
        assertNull(CupScoreService.parseResult(mapOf("oldScore" to 200)))
        assertNull(CupScoreService.parseResult(mapOf("oldScore" to "200", "newScore" to 1)))
        assertNull(CupScoreService.parseResult(null))
    }

    @Test
    fun cupFieldFor() {
        val c = CupScoreService
        assertEquals("blinding_impact_abacus_cup", c.cupFieldFor(true, null, true))
        assertEquals("impact_abacus_cup", c.cupFieldFor(true, true, false))
        assertEquals("blinding_extraction_abacus_cup", c.cupFieldFor(null, true, true))
        assertEquals("extraction_abacus_cup", c.cupFieldFor(false, true, null))
        assertEquals("blinding_addition_abacus_cup", c.cupFieldFor(null, null, true))
        assertEquals("addition_abacus_cup", c.cupFieldFor(null, null, null))
        // BadgePrecalcHelper.resolveCupPathKind ile aynı eşleşme
        for (m in listOf(null, true)) for (e in listOf(null, true)) for (b in listOf(null, true)) {
            assertEquals(BadgePrecalcHelper.resolveCupPathKind(m, e, b).repository.FIELD, c.cupFieldFor(m, e, b))
        }
    }

    @Test
    fun badgePrecalcSubmitYuku() {
        assertEquals(
            mapOf<String, Any>(
                "cupField" to "blinding_impact_abacus_cup", "won" to false, "difficultyLevel" to 3,
                "isMultiplication" to true, "dayId" to "2026-11-27",
            ),
            BadgePrecalcHelper.buildCupSubmitPayload(-20, true, null, true, 3, "2026-11-27"),
        )
        // Zorluk yoksa 0; delta 0 kazanç sayılmaz
        val p = BadgePrecalcHelper.buildCupSubmitPayload(0, null, null, null, null, "d")
        assertEquals(false, p["won"])
        assertEquals(0, p["difficultyLevel"])
        assertEquals("addition_abacus_cup", p["cupField"])
    }

    // ---------------------------------------------------------------- CupPathRewardRepository

    @Test
    fun sabitlerVeNadirlik() {
        assertEquals(200, CupPathRewardRepository.START)
        assertEquals(100, CupPathRewardRepository.STEP)
        assertEquals(10_000, CupPathRewardRepository.MAX)
        assertEquals(6, CupPathRewardRepository.FIELDS.size)
        assertEquals(CupPathRewardRepository.FIELDS, CupPathLogic.CARDS.map { it.field })
        assertEquals(ChestRarity.COMMON, CupPathRewardRepository.rarityOf(300))
        assertEquals(ChestRarity.COMMON, CupPathRewardRepository.rarityOf(400))
        assertEquals(ChestRarity.RARE, CupPathRewardRepository.rarityOf(500))
        assertEquals(ChestRarity.RARE, CupPathRewardRepository.rarityOf(1500))
        assertEquals(ChestRarity.EPIC, CupPathRewardRepository.rarityOf(1000))
        assertEquals(ChestRarity.EPIC, CupPathRewardRepository.rarityOf(10_000))
    }

    @Test
    fun durumHesaplari() {
        val a = CupPathState(f, 250, 200)
        assertEquals(300, a.nextMilestone)
        assertEquals(200, a.milestoneStart)
        assertFalse(a.claimable)
        assertFalse(a.finished)
        assertEquals(0.5f, a.fraction)
        assertEquals("250 / 300", a.label)
        assertEquals(0, a.pendingChests)
        assertFalse(a.tapOpensChest)

        val b = CupPathState(f, 350, 200)
        assertTrue(b.claimable)
        assertEquals(1f, b.fraction)
        assertEquals("Sandık hazır!", b.label)
        assertEquals(1, b.pendingChests)
        assertTrue(b.tapOpensChest)

        // Puan düşerse çubuk geriler (sol uçtan aşağıya inmez)
        assertEquals(0f, CupPathState(f, 150, 200).fraction)

        // Sıra dışı alınmış eşik: su seviyesi 200'de kalır, 500 alınmış sayılır
        val c = CupPathState(f, 500, 200, setOf(500))
        assertEquals(300, c.nextMilestone)
        assertTrue(c.isClaimed(500))
        assertFalse(c.isClaimed(400))
        assertEquals(2, c.pendingChests)

        // Su seviyesi 400: sıradaki 500
        val d = CupPathState(f, 600, 400)
        assertEquals(500, d.nextMilestone)
        assertEquals(400, d.milestoneStart)
        assertTrue(d.isClaimed(400))
        assertEquals(2, d.pendingChests)

        // Yol bitti: sandık verilmez
        val e = CupPathState(f, 20_000, 10_000)
        assertTrue(e.finished)
        assertFalse(e.claimable)
        assertEquals(10_000, e.nextMilestone)
        assertEquals("Tamamlandı", e.label)
    }

    @Test
    fun esikListesi() {
        val a = CupPathState(f, 250, 200).milestones()
        assertEquals(21, a.size)
        assertEquals(300, a.first().cupValue)
        assertEquals(2300, a.last().cupValue)
        assertTrue(a.all { it.status == MilestoneStatus.LOCKED })

        val b = CupPathState(f, 450, 300).milestones()
        assertEquals(22, b.size)
        assertEquals(
            listOf(MilestoneStatus.CLAIMED, MilestoneStatus.CLAIMABLE, MilestoneStatus.LOCKED),
            b.take(3).map { it.status }.let { listOf(it[0], it[1], it[2]) },
        )

        // Uzun liste BAŞTAN kırpılır; son eşik MAX'ı aşmaz
        val c = CupPathState(f, 9_000, 200).milestones()
        assertEquals(60, c.size)
        assertEquals(4100, c.first().cupValue)
        assertEquals(10_000, c.last().cupValue)
    }

    @Test
    fun belgedenDurumlar() {
        val s = CupPathRewardRepository.buildStates(
            cupDoc = mapOf(f to 450L, CupPathRewardRepository.FIELD_EXTRACTION to -5),
            ledgerDoc = mapOf(f to mapOf("lastClaimed" to 300L, "claimed" to listOf(500L, "x"))),
        )
        assertEquals(CupPathState(f, 450, 300, setOf(500)), s[f])
        // Negatif puan 0'a çekilir; defter yoksa 200
        assertEquals(CupPathState(CupPathRewardRepository.FIELD_EXTRACTION, 0, 200, emptySet()), s[CupPathRewardRepository.FIELD_EXTRACTION])
        assertEquals(6, s.size)
        // Doküman yoksa varsayılanlar
        val bos = CupPathRewardRepository.buildStates(null, null)
        assertTrue(bos.values.all { it.cupScore == 200 && it.lastClaimed == 200 && it.claimedAbove.isEmpty() })
        assertEquals(CupPathRewardRepository.defaultState(f), bos[f])
        assertEquals("users/u1/cupPathRewards/progress", CupPathRewardRepository.ledgerDocumentPath("u1"))
        assertEquals("Çarpma Kupa Yolu - Körleme", CupPathRewardRepository.titleOf(CupPathRewardRepository.FIELD_BLINDING_IMPACT))
        assertEquals("Kupa Yolu", CupPathRewardRepository.titleOf("bilinmeyen"))
        assertEquals("700 kupaya ulaşınca açılır", CupPathRewardRepository.lockedMilestoneText(700))
    }

    // ---------------------------------------------------------------- CupPathLogic (kart kilidi)

    @Test
    fun kartKilidiOnayliOgretmenVeIpucu() {
        val turler = listOf(0, 1, 2)
        // Son sandık bitmemiş: normalde kapalı, onaylı öğretmende açık
        assertFalse(CupPathLogic.isCardActive(turler, listOf(true, true, false), false))
        assertTrue(CupPathLogic.isCardActive(turler, listOf(true, true, false), true))
        assertTrue(CupPathLogic.isCardActive(turler, listOf(false, false, true), false))
        assertEquals(
            "Açmak için \"Abaküsün Temeli ve Toplama\" bölümünün son ünite maratonunu bitir.",
            CupPathLogic.lockedCardHint(1),
        )
        assertEquals(
            "Açmak için \"Körleme Çarpma\" bölümünün son ünite maratonunu bitir.",
            CupPathLogic.lockedCardHint(99),
        )
        assertEquals(6, CupPathLogic.CARD_UNLOCK_REQUIREMENTS.size)
        assertEquals(600L, CupPathLogic.PANEL_REOPEN_GUARD_MS)
    }

    // ---------------------------------------------------------------- rozet eşikleri (tek tablo)

    @Test
    fun levelSpecTekKaynak() {
        assertEquals(listOf(3, 5, 10, 15, 20) to 5, BadgeProgressEngine.levelSpec(BadgeKind.GOLF))
        assertEquals(listOf(3, 10, 15, 25, 30) to 10, BadgeProgressEngine.levelSpec(BadgeKind.ROCKET))
        assertEquals(BadgeProgressEngine.levelSpec(BadgeKind.GOLF), BadgeProgressEngine.levelSpec(BadgeAnimMode.GOLF_WITH_BASE))
        assertNull(BadgeProgressEngine.levelSpec(BadgeKind.CUP))
        assertNull(BadgeProgressEngine.levelSpec(BadgeAnimMode.KARATE))
        for (k in BadgeKind.entries) {
            val spec = BadgeProgressEngine.levelSpec(k)
            if (k in listOf(BadgeKind.CUP, BadgeKind.GOLD, BadgeKind.SILVER, BadgeKind.BRONZE, BadgeKind.KARATE)) {
                assertNull(spec)
            } else {
                assertEquals(5, spec!!.first.size)
            }
        }
    }

    @Test
    fun resolveLevelUpPayloadEsikleriTablodanAlir() {
        val g = BadgeAnimMode.GOLF_WITH_BASE
        assertEquals(BadgeLevelUpPayload(g, 2, 3, 3), BadgeProgressFirestore.resolveLevelUpPayload(g, 2, 3))
        assertNull(BadgeProgressFirestore.resolveLevelUpPayload(g, 3, 4))
        assertEquals(BadgeLevelUpPayload(g, 19, 20, 20), BadgeProgressFirestore.resolveLevelUpPayload(g, 19, 20))
        // Liste sonrası kutlanmaz
        assertNull(BadgeProgressFirestore.resolveLevelUpPayload(g, 20, 25))
        // Kademesi olmayan rozet
        assertNull(BadgeProgressFirestore.resolveLevelUpPayload(BadgeAnimMode.CUP_GOOGLE, 0, 5))
    }
}
