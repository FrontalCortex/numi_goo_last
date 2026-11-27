import SwiftUI

// Ekran galerisinin, giriş görünümüyle AÇILAMAYAN iç durumu: profil düzenleme.
// (Giriş ve kayıt adımları artık GirisAkisi(servis:girisYapildi:ornek:) ile açılır.)
//
// DİKKAT: Bu dosya öteki Yonetim/ dosyalarından farklı olarak ekran klasörünün İÇ türlerine
// dayanır (Profil: ProfilGezinme, SahteProfilDeposu, ProfilDuzenleEkrani). O türlerin adı ya da
// kurucusu değişirse derleme burada kırılır; o durumda bu dosya silinip EkranGalerisi.swift'teki
// "profil-duzenle" dalı kaldırılabilir. Kalıcı çözüm: ProfilEkrani'ne isteğe bağlı bir
// "başlangıç ekranı" parametresi eklemek.

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

/// "Ses ve Bildirimler" ekranı: profilin geri yığınındaki ekran tek başına açılır.
@MainActor
struct EkranGalerisiSesAyarlari: View {
    @StateObject private var gezinme: ProfilGezinme

    init() {
        let nesne = ProfilGezinme(depo: SahteProfilDeposu())
        _gezinme = StateObject(wrappedValue: nesne)
    }

    var body: some View {
        ZStack {
            Color("background_color").ignoresSafeArea()
            ProfilSesAyarlariEkrani(gezinme: gezinme)
        }
    }
}
