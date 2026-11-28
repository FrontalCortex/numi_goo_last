import Foundation
import Shared

// Sobi testinin oynatıcısı: sorular, sayıların akışı, abaküs kaydı, cevap, sonuç paneli, özet.
//
// Android kaynağı: homework/HomeworkPlayerFragment.kt (Sobi testi kipi: newSobiTest → startSobiTest,
// showQuestion, startFlash / showFlashNumber / hideFlashNumber, onKontrol, countRules, bindSobiReview,
// onReset, onSkip, nextQuestion, finishSobiTest). Saf kısım ortak kodda: SobiTestOturumu (etiketler,
// değerlendirme, kural sayımı, analiz kaydı, sonuç satırları ve yeni seviyeler).
//
// Zamanlama (Android ile aynı): sayının ekranda kalmadığı pay = aralık × 0,15 (40...250 ms); sayı
// (aralık − pay) kadar görünür, söner, pay kadar sonra sıradaki gelir. Son sayı ekranda kalır ve Kontrol
// hemen açılır. Çarpmada akış yok: işlem tahtada sabit.

/// Cevaptan sonra alttan kayan panel.
struct SobiTestCevapPaneli: Equatable {
    let dogru: Bool
    let baslik: String
    /// Yanlışta işlemin tamamı ("12 + 7 + 5 = 24"); doğruda nil.
    let mesaj: String?
    /// "Sobi ile analiz et" düğmesi.
    let analiz: Bool
}

/// Test bitti: özet ekranının içeriği.
struct SobiTestOzet: Equatable {
    let skor: String
    let mesaj: String
}

/// Oynatıcının dışarıya verdiği sonuç.
enum SobiTestBitis {
    /// "Sandığını aç": sıradan sandık (Android NewChestFragment, kaynak CHEST_SOURCE_SOBI_TEST).
    case sandik
}

final class SobiTestOynaticiModeli: ObservableObject {
    @Published private(set) var soruSirasi: Int = 0
    @Published private(set) var adimMetni: String = ""
    @Published private(set) var tahta: String = ""
    @Published private(set) var dugmeMetni: String = ""
    @Published private(set) var dugmeEtkin: Bool = false
    /// Abaküssüz soruda cevap kutusu (sayılar bitince).
    @Published private(set) var cevapKutusu: Bool = false
    @Published var cevapMetni: String = ""
    @Published private(set) var abakusGorunur: Bool = true
    /// Atla düğmesi (çarpmada gizli).
    @Published private(set) var atlaGorunur: Bool = true
    @Published private(set) var panel: SobiTestCevapPaneli? = nil
    @Published private(set) var ozet: SobiTestOzet? = nil

    let abakus: AbakusDenetleyici = AbakusDenetleyici()
    let oturum: SobiTestOturumu

    private let soruSayisi: Int
    private var cevaplandi: Bool = false
    private var akiyor: Bool = false
    private var akisBasladi: Bool = false
    private var bosta: Bool = false
    private var kayitAcik: Bool = false
    private var etiketler: [String] = []
    private var etiketSirasi: Int = 0
    private var gosterimMs: Int = 0
    private var bosluk: Int = 0
    private var zamanlayici: DispatchWorkItem? = nil
    private var kayitZamanlari: [Int64] = []
    private var kayitDegerleri: [Int64] = []
    private var gorunmeAnlari: [Int64] = []
    private var sonVerilen: Int64 = 0
    private var bitti: Bool = false
    private let kalici: Bool

    /// - Parameter kalici: false ise (ekran galerisi) bitişte sayaç ve seviyeler yazılmaz.
    init(baslangic: SobiTestBaslangici, kalici: Bool = true) {
        oturum = SobiTestOturumu(plan: baslangic.plan, stats: kalici ? SahteSobiAnalizKuralDeposu.ortak.depo : nil)
        soruSayisi = Int(oturum.questionCount)
        self.kalici = kalici
    }

    /// Hazır özetle (ekran galerisi "sobi-test-sonuc").
    func ozetiGoster(_ hazir: SobiTestOzet) {
        bitti = true
        ozet = hazir
    }

    var soruVar: Bool {
        return soruSayisi > 0
    }

    /// Yarıdaysa çıkışta onay sorulur (Android requestQuit: midBlock).
    var yarida: Bool {
        return ozet == nil && soruVar && (soruSirasi > 0 || cevaplandi || akiyor)
    }

    private var i32: Int32 {
        return Int32(soruSirasi)
    }

    private var abakuslu: Bool {
        return oturum.withAbacus(index: i32)
    }

    // MARK: Soru akışı

    func baslat() {
        guard soruVar, ozet == nil else { return }
        soruyuGoster()
    }

    /// Android showQuestion.
    private func soruyuGoster() {
        cevaplandi = false
        akisBasladi = false
        panel = nil
        let no: Int = soruSirasi + 1
        adimMetni = abakuslu
            ? Metin.al("sobi_test_step", no, soruSayisi)
            : Metin.al("sobi_test_step_mental", no, soruSayisi)
        abakusGorunur = abakuslu
        abakus.sifirla(animasyonlu: true)
        cevapMetni = ""
        cevapKutusu = false
        let carpma: Bool = oturum.isMultiplication(index: i32)
        atlaGorunur = !carpma
        if carpma {
            tahta = oturum.questionLine(index: i32)
            akisBitti()
            return
        }
        // Başla'ya basınca akar: çocuk hazır olmadan ilk sayı geçip gitmesin.
        tahta = ""
        dugmeMetni = Metin.al("hw_player_start")
        dugmeEtkin = true
    }

    /// Ana düğme: soru akmadıysa Başla, sonra Kontrol.
    func anaDugme() {
        if cevaplandi || bitti {
            return
        }
        if !akisBasladi && !oturum.isMultiplication(index: i32) {
            akisiBaslat()
        } else {
            kontrol()
        }
    }

    /// Android startFlash.
    private func akisiBaslat() {
        akiyor = true
        akisBasladi = true
        dugmeMetni = Metin.al("hw_player_check")
        dugmeEtkin = false
        let aralik: Int = Int(oturum.intervalMs(index: i32))
        let pay: Int = min(max(aralik * 15 / 100, 40), 250)
        bosluk = pay
        gosterimMs = max(aralik - pay, 40)
        kayitZamanlari = []
        kayitDegerleri = []
        gorunmeAnlari = []
        kayitAcik = abakuslu
        abakusKaydet(abakus.anlikDeger)
        etiketler = oturum.flashLabels(index: i32)
        etiketSirasi = 0
        sayiyiGoster()
    }

    /// Android showFlashNumber: son sayı ekranda kalır, Kontrol hemen açılır.
    private func sayiyiGoster() {
        guard etiketSirasi < etiketler.count else { return }
        gorunmeAnlari.append(SobiTestOynaticiModeli.simdiMs())
        tahta = etiketler[etiketSirasi]
        bosta = false
        if etiketSirasi == etiketler.count - 1 {
            akiyor = false
            akisBitti()
            return
        }
        planla(gosterimMs) { [weak self] in self?.sayiyiSondur() }
    }

    /// Android hideFlashNumber.
    private func sayiyiSondur() {
        tahta = ""
        bosta = true
        etiketSirasi += 1
        planla(bosluk) { [weak self] in self?.sayiyiGoster() }
    }

    /// Android onFlashFinished.
    private func akisBitti() {
        dugmeMetni = Metin.al("hw_player_check")
        dugmeEtkin = true
        if !abakuslu {
            cevapKutusu = true
        }
    }

    private func planla(_ ms: Int, _ isi: @escaping () -> Void) {
        zamanlayici?.cancel()
        let yeni = DispatchWorkItem(block: isi)
        zamanlayici = yeni
        DispatchQueue.main.asyncAfter(deadline: .now() + Double(ms) / 1000.0, execute: yeni)
    }

    private func akisiDurdur() {
        zamanlayici?.cancel()
        zamanlayici = nil
        akiyor = false
        bosta = false
    }

    // MARK: Abaküs kaydı

    /// Abaküsün her değişimi (sıfırlama animasyonunun ara değerleri hariç) soru akarken ve cevaplanana dek.
    func abakusDegisti(_ yeni: Int) {
        if abakus.sifirlaniyor {
            return
        }
        abakusKaydet(yeni)
    }

    private func abakusKaydet(_ deger: Int) {
        guard kayitAcik else { return }
        let d: Int64 = Int64(deger)
        if kayitDegerleri.last == d {
            return
        }
        kayitZamanlari.append(SobiTestOynaticiModeli.simdiMs())
        kayitDegerleri.append(d)
    }

    private static func simdiMs() -> Int64 {
        return Int64(ProcessInfo.processInfo.systemUptime * 1000.0)
    }

    private static func kutula(_ dizi: [Int64]) -> [KotlinLong] {
        return dizi.map { KotlinLong(longLong: $0) }
    }

    // MARK: Cevap

    /// Android onKontrol.
    private func kontrol() {
        if akiyor || cevaplandi {
            return
        }
        var verilen: Int64? = nil
        if abakuslu {
            verilen = Int64(abakus.anlikDeger)
        } else {
            let yazi: String = cevapMetni.trimmingCharacters(in: .whitespacesAndNewlines)
            verilen = Int64(yazi)
        }
        guard let cevap = verilen else { return }
        cevaplandi = true
        kayitAcik = false
        cevapKutusu = false
        sonVerilen = cevap
        let dogru: Bool = oturum.submit(
            index: i32,
            given: cevap,
            logTimesMs: SobiTestOynaticiModeli.kutula(kayitZamanlari),
            logValues: SobiTestOynaticiModeli.kutula(kayitDegerleri),
            shownAtMs: SobiTestOynaticiModeli.kutula(gorunmeAnlari)
        )
        tahta = oturum.revealText(index: i32)
        dugmeEtkin = false
        let dogruCevap: String = String(oturum.answer(index: i32))
        let baslik: String = dogru ? Metin.al("hw_player_correct") : Metin.al("hw_player_wrong", dogruCevap)
        let mesaj: String? = dogru ? nil : oturum.fullLine(index: i32)
        let analiz: Bool = oturum.canReview(index: i32, correct: dogru)
        panel = SobiTestCevapPaneli(dogru: dogru, baslik: baslik, mesaj: mesaj, analiz: analiz)
        // Android HomeworkPlayerFragment.kt:599 ile aynı ses ve düzey (doğru 0.3, yanlış 1.0). Dosyalar
        // Sesler/Ogretici/ (correct_answer_sound.wav, incorrect_answer_sound.mp3); ortak çalıcı
        // "sound_enabled" kapalıysa ve galeri kipinde çalmaz.
        if dogru {
            SesCalar.ortak.cal(ad: "correct_answer_sound", sesDuzeyi: 0.3)
        } else {
            SesCalar.ortak.cal(ad: "incorrect_answer_sound", sesDuzeyi: 1.0)
        }
    }

    /// "Sobi ile analiz et" penceresinin isteği (Android bindSobiReview → MistakeReviewDialog.newInstance).
    func analizIstegi() -> AbakusSobiAnalizIstegi {
        let sayilar: [Int] = oturum.reviewNumbers(index: i32).map { Int($0.intValue) }
        let inceleme: SobiTestInceleme = oturum.review(
            index: i32,
            logTimesMs: SobiTestOynaticiModeli.kutula(kayitZamanlari),
            logValues: SobiTestOynaticiModeli.kutula(kayitDegerleri),
            shownAtMs: SobiTestOynaticiModeli.kutula(gorunmeAnlari)
        )
        let kayit: [Int] = inceleme.values.map { Int($0.intValue) }
        let isaretler: [Int] = inceleme.marks.map { Int($0.intValue) }
        return AbakusSobiAnalizIstegi(
            sayilar: sayilar,
            cevap: Int(clamping: sonVerilen),
            kayit: kayit,
            isaretler: isaretler,
            dersBasligi: Metin.al("sobi_test_title"),
            kapandi: {}
        )
    }

    /// "Sobi ile analiz et" düğmesi gösterilebilir mi (yönetici kararı: SobiAnalizModeli.analizYapilabilir).
    func analizSayilari() -> [Int] {
        return oturum.reviewNumbers(index: i32).map { Int($0.intValue) }
    }

    /// Paneldeki Devam (Android: hideResultPanel → nextQuestion).
    func devam() {
        guard panel != nil else { return }
        panel = nil
        if soruSirasi + 1 < soruSayisi {
            soruSirasi += 1
            soruyuGoster()
        } else {
            testiBitir()
        }
    }

    // MARK: Yan düğmeler

    /// Android onReset: abaküs (abaküssüzde cevap kutusu) sıfırlanır; Başla'ya basıldıysa sayılar baştan akar.
    func sifirla() {
        if cevaplandi || !soruVar || ozet != nil {
            return
        }
        abakus.sifirla(animasyonlu: true)
        cevapMetni = ""
        if akisBasladi && !oturum.isMultiplication(index: i32) {
            akisiDurdur()
            tahta = ""
            dugmeEtkin = false
            cevapKutusu = false
            // Abaküsün sıfırlanma animasyonu (300 ms) bitsin, kayda girmesin.
            planla(350) { [weak self] in
                guard let self = self, !self.cevaplandi else { return }
                self.akisiBaslat()
            }
        }
    }

    /// Android onSkip: gösterilen sayı hemen söner, sıradaki gelir (puana etkisi yok).
    func atla() {
        if !akiyor || bosta {
            return
        }
        zamanlayici?.cancel()
        sayiyiSondur()
    }

    // MARK: Son

    /// Android finishSobiTest: test bugün yapılmış sayılır, seviyeler yazılır, Sobi kural kural özetler.
    private func testiBitir() {
        bitti = true
        akisiDurdur()
        let sonuc: SobiTestSonucu = oturum.finish(metin: SobiTestOrtak.metin)
        if kalici {
            SobiTestOrtak.depo.markDone(today: SobiTestOrtak.bugun())
            let seviyeler: [String: KotlinInt] = sonuc.newLevels
            if !seviyeler.isEmpty {
                SobiTestOrtak.depo.saveLevels(levels: seviyeler)
                SobiTestOrtak.servis.seviyeleriGonder(SobiTestOrtak.sozluk(seviyeler))
            }
            SobiTestOrtak.dugme.yenile(balonla: false)
        }
        ozet = SobiTestOzet(
            skor: Metin.al("hw_player_block_score", Int(sonuc.correct), Int(sonuc.total)),
            mesaj: sonuc.message
        )
    }

    func kapanis() {
        akisiDurdur()
    }
}
