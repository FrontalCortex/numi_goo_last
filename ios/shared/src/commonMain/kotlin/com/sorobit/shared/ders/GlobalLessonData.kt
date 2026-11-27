// KAYNAK: app/src/main/java/com/example/app/GlobalLessonData.kt
//   (katalog verisi ayrı dosyada: GlobalLessonDataKatalog.kt)
//
// Android'deki `object GlobalLessonData` içinden platformdan bağımsız her şey buraya taşındı:
//   bellek durumu (globalPartId / lessonItems), createLessonItems, varsayılan liste, Firestore doküman ↔ model
//   eşlemesi (progressFieldsOf / applyProgressFields), okuma sonucu birleştirme, canlı (realtime) snapshot
//   birleştirme, updateLessonItem + liderlik gönderim kararı, ilk sandık indeksi çözümü, tamamlanma yüzdesi,
//   yerel önbellek anahtarları ve eski "record" alanı göçü.
//
// Android'den bilinçli farklar:
//  - Firebase / Handler / Context / Gson çağrıları YOK. Firestore yazmaları [LessonProgressSink] arayüzüne,
//    okumalar ise "belge kimliği → alan haritası" parametrelerine çevrildi. Platform katmanı (Swift) Firestore'u
//    okuyup bu fonksiyonlara düz Map verir.
//  - `SeasonClock.currentSeason()` yerine `season` parametresi; SharedPreferences'taki
//    `tutorial1_login_flow_pending` bayrağı yerine `tutorial1Pending` parametresi.
//  - Log / debugLog çağrıları kaldırıldı.
//  - Kimlik: Android artık ilerleme dokümanlarını liste sırasıyla DEĞİL `LessonItem.stableId` ile anahtarlıyor
//    (`items/{stableId}`); eski sıra anahtarlı ("0","1",...) dokümanlar okunurken tanınır ve göç planı
//    ([legacyMigrationPlan]) ile taşınır. Kilit durumu liste durumundan türetilir ([withDerivedUnlocks]); onaylı
//    öğretmenin dersleri bir kez açılır ([teacherUnlockPlan]). Ders adı/ipucu Android'de strings_lessons.xml'den gelir;
//    ortak kopyada katalog Türkçe sabit metindir ([turkishItem], [localizeLessonText] bu yüzden kimlik/aynen döner).
//  - `stepCupIcon` Android'de Firestore'a R.drawable TAM SAYI kimliği olarak yazılıyor (derlemeye özgü bir sayı).
//    Ortak kodda bu alan kaynak adı (String). Bkz. [GlobalLessonData.progressFieldsOf] ve
//    [GlobalLessonData.applyProgressFields] açıklamaları.
package com.sorobit.shared.ders

/**
 * Ders ilerlemesi için dışarı giden G/Ç (Android'de doğrudan Firestore + LessonLeaderboardRepository çağrılarıydı).
 *
 * Uygulayan taraf Android'deki koşulu korumalıdır: yazmalar YALNIZCA giriş yapmış kullanıcı (uid != null) varken
 * yapılır; uid yoksa çağrı sessizce yok sayılır.
 */
interface LessonProgressSink {
    /** `users/{uid}/lessonProgress/{partId}/items/{stableId}` dokümanını [fields] ile `set()` eder (merge DEĞİL). */
    fun writeSingleItem(partId: Int, stableId: String, fields: Map<String, Any?>)

    /**
     * [itemsFields] listesindeki her öğeyi, [stableIds] listesindeki aynı sıradaki kimlikle `items/{stableId}`
     * dokümanına tek batch içinde `set()` eder (iki liste aynı uzunlukta).
     */
    fun writeAllItems(partId: Int, stableIds: List<String>, itemsFields: List<Map<String, Any?>>)

    /**
     * Eski sıra anahtarlı dokümanların göçü (Android: migrateLegacyItemKeys): [stableIds]/[itemsFields] yazılır,
     * [legacyDocIds] ("0","1",...) aynı batch'te silinir. Başarısızsa sessiz kalınmamalı; bir sonraki okumada yeniden denenir.
     */
    fun migrateLegacyItemKeys(partId: Int, stableIds: List<String>, itemsFields: List<Map<String, Any?>>, legacyDocIds: List<String>)

    /**
     * Android: LessonLeaderboardRepository.submitBestIfNeeded(partId, lessonKey, score, season, titleUnit).
     * Tahta kimliği artık `part_{partId}_lesson_{lessonKey}_season_{season}` ([lessonKey] = stableId).
     */
    fun submitBestIfNeeded(partId: Int, lessonKey: String, score: Int, season: Int, titleUnit: String?)
}

object GlobalLessonData {
    data class ChestLessonRef(
        val partId: Int,
        val index: Int,
        val item: LessonItem,
    )

    /** Liderlik tablosuna gönderilecek skor (Android: submitBestIfNeeded argümanları). */
    data class LeaderboardSubmit(
        val partId: Int,
        /** Dersin kalıcı kimliği ([LessonItem.stableId]); eskiden liste konumuydu. */
        val lessonKey: String,
        val score: Int,
        val season: Int,
        val titleUnit: String?,
    )

    var globalPartId = 1
    private var _lessonItems: MutableList<LessonItem> = mutableListOf()
    val lessonItems: List<LessonItem> get() = _lessonItems

    /** Firestore yazmaları ve liderlik gönderimi için platform köprüsü; null ise yazma yapılmaz. */
    var sink: LessonProgressSink? = null

    // --- Firestore yolları (iOS aynı veritabanını kullanır; DEĞİŞTİRME) ---
    const val FIRESTORE_USERS = "users"
    const val FIRESTORE_LESSON_PROGRESS = "lessonProgress"

    /** [FIRESTORE_LESSON_PROGRESS]/{partId} altındaki alt koleksiyon: her item'ın progress alanları {position} dokümanında. */
    const val FIRESTORE_LESSON_ITEMS = "items"

    /** [createLessonItems] ile tanımlı tüm part id'leri; seed ve çapraz-part okumalar için. */
    val SEED_PART_IDS: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7, 8)
    const val AUTH_WAIT_TIMEOUT_MS = 1500L

    /** Android: SharedPreferences("AppPrefs") içindeki bayrak adı; [buildDefaultLessonItemsForPart] bunu okur. */
    const val APP_PREFS_NAME = "AppPrefs"
    const val KEY_TUTORIAL1_LOGIN_FLOW_PENDING = "tutorial1_login_flow_pending"

    /** Kupa dersi liderlik tablosunu destekleyen part'lar (4 ve 5 desteklemiyor). */
    val LEADERBOARD_SUPPORTED_PARTS: Set<Int> = setOf(1, 2, 3, 6, 7, 8)

    private var lessonRealtimeAttached: Boolean = false
    private var lessonRealtimeUid: String? = null
    private var lessonRealtimePartId: Int? = null

    enum class FirestoreLoadStatus {
        LOADED,
        NOT_FOUND,
        ERROR,
    }

    /** `users/{uid}/lessonProgress/{partId}/items` koleksiyon yolu (Android: itemsCollectionRef). */
    fun itemsCollectionPath(uid: String, partId: Int): String =
        "$FIRESTORE_USERS/$uid/$FIRESTORE_LESSON_PROGRESS/$partId/$FIRESTORE_LESSON_ITEMS"

    /** `users/{uid}/lessonProgress/{partId}/items/{stableId}` doküman yolu. */
    fun itemDocumentPath(uid: String, partId: Int, stableId: String): String =
        "${itemsCollectionPath(uid, partId)}/$stableId"

    /** ESKİ sıra anahtarlı doküman yolu (`items/{position}`); yalnızca göç/okuma için. */
    fun legacyItemDocumentPath(uid: String, partId: Int, position: Int): String =
        "${itemsCollectionPath(uid, partId)}/$position"

    /** `users/{uid}` alanı: onaylı öğretmenin dersleri bir kez açıldı mı (Android: FIELD_TEACHER_LESSONS_UNLOCKED). */
    const val FIELD_TEACHER_LESSONS_UNLOCKED = "teacherLessonsUnlocked"

    // ---------------------------------------------------------------------------------------------
    // Genel tamamlanma yüzdesi (Android: calculateGlobalCompletionPercentage)
    // ---------------------------------------------------------------------------------------------

    /** Part 1..6 şablonlarındaki TYPE_LESSON + TYPE_CHEST item sayısı. */
    fun totalValidItemsForCompletion(): Int {
        var totalValidItems = 0
        for (part in 1..6) {
            val items = createLessonItems(part)
            totalValidItems += items.count { it.type == LessonItem.TYPE_LESSON || it.type == LessonItem.TYPE_CHEST }
        }
        return totalValidItems
    }

    /**
     * [itemsByPart]: part 1..6 için Firestore'dan okunup şablonla birleştirilmiş listeler
     * ([lessonItemsFromFirestoreDocs] çıktısı). Okuma hatası ya da boş koleksiyon → o part için null / eksik anahtar
     * (Android'de de o part 0 tamamlanmış sayılır). uid yoksa Android doğrudan 0 döndürür — çağıran taraf korumalı.
     */
    fun calculateGlobalCompletionPercentage(itemsByPart: Map<Int, List<LessonItem>?>): Int {
        val totalValidItems = totalValidItemsForCompletion()
        if (totalValidItems == 0) return 0

        var completedValidItems = 0
        for (partId in 1..6) {
            val items = itemsByPart[partId]
            if (items != null) {
                completedValidItems += items.count {
                    (it.type == LessonItem.TYPE_LESSON || it.type == LessonItem.TYPE_CHEST) && it.stepIsFinish
                }
            }
        }
        return ((completedValidItems.toFloat() / totalValidItems.toFloat()) * 100).toInt()
    }

    // ---------------------------------------------------------------------------------------------
    // Başlatma (Android: initialize + loadFromFirestore + ensureLessonRealtimeSync)
    // ---------------------------------------------------------------------------------------------

    /**
     * Android `initialize()` ilk satırı: aktif part'ı ayarlar. Devamındaki akış platformda kurulur:
     *
     *  1. uid varsa: [prepareRealtimeSync] true dönerse canlı dinleyiciyi [itemsCollectionPath] üzerine (yeniden) bağla;
     *     koleksiyonu bir kez oku → [onLoadFromFirestore] → dönen durumu [applyLoadStatus]'a ver → onReady.
     *  2. uid yoksa ve onReady YOKSA: [applyDefaultLessonItems] (saveRemote = false).
     *  3. uid yoksa ve onReady VARSA: en çok [AUTH_WAIT_TIMEOUT_MS] ms auth bekle; hâlâ uid yoksa
     *     [applyDefaultLessonItems] (saveRemote = true; uid olmadığı için sink zaten yazmaz) → onReady;
     *     uid geldiyse 1. adım.
     */
    fun initialize(partId: Int) {
        globalPartId = partId
    }

    /**
     * Android `ensureLessonRealtimeSync` içindeki defter tutma. true dönerse platform eski dinleyiciyi kaldırıp
     * [partId] için yenisini bağlamalıdır; false ise aynı (uid, part) için dinleyici zaten var.
     * Part değiştiyse eski part'ın bellek listesi temizlenir (yeni snapshot eski part verisiyle karışmasın).
     */
    fun prepareRealtimeSync(partId: Int, uid: String): Boolean {
        if (lessonRealtimeAttached && lessonRealtimeUid == uid && lessonRealtimePartId == partId) return false
        val oldListenerPartId = lessonRealtimePartId
        lessonRealtimeAttached = true
        lessonRealtimeUid = uid
        lessonRealtimePartId = partId

        if (oldListenerPartId != null && oldListenerPartId != partId) {
            _lessonItems = mutableListOf()
        }
        return true
    }

    /**
     * Canlı snapshot geldiğinde çağrılır (Android: addSnapshotListener gövdesi). Hata durumunda platform bu
     * fonksiyonu hiç çağırmamalı (Android'de hata sessizce yok sayılır).
     *
     * @param docs belge kimliği (stableId; eski kayıtlarda "0", "1", ...) → alan haritası.
     * @return bellek listesi değiştiyse true (Android'de ardından LessonManager.refreshLessonsFromGlobalData çağrılır;
     *         burada da çağrılıyor).
     */
    fun onRealtimeSnapshot(partId: Int, docs: Map<String, Map<String, Any?>?>, season: Int): Boolean {
        if (partId != globalPartId) return false
        if (docs.isEmpty()) return false
        try {
            val parsed = mergeTemplateWithDocs(partId, docs)
            val merged = if (_lessonItems.isEmpty()) {
                parsed.toMutableList()
            } else {
                LessonProgressMerge.mergeListsPreferMoreProgress(_lessonItems, parsed, season, globalPartId)
            }
            if (LessonProgressMerge.sameProgressState(_lessonItems, merged)) {
                return false
            }
            _lessonItems = withDerivedUnlocks(partId, merged)
            LessonManager.refreshLessonsFromGlobalData()
            return true
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Android `loadFromFirestore` geri çağrısının saf kısmı.
     * [items]: [lessonItemsFromFirestoreDocs] çıktısı (koleksiyon boşsa null); [error]: okuma hata verdiyse true.
     * LOADED: [lessonItems] ayarlandı. NOT_FOUND: doküman yok. ERROR: ağ/izin hatası ya da part bu arada değişti.
     */
    fun onLoadFromFirestore(partId: Int, items: List<LessonItem>?, error: Boolean): FirestoreLoadStatus {
        if (partId != globalPartId) {
            // Çağıran askıda kalmasın diye durum dönülür ama _lessonItems üzerine yazılmaz.
            return FirestoreLoadStatus.ERROR
        }
        if (error) return FirestoreLoadStatus.ERROR
        return if (items != null) {
            _lessonItems = withDerivedUnlocks(partId, items)
            FirestoreLoadStatus.LOADED
        } else {
            FirestoreLoadStatus.NOT_FOUND
        }
    }

    /**
     * Android `initialize()` içindeki `when (status)` dalları:
     * LOADED → bulut verisi kullanılır; NOT_FOUND → varsayılan liste uygulanır ve buluta yazılır;
     * ERROR → varsayılan liste uygulanır ama buluta YAZILMAZ (bulutun üstüne yazmamak için).
     */
    fun applyLoadStatus(status: FirestoreLoadStatus, partId: Int, tutorial1Pending: Boolean) {
        when (status) {
            FirestoreLoadStatus.LOADED -> Unit
            FirestoreLoadStatus.NOT_FOUND -> applyDefaultLessonItems(partId, tutorial1Pending)
            FirestoreLoadStatus.ERROR -> applyDefaultLessonItems(partId, tutorial1Pending, saveRemote = false)
        }
    }

    /**
     * Varsayılan ders listesi: [createLessonItems] + `tutorial1_login_flow_pending` ile part 1 index 1 düzeltmesi.
     * Bellekteki [lessonItems] / [globalPartId] değiştirilmez; seed ve [applyDefaultLessonItems] ortak kaynak.
     *
     * Not (Android davranışı aynen korunuyor): düzeltilen item 2 adımlı (stepCount = 2) olmasına rağmen
     * stepCompletionStatus 3 elemanlı `[true, false, false]` olarak yazılır.
     */
    fun buildDefaultLessonItemsForPart(partId: Int, tutorial1Pending: Boolean): List<LessonItem> {
        val items = createLessonItems(partId).toMutableList()
        if (partId == 1 && tutorial1Pending && items.size > 1) {
            val original = items[1]
            val updated = original.copy(
                tutorialIsFinish = true,
                currentStep = 2,
                startStepNumber = 2,
                stepCompletionStatus = listOf(true, false, false),
            )
            items[1] = updated
        }
        return items
    }

    /** Varsayılan ders listesini oluşturur ve [lessonItems] + tercihe göre Firestore'a kaydeder. */
    fun applyDefaultLessonItems(partId: Int, tutorial1Pending: Boolean, saveRemote: Boolean = true) {
        _lessonItems = withDerivedUnlocks(partId, buildDefaultLessonItemsForPart(partId, tutorial1Pending))
        saveToPreferences(saveRemote = saveRemote)
    }

    /**
     * Android `seedAllLessonProgressIfMissing` için: [partId] koleksiyonu BOŞSA yazılacak alan haritaları.
     * Şablon boşsa null (o part atlanır). Platform akışı: uid yoksa hiçbir şey yapma; [SEED_PART_IDS] sırasıyla,
     * her part için `items` koleksiyonundan `limit(1)` oku; boşsa bu listeyi batch ile yaz; okuma/yazma hatasında
     * sıradaki part'a geç. [lessonItems] / [globalPartId] değiştirilmez.
     */
    fun seedItemsForPart(partId: Int, tutorial1Pending: Boolean): List<Map<String, Any?>>? {
        val items = buildDefaultLessonItemsForPart(partId, tutorial1Pending)
        if (items.isEmpty()) return null
        return items.map { progressFieldsOf(it) }
    }

    /** [seedItemsForPart] çıktısıyla aynı sırada doküman kimlikleri (`items/{stableId}`). */
    fun seedStableIdsForPart(partId: Int): List<String> = createLessonItems(partId).map { it.stableId }

    /** Bir doküman yazımı: kimlik + alanlar. */
    data class ItemWrite(val stableId: String, val fields: Map<String, Any?>)

    /**
     * Onaylı öğretmenin derslerini İLK SEFERDE açma kuralı (Android: withTeacherUnlocks); yalnızca AÇAR.
     *  - 1–6: türü ders veya sandık olan her item `isCompleted = true` (başlık/yarışa dokunulmaz).
     *  - 7–8: kilitli (`raceBusyLevel == 2`) her item 1'e (BAŞLAT) çekilir; çözülmüş (0) item'a dokunulmaz.
     */
    fun withTeacherUnlocks(partId: Int, items: List<LessonItem>): List<LessonItem> = when (partId) {
        in 1..6 -> items.map { item ->
            val playable = item.type == LessonItem.TYPE_LESSON || item.type == LessonItem.TYPE_CHEST
            if (playable && !item.isCompleted) item.copy(isCompleted = true) else item
        }
        7, 8 -> items.map { item ->
            if (item.raceBusyLevel == 2) item.copy(raceBusyLevel = 1) else item
        }
        else -> items
    }

    /**
     * Android `unlockLessonsForApprovedTeacherOnce` içindeki bir bölümün yazım planı. Platform akışı: [SEED_PART_IDS]
     * sırasıyla her bölüm için (tohumlama sürüyorsa bitmesini bekle; aynı anda ikinci çalıştırma yok) `items` koleksiyonunu
     * oku ([lessonItemsFromFirestoreDocs]; boşsa null) → bu fonksiyonun döndürdüğü yazımları [LessonProgressSink.writeAllItems]
     * ile yaz; okuma/yazma hatasında akışı durdur (bayrak konmaz). Sekiz bölümün hepsi yazılınca
     * `users/{uid}.`[FIELD_TEACHER_LESSONS_UNLOCKED] = true yazılır, ardından [applyTeacherUnlocksToMemory] çağrılır.
     *
     * [cloudItems] null ise (bölüm hiç oynanmamış) bütün liste (varsayılan + açılmış hâli) yazılır; doluysa yalnızca
     * değişen item'lar.
     */
    fun teacherUnlockPlan(partId: Int, cloudItems: List<LessonItem>?, tutorial1Pending: Boolean = false): List<ItemWrite> {
        val base = cloudItems ?: buildDefaultLessonItemsForPart(partId, tutorial1Pending)
        val unlocked = withTeacherUnlocks(partId, base)
        val changed = if (cloudItems == null) unlocked else unlocked.filterIndexed { i, item -> item != base[i] }
        return changed.map { ItemWrite(it.stableId, progressFieldsOf(it)) }
    }

    /**
     * Öğretmen açması bittikten sonra bellekteki aktif bölümü de açar (Android: finish() içindeki blok). Platform
     * yalnızca oturum uid'si öğretmen uid'siyle aynıysa çağırmalı. Bellek boşsa dokunmaz.
     * @return bellek değiştiyse true (LessonManager.refreshLessonsFromGlobalData da çağrılır).
     */
    fun applyTeacherUnlocksToMemory(): Boolean {
        if (_lessonItems.isEmpty()) return false
        _lessonItems = withDerivedUnlocks(globalPartId, withTeacherUnlocks(globalPartId, _lessonItems))
        LessonManager.refreshLessonsFromGlobalData()
        return true
    }

    /**
     * Kilit durumunu LİSTE DURUMUNDAN türetir (Android: withDerivedUnlocks): bir ders kendisi açıksa ya da bir önceki
     * oynanabilir ders bitirilmişse (`stepIsFinish`) açıktır; başlıklar zincirde şeffaftır. Yalnızca AÇAR, asla kilitlemez.
     * Yalnızca 1–6 için (7–8'de kilit `raceBusyLevel`).
     */
    fun withDerivedUnlocks(partId: Int, items: List<LessonItem>): MutableList<LessonItem> {
        if (partId !in 1..6) return items.toMutableList()
        var previousFinished = true // ilk oynanabilir ders her zaman açıktır
        return items.map { item ->
            if (item.type == LessonItem.TYPE_HEADER) return@map item
            val unlocked = item.isCompleted || previousFinished
            previousFinished = item.stepIsFinish
            if (unlocked && !item.isCompleted) item.copy(isCompleted = true) else item
        }.toMutableList()
    }

    /** Şablondaki [position]'daki dersin kalıcı kimliği; konum dışındaysa null (Android: stableIdAt). */
    fun stableIdAt(partId: Int, position: Int): String? = createLessonItems(partId).getOrNull(position)?.stableId

    /** Android: turkishItem. Ortak katalog zaten Türkçe olduğundan şablondaki item'ın kendisi. */
    fun turkishItem(partId: Int, stableId: String): LessonItem? =
        createLessonItems(partId).firstOrNull { it.stableId == stableId }

    /** Android: createLessonItemsInTurkish. Ortak katalog Türkçe olduğundan [createLessonItems] ile aynı. */
    fun createLessonItemsInTurkish(partId: Int): List<LessonItem> = createLessonItems(partId)

    /** Android: localizeLessonText (Türkçe kimlik metnini uygulama diline çevirir). Ortak kopyada çeviri yok → aynen. */
    fun localizeLessonText(turkish: String): String = turkish

    // ---------------------------------------------------------------------------------------------
    // Güncelleme / okuma
    // ---------------------------------------------------------------------------------------------

    /**
     * @return item belleğe yazıldıysa true (Android'de dönüş değeri yok; test/teşhis için eklendi).
     */
    fun updateLessonItem(position: Int, newItem: LessonItem, season: Int): Boolean {
        if (newItem.partId != null && newItem.partId != globalPartId) {
            return false
        }
        if (position in _lessonItems.indices) {
            _lessonItems[position] = newItem
            sink?.writeSingleItem(globalPartId, newItem.stableId, progressFieldsOf(newItem))
            // Kupa dersi tamamlandığında liderlik senkronu: [record] tüm zamanların en iyisi (UI);
            // Firestore tahtasına yalnızca [leaderboardSeasonBest] (mevcut sezon) yazılır.
            // Not: globalPartId 4 ve 5 liderlik tablosunu desteklemiyor; bu partlar için submit atlanır.
            val submit = leaderboardSubmitFor(globalPartId, position, newItem, season)
            if (submit != null) {
                sink?.submitBestIfNeeded(submit.partId, submit.lessonKey, submit.score, submit.season, submit.titleUnit)
            }
            return true
        }
        return false
    }

    /** [updateLessonItem] içindeki liderlik gönderim kararı; gönderilmeyecekse null. [position] artık kimliğe girmez. */
    fun leaderboardSubmitFor(partId: Int, position: Int, newItem: LessonItem, season: Int): LeaderboardSubmit? {
        if (newItem.type == LessonItem.TYPE_CHEST && newItem.stepIsFinish &&
            partId in LEADERBOARD_SUPPORTED_PARTS
        ) {
            val lbScore = newItem.leaderboardSubmitScore(season)
            if (lbScore != null && lbScore > 0) {
                return LeaderboardSubmit(
                    partId = partId,
                    lessonKey = newItem.stableId,
                    score = lbScore,
                    season = season,
                    // Sezon sonunda madalya/kupa parçasının adı olur: Türkçe kimlik (ortak katalog zaten Türkçe).
                    titleUnit = (turkishItem(partId, newItem.stableId)?.titleUnit ?: newItem.titleUnit)
                        ?.trim()?.take(127),
                )
            }
        }
        return null
    }

    fun getLessonItem(position: Int): LessonItem? {
        return if (position in _lessonItems.indices) {
            _lessonItems[position]
        } else {
            null
        }
    }

    fun templateFirstChestLessonIndexOrNull(partId: Int): Int? {
        val idx = createLessonItems(partId).indexOfFirst { it.type == LessonItem.TYPE_CHEST }
        return idx.takeIf { it >= 0 }
    }

    /**
     * Kullanıcının Firestore `lessonProgress/{partId}` listesindeki ilk [LessonItem.TYPE_CHEST] satırının 0-tabanlı
     * indeksi (Android: resolveFirstChestLessonIndexForUser geri çağrısının saf kısmı).
     *
     * uid boşsa Android okumadan şablon indeksini döner — platform o durumda [templateFirstChestLessonIndexOrNull]
     * çağırmalı. [error] true ise şablon indeksi; [cloudItems] null ise (boş bulut) şablon listesi kullanılır.
     */
    fun resolveFirstChestLessonIndex(partId: Int, cloudItems: List<LessonItem>?, error: Boolean): Int? {
        val templateChest = templateFirstChestLessonIndexOrNull(partId)
        if (error) return templateChest
        val items = cloudItems ?: createLessonItems(partId)
        var idx = items.indexOfFirst { it.type == LessonItem.TYPE_CHEST }
        val usedFallback = idx < 0
        if (usedFallback) {
            idx = templateChest ?: -1
        }
        return idx.takeIf { it >= 0 }
    }

    /**
     * (Android'de bu akış KALDIRILDI — soban-gamified-education dalında `backfillLeaderboardFromStoredChest` yok; ortak kopyada
     * geriye uyum için duruyor, kullanılmayabilir.) Android `backfillLeaderboardFromStoredChest` kararı: lessonProgress'ta [chestIndex] sandığı tamamlanmış ve
     * mevcut sezon için [LessonItem.leaderboardSubmitScore] doluysa gönderilecek liderlik girişini döner.
     *
     * Platform ön koşulları (Android ile aynı): uid boş değil VE uid == oturum uid; okuma hatasında / boş
     * koleksiyonda ([items] == null) gönderim yok.
     */
    fun backfillLeaderboardSubmit(
        partId: Int,
        chestIndex: Int,
        items: List<LessonItem>?,
        season: Int,
    ): LeaderboardSubmit? {
        if (items == null) return null
        val item = items.getOrNull(chestIndex)
        if (item == null || item.type != LessonItem.TYPE_CHEST) return null
        if (!item.stepIsFinish) return null
        val r = item.leaderboardSubmitScore(season)
        if (r == null || r <= 0) return null
        return LeaderboardSubmit(
            partId = partId,
            lessonKey = item.stableId,
            score = r,
            season = season,
            titleUnit = item.titleUnit?.trim()?.take(127),
        )
    }

    /**
     * Global state'i değiştirmeden belirli part/index için kullanıcıya ait lesson item'ı döndürür.
     * [cloudItems]: Firestore'dan okunan liste; giriş yoksa / okuma hatasında / boş koleksiyonda null → şablon.
     */
    fun getLessonItemForPart(partId: Int, index: Int, cloudItems: List<LessonItem>?): LessonItem? =
        (cloudItems ?: createLessonItems(partId)).getOrNull(index)

    /** Part lesson listesi (Firestore veya yerel şablon); günlük soru havuzu vb. için. */
    fun loadLessonItemsForPart(partId: Int, cloudItems: List<LessonItem>?): List<LessonItem> =
        cloudItems ?: createLessonItems(partId)

    /**
     * Kullanıcının tüm part'lerindeki (1..8) TYPE_CHEST + stepIsFinish=true item'ları döndürür.
     * [cloudItemsByPart]: part → Firestore listesi (yoksa şablon kullanılır). Global state'i değiştirmez.
     */
    fun getFinishedChestItemsAcrossParts(cloudItemsByPart: Map<Int, List<LessonItem>?>): List<ChestLessonRef> {
        val out = mutableListOf<ChestLessonRef>()
        for (partId in 1..8) {
            val items = loadLessonItemsForPart(partId, cloudItemsByPart[partId])
            items.forEachIndexed { idx, item ->
                if (item.type == LessonItem.TYPE_CHEST && item.stepIsFinish) {
                    out.add(ChestLessonRef(partId = partId, index = idx, item = item))
                }
            }
        }
        return out
    }

    // ---------------------------------------------------------------------------------------------
    // Firestore doküman ↔ model eşlemesi
    // ---------------------------------------------------------------------------------------------

    /**
     * Bir [LessonItem]'ın yalnızca mutasyona uğrayan (ilerleme) alanları — title/type/stepCount gibi statik alanlar
     * hariç. null değerli alanlar hiç yazılmaz (ör. chest olmayan item'larda cupPoint1/record hep null kalır).
     * Alan adları Android ile birebir aynıdır.
     *
     * `stepCupIcon` UYARISI: Android bu alana R.drawable tam sayı kimliğini yazar (derlemeden derlemeye değişebilen
     * bir sayı). Ortak kodda değer kaynak adıdır; varsayılan olarak String yazılır. Android okurken
     * `as? Number` kullandığı için String değeri yok sayar ve şablondaki varsayılana düşer (çökme olmaz).
     * Farklı bir kodlama istenirse [stepCupIconYaz] ile değiştirilebilir (null dönerse alan yazılmaz).
     */
    fun progressFieldsOf(
        item: LessonItem,
        stepCupIconYaz: (String) -> Any? = { it },
    ): Map<String, Any?> = mapOf(
        "stepIsFinish" to item.stepIsFinish,
        "currentStep" to item.currentStep,
        "stepCompletionStatus" to item.stepCompletionStatus,
        "isCompleted" to item.isCompleted,
        "finalGoldVisualUnlocked" to item.finalGoldVisualUnlocked,
        "tutorialIsFinish" to item.tutorialIsFinish,
        "tutorialNumber" to item.tutorialNumber,
        "raceBusyLevel" to item.raceBusyLevel,
        "record" to item.record,
        "leaderboardSeasonId" to item.leaderboardSeasonId,
        "leaderboardSeasonBest" to item.leaderboardSeasonBest,
        "stepCupIcon" to stepCupIconYaz(item.stepCupIcon),
        "cupDifficultyLevel" to item.cupDifficultyLevel,
        "cupWinDelta" to item.cupWinDelta,
        "cupLossDelta" to item.cupLossDelta,
        "worstCupTime" to item.worstCupTime,
        "cupPoint1" to item.cupPoint1,
        "cupPoint2" to item.cupPoint2,
        "progressBarLevel" to item.progressBarLevel,
    ).filterValues { it != null }

    /**
     * `stepCupIcon` ham Firestore değerini kaynak adına çevirir: boş olmayan String → kendisi; diğer her şey
     * (Android'in yazdığı sayısal R.drawable kimliği dahil) → null (şablon değeri korunur).
     */
    fun stepCupIconVarsayilanOku(raw: Any?): String? = (raw as? String)?.takeIf { it.isNotEmpty() }

    /**
     * [template] üzerine Firestore'dan gelen progress alanlarını uygular; alan yoksa template değeri korunur.
     *
     * Sayısal alanlar `Number` olarak beklenir (Firestore Long/Double verir) ve `toInt()` ile çevrilir; tür uymuyorsa
     * şablon değeri kalır. `stepCompletionStatus` listesinde Boolean olmayan öğeler false sayılır.
     *
     * `stepCupIcon`: Android sayısal kimlik bekler; ortak kodda [stepCupIconOku] ham değeri kaynak adına çevirir.
     * Android'in yazdığı sayı burada çözülemez → şablon değeri ("chest_stars_tier0") kalır. iOS'ta doğru yıldızı
     * göstermek için okuma sonrası [stepCupIconKayittanTuret] kullanılabilir.
     */
    fun applyProgressFields(
        template: LessonItem,
        data: Map<String, Any?>,
        stepCupIconOku: (Any?) -> String? = ::stepCupIconVarsayilanOku,
    ): LessonItem {
        if (data.isEmpty()) return template
        fun int(key: String) = (data[key] as? Number)?.toInt()
        fun bool(key: String, default: Boolean) = data[key] as? Boolean ?: default
        return template.copy(
            stepIsFinish = bool("stepIsFinish", template.stepIsFinish),
            currentStep = int("currentStep") ?: template.currentStep,
            stepCompletionStatus = (data["stepCompletionStatus"] as? List<*>)
                ?.map { it as? Boolean ?: false }
                ?: template.stepCompletionStatus,
            isCompleted = bool("isCompleted", template.isCompleted),
            finalGoldVisualUnlocked = bool("finalGoldVisualUnlocked", template.finalGoldVisualUnlocked),
            tutorialIsFinish = bool("tutorialIsFinish", template.tutorialIsFinish),
            tutorialNumber = int("tutorialNumber") ?: template.tutorialNumber,
            raceBusyLevel = int("raceBusyLevel") ?: template.raceBusyLevel,
            record = int("record") ?: template.record,
            leaderboardSeasonId = int("leaderboardSeasonId") ?: template.leaderboardSeasonId,
            leaderboardSeasonBest = int("leaderboardSeasonBest") ?: template.leaderboardSeasonBest,
            stepCupIcon = stepCupIconOku(data["stepCupIcon"]) ?: template.stepCupIcon,
            cupDifficultyLevel = int("cupDifficultyLevel") ?: template.cupDifficultyLevel,
            cupWinDelta = int("cupWinDelta") ?: template.cupWinDelta,
            cupLossDelta = int("cupLossDelta") ?: template.cupLossDelta,
            worstCupTime = int("worstCupTime") ?: template.worstCupTime,
            cupPoint1 = int("cupPoint1") ?: template.cupPoint1,
            cupPoint2 = int("cupPoint2") ?: template.cupPoint2,
            progressBarLevel = int("progressBarLevel") ?: template.progressBarLevel,
        )
    }

    /**
     * iOS'A ÖZGÜ YARDIMCI (Android'de karşılığı yok; varsayılan akışta ÇAĞRILMAZ).
     *
     * Android'in Firestore'a yazdığı sayısal `stepCupIcon` iOS'ta çözülemediği için, tamamlanmış sandıkların görselini
     * rekordan yeniden türetir. Android'de saklanan görsel de her zaman rekordan
     * ([ChestTypeProgressHelper.resolvedChestIcon]) hesaplandığı için sonuç aynıdır.
     */
    fun stepCupIconKayittanTuret(item: LessonItem, globalPartId: Int = this.globalPartId): LessonItem {
        if (item.type != LessonItem.TYPE_CHEST || !item.stepIsFinish) return item
        return item.copy(
            stepCupIcon = ChestTypeProgressHelper.resolvedChestIcon(item, item.record ?: 0, globalPartId),
        )
    }

    /** Belge haritasındaki ESKİ sıra anahtarlı ("0","1",...) belgeler: indeks → alanlar. */
    private fun legacyDocsByIndex(docs: Map<String, Map<String, Any?>?>): Map<Int, Map<String, Any?>?> =
        docs.entries.mapNotNull { (id, data) -> id.toIntOrNull()?.let { it to data } }.toMap()

    /**
     * Şablonu ([createLessonItems]) belge haritasıyla birleştirir (Android: readLessonItemsFromFirestore).
     * Önce belge kimliği = [LessonItem.stableId] aranır; yoksa ESKİ biçim (belge kimliği = liste sırası) denenir;
     * ikisi de yoksa şablon item'ı kalır. Şablonda olmayan belgeler yok sayılır.
     */
    fun mergeTemplateWithDocs(partId: Int, docs: Map<String, Map<String, Any?>?>): List<LessonItem> {
        val template = createLessonItems(partId)
        val byLegacyIndex = legacyDocsByIndex(docs)
        return template.mapIndexed { index, item ->
            val data = docs[item.stableId] ?: byLegacyIndex[index] ?: return@mapIndexed item
            applyProgressFields(item, data)
        }
    }

    /** Göç planı: yazılacak yeni anahtarlı belgeler + silinecek eski belge kimlikleri. */
    data class LegacyMigrationPlan(val writes: List<ItemWrite>, val legacyDocIds: List<String>)

    /**
     * Android `migrateLegacyItemKeys`: [docs] içinde eski sıra anahtarlı belge varsa, [merged] ([lessonItemsFromFirestoreDocs]
     * çıktısı) içindeki o indekslerin item'ları `items/{stableId}` olarak yazılır ve eski belgeler silinir. Eski belge yoksa null.
     * Platform okuma sonucunu önce uygulamaya verir, göçü arkada yapar ([LessonProgressSink.migrateLegacyItemKeys]).
     */
    fun legacyMigrationPlan(docs: Map<String, Map<String, Any?>?>, merged: List<LessonItem>): LegacyMigrationPlan? {
        val legacy = legacyDocsByIndex(docs).keys
        if (legacy.isEmpty()) return null
        val writes = merged.filterIndexed { index, _ -> index in legacy }.map { ItemWrite(it.stableId, progressFieldsOf(it)) }
        return LegacyMigrationPlan(writes, legacy.map { it.toString() })
    }

    /**
     * [partId] altındaki `items/` alt koleksiyonunun içeriğini şablonla birleştirir
     * (Android: readLessonItemsFromFirestore başarı dalı). Koleksiyon boşsa (kullanıcı bu part'ı hiç oynamamış)
     * null döner.
     */
    fun lessonItemsFromFirestoreDocs(partId: Int, docs: Map<String, Map<String, Any?>?>): List<LessonItem>? {
        if (docs.isEmpty()) return null
        return mergeTemplateWithDocs(partId, docs)
    }

    // ---------------------------------------------------------------------------------------------
    // Katalog
    // ---------------------------------------------------------------------------------------------

    fun createLessonItems(partId: Int): List<LessonItem> {
        val items = when (partId) {
            1 -> katalogPart1()
            2 -> katalogPart2()
            3 -> katalogPart3()
            4 -> katalogPart4()
            5 -> katalogPart5()
            6 -> katalogPart6()
            7 -> katalogPart7()
            8 -> katalogPart8()
            9 -> katalogPart9()
            else -> emptyList()
        }
        return items.mapIndexed { index, item ->
            item.apply {
                mapFragmentIndex = index
                this.partId = partId
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Yerel önbellek (Android: SharedPreferences) — yalnızca biçim/anahtarlar ve saf göç mantığı
    // ---------------------------------------------------------------------------------------------
    //
    // Android'de yerel önbellek artık KAYNAK olarak kullanılmıyor ("cloud-only"): saveToPreferences yalnızca
    // Firestore'a yazar, loadFromPreferences eski kurulumlardan kalan veriyi okuyabilir.
    //
    // Biçim: dosya adı "LessonPrefs_{uid}" (giriş yoksa "LessonPrefs_guest"), anahtar "lesson_items_part_{partId}",
    // değer Gson ile serileştirilmiş `List<LessonItem>` JSON dizisi. Her nesnede alan adları sınıftaki özellik
    // adlarıyla aynıdır (type, title, partId, offset, isCompleted, progressBarLevel, stepCount, currentStep, color,
    // LESSON_ID, isBlinding, ..., stepCupIcon, record, ...). `fragment` alanı @Transient olduğu için yazılmaz; null
    // alanlar Gson varsayılanıyla atlanır; `color` ve `stepCupIcon` Android kaynak kimliği (tam sayı) olarak durur.
    // Eski sürümlerde `record` "mm:ss" metni olabiliyordu → [migrateRecordValue].
    // iOS bu önbelleği paylaşmaz (uygulama verisi platforma özeldir); JSON ayrıştırıcı ortak koda eklenmedi.

    const val PREFS_PREFIX = "LessonPrefs_"
    const val KEY_LESSON_ITEMS = "lesson_items"
    fun getKey(partId: Int) = "lesson_items_part_$partId"

    /** Giriş yapmış kullanıcı için LessonPrefs_uid, giriş yoksa LessonPrefs_guest. */
    fun lessonPrefsName(uid: String?): String = "$PREFS_PREFIX${uid ?: "guest"}"

    /**
     * Android adı korunmuştur: yerel tercihlere yazmaz ("cloud-only"); [saveRemote] true ise aktif part'ın tüm
     * item'larını [sink] üzerinden Firestore'a yazar (uid koşulu sink'te).
     */
    fun saveToPreferences(saveRemote: Boolean = true) {
        if (saveRemote) {
            sink?.writeAllItems(globalPartId, _lessonItems.map { it.stableId }, _lessonItems.map { progressFieldsOf(it) })
        }
    }

    /**
     * Android `parseLessonItemsWithMigration` sonrası adım: ayrıştırılmış önbellek listesindeki her item'ın
     * partId'si aktif part'a çekilir ve liste belleğe alınır (Android: loadFromPreferences başarı dalı).
     */
    fun loadFromParsedCache(items: List<LessonItem>, partId: Int = globalPartId) {
        val list = items.toMutableList()
        list.forEach { it.partId = partId }
        _lessonItems = withDerivedUnlocks(partId, list)
    }

    /**
     * Eski önbellek JSON'undaki `record` değerinin göçü (Android: migrateRecordValue).
     * Sayı → aynen; String → kırpılıp tam sayıya çevrilir, çevrilemezse (eski "mm:ss") null; diğer türler aynen.
     */
    fun migrateRecordValue(recordElement: Any?): Any? {
        if (recordElement == null) return null
        if (recordElement is Number) return recordElement
        if (recordElement !is String) return recordElement
        val legacyValue = recordElement.trim()
        // Eski "mm:ss" kayıtları puan modeliyle uyumsuz, güvenli fallback (null).
        return legacyValue.toIntOrNull()
    }

    /**
     * Android `migrateLegacyRecordField`: kök bir dizi değilse dokunulmaz; dizideki nesne (Map) öğelerinin
     * `record` alanı [migrateRecordValue] ile düzeltilir, nesne olmayan öğeler aynen kalır.
     */
    fun migrateLegacyRecordField(root: Any?): Any? {
        if (root !is List<*>) return root
        return root.map { element ->
            if (element !is Map<*, *>) return@map element
            if (!element.containsKey("record")) return@map element
            val kopya = LinkedHashMap<Any?, Any?>(element)
            kopya["record"] = migrateRecordValue(element["record"])
            kopya
        }
    }

    /**
     * Sadece şu anki kullanıcının ders verisini temizler (test / sıfırlama için).
     * @return platformun temizlemesi gereken tercih dosyası adları (misafir + varsa kullanıcı).
     */
    fun clearCurrentUserLessonData(uid: String?): List<String> {
        val dosyalar = mutableListOf("${PREFS_PREFIX}guest")
        if (!uid.isNullOrBlank()) {
            dosyalar.add("$PREFS_PREFIX$uid")
        }
        _lessonItems = mutableListOf()
        return dosyalar
    }

    /** Yalnızca testler için: bellek durumunu ve köprüleri başlangıç haline döndürür. */
    internal fun resetForTest() {
        globalPartId = 1
        _lessonItems = mutableListOf()
        sink = null
        lessonRealtimeAttached = false
        lessonRealtimeUid = null
        lessonRealtimePartId = null
    }

    /** Yalnızca testler için: bellek listesini doğrudan ayarlar. */
    internal fun setLessonItemsForTest(items: List<LessonItem>) {
        _lessonItems = items.toMutableList()
    }
}

/**
 * KAYNAK: app/src/main/java/com/example/app/LessonDataManager.kt
 *
 * Android'de Gson + SharedPreferences ile tüm listeyi saklayan eski yardımcı (güncel akışta kullanılmıyor).
 * Ortak koda yalnızca depolama anahtarları taşındı; biçim [GlobalLessonData] yerel önbellek notundaki JSON ile aynı
 * (Gson `List<LessonItem>` dizisi).
 */
object LessonDataManager {
    const val PREFS_NAME = "lesson_prefs"
    const val KEY_LESSON_ITEMS = "lesson_items"
}
