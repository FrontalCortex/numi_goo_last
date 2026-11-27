// Kaynak (Android):
//  - app/src/main/java/com/example/app/SessionDeviceManager.kt (tek cihaz oturumu kuralı)
//  - app/src/main/java/com/example/app/AccountDeletionHelper.kt (hesap silme ön koşulu ve
//    silinecek alt koleksiyonların listesi)
//  - app/src/main/java/com/example/app/MyFirebaseMessagingService.kt (FCM cihaz listesi kuralları,
//    bildirim yükü alanları)
//
// Firestore / Auth / FCM çağrıları taşınmadı; yalnızca karar kuralları, alan adları ve yollar var.
// Zaman değerleri epoch ms (Long).
package com.sorobit.shared.model

/** Tek cihaz oturumu kuralı. */
object SessionDeviceRules {
    const val PREFS_DEVICE = "device_prefs"
    const val KEY_DEVICE_ID = "device_id"
    const val HEARTBEAT_INTERVAL_MS = 60_000L
    const val SESSION_STALE_MS = 150_000L

    const val COLLECTION_USERS = "users"
    const val FIELD_ACTIVE_DEVICE_ID = "activeDeviceId"
    const val FIELD_ACTIVE_SESSION_UPDATED_AT = "activeSessionUpdatedAt"

    const val MESSAGE_TRY_AGAIN_LATER = "Bir kaç dakika sonra tekrar dene."

    /** Kayıtlı cihaz kimliği boş/yoksa yenisi üretilmelidir (UUID; platform üretir). */
    fun needsNewDeviceId(existing: String?): Boolean = existing.isNullOrBlank()

    /** Android: claimActiveSessionIfLoggedIn'in yazdığı güncelleme. */
    fun claimUpdate(deviceId: String, nowMs: Long): Map<String, Any?> = mapOf(
        FIELD_ACTIVE_DEVICE_ID to deviceId,
        FIELD_ACTIVE_SESSION_UPDATED_AT to nowMs,
    )

    /** Android: releaseActiveSessionIfOwned'ın yazdığı güncelleme (cihaz kimliği null yapılır). */
    fun releaseUpdate(nowMs: Long): Map<String, Any?> = mapOf(
        FIELD_ACTIVE_DEVICE_ID to null,
        FIELD_ACTIVE_SESSION_UPDATED_AT to nowMs,
    )

    /** Oturum yalnızca hâlâ bu cihaza aitse bırakılır. */
    fun shouldRelease(activeDeviceId: String?, deviceId: String): Boolean = activeDeviceId == deviceId

    /**
     * Canlı dinleyici kuralı: başka bir cihaz oturumu ele geçirdiyse (kimlik dolu ve farklı)
     * bu cihaz hemen oturumdan düşürülür.
     */
    fun shouldForceSignOut(activeDeviceId: String?, deviceId: String): Boolean =
        !activeDeviceId.isNullOrBlank() && activeDeviceId != deviceId

    /** requireLoggedInAndSingleDevice kararı. */
    enum class Decision {
        /** Oturum yok: giriş ekranına yönlendir. */
        REDIRECT_TO_LOGIN,
        /** Kullanıcı dokümanı yok: çıkış yap + giriş ekranı. */
        SIGN_OUT_AND_REDIRECT,
        /** İzin ver; önce oturumu bu cihaza al. */
        CLAIM_AND_ALLOW,
        /** İzin ver (oturum zaten bu cihazda). */
        ALLOW,
        /** Başka cihazda canlı oturum var: çıkış yap, "Bir kaç dakika sonra tekrar dene." göster. */
        REJECT_OTHER_DEVICE,
    }

    /**
     * Android: requireLoggedInAndSingleDevice.
     * Oturum "bayat" sayılır: güncelleme zamanı yoksa ya da üzerinden 150 sn'den FAZLA geçtiyse.
     * Kimlik boşsa / bu cihazsa / bayatsa izin verilir; kimlik boşsa ya da başka cihazsa (bayat)
     * önce oturum bu cihaza alınır. Okuma hatasında izin VERİLMEZ (çağıran REDIRECT_TO_LOGIN uygular).
     */
    fun decide(
        isLoggedIn: Boolean,
        userDocExists: Boolean,
        activeDeviceId: String?,
        lastUpdatedMs: Long?,
        deviceId: String,
        nowMs: Long,
    ): Decision {
        if (!isLoggedIn) return Decision.REDIRECT_TO_LOGIN
        if (!userDocExists) return Decision.SIGN_OUT_AND_REDIRECT

        val lastMs = lastUpdatedMs ?: 0L
        val isStale = lastUpdatedMs == null || (nowMs - lastMs) > SESSION_STALE_MS

        return if (activeDeviceId.isNullOrBlank() || activeDeviceId == deviceId || isStale) {
            if (activeDeviceId.isNullOrBlank() || activeDeviceId != deviceId) {
                Decision.CLAIM_AND_ALLOW
            } else {
                Decision.ALLOW
            }
        } else {
            Decision.REJECT_OTHER_DEVICE
        }
    }
}

/** Hesap silme kuralları. */
object AccountDeletionRules {
    /**
     * Hassas işlem için son giriş en çok bu kadar önce olmalı (4,5 dk; Firebase sınırı 5 dk).
     */
    const val RECENT_LOGIN_WINDOW_MS = 270_000L

    const val MESSAGE_USER_NOT_FOUND = "Kullanıcı bulunamadı."

    /**
     * Son girişten bu yana 270 sn'den FAZLA geçtiyse Firestore verisine dokunmadan yeniden giriş
     * istenir. Son giriş zamanı bilinmiyorsa 0 sayılır (her zaman yeniden giriş gerekir).
     */
    fun requiresRecentLogin(lastSignInTimestampMs: Long?, nowMs: Long): Boolean {
        val lastSignInTime = lastSignInTimestampMs ?: 0
        return nowMs - lastSignInTime > RECENT_LOGIN_WINDOW_MS
    }

    const val COLLECTION_USERS = "users"
    const val SUBCOLLECTION_FOLLOWERS = "followers"
    const val SUBCOLLECTION_FOLLOWING = "following"

    /** İki seviyeli: lessonProgress/{partId}/items/{position} — önce items, sonra partId dokümanı. */
    const val SUBCOLLECTION_LESSON_PROGRESS = "lessonProgress"
    const val SUBCOLLECTION_LESSON_PROGRESS_ITEMS = "items"

    /** Tek dokümanlık alt koleksiyonlar: (koleksiyon, doküman). */
    val SINGLE_DOC_SUBCOLLECTIONS: List<Pair<String, String>> = listOf(
        "badgeProgress" to "state",
        "cupWayProgress" to "progress",
        "abacusCustomization" to "state",
        "missionProgress" to "state",
        "dailyQuestion" to "current",
    )

    /** Çok dokümanlı düz alt koleksiyonlar. */
    val LEAF_SUBCOLLECTIONS: List<String> = listOf("cupHistory", "lessonSuccessRateState", "weeklyStats", "lessonMilestones")

    /** Toplu silmede bir batch'e konan en çok doküman. */
    const val DELETE_BATCH_SIZE = 450

    /**
     * Silme adımlarının sırası (Android: performCompleteAccountDeletion):
     * 1. Her takipçinin `following/{benimUid}` kaydı, 2. takip edilenlerin `followers/{benimUid}`
     * kaydı, 3. kendi alt koleksiyonları (followers, following, lessonProgress, tek dokümanlıklar,
     * düz koleksiyonlar), 4. `users/{uid}` dokümanı, 5. Auth kaydı.
     * 1–3. adımlardaki hatalar yutulur; 4. ve 5. adım hataları işlemi başarısız kılar.
     *
     * DİKKAT (Android davranışı): `haveBead`, `haveFrame` gibi listede olmayan alt koleksiyonlar
     * SİLİNMEZ.
     */
    val STEP_ORDER: List<String> = listOf(
        "followers→following",
        "following→followers",
        "subcollections",
        "userDocument",
        "authRecord",
    )

    /** `users/{followerUid}/following/{myUid}` */
    fun followingRefPath(followerUid: String, myUid: String): String =
        "$COLLECTION_USERS/$followerUid/$SUBCOLLECTION_FOLLOWING/$myUid"

    /** `users/{followingUid}/followers/{myUid}` */
    fun followerRefPath(followingUid: String, myUid: String): String =
        "$COLLECTION_USERS/$followingUid/$SUBCOLLECTION_FOLLOWERS/$myUid"
}

/** FCM belirteci / bildirim kuralları. */
object FcmRules {
    const val CHANNEL_ID_MESSAGES = "messages"
    const val PREFS_NAME_THREADS = "notification_threads"
    const val MAX_INBOX_LINES = 7

    /** Bir kullanıcı için saklanan en çok cihaz girdisi. */
    const val MAX_DEVICES = 2

    const val FIELD_FCM_DEVICES = "fcmDevices"
    const val FIELD_FCM_TOKEN = "fcmToken"
    const val FIELD_FCM_TOKEN_UPDATED_AT = "fcmTokenUpdatedAt"
    const val DEVICE_FIELD_DEVICE_ID = "deviceId"
    const val DEVICE_FIELD_TOKEN = "token"
    const val DEVICE_FIELD_UPDATED_AT = "updatedAt"

    const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val UNKNOWN_DEVICE = "unknown-device"

    // Veri yükü (data message) alanları
    const val DATA_TITLE = "title"
    const val DATA_SENDER_NAME = "senderName"
    const val DATA_BODY = "body"
    const val DATA_MESSAGE_PREVIEW = "messagePreview"
    const val DATA_QUESTION_ID = "questionId"
    const val DATA_MESSAGE_ID = "messageId"
    const val DATA_RECIPIENT_UID = "recipientUid"

    /** Gösterilecek bildirim içeriği. */
    data class Incoming(
        val title: String,
        val body: String,
        val questionId: String,
        val messageId: String?,
        val recipientUid: String,
    )

    /**
     * Gelen veri yükünü çözer (onMessageReceived). Yük boşsa, `questionId` ya da `recipientUid`
     * yoksa null (bildirim gösterilmez). Başlık: title → senderName → [appName]; gövde: body →
     * messagePreview → "".
     */
    fun parseIncoming(data: Map<String, String>, appName: String): Incoming? {
        if (data.isEmpty()) return null
        val title = data[DATA_TITLE] ?: data[DATA_SENDER_NAME] ?: appName
        val body = data[DATA_BODY] ?: data[DATA_MESSAGE_PREVIEW] ?: ""
        val questionId = data[DATA_QUESTION_ID] ?: return null
        val messageId = data[DATA_MESSAGE_ID]
        val recipientUid = data[DATA_RECIPIENT_UID] ?: return null
        return Incoming(title, body, questionId, messageId, recipientUid)
    }

    /**
     * Bildirim yalnızca cihazda oturumu açık kullanıcı alıcının kendisiyse ve bildirimler açıksa
     * (varsayılan açık) gösterilir.
     */
    fun shouldShowNotification(currentUid: String?, recipientUid: String, notificationsEnabled: Boolean): Boolean {
        if (currentUid == null) return false
        if (currentUid != recipientUid) return false
        return notificationsEnabled
    }

    /** Belirteç yalnızca profil tamamsa (e-posta ve rol dolu) kaydedilir. */
    fun isProfileCompleteForToken(userData: Map<String, Any?>?): Boolean {
        val email = userData?.get("email") as? String
        val role = userData?.get("role") as? String
        return !(userData == null || email.isNullOrBlank() || role.isNullOrBlank())
    }

    /**
     * Android: saveCurrentTokenToFirestore'un yazdığı veri (merge). Bu cihazın eski girdisi çıkarılır,
     * yenisi SONA eklenir, liste son 2 girdiyle sınırlanır; `fcmToken` = son girdinin belirteci.
     */
    fun saveTokenUpdate(existingDevices: List<Map<*, *>>, deviceId: String, token: String, nowMs: Long): Map<String, Any?> {
        val filtered = existingDevices.filterNot { it[DEVICE_FIELD_DEVICE_ID] == deviceId }.toMutableList()
        filtered.add(
            mapOf(
                DEVICE_FIELD_DEVICE_ID to deviceId,
                DEVICE_FIELD_TOKEN to token,
                DEVICE_FIELD_UPDATED_AT to nowMs,
            )
        )
        val trimmed = if (filtered.size > 2) filtered.takeLast(2) else filtered
        val lastToken = trimmed.lastOrNull()?.get(DEVICE_FIELD_TOKEN) as? String
        return mapOf(
            FIELD_FCM_DEVICES to trimmed,
            FIELD_FCM_TOKEN_UPDATED_AT to nowMs,
            FIELD_FCM_TOKEN to lastToken,
        )
    }

    /**
     * Android: clearCurrentTokenFromFirestore'un yazdığı veri (merge). Bu cihazın girdisi çıkarılır;
     * `fcmToken` = kalan son girdinin belirteci (kalmadıysa null).
     */
    fun clearTokenUpdate(existingDevices: List<Map<*, *>>, deviceId: String, nowMs: Long): Map<String, Any?> {
        val filtered = existingDevices.filter { it[DEVICE_FIELD_DEVICE_ID] != deviceId }
        val lastToken = (filtered.lastOrNull()?.get(DEVICE_FIELD_TOKEN) as? String)
        return mapOf(
            FIELD_FCM_DEVICES to filtered,
            FIELD_FCM_TOKEN_UPDATED_AT to nowMs,
            FIELD_FCM_TOKEN to lastToken,
        )
    }

    /** Kullanıcı dokümanındaki `fcmDevices` alanını güvenle listeye çevirir. */
    fun devicesFrom(raw: Any?): List<Map<*, *>> = (raw as? List<*>)?.mapNotNull { it as? Map<*, *> } ?: emptyList()
}

/** Takip (follow) modeli ve arkadaş arama sonuçları. */
object FollowRules {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_PUBLIC_PROFILES = "publicProfiles"
    const val SUBCOLLECTION_FOLLOWERS = "followers"
    const val SUBCOLLECTION_FOLLOWING = "following"

    const val FIELD_USER_ID = "userId"
    const val FIELD_NAME = "name"
    const val FIELD_FOLLOWED_AT = "followedAt"
    const val FIELD_SELECTED_AVATAR = "selectedAvatar"

    /** FollowersFollowingFragment sekmeleri. */
    const val TAB_FOLLOWING = 0 // "Takip Edilen"
    const val TAB_FOLLOWERS = 1 // "Takipçi"

    /** `users/{targetUid}/followers/{myUid}` */
    fun followerDocPath(targetUid: String, myUid: String): String =
        "$COLLECTION_USERS/$targetUid/$SUBCOLLECTION_FOLLOWERS/$myUid"

    /** `users/{myUid}/following/{targetUid}` */
    fun followingDocPath(myUid: String, targetUid: String): String =
        "$COLLECTION_USERS/$myUid/$SUBCOLLECTION_FOLLOWING/$targetUid"

    /** Takip kaydı dokümanı (iki tarafa da aynı biçimde yazılır). */
    fun followDoc(userId: String, name: String, nowMs: Long): Map<String, Any> = mapOf(
        FIELD_USER_ID to userId,
        FIELD_NAME to name,
        FIELD_FOLLOWED_AT to nowMs,
    )

    /**
     * Takip ederken yazılan iki doküman:
     * - hedefin `followers/{benimUid}` kaydına BENİM kimliğim/adım,
     * - benim `following/{hedefUid}` kaydıma HEDEFİN kimliği/adı.
     * Sayaçlar (followersCount / followingCount) sunucudaki tetikleyicilerle güncellenir.
     */
    fun followWrites(
        myUid: String,
        myUserId: String,
        myName: String,
        targetUid: String,
        targetUserId: String,
        targetName: String,
        nowMs: Long,
    ): Map<String, Map<String, Any>> = mapOf(
        followerDocPath(targetUid, myUid) to followDoc(myUserId, myName, nowMs),
        followingDocPath(myUid, targetUid) to followDoc(targetUserId, targetName, nowMs),
    )

    /**
     * FollowersFollowingFragment'teki "geri takip et" yazımı.
     * DİKKAT (Android davranışı — büyük olasılıkla hata): AddFriendFragment'in tersine burada
     * veriler ÇAPRAZ yazılır: hedefin `followers/{benimUid}` kaydına HEDEFİN kimliği/adı,
     * benim `following/{hedefUid}` kaydıma BENİM kimliğim/adım konur. Aynen korunmuştur.
     */
    fun followBackWrites(
        myUid: String,
        myUserId: String,
        myName: String,
        targetUid: String,
        targetUserId: String,
        targetName: String,
        nowMs: Long,
    ): Map<String, Map<String, Any>> = mapOf(
        followerDocPath(targetUid, myUid) to followDoc(targetUserId, targetName, nowMs),
        followingDocPath(myUid, targetUid) to followDoc(myUserId, myName, nowMs),
    )

    data class FriendSearchResult(
        val firebaseUid: String,
        val name: String,
        val userId: String,
        val selectedAvatar: Int,
    )

    /**
     * `publicProfiles` dokümanı → arama sonucu (AddFriendFragment.parseSnapshot).
     * Ad ya da kullanıcı kimliği yoksa ve kullanıcının kendisiyse atlanır; avatar yoksa 0.
     */
    fun parseSearchResult(docId: String, data: Map<String, Any?>, currentUserId: String): FriendSearchResult? {
        val name = data[FIELD_NAME] as? String ?: return null
        val userId = data[FIELD_USER_ID] as? String ?: return null
        if (userId == currentUserId) return null
        val selectedAvatar = (data[FIELD_SELECTED_AVATAR] as? Number)?.toInt() ?: 0
        return FriendSearchResult(firebaseUid = docId, name = name, userId = userId, selectedAvatar = selectedAvatar)
    }
}
