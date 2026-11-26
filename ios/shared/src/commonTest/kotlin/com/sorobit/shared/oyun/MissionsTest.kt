// Görev kataloğu, MissionsProgressStore ve görev arayüzü hesaplarının Android davranışıyla aynı
// sonucu verdiğini doğrular. Zaman değerleri elle hesaplanmıştır:
//   2026-06-01T00:00:00Z = 1_780_272_000_000 ms (Pazartesi, yılın 152. günü)
//   2027-01-01T00:00:00Z = 1_798_761_600_000 ms (Cuma)
package com.sorobit.shared.oyun

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone

private const val SAAT = 3_600_000L
private const val GUN = 86_400_000L
private const val PZT_1_HAZIRAN = 1_780_272_000_000L // 2026-06-01T00:00Z, Pazartesi
private const val CAR_OGLEN = PZT_1_HAZIRAN + 2 * GUN + 12 * SAAT // 2026-06-03T12:00Z, Çarşamba
private val UTC = TimeZone.UTC
private val ARTI_UC = UtcOffset(hours = 3).asTimeZone()

/** Sahte bulut: çağrıları kaydeder; eşzamansız okumalar elle tamamlanır. */
private class SahteBulut(var uid: String? = null) : MissionCloudGateway {
    /** null → engelleyen okuma istisna fırlatır. */
    var engelleyenYanit: (() -> Map<String, Any?>?)? = null
    var engelleyenOkumaSayisi = 0
    val bekleyenOkumalar = mutableListOf<Pair<(Map<String, Any?>?) -> Unit, () -> Unit>>()
    val yazilanlar = mutableListOf<Pair<String, Map<String, Any>>>()
    val dinleyiciler = mutableListOf<Pair<String, (Map<String, Any?>?) -> Unit>>()
    var kaldirilanDinleyici = 0

    override fun currentUid(): String? = uid

    override fun fetchState(uid: String, onSuccess: (Map<String, Any?>?) -> Unit, onFailure: () -> Unit) {
        bekleyenOkumalar.add(onSuccess to onFailure)
    }

    override fun fetchStateBlocking(uid: String): Map<String, Any?>? {
        engelleyenOkumaSayisi++
        val yanit = engelleyenYanit ?: throw IllegalStateException("ağ yok")
        return yanit()
    }

    override fun setState(uid: String, payload: Map<String, Any>) {
        yazilanlar.add(uid to payload)
    }

    override fun listenState(uid: String, onSnapshot: (Map<String, Any?>?) -> Unit): () -> Unit {
        dinleyiciler.add(uid to onSnapshot)
        return { kaldirilanDinleyici++ }
    }
}

private class Ortam(uid: String? = null, var duvar: Long = CAR_OGLEN, var gecen: Long = 1_000L, val tz: TimeZone = UTC) {
    val bulut = SahteBulut(uid)
    val dosyalar = mutableMapOf<String, InMemoryKeyValueStore>()
    val depo = MissionsProgressStore(
        prefsProvider = { ad -> dosyalar.getOrPut(ad) { InMemoryKeyValueStore() } },
        cloud = bulut,
        elapsedRealtimeMs = { gecen },
        wallClockMs = { duvar },
        timeZone = { tz },
    )

    fun prefs(ad: String = "missions_progress_${bulut.uid ?: "guest"}") = dosyalar.getOrPut(ad) { InMemoryKeyValueStore() }

    /** Gerçek zaman akışı: duvar saati ve monotonik saat birlikte ilerler. */
    fun ilerlet(ms: Long) {
        duvar += ms
        gecen += ms
    }
}

class MissionQuestCatalogTest {
    @Test
    fun katalogIcerigi() {
        val beklenen = listOf(
            MissionQuestDefinition("finish_step_2", "mission_finish_two_lessons", 2, MissionCounterType.STEP_FINISH, setOf(MissionWindow.DAILY)),
            MissionQuestDefinition("finish_lessons_5", "mission_finish_five_lessons", 5, MissionCounterType.STEP_INCREMENT, setOf(MissionWindow.DAILY)),
            MissionQuestDefinition("finish_lessons_perfect_2", "mission_finish_two_lessons_perfect", 2, MissionCounterType.PERFECT_STEP_INCREMENT, setOf(MissionWindow.DAILY)),
            MissionQuestDefinition("chest_record_break_1", "mission_break_chest_record", 1, MissionCounterType.CHEST_RECORD_BREAK, setOf(MissionWindow.WEEKLY)),
            MissionQuestDefinition("chest_star_gain_1", "mission_gain_one_star_in_unit_eval", 5, MissionCounterType.CHEST_STAR_GAIN, setOf(MissionWindow.WEEKLY)),
            MissionQuestDefinition("chest_star_gain_2_daily", "mission_gain_two_stars", 2, MissionCounterType.CHEST_STAR_GAIN, setOf(MissionWindow.DAILY)),
            MissionQuestDefinition("learn_for_10_minutes", "mission_learn_for_10_minutes", 10, MissionCounterType.LEARN_MINUTES, setOf(MissionWindow.DAILY)),
            MissionQuestDefinition("finish_lessons_20", "mission_finish_twenty_lessons", 20, MissionCounterType.STEP_INCREMENT, setOf(MissionWindow.WEEKLY)),
            MissionQuestDefinition("finish_step_5_weekly", "mission_finish_five_steps", 5, MissionCounterType.STEP_FINISH, setOf(MissionWindow.WEEKLY)),
            MissionQuestDefinition("finish_lessons_perfect_10_weekly", "mission_finish_ten_lessons_perfect", 10, MissionCounterType.PERFECT_STEP_INCREMENT, setOf(MissionWindow.WEEKLY)),
        )
        assertEquals(beklenen, MissionQuestCatalog.all)
        assertEquals(5, MissionQuestCatalog.all.count { MissionWindow.DAILY in it.availableWindows })
        assertEquals(5, MissionQuestCatalog.all.count { MissionWindow.WEEKLY in it.availableWindows })
        assertEquals(10, MissionQuestCatalog.all.map { it.id }.toSet().size)
    }

    @Test
    fun enumSiralari() {
        assertEquals(
            listOf("STEP_FINISH", "STEP_INCREMENT", "PERFECT_STEP_INCREMENT", "CHEST_RECORD_BREAK", "CHEST_STAR_GAIN", "LEARN_MINUTES"),
            MissionCounterType.entries.map { it.name },
        )
        assertEquals(listOf("DAILY", "WEEKLY"), MissionWindow.entries.map { it.name })
        // Varsayılan pencere kümesi: ikisi de
        assertEquals(setOf(MissionWindow.DAILY, MissionWindow.WEEKLY), MissionQuestDefinition("x", "t", 1, MissionCounterType.STEP_FINISH).availableWindows)
    }
}

class MissionsProgressStoreSafHesapTest {
    @Test
    fun sabitler() {
        assertEquals("missions_progress", MissionsProgressStore.PREFS)
        assertEquals("users/u1/missionProgress/state", MissionsProgressStore.stateDocPath("u1"))
        assertEquals("rewardClaimedKeys", MissionsProgressStore.FIELD_REWARD_CLAIMED_KEYS)
        assertEquals(300_000L, MissionsProgressStore.CLOCK_TOLERANCE_MS)
        assertEquals("day_id", MissionsProgressStore.KEY_DAY_ID)
        assertEquals("week_id", MissionsProgressStore.KEY_WEEK_ID)
        assertEquals("daily_learn_remainder_ms", MissionsProgressStore.KEY_DAILY_LEARN_REMAINDER_MS)
        assertEquals("weekly_selected_mission_ids", MissionsProgressStore.KEY_WEEKLY_SELECTED_MISSION_IDS)
        assertEquals("trusted_now_ms", MissionsProgressStore.KEY_TRUSTED_NOW_MS)
        assertEquals("trusted_anchor_elapsed_ms", MissionsProgressStore.KEY_TRUSTED_ANCHOR_ELAPSED_MS)
    }

    @Test
    fun gunKimligi() {
        // 1 Haziran 2026 = yılın 152. günü
        assertEquals("2026-152", MissionsProgressStore.dayId(PZT_1_HAZIRAN, UTC))
        assertEquals("2026-154", MissionsProgressStore.dayId(CAR_OGLEN, UTC))
        // Gün sınırı
        assertEquals("2026-151", MissionsProgressStore.dayId(PZT_1_HAZIRAN - 1, UTC))
        // Yerel saat dilimi: 31 Mayıs 22:30Z = 1 Haziran 01:30 (+03)
        assertEquals("2026-151", MissionsProgressStore.dayId(PZT_1_HAZIRAN - 90 * 60_000L, UTC))
        assertEquals("2026-152", MissionsProgressStore.dayId(PZT_1_HAZIRAN - 90 * 60_000L, ARTI_UC))
        // Yıl başı ve yıl sonu (sıfır doldurma yok)
        assertEquals("2027-1", MissionsProgressStore.dayId(1_798_761_600_000L, UTC))
        assertEquals("2026-365", MissionsProgressStore.dayId(1_798_761_600_000L - 1, UTC))
        assertEquals("1970-1", MissionsProgressStore.dayId(0L, UTC))
    }

    @Test
    fun haftaKimligiAyiSifirTabanlidir() {
        // Pazartesi 1 Haziran → ay 5 (Calendar.MONTH: Haziran = 5)
        assertEquals("2026-5-1", MissionsProgressStore.weekId(PZT_1_HAZIRAN, UTC))
        assertEquals("2026-5-1", MissionsProgressStore.weekId(CAR_OGLEN, UTC))
        // Pazar 7 Haziran 23:59:59.999 hâlâ aynı hafta; Pazartesi 8 Haziran yeni hafta
        assertEquals("2026-5-1", MissionsProgressStore.weekId(PZT_1_HAZIRAN + 7 * GUN - 1, UTC))
        assertEquals("2026-5-8", MissionsProgressStore.weekId(PZT_1_HAZIRAN + 7 * GUN, UTC))
        // Pazar 31 Mayıs → önceki haftanın Pazartesi'si 25 Mayıs (ay 4)
        assertEquals("2026-4-25", MissionsProgressStore.weekId(PZT_1_HAZIRAN - 1, UTC))
        // Yerel saat dilimi: Pazar 22:30Z = Pazartesi 01:30 (+03) → yeni hafta
        val pazarGece = PZT_1_HAZIRAN + 6 * GUN + 22 * SAAT + 30 * 60_000L
        assertEquals("2026-5-1", MissionsProgressStore.weekId(pazarGece, UTC))
        assertEquals("2026-5-8", MissionsProgressStore.weekId(pazarGece, ARTI_UC))
        // Yıl sınırı: Cuma 1 Ocak 2027 → Pazartesi 28 Aralık 2026 (ay 11)
        assertEquals("2026-11-28", MissionsProgressStore.weekId(1_798_761_600_000L, UTC))
        // Ocak = 0: Pazartesi 4 Ocak 2027
        assertEquals("2027-0-4", MissionsProgressStore.weekId(1_798_761_600_000L + 3 * GUN, UTC))
        // 1 Ocak 1970 Perşembe → Pazartesi 29 Aralık 1969
        assertEquals("1969-11-29", MissionsProgressStore.weekId(0L, UTC))
    }

    @Test
    fun gunlukSifirlanmayaKalanSaat() {
        assertEquals(12, MissionsProgressStore.hoursUntilDailyReset(CAR_OGLEN, UTC))
        // Tam gece yarısı: 24 saat
        assertEquals(24, MissionsProgressStore.hoursUntilDailyReset(PZT_1_HAZIRAN, UTC))
        // Aşağı yuvarlanır: 23 sa 59 dk → 23
        assertEquals(23, MissionsProgressStore.hoursUntilDailyReset(PZT_1_HAZIRAN + 60_000L, UTC))
        // 1 saatten az kaldıysa en az 1
        assertEquals(1, MissionsProgressStore.hoursUntilDailyReset(PZT_1_HAZIRAN + 23 * SAAT + 30 * 60_000L, UTC))
        assertEquals(1, MissionsProgressStore.hoursUntilDailyReset(PZT_1_HAZIRAN + GUN - 1, UTC))
        // Yerel saat dilimi: 12:00Z = 15:00 (+03) → 9 saat
        assertEquals(9, MissionsProgressStore.hoursUntilDailyReset(CAR_OGLEN, ARTI_UC))
    }

    @Test
    fun haftalikSifirlanmayaKalanSure() {
        // Çarşamba 12:00 → Pazartesi 00:00'a 4,5 gün
        assertEquals(4 * GUN + 12 * SAAT, MissionsProgressStore.millisUntilWeeklyReset(CAR_OGLEN, UTC))
        // Tam Pazartesi 00:00: 7 gün
        assertEquals(7 * GUN, MissionsProgressStore.millisUntilWeeklyReset(PZT_1_HAZIRAN, UTC))
        assertEquals(1L, MissionsProgressStore.millisUntilWeeklyReset(PZT_1_HAZIRAN + 7 * GUN - 1, UTC))
        // +03: yerel Pazartesi 00:00 = Pazar 21:00Z
        assertEquals(4 * GUN + 9 * SAAT, MissionsProgressStore.millisUntilWeeklyReset(CAR_OGLEN, ARTI_UC))
    }

    @Test
    fun guvenilirSaat() {
        val t = 1_000_000_000_000L
        // İlk çağrı: duvar saati
        assertEquals(t, MissionsProgressStore.computeTrustedNowMs(t, 5_000, 0, 0))
        // Normal akış (tolerans içinde): duvar saati
        assertEquals(t + 60_000, MissionsProgressStore.computeTrustedNowMs(t + 60_000, 65_000, t, 5_000))
        // Tolerans tam sınırda (5 dk sapma): duvar saati
        assertEquals(t + 60_000 + 300_000, MissionsProgressStore.computeTrustedNowMs(t + 60_000 + 300_000, 65_000, t, 5_000))
        assertEquals(t + 60_000 - 300_000, MissionsProgressStore.computeTrustedNowMs(t + 60_000 - 300_000, 65_000, t, 5_000))
        // Saat ileri alınmış (1 gün): monotonik değer
        assertEquals(t + 60_000, MissionsProgressStore.computeTrustedNowMs(t + GUN, 65_000, t, 5_000))
        // Tolerans 1 ms aşıldı: monotonik değer
        assertEquals(t + 60_000, MissionsProgressStore.computeTrustedNowMs(t + 60_000 + 300_001, 65_000, t, 5_000))
        // Saat geri alınmış: monotonik değer
        assertEquals(t + 60_000, MissionsProgressStore.computeTrustedNowMs(t - GUN, 65_000, t, 5_000))
        // Yeniden başlatma (monotonik saat küçüldü): duvar saatine güven ama geriye gitme
        assertEquals(t + 500, MissionsProgressStore.computeTrustedNowMs(t + 500, 100, t, 5_000))
        assertEquals(t, MissionsProgressStore.computeTrustedNowMs(t - GUN, 100, t, 5_000))
        // Yeniden başlatma sonrası saat ileri alınmışsa kabul edilir (Android'deki bilinen sınır)
        assertEquals(t + GUN, MissionsProgressStore.computeTrustedNowMs(t + GUN, 100, t, 5_000))
    }

    @Test
    fun odulAnahtariVeCsv() {
        assertEquals(
            "reward_claimed_daily_2026-154_finish_step_2",
            MissionsProgressStore.rewardClaimKeyFor(MissionWindow.DAILY, "2026-154", "finish_step_2"),
        )
        assertEquals(
            "reward_claimed_weekly_2026-5-1_finish_lessons_20",
            MissionsProgressStore.rewardClaimKeyFor(MissionWindow.WEEKLY, "2026-5-1", "finish_lessons_20"),
        )
        assertEquals(emptyList(), MissionsProgressStore.parseMissionIdsCsv(null))
        assertEquals(emptyList(), MissionsProgressStore.parseMissionIdsCsv(""))
        assertEquals(listOf("a", "b", "c"), MissionsProgressStore.parseMissionIdsCsv(" a, b ,,c, "))
        assertEquals("a,b,c", MissionsProgressStore.missionIdsCsv(listOf("a", "b", "c")))
        assertEquals("", MissionsProgressStore.missionIdsCsv(emptyList()))
    }

    @Test
    fun javaHashKodu() {
        assertEquals(0, MissionsProgressStore.javaStringHashCode(""))
        assertEquals(97, MissionsProgressStore.javaStringHashCode("a"))
        assertEquals(96354, MissionsProgressStore.javaStringHashCode("abc"))
        assertEquals(99162322, MissionsProgressStore.javaStringHashCode("hello"))
        // 31^2*'z' + 31*'z' + 'z' = 122 * 993 = 121146
        assertEquals(121146, MissionsProgressStore.javaStringHashCode("zzz"))
        // 32 bit taşma: "zzzzzzz" → 122 * (31^7 - 1) / 30 = 111_884_630_714 → mod 2^32 = 215_481_018
        assertEquals(215_481_018, MissionsProgressStore.javaStringHashCode("zzzzzzz"))
        assertEquals(1162716598, MissionsProgressStore.javaStringHashCode("2026-154_0"))
        assertEquals(1166169471, MissionsProgressStore.javaStringHashCode("2026-5-1_0"))
        // Platformun kendi String.hashCode'u ile de aynı (JVM'de Java'nınki)
        listOf("", "a", "2026-154_0", "2027-0-4_17", "ünite şğı", "uzun bir anahtar ".repeat(20)).forEach {
            assertEquals(it.hashCode(), MissionsProgressStore.javaStringHashCode(it), it)
        }
    }

    @Test
    fun tohumluGorevSecimi() {
        // Altın değerler: Android'in kullandığı aynı algoritma (Java String.hashCode tohumu +
        // kotlin.random.Random + shuffled().take(3)) ile JVM'de üretildi; Kotlin/Native'de de aynı
        // çıkması iOS'un Android'le aynı görevleri seçeceğini gösterir.
        assertEquals(
            listOf("finish_step_2", "finish_lessons_5", "learn_for_10_minutes"),
            MissionsProgressStore.generateMissionIds(MissionWindow.DAILY, "2026-154_0"),
        )
        assertEquals(
            listOf("chest_star_gain_2_daily", "finish_lessons_5", "finish_step_2"),
            MissionsProgressStore.generateMissionIds(MissionWindow.DAILY, "2026-154_1"),
        )
        assertEquals(
            listOf("learn_for_10_minutes", "finish_step_2", "finish_lessons_5"),
            MissionsProgressStore.generateMissionIds(MissionWindow.DAILY, "2026-155_0"),
        )
        assertEquals(
            listOf("chest_record_break_1", "finish_lessons_20", "finish_step_5_weekly"),
            MissionsProgressStore.generateMissionIds(MissionWindow.WEEKLY, "2026-5-1_0"),
        )
        assertEquals(
            listOf("finish_step_5_weekly", "chest_star_gain_1", "finish_lessons_20"),
            MissionsProgressStore.generateMissionIds(MissionWindow.WEEKLY, "2026-5-1_1"),
        )
        assertEquals(
            listOf("finish_lessons_20", "finish_step_5_weekly", "finish_lessons_perfect_10_weekly"),
            MissionsProgressStore.generateMissionIds(MissionWindow.WEEKLY, "2026-5-8_0"),
        )
    }

    @Test
    fun sayacOkuma() {
        val s = MissionsProgressStore.Snapshot(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val beklenen = mapOf(
            (MissionWindow.DAILY to MissionCounterType.STEP_FINISH) to 1,
            (MissionWindow.WEEKLY to MissionCounterType.STEP_FINISH) to 2,
            (MissionWindow.DAILY to MissionCounterType.STEP_INCREMENT) to 3,
            (MissionWindow.WEEKLY to MissionCounterType.STEP_INCREMENT) to 4,
            (MissionWindow.DAILY to MissionCounterType.PERFECT_STEP_INCREMENT) to 5,
            (MissionWindow.WEEKLY to MissionCounterType.PERFECT_STEP_INCREMENT) to 6,
            (MissionWindow.DAILY to MissionCounterType.CHEST_RECORD_BREAK) to 7,
            (MissionWindow.WEEKLY to MissionCounterType.CHEST_RECORD_BREAK) to 8,
            (MissionWindow.DAILY to MissionCounterType.CHEST_STAR_GAIN) to 9,
            (MissionWindow.WEEKLY to MissionCounterType.CHEST_STAR_GAIN) to 10,
            (MissionWindow.DAILY to MissionCounterType.LEARN_MINUTES) to 11,
            (MissionWindow.WEEKLY to MissionCounterType.LEARN_MINUTES) to 12,
        )
        beklenen.forEach { (anahtar, deger) ->
            assertEquals(deger, MissionsProgressStore.readCounter(s, anahtar.first, anahtar.second), "$anahtar")
        }
        val gorev = MissionQuestCatalog.all.first { it.id == "chest_star_gain_1" }
        assertEquals(10, MissionsProgressStore.missionProgress(s, MissionWindow.WEEKLY, gorev))
        assertEquals(9, MissionsProgressStore.missionProgress(s, MissionWindow.DAILY, gorev))
    }
}

class MissionsProgressStoreMisafirTest {
    @Test
    fun ilkOkumadaKimliklerYazilirVeSayaclarSifirdir() {
        val o = Ortam()
        val s = o.depo.getSnapshot()
        assertEquals(MissionsProgressStore.Snapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), s)
        val p = o.prefs()
        assertEquals("2026-154", p.getString("day_id"))
        assertEquals("2026-5-1", p.getString("week_id"))
        assertEquals(CAR_OGLEN, p.getLong("trusted_now_ms", -1))
        assertEquals(1_000L, p.getLong("trusted_anchor_elapsed_ms", -1))
        // Misafir: prefs dosyası "missions_progress_guest", bulut çağrısı yok
        assertEquals(setOf("missions_progress_guest"), o.dosyalar.keys)
        assertTrue(o.bulut.yazilanlar.isEmpty())
        assertTrue(o.bulut.dinleyiciler.isEmpty())
        assertEquals(0, o.bulut.engelleyenOkumaSayisi)
        assertEquals("2026-154", o.depo.calendarDayId())
    }

    @Test
    fun sayacArtislari() {
        val o = Ortam()
        o.depo.recordStepFinishProgress()
        o.depo.recordStepIncrementProgress()
        o.depo.recordStepIncrementProgress()
        o.depo.recordPerfectStepIncrementProgress()
        o.depo.recordChestRecordBreakProgress()
        o.depo.recordChestStarGainProgress()
        o.depo.recordChestStarGainProgress(3)
        // 0 ve negatif miktar yok sayılır
        o.depo.recordChestStarGainProgress(0)
        o.depo.recordChestStarGainProgress(-2)
        assertEquals(MissionsProgressStore.Snapshot(1, 1, 2, 2, 1, 1, 1, 1, 4, 4, 0, 0), o.depo.getSnapshot())
    }

    @Test
    fun ogrenmeSuresiDakikayaCevrilir() {
        val o = Ortam()
        o.depo.recordLearningDurationMs(0)
        o.depo.recordLearningDurationMs(-5)
        o.depo.recordLearningDurationMs(150_000)
        // Uygulanana dek dakika sayacı değişmez
        assertEquals(0, o.depo.getSnapshot().dailyLearnMinutesCount)
        o.depo.applyPendingLearningMinutes()
        var s = o.depo.getSnapshot()
        assertEquals(2, s.dailyLearnMinutesCount)
        assertEquals(2, s.weeklyLearnMinutesCount)
        assertEquals(30_000L, o.prefs().getLong("daily_learn_remainder_ms", -1))
        assertEquals(30_000L, o.prefs().getLong("weekly_learn_remainder_ms", -1))
        // Kalan + 20 sn = 50 sn → dakika dolmadı, hiçbir şey değişmez
        o.depo.recordLearningDurationMs(20_000)
        o.depo.applyPendingLearningMinutes()
        assertEquals(2, o.depo.getSnapshot().dailyLearnMinutesCount)
        assertEquals(50_000L, o.prefs().getLong("daily_learn_remainder_ms", -1))
        // +10 sn → 1 dakika daha, kalan 0
        o.depo.recordLearningDurationMs(10_000)
        o.depo.applyPendingLearningMinutes()
        s = o.depo.getSnapshot()
        assertEquals(3, s.dailyLearnMinutesCount)
        assertEquals(0L, o.prefs().getLong("daily_learn_remainder_ms", -1))
    }

    @Test
    fun gunDegisinceYalnizcaGunlukSayaclarSifirlanir() {
        val o = Ortam()
        o.depo.recordStepFinishProgress()
        o.depo.recordLearningDurationMs(90_000)
        o.depo.applyPendingLearningMinutes()
        o.depo.selectedMissionsForDaily()
        o.depo.selectedMissionsForWeekly()
        assertNotNull(o.prefs().getString("daily_selected_mission_ids"))

        o.ilerlet(GUN) // Perşembe: aynı hafta
        val s = o.depo.getSnapshot()
        assertEquals(0, s.dailyStepFinishCount)
        assertEquals(1, s.weeklyStepFinishCount)
        assertEquals(0, s.dailyLearnMinutesCount)
        assertEquals(1, s.weeklyLearnMinutesCount)
        assertEquals("2026-155", o.prefs().getString("day_id"))
        assertEquals("2026-5-1", o.prefs().getString("week_id"))
        assertEquals(0L, o.prefs().getLong("daily_learn_remainder_ms", -1))
        assertEquals(30_000L, o.prefs().getLong("weekly_learn_remainder_ms", -1))
        // Günlük seçim silinir, haftalık kalır
        assertNull(o.prefs().getString("daily_selected_mission_ids"))
        assertNotNull(o.prefs().getString("weekly_selected_mission_ids"))
    }

    @Test
    fun haftaDegisinceHaftalikSayaclarDaSifirlanir() {
        val o = Ortam()
        o.depo.recordStepFinishProgress()
        o.depo.selectedMissionsForWeekly()
        o.ilerlet(5 * GUN) // Pazartesi 8 Haziran
        val s = o.depo.getSnapshot()
        assertEquals(0, s.dailyStepFinishCount)
        assertEquals(0, s.weeklyStepFinishCount)
        assertEquals("2026-159", o.prefs().getString("day_id"))
        assertEquals("2026-5-8", o.prefs().getString("week_id"))
        assertNull(o.prefs().getString("weekly_selected_mission_ids"))
    }

    @Test
    fun saatiIleriAlmakSifirlamayiTetiklemez() {
        val o = Ortam()
        o.depo.recordStepFinishProgress()
        // Kullanıcı tarihi 1 gün ileri aldı; gerçek süre yalnızca 1 sn geçti
        o.duvar += GUN
        o.gecen += 1_000
        val s = o.depo.getSnapshot()
        assertEquals(1, s.dailyStepFinishCount)
        assertEquals("2026-154", o.prefs().getString("day_id"))
        assertEquals(CAR_OGLEN + 1_000, o.prefs().getLong("trusted_now_ms", -1))
        // Güvenilir saatle hesaplanan kalan süreler de değişmez (12 saat − 1 sn → 11)
        assertEquals(11, o.depo.hoursUntilDailyReset())
        assertEquals(4 * GUN + 12 * SAAT - 1_000, o.depo.millisUntilWeeklyReset())
        // calendarDayId ise ham duvar saatini kullanır (Android ile aynı)
        assertEquals("2026-155", o.depo.calendarDayId())
    }

    @Test
    fun gorevSecimiKararliVeKaydedilir() {
        val o = Ortam()
        val gunluk = o.depo.selectedMissionsForDaily().map { it.id }
        val haftalik = o.depo.selectedMissionsForWeekly().map { it.id }
        // Tohum: "2026-154_0" ve "2026-5-1_0"
        assertEquals(listOf("finish_step_2", "finish_lessons_5", "learn_for_10_minutes"), gunluk)
        assertEquals(listOf("chest_record_break_1", "finish_lessons_20", "finish_step_5_weekly"), haftalik)
        assertEquals("finish_step_2,finish_lessons_5,learn_for_10_minutes", o.prefs().getString("daily_selected_mission_ids"))
        assertEquals("chest_record_break_1,finish_lessons_20,finish_step_5_weekly", o.prefs().getString("weekly_selected_mission_ids"))
        assertEquals(gunluk, o.depo.selectedMissionsForDaily().map { it.id })
        assertEquals(gunluk, o.depo.selectedMissionsForWindow(MissionWindow.DAILY).map { it.id })
    }

    @Test
    fun kayitliSecimGecerliyseAynenKullanilir() {
        val o = Ortam()
        o.depo.ensureResets()
        o.prefs().putString("daily_selected_mission_ids", "learn_for_10_minutes, finish_step_2 ,chest_star_gain_2_daily")
        assertEquals(
            listOf("learn_for_10_minutes", "finish_step_2", "chest_star_gain_2_daily"),
            o.depo.selectedMissionsForDaily().map { it.id },
        )
        // 4 kayıtlıysa ilk 3'ü alınır
        o.prefs().putString("daily_selected_mission_ids", "learn_for_10_minutes,finish_step_2,chest_star_gain_2_daily,finish_lessons_5")
        assertEquals(
            listOf("learn_for_10_minutes", "finish_step_2", "chest_star_gain_2_daily"),
            o.depo.selectedMissionsForDaily().map { it.id },
        )
    }

    @Test
    fun kayitliSecimGecersizseYenidenUretilir() {
        val beklenen = listOf("finish_step_2", "finish_lessons_5", "learn_for_10_minutes")
        // Haftalık görev günlük listede: izin verilmez → 2 geçerli kalır → yeniden üret
        var o = Ortam()
        o.depo.ensureResets()
        o.prefs().putString("daily_selected_mission_ids", "learn_for_10_minutes,finish_step_2,finish_lessons_20")
        assertEquals(beklenen, o.depo.selectedMissionsForDaily().map { it.id })
        assertEquals(beklenen.joinToString(","), o.prefs().getString("daily_selected_mission_ids"))
        // Yinelenen kimlik
        o = Ortam()
        o.depo.ensureResets()
        o.prefs().putString("daily_selected_mission_ids", "finish_step_2,finish_step_2,finish_lessons_5")
        assertEquals(beklenen, o.depo.selectedMissionsForDaily().map { it.id })
        // Bilinmeyen kimlik / boş
        o = Ortam()
        o.depo.ensureResets()
        o.prefs().putString("daily_selected_mission_ids", "yok,finish_step_2,finish_lessons_5")
        assertEquals(beklenen, o.depo.selectedMissionsForDaily().map { it.id })
    }

    @Test
    fun yenidenSecimNonceArtirir() {
        val o = Ortam()
        o.depo.selectedMissionsForDaily()
        o.depo.selectedMissionsForWeekly()
        o.depo.forceReselectMissions()
        assertEquals(1, o.prefs().getInt("daily_selection_nonce", -1))
        assertEquals(1, o.prefs().getInt("weekly_selection_nonce", -1))
        assertNull(o.prefs().getString("daily_selected_mission_ids"))
        assertNull(o.prefs().getString("weekly_selected_mission_ids"))
        // Tohum: "2026-154_1" ve "2026-5-1_1"
        assertEquals(listOf("chest_star_gain_2_daily", "finish_lessons_5", "finish_step_2"), o.depo.selectedMissionsForDaily().map { it.id })
        assertEquals(listOf("finish_step_5_weekly", "chest_star_gain_1", "finish_lessons_20"), o.depo.selectedMissionsForWeekly().map { it.id })
    }

    @Test
    fun odulTalebiPeriyodaBaglidir() {
        val o = Ortam()
        assertFalse(o.depo.isMissionRewardClaimed(MissionWindow.DAILY, "finish_step_2"))
        o.depo.markMissionRewardClaimed(MissionWindow.DAILY, "finish_step_2")
        o.depo.markMissionRewardClaimed(MissionWindow.WEEKLY, "finish_lessons_20")
        assertTrue(o.prefs().getBoolean("reward_claimed_daily_2026-154_finish_step_2", false))
        assertTrue(o.prefs().getBoolean("reward_claimed_weekly_2026-5-1_finish_lessons_20", false))
        assertTrue(o.depo.isMissionRewardClaimed(MissionWindow.DAILY, "finish_step_2"))
        assertTrue(o.depo.isMissionRewardClaimed(MissionWindow.WEEKLY, "finish_lessons_20"))
        // Pencere ve görev ayrıdır
        assertFalse(o.depo.isMissionRewardClaimed(MissionWindow.WEEKLY, "finish_step_2"))
        assertFalse(o.depo.isMissionRewardClaimed(MissionWindow.DAILY, "finish_lessons_5"))
        // Ertesi gün: günlük talep düşer (anahtar değişti), haftalık sürer; eski anahtar silinmez
        o.ilerlet(GUN)
        assertFalse(o.depo.isMissionRewardClaimed(MissionWindow.DAILY, "finish_step_2"))
        assertTrue(o.depo.isMissionRewardClaimed(MissionWindow.WEEKLY, "finish_lessons_20"))
        assertTrue(o.prefs().contains("reward_claimed_daily_2026-154_finish_step_2"))
    }

    @Test
    fun tumIlerlemeyiSifirlama() {
        val o = Ortam()
        o.depo.recordStepFinishProgress()
        o.depo.recordChestStarGainProgress(2)
        o.depo.recordLearningDurationMs(70_000)
        o.depo.applyPendingLearningMinutes()
        o.depo.markMissionRewardClaimed(MissionWindow.DAILY, "finish_step_2")
        o.depo.selectedMissionsForDaily()
        o.depo.forceReselectMissions()
        o.depo.resetAllProgress()
        assertEquals(MissionsProgressStore.Snapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), o.depo.getSnapshot())
        val p = o.prefs()
        assertFalse(p.keys().any { it.startsWith("reward_claimed_") })
        assertEquals(0L, p.getLong("daily_learn_remainder_ms", -1))
        assertNull(p.getString("daily_selected_mission_ids"))
        assertEquals("2026-154", p.getString("day_id"))
        // Seçim nonce'ları SIFIRLANMAZ (Android ile aynı)
        assertEquals(1, p.getInt("daily_selection_nonce", -1))
    }

    @Test
    fun gorunurIlerleme() {
        val o = Ortam()
        // Günlük seçim: finish_step_2 (hedef 2), finish_lessons_5 (5), learn_for_10_minutes (10)
        // Haftalık seçim: chest_record_break_1 (1), finish_lessons_20 (20), finish_step_5_weekly (5)
        val once = o.depo.getSnapshot()
        assertFalse(o.depo.hasVisibleMissionProgress(once, once))
        o.depo.recordStepFinishProgress()
        assertTrue(o.depo.hasVisibleMissionProgress(once, o.depo.getSnapshot()))

        // Seçili olmayan sayaç (hatasız ders) görünür ilerleme sayılmaz
        val a = o.depo.getSnapshot()
        o.depo.recordPerfectStepIncrementProgress()
        assertFalse(o.depo.hasVisibleMissionProgress(a, o.depo.getSnapshot()))

        // Hedefi aşmış sayaçtaki artış görünmez: günlük 2/2 → 3, haftalık 5/5 → 6
        repeat(4) { o.depo.recordStepFinishProgress() } // günlük 5, haftalık 5
        val dolu = o.depo.getSnapshot()
        o.depo.recordStepFinishProgress()
        assertFalse(o.depo.hasVisibleMissionProgress(dolu, o.depo.getSnapshot()))

        // Yalnızca haftalıkta görünen değişim: rekor kırma (günlükte seçili değil)
        val b = o.depo.getSnapshot()
        o.depo.recordChestRecordBreakProgress()
        assertTrue(o.depo.hasVisibleMissionProgress(b, o.depo.getSnapshot()))
    }
}

class MissionsProgressStoreBulutTest {
    @Test
    fun ilkYazimBuluttanOkumayiBekler() {
        val o = Ortam(uid = "u1")
        o.depo.recordStepFinishProgress()
        // Kullanıcıya özel prefs dosyası
        assertEquals(setOf("missions_progress_u1"), o.dosyalar.keys)
        // Dinleyici kuruldu, bulut durumu henüz uygulanmadığı için yazılmadı, okuma istendi
        assertEquals(listOf("u1"), o.bulut.dinleyiciler.map { it.first })
        assertTrue(o.bulut.yazilanlar.isEmpty())
        assertEquals(1, o.bulut.bekleyenOkumalar.size)
        // Bekleyen okuma varken ikinci istek yapılmaz
        o.depo.recordStepFinishProgress()
        assertEquals(1, o.bulut.bekleyenOkumalar.size)

        // Bulutta belge yok → yerel durum yüklenir
        o.bulut.bekleyenOkumalar.single().first(null)
        val (uid, yuk) = o.bulut.yazilanlar.single()
        assertEquals("u1", uid)
        assertEquals(21, yuk.size)
        assertEquals("2026-154", yuk["day_id"])
        assertEquals("2026-5-1", yuk["week_id"])
        assertEquals(2, yuk["daily_step_finish_count"])
        assertEquals(2, yuk["weekly_step_finish_count"])
        assertEquals(0, yuk["daily_learn_minutes_count"])
        assertEquals(0L, yuk["daily_learn_remainder_ms"])
        assertEquals(0, yuk["daily_selection_nonce"])
        assertEquals("", yuk["daily_selected_mission_ids"])
        assertEquals("", yuk["weekly_selected_mission_ids"])
        assertEquals(emptyList<String>(), yuk["rewardClaimedKeys"])

        // Artık her değişiklik doğrudan yazılır
        o.depo.recordStepIncrementProgress()
        assertEquals(2, o.bulut.yazilanlar.size)
        assertEquals(1, o.bulut.yazilanlar.last().second["daily_step_increment_count"])
    }

    @Test
    fun okumaHatasindanSonraYenidenDenenir() {
        val o = Ortam(uid = "u1")
        o.depo.recordStepFinishProgress()
        o.bulut.bekleyenOkumalar.single().second()
        assertTrue(o.bulut.yazilanlar.isEmpty())
        o.depo.recordStepFinishProgress()
        assertEquals(2, o.bulut.bekleyenOkumalar.size)
    }

    @Test
    fun bulutBelgesiYereleUygulanir() {
        val o = Ortam(uid = "u1")
        // Yerelde (bugüne ait) eski bir talep bayrağı ve sayaç var
        o.prefs().putString("day_id", "2026-154")
        o.prefs().putString("week_id", "2026-5-1")
        o.prefs().putBoolean("reward_claimed_daily_2026-154_eski", true)
        o.prefs().putInt("weekly_step_increment_count", 4)
        o.bulut.engelleyenYanit = {
            mapOf(
                "day_id" to "2026-154",
                "week_id" to "2026-5-1",
                "daily_step_finish_count" to 7L,
                "weekly_step_finish_count" to 9.0,
                "daily_learn_remainder_ms" to 45_000L,
                "daily_selection_nonce" to 2L,
                "daily_selected_mission_ids" to "finish_step_2,finish_lessons_5,chest_star_gain_2_daily",
                "rewardClaimedKeys" to listOf("reward_claimed_daily_2026-154_finish_step_2", 5, null),
            )
        }
        val s = o.depo.getSnapshot()
        assertEquals(1, o.bulut.engelleyenOkumaSayisi)
        assertEquals(7, s.dailyStepFinishCount)
        assertEquals(9, s.weeklyStepFinishCount)
        // Belgede olmayan sayaç yerel değerini korur
        assertEquals(4, s.weeklyStepIncrementCount)
        val p = o.prefs()
        assertEquals(45_000L, p.getLong("daily_learn_remainder_ms", -1))
        assertEquals(2, p.getInt("daily_selection_nonce", -1))
        // Belgede olmayan seçim alanı boş dizgeye çekilir
        assertEquals("", p.getString("weekly_selected_mission_ids"))
        // Talep bayrakları buluttakilerle DEĞİŞTİRİLİR
        assertFalse(p.contains("reward_claimed_daily_2026-154_eski"))
        assertTrue(o.depo.isMissionRewardClaimed(MissionWindow.DAILY, "finish_step_2"))
        // Uygulandıktan sonra engelleyen okuma tekrarlanmaz
        assertEquals(1, o.bulut.engelleyenOkumaSayisi)
        assertTrue(o.bulut.bekleyenOkumalar.isEmpty())
        // Buluttaki kayıtlı seçim kullanılır
        assertEquals(
            listOf("finish_step_2", "finish_lessons_5", "chest_star_gain_2_daily"),
            o.depo.selectedMissionsForDaily().map { it.id },
        )
    }

    @Test
    fun eskiGunluBulutBelgesiUygulaninccaSifirlanir() {
        val o = Ortam(uid = "u1")
        o.bulut.engelleyenYanit = {
            mapOf(
                "day_id" to "2026-150",
                "week_id" to "2026-4-25",
                "daily_step_finish_count" to 7L,
                "weekly_step_finish_count" to 9L,
                "daily_selected_mission_ids" to "finish_step_2,finish_lessons_5,chest_star_gain_2_daily",
            )
        }
        val s = o.depo.getSnapshot()
        assertEquals(0, s.dailyStepFinishCount)
        assertEquals(0, s.weeklyStepFinishCount)
        assertEquals("2026-154", o.prefs().getString("day_id"))
        assertEquals("2026-5-1", o.prefs().getString("week_id"))
        assertNull(o.prefs().getString("daily_selected_mission_ids"))
    }

    @Test
    fun engelleyenOkumaHatasindaYerelDevamEder() {
        val o = Ortam(uid = "u1")
        o.prefs().putInt("daily_step_finish_count", 3)
        o.prefs().putString("day_id", "2026-154")
        o.prefs().putString("week_id", "2026-5-1")
        val s = o.depo.getSnapshot()
        assertEquals(3, s.dailyStepFinishCount)
        assertEquals(1, o.bulut.engelleyenOkumaSayisi)
        // Eşzamansız okuma yine istenir
        assertEquals(1, o.bulut.bekleyenOkumalar.size)
        // applyCloudHydrate=false: buluta hiç gidilmez
        val o2 = Ortam(uid = "u1")
        o2.depo.getSnapshot(applyCloudHydrate = false)
        assertEquals(0, o2.bulut.engelleyenOkumaSayisi)
        assertTrue(o2.bulut.bekleyenOkumalar.isEmpty())
        assertTrue(o2.bulut.dinleyiciler.isEmpty())
    }

    @Test
    fun anlikDinleyiciBelgeyiUygular() {
        val o = Ortam(uid = "u1")
        o.bulut.engelleyenYanit = { null }
        o.depo.getSnapshot()
        // Belge yoktu → yerel durum yüklendi
        assertEquals(1, o.bulut.yazilanlar.size)
        val dinleyici = o.bulut.dinleyiciler.single().second
        dinleyici(mapOf("day_id" to "2026-154", "week_id" to "2026-5-1", "daily_chest_star_gain_count" to 3L))
        assertEquals(3, o.depo.getSnapshot().dailyChestStarGainCount)
    }

    @Test
    fun zorlaYuklemeBeklemedenYazar() {
        val o = Ortam(uid = "u1")
        o.depo.forceUploadStateToCloud()
        assertEquals(1, o.bulut.yazilanlar.size)
        assertTrue(o.bulut.bekleyenOkumalar.isEmpty())
        // Sonraki yazımlar da doğrudan gider (durum "uygulandı" sayılır)
        o.depo.recordStepFinishProgress()
        assertEquals(2, o.bulut.yazilanlar.size)
        assertEquals(0, o.bulut.engelleyenOkumaSayisi)
    }

    @Test
    fun kullaniciDegisinceEsitlemeDurumuSifirlanir() {
        val o = Ortam(uid = "u1")
        o.depo.forceUploadStateToCloud()
        o.depo.recordStepFinishProgress()
        o.bulut.uid = "u2"
        o.depo.recordStepFinishProgress()
        // Eski dinleyici kaldırıldı, yenisi kuruldu; u2 için önce okuma istenir
        assertEquals(1, o.bulut.kaldirilanDinleyici)
        assertEquals(listOf("u1", "u2"), o.bulut.dinleyiciler.map { it.first })
        assertEquals(1, o.bulut.bekleyenOkumalar.size)
        assertEquals(setOf("missions_progress_u1", "missions_progress_u2"), o.dosyalar.keys)
        assertEquals(1, o.prefs("missions_progress_u1").getInt("daily_step_finish_count", -1))
        assertEquals(1, o.prefs("missions_progress_u2").getInt("daily_step_finish_count", -1))
    }

    @Test
    fun yerelOnbellekTemizleme() {
        val o = Ortam(uid = "u1")
        o.prefs("missions_progress_guest").putInt("daily_step_finish_count", 5)
        o.depo.forceUploadStateToCloud()
        o.depo.recordStepFinishProgress()
        o.depo.clearLocalCache()
        assertTrue(o.prefs("missions_progress_guest").keys().isEmpty())
        assertTrue(o.prefs("missions_progress_u1").keys().isEmpty())
        // Eşitleme durumu sıfırlandığı için sonraki yazım yeniden okuma ister
        val oncekiYazim = o.bulut.yazilanlar.size
        o.depo.recordStepFinishProgress()
        assertEquals(oncekiYazim, o.bulut.yazilanlar.size)
        assertEquals(1, o.bulut.bekleyenOkumalar.size)
    }

    @Test
    fun engelleyenOkumaIstisnasiYutulur() {
        val bulut = SahteBulut("u1")
        assertFailsWith<IllegalStateException> { bulut.fetchStateBlocking("u1") }
        val o = Ortam(uid = "u1")
        // İstisna depo dışına sızmaz
        o.depo.selectedMissionsForDaily()
        o.depo.isMissionRewardClaimed(MissionWindow.DAILY, "x")
    }
}

class MissionUiLogicTest {
    private val sifir = MissionsProgressStore.Snapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    private fun gorev(id: String) = MissionQuestCatalog.all.first { it.id == id }

    @Test
    fun haftalikGeriSayim() {
        // 24 saatin altı: saat (aşağı yuvarlanır, en az 1)
        assertEquals(MissionCountdownLabel("missions_hours_short", 1), MissionUiLogic.formatWeeklyCountdown(0))
        assertEquals(MissionCountdownLabel("missions_hours_short", 1), MissionUiLogic.formatWeeklyCountdown(SAAT + 1))
        assertEquals(MissionCountdownLabel("missions_hours_short", 23), MissionUiLogic.formatWeeklyCountdown(GUN - 1))
        // 24 saat ve üzeri: gün (yukarı yuvarlanır)
        assertEquals(MissionCountdownLabel("missions_days_short", 1), MissionUiLogic.formatWeeklyCountdown(GUN))
        assertEquals(MissionCountdownLabel("missions_days_short", 2), MissionUiLogic.formatWeeklyCountdown(GUN + 1))
        assertEquals(MissionCountdownLabel("missions_days_short", 5), MissionUiLogic.formatWeeklyCountdown(4 * GUN + 12 * SAAT))
        assertEquals(MissionCountdownLabel("missions_days_short", 7), MissionUiLogic.formatWeeklyCountdown(7 * GUN))
        assertEquals(MissionCountdownLabel("missions_hours_short", 12), MissionUiLogic.dailyCountdownLabel(12))
    }

    @Test
    fun gorevSatiriVerisi() {
        val g = gorev("finish_lessons_5")
        val bekleyen = MissionUiLogic.questData(g, MissionWindow.DAILY, sifir.copy(dailyStepIncrementCount = 2), isClaimed = false)
        assertEquals("finish_lessons_5", bekleyen.missionId)
        assertEquals("mission_finish_five_lessons", bekleyen.titleResId)
        assertEquals(2, bekleyen.progress)
        assertEquals(5, bekleyen.target)
        assertEquals("new_chest_close_ic1", bekleyen.iconRes)
        assertEquals(40, bekleyen.percent)
        assertEquals(MissionQuestState.PENDING, bekleyen.state)
        assertEquals(2 to 5, bekleyen.progressLabelArgs)
        assertFalse(bekleyen.canClaim)

        // Sayaç hedefi aşarsa hedefe kırpılır
        val bitti = MissionUiLogic.questData(g, MissionWindow.DAILY, sifir.copy(dailyStepIncrementCount = 9), isClaimed = false)
        assertEquals(5, bitti.progress)
        assertEquals(100, bitti.percent)
        assertEquals(MissionQuestState.DONE, bitti.state)
        assertTrue(bitti.canClaim)

        val alindi = bitti.copy(isClaimed = true)
        assertEquals(MissionQuestState.CLAIMED, alindi.state)
        assertFalse(alindi.canClaim)

        // Haftalık ikon ve yüzde aşağı yuvarlama (1/3 → 33)
        val haftalik = MissionUiLogic.questData(gorev("finish_lessons_20"), MissionWindow.WEEKLY, sifir.copy(weeklyStepIncrementCount = 7), false)
        assertEquals("new_chest_close_ic2", haftalik.iconRes)
        assertEquals(35, haftalik.percent)
        assertEquals(33, MissionQuestData("x", "t", 1, 3, "i", MissionWindow.DAILY, false).percent)
        // Hedef 0 ise bölen 1 alınır
        assertEquals(0, MissionQuestData("x", "t", 0, 0, "i", MissionWindow.DAILY, false).percent)
    }

    @Test
    fun sandikNadirligi() {
        assertEquals(ChestRarity.RARE, MissionUiLogic.chestStartRarityForWindow(MissionWindow.WEEKLY))
        assertEquals(ChestRarity.COMMON, MissionUiLogic.chestStartRarityForWindow(MissionWindow.DAILY))
    }

    @Test
    fun animasyonluGorevListesi() {
        val haftalik = listOf(gorev("finish_lessons_20"), gorev("chest_record_break_1"), gorev("finish_step_5_weekly"))
        val gunluk = listOf(gorev("finish_step_2"), gorev("finish_lessons_5"), gorev("learn_for_10_minutes"))
        val once = sifir.copy(weeklyStepIncrementCount = 3, dailyStepIncrementCount = 3, dailyStepFinishCount = 2, weeklyStepFinishCount = 2, weeklyChestRecordBreakCount = 1)
        val sonra = once.copy(
            weeklyStepIncrementCount = 4, dailyStepIncrementCount = 4, // ikisi de değişti
            dailyStepFinishCount = 3, weeklyStepFinishCount = 3, // günlük 2/2 dolu → görünmez; haftalık 2→3 görünür
            weeklyChestRecordBreakCount = 2, // 1/1 dolu → görünmez
        )
        val sorulan = mutableListOf<Pair<MissionWindow, String>>()
        val (h, g) = MissionUiLogic.animatedQuests(haftalik, gunluk, once, sonra) { w, id ->
            sorulan.add(w to id)
            id == "finish_lessons_5"
        }
        assertEquals(listOf("finish_lessons_20", "finish_step_5_weekly"), h.map { it.missionId })
        assertEquals(listOf("finish_lessons_5"), g.map { it.missionId })
        // Sıra dizini haftalıktan başlar, günlükte devam eder
        assertEquals(listOf(0, 1), h.map { it.staggerIndex })
        assertEquals(listOf(2), g.map { it.staggerIndex })
        assertEquals(3 to 4, h[0].fromCount to h[0].toCount)
        assertEquals("new_chest_close_ic2", h[0].iconRes)
        assertEquals("new_chest_close_ic1", g[0].iconRes)
        assertTrue(g[0].isClaimed)
        assertFalse(h[0].isClaimed)
        // Talep durumu yalnızca değişen görevler için sorulur
        assertEquals(
            listOf(MissionWindow.WEEKLY to "finish_lessons_20", MissionWindow.WEEKLY to "finish_step_5_weekly", MissionWindow.DAILY to "finish_lessons_5"),
            sorulan,
        )
        // Hiç değişiklik yoksa iki liste de boş
        val (bosH, bosG) = MissionUiLogic.animatedQuests(haftalik, gunluk, once, once) { _, _ -> false }
        assertTrue(bosH.isEmpty() && bosG.isEmpty())
    }

    @Test
    fun animasyonluGorevHesaplari() {
        val q = AnimatedQuest("m", MissionWindow.DAILY, false, "t", "i", target = 5, fromCount = 3, toCount = 5, staggerIndex = 2)
        assertEquals(60f, q.startPct)
        assertEquals(100f, q.endPct)
        assertTrue(q.canClaim)
        assertTrue(q.shouldAnimate)
        assertEquals(240L, q.startDelayMs)
        // Kare başına tahmini sayaç: yuvarlanır
        assertEquals(3, q.estimatedCountAt(60f))
        assertEquals(3, q.estimatedCountAt(69.9f))
        assertEquals(4, q.estimatedCountAt(70f))
        assertEquals(5, q.estimatedCountAt(90f))
        assertEquals(5, q.estimatedCountAt(150f))
        assertEquals(0, q.estimatedCountAt(-10f))
        assertEquals(MissionQuestState.PENDING, q.stateAt(80f))
        assertEquals(MissionQuestState.DONE, q.stateAt(90f))
        assertEquals(MissionQuestState.CLAIMED, q.copy(isClaimed = true).stateAt(10f))
        assertFalse(q.copy(isClaimed = true).canClaim)
        assertFalse(q.copy(toCount = 4).canClaim)
        assertFalse(q.copy(toCount = 3).shouldAnimate)
        // Hedef 0 → 1 kabul edilir
        val sifirHedef = q.copy(target = 0, fromCount = 0, toCount = 0)
        assertEquals(0f, sifirHedef.endPct)
        assertFalse(sifirHedef.canClaim)
        assertEquals(2800L, MissionUiLogic.PROGRESS_ANIM_DURATION_MS)
        assertEquals("chest_closed", MissionUiLogic.RESULT_CHEST_CLOSED)
    }

    @Test
    fun tamamlananGorevSayisi() {
        val gunluk = listOf(gorev("finish_step_2"), gorev("finish_lessons_5"))
        val haftalik = listOf(gorev("chest_record_break_1"), gorev("finish_step_5_weekly"))
        val once = sifir.copy(dailyStepFinishCount = 1, weeklyStepFinishCount = 4, dailyStepIncrementCount = 5, weeklyChestRecordBreakCount = 0)
        val sonra = once.copy(dailyStepFinishCount = 2, weeklyStepFinishCount = 5, dailyStepIncrementCount = 6, weeklyChestRecordBreakCount = 1)
        // Günlük: finish_step_2 (1→2 tamam), finish_lessons_5 (zaten 5 → sayılmaz)
        // Haftalık: rekor (0→1 tamam), finish_step_5_weekly (4→5 tamam)
        assertEquals(3, MissionUiLogic.countCompletedMissions(gunluk, haftalik, once, sonra))
        assertEquals(0, MissionUiLogic.countCompletedMissions(gunluk, haftalik, once, once))
        assertEquals(0, MissionUiLogic.countCompletedMissions(emptyList(), emptyList(), once, sonra))
    }

    @Test
    fun sayacFarki() {
        assertFalse(MissionUiLogic.missionCounterDelta(sifir, sifir))
        val alanlar = listOf<(MissionsProgressStore.Snapshot) -> MissionsProgressStore.Snapshot>(
            { it.copy(dailyStepFinishCount = 1) }, { it.copy(weeklyStepFinishCount = 1) },
            { it.copy(dailyStepIncrementCount = 1) }, { it.copy(weeklyStepIncrementCount = 1) },
            { it.copy(dailyPerfectStepIncrementCount = 1) }, { it.copy(weeklyPerfectStepIncrementCount = 1) },
            { it.copy(dailyChestRecordBreakCount = 1) }, { it.copy(weeklyChestRecordBreakCount = 1) },
            { it.copy(dailyChestStarGainCount = 1) }, { it.copy(weeklyChestStarGainCount = 1) },
            { it.copy(dailyLearnMinutesCount = 1) }, { it.copy(weeklyLearnMinutesCount = 1) },
        )
        alanlar.forEachIndexed { i, degistir ->
            assertTrue(MissionUiLogic.missionCounterDelta(sifir, degistir(sifir)), "alan $i")
        }
    }

    @Test
    fun dersKurallari() {
        assertTrue(MissionUiLogic.isPerfectLesson(100f))
        assertFalse(MissionUiLogic.isPerfectLesson(99.99f))
        val ders = OyunLessonItemData(type = OyunLessonItem.TYPE_LESSON, title = "D")
        assertTrue(MissionUiLogic.shouldIncrementDartProgress(ders, 100f))
        assertFalse(MissionUiLogic.shouldIncrementDartProgress(ders, 90f))
        assertFalse(MissionUiLogic.shouldIncrementDartProgress(ders.copy(stepIsFinish = true), 100f))
        assertFalse(MissionUiLogic.shouldIncrementDartProgress(ders.copy(type = OyunLessonItem.TYPE_CHEST), 100f))
        assertFalse(MissionUiLogic.shouldIncrementDartProgress(null, 100f))

        val sandik = OyunLessonItemData(type = OyunLessonItem.TYPE_CHEST, title = "S")
        assertTrue(MissionUiLogic.justFinishedChestForRating(sandik))
        assertFalse(MissionUiLogic.justFinishedChestForRating(sandik.copy(stepIsFinish = true)))
        assertFalse(MissionUiLogic.justFinishedChestForRating(ders))
        assertFalse(MissionUiLogic.justFinishedChestForRating(null))
    }

    @Test
    fun ilerlemeCubuguKatmani() {
        // Genişlik yoksa hiçbir şey yapılmaz
        assertNull(MissionUiLogic.missionProgressOverlay(0, 50f, done = false, claimed = false, startInset = 4, endGap = 6))
        assertNull(MissionUiLogic.dailyQuestionProgressOverlay(-1, 50f, complete = false, startInset = 4, endGap = 6))

        val yari = assertNotNull(MissionUiLogic.missionProgressOverlay(200, 50f, done = false, claimed = false, startInset = 4, endGap = 6))
        assertEquals(MissionProgressOverlay(100, true, 90, true, "mission_progress_fill_blue", "mission_progress_shine_blue"), yari)

        val altin = assertNotNull(MissionUiLogic.missionProgressOverlay(200, 100f, done = true, claimed = false, startInset = 4, endGap = 6))
        assertEquals("mission_progress_fill_gold", altin.fillDrawable)
        assertEquals("mission_progress_shine_gold", altin.shineDrawable)
        assertEquals(200, altin.fillWidth)

        // Ödül alındıysa (done olsa bile) "claimed" kaynakları ve parlama gizli
        val alindi = assertNotNull(MissionUiLogic.missionProgressOverlay(200, 100f, done = true, claimed = true, startInset = 4, endGap = 6))
        assertEquals("mission_progress_fill_claimed", alindi.fillDrawable)
        assertEquals("mission_progress_shine_claimed", alindi.shineDrawable)
        assertFalse(alindi.shineVisible)

        // Yüzde 0..100'e kırpılır; çok dar dolguda parlama görünmez
        val sifirYuzde = assertNotNull(MissionUiLogic.missionProgressOverlay(200, -5f, false, false, 4, 6))
        assertEquals(0, sifirYuzde.fillWidth)
        assertFalse(sifirYuzde.fillVisible)
        assertFalse(sifirYuzde.shineVisible)
        assertEquals(200, assertNotNull(MissionUiLogic.missionProgressOverlay(200, 250f, false, false, 4, 6)).fillWidth)
        val dar = assertNotNull(MissionUiLogic.missionProgressOverlay(200, 5f, false, false, 4, 6))
        assertEquals(10, dar.fillWidth)
        assertEquals(0, dar.shineWidth)
        assertTrue(dar.fillVisible)
        assertFalse(dar.shineVisible)
        // Aşağı yuvarlama: 199 * 33.3 / 100 = 66.26 → 66
        assertEquals(66, assertNotNull(MissionUiLogic.missionProgressOverlay(199, 33.3f, false, false, 0, 0)).fillWidth)

        val soru = assertNotNull(MissionUiLogic.dailyQuestionProgressOverlay(300, 100f / 3, complete = false, startInset = 4, endGap = 6))
        assertEquals("daily_question_progress_fill", soru.fillDrawable)
        assertEquals("daily_question_progress_shine", soru.shineDrawable)
        assertEquals(100, soru.fillWidth)
        assertEquals(90, soru.shineWidth)
        val soruTamam = assertNotNull(MissionUiLogic.dailyQuestionProgressOverlay(300, 100f, complete = true, startInset = 4, endGap = 6))
        assertEquals("daily_question_progress_fill_complete", soruTamam.fillDrawable)
        assertEquals("daily_question_progress_shine_complete", soruTamam.shineDrawable)
        assertEquals(300, soruTamam.fillWidth)
    }
}
