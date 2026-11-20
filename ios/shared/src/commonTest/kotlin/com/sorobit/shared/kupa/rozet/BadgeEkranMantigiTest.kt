// Android BadgeFragment.kt ve BadgeDetailFragment.kt içinden ayıklanan görünümden bağımsız mantığın testleri.
package com.sorobit.shared.kupa.rozet

import com.sorobit.shared.kupa.rozet.BadgeFragmentLogic.CelebrationQueueItem
import com.sorobit.shared.kupa.rozet.BadgeFragmentLogic.LevelUpAnimConfig
import com.sorobit.shared.kupa.rozet.BadgeFragmentLogic.PieceCountBadge
import com.sorobit.shared.kupa.rozet.BadgeFragmentLogic.ProgressUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BadgeEkranMantigiTest {

    private val B = BadgeFragmentLogic
    private val D = BadgeDetailLogic

    // ---------------------------------------------------------------- BadgeFragment: metinler

    @Test
    fun metinTablolariTumModlariKapsar() {
        assertEquals(BadgeAnimMode.entries.toSet(), B.unlockedDetailTextOverrides.keys)
        assertEquals(BadgeAnimMode.entries.toSet(), B.lockedDetailTextOverrides.keys)
        assertEquals("x dersi hatasız tamamlayarak Kusursuz Odak rozetini kazandın.", B.unlockedDetailTemplate(BadgeAnimMode.DAILY_ONLY))
        assertEquals("Ünite maratonunda 1. olarak Altın Madalya kazandın.", B.unlockedDetailTemplate(BadgeAnimMode.GOLD_MEDAL))
        assertEquals("Bu rozeti kazanmak için 5 kere öğretmene danış.", B.lockedDetailText(BadgeAnimMode.GOLF_WITH_BASE))
        assertEquals(
            "Bu kupayı kazanmak için ünite maratonunda ilk 100'e girerek başarı elde et.",
            B.lockedDetailText(BadgeAnimMode.CUP_GOOGLE),
        )
        // Android metinlerindeki tutarsızlıklar aynen korunur
        assertTrue(B.unlockedDetailTemplate(BadgeAnimMode.VOLCANO).contains("Kasırga rozetini"))
        assertTrue(B.unlockedDetailTemplate(BadgeAnimMode.GOAT).contains("Keço"))
        assertTrue(B.unlockedDetailTemplate(BadgeAnimMode.EAGLE).contains("Karti"))
    }

    @Test
    fun formatUnlockedDetailText() {
        BadgeProgressRepository.update(
            UserBadgeProgress(
                userGolfProgress = 20,
                userKarateProgress = 2,
                cupPiece = listOf(CupPieceRow("A", 12, 1), CupPieceRow("B", 7, 2)),
            ),
        )
        assertEquals(
            "20 kere eğitmene danışarak Nokta Atışı rozetini kazandın.",
            B.badgeDetailText(BadgeAnimMode.GOLF_WITH_BASE),
        )
        assertEquals(
            "Ünite Maratonunda 7. olarak Elit Seviye kupasını kazandın.",
            B.badgeDetailText(BadgeAnimMode.CUP_GOOGLE),
        )
        assertEquals(
            "2 Ünite Maratonunu 3 yıldız ile tamamlayarak Siyah Kuşak rozetini kazandın.",
            B.badgeDetailText(BadgeAnimMode.KARATE),
        )
        // Kutlama akışı: hedef değeri kullanılır
        assertEquals(
            "5 kere eğitmene danışarak Nokta Atışı rozetini kazandın.",
            B.badgeDetailText(BadgeAnimMode.GOLF_WITH_BASE, unlocked = true, celebrateTarget = 5),
        )
        // Madalyalarda dinamik değer yok
        assertEquals(
            "Ünite maratonunda 2. olarak Gümüş Madalya kazandın.",
            B.badgeDetailText(BadgeAnimMode.SILVER_MEDAL, celebrateTarget = 9),
        )
        // Kilitli metin
        assertEquals(
            "Bu rozeti kazanmak için Toplama Kupa Yolunda 500 kupaya ulaş.",
            B.badgeDetailText(BadgeAnimMode.DINO, unlocked = false),
        )
        // Kupa yoksa ilerleme değeri null → şablon aynen kalır
        BadgeProgressRepository.update(UserBadgeProgress())
        assertEquals(
            "Ünite Maratonunda x. olarak Elit Seviye kupasını kazandın.",
            B.badgeDetailText(BadgeAnimMode.CUP_GOOGLE),
        )
        assertEquals("a5b5", B.formatUnlockedDetailText(BadgeAnimMode.DINO, "axbx", celebrateTarget = 5))
        assertEquals("axbx", B.formatUnlockedDetailText(BadgeAnimMode.GOLD_MEDAL, "axbx", celebrateTarget = 5))
    }

    // ---------------------------------------------------------------- BadgeFragment: kuyruk

    @Test
    fun parseQueueItem() {
        val q = listOf("DINO|400|500|500", "CUP_GOOGLE|3|3|3", "YOK|1|2|3", "KARATE|a|", "")
        assertEquals(CelebrationQueueItem(BadgeAnimMode.DINO, 400, 500, 500), B.parseQueueItem(q))
        assertEquals(CelebrationQueueItem(BadgeAnimMode.CUP_GOOGLE, 3, 3, 3), B.parseQueueItem(q, 1))
        // Bilinmeyen mod → DAILY_ONLY
        assertEquals(CelebrationQueueItem(BadgeAnimMode.DAILY_ONLY, 1, 2, 3), B.parseQueueItem(q, 2))
        // Sayı olmayan/eksik parçalar → 0
        assertEquals(CelebrationQueueItem(BadgeAnimMode.KARATE, 0, 0, 0), B.parseQueueItem(q, 3))
        assertEquals(CelebrationQueueItem(BadgeAnimMode.DAILY_ONLY, 0, 0, 0), B.parseQueueItem(q, 4))
        // İndeks liste dışında
        assertEquals(CelebrationQueueItem(BadgeAnimMode.DAILY_ONLY, 0, 0, 0), B.parseQueueItem(q, 9))
        assertEquals(CelebrationQueueItem(BadgeAnimMode.DAILY_ONLY, 0, 0, 0), B.parseQueueItem(emptyList()))
        // payloadToQueueItem ile gidiş-dönüş
        val p = BadgeLevelUpPayload(BadgeAnimMode.ROCKET_WITH_BASE, 3, 10, 10)
        assertEquals(
            CelebrationQueueItem(p.mode, p.fromProgress, p.toProgress, p.reachedTarget),
            B.parseQueueItem(listOf(BadgeProgressFirestore.payloadToQueueItem(p))),
        )

        assertFalse(B.hasNextCelebrationInQueue(null, 0))
        assertFalse(B.hasNextCelebrationInQueue(1, 0))
        assertTrue(B.hasNextCelebrationInQueue(2, 0))
        assertFalse(B.hasNextCelebrationInQueue(2, 1))
    }

    @Test
    fun defaultRewardAmountForMode() {
        val beklenen = mapOf(
            BadgeAnimMode.DAILY_ONLY to 50, BadgeAnimMode.ROCKET_WITH_BASE to 100, BadgeAnimMode.BOWLING_WITH_BASE to 150,
            BadgeAnimMode.FISHING_WITH_BASE to 3, BadgeAnimMode.GOLF_WITH_BASE to 225, BadgeAnimMode.CUP_GOOGLE to 87,
            BadgeAnimMode.KARATE to 1, BadgeAnimMode.TORNADO to 1, BadgeAnimMode.VOLCANO to 1, BadgeAnimMode.DINO to 1,
            BadgeAnimMode.CROCODILE to 1, BadgeAnimMode.GOAT to 1, BadgeAnimMode.EAGLE to 1, BadgeAnimMode.FLY to 1,
            BadgeAnimMode.TURTLE to 0, BadgeAnimMode.GOLD_MEDAL to null, BadgeAnimMode.SILVER_MEDAL to null,
            BadgeAnimMode.BRONZE_MEDAL to null,
        )
        assertEquals(18, beklenen.size)
        for ((m, v) in beklenen) assertEquals(v, B.defaultRewardAmountForMode(m), m.name)
    }

    // ---------------------------------------------------------------- BadgeFragment: parça paneli

    private val parcali = UserBadgeProgress(
        goldMedalPiece = listOf(MedalPieceRow("A", 1), MedalPieceRow("B", 2), MedalPieceRow("C", 2)),
        silverMedalPiece = listOf(MedalPieceRow("D", 2)),
        cupPiece = listOf(CupPieceRow("E", 9, 1), CupPieceRow("F", 4, 2)),
    )

    @Test
    fun pieceCountBadge() {
        fun rozet(m: BadgeAnimMode, unlocked: Boolean = true, kutlama: Boolean = false, sezon: Int? = null) =
            B.pieceCountBadge(m, unlocked, parcali, kutlama, sezon)
        assertEquals(PieceCountBadge(3, false), rozet(BadgeAnimMode.GOLD_MEDAL))
        assertEquals("3x", rozet(BadgeAnimMode.GOLD_MEDAL)!!.text)
        assertEquals(PieceCountBadge(1, false), rozet(BadgeAnimMode.SILVER_MEDAL))
        assertEquals(PieceCountBadge(2, true), rozet(BadgeAnimMode.CUP_GOOGLE))
        // Sayı 0 → gizli
        assertNull(rozet(BadgeAnimMode.BRONZE_MEDAL))
        // Kilitli ya da parça modu değil → gizli
        assertNull(rozet(BadgeAnimMode.GOLD_MEDAL, unlocked = false))
        assertNull(rozet(BadgeAnimMode.DINO))
        // Kutlama akışında sezon filtresi yoksa gizli; varsa yalnızca o sezon sayılır
        assertNull(rozet(BadgeAnimMode.GOLD_MEDAL, kutlama = true))
        assertEquals(PieceCountBadge(2, false), rozet(BadgeAnimMode.GOLD_MEDAL, kutlama = true, sezon = 2))
        assertEquals(PieceCountBadge(1, true), rozet(BadgeAnimMode.CUP_GOOGLE, kutlama = true, sezon = 1))
        assertNull(rozet(BadgeAnimMode.SILVER_MEDAL, kutlama = true, sezon = 1))
    }

    @Test
    fun parcaSatirlariVeAnahtar() {
        assertEquals("GOLD_MEDAL|false", B.piecePanelKey(BadgeAnimMode.GOLD_MEDAL, false))
        assertEquals("CUP_GOOGLE|true|s=4", B.piecePanelKey(BadgeAnimMode.CUP_GOOGLE, true, 4))
        assertEquals(parcali.cupPiece, B.cupPieceRows(parcali))
        assertEquals(listOf(CupPieceRow("F", 4, 2)), B.cupPieceRows(parcali, 2))
        assertEquals(parcali.goldMedalPiece, B.medalPieceRows(BadgeAnimMode.GOLD_MEDAL, parcali))
        assertEquals(listOf(MedalPieceRow("B", 2), MedalPieceRow("C", 2)), B.medalPieceRows(BadgeAnimMode.GOLD_MEDAL, parcali, 2))
        assertEquals(emptyList(), B.medalPieceRows(BadgeAnimMode.SILVER_MEDAL, parcali, 1))
        assertEquals(emptyList(), B.medalPieceRows(BadgeAnimMode.BRONZE_MEDAL, parcali))
        assertEquals(emptyList(), B.medalPieceRows(BadgeAnimMode.CUP_GOOGLE, parcali))
        assertEquals("gold_medal_anim.json", B.medalPieceAsset(BadgeAnimMode.GOLD_MEDAL))
        assertEquals("silver_medal_anim.json.json", B.medalPieceAsset(BadgeAnimMode.SILVER_MEDAL))
        assertEquals("bronze_medal_anim.json.json", B.medalPieceAsset(BadgeAnimMode.BRONZE_MEDAL))
        assertNull(B.medalPieceAsset(BadgeAnimMode.CUP_GOOGLE))
        assertEquals(setOf(BadgeAnimMode.GOLD_MEDAL, BadgeAnimMode.SILVER_MEDAL, BadgeAnimMode.BRONZE_MEDAL, BadgeAnimMode.CUP_GOOGLE), B.pieceModes)
    }

    // ---------------------------------------------------------------- BadgeFragment: ilerleme / kutlama

    @Test
    fun ilerlemeCubugu() {
        assertEquals(ProgressUi("2/10", 20), B.lockedProgressUi(2, 10))
        assertEquals(ProgressUi("1/3", 33), B.lockedProgressUi(1, 3))
        assertEquals(ProgressUi("0/1", 0), B.lockedProgressUi(-1, 0))
        assertEquals(ProgressUi("15/10", 100), B.lockedProgressUi(15, 10))
        assertEquals(ProgressUi("4/10", 50), B.celebrationProgressUi(0.5f, 4, 10))
        assertEquals(ProgressUi("10/10", 100), B.celebrationProgressUi(1.7f, 10, 10))
        assertEquals(ProgressUi("0/10", 0), B.celebrationProgressUi(-1f, 0, 10))

        assertEquals(3, B.celebrationAnimatedCurrent(3, 10, 0f))
        assertEquals(6, B.celebrationAnimatedCurrent(3, 10, 0.5f))
        assertEquals(10, B.celebrationAnimatedCurrent(3, 10, 1f))
        assertEquals(10, B.celebrationAnimatedCurrent(3, 10, 2f))
        // from == target: aralık 1 sayılır ama sonuç hedefi aşamaz
        assertEquals(5, B.celebrationAnimatedCurrent(5, 5, 1f))
        assertEquals(3000L, B.CELEBRATION_COUNT_DURATION_MS)
    }

    @Test
    fun levelUpCelebrationPlan() {
        var p = B.levelUpCelebrationPlan(BadgeAnimMode.DAILY_ONLY, 9, 10, 10)
        assertEquals(3 to 10, p.oldShownValue to p.newShownValue)
        assertEquals(9 to 10, p.lockedProgressCurrent to p.lockedProgressTarget)
        assertEquals(BadgeLevelTone.BRONZE to BadgeLevelTone.SILVER, p.previousTone to p.newTone)
        // İlk açılış: önceki ton yok
        p = B.levelUpCelebrationPlan(BadgeAnimMode.DINO, 480, 500, 500)
        assertEquals(500 to 500, p.oldShownValue to p.newShownValue)
        assertEquals(null to BadgeLevelTone.BRONZE, p.previousTone to p.newTone)
        // Karate: değer ilerlemenin kendisi, ton yok
        p = B.levelUpCelebrationPlan(BadgeAnimMode.KARATE, 0, 1, 1)
        assertEquals(0 to 1, p.oldShownValue to p.newShownValue)
        assertEquals(null to null, p.previousTone to p.newTone)
        // Madalya/kupa: kademe tanımı yok → yedek hesap (ilerlemenin kendisi)
        p = B.levelUpCelebrationPlan(BadgeAnimMode.GOLD_MEDAL, 0, 1, 1)
        assertEquals(0 to 1, p.oldShownValue to p.newShownValue)
        p = B.levelUpCelebrationPlan(BadgeAnimMode.CUP_GOOGLE, 7, 7, 7)
        assertEquals(7 to 7, p.oldShownValue to p.newShownValue)
        // Hedef 0 → en az 1; negatif başlangıç → 0
        p = B.levelUpCelebrationPlan(BadgeAnimMode.GOLD_MEDAL, -2, 0, 0)
        assertEquals(0 to 1, p.lockedProgressCurrent to p.lockedProgressTarget)
        assertEquals(0, p.oldShownValue)
    }

    @Test
    fun levelUpAnimConfigVeDigerleri() {
        assertEquals(LevelUpAnimConfig("dart_anim.json", 60f, 0.65f), B.levelUpAnimConfig(BadgeAnimMode.DAILY_ONLY))
        assertEquals(LevelUpAnimConfig("rocket_badge_anim2.json", 60f, 1f), B.levelUpAnimConfig(BadgeAnimMode.ROCKET_WITH_BASE))
        assertEquals(
            LevelUpAnimConfig("bowling_anim.json", 47f, 0.68f, topSpeed = 0.5f, withCircleFrame = true),
            B.levelUpAnimConfig(BadgeAnimMode.BOWLING_WITH_BASE),
        )
        assertEquals(LevelUpAnimConfig("fishing_pole_anim.json", 24f, 0.65f), B.levelUpAnimConfig(BadgeAnimMode.FISHING_WITH_BASE))
        assertEquals(LevelUpAnimConfig("golf_anim.json", 9999f, 0.60f), B.levelUpAnimConfig(BadgeAnimMode.GOLF_WITH_BASE))
        assertEquals(LevelUpAnimConfig("karate_anim.json", 1f, 1f), B.levelUpAnimConfig(BadgeAnimMode.KARATE))
        assertEquals(
            LevelUpAnimConfig("volcano_anim.json", 9999f, 0.65f, withCircleFrame = true, topRepeatCount = 1),
            B.levelUpAnimConfig(BadgeAnimMode.VOLCANO),
        )
        assertEquals(
            LevelUpAnimConfig("dinosaur_anim.json", 9999f, 0.5f, growFromZero = true),
            B.levelUpAnimConfig(BadgeAnimMode.DINO),
        )
        assertEquals(
            LevelUpAnimConfig("goat_anim.json", 9999f, 0.5f, growFromZero = true, topRepeatCount = 1),
            B.levelUpAnimConfig(BadgeAnimMode.GOAT),
        )
        // Tanımsız modlar dart yapılandırmasına düşer
        assertEquals(B.levelUpAnimConfig(BadgeAnimMode.DAILY_ONLY), B.levelUpAnimConfig(BadgeAnimMode.GOLD_MEDAL))
        assertEquals(B.levelUpAnimConfig(BadgeAnimMode.DAILY_ONLY), B.levelUpAnimConfig(BadgeAnimMode.CUP_GOOGLE))

        assertEquals(20, B.resolveShownValueFallback(BadgeAnimMode.DAILY_ONLY, 25))
        assertEquals(3, B.resolveShownValueFallback(BadgeAnimMode.DAILY_ONLY, 0))
        assertEquals(60, B.resolveShownValueFallback(BadgeAnimMode.DAILY_ONLY, 61))
        assertEquals(4, B.resolveShownValueFallback(BadgeAnimMode.SILVER_MEDAL, 4))
        assertEquals(0, B.resolveShownValueFallback(BadgeAnimMode.SILVER_MEDAL, -4))

        val gosteren = BadgeAnimMode.entries.filter { B.shouldShowProgressForMode(it) }.toSet()
        assertEquals(
            BadgeAnimMode.entries.toSet() - setOf(
                BadgeAnimMode.CUP_GOOGLE, BadgeAnimMode.KARATE, BadgeAnimMode.GOLD_MEDAL,
                BadgeAnimMode.SILVER_MEDAL, BadgeAnimMode.BRONZE_MEDAL,
            ),
            gosteren,
        )

        assertEquals(0.5f, B.frameToProgress(60f, 0f, 120f))
        assertEquals(1f, B.frameToProgress(9999f, 0f, 120f))
        assertEquals(0f, B.frameToProgress(-5f, 0f, 120f))
        // Aralık 0 ise 1 kare sayılır (BadgeFragment sürümü)
        assertEquals(1f, B.frameToProgress(5f, 0f, 0f))
        // BadgeDetailFragment sürümü: aralık 0 ise 0
        assertEquals(0f, D.frameToProgress(5f, 0f, 0f))
        assertEquals(0.25f, D.frameToProgress(30f, 0f, 120f))
        assertEquals(1f, D.frameToProgress(9999f, 0f, 120f))
        assertEquals(135f, B.CUP_GOOGLE_HOLD_FRAME)
        assertEquals(40f, B.KARATE_REPLAY_START_FRAME)
    }

    // ---------------------------------------------------------------- BadgeDetailFragment

    @Test
    fun levelSortScore() {
        assertEquals(
            listOf(5, 4, 3, 2, 1, 0),
            listOf(BadgeLevelTone.PURPLE, BadgeLevelTone.ORIGINAL, BadgeLevelTone.RED, BadgeLevelTone.SILVER, BadgeLevelTone.BRONZE, null)
                .map { D.levelSortScore(it) },
        )
    }

    @Test
    fun kisiselRekorlarVeOdulSirasi() {
        BadgeProgressRepository.update(
            UserBadgeProgress(
                userKarateProgress = 1,
                userDartProgress = 12,
                userGolfProgress = 100,
                userDinoProgress = 600,
                goldMedalPiece = listOf(MedalPieceRow("A", 1), MedalPieceRow("B", 2)),
            ),
        )
        val rekorlar = D.personalRecordBadges()
        // Açık olanlar önce (GOLD, KARATE), sonra liste sırası (SILVER, BRONZE, CUP)
        assertEquals(
            listOf(BadgeAnimMode.GOLD_MEDAL, BadgeAnimMode.KARATE, BadgeAnimMode.SILVER_MEDAL, BadgeAnimMode.BRONZE_MEDAL, BadgeAnimMode.CUP_GOOGLE),
            rekorlar.map { it.targetMode },
        )
        assertEquals(listOf(true, true, false, false, false), rekorlar.map { it.unlocked })
        assertEquals(listOf("2x", "", "", "", ""), rekorlar.map { it.subtext })
        assertEquals("Şampiyonluk Madalyası", rekorlar[0].title)
        assertEquals(listOf(false, true, false, false, false), rekorlar.map { it.showCount })
        assertEquals(listOf(0, 1, 0, 0, 0), rekorlar.map { it.progressCount })
        assertEquals("gold_medal_anim.json", rekorlar[0].topAsset)
        assertNull(rekorlar[0].baseAsset)

        val oduller = D.awardBadges()
        assertEquals(
            listOf(
                // Ton puanı: GOLF mor(5), DART gümüş(2), DINO bronz(1)
                BadgeAnimMode.GOLF_WITH_BASE, BadgeAnimMode.DAILY_ONLY, BadgeAnimMode.DINO,
                // Kilitliler (ton yok): gösterilen değere göre azalan, eşitlikte awardKinds sırası
                BadgeAnimMode.CROCODILE, BadgeAnimMode.GOAT, BadgeAnimMode.EAGLE, BadgeAnimMode.FLY, BadgeAnimMode.TURTLE,
                BadgeAnimMode.BOWLING_WITH_BASE, BadgeAnimMode.ROCKET_WITH_BASE, BadgeAnimMode.FISHING_WITH_BASE,
                BadgeAnimMode.TORNADO, BadgeAnimMode.VOLCANO,
            ),
            oduller.map { it.targetMode },
        )
        assertEquals(listOf("5/5", "2/5", "1/5"), oduller.take(3).map { it.subtext })
        assertEquals(listOf(100, 10, 500), oduller.take(3).map { it.progressCount })
        assertEquals("0/5", oduller.last().subtext)
        assertTrue(oduller.all { it.showCount })
        assertEquals(listOf(BadgeLevelTone.PURPLE, BadgeLevelTone.SILVER, BadgeLevelTone.BRONZE), oduller.take(3).map { it.levelTone })
        assertEquals(listOf(3, 3, 3, 3, 1), D.awardBadgeRows().map { it.size })
        assertEquals(3, D.AWARDS_PER_ROW)
    }

    @Test
    fun kupaParcaSayisiAltMetni() {
        BadgeProgressRepository.update(
            UserBadgeProgress(cupPiece = listOf(CupPieceRow("A", 9, 1), CupPieceRow("B", 4, 2), CupPieceRow("C", 30, 3))),
        )
        val kupa = D.personalRecordBadges().first()
        assertEquals(BadgeAnimMode.CUP_GOOGLE, kupa.targetMode)
        assertEquals("3x", kupa.subtext)
        assertEquals(4, kupa.progressCount) // en iyi sıra
        assertTrue(kupa.showCount)
    }

    @Test
    fun getRenderConfig() {
        for (k in BadgeKind.entries) {
            val c = D.getRenderConfig(k)
            assertTrue(c.topAsset.endsWith(".json"), k.name)
            // Kişisel rekor rozetlerinde taban animasyonu yoktur
            assertEquals(k in D.personalRecordKinds, c.baseAsset == null, k.name)
        }
        assertEquals(
            BadgeDetailLogic.BadgeRenderConfig("daily_tasks_complite_badge2.json", "bowling_anim.json", 47f, 0.68f, "bg_badge_circle_frame"),
            D.getRenderConfig(BadgeKind.BOWLING),
        )
        assertEquals(
            BadgeDetailLogic.BadgeRenderConfig("daily_tasks_complite_badge2.json", "golf_anim.json", 30f, 0.60f, null),
            D.getRenderConfig(BadgeKind.GOLF),
        )
        assertEquals(
            BadgeDetailLogic.BadgeRenderConfig(null, "silver_medal_anim.json.json", 0f, 1f, null),
            D.getRenderConfig(BadgeKind.SILVER),
        )
        assertEquals(
            BadgeDetailLogic.BadgeRenderConfig("daily_tasks_complite_badge2.json", "turtle_anim.json", 9999f, 0.50f, "bg_badge_circle_frame"),
            D.getRenderConfig(BadgeKind.TURTLE),
        )
        assertEquals(D.personalRecordKinds.toSet() + D.awardKinds.toSet(), BadgeKind.entries.toSet())
    }
}
