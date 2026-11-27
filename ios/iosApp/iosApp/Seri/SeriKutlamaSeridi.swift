import Shared
import SwiftUI

// Günlük hedef kutlaması ve dondurma bildirimi (ana kabuğun üstünde).
// Android karşılığı: MainActivity.maybeShowStreakCelebration / showStreakCelebration / hideStreakCelebration
// + activity_main.xml `streakCelebration`; dondurma bildirimi MainActivity.refreshStreakUi → Toast.
//
// Kutlama ders ekranındayken hak edilir ama orada gösterilmez: güvenli bir ekrana (kabuk, üstünde katman
// yokken) dönülünce şerit üst çubuğun altında belirir, 4 sn sonra çekilir; dokununca seri ekranı açılır.
// Tam ekran değil: dersten yeni çıkan çocuğun önünü ikinci kez kesmemek için.

/// Kabuktaki seri bildirimlerinin durumu (tek nesne; yönetici yazar, kabuk çizer).
final class SeriAkisDurumu: ObservableObject {
    static let ortak = SeriAkisDurumu()

    /// Gösterilmeyi bekleyen ya da gösterilen kutlamanın seri değeri.
    @Published private(set) var kutlama: Int? = nil
    /// Android Toast: dondurma harcandı bildirimi.
    @Published private(set) var bildirim: String? = nil

    private var kutlamaSayaci: Int = 0
    private var bildirimSayaci: Int = 0

    func kutlamaGoster(_ seri: Int) {
        if seri <= 0 { return }
        kutlamaSayaci += 1
        kutlama = seri
    }

    /// Şerit ekrana geldi: 4 sn sonra çekilir (Android: STREAK_CELEBRATION_MS).
    func kutlamaGorundu() {
        let sira: Int = kutlamaSayaci
        let sure: Double = Double(StreakScreenLogic.shared.CELEBRATION_MS) / 1000.0
        DispatchQueue.main.asyncAfter(deadline: .now() + sure) { [weak self] in
            guard let self = self, self.kutlamaSayaci == sira else { return }
            self.kutlama = nil
        }
    }

    func kutlamayiKapat() {
        kutlamaSayaci += 1
        kutlama = nil
    }

    func bildir(_ metin: String) {
        bildirimSayaci += 1
        let sira: Int = bildirimSayaci
        bildirim = metin
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.5) { [weak self] in
            guard let self = self, self.bildirimSayaci == sira else { return }
            self.bildirim = nil
        }
    }
}

/// Kabuğun içerik alanına bindirilen katman: üstte kutlama şeridi, altta bildirim. Dokunmaları yalnızca
/// şerit alır; kalan alan alttaki ekrana geçer.
struct SeriKabukKatmani: View {
    @ObservedObject var durum: SeriAkisDurumu
    /// Şerit yalnızca üst çubuğun göründüğü sekmelerde (Android: currencyPanel görünür değilse "not_home_screen").
    let seritGorunur: Bool
    /// Şeride dokunuldu: seri ekranı açılmalı.
    let seriAc: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            if seritGorunur, let seri = durum.kutlama {
                SeriKutlamaSeridi(seri: seri) {
                    durum.kutlamayiKapat()
                    seriAc()
                }
                .onAppear { durum.kutlamaGorundu() }
                .transition(.move(edge: .top).combined(with: .opacity))
            }
            Spacer(minLength: 0)
                .allowsHitTesting(false)
            if let metin = durum.bildirim {
                SeriBildirimGorunumu(metin: metin)
                    .allowsHitTesting(false)
                    .transition(.opacity)
            }
        }
        .animation(.easeOut(duration: 0.26), value: durum.kutlama)
        .animation(.easeInOut(duration: 0.2), value: durum.bildirim)
    }
}

/// streakCelebration: yanlardan 16dp, üstten 8dp; zemin bg_streak_celebration (soldan sağa #E65100 → #FF9800,
/// köşe 16dp); iç boşluk 14/12/16/12dp; 44dp alev (fire_anim, sürekli); 12dp sonra başlık (15sp kalın beyaz)
/// ve alt başlık (13sp #FFE0B2, 2dp).
struct SeriKutlamaSeridi: View {
    let seri: Int
    let dokunuldu: () -> Void

    var body: some View {
        Button(action: dokunuldu) {
            HStack(spacing: 0) {
                LottieGorunumu("fire_anim", dongu: true)
                    .frame(width: 44, height: 44)
                VStack(alignment: .leading, spacing: 2) {
                    Text(Metin.al("ui_bugunku_hedefini_tamamladin"))
                        .font(Tema.yazi(15))
                        .foregroundColor(Color.white)
                    Text(SeriMetin.yaz(StreakScreenLogic.shared.celebrationSubtitle(streak: Int32(seri))))
                        .font(Tema.yazi(13))
                        .foregroundColor(SeriStil.kutlamaAlt)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.leading, 12)
            }
            .padding(.leading, 14)
            .padding(.trailing, 16)
            .padding(.vertical, 12)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(LinearGradient(colors: [SeriStil.kutlamaBas, SeriStil.turuncu], startPoint: .leading, endPoint: .trailing))
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }
}
