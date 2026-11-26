import Shared
import SwiftUI

// Kupa geçmişi ekranı.
//
// Android karşılığı: CupHistoryFragment.kt + res/layout/fragment_cup_history.xml.
// Profildeki altı kupa kartından birine basınca açılır (ProfileFragment.openCupHistoryFragment):
// seçilen kupa türünün güncel skoru ve Haftalık / Aylık / Yıllık / Tüm Zamanlar sekmeleriyle
// basamaklı çizgi grafiği. Gruplama, etiketler ve eksen aralığı ortak koddadır (CupHistoryLogic).

/// Grafikte çizilecek nokta.
struct KupaGrafikNoktaDurumu: Equatable {
    let etiket: String
    let deger: Int
}

/// Bir sekmenin çizime hazır grafiği.
struct KupaGrafikDurumu: Equatable {
    let bos: Bool
    let tekNokta: Bool
    let noktalar: [KupaGrafikNoktaDurumu]
    let eksenAlt: Int
    let eksenUst: Int
    let xEtiketSayisi: Int
}

final class KupaGecmisModeli: ObservableObject {
    @Published private(set) var sekmeBasliklari: [String] = []
    @Published private(set) var seciliSekme: Int = 0
    @Published private(set) var skorMetni: String = ""
    @Published private(set) var yukleniyor: Bool = false
    @Published private(set) var grafik: KupaGrafikDurumu? = nil

    private let kullaniciKimligi: String
    private let alan: String
    private let depo: KupaGecmisDeposu
    private let kopru: KupaEkranKoprusu = KupaEkranKoprusu.shared
    /// Sekme başına önbellek: aynı sekmeye dönünce yeniden okunmaz (Android `cache`).
    private var onbellek: [Int: [KupaGecmisKaydi]] = [:]
    private var basladi: Bool = false

    init(kullaniciKimligi: String, alan: String, depo: KupaGecmisDeposu) {
        self.kullaniciKimligi = kullaniciKimligi
        self.alan = alan
        self.depo = depo
        self.sekmeBasliklari = kopru.gecmisSekmeBasliklari()
    }

    func basla() {
        if basladi { return }
        basladi = true
        let varsayilan: Int = Int(kopru.gecmisVarsayilanSkor())
        depo.guncelSkoruGetir(kullaniciKimligi: kullaniciKimligi, alan: alan) { [weak self] skor in
            self?.skorMetni = String(skor ?? varsayilan)
        }
        sekmeSec(0)
    }

    func sekmeSec(_ sekme: Int) {
        seciliSekme = sekme
        if let hazir = onbellek[sekme] {
            ciz(sekme: sekme, kayitlar: hazir)
            return
        }
        yukleniyor = true
        let baslangic: String = kopru.gecmisBaslangicKimligi(tabIndex: Int32(sekme))
        depo.gecmisiGetir(kullaniciKimligi: kullaniciKimligi, alan: alan, baslangicKimligi: baslangic) { [weak self] kayitlar in
            guard let self = self else { return }
            self.yukleniyor = false
            self.onbellek[sekme] = kayitlar
            // Yanıt gelene kadar başka sekmeye geçildiyse çizme.
            if self.seciliSekme == sekme {
                self.ciz(sekme: sekme, kayitlar: kayitlar)
            }
        }
    }

    private func ciz(sekme: Int, kayitlar: [KupaGecmisKaydi]) {
        let noktalar: [KupaGecmisNoktasi] = kayitlar.map {
            KupaGecmisNoktasi(dateId: $0.tarihKimligi, value: Int32($0.deger))
        }
        let v: KupaGrafikVerisi = kopru.gecmisGrafigi(tabIndex: Int32(sekme), noktalar: noktalar)
        grafik = KupaGrafikDurumu(
            bos: v.isEmpty,
            tekNokta: v.isSinglePoint,
            noktalar: v.points.map { KupaGrafikNoktaDurumu(etiket: $0.label, deger: Int($0.value)) },
            eksenAlt: Int(v.axisMin),
            eksenUst: Int(v.axisMax),
            xEtiketSayisi: Int(v.xAxisLabelCount)
        )
    }
}

struct KupaGecmisiEkrani: View {
    private let baslik: String
    private let animasyonDosyasi: String
    private let kapat: (() -> Void)?

    @StateObject private var model: KupaGecmisModeli

    /// - Parameters:
    ///   - kullaniciKimligi: geçmişi gösterilecek kullanıcı (kendi profili ya da başkasının).
    ///   - alan: kupa alanı (ör. "addition_abacus_cup"; ortak kod `CupPathCard.field`).
    ///   - baslik: üst çubuk başlığı (profil kartının başlığı: "Toplama", "Çıkarma", "Çarpma").
    ///   - animasyonDosyasi: skorun yanındaki hayvan animasyonu (ör. "dinosaur_anim.json").
    ///   - kapat: geri düğmesi.
    ///   - ortam: veri servisleri (varsayılan: örnek veri).
    init(
        kullaniciKimligi: String,
        alan: String,
        baslik: String,
        animasyonDosyasi: String,
        kapat: (() -> Void)?,
        ortam: KupaOrtami = KupaOrtami.sahte
    ) {
        self.baslik = baslik
        self.animasyonDosyasi = animasyonDosyasi
        self.kapat = kapat
        _model = StateObject(
            wrappedValue: KupaGecmisModeli(kullaniciKimligi: kullaniciKimligi, alan: alan, depo: ortam.gecmisDeposu)
        )
    }

    var body: some View {
        VStack(spacing: 0) {
            ustCubuk
            skorOzeti
            KupaGecmisSekmeleri(
                basliklar: model.sekmeBasliklari,
                secili: model.seciliSekme,
                secildi: { model.sekmeSec($0) }
            )
            Rectangle()
                .fill(KupaStil.izgaraCizgisi)
                .frame(height: 1)
            if let grafik = model.grafik, grafik.tekNokta, !grafik.bos {
                tekNoktaIpucu
            }
            grafikAlani
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color("background_color").ignoresSafeArea())
        .onAppear {
            model.basla()
        }
    }

    private var ustCubuk: some View {
        HStack(spacing: 0) {
            KupaGeriDugmesi(boyut: 48, icBosluk: 12, renk: .white, eylem: { kapat?() })
            Text(baslik)
                .font(Tema.yazi(18))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
            Color.clear.frame(width: 48, height: 48)
        }
        .padding(.leading, 4)
        .padding(.trailing, 16)
        .frame(height: 56)
    }

    private var skorOzeti: some View {
        HStack(spacing: 12) {
            KupaAnimasyonGorunumu(dosya: animasyonDosyasi, oynuyor: true)
                .frame(width: 56, height: 56)
            VStack(alignment: .leading, spacing: 0) {
                Text(model.skorMetni.isEmpty ? " " : model.skorMetni)
                    .font(Tema.yazi(32))
                    .foregroundColor(.white)
                Text(KupaMetin.guncelKupa)
                    .font(Tema.yazi(13))
                    .foregroundColor(KupaStil.solukYazi)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 20)
        .padding(.top, 8)
        .padding(.bottom, 16)
    }

    private var tekNoktaIpucu: some View {
        Text(KupaMetin.tekNoktaIpucu)
            .font(Tema.yazi(12))
            .foregroundColor(KupaStil.solukYazi)
            .multilineTextAlignment(.center)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity)
            .background(KupaStil.renk(0x0D1C24))
    }

    private var grafikAlani: some View {
        ZStack {
            if let grafik = model.grafik, !grafik.bos {
                KupaGecmisGrafigi(grafik: grafik)
                    .padding(16)
            }
            if model.yukleniyor {
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(.white)
            } else if let grafik = model.grafik, grafik.bos {
                Text(KupaMetin.gecmisYok)
                    .font(Tema.yazi(14))
                    .foregroundColor(KupaStil.solukYazi)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Sekmeler

/// Kaydırılabilir sekme şeridi (TabLayout tabMode=scrollable; gösterge yazı genişliğinde).
private struct KupaGecmisSekmeleri: View {
    let basliklar: [String]
    let secili: Int
    let secildi: (Int) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 0) {
                ForEach(Array(basliklar.enumerated()), id: \.offset) { sekme in
                    sekmeDugmesi(sira: sekme.offset, baslik: sekme.element)
                }
            }
        }
        .frame(height: 48)
    }

    private func sekmeDugmesi(sira: Int, baslik: String) -> some View {
        let seciliMi: Bool = sira == secili
        let renk: Color = seciliMi ? KupaStil.sekmeRengi : KupaStil.solukYazi
        return Button(action: { secildi(sira) }) {
            VStack(spacing: 0) {
                Spacer(minLength: 0)
                Text(baslik)
                    .font(Tema.yazi(14))
                    .foregroundColor(renk)
                Spacer(minLength: 0)
                Rectangle()
                    .fill(seciliMi ? KupaStil.sekmeRengi : Color.clear)
                    .frame(height: 2)
            }
            .fixedSize(horizontal: true, vertical: false)
            .padding(.horizontal, 16)
            .frame(height: 48)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Grafik

/// Basamaklı (STEPPED) çizgi grafiği: Android'de MPAndroidChart LineChart.
/// Sol eksende tam sayı etiketleri ve yatay ızgara, altta tarih etiketleri, çizginin altı dolgulu.
private struct KupaGecmisGrafigi: View {
    let grafik: KupaGrafikDurumu

    private let solEksenGenisligi: CGFloat = 36
    private let altEksenYuksekligi: CGFloat = 20
    private let yEtiketSayisi: Int = 6

    private var yDegerleri: [Int] {
        let alt: Int = grafik.eksenAlt
        let ust: Int = grafik.eksenUst
        if ust <= alt { return [alt] }
        let aralik: Int = ust - alt
        var sonuc: [Int] = []
        for i in 0..<yEtiketSayisi {
            let deger: Int = alt + (aralik * i) / (yEtiketSayisi - 1)
            if sonuc.last != deger {
                sonuc.append(deger)
            }
        }
        return sonuc
    }

    /// x ekseninde etiketi yazılacak nokta indeksleri (en çok `xEtiketSayisi` tane, eşit aralıklı).
    private var xEtiketIndeksleri: [Int] {
        let adet: Int = grafik.noktalar.count
        let istenen: Int = max(1, min(grafik.xEtiketSayisi, adet))
        if adet <= 1 || istenen <= 1 { return adet > 0 ? [0] : [] }
        var sonuc: [Int] = []
        for i in 0..<istenen {
            let indeks: Int = (i * (adet - 1)) / (istenen - 1)
            if sonuc.last != indeks {
                sonuc.append(indeks)
            }
        }
        return sonuc
    }

    var body: some View {
        GeometryReader { geo in
            let cizim = CGRect(
                x: solEksenGenisligi,
                y: 8,
                width: max(1, geo.size.width - solEksenGenisligi - 12),
                height: max(1, geo.size.height - altEksenYuksekligi - 8)
            )
            ZStack(alignment: .topLeading) {
                izgara(cizim)
                dolgu(cizim)
                cizgi(cizim)
                if grafik.tekNokta {
                    tekDaire(cizim)
                }
                yEtiketleri(cizim)
                xEtiketleri(cizim)
            }
        }
    }

    private func xKonumu(_ indeks: Int, _ cizim: CGRect) -> CGFloat {
        let adet: Int = grafik.noktalar.count
        if adet <= 1 { return cizim.midX }
        let oran: CGFloat = CGFloat(indeks) / CGFloat(adet - 1)
        return cizim.minX + cizim.width * oran
    }

    private func yKonumu(_ deger: Int, _ cizim: CGRect) -> CGFloat {
        let aralik: Int = grafik.eksenUst - grafik.eksenAlt
        if aralik <= 0 { return cizim.midY }
        let oran: CGFloat = CGFloat(deger - grafik.eksenAlt) / CGFloat(aralik)
        return cizim.maxY - cizim.height * oran
    }

    /// Basamaklı yol: önce yatay ilerler, sonra bir sonraki noktanın değerine dikey çıkar/iner.
    private func basamakliYol(_ cizim: CGRect) -> Path {
        var yol = Path()
        let noktalar = grafik.noktalar
        guard let ilk = noktalar.first else { return yol }
        yol.move(to: CGPoint(x: xKonumu(0, cizim), y: yKonumu(ilk.deger, cizim)))
        if noktalar.count > 1 {
            for i in 1..<noktalar.count {
                let x: CGFloat = xKonumu(i, cizim)
                let oncekiY: CGFloat = yKonumu(noktalar[i - 1].deger, cizim)
                let y: CGFloat = yKonumu(noktalar[i].deger, cizim)
                yol.addLine(to: CGPoint(x: x, y: oncekiY))
                yol.addLine(to: CGPoint(x: x, y: y))
            }
        }
        return yol
    }

    private func izgara(_ cizim: CGRect) -> some View {
        Path { yol in
            for deger in yDegerleri {
                let y: CGFloat = yKonumu(deger, cizim)
                yol.move(to: CGPoint(x: cizim.minX, y: y))
                yol.addLine(to: CGPoint(x: cizim.maxX, y: y))
            }
        }
        .stroke(KupaStil.izgaraCizgisi, lineWidth: 1)
    }

    private func dolgu(_ cizim: CGRect) -> some View {
        var yol: Path = basamakliYol(cizim)
        if grafik.noktalar.count > 1 {
            yol.addLine(to: CGPoint(x: cizim.maxX, y: cizim.maxY))
            yol.addLine(to: CGPoint(x: cizim.minX, y: cizim.maxY))
            yol.closeSubpath()
        }
        // Android: fillAlpha = 90 (0..255).
        return yol.fill(KupaStil.grafikRengi.opacity(90.0 / 255.0))
    }

    private func cizgi(_ cizim: CGRect) -> some View {
        basamakliYol(cizim)
            .stroke(KupaStil.grafikRengi, style: StrokeStyle(lineWidth: 2, lineJoin: .round))
    }

    private func tekDaire(_ cizim: CGRect) -> some View {
        let deger: Int = grafik.noktalar.first?.deger ?? grafik.eksenAlt
        return Circle()
            .fill(KupaStil.grafikRengi)
            .frame(width: 10, height: 10)
            .position(x: xKonumu(0, cizim), y: yKonumu(deger, cizim))
    }

    private func yEtiketleri(_ cizim: CGRect) -> some View {
        ForEach(yDegerleri, id: \.self) { deger in
            Text(String(deger))
                .font(Tema.yazi(10, kalin: false))
                .foregroundColor(KupaStil.solukYazi)
                .frame(width: solEksenGenisligi - 6, alignment: .trailing)
                .position(x: (solEksenGenisligi - 6) / 2, y: yKonumu(deger, cizim))
        }
    }

    private func xEtiketleri(_ cizim: CGRect) -> some View {
        ForEach(xEtiketIndeksleri, id: \.self) { indeks in
            Text(grafik.noktalar[indeks].etiket)
                .font(Tema.yazi(10, kalin: false))
                .foregroundColor(KupaStil.solukYazi)
                .fixedSize()
                .position(x: xKonumu(indeks, cizim), y: cizim.maxY + altEksenYuksekligi / 2 + 2)
        }
    }
}
