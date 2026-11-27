import SwiftUI

// Ekran galerisinin, giriş görünümüyle AÇILAMAYAN iki iç durumu: kayıt ekranı ve profil düzenleme.
//
// DİKKAT: Bu dosya öteki Yonetim/ dosyalarından farklı olarak ekran klasörlerinin İÇ türlerine
// dayanır (Giris: GirisModeli, GirisYerelDepo, GirisEkran, KayitGorunumu; Profil: ProfilGezinme,
// SahteProfilDeposu, ProfilDuzenleEkrani). O türlerin adı ya da kurucusu değişirse derleme burada
// kırılır; o durumda bu dosya silinip EkranGalerisi.swift'teki "kayit" ve "profil-duzenle"
// dalları kaldırılabilir (başka hiçbir yer bu iki görünümü kullanmaz). Kalıcı çözüm: GirisAkisi'ne
// ve ProfilEkrani'ne isteğe bağlı bir "başlangıç ekranı" parametresi eklemek.

/// Kayıt ekranı (öğrenci): giriş akışı modeli doğrudan kayıt adımına getirilir.
@MainActor
struct EkranGalerisiKayit: View {
    @StateObject private var model: GirisModeli

    init() {
        let nesne = GirisModeli(servis: SahteKimlikServisi(), depo: GirisYerelDepo(), girisYapildi: {})
        nesne.yigin = [GirisEkran.baslangic, GirisEkran.kayit]
        _model = StateObject(wrappedValue: nesne)
    }

    var body: some View {
        ZStack {
            // GirisAkisi'nin zemini.
            Color("dark_background").ignoresSafeArea()
            KayitGorunumu(model: model)
        }
    }
}

/// "Profili Düzenle" ekranı: profilin geri yığınındaki ekran tek başına açılır.
@MainActor
struct EkranGalerisiProfilDuzenle: View {
    @StateObject private var gezinme: ProfilGezinme

    init() {
        let nesne = ProfilGezinme(depo: SahteProfilDeposu())
        _gezinme = StateObject(wrappedValue: nesne)
    }

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            ProfilDuzenleEkrani(gezinme: gezinme)
        }
    }
}
