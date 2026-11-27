import Foundation

// Ekran galerisinin (Yonetim/EkranGalerisi.swift, `-ekran <ad>`) giriş akışı durumları.
// Ham değer galerideki addır; GirisAkisi(servis:girisYapildi:ornek:) bu durumla açılır.
// Örnek kipte açılış yönlendirmesi yapılmaz ve "başlangıç görüldü" bayrağı yazılmaz.
enum GirisOrnekAdimi: String, CaseIterable {
    /// Başlangıç ekranı (LoginStartActivity).
    case baslangic = "giris"
    /// Açılış (SplashActivity), yönlendirmesiz.
    case acilis = "giris-acilis"
    /// Kayıt sorularının ilk ekranı: Sobi'yle tanışma (MEET).
    case tanisma = "giris-tanisma"
    /// Öğrenci girişi, e-posta adımı (LoginActivity).
    case eposta = "giris-eposta"
    /// Kayıt ekranı, öğrenci (RegisterActivity).
    case kayit = "kayit"
    case yas = "kayit-yas"
    case kaynak = "kayit-kaynak"
    case abakus = "kayit-abakus"
    case hedef = "kayit-hedef"
    case yol = "kayit-yol"
    case meydan = "kayit-meydan-okuma"
    case hatirlatma = "kayit-hatirlatma"
    /// 13 yaş altı veli e-postası (ParentConsentFragment).
    case veliOnayi = "kayit-veli-onayi"
    /// Kayıt doğrulama kodu (OtpVerificationFragment, isRegistration).
    case kod = "kayit-kod"
}

@MainActor
enum GirisOrnekleri {
    static func kur(_ model: GirisModeli, _ adim: GirisOrnekAdimi) {
        model.ornekKipi = true
        let buYil: Int = Calendar.current.component(.year, from: Date())
        switch adim {
        case .baslangic:
            model.yigin = [.baslangic]
        case .acilis:
            model.yigin = [.acilis]
        case .eposta:
            model.yigin = [.baslangic, .ogrenciGiris]
        case .kayit:
            model.kayitDogumYili = buYil - 15
            model.kayitKaynak = "Okul/Öğretmen"
            model.yigin = [.baslangic, .kayit]
        case .veliOnayi:
            model.kayitDogumYili = buYil - 9
            model.kayitKaynak = "Okul/Öğretmen"
            model.yigin = [.baslangic, .kayit]
            model.veliOnayiniAc(cocukEposta: "cocuk@ornek.com", saglayici: false, dogumYili: buYil - 9)
        case .kod:
            model.kayitDogumYili = buYil - 15
            model.yigin = [.baslangic, .kayit, .kod(eposta: "ogrenci@ornek.com", tur: .kayit)]
        case .tanisma:
            model.yigin = [.baslangic]
            model.sorulariAc(ogretmen: false, onDoldurmaEposta: nil, yonlendirmeBildirimi: false)
        case .yas:
            soruAdimi(model, .yas)
        case .kaynak:
            soruAdimi(model, .kaynak)
        case .abakus:
            soruAdimi(model, .abakus)
        case .hedef:
            soruAdimi(model, .hedef)
        case .yol:
            soruAdimi(model, .yol)
        case .meydan:
            soruAdimi(model, .meydan)
        case .hatirlatma:
            soruAdimi(model, .hatirlatma)
        }
    }

    private static func soruAdimi(_ model: GirisModeli, _ hedef: GirisSoruAdimi) {
        model.yigin = [.baslangic]
        model.sorulariAc(ogretmen: false, onDoldurmaEposta: nil, yonlendirmeBildirimi: false)
        model.sorular?.ornekAdimaGit(hedef)
    }
}
