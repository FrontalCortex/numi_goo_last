import SwiftUI

// Maskotun konuşma balonu, balondaki daktilo yazısı ve tekrarlanan maskot hâli.
// Android: SpeechBubbleDrawable, UserInfoFragment.typeText / introSay,
// BunnyMascotView.repeatWithPause.

/// Balonun kuyruğu hangi kenarda (SpeechBubbleDrawable.TailSide).
enum GirisBalonKuyrugu {
    /// Sol kenarın ortasında, sola bakan (soru başlığında maskotun sağındaki balon).
    case sol
    /// Alt kenarın ortasında, aşağı bakan (tanışmada tavşanın üstündeki balon).
    case alt
}

/// Yuvarlak köşeli kutu + konuşana bakan kuyruk, TEK yol olarak (Android Path.op(UNION)): ayrı
/// çizilseler kenar çizgisi kuyruğun dibinden geçerdi. Kuyruk o kenarda `kuyrukBoyu` kadar yer
/// kaplar; içeriğin o kenardaki dolgusu bunu hesaba katmalı.
struct GirisBalonSekli: Shape {
    let kuyruk: GirisBalonKuyrugu
    var kose: CGFloat = 16
    var kuyrukBoyu: CGFloat
    var kuyrukTabani: CGFloat = 16
    /// Çizgi kalınlığının yarısı: kenar çizgisi kutunun içine sığsın.
    var yarimCizgi: CGFloat = 1

    func path(in rect: CGRect) -> Path {
        switch kuyruk {
        case .sol:
            return solKuyruklu(rect)
        case .alt:
            return altKuyruklu(rect)
        }
    }

    private func solKuyruklu(_ rect: CGRect) -> Path {
        let h: CGFloat = yarimCizgi
        let kutu = CGRect(
            x: rect.minX + kuyrukBoyu + h,
            y: rect.minY + h,
            width: max(0, rect.width - kuyrukBoyu - 2 * h),
            height: max(0, rect.height - 2 * h)
        )
        let r: CGFloat = min(kose, kutu.height / 2, kutu.width / 2)
        let orta: CGFloat = kutu.midY
        let yari: CGFloat = kuyrukTabani / 2
        var yol = Path()
        yol.move(to: CGPoint(x: kutu.minX + r, y: kutu.minY))
        yol.addLine(to: CGPoint(x: kutu.maxX - r, y: kutu.minY))
        yol.addArc(tangent1End: CGPoint(x: kutu.maxX, y: kutu.minY), tangent2End: CGPoint(x: kutu.maxX, y: kutu.minY + r), radius: r)
        yol.addLine(to: CGPoint(x: kutu.maxX, y: kutu.maxY - r))
        yol.addArc(tangent1End: CGPoint(x: kutu.maxX, y: kutu.maxY), tangent2End: CGPoint(x: kutu.maxX - r, y: kutu.maxY), radius: r)
        yol.addLine(to: CGPoint(x: kutu.minX + r, y: kutu.maxY))
        yol.addArc(tangent1End: CGPoint(x: kutu.minX, y: kutu.maxY), tangent2End: CGPoint(x: kutu.minX, y: kutu.maxY - r), radius: r)
        yol.addLine(to: CGPoint(x: kutu.minX, y: orta + yari))
        yol.addLine(to: CGPoint(x: rect.minX + h, y: orta))
        yol.addLine(to: CGPoint(x: kutu.minX, y: orta - yari))
        yol.addLine(to: CGPoint(x: kutu.minX, y: kutu.minY + r))
        yol.addArc(tangent1End: CGPoint(x: kutu.minX, y: kutu.minY), tangent2End: CGPoint(x: kutu.minX + r, y: kutu.minY), radius: r)
        yol.closeSubpath()
        return yol
    }

    private func altKuyruklu(_ rect: CGRect) -> Path {
        let h: CGFloat = yarimCizgi
        let kutu = CGRect(
            x: rect.minX + h,
            y: rect.minY + h,
            width: max(0, rect.width - 2 * h),
            height: max(0, rect.height - kuyrukBoyu - 2 * h)
        )
        let r: CGFloat = min(kose, kutu.height / 2, kutu.width / 2)
        let orta: CGFloat = kutu.midX
        let yari: CGFloat = kuyrukTabani / 2
        var yol = Path()
        yol.move(to: CGPoint(x: kutu.minX + r, y: kutu.minY))
        yol.addLine(to: CGPoint(x: kutu.maxX - r, y: kutu.minY))
        yol.addArc(tangent1End: CGPoint(x: kutu.maxX, y: kutu.minY), tangent2End: CGPoint(x: kutu.maxX, y: kutu.minY + r), radius: r)
        yol.addLine(to: CGPoint(x: kutu.maxX, y: kutu.maxY - r))
        yol.addArc(tangent1End: CGPoint(x: kutu.maxX, y: kutu.maxY), tangent2End: CGPoint(x: kutu.maxX - r, y: kutu.maxY), radius: r)
        yol.addLine(to: CGPoint(x: orta + yari, y: kutu.maxY))
        yol.addLine(to: CGPoint(x: orta, y: rect.maxY - h))
        yol.addLine(to: CGPoint(x: orta - yari, y: kutu.maxY))
        yol.addLine(to: CGPoint(x: kutu.minX + r, y: kutu.maxY))
        yol.addArc(tangent1End: CGPoint(x: kutu.minX, y: kutu.maxY), tangent2End: CGPoint(x: kutu.minX, y: kutu.maxY - r), radius: r)
        yol.addLine(to: CGPoint(x: kutu.minX, y: kutu.minY + r))
        yol.addArc(tangent1End: CGPoint(x: kutu.minX, y: kutu.minY), tangent2End: CGPoint(x: kutu.minX + r, y: kutu.minY), radius: r)
        yol.closeSubpath()
        return yol
    }
}

/// Balonun zemini: background_color dolgu, 2dp missions_track kenar, köşe 16dp
/// (UserInfoFragment.bubble; kuyruk solda 12dp, altta 16dp, taban 16dp).
struct GirisBalonZemini: View {
    let kuyruk: GirisBalonKuyrugu

    var body: some View {
        let boy: CGFloat = kuyruk == .sol ? 12 : 16
        let sekil = GirisBalonSekli(kuyruk: kuyruk, kuyrukBoyu: boy)
        ZStack {
            sekil.fill(Color("background_color"))
            sekil.stroke(Color("missions_track"), style: StrokeStyle(lineWidth: 2, lineJoin: .round))
        }
    }
}

/// Balon metninin bir parçası; `renk` doluysa vurgulu (ör. ilerleme yolu başlığındaki altın
/// "N dakikayla").
struct GirisMetinParcasi: Equatable {
    let metin: String
    var renk: Color? = nil
}

/// Daktilo gibi harf harf yazılan metin (UserInfoFragment.typeText): metin baştan TAMAMI konur,
/// henüz yazılmamış kısmı saydam boyanır; böylece balon içeriğe göre boyutlanırken zıplamaz.
/// Süre: harf başına 22 ms, en az 150 ms. Metin değişince baştan yazılır; aynı metin yeniden
/// verilirse yeniden yazılmaz.
struct GirisDaktiloMetni: View {
    let parcalar: [GirisMetinParcasi]
    let boyut: CGFloat
    var satirArasi: CGFloat = 0

    @State private var gorunen: Int = 0

    private var tumMetin: String {
        return parcalar.map { $0.metin }.joined()
    }

    var body: some View {
        Text(GirisDaktiloMetni.bicimle(parcalar, gorunen: gorunen, boyut: boyut))
            .lineSpacing(satirArasi)
            .fixedSize(horizontal: false, vertical: true)
            .task(id: tumMetin) {
                await yaz()
            }
    }

    private func yaz() async {
        let toplam: Int = tumMetin.count
        gorunen = 0
        if toplam == 0 { return }
        let sureMs: Double = max(Double(toplam) * GirisSoruModeli.harfMs, 150)
        let adimNs: UInt64 = UInt64(sureMs / Double(toplam) * 1_000_000)
        var sayi: Int = 0
        while sayi < toplam {
            try? await Task.sleep(nanoseconds: adimNs)
            // Kesildiyse (metin değişti) yeni yazma baştan başlıyor; eskisi dokunmaz.
            if Task.isCancelled { return }
            sayi += 1
            gorunen = sayi
        }
    }

    static func bicimle(_ parcalar: [GirisMetinParcasi], gorunen: Int, boyut: CGFloat) -> AttributedString {
        var sonuc = AttributedString()
        var kalan: Int = gorunen
        for parca in parcalar {
            let harfler: [Character] = Array(parca.metin)
            let yazilan: Int = max(0, min(kalan, harfler.count))
            kalan -= harfler.count
            var acik = AttributedString(String(harfler[0..<yazilan]))
            acik.font = Tema.yazi(boyut)
            acik.foregroundColor = parca.renk ?? Color("dark_text")
            var gizli = AttributedString(String(harfler[yazilan..<harfler.count]))
            gizli.font = Tema.yazi(boyut)
            gizli.foregroundColor = Color.clear
            sonuc.append(acik)
            sonuc.append(gizli)
        }
        return sonuc
    }
}

/// Bir hâli oynatıp her bitişinde `araMs` bekleyerek yeniden oynatan maskot
/// (BunnyMascotView.repeatWithPause; `araMs` 0 ise play(emote, times = MAX) gibi aralıksız).
/// Hâl değişince baştan başlar. Aynı hâli baştan oynatmak için görünüm kimliği değiştirilir.
struct GirisTekrarliMaskot: View {
    let emote: String
    let araMs: Double

    @State private var sayac: Int = 0

    var body: some View {
        ZStack {
            SobiMaskotGorunumu(emote: emote)
                .id(sayac)
        }
        .task(id: emote) {
            let sure: Double = SobiMaskotHal(rawValue: emote)?.sureMs ?? 2000
            let bekleme: UInt64 = UInt64((sure + araMs) * 1_000_000)
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: bekleme)
                if Task.isCancelled { return }
                sayac += 1
            }
        }
    }
}
