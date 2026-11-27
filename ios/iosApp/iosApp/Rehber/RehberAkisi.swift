import SwiftUI

// Rehber akışı: sayfa listesi, ilerleme, panelin giriş / çıkış durumu.
//
// Android karşılıkları: AbacusFragment ve BlindingLessonFragment içindeki rehber akışı
// (setGuideContents, showGuideContent, showNextGuideContent, showPreviousGuideContent,
// handleRequiredClickTarget, completeGuideIfWaitingForRulesSelection, enable/disableGuidePanelMode,
// show/hideGuidePanelWithAnimation) ve GuidePanelView (harita; showNextContent, hideToLeft).
//
// Görünüm (RehberPaneli / RehberKatmani) bu nesneyi okur; ekran yalnızca `baslat` ve kapanışları bağlar.
// Hedef öğelerin ekranda var olup olmadığını görünüm bilir (anchorPreference); ilerleme kararları
// `kayitli` kümesiyle verilir: hedefi ekranda olmayan "dokunmak zorunlu" sayfa ileri düğmesiyle geçilir.

final class RehberAkisi: ObservableObject {
    @Published private(set) var adimlar: [RehberAdimi] = []
    @Published private(set) var indeks: Int = 0
    /// Panel görünür (giriş kaymasından çıkış kaymasının sonuna kadar).
    @Published private(set) var aktif: Bool = false
    /// Karartma / dokunma engeli (Android: overlay + disableOtherViews). Çıkış kaymasından önce kalkar.
    @Published private(set) var engel: Bool = false
    @Published private(set) var konum: RehberPanelKonumu = .bekliyor
    /// Giriş ya da çıkış kayması sürüyor; düğmeler çalışmaz (GuidePanelView.isAnimating).
    @Published private(set) var animasyonda: Bool = false

    /// "rod4BottomBead4" gibi bir boncuğu oynatır (Android: abacusController.animateGuideBead).
    var boncukOynat: ((String) -> Void)?
    /// Zorunlu dokunma hedefine basıldı; ekran hedefin asıl işini yapar (kural kitabını açmak, ayar
    /// panelini açmak, liderlik tablosuna gitmek …).
    var hedefTiklandi: ((RehberHedefi) -> Void)?
    /// "Kural seçimi bekleyen" sayfadan geri dönülürken açık kural kitabı kapatılır
    /// (Android: closeRulesBookIfOpen).
    var kuralKitabiniKapat: (() -> Void)?
    /// Rehber tamamen kapandı. `true`: son sayfadan ileri ile normal çıkış (Android: showCurrentOperation
    /// çağrılan yol); `false`: hedefe dokunarak çıkış.
    var bitti: ((Bool) -> Void)?

    private var baslatildi: Bool = false
    private var kapaniyor: Bool = false
    private let girisSuresi: Double
    private let cikisSuresi: Double

    init(girisSuresi: Double = 0.3, cikisSuresi: Double = 0.3) {
        self.girisSuresi = girisSuresi
        self.cikisSuresi = cikisSuresi
    }

    /// Geçerli sayfa; liste boşsa nil.
    var gecerliAdim: RehberAdimi? {
        if indeks >= 0 && indeks < adimlar.count {
            return adimlar[indeks]
        }
        return nil
    }

    // MARK: Başlatma

    /// Rehberi başlatır. Ekran hemen kilitlenir (Android: overlay alfa 0,01), `gecikme` sonra ilk sayfa
    /// gösterilir ve panel kayarak girer (Android: postDelayedSafely(500)). Bir akış yalnızca bir kez
    /// başlar; `sifirla` ile yeniden kullanılabilir. `acildi`: panel kaymaya başlarken (gösterim kaydı).
    func baslat(_ yeniAdimlar: [RehberAdimi], gecikme: Double, acildi: (() -> Void)?) {
        if baslatildi || yeniAdimlar.isEmpty {
            return
        }
        baslatildi = true
        kapaniyor = false
        adimlar = yeniAdimlar
        indeks = 0
        konum = .bekliyor
        animasyonda = true
        engel = true
        aktif = true
        let sure: Double = girisSuresi
        DispatchQueue.main.asyncAfter(deadline: .now() + gecikme) { [weak self] in
            guard let kendisi = self, kendisi.aktif, !kendisi.kapaniyor else { return }
            kendisi.adimiGoster(0, boncuklu: true)
            withAnimation(Animation.easeInOut(duration: sure)) {
                kendisi.konum = .icerde
            }
            acildi?()
            DispatchQueue.main.asyncAfter(deadline: .now() + sure) { [weak kendisi] in
                kendisi?.animasyonda = false
            }
        }
    }

    /// Akışı baştan kullanıma hazırlar (harita rehberi tek nesneyle tekrar kullanılır).
    func sifirla() {
        baslatildi = false
        kapaniyor = false
        adimlar = []
        indeks = 0
        aktif = false
        engel = false
        animasyonda = false
        konum = .bekliyor
    }

    // MARK: İlerleme

    /// İleri düğmesi görünür mü (Android: btnForward; hedefli son sayfada ve zorunlu dokunma sayfalarında gizli /
    /// işlevsiz).
    func ileriGorunur(kayitli: Set<RehberHedefi>) -> Bool {
        guard let adim = gecerliAdim else { return false }
        return !ilerlemeEngelli(adim, kayitli: kayitli)
    }

    /// Zorunlu dokunma hedefi ekrandaysa ya da kural seçimi bekleniyorsa ileri düğmesi işlemez.
    private func ilerlemeEngelli(_ adim: RehberAdimi, kayitli: Set<RehberHedefi>) -> Bool {
        if adim.tiklamaGerekli, let hedef = adim.hedef, kayitli.contains(hedef) {
            return true
        }
        if adim.kuralSecimiBekler && kayitli.contains(.kuralKitabiDugmesi) {
            return true
        }
        return false
    }

    /// btnForward.
    func ileri(kayitli: Set<RehberHedefi>) {
        guard aktif, engel, !animasyonda, let adim = gecerliAdim else { return }
        if ilerlemeEngelli(adim, kayitli: kayitli) {
            return
        }
        sonrakiAdim()
    }

    /// showNextGuideContent: ayrılan sayfanın bitiş boncukları oynar; son sayfaysa rehber kapanır.
    private func sonrakiAdim() {
        guard let adim = gecerliAdim else { return }
        if let oynat = boncukOynat {
            for kimlik in adim.bitisBoncuklari {
                oynat(kimlik)
            }
        }
        if indeks >= adimlar.count - 1 {
            kapat(normal: true)
            return
        }
        adimiGoster(indeks + 1, boncuklu: true)
    }

    /// btnBack: yalnızca önceki sayfaya döner; ilk sayfada işlevsiz.
    func geri() {
        guard aktif, engel, !animasyonda, indeks > 0, let adim = gecerliAdim else { return }
        // Geri giderken backBeadIds (yoksa beadIds) ters yönde oynar; bitiş boncukları oynamaz.
        let liste: [String] = adim.geriBoncuklari ?? adim.boncuklar
        if let oynat = boncukOynat {
            for kimlik in RehberIcerigi.tersiIcinSadelestir(liste) {
                oynat(kimlik)
            }
        }
        if adim.kuralSecimiBekler {
            kuralKitabiniKapat?()
        }
        adimiGoster(indeks - 1, boncuklu: false)
    }

    /// Zorunlu dokunma hedefine basıldı (Android: handleRequiredClickTarget).
    func hedefeTiklandi(kayitli: Set<RehberHedefi>) {
        guard aktif, engel, !animasyonda, let adim = gecerliAdim else { return }
        guard adim.tiklamaGerekli, let hedef = adim.hedef, kayitli.contains(hedef) else { return }
        hedefTiklandi?(hedef)
        if adim.tiklamaIlerletir {
            sonrakiAdim()
        } else {
            kapat(normal: false)
        }
    }

    /// Kural tablosundan bir kural seçildi (Android: completeGuideIfWaitingForRulesSelection).
    func kuralSecildi() {
        guard aktif, engel, let adim = gecerliAdim, adim.kuralSecimiBekler else { return }
        sonrakiAdim()
    }

    // MARK: Gösterim ve kapanış

    /// showGuideContent (boncuklu) / showGuideContentWithoutBeads.
    private func adimiGoster(_ yeni: Int, boncuklu: Bool) {
        indeks = yeni
        if !boncuklu {
            return
        }
        guard let oynat = boncukOynat, yeni >= 0, yeni < adimlar.count else { return }
        for kimlik in adimlar[yeni].boncuklar {
            oynat(kimlik)
        }
    }

    /// disableGuidePanelMode + hideGuidePanelWithAnimation: engel hemen kalkar, panel sola kayarak çıkar.
    private func kapat(normal: Bool) {
        if kapaniyor {
            return
        }
        kapaniyor = true
        engel = false
        animasyonda = true
        let sure: Double = cikisSuresi
        withAnimation(Animation.easeInOut(duration: sure)) {
            konum = .cikti
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + sure) { [weak self] in
            guard let kendisi = self, kendisi.kapaniyor else { return }
            kendisi.aktif = false
            kendisi.animasyonda = false
            kendisi.bitti?(normal)
        }
    }
}
