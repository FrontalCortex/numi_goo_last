import Shared
import SwiftUI

// Seri kırıkken ders dönüşünde çıkan "yeni seri başlat" ekranı.
// Android karşılığı: NewStreakFragment.kt + res/layout/fragment_new_streak.xml.
//
// İki sayfa: önce niyet ("Şimdi bir öğrenme alışkanlığı kazanmana yardım edelim.", maskot iki kez selam
// verir), sonra soru ("Kaç gün üst üste öğreneceksin?", maskotun elinde alev; ön seçim yok, "Başla" seçim
// yapılınca açılır). Ekran AÇILDIĞINDA soru bugün için sorulmuş sayılır; kapatılan ekran o gün yeniden
// sorulmaz. Seçim `startNewChallenge` ile yazılır ve sunucuya gönderilir.
//
// Basitleştirme: Android'de seçilen gün arttıkça maskotun elindeki alev büyür ve tavşan sevinir
// (BunnyMascotView.streakDays). `SobiMaskotGorunumu` bu girdiyi dışa açmıyor (Maskot/ klasörü): alev sabit.
// Android "GREET, times = 2" sonra beklemeye geçer; burada GREET hâli sürekli oynar.
struct SeriYeniSeriEkrani: View {
    @StateObject private var model: SeriYeniSeriModeli
    private let bitti: () -> Void

    /// - Parameters:
    ///   - servis: Seri verisi; verilmezse `SeriOrtak.servis`.
    ///   - soruSayfasiyla: Galeri için doğrudan soru sayfasıyla açar.
    ///   - bitti: Ekran nasıl kapanırsa kapansın (Başla ya da geri) çağrılır; ders sonrası akış sürer
    ///     (Android: onDismiss → MainActivity.onNewStreakPromptClosed).
    init(servis: SeriServisi? = nil, soruSayfasiyla: Bool = false, bitti: @escaping () -> Void) {
        let kaynak: SeriServisi = servis ?? SeriOrtak.servis
        _model = StateObject(wrappedValue: SeriYeniSeriModeli(servis: kaynak, soruda: soruSayfasiyla))
        self.bitti = bitti
    }

    var body: some View {
        VStack(spacing: 0) {
            // newStreakMascot: 190dp, üstten 48dp.
            SobiMaskotGorunumu(emote: model.soruda ? "FLAME" : "GREET")
                .frame(width: 190, height: 190)
                .padding(.top, 48)
                .id(model.soruda)
            SeriYeniSeriMetinleri(model: model)
            Spacer(minLength: 0)
            devamDugmesi
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color("background_color").ignoresSafeArea())
        .preferredColorScheme(.dark)
        .overlay(alignment: .topLeading) {
            kapatDugmesi
        }
        .onAppear { model.acildi() }
    }

    /// Android'de NewStreakFragment bir DialogFragment'tır ve iptal edilebilir: sistem geri tuşu ekranı kapatır
    /// (onDismiss → ders sonrası kuyruk sürer). iOS'ta donanım geri tuşu yoktur; bunun karşılığı sol üstte bir
    /// kapat düğmesidir (Android'de görsel karşılığı yok). `bitti` çağrılır; seçim yazılmaz, soru bugün için
    /// zaten sorulmuş sayıldığından yeniden sorulmaz.
    private var kapatDugmesi: some View {
        Button {
            bitti()
        } label: {
            Image("ic_close_black")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .foregroundColor(Color.white)
                .frame(width: 24, height: 24)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(.leading, 8)
        .padding(.top, 8)
    }

    /// newStreakContinue: 56dp, yanlardan 10dp, alttan 24dp, köşe 8dp, 18sp kalın; açıkken dark_primary,
    /// kapalıyken dark_text_secondary zemin.
    private var devamDugmesi: some View {
        let acik: Bool = model.devamAcik
        let metin: String = model.soruda ? Metin.al("ui_basla") : Metin.al("ui_devam_et")
        return Button {
            if model.devam() {
                bitti()
            }
        } label: {
            Text(metin)
                .font(Tema.yazi(18))
                .foregroundColor(Color.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(acik ? Color("dark_primary") : Color("dark_text_secondary"))
                )
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!acik)
        .padding(.horizontal, 10)
        .padding(.bottom, 24)
    }
}

/// Başlık (24sp kalın dark_text, satır arası 4dp, yanlardan 28dp, maskotun 32dp altında), alt başlık
/// (15sp #93A5B3, 10dp altında) ve seçenekler (yanlardan 24dp, 26dp altında). Soru sayfası sağdan kayarak gelir.
private struct SeriYeniSeriMetinleri: View {
    @ObservedObject var model: SeriYeniSeriModeli

    var body: some View {
        VStack(spacing: 0) {
            Text(model.soruda ? Metin.al("ui_kac_gun_ust_uste_ogreneceksin") : Metin.al("ui_simdi_bir_ogrenme_aliskanligi_kazanmana_yard"))
                .font(Tema.yazi(24))
                .foregroundColor(Color("dark_text"))
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 28)
                .padding(.top, 32)
            if model.soruda {
                soru
                    .transition(.move(edge: .trailing).combined(with: .opacity))
            }
        }
        .animation(.easeOut(duration: 0.22), value: model.soruda)
    }

    private var soru: some View {
        VStack(spacing: 0) {
            Text(Metin.al("ui_gunde_x_dakika_bu_secim_seri", model.hedefDakika))
                .font(Tema.yazi(15))
                .foregroundColor(SeriStil.soluk)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 28)
                .padding(.top, 10)
            SeriSecenekListesi(satirlar: model.satirlar, sagRengi: SeriStil.altin) { gun in
                model.sec(gun)
            }
            .padding(.horizontal, 24)
            .padding(.top, 26)
        }
    }
}

final class SeriYeniSeriModeli: ObservableObject {
    @Published private(set) var soruda: Bool
    @Published private(set) var secilen: Int = 0

    private let servis: SeriServisi
    private var isaretlendi: Bool = false

    init(servis: SeriServisi, soruda: Bool) {
        self.servis = servis
        self.soruda = soruda
    }

    var hedefDakika: Int {
        return servis.hedefDakika
    }

    /// Bilerek ön seçimsiz: söz kullanıcının kendisinin olmalı.
    var devamAcik: Bool {
        return !soruda || secilen > 0
    }

    var satirlar: [StreakOptionRowUi] {
        return StreakScreenLogic.shared.challengeRows(picked: Int32(secilen))
    }

    /// Android onViewCreated: bugün için soruldu sayılır.
    func acildi() {
        if isaretlendi { return }
        isaretlendi = true
        servis.yeniSeriSorusuGosterildi()
    }

    func sec(_ gun: Int) {
        secilen = gun
    }

    /// "Devam et" → soru sayfası; "Başla" → seçim yazılır. true dönerse ekran kapanmalı.
    func devam() -> Bool {
        if !soruda {
            soruda = true
            return false
        }
        if secilen <= 0 { return false }
        servis.yeniMeydanOkuma(secilen)
        return true
    }
}
