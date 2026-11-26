import Foundation
import Shared

// Profil klasörünün ortak Kotlin koduna açılan TEK kapısı. Bütün `Shared` çağrıları buradadır;
// diğer Profil dosyaları `Shared`'i içe aktarmaz.
//
// Çağrılan Kotlin dosyaları:
//   ios/shared/.../profil/ProfileFragmentLogic.kt  (ProfileFragmentLogic, EditProfileRules,
//       AccountSettingsRules, FriendSearchRules, FeedbackRules) — bu iş sırasında eklendi;
//       `ios/docs/Shared.h` içinde HENÜZ YOK. Adlar başlıktaki kurallara göre yazıldı
//       (object -> `.shared`, const val -> aynı adlı özellik, Int -> Int32, Long -> Int64).
//   ios/shared/.../model/AuthRules.kt, SessionDeviceRules.kt — başlıkta doğrulandı.

/// Kupa yolu kartının sabit bilgisi (ortak kod: ProfileCupCard).
struct ProfilKupaKartTanimi {
    let baslik: String
    let animasyonDosyasi: String
    let yasakSimgesi: Bool
    /// `users/{uid}/cupWayProgress/progress` dokümanındaki alan.
    let alan: String
}

/// Rozet satırındaki bir yuva (ortak kod: ProfileBadgeSlotUi).
struct ProfilRozetYuvasi: Identifiable {
    let id: Int
    let tur: String
    let acik: Bool
    let degerGorunur: Bool
    let deger: String
    /// Alt katman Lottie dosyası; yoksa boş.
    let altDosya: String
    let ustDosya: String
    let ustKare: Float
    let ustOlcek: CGFloat
    let daireCerceve: Bool
    /// "BRONZE", "SILVER", "RED", "PURPLE", "ORIGINAL" ya da boş.
    let ton: String
}

enum ProfilOrtak {
    private static var mantik: ProfileFragmentLogic { ProfileFragmentLogic.shared }

    // MARK: Profil başlığı

    static func avatarAdi(_ seciliAvatar: Int) -> String {
        return mantik.avatarDrawableName(selectedAvatar: Int32(seciliAvatar))
    }

    static func avatarGecerli(_ seciliAvatar: Int) -> Bool {
        return mantik.isValidAvatar(selectedAvatar: Int32(seciliAvatar))
    }

    static func gorunenAd(belgeAdi: String?, oturumAdi: String?, baskasi: Bool) -> String {
        return mantik.displayName(docName: belgeAdi, authDisplayName: oturumAdi, isOtherUser: baskasi)
    }

    static func kullaniciSatiri(kod: String, yil: Int) -> String {
        return mantik.usernameAndDateText(userId: kod, year: Int32(yil))
    }

    static func hedefKod(kullaniciSatiri: String) -> String {
        return mantik.followTargetUserId(usernameAndDateText: kullaniciSatiri)
    }

    static func tamamlanmaMetni(_ yuzde: Int) -> String {
        return mantik.completionText(percentage: Int32(yuzde))
    }

    static var tamamlanmaBekleme: String { mantik.TEXT_COMPLETION_LOADING }

    static func sureMetni(_ saniye: Int64) -> String {
        return mantik.totalTimeText(totalSeconds: saniye)
    }

    static func abonelikMetni(_ plan: String?) -> String {
        return mantik.subscriptionText(plan: plan)
    }

    static var takipEtMetni: String { mantik.TEXT_FOLLOW }
    static var takipEdiliyorMetni: String { mantik.TEXT_FOLLOWING }
    static var takipEdildiBildirimi: String { mantik.TOAST_FOLLOWED }
    static var tekBoncukBildirimi: String { mantik.TOAST_SINGLE_BEAD }

    // MARK: Kupa yolu

    static var kupaKartlari: [ProfilKupaKartTanimi] {
        let kartlar: [ProfileCupCard] = mantik.CUP_CARDS
        return kartlar.map { kart in
            ProfilKupaKartTanimi(
                baslik: kart.title,
                animasyonDosyasi: kart.animFile,
                yasakSimgesi: kart.showForbiddenIcon,
                alan: kart.field
            )
        }
    }

    /// Alan yoksa ya da sayı değilse 200.
    static func kupaSkoru(_ hamDeger: Any?) -> Int {
        var deger: Any? = nil
        if let ham = hamDeger {
            deger = kotlinDegeri(ham)
        }
        return Int(mantik.cupScore(rawFieldValue: deger))
    }

    /// Swift tam sayılarını Kotlin'in `Number` olarak tanıdığı kutuya çevirir; diğer değerler aynen geçer.
    private static func kotlinDegeri(_ deger: Any) -> Any {
        if let tam = deger as? Int {
            return KotlinLong(longLong: Int64(tam))
        }
        if let tam64 = deger as? Int64 {
            return KotlinLong(longLong: tam64)
        }
        return deger
    }

    // MARK: Rozetler

    /// `users/{uid}/badgeProgress/state` dokümanından profil rozet satırı (belge yoksa boş ilerleme).
    /// Sayı alanları Kotlin tarafında `Number` olarak okunur; burada `KotlinLong`'a çevrilir.
    static func rozetYuvalari(durumBelgesi: [String: Any]?) -> [ProfilRozetYuvasi] {
        var kotlinBelgesi: [String: Any]? = nil
        if let belge = durumBelgesi {
            var cevrilmis: [String: Any] = [:]
            for (anahtar, deger) in belge {
                cevrilmis[anahtar] = kotlinDegeri(deger)
            }
            kotlinBelgesi = cevrilmis
        }
        let yuvalar: [ProfileBadgeSlotUi] = mantik.profileBadgeSlotsFromStateDoc(doc: kotlinBelgesi)
        var sonuc: [ProfilRozetYuvasi] = []
        for (sira, yuva) in yuvalar.enumerated() {
            sonuc.append(
                ProfilRozetYuvasi(
                    id: sira,
                    tur: yuva.kindName,
                    acik: yuva.unlocked,
                    degerGorunur: yuva.showValue,
                    deger: yuva.valueText,
                    altDosya: yuva.baseAsset,
                    ustDosya: yuva.topAsset,
                    ustKare: yuva.topHoldFrame,
                    ustOlcek: CGFloat(yuva.topScale),
                    daireCerceve: yuva.hasCircleFrame,
                    ton: yuva.toneName
                )
            )
        }
        return sonuc
    }

    /// Belgede bulunmayan varsayılan rozet alanları (yalnızca kendi profilinde yazılır).
    static func eksikRozetVarsayilanlari(mevcutAlanlar: Set<String>) -> [String: Any] {
        return mantik.missingBadgeDefaults(existingFields: mevcutAlanlar)
    }

    // MARK: Boncuk seçici

    /// Bütün boncuk tiplerinin adları (AbacusPreferences.BeadType sırasıyla).
    static var tumBoncukTipleri: [String] { mantik.ALL_BEAD_TYPE_NAMES }

    static func sahipOlunanBoncuklar(tumTipler: [String], adetler: [String: Int], baslangic: String) -> [String] {
        var kotlinAdetler: [String: KotlinInt] = [:]
        for (tip, adet) in adetler {
            kotlinAdetler[tip] = KotlinInt(int: Int32(adet))
        }
        return mantik.ownedPreviewBeadTypes(allTypeNames: tumTipler, ownedCounts: kotlinAdetler, initialType: baslangic)
    }

    /// Sonraki/önceki boncuğun sırası; tek boncuk varsa nil.
    static func boncukAdimi(sira: Int, adet: Int, ileri: Bool) -> Int? {
        let yeni = Int(mantik.previewBeadStep(index: Int32(sira), size: Int32(adet), forward: ileri))
        return yeni < 0 ? nil : yeni
    }

    static var boncukKayitGecikmesiNs: UInt64 {
        return UInt64(mantik.PREVIEW_BEAD_SYNC_DELAY_MS) * 1_000_000
    }

    // MARK: Profil düzenleme

    /// Ad doğrulaması (çağıran önce kırpar). Hata yoksa nil.
    static func adHatasi(_ ad: String) -> String? {
        return AuthRules.shared.validateName(name: ad)
    }

    static func parolaHatasi(_ parola: String) -> String? {
        return AuthRules.shared.validateNewPassword(newPassword: parola)
    }

    static func parolaAlaniGorunur(saglayicilar: [String], rol: String?, belgeOkundu: Bool) -> Bool {
        return EditProfileRules.shared.showPasswordField(providerIds: saglayicilar, role: rol, userDocLoaded: belgeOkundu)
    }

    static func parolaGuncellenmeli(parola: String, alanGorunur: Bool) -> Bool {
        return EditProfileRules.shared.shouldUpdatePassword(newPassword: parola, passwordFieldVisible: alanGorunur)
    }

    static func kullaniciKoduAlani(_ kod: String) -> String {
        return EditProfileRules.shared.usernameFieldText(userId: kod)
    }

    static var duzenleMetinleri: EditProfileRules { EditProfileRules.shared }

    // MARK: Hesap

    static func odemelerGorunur(plan: String?) -> Bool {
        return AccountSettingsRules.shared.showPayments(plan: plan)
    }

    /// Son girişten bu yana 270 sn'den fazla geçtiyse silmeden önce yeniden giriş istenir.
    static func yenidenGirisGerekli(sonGirisMs: Int64?, simdiMs: Int64) -> Bool {
        var kutulu: KotlinLong? = nil
        if let deger = sonGirisMs {
            kutulu = KotlinLong(longLong: deger)
        }
        return AccountDeletionRules.shared.requiresRecentLogin(lastSignInTimestampMs: kutulu, nowMs: simdiMs)
    }

    // MARK: Arkadaş arama ve takip listesi

    static var arama: FriendSearchRules { FriendSearchRules.shared }

    static func aramaTemizlenmeli(_ sorgu: String) -> Bool {
        return arama.shouldClearResults(query: sorgu)
    }

    static func aramaYapilmali(_ sorgu: String) -> Bool {
        return arama.shouldSearch(query: sorgu)
    }

    static func aramaSayfaBoyu(devam: Bool) -> Int {
        return Int(arama.pageSize(isLoadMore: devam))
    }

    static var aramaGecikmesiNs: UInt64 {
        return UInt64(arama.SEARCH_DELAY_MS) * 1_000_000
    }

    static func tekilUidler(_ uidler: [String]) -> [String] {
        return arama.dedupeUids(uids: uidler)
    }

    static func dahaFazlaVar(buTurSonucSayisi: Int) -> Bool {
        return arama.hasMoreResults(batchResultCount: Int32(buTurSonucSayisi))
    }

    static func sonucSayisiMetni(_ adet: Int) -> String {
        return arama.resultCountText(count: Int32(adet))
    }

    static func takipEdildiMetni(ad: String) -> String {
        return arama.followedToast(name: ad)
    }

    static func geriTakipEdildiMetni(ad: String) -> String {
        return arama.followedBackToast(name: ad)
    }

    static func bosListeMetni(sekme: Int) -> String {
        return arama.emptyListText(tab: Int32(sekme))
    }

    static func geriTakipDugmesiGorunur(sekme: Int, zatenTakip: Bool) -> Bool {
        return arama.showFollowBackButton(tab: Int32(sekme), alreadyFollowing: zatenTakip)
    }

    static func takipAvatarRengi(sira: Int) -> String {
        return arama.followAvatarColor(position: Int32(sira))
    }

    static func aramaAvatarRengi(sira: Int) -> String {
        return arama.searchAvatarColor(position: Int32(sira))
    }

    static func avatarHarfi(ad: String) -> String {
        return arama.avatarLetter(name: ad)
    }

    // MARK: Geri bildirim

    static var geriBildirim: FeedbackRules { FeedbackRules.shared }

    /// (izin var mı, geri yazılacak temizlenmiş zaman listesi)
    static func geriBildirimIzni(kayitliZamanlar: String, simdiMs: Int64) -> (Bool, String) {
        let kapi: FeedbackGate = geriBildirim.canSendFeedback(savedTimestamps: kayitliZamanlar, nowMs: simdiMs)
        return (kapi.allowed, kapi.prunedTimestamps)
    }

    static func geriBildirimKaydi(kayitliZamanlar: String, simdiMs: Int64) -> String {
        return geriBildirim.recordFeedbackTimestamp(savedTimestamps: kayitliZamanlar, nowMs: simdiMs)
    }

    static func geriBildirimGonderilebilir(_ metin: String) -> Bool {
        return geriBildirim.canSubmit(text: metin)
    }

    static func geriBildirimHataMetni(_ hata: String?) -> String {
        return geriBildirim.sendFailedToast(message: hata)
    }

    /// `feedback` koleksiyonuna eklenecek doküman (`createdAt` sunucu zaman damgasını yazan taraf ekler).
    static func geriBildirimBelgesi(
        uid: String?, eposta: String?, mesaj: String, surum: String?, isletimSistemi: String, cihaz: String
    ) -> [String: Any] {
        return geriBildirim.feedbackDoc(
            uid: uid, email: eposta, message: mesaj, appVersion: surum, osVersion: isletimSistemi, deviceModel: cihaz
        )
    }
}
