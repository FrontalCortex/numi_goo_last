import Foundation
import Shared
import SwiftUI

// Günlük serinin uygulamaya bağlandığı yerler (Seri/ ekranlarını yönetici bağlar).
//
// Android karşılıkları:
//   MainActivity.refreshStreakUi        → seriyiYenile (oturumuYenile'den): üst çubuk, kutlama, dondurma bildirimi, eşitleme
//   MainActivity.openStreakFragment     → seriAc
//   StreakFragment.openShopForFreeze    → seriDondurmaMagazasiniAc (MainActivity.openShopFragment(focusStreakFreeze = true))
//   MainActivity.showNewStreakStep      → seriSorusunuDene (ders sonrası kuyruğundaki "yeni seri" adımı)
//   NumiGooApplication → StudyTimeTracker.setActiveScreen → seriCalismaEkraniniGuncelle
//   refreshStreakUi → bindToUser + applyPendingSignup → seriHesabiniBagla
//
// Ders sonrası sıra (Android MainActivity.runPostLessonQueue): reklam → sezon kapısı → rozet → YENİ SERİ →
// öğretmene sorma → puanlama → maraton rehberi → kupa yolu. iOS'ta bunlardan yalnızca "yeni seri" var;
// sonuç → sandık zinciri haritaya dönerken sorulur. Kutlama şeridi kuyruğun dışında: katman kalmayınca.

/// Seri katmanları (YonetimEkrani.seri).
enum YonetimSeriKatmani {
    /// Günlük seri ekranı (StreakFragment).
    case seri
    /// Yeni seri sorusu (NewStreakFragment).
    case yeniSeri
    /// Mağaza, seri dondurma kartına kaydırılmış (ShopFragment.newInstance(focusStreakFreeze = true)).
    case dondurmaMagazasi
}

extension UygulamaYoneticisi {

    /// Uygulamanın tek seri kaynağı (mağaza kartı ve profil de bunu okur).
    var seri: SahteSeriServisi {
        return SeriOrtak.servis
    }

    /// init'te bir kez: seri değişince (eşitleme, ödül, dondurma) üst çubuk hemen tazelensin.
    func seriyiBagla() {
        _ = seri.dinle { [weak self] in
            self?.oturumuYenile()
        }
    }

    /// Android: MainActivity.refreshStreakUi. oturumuYenile içinden çağrılır.
    func seriyiYenile() {
        let durum: StreakState = seri.durum()
        let gun: Int = Int(durum.current)
        if oturum.seri != gun {
            oturum.seri = gun
        }
        let akis: SeriAkisDurumu = SeriAkisDurumu.ortak
        // Kutlama yalnızca kabukta, üstünde ekran yokken (Android: streakCelebrationBlockReason).
        if asama == .kabuk && katmanlar.isEmpty {
            let kutlama: Int = seri.kutlamayiAl()
            if kutlama > 0 {
                akis.kutlamaGoster(kutlama)
            }
        } else if akis.kutlama != nil {
            // Şerit açıkken üstüne bir ekran geldiyse çekilir.
            akis.kutlamayiKapat()
        }
        if let haber = seri.dondurmaBildiriminiAl() {
            akis.bildir(Metin.al(StreakScreenLogic.shared.freezeNoticeKey(notice: haber)))
        }
        // Gün ders ekranındayken tutturulur; eşitleme burada tetiklenir (gönderecek bir şey yoksa kendini eler).
        seri.esitle(tamam: {})
    }

    /// Android: MainActivity.openStreakFragment (üstüne ekler; zaten açıksa açmaz).
    func seriAc() {
        if let ust = katmanlar.last, case .seri(.seri) = ust.ekran {
            return
        }
        ac(.seri(.seri))
    }

    /// Android: StreakFragment.openShopForFreeze → openShopFragment(focusStreakFreeze = true).
    func seriDondurmaMagazasiniAc() {
        if let ust = katmanlar.last, ust.ekran.magazaMi {
            return
        }
        ac(.seri(.dondurmaMagazasi))
    }

    /// Ders sonrası "yeni seri" adımı (Android: finalizeMapReturnAfterLessonClaim → newStreakPromptQueued →
    /// showNewStreakStep; Görevler/kupa dönüşünde requestNewStreakPromptOnTasks). Koşul: seri YOK ve bugün sorulmadı.
    func seriSorusunuDene() {
        if asama != .kabuk {
            return
        }
        // Gün az önce bitmiş olabilir: önce tazelenir (seri başladıysa soru sorulmaz).
        _ = seri.durum()
        if !seri.yeniSeriSorusuGerekli() {
            // Kuyruğun sıradaki adımı: ünite maratonu rehberi (Android: showMarathonGuideStep, yeni seriden sonra).
            maratonRehberiniDene()
            return
        }
        if let ust = katmanlar.last, case .seri(.yeniSeri) = ust.ekran {
            return
        }
        ac(.seri(.yeniSeri))
    }

    /// Önde çalışma sayılan bir ders ekranı var mı (Android: StudyTimeTracker.STUDY_SCREENS). Katman yığını
    /// değişince ve uygulama öne/arkaya geçince çağrılır. `aktif`: uygulama önde mi.
    func seriCalismaEkraniniGuncelle(aktif: Bool) {
        var ad: String? = nil
        if aktif, let ust = katmanlar.last {
            switch ust.ekran {
            case .abakusDers:
                ad = "AbacusFragment"
            case .korDers:
                ad = "BlindingLessonFragment"
            case .ogretici:
                ad = "TutorialFragment"
            default:
                ad = nil
            }
        }
        seri.calismaEkrani(ad)
    }

    /// Oturumdaki hesabı seriye bağlar ve kayıt sorularının bekleyen cevaplarını uygular (hesap yeniyse) ya da
    /// atar (Android: refreshStreakUi → bindToUser + applyPendingSignup). Kabuk açılınca çağrılır.
    /// Hesap açılış zamanı: Android FirebaseUser.metadata.creationTimestamp; sahte serviste `users/{uid}.createdAt`.
    @MainActor
    func seriHesabiniBagla() {
        guard let hesap = kimlikServisi.mevcutOturum else {
            return
        }
        let belge: [String: Any]? = kimlikServisi.kayit.anlikBelge(uid: hesap.uid)
        let acilisSayisi: NSNumber? = belge?[AuthRules.shared.FIELD_CREATED_AT] as? NSNumber
        let acilis: Int64? = acilisSayisi?.int64Value
        let yerel = GirisYerelDepo()
        let bekleyen: StreakPendingSignup? = yerel.seriCevaplari()
        seri.hesabaBagla(uid: hesap.uid, hesapAcilisMs: acilis, bekleyenKayit: bekleyen)
        if bekleyen != nil {
            // Android: sonuç ne olursa olsun bekleyen cevaplar silinir.
            yerel.seriCevaplariniSil()
        }
        oturumuYenile()
    }
}

// MARK: - Katman görünümü

extension YonetimKatmanGorunumu {
    @ViewBuilder
    func seriKatmani(_ tur: YonetimSeriKatmani) -> some View {
        switch tur {
        case .seri:
            seriEkrani
        case .yeniSeri:
            yeniSeriEkrani
        case .dondurmaMagazasi:
            dondurmaMagazasi
        }
    }

    private var seriEkrani: some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return SeriEkrani(
            servis: nesne.seri,
            kapat: { nesne.kapat(no) },
            dondurmaMagazasiniAc: { nesne.seriDondurmaMagazasiniAc() },
            cuzdanDegisti: { nesne.oturumuYenile() }
        )
    }

    private var yeniSeriEkrani: some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return SeriYeniSeriEkrani(
            servis: nesne.seri,
            bitti: {
                nesne.kapat(no)
                // Yeni seri sorusu kapandı: kuyruğun sıradaki adımı ünite maratonu rehberi.
                nesne.maratonRehberiniDene()
            }
        )
    }

    private var dondurmaMagazasi: some View {
        let nesne: UygulamaYoneticisi = yonetici
        let no: Int = katman.id
        return MagazaEkrani(
            kapat: { nesne.kapat(no) },
            sandikAc: { nonce in nesne.reklamSandiginiAc(nonce: nonce) },
            planKarsilastirmaAc: { nesne.planKarsilastirmaAc() },
            odak: .seriDondurma
        )
    }
}
