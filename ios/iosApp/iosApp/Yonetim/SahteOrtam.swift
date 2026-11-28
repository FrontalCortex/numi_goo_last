import Foundation
import Shared

// Uygulamanın oturum ömürlü SAHTE ortamı: Firebase / StoreKit bağlanana kadar bütün ekranların paylaştığı
// tek veri kaynağı. Gerçek servislere geçiş bu dosyadan (ve yöneticinin ekranları kurduğu yerlerden) yapılır.
//
// Hangi nesne neyi tutar (Android karşılığı):
//   kimlik + kayit      SahteKimlikServisi + SahteKullaniciKayitServisi   FirebaseAuth + `users/{uid}`
//   cuzdan              MagazaSahteCuzdanServisi.ortak                     UserWalletFirestore (`users/{uid}.currency/keys/plan`)
//   enerji              MagazaSahteEnerjiServisi.ortak (ortak EnergyManager)  EnergyManager (`energy_full_time`)
//   magaza              MagazaBagimliliklar.sahte (cüzdan + can + seri)    ShopFragment bağımlılıkları
//   kupa                KupaOrtami.sahte'nin depoları + ana can            `cupWayProgress`, `cupPathRewards`, `badgeProgress`
//   profil              SahteProfilDeposu(baglanti:)                       ProfileFragment (`users/{uid}`, `publicProfiles`)
//   sahiplikler         hesap başına OzellestirmeSahteSahiplikServisi      `users/{uid}/haveBead`, `haveFrame`
//   seri                SeriOrtak.servis (cüzdana kendisi yazar)           StreakRepository + StreakSyncService
//   rozet kazanımı      SahteRozetKazanimServisi.ortak (kupa rozet deposuna yazar)
//
// Hesap değişimi (giriş / misafir / çıkış; `hesabaGec`): Android'de bu verilerin hepsi uid'ye bağlı
// belgelerdedir. Sahte ortamda bırakılan hesabın durumu saklanır, gelen hesabınki geri yüklenir:
//   - İlk bağlanan hesap (uygulama açılınca ilk giriş ya da misafir) başlangıç örnek durumunu devralır.
//   - Daha önce görülen hesap kaldığı yerden sürer.
//   - Yeni hesap: cüzdan `users/{uid}` belgesindeki currency/keys (yoksa ortak kodun kayıt varsayılanları),
//     can dolu, plan Free, kupa/defter/rozet belgesi boş, ders ilerlemesi boş. İstisna: hesapsız 1. öğreticiden
//     sonra açılan hesaba misafirin ders ilerlemesi taşınır (Android tutorial1_login_flow_pending).
//   - Silinen hesabın durumu saklanmaz.
//
// Ekran galerisi (`-ekran …`) bu ortamı kullanmaz; ekranlar kendi varsayılan örnek verileriyle kurulur.

/// Özelleştirme sahiplik kayıtları, hesap başına (Android: `users/{uid}/haveBead`, `haveFrame`).
final class SahteOrtamSahiplikleri {
    private var servisler: [String: OzellestirmeSahteSahiplikServisi] = [:]

    func servis(uid: String) -> OzellestirmeSahteSahiplikServisi {
        if let mevcut = servisler[uid] {
            return mevcut
        }
        let yeni = OzellestirmeSahteSahiplikServisi()
        servisler[uid] = yeni
        return yeni
    }

    func sil(uid: String) {
        servisler[uid] = nil
    }
}

/// Kupa Yolu panelinin can göstergesi ve BAŞLAT kararı ana can servisinden okunur
/// (Android: TasksFragment → MainActivity.getEnergyManager / isInfiniteEnergy; plan PlanStatus'tan).
final class SahteOrtamKupaEnerjisi: KupaEnerjiKaynagi {
    private let enerji: MagazaEnerjiServisi
    private let cuzdan: MagazaCuzdanServisi

    init(enerji: MagazaEnerjiServisi, cuzdan: MagazaCuzdanServisi) {
        self.enerji = enerji
        self.cuzdan = cuzdan
    }

    var sonsuzEnerji: Bool {
        return enerji.durum().sonsuz
    }

    var guncelEnerji: Int {
        return enerji.durum().enerji
    }

    var proPlan: Bool {
        return PlanStatus.shared.isProPlan(plan: cuzdan.cuzdan.plan)
    }
}

/// Kör dersin (kupa kipinde yanlış cevap / terk) can harcaması ana can servisine gider
/// (Android: BlindingLessonFragment → MainActivity.getEnergyManager().useEnergy(n); sonuç beklenmez).
struct SahteOrtamKorDersEnerjisi: KorDersEnerjiServisi {
    let enerji: MagazaEnerjiServisi

    func enerjiHarca(_ miktar: Int) {
        enerji.harca(adet: miktar, tamam: { _ in })
    }
}

/// Bir hesabın saklanan durumu.
struct SahteOrtamHesapKaydi {
    let cuzdan: MagazaCuzdan
    let enerji: MagazaSahteEnerjiKaydi
    let kupaSkorlari: [String: Int]
    let kupaDefteri: [String: Any]
    let rozetBelgesi: [String: Any]?
    let partlar: [Int32: [LessonItem]]
}

final class SahteOrtam {
    /// Uygulamanın tek sahte ortamı (oturum ömürlü).
    static let ortak: SahteOrtam = SahteOrtam()

    /// Galeri kipindeki kabuğun profil sekmesi için örnek depo ("Elif").
    @MainActor static let galeriProfili: SahteProfilDeposu = SahteProfilDeposu()

    static let misafirAnahtari: String = "misafir"

    let kayit: SahteKullaniciKayitServisi
    let kimlik: SahteKimlikServisi
    let cuzdan: MagazaSahteCuzdanServisi
    let enerji: MagazaSahteEnerjiServisi
    let magaza: MagazaBagimliliklar
    /// Kupa ekranlarının servisleri: `KupaOrtami.sahte`'nin depoları, can kaynağı ana can servisi.
    let kupa: KupaOrtami
    let kupaDeposu: SahteKupaDeposu?
    let rozetDeposu: SahteKupaRozetDeposu?
    let sahiplikler: SahteOrtamSahiplikleri
    let profil: SahteProfilDeposu
    let korDersEnerjisi: SahteOrtamKorDersEnerjisi
    /// Sandık ödülü ana cüzdana, kupa yolu sandığı kupa defterine yazılır.
    let sandikOdulServisi: SahteSandikOdulServisi

    /// Bağlı hesabın uid'si (misafirde ve henüz bağlanmadan nil).
    private(set) var aktifUid: String? = nil
    /// Bağlı hesabın anahtarı (uid ya da `misafirAnahtari`); nil = henüz hiçbir hesaba bağlanmadı.
    private var aktifAnahtar: String? = nil
    private var kayitlar: [String: SahteOrtamHesapKaydi] = [:]

    private init() {
        let kayitDeposu = SahteKullaniciKayitServisi()
        var ayarlar = SahteKimlikAyarlari()
        if UserDefaults.standard.string(forKey: "oturum") == "acik" {
            // `-oturum acik`: SahteKimlikServisi'nin hazır öğrenci hesabının oturumu açık başlar.
            ayarlar.acilistaOturumEposta = "ogrenci@sorobit.test"
        }
        let kimlikServisi = SahteKimlikServisi(ayarlar: ayarlar, kayit: kayitDeposu)
        let anaCuzdan: MagazaSahteCuzdanServisi = MagazaSahteCuzdanServisi.ortak
        let anaEnerji: MagazaSahteEnerjiServisi = MagazaSahteEnerjiServisi.ortak
        let kupaSkor: SahteKupaDeposu? = KupaOrtami.sahte.skorDeposu as? SahteKupaDeposu
        let kupaRozet: SahteKupaRozetDeposu? = KupaOrtami.sahte.rozetDeposu as? SahteKupaRozetDeposu
        let sahiplikKayitlari = SahteOrtamSahiplikleri()

        var kupaOrtami: KupaOrtami = KupaOrtami.sahte
        kupaOrtami.enerjiKaynagi = SahteOrtamKupaEnerjisi(enerji: anaEnerji, cuzdan: anaCuzdan)

        let baglanti = SahteProfilBaglantisi(
            kimlik: kimlikServisi,
            cuzdan: anaCuzdan,
            kupaDeposu: kupaSkor,
            rozetDeposu: kupaRozet,
            sahiplikler: sahiplikKayitlari
        )

        kayit = kayitDeposu
        kimlik = kimlikServisi
        cuzdan = anaCuzdan
        enerji = anaEnerji
        magaza = MagazaBagimliliklar.sahte
        kupa = kupaOrtami
        kupaDeposu = kupaSkor
        rozetDeposu = kupaRozet
        sahiplikler = sahiplikKayitlari
        profil = SahteProfilDeposu(baglanti: baglanti)
        korDersEnerjisi = SahteOrtamKorDersEnerjisi(enerji: anaEnerji)
        sandikOdulServisi = SahteSandikOdulServisi(cuzdan: anaCuzdan, kupaDeposu: kupaSkor)
    }

    // MARK: Ekranlara verilenler

    /// Özelleştirme sekmesinin servisleri: oturumdaki hesabın sahiplik kayıtları ve yerel ayar dosyası.
    /// Misafirde kimlik nil: satın alma ve yuva panelleri açılmaz (Android AbacusCustomizationFragment:
    /// `FirebaseAuth.currentUser?.uid ?: return`).
    func ozellestirmeBagimliliklari() -> OzellestirmeBagimliliklar {
        let uid: String? = aktifUid
        let sahiplik: OzellestirmeSahiplikServisi
        if let kimlikNo = uid {
            sahiplik = sahiplikler.servis(uid: kimlikNo)
        } else {
            sahiplik = OzellestirmeSahteSahiplikServisi()
        }
        return OzellestirmeBagimliliklar(
            sahiplik: sahiplik,
            cuzdan: cuzdan,
            depo: OzellestirmeYerelDepo(kullaniciKimligi: uid),
            kullaniciKimligi: uid
        )
    }

    // MARK: Hesap değişimi

    /// Bağlı hesabı değiştirir (`uid` nil = misafir). Değişiklik olduysa true.
    /// - Parameter misafirIlerlemesiniTasi: misafirden YENİ bir hesaba geçerken misafirin ders ilerlemesi
    ///   yeni hesaba taşınsın mı (hesapsız 1. öğreticiden sonra kayıt).
    @MainActor
    @discardableResult
    func hesabaGec(uid: String?, ilerleme: YonetimIlerlemeDeposu, misafirIlerlemesiniTasi: Bool) -> Bool {
        let yeni: String = uid ?? SahteOrtam.misafirAnahtari
        if aktifAnahtar == yeni {
            return false
        }
        let eski: String? = aktifAnahtar
        if let eskiAnahtar = eski {
            if hesapDuruyor(eskiAnahtar) {
                kayitlar[eskiAnahtar] = anlikKayit(ilerleme: ilerleme)
            } else {
                kayitlar[eskiAnahtar] = nil
            }
        }
        var belge: [String: Any]? = nil
        if let kimlikNo = uid {
            belge = kayit.anlikBelge(uid: kimlikNo)
        }
        if let saklanan = kayitlar[yeni] {
            kaydiYukle(saklanan, ilerleme: ilerleme)
        } else if eski != nil {
            let misafirden: Bool = eski == SahteOrtam.misafirAnahtari
            let tasinan: [Int32: [LessonItem]] = (misafirden && misafirIlerlemesiniTasi) ? ilerleme.hesapPartlari : [:]
            kaydiYukle(SahteOrtam.yeniHesapKaydi(belge: belge, partlar: tasinan), ilerleme: ilerleme)
        }
        // eski == nil: ilk bağlanan hesap başlangıç örnek durumunu devralır (hiçbir şey yüklenmez).

        // Rol ve plan can kuralını belirler (Android: EnergyManager.setUserRoleApproval / setUserPlan).
        let kurallar = AuthRules.shared
        let rol: String = (belge?[kurallar.FIELD_ROLE] as? String) ?? ""
        let onayli: Bool = (belge?[kurallar.FIELD_TEACHER_APPROVED] as? Bool) ?? false
        enerji.rolUygula(rol: rol, ogretmenOnayli: onayli)
        enerji.planiUygula(cuzdan.cuzdan.plan)
        aktifAnahtar = yeni
        aktifUid = uid
        return true
    }

    /// Hesap hâlâ var mı (silinen hesabın durumu saklanmaz).
    @MainActor
    private func hesapDuruyor(_ anahtar: String) -> Bool {
        if anahtar == SahteOrtam.misafirAnahtari {
            return true
        }
        return kayit.anlikBelge(uid: anahtar) != nil
    }

    private func anlikKayit(ilerleme: YonetimIlerlemeDeposu) -> SahteOrtamHesapKaydi {
        return SahteOrtamHesapKaydi(
            cuzdan: cuzdan.cuzdan,
            enerji: enerji.hesapKaydi(),
            kupaSkorlari: kupaDeposu?.skorlar ?? [:],
            kupaDefteri: kupaDeposu?.defter ?? [:],
            rozetBelgesi: rozetDeposu?.dokuman,
            partlar: ilerleme.hesapPartlari
        )
    }

    private func kaydiYukle(_ hesap: SahteOrtamHesapKaydi, ilerleme: YonetimIlerlemeDeposu) {
        cuzdan.hesabinCuzdaniniYukle(hesap.cuzdan)
        enerji.hesapKaydiniYukle(hesap.enerji)
        if let depo = kupaDeposu {
            depo.skorlar = hesap.kupaSkorlari
            depo.defter = hesap.kupaDefteri
        }
        if let depo = rozetDeposu {
            depo.dokuman = hesap.rozetBelgesi
        }
        ilerleme.hesabaGec(partlar: hesap.partlar)
    }

    /// Yeni hesabın başlangıç durumu (Android: kayıtta yazılan cüzdan alanları; diğer belgeler yok).
    private static func yeniHesapKaydi(belge: [String: Any]?, partlar: [Int32: [LessonItem]]) -> SahteOrtamHesapKaydi {
        let cuzdanKurali = UserWalletFirestore.shared
        let varsayilanAltin: Int = Int(cuzdanKurali.DEFAULT_CURRENCY)
        let varsayilanAnahtar: Int = Int(cuzdanKurali.DEFAULT_KEYS)
        let altin: Int = (belge?[cuzdanKurali.FIELD_CURRENCY] as? NSNumber)?.intValue ?? varsayilanAltin
        let anahtar: Int = (belge?[cuzdanKurali.FIELD_KEYS] as? NSNumber)?.intValue ?? varsayilanAnahtar
        let yeniCuzdan = MagazaCuzdan(
            altin: altin,
            anahtar: anahtar,
            soruKredisi: 0,
            sobiKredisi: 0,
            sobiUcretsizSinir: 2,
            plan: "Free"
        )
        return SahteOrtamHesapKaydi(
            cuzdan: yeniCuzdan,
            enerji: MagazaSahteEnerjiServisi.yeniHesapKaydi(),
            kupaSkorlari: [:],
            kupaDefteri: [:],
            rozetBelgesi: nil,
            partlar: partlar
        )
    }
}

// MARK: - Yöneticinin ekranlara verdiği ortak nesneler

extension UygulamaYoneticisi {
    /// Profil sekmesinin deposu: oturum boyunca TEK nesne (sekmeden çıkıp dönünce veriler kalır).
    /// Galeri kipinde örnek kişi.
    @MainActor
    var profilDeposu: ProfilDeposu {
        if ornekKipi {
            return SahteOrtam.galeriProfili
        }
        return SahteOrtam.ortak.profil
    }

    /// Özelleştirme sekmesinin servisleri (galeri kipinde örnek servisler).
    var ozellestirmeServisleri: OzellestirmeBagimliliklar {
        if ornekKipi {
            return OzellestirmeBagimliliklar.sahte
        }
        return SahteOrtam.ortak.ozellestirmeBagimliliklari()
    }
}
