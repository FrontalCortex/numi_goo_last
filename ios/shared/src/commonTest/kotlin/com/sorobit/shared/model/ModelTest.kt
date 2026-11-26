// Soru-mesaj / şikâyet / kısıtlama modelleri ile oturum, FCM ve takip kurallarının Android ile
// aynı alan adlarını ve sonuçları ürettiğini doğrular. Beklenen değerler Android kaynağından
// (model/*.kt, UserAskQuestionRestriction.kt, NotificationFragment.kt, SessionDeviceManager.kt,
// AccountDeletionHelper.kt, MyFirebaseMessagingService.kt, AddFriendFragment.kt,
// FollowersFollowingFragment.kt) elle türetilmiştir.
package com.sorobit.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudentQuestionTest {
    @Test
    fun varsayilanlar() {
        val q = StudentQuestion.fromMap("q1", emptyMap())
        assertEquals(StudentQuestion(id = "q1"), q)
        assertEquals("image", q.mediaType)
        assertEquals("pending", q.status)
        assertNull(q.createdAt)
        assertNull(q.deletedForUids)
        assertEquals("questions/q1", StudentQuestion.docPath("q1"))
        assertEquals("questions/q1/messages", StudentQuestion.messagesPath("q1"))
    }

    @Test
    fun haritadanOkuma() {
        val q = StudentQuestion.fromMap(
            "q2",
            mapOf(
                "studentUid" to "s1",
                "studentEmail" to "a@b.co",
                "screenshotStoragePath" to "p/x.png",
                "screenshotUrl" to "http://x",
                "mediaType" to "video",
                "videoStoragePath" to "v/x.mp4",
                "videoUrl" to "http://v",
                "videoDurationSec" to 12L,
                "message" to "merhaba",
                "previewText" to "mer",
                "lastMessageAt" to 2000L,
                "createdAt" to 1000L,
                "status" to "claimed",
                "claimedByTeacherUid" to "t1",
                "claimedAt" to 1500L,
                "resolvedAt" to null,
                "deletedForUids" to listOf("u1", 5, "u2"),
                "bilinmeyen" to 1,
            ),
        )
        assertEquals(
            StudentQuestion(
                id = "q2", studentUid = "s1", studentEmail = "a@b.co", screenshotStoragePath = "p/x.png",
                screenshotUrl = "http://x", mediaType = "video", videoStoragePath = "v/x.mp4", videoUrl = "http://v",
                videoDurationSec = 12, message = "merhaba", previewText = "mer", lastMessageAt = 2000L,
                createdAt = 1000L, status = "claimed", claimedByTeacherUid = "t1", claimedAt = 1500L,
                resolvedAt = null, deletedForUids = listOf("u1", "u2"),
            ),
            q,
        )
        // Gidiş-dönüş
        assertEquals(q, StudentQuestion.fromMap("q2", q.toMap()))
        // Kimlik alan olarak yazılmaz; 17 alan
        assertEquals(17, q.toMap().size)
        assertFalse(q.toMap().containsKey("id"))
        assertTrue(q.toMap().containsKey("createdAt"))
        // Tipi uyuşmayan alan varsayılana düşer
        assertEquals("pending", StudentQuestion.fromMap("x", mapOf("status" to 5)).status)
    }

    @Test
    fun sabitler() {
        assertEquals("pending", StudentQuestion.STATUS_PENDING)
        assertEquals("claimed", StudentQuestion.STATUS_CLAIMED)
        assertEquals("resolved", StudentQuestion.STATUS_RESOLVED)
        assertEquals("image", StudentQuestion.MEDIA_TYPE_IMAGE)
        assertEquals("video", StudentQuestion.MEDIA_TYPE_VIDEO)
        assertEquals("claimedByTeacherUid", StudentQuestion.FIELD_CLAIMED_BY_TEACHER_UID)
    }
}

class QuestionMessageTest {
    @Test
    fun bekleyenVeOkunmamis() {
        assertTrue(QuestionMessage(id = "pending_123").isPending())
        assertFalse(QuestionMessage(id = "abc").isPending())
        assertFalse(QuestionMessage().isPending())
        assertTrue(QuestionMessage(senderUid = "o").isUnreadFor("ben"))
        assertFalse(QuestionMessage(senderUid = "ben").isUnreadFor("ben")) // kendi mesajım
        assertFalse(QuestionMessage(senderUid = "o", readAt = 5L).isUnreadFor("ben"))
        assertEquals(100L, QuestionMessage.UNREAD_SCAN_LIMIT)
    }

    @Test
    fun haritaEslemesi() {
        assertEquals(QuestionMessage(), QuestionMessage.fromMap(emptyMap()))
        assertEquals("text", QuestionMessage().type)
        val m = QuestionMessage(
            id = "m1", senderUid = "u", senderRole = "TEACHER", type = "audio", textContent = null,
            mediaStoragePath = "a/b", mediaUrl = "http://m", thumbnailUrl = "http://t", mediaSizeBytes = 5_000_000_000L,
            clientId = "c1", createdAt = 10L, deliveredAt = 20L, readAt = 30L, deletedForUids = listOf("x"),
        )
        // `id` gerçek bir alandır (@DocumentId değil)
        assertEquals("m1", m.toMap()["id"])
        assertEquals(14, m.toMap().size)
        assertEquals(m, QuestionMessage.fromMap(m.toMap()))
        assertEquals(7L, QuestionMessage.fromMap(mapOf("mediaSizeBytes" to 7)).mediaSizeBytes)
        assertEquals(listOf("text", "audio", "video", "image"),
            listOf(QuestionMessage.TYPE_TEXT, QuestionMessage.TYPE_AUDIO, QuestionMessage.TYPE_VIDEO, QuestionMessage.TYPE_IMAGE))
        assertEquals(PendingUploadMeta("q", "image", "/f", null, "alt"), PendingUploadMeta("q", "image", "/f", null, "alt"))
    }
}

class MessageReportTest {
    private val rapor = MessageReport(
        id = "r1", questionId = "q", messageId = "m", reportedByUid = "a", reportedUserUid = "b",
        messagePreview = "kötü söz", reason = "harassment", type = "image", mediaUrl = "http://m",
        thumbnailUrl = null, reportedAt = 100L,
    )

    @Test
    fun nedenEtiketleri() {
        assertEquals("Taciz veya zorbalık", MessageReport.reasonLabel("harassment"))
        assertEquals("Spam", MessageReport.reasonLabel("spam"))
        assertEquals("Uygunsuz içerik", MessageReport.reasonLabel("inappropriate"))
        assertEquals("Diğer", MessageReport.reasonLabel("other"))
        assertEquals("Belirtilmedi", MessageReport.reasonLabel(""))
        assertEquals("Belirtilmedi", MessageReport.reasonLabel("SPAM"))
    }

    @Test
    fun haritaEslemesi() {
        assertEquals(MessageReport(id = "x"), MessageReport.fromMap("x", emptyMap()))
        assertEquals("pending", MessageReport().status)
        assertEquals("text", MessageReport().type)
        assertEquals(rapor, MessageReport.fromMap("r1", rapor.toMap()))
        assertEquals(13, rapor.toMap().size)
        assertEquals("messageReports", MessageReport.COLLECTION)
    }

    @Test
    fun durumGuncellemeleri() {
        assertEquals(
            mapOf("status" to "dismissed", "handledByUid" to "ogr", "handledAt" to 500L),
            MessageReport.dismissUpdate("ogr", 500L),
        )
        assertEquals(
            mapOf("status" to "action_taken", "handledByUid" to "ogr", "handledAt" to 500L),
            MessageReport.actionTakenUpdate("ogr", 500L),
        )
    }

    @Test
    fun kisitlamaGuncellemesi() {
        // Bitiş: şimdiki saniye (aşağı yuvarlanmış) + gün
        assertEquals(1_700_259_200_000L, RestrictedUser.restrictedUntilMs(1_700_000_000_999L, 3))
        assertEquals(86_400_000L, RestrictedUser.restrictedUntilMs(0L, 1))

        val baglam = mapOf(
            "restrictionReason" to "harassment",
            "restrictionMessagePreview" to "kötü söz",
            "restrictionMessageType" to "image",
            "restrictionMediaUrl" to "http://m",
            "restrictionThumbnailUrl" to null,
            "restrictedAt" to 1_700_000_000_999L,
            "restrictedByUid" to "ogr",
        )
        // Kalıcı yasak: banned = true, restrictedUntil silinir
        val kalici = RestrictedUser.restrictionUpdate(rapor, "ogr", 1_700_000_000_999L, days = null)
        assertEquals(baglam + mapOf("banned" to true), kalici.alanlar)
        assertEquals(listOf("restrictedUntil"), kalici.silinecekAlanlar)
        // Süreli kısıtlama
        val sureli = RestrictedUser.restrictionUpdate(rapor, "ogr", 1_700_000_000_999L, days = 7)
        assertEquals(baglam + mapOf("restrictedUntil" to 1_700_604_800_000L, "banned" to false), sureli.alanlar)
        assertTrue(sureli.silinecekAlanlar.isEmpty())
    }

    @Test
    fun kisitliKullanici() {
        assertEquals(RestrictedUser(uid = "u"), RestrictedUser.fromMap("u", emptyMap()))
        val u = RestrictedUser.fromMap(
            "u",
            mapOf("name" to "Ali", "banned" to true, "restrictedUntil" to 9L, "restrictionReason" to "spam",
                "restrictionMessageType" to "text", "restrictedAt" to 3L, "restrictedByUid" to "t"),
        )
        assertEquals(
            RestrictedUser(uid = "u", name = "Ali", banned = true, restrictedUntil = 9L, restrictionReason = "spam",
                restrictionMessageType = "text", restrictedAt = 3L, restrictedByUid = "t"),
            u,
        )
    }

    @Test
    fun soruSormaKisiti() {
        val r = UserAskQuestionRestriction
        assertTrue(r.isRestricted(banned = true, restrictedUntilMs = null, nowMs = 100))
        assertTrue(r.isRestricted(banned = false, restrictedUntilMs = 101, nowMs = 100))
        assertFalse(r.isRestricted(banned = false, restrictedUntilMs = 100, nowMs = 100)) // eşitse kısıtlı değil
        assertFalse(r.isRestricted(banned = null, restrictedUntilMs = 99, nowMs = 100))
        assertFalse(r.isRestricted(banned = null, restrictedUntilMs = null, nowMs = 100))
        assertTrue(r.isRestricted(mapOf("banned" to true), 100))
        assertTrue(r.isRestricted(mapOf("restrictedUntil" to 200L), 100))
        assertFalse(r.isRestricted(mapOf("banned" to "true", "restrictedUntil" to 50L), 100))
        assertFalse(r.isRestricted(emptyMap(), 100))
    }
}

class SessionDeviceRulesTest {
    private val k = SessionDeviceRules

    @Test
    fun tekCihazKarari() {
        val simdi = 1_000_000L
        fun karar(aktif: String?, son: Long?, giris: Boolean = true, dokuman: Boolean = true) =
            k.decide(giris, dokuman, aktif, son, deviceId = "ben", nowMs = simdi)

        assertEquals(SessionDeviceRules.Decision.REDIRECT_TO_LOGIN, karar("ben", simdi, giris = false))
        assertEquals(SessionDeviceRules.Decision.SIGN_OUT_AND_REDIRECT, karar("ben", simdi, dokuman = false))
        // Oturum sahipsiz: al ve izin ver
        assertEquals(SessionDeviceRules.Decision.CLAIM_AND_ALLOW, karar(null, simdi))
        assertEquals(SessionDeviceRules.Decision.CLAIM_AND_ALLOW, karar("", null))
        assertEquals(SessionDeviceRules.Decision.CLAIM_AND_ALLOW, karar("  ", simdi))
        // Zaten bu cihazda
        assertEquals(SessionDeviceRules.Decision.ALLOW, karar("ben", simdi))
        assertEquals(SessionDeviceRules.Decision.ALLOW, karar("ben", null))
        assertEquals(SessionDeviceRules.Decision.ALLOW, karar("ben", 0L))
        // Başka cihazda canlı oturum: tam 150 sn bayat SAYILMAZ
        assertEquals(SessionDeviceRules.Decision.REJECT_OTHER_DEVICE, karar("baska", simdi))
        assertEquals(SessionDeviceRules.Decision.REJECT_OTHER_DEVICE, karar("baska", simdi - 150_000))
        // 150 sn'den fazla ya da zaman yok: bayat → devral
        assertEquals(SessionDeviceRules.Decision.CLAIM_AND_ALLOW, karar("baska", simdi - 150_001))
        assertEquals(SessionDeviceRules.Decision.CLAIM_AND_ALLOW, karar("baska", null))
    }

    @Test
    fun oturumYardimcilari() {
        assertEquals(60_000L, k.HEARTBEAT_INTERVAL_MS)
        assertEquals(150_000L, k.SESSION_STALE_MS)
        assertTrue(k.needsNewDeviceId(null)); assertTrue(k.needsNewDeviceId(" ")); assertFalse(k.needsNewDeviceId("d"))
        assertEquals(mapOf("activeDeviceId" to "d", "activeSessionUpdatedAt" to 5L), k.claimUpdate("d", 5L))
        assertEquals(mapOf("activeDeviceId" to null, "activeSessionUpdatedAt" to 5L), k.releaseUpdate(5L))
        assertTrue(k.shouldRelease("d", "d")); assertFalse(k.shouldRelease("x", "d")); assertFalse(k.shouldRelease(null, "d"))
        assertTrue(k.shouldForceSignOut("x", "d"))
        assertFalse(k.shouldForceSignOut("d", "d"))
        assertFalse(k.shouldForceSignOut(null, "d"))
        assertFalse(k.shouldForceSignOut("", "d"))
        assertEquals("Bir kaç dakika sonra tekrar dene.", k.MESSAGE_TRY_AGAIN_LATER)
    }

    @Test
    fun hesapSilme() {
        val a = AccountDeletionRules
        assertFalse(a.requiresRecentLogin(1_000_000L - 270_000L, 1_000_000L))
        assertTrue(a.requiresRecentLogin(1_000_000L - 270_001L, 1_000_000L))
        assertTrue(a.requiresRecentLogin(null, 1_000_000L))
        assertFalse(a.requiresRecentLogin(null, 270_000L))
        assertEquals(
            listOf("badgeProgress" to "state", "cupWayProgress" to "progress", "abacusCustomization" to "state",
                "missionProgress" to "state", "dailyQuestion" to "current"),
            a.SINGLE_DOC_SUBCOLLECTIONS,
        )
        assertEquals(listOf("cupHistory", "lessonSuccessRateState"), a.LEAF_SUBCOLLECTIONS)
        assertEquals(450, a.DELETE_BATCH_SIZE)
        assertEquals("users/o/following/ben", a.followingRefPath("o", "ben"))
        assertEquals("users/o/followers/ben", a.followerRefPath("o", "ben"))
        assertEquals(5, a.STEP_ORDER.size)
    }
}

class FcmRulesTest {
    private val f = FcmRules

    @Test
    fun gelenYuk() {
        assertNull(f.parseIncoming(emptyMap(), "Sorobit"))
        assertNull(f.parseIncoming(mapOf("recipientUid" to "u"), "Sorobit")) // questionId yok
        assertNull(f.parseIncoming(mapOf("questionId" to "q"), "Sorobit")) // recipientUid yok
        assertEquals(
            FcmRules.Incoming("Başlık", "Gövde", "q", "m", "u"),
            f.parseIncoming(mapOf("title" to "Başlık", "senderName" to "Ali", "body" to "Gövde", "messagePreview" to "ön",
                "questionId" to "q", "messageId" to "m", "recipientUid" to "u"), "Sorobit"),
        )
        assertEquals(
            FcmRules.Incoming("Ali", "ön", "q", null, "u"),
            f.parseIncoming(mapOf("senderName" to "Ali", "messagePreview" to "ön", "questionId" to "q", "recipientUid" to "u"), "Sorobit"),
        )
        assertEquals(
            FcmRules.Incoming("Sorobit", "", "q", null, "u"),
            f.parseIncoming(mapOf("questionId" to "q", "recipientUid" to "u"), "Sorobit"),
        )
    }

    @Test
    fun gosterimKurali() {
        assertTrue(f.shouldShowNotification("u", "u", true))
        assertFalse(f.shouldShowNotification("u", "u", false))
        assertFalse(f.shouldShowNotification("v", "u", true))
        assertFalse(f.shouldShowNotification(null, "u", true))
        assertTrue(f.isProfileCompleteForToken(mapOf("email" to "a@b.co", "role" to "STUDENT")))
        assertFalse(f.isProfileCompleteForToken(mapOf("email" to " ", "role" to "STUDENT")))
        assertFalse(f.isProfileCompleteForToken(mapOf("email" to "a@b.co")))
        assertFalse(f.isProfileCompleteForToken(null))
    }

    @Test
    fun cihazListesi() {
        val a = mapOf("deviceId" to "d1", "token" to "t1", "updatedAt" to 1L)
        val b = mapOf("deviceId" to "d2", "token" to "t2", "updatedAt" to 2L)
        fun yeni(d: String, t: String) = mapOf("deviceId" to d, "token" to t, "updatedAt" to 9L)

        // Aynı cihaz: eski girdi çıkar, yenisi sona eklenir
        assertEquals(
            mapOf("fcmDevices" to listOf(b, yeni("d1", "t9")), "fcmTokenUpdatedAt" to 9L, "fcmToken" to "t9"),
            f.saveTokenUpdate(listOf(a, b), "d1", "t9", 9L),
        )
        // Üçüncü cihaz: en eski düşer (en çok 2)
        assertEquals(
            mapOf("fcmDevices" to listOf(b, yeni("d3", "t3")), "fcmTokenUpdatedAt" to 9L, "fcmToken" to "t3"),
            f.saveTokenUpdate(listOf(a, b), "d3", "t3", 9L),
        )
        assertEquals(listOf(yeni("d1", "t1")), f.saveTokenUpdate(emptyList(), "d1", "t1", 9L)["fcmDevices"])

        // Temizleme: kalan son girdinin belirteci
        assertEquals(
            mapOf("fcmDevices" to listOf(a), "fcmTokenUpdatedAt" to 9L, "fcmToken" to "t1"),
            f.clearTokenUpdate(listOf(a, b), "d2", 9L),
        )
        assertEquals(
            mapOf("fcmDevices" to emptyList<Any>(), "fcmTokenUpdatedAt" to 9L, "fcmToken" to null),
            f.clearTokenUpdate(listOf(a), "d1", 9L),
        )
        assertEquals(listOf<Map<*, *>>(a), f.devicesFrom(listOf(a, "x", 5)))
        assertTrue(f.devicesFrom(null).isEmpty())
        assertTrue(f.devicesFrom("x").isEmpty())
        assertEquals("messages", f.CHANNEL_ID_MESSAGES)
        assertEquals(7, f.MAX_INBOX_LINES)
    }
}

class FollowRulesTest {
    private val f = FollowRules

    @Test
    fun yollarVeYazimlar() {
        assertEquals("users/hedef/followers/ben", f.followerDocPath("hedef", "ben"))
        assertEquals("users/ben/following/hedef", f.followingDocPath("ben", "hedef"))
        assertEquals(mapOf("userId" to "123AB", "name" to "Ali", "followedAt" to 7L), f.followDoc("123AB", "Ali", 7L))

        // AddFriendFragment: hedefin takipçi kaydına BEN, benim takip kaydıma HEDEF
        assertEquals(
            mapOf(
                "users/hedef/followers/ben" to mapOf("userId" to "BENID", "name" to "Ben", "followedAt" to 7L),
                "users/ben/following/hedef" to mapOf("userId" to "HEDEFID", "name" to "Hedef", "followedAt" to 7L),
            ),
            f.followWrites("ben", "BENID", "Ben", "hedef", "HEDEFID", "Hedef", 7L),
        )
        // FollowersFollowingFragment (Android davranışı): veriler çapraz yazılır
        assertEquals(
            mapOf(
                "users/hedef/followers/ben" to mapOf("userId" to "HEDEFID", "name" to "Hedef", "followedAt" to 7L),
                "users/ben/following/hedef" to mapOf("userId" to "BENID", "name" to "Ben", "followedAt" to 7L),
            ),
            f.followBackWrites("ben", "BENID", "Ben", "hedef", "HEDEFID", "Hedef", 7L),
        )
        assertEquals(0, f.TAB_FOLLOWING); assertEquals(1, f.TAB_FOLLOWERS)
        assertEquals("publicProfiles", f.COLLECTION_PUBLIC_PROFILES)
    }

    @Test
    fun aramaSonucu() {
        assertEquals(
            FollowRules.FriendSearchResult("uid1", "Ali", "111AA", 3),
            f.parseSearchResult("uid1", mapOf("name" to "Ali", "userId" to "111AA", "selectedAvatar" to 3L), "999ZZ"),
        )
        assertEquals(0, f.parseSearchResult("uid1", mapOf("name" to "Ali", "userId" to "111AA"), "999ZZ")!!.selectedAvatar)
        assertNull(f.parseSearchResult("uid1", mapOf("userId" to "111AA"), "999ZZ")) // ad yok
        assertNull(f.parseSearchResult("uid1", mapOf("name" to "Ali"), "999ZZ")) // kimlik yok
        assertNull(f.parseSearchResult("uid1", mapOf("name" to "Ali", "userId" to "999ZZ"), "999ZZ")) // kendisi
    }
}
