import Foundation
import Shared

// Profil klasörünün ortak Kotlin koduna açılan TEK kapısı. Bütün `Shared` çağrıları buradadır;
// diğer Profil dosyaları `Shared`'i içe aktarmaz.
//
// Çağrılan Kotlin dosyaları:
//   ios/shared/.../profil/ProfileFragmentLogic.kt  (ProfileFragmentLogic, EditProfileRules,
//       AccountSettingsRules, FriendSearchRules, FeedbackRules) — burada kullanılan adlar
//       `ios/docs/Shared.h` içinde doğrulandı (`followButtonTapUnfollows(following:)`,
//       `nudgedFollowersCount(current:delta:)` dahil).
//   ios/shared/.../model/AuthRules.kt, SessionDeviceRules.kt — başlıkta doğrulandı.
// Avatar kuralları ayrı kapıdan: Profil/Avatar/AvatarOrtakKod.swift (AvatarOrtak).

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
    //
    // Güncel Android metinleri strings.xml'den okuyor (appString); burada da `Metin.al`. Ortak koddaki
    // TEXT_* / TOAST_* sabitleri eski gömülü Türkçe metinlerdir, Türkçede aynıdır ama kullanılmıyor.

    /// Doküman adı → (yalnızca kendi profilinde) Auth görünen adı → "Kullanıcı" (ortak kodun sabiti;
    /// Android'de `ui_kullanici`, Türkçesi aynı).
    static func gorunenAd(belgeAdi: String?, oturumAdi: String?, baskasi: Bool) -> String {
        return mantik.displayName(docName: belgeAdi, authDisplayName: oturumAdi, isOtherUser: baskasi)
    }

    /// "@kimlik • 2024 YILINDA KATILDI"; kimlik boşsa yalnızca "2024 YILINDA KATILDI".
    static func kullaniciSatiri(kod: String, yil: Int) -> String {
        if kod.isEmpty {
            return Metin.al("ui_x_yilinda_katildi", yil)
        }
        return Metin.al("ui_x_x_yilinda_katildi", kod, yil)
    }

    /// Android: followTargetUser — hedefin kullanıcı kimliği ekrandaki satırdan ayıklanır.
    static func hedefKod(kullaniciSatiri: String) -> String {
        return mantik.followTargetUserId(usernameAndDateText: kullaniciSatiri)
    }

    static func tamamlanmaMetni(_ yuzde: Int) -> String {
        return Metin.al("ui_x_tamamlandi", yuzde)
    }

    static var tamamlanmaBekleme: String { Metin.al("ui_hesaplaniyor") }

    /// Android: updateTotalTimeDisplay — gün / saat / dakika ayrımı Android ile aynı, metinler string
    /// kaynaklarından (ui_x_gun_x_saat_x_dakika, ui_x_saat_x_dakika, plurals ui_x_dakika). Türkçe çıktı
    /// ortak koddaki `totalTimeText` ile aynıdır.
    static func sureMetni(_ saniye: Int64) -> String {
        let gun: Int64 = saniye / 86400
        let saat: Int64 = (saniye % 86400) / 3600
        let dakika: Int64 = (saniye % 3600) / 60
        if gun > 0 {
            return Metin.al("ui_x_gun_x_saat_x_dakika", gun, saat, dakika)
        }
        if saat > 0 {
            return Metin.al("ui_x_saat_x_dakika", saat, dakika)
        }
        return Metin.cogul("ui_x_dakika", Int(clamping: dakika), dakika)
    }

    /// "En uzun seri: 7 gün" (Android: appPlural(R.plurals.ui_en_uzun_seri_x_gun, n, n)).
    static func enUzunSeriMetni(_ gun: Int64) -> String {
        let adet: Int = Int(clamping: gun)
        return Metin.cogul("ui_en_uzun_seri_x_gun", adet, adet)
    }

    /// Belge yokken (Android: `ui_en_uzun_seri_0_gun`).
    static var enUzunSeriSifir: String { Metin.al("ui_en_uzun_seri_0_gun") }

    static var takipEtMetni: String { Metin.al("ui_takip_et") }
    static var takipEdiliyorMetni: String { Metin.al("ui_takip_ediliyor") }
    static var takipEdildiBildirimi: String { Metin.al("ui_takip_edildi") }
    static var takipBirakildiBildirimi: String { Metin.al("ui_takip_birakildi") }
    static var tekBoncukBildirimi: String { Metin.al("ui_sadece_1_adet_boncugun_var") }

    static func hataBildirimi(_ mesaj: String) -> String {
        return Metin.al("ui_hata_x", mesaj)
    }

    /// Takibi bırakma onayı: "X adlı kullanıcıyı takip etmeyi bırakmak istiyor musun?"
    static func takipBirakmaSorusu(ad: String) -> String {
        return Metin.al("ui_x_adli_kullaniciyi_takip_etmeyi_birakmak", ad)
    }

    /// Takip düğmesine dokunuş takibi bırakır mı (önce onay sorulur). Android: applyFollowButtonState.
    static func dokunusTakibiBirakir(takipte: Bool) -> Bool {
        return mantik.followButtonTapUnfollows(following: takipte)
    }

    /// Takip/bırakma sonrası ekrandaki takipçi sayısı (Android: nudgeFollowersCount); negatife inmez.
    static func duzeltilmisTakipciSayisi(_ simdiki: Int, _ fark: Int) -> Int {
        return Int(mantik.nudgedFollowersCount(current: Int32(clamping: simdiki), delta: Int32(clamping: fark)))
    }

    // MARK: Kupa yolu

    static var kupaKartlari: [ProfilKupaKartTanimi] {
        let kartlar: [ProfileCupCard] = mantik.CUP_CARDS
        return kartlar.map { kart in
            ProfilKupaKartTanimi(
                baslik: Metin.cevir(kart.title),
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

    /// "Kullanıcı bulunamadı." (AccountDeletionHelper'ın oturum yokken verdiği hata metni).
    static var kullaniciBulunamadi: String { AccountDeletionRules.shared.MESSAGE_USER_NOT_FOUND }

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
