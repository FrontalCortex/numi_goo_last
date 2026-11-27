import Foundation

// Profil ekranlarının veri modelleri ve Firebase'in arkasına konduğu protokol.
// Firebase henüz bağlı değil; gerçek uygulama yazılana kadar `SahteProfilDeposu` kullanılır.
// Her üyenin yanında Android'de verinin geldiği koleksiyon/alan yazılıdır.

/// Oturumdaki kullanıcı (Android: FirebaseAuth.currentUser + AuthManager önbelleği).
struct ProfilOturumBilgisi {
    /// FirebaseAuth.currentUser.uid
    var uid: String
    /// AuthManager.getCurrentUserId() — kullanıcı kodu (`users/{uid}.userId`, yerelde `auth_prefs.user_id`).
    var kullaniciKodu: String
    /// AuthManager.getCurrentUserName() (`auth_prefs.user_name`).
    var ad: String
    /// FirebaseAuth.currentUser.displayName
    var gorunenAd: String?
    /// FirebaseAuth.currentUser.email
    var eposta: String?
    /// FirebaseAuth.currentUser.providerData[].providerId ("password", "google.com"; iOS'ta "apple.com" da olabilir).
    var saglayicilar: [String]
    /// FirebaseAuth.currentUser.metadata.creationTimestamp'in yılı.
    var kayitYili: Int
    /// FirebaseAuth.currentUser.metadata.lastSignInTimestamp (ms); bilinmiyorsa nil.
    var sonGirisMs: Int64?
}

/// Profil dokümanı. Kendi profilinde `users/{uid}`, başkasınınkinde `publicProfiles/{uid}`
/// (`users` dokümanını yalnızca sahibi ve onaylı öğretmenler okuyabilir).
struct ProfilBelgesi {
    var uid: String
    /// `name`
    var ad: String?
    /// `userId`
    var kullaniciKodu: String
    /// `avatarConfig` (Android AvatarConfig.encode biçimi; AvatarStore.FIRESTORE_FIELD). Yoksa nil.
    /// Eski `selectedAvatar` (1...12) alanı güncel Android'de okunmuyor.
    var avatarConfig: String?
    /// `totalTimeSpent` (saniye)
    var toplamSureSn: Int64
    /// `longestStreak` — en uzun günlük seri (gün; sunucu yazar, publicProfiles'a da aynalanır). Yoksa 0.
    var enUzunSeri: Int64
    /// `plan` ("Free", "Lite", "Pro") — profilde artık gösterilmiyor; Ayarlar'daki "Ödemeler" bölümü için.
    var plan: String?
    /// `followersCount` — sunucudaki takip tetikleyicileri günceller, istemci yazmaz.
    var takipciSayisi: Int
    /// `followingCount`
    var takipSayisi: Int
    /// `createdAt` zaman damgasının yılı (başkasının profilinde kullanılır); yoksa nil.
    var olusturmaYili: Int?
    /// `role` ("STUDENT", "TEACHER") — yalnızca `users` dokümanında.
    var rol: String?
}

/// Takip listesindeki bir kayıt: `users/{benimUid}/following|followers/{uid}` dokümanı
/// (`name`, `userId`, `followedAt`).
struct ProfilTakipKaydi: Identifiable, Equatable {
    var uid: String
    var ad: String
    var kullaniciKodu: String
    var id: String { uid }
}

/// Arama sonucu: `publicProfiles/{uid}` dokümanı (`name`, `userId`, `avatarConfig`).
struct ProfilAramaSonucu: Identifiable, Equatable {
    var uid: String
    var ad: String
    var kullaniciKodu: String
    /// `publicProfiles/{uid}.avatarConfig`; nil → harfli daire.
    var avatarConfig: String?
    var id: String { uid }
}

/// Kupa kartına dokununca dışarı verilen bilgi (Android: CupHistoryFragment.newInstance(uid, field, title, animFile)).
struct ProfilKupaSecimi {
    let uid: String
    let alan: String
    let baslik: String
    let animasyonDosyasi: String
}

enum ProfilHata: Error {
    /// Firebase Auth profil güncellemesi başarısız.
    case kimlik
    /// Firestore yazımı başarısız.
    case veritabani
    /// FirebaseAuthRecentLoginRequiredException karşılığı.
    case yenidenGirisGerekli
    /// Diğer hatalar; metin Android'deki `e.message` / `e.localizedMessage` karşılığıdır.
    case diger(String)

    var mesaj: String {
        switch self {
        case .kimlik: return "kimlik"
        case .veritabani: return "veritabanı"
        case .yenidenGirisGerekli: return "yeniden giriş gerekli"
        case .diger(let metin): return metin
        }
    }
}

enum ProfilHesapSilmeSonucu {
    case basarili
    case yenidenGirisGerekli
    case hata(String)
}

/// Profil ekranlarının bütün ağ/oturum/yerel depolama işleri.
/// Gerçek uygulama Firebase iOS SDK ile yazılacak; yol ve alan adları için
/// `ios/shared/.../model/SessionDeviceRules.kt` (FollowRules, AccountDeletionRules),
/// `ios/shared/.../profil/ProfileFragmentLogic.kt` ve `.../profil/AvatarConfigLogic.kt` içindeki sabitler
/// kullanılmalı.
@MainActor
protocol ProfilDeposu: AnyObject {
    /// Oturumdaki kullanıcı; oturum yoksa nil.
    var oturum: ProfilOturumBilgisi? { get }

    // MARK: Profil (ProfileFragment)

    /// `users/{uid}` (kendi) ya da `publicProfiles/{uid}` (başkası). Belge yoksa nil.
    func profilBelgesi(uid: String, baskasi: Bool) async throws -> ProfilBelgesi?

    /// GlobalLessonData.calculateGlobalCompletionPercentage(uid): `users/{uid}/lessonProgress/{1..6}/items`
    /// okunur, ortak koddaki `GlobalLessonData.calculateGlobalCompletionPercentage` ile hesaplanır.
    func tamamlanmaYuzdesi(uid: String) async -> Int

    /// TimeTracker.unsyncedTimeFlow'un anlık değeri: henüz Firestore'a yazılmamış süre (saniye).
    func senkronlanmamisSureSn() -> Int64

    /// `users/{uid}/badgeProgress/state` dokümanı (alan adı -> değer). Belge yoksa nil.
    func rozetDurumBelgesi(uid: String) async throws -> [String: Any]?

    /// Aynı dokümana `set(alanlar, merge)`; yalnızca kendi profilinde, eksik varsayılanlar için.
    func rozetVarsayilanlariniYaz(uid: String, alanlar: [String: Any]) async

    /// `users/{uid}/cupWayProgress/progress` dokümanı. Kendi profilinde Android eksik alanı 200 ile
    /// doldurur (altı `*CupRepository.fetchCupScore`); gerçek uygulama bunu burada yapmalı.
    func kupaIlerlemeBelgesi(uid: String) async throws -> [String: Any]?

    // MARK: Abaküs önizlemesi (AbacusCustomizationFirestore, BeadPurchaseFirestore)

    /// `users/{uid}/abacusCustomization/state.previewBeadType`; seçilmemişse Android kullanılan
    /// tiplerden rastgele birini gösterir. Okunamazsa nil.
    func onizlemeBoncukTipi(uid: String) async -> String?

    /// `users/{uid}/haveBead/{tip}.count` — sahip olunan boncuklar (tip adı -> adet).
    func sahipOlunanBoncuklar(uid: String) async -> [String: Int]

    /// AbacusCustomizationFirestore.setPreviewBeadType: `abacusCustomization/state.previewBeadType`.
    func onizlemeBoncukTipiniYaz(uid: String, tip: String) async

    // MARK: Takip (ProfileFragment, AddFriendFragment, FollowersFollowingFragment)

    /// `users/{hedefUid}/followers/{benimUid}` var mı.
    func takipEdiyorMuyum(hedefUid: String) async throws -> Bool

    /// Tek batch'te iki yazım (FollowRules.followWrites):
    /// `users/{hedef}/followers/{ben}` = BENİM kodum/adım, `users/{ben}/following/{hedef}` = HEDEFİN kodu/adı.
    /// Takipçi sekmesindeki "+" (geri takip et) da güncel Android'de bu yazımı yapar
    /// (FollowersFollowingFragment.followUser; eski çapraz yazım düzeltildi). DİKKAT: ortak koddaki
    /// `FollowRules.followBackWrites` hâlâ eski ÇAPRAZ yazımı veriyor; kullanılmamalı.
    func takipEt(hedefUid: String, hedefKod: String, hedefAd: String) async throws

    /// Takibi bırakma (Android: ProfileFragment.unfollowTargetUser, FollowersFollowingFragment.unfollowUser):
    /// tek batch'te `ProfileFragmentLogic.unfollowDeletePaths` yolları silinir
    /// (`users/{hedef}/followers/{ben}`, `users/{ben}/following/{hedef}`); sayaçları sunucu tetikleyicileri düşürür.
    func takibiBirak(hedefUid: String) async throws

    /// `users/{ben}/following` doküman kimlikleri.
    func takipEttiklerim() async -> Set<String>

    /// `users/{ben}/following` (sekme 0) ya da `followers` (sekme 1), `followedAt` azalan.
    /// `name` ya da `userId` alanı olmayan kayıtlar atlanır.
    func takipListesi(sekme: Int) async throws -> [ProfilTakipKaydi]

    /// `publicProfiles/{uid}.avatarConfig` (uid -> dizge; alanı olmayanlar sözlükte yok).
    /// Firestore `whereIn` en çok 30 değer alır; Android listeyi tek sorguda okuyor.
    func acikAvatarlar(uidler: [String]) async throws -> [String: String]

    /// `publicProfiles` üzerinde iki önek sorgusu (FriendSearchRules.queryEnd):
    /// `name` ve `userId` alanlarında `>= sorgu && <= sorgu + "\u{F8FF}"`, `limit(sayfaBoyu)`.
    /// `devam` true ise iki sorgu da kendi son belgesinden sürer (`startAfter`).
    /// Kendi kaydımız ve eksik alanlı belgeler atılır; avatar `avatarConfig` alanından.
    /// Sorgu hataları yutulur (boş sonuç). Dönen liste tekilleştirilmemiştir.
    func ara(sorgu: String, sayfaBoyu: Int, devam: Bool) async -> [ProfilAramaSonucu]

    // MARK: Avatar (AvatarStore) ve profil düzenleme (EditProfileFragment)

    /// Cihazdaki avatar kopyası (Android: SharedPreferences `avatar_prefs`, anahtar `active_{uid}`; kullanıcıya
    /// göre ayrı). Hiç kaydedilmemişse nil (çizen taraf varsayılan avatarı çizer). Alt çubuk ikonu ve profil
    /// açılışta ağı beklemeden bundan çizilir.
    var yerelAvatar: String? { get set }

    /// `users/{uid}.avatarConfig` güncellemesi (Android: AvatarStore.save; yazım başarısız olsa da yerel kayıt kalır).
    func avatarKaydet(_ config: String) async throws

    /// Auth `updateProfile(displayName)` (hata: `.kimlik`), ardından `users/{uid}.name` (hata: `.veritabani`).
    func adGuncelle(_ ad: String) async throws

    /// Auth `updatePassword` (son giriş eskiyse `.yenidenGirisGerekli`).
    func parolaGuncelle(_ parola: String) async throws

    // MARK: Hesap (AccountSettingsFragment, AccountDeletionHelper)

    /// MyFirebaseMessagingService.clearCurrentTokenFromFirestore() + AuthManager.logout().
    func cikisYap()

    /// AccountDeletionHelper.performCompleteAccountDeletion'ın yeniden giriş denetiminden SONRAKİ kısmı;
    /// adımlar ve silinen alt koleksiyonlar `AccountDeletionRules` içinde (takipçi/takip kayıtları,
    /// kendi alt koleksiyonları, `users/{uid}`, Auth kaydı). Apple ile girişte Apple belirteci de iptal
    /// edilmelidir (App Store kuralı 5.1.1(v)).
    func hesabiSil() async -> ProfilHesapSilmeSonucu

    // MARK: Geri bildirim (FeedbackFragment)

    /// SharedPreferences `feedback_prefs.timestamps` karşılığı (virgülle ayrılmış ms listesi).
    var geriBildirimZamanlari: String { get set }

    /// `feedback` koleksiyonuna `add` (FeedbackRules.feedbackDoc + `createdAt` sunucu zaman damgası).
    func geriBildirimGonder(mesaj: String) async throws
}
