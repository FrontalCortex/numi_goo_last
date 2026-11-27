import Foundation
import SwiftUI

// Profil sayfasının (kendi profili ya da başkasının profili) durumu.
// Android karşılığı: ProfileFragment — loadUserData, showAvatar / applyHeaderColor, updateFollowButtonState,
// applyFollowButtonState, followTargetUser, confirmUnfollow / unfollowTargetUser, nudgeFollowersCount,
// loadBadgeProgressFromFirestore, loadAbacusPreview, loadCupPathScores(ForUser).
// Metin biçimleri, rozet seçimi, kupa kartı tablosu ve avatar kuralları ortak koddan gelir
// (ProfilOrtak, AvatarOrtak).

/// Kupa yolu kartının ekrandaki hali.
struct ProfilKupaKarti: Identifiable {
    let id: Int
    let tanim: ProfilKupaKartTanimi
    /// Yüklenene kadar "-".
    var skor: String
}

@MainActor
final class ProfilSayfaModeli: ObservableObject {
    /// Başkasının profili gösteriliyorsa onun uid'si; kendi profilinde nil.
    let hedefUid: String?
    private let depo: ProfilDeposu

    /// Büyük avatarın dizgesi; nil ise `non_user` görseli (Android: showAvatar(null)).
    @Published var avatarConfig: String? = nil
    /// Avatar alanının, üst panelin ve durum çubuğunun rengi (AARRGGBB); nil → background_color.
    @Published var ustAlanRengi: String? = nil
    @Published var ad: String = ""
    @Published var kullaniciSatiri: String = ""
    @Published var takipSayisi: String = "-"
    @Published var takipciSayisi: String = "-"

    /// Android: showLoadingState / hideLoadingState — üç ilerleme satırı yüklenene kadar gizlidir.
    @Published var ilerlemeGorunur: Bool = false
    @Published var tamamlanma: String = ""
    @Published var toplamSure: String = ""
    /// "En uzun seri: N gün" (Android: tvLongestStreak; eski "plan" satırının yerinde).
    @Published var enUzunSeri: String = ""

    @Published var kupalar: [ProfilKupaKarti] = []
    @Published var rozetler: [ProfilRozetYuvasi] = []
    /// Android: profileBadgesCard rozet belgesi okunana kadar gizlidir.
    @Published var rozetKartiGorunur: Bool = false

    /// Abaküs önizleme kartındaki boncuk tipi (BeadType adı); bilinmiyorsa nil.
    @Published var boncukTipi: String? = nil
    @Published var abakusPaneliAcik: Bool = false

    // Başkasının profilindeki takip düğmesi (Android: applyFollowButtonState — metin ve davranış birlikte).
    @Published var takipte: Bool = false
    @Published var takipDugmesiEtkin: Bool = true
    /// "Takibi bırak" onayı açık mı (Android: confirmUnfollow).
    @Published var takipBirakmaOnayiAcik: Bool = false

    private var yuklendi: Bool = false
    private var yukleniyor: Bool = false
    private var boncukKayitGorevi: Task<Void, Never>? = nil

    init(depo: ProfilDeposu, hedefUid: String?) {
        self.depo = depo
        self.hedefUid = hedefUid
        var sira = 0
        var kartlar: [ProfilKupaKarti] = []
        for tanim in ProfilOrtak.kupaKartlari {
            kartlar.append(ProfilKupaKarti(id: sira, tanim: tanim, skor: "-"))
            sira += 1
        }
        kupalar = kartlar
    }

    /// Android: isOtherUser.
    var baskasi: Bool {
        guard let hedef = hedefUid else { return false }
        return hedef != depo.oturum?.uid
    }

    private var uid: String? {
        return hedefUid ?? depo.oturum?.uid
    }

    var takipDugmesiMetni: String {
        return takipte ? ProfilOrtak.takipEdiliyorMetni : ProfilOrtak.takipEtMetni
    }

    /// Android: onViewCreated → loadUserData; onResume'da henüz yüklenmediyse yeniden.
    func gorundu() {
        if !yuklendi && !yukleniyor {
            Task { await yukle() }
        }
    }

    /// Profil düzenleme / avatar değişikliği sonrası yeniden okuma.
    func yenile() {
        if !yukleniyor {
            Task { await yukle() }
        }
    }

    // MARK: Avatar

    /// Android: showAvatar + applyHeaderColor. Avatar dairesiz çizilir; arka plan rengi bütün avatar alanını,
    /// kendi profilimizde sabit üst paneli ve durum çubuğunu kaplar.
    private func avatariGoster(_ config: String?) {
        guard let c = config, AvatarOrtak.avatarVar(c) else {
            avatarConfig = nil
            ustAlanRengi = nil
            return
        }
        avatarConfig = c
        ustAlanRengi = AvatarOrtak.ustAlanRengi(c)
    }

    /// Kendi profilimizde kullanılan avatar (Android: AvatarStore.loadActive — hiç kaydedilmemişse varsayılan).
    private var yerelAvatar: String {
        return depo.yerelAvatar ?? AvatarOrtak.varsayilan
    }

    /// Avatar düzenleme ekranında kaydedilince (ProfilGezinme.avatarKaydet) yerel kopyadan yeniden çizer.
    /// Android'de profil geri yığından dönünce yeniden kurulup `AvatarStore.loadActive` ile çizer.
    func yerelAvatariGoster() {
        if !baskasi {
            avatariGoster(yerelAvatar)
        }
    }

    // MARK: Yükleme

    func yukle() async {
        if yukleniyor { return }
        guard let uid = uid else { return }
        yukleniyor = true
        defer {
            yukleniyor = false
            yuklendi = true
        }

        // Kendi profilimizde yerel avatar hemen; başkasınınki Firestore'dan gelene kadar non_user.
        avatariGoster(baskasi ? nil : yerelAvatar)

        let belge: ProfilBelgesi?
        do {
            belge = try await depo.profilBelgesi(uid: uid, baskasi: baskasi)
        } catch {
            // Android: hata dalı — boş rozet durumu, satırlar görünür, rozet kartı gizli kalır.
            rozetler = ProfilOrtak.rozetYuvalari(durumBelgesi: nil)
            ilerlemeGorunur = true
            return
        }

        guard let profil = belge else {
            // Android: belge yok — boş rozet durumu, avatar (kendi: yerel), seri 0.
            rozetler = ProfilOrtak.rozetYuvalari(durumBelgesi: nil)
            avatariGoster(baskasi ? nil : yerelAvatar)
            enUzunSeri = ProfilOrtak.enUzunSeriSifir
            ilerlemeGorunur = true
            return
        }

        // Android bu üç yüklemeyi belge gelir gelmez başlatır; sonuçlar ayrı ayrı gelir.
        Task { await rozetleriYukle(uid: uid) }
        Task { await boncuguYukle(uid: uid) }
        Task { await kupalariYukle(uid: uid) }

        if baskasi {
            avatariGoster(profil.avatarConfig)
        } else {
            // Yeni cihaz / yeniden kurulum: Firestore'daki avatar yerele alınır (AvatarStore.applyRemote).
            if let alinacak = AvatarOrtak.yereleAlinacak(yerel: depo.yerelAvatar, uzak: profil.avatarConfig) {
                depo.yerelAvatar = alinacak
            }
            avatariGoster(yerelAvatar)
        }

        let oturum = depo.oturum
        ad = ProfilOrtak.gorunenAd(belgeAdi: profil.ad, oturumAdi: oturum?.gorunenAd, baskasi: baskasi)

        // Katılım yılı: kendi profilinde Auth kaydı, başkasında belgedeki createdAt (yoksa bu yıl).
        let buYil = Calendar.current.component(.year, from: Date())
        let yil: Int = baskasi ? (profil.olusturmaYili ?? buYil) : (oturum?.kayitYili ?? buYil)
        kullaniciSatiri = ProfilOrtak.kullaniciSatiri(kod: profil.kullaniciKodu, yil: yil)

        tamamlanma = ProfilOrtak.tamamlanmaBekleme
        Task {
            let yuzde = await depo.tamamlanmaYuzdesi(uid: uid)
            self.tamamlanma = ProfilOrtak.tamamlanmaMetni(yuzde)
        }

        // Kendi profilinde henüz Firestore'a yazılmamış süre de eklenir (Android: TimeTracker.unsyncedTimeFlow;
        // orada akış dinlenir, burada tek seferlik okunur).
        var sure: Int64 = profil.toplamSureSn
        if !baskasi {
            sure += depo.senkronlanmamisSureSn()
        }
        toplamSure = ProfilOrtak.sureMetni(sure)

        // En uzun günlük seri: dokümandan (başkasının profilinde publicProfiles aynası). Kendi profilinde
        // seri servisinden (sahte sunucunun `users/{uid}.longestStreak` karşılığı; tek doğruluk kaynağı).
        let enUzun: Int64 = baskasi ? profil.enUzunSeri : Int64(SeriOrtak.servis.profilEnUzunSeri)
        enUzunSeri = ProfilOrtak.enUzunSeriMetni(enUzun)

        takipciSayisi = String(profil.takipciSayisi)
        takipSayisi = String(profil.takipSayisi)

        if baskasi {
            Task { await takipDugmesiniGuncelle(hedef: uid) }
        }
        ilerlemeGorunur = true
    }

    // MARK: Rozetler

    private func rozetleriYukle(uid: String) async {
        rozetKartiGorunur = false
        do {
            let belge = try await depo.rozetDurumBelgesi(uid: uid)
            // Eksik varsayılan alanlar yalnızca kendi profilimizde yazılır.
            if !baskasi {
                let mevcut = Set((belge ?? [:]).keys)
                let eksik = ProfilOrtak.eksikRozetVarsayilanlari(mevcutAlanlar: mevcut)
                if !eksik.isEmpty {
                    await depo.rozetVarsayilanlariniYaz(uid: uid, alanlar: eksik)
                }
            }
            rozetler = ProfilOrtak.rozetYuvalari(durumBelgesi: belge)
        } catch {
            rozetler = ProfilOrtak.rozetYuvalari(durumBelgesi: nil)
        }
        rozetKartiGorunur = true
    }

    // MARK: Kupa yolu

    private func kupalariYukle(uid: String) async {
        var belge: [String: Any]? = nil
        do {
            belge = try await depo.kupaIlerlemeBelgesi(uid: uid)
        } catch {
            belge = nil
        }
        var yeni = kupalar
        for sira in yeni.indices {
            let ham: Any? = belge?[yeni[sira].tanim.alan]
            yeni[sira].skor = String(ProfilOrtak.kupaSkoru(ham))
        }
        kupalar = yeni
    }

    func kupaSecimi(_ kart: ProfilKupaKarti) -> ProfilKupaSecimi? {
        guard let uid = uid else { return nil }
        return ProfilKupaSecimi(
            uid: uid, alan: kart.tanim.alan, baslik: kart.tanim.baslik, animasyonDosyasi: kart.tanim.animasyonDosyasi
        )
    }

    // MARK: Abaküs önizlemesi

    private func boncuguYukle(uid: String) async {
        boncukTipi = await depo.onizlemeBoncukTipi(uid: uid)
    }

    /// Android: schedulePreviewBeadTypeSync — art arda ok dokunuşlarında yazım 500 ms geciktirilir.
    func boncukSecildi(_ tip: String) {
        boncukTipi = tip
        guard let uid = depo.oturum?.uid else { return }
        boncukKayitGorevi?.cancel()
        let gecikme = ProfilOrtak.boncukKayitGecikmesiNs
        let kaynak = depo
        boncukKayitGorevi = Task {
            try? await Task.sleep(nanoseconds: gecikme)
            if Task.isCancelled { return }
            await kaynak.onizlemeBoncukTipiniYaz(uid: uid, tip: tip)
        }
    }

    func sahipOlunanBoncuklar() async -> [String: Int] {
        guard let uid = depo.oturum?.uid else { return [:] }
        return await depo.sahipOlunanBoncuklar(uid: uid)
    }

    // MARK: Takip (başkasının profili)

    private func takipDugmesiniGuncelle(hedef: String) async {
        // Android: okuma hatasında düğme olduğu gibi kalır.
        guard let durum = try? await depo.takipEdiyorMuyum(hedefUid: hedef) else { return }
        takipte = durum
        takipDugmesiEtkin = true
    }

    /// Takip düğmesine dokunuş: takip ediliyorsa onay sorar (sonra bırakır), değilse takip eder.
    func takipDugmesineDokunuldu(bildir: @escaping (String) -> Void) {
        if ProfilOrtak.dokunusTakibiBirakir(takipte: takipte) {
            takipBirakmaOnayiAcik = true
        } else {
            takipEt(bildir: bildir)
        }
    }

    /// Android: followTargetUser. Sonuç bildirimi `bildir` ile dışarı verilir.
    private func takipEt(bildir: @escaping (String) -> Void) {
        guard let hedef = hedefUid, depo.oturum != nil else { return }
        // Hedefin adı ve kodu ekrandaki metinlerden alınır (Android'deki gibi).
        let hedefAd = ad
        let hedefKod = ProfilOrtak.hedefKod(kullaniciSatiri: kullaniciSatiri)
        takipDugmesiEtkin = false
        let kaynak = depo
        Task {
            do {
                try await kaynak.takipEt(hedefUid: hedef, hedefKod: hedefKod, hedefAd: hedefAd)
                self.takipte = true
                self.takipDugmesiEtkin = true
                self.takipciSayisiniDuzelt(1)
                bildir(ProfilOrtak.takipEdildiBildirimi)
            } catch {
                self.takipDugmesiEtkin = true
                bildir(ProfilOrtak.hataBildirimi(ProfilSayfaModeli.hataMetni(error)))
            }
        }
    }

    /// Android: unfollowTargetUser (onaydan sonra).
    func takibiBirak(bildir: @escaping (String) -> Void) {
        guard let hedef = hedefUid, depo.oturum != nil else { return }
        takipDugmesiEtkin = false
        let kaynak = depo
        Task {
            do {
                try await kaynak.takibiBirak(hedefUid: hedef)
                self.takipte = false
                self.takipDugmesiEtkin = true
                self.takipciSayisiniDuzelt(-1)
                bildir(ProfilOrtak.takipBirakildiBildirimi)
            } catch {
                self.takipDugmesiEtkin = true
                bildir(ProfilOrtak.hataBildirimi(ProfilSayfaModeli.hataMetni(error)))
            }
        }
    }

    /// Android: nudgeFollowersCount — gerçek sayaç sunucuda birkaç saniyede güncellenir; ekrandaki sayı hemen
    /// düzeltilir. Sayı henüz yüklenmediyse ("-") dokunulmaz.
    private func takipciSayisiniDuzelt(_ fark: Int) {
        guard let simdiki = Int(takipciSayisi) else { return }
        takipciSayisi = String(ProfilOrtak.duzeltilmisTakipciSayisi(simdiki, fark))
    }

    static func hataMetni(_ hata: Error) -> String {
        if let profilHatasi = hata as? ProfilHata {
            return profilHatasi.mesaj
        }
        return hata.localizedDescription
    }
}
