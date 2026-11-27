import Foundation
import Lottie
import SwiftUI
import UIKit

// Android `LottieAnimationView` karşılığı: bütün ekranların kullandığı ortak sarmalayıcı.
// Sözleşme `ios/iosApp/GELISTIRME.md` → "Lottie animasyonları" bölümündedir; imza değiştirilmez.
//
//   LottieGorunumu("confetti_anim")                 // bir kez oynar
//   LottieGorunumu("fire_anim", dongu: true)        // sürekli döner
//   LottieGorunumu("gold_medal_anim", dongu: false, hiz: 1.0, oynat: true, bitti: { ... })
//
// Davranış:
// - `ad`: `iosApp/Lottie/` altındaki dosyanın uzantısız adı. Sondaki ".json" ekleri (Android'deki
//   "bronze_medal_anim.json.json" gibi çift uzantı dahil) hoş görülür ve atılır.
// - Dosya bulunamazsa ya da çözülemezse çökmez, boş çizilir.
// - Boyutu dışarıdan `.frame(...)` belirler; içerik oranı korunarak sığdırılır (Android `fitCenter`).
// - `oynat: false` → ilk karede bekler; oynarken false olursa o karede duraklar, yeniden true olunca
//   kaldığı yerden sürer. Döngüsüz oynatma bittikten sonra false → true yapılırsa baştan oynar.
// - `bitti`: yalnızca döngüsüz oynatma sonuna kadar oynayınca, her oynatma için bir kez çağrılır
//   (duraklatma, ekrandan kalkma ya da döngülü oynatma çağırmaz). Ana iş parçacığında, bir sonraki
//   çalışma döngüsü turunda gelir.
// - Kare aralığı (isteğe bağlı, Android `setMinAndMaxFrame` / `setFrame` karşılığı):
//   `bitisKare` verilirse `baslangicKare ?? ilk kare`'den o kareye kadar oynar ve o karede kalır
//   (`dongu` true ise bu aralıkta döner); yalnızca `baslangicKare` verilirse oradan sona oynar.
//   `oynat: false` iken `baslangicKare` verilmişse o karede sabit durur
//   (`LottieGorunumu("x", oynat: false, baslangicKare: 116)`). Aralık değişince yeni aralık baştan
//   oynar; `bitti` aralığın sonuna gelince çağrılır. Kareler dosyanın kendi kare numaralarıdır.
// - Dokunmaları almaz (Android'deki tıklanamaz görünüm gibi alttakine geçirir). Dokunma gerekiyorsa
//   kapsayan görünüme `.contentShape(Rectangle())` ile birlikte verin.
//
// lottie-ios 4.6.1 kaynağından doğrulanan API: `LottieAnimationView(frame:)`, `animation`, `loopMode`,
// `animationSpeed`, `backgroundBehavior`, `play(completion:)`, `play(fromFrame:toFrame:loopMode:completion:)`,
// `currentFrame`, `pause()`, `stop()`, `LottieAnimation.filepath(_:)`, `LottieAnimation.startFrame` /
// `endFrame`.
struct LottieGorunumu: View {
    private let ad: String
    private let dongu: Bool
    private let hiz: Double
    private let oynat: Bool
    private let bitti: (() -> Void)?
    private let baslangicKare: Double?
    private let bitisKare: Double?

    init(
        _ ad: String,
        dongu: Bool = false,
        hiz: Double = 1.0,
        oynat: Bool = true,
        bitti: (() -> Void)? = nil,
        baslangicKare: Double? = nil,
        bitisKare: Double? = nil
    ) {
        self.ad = ad
        self.dongu = dongu
        self.hiz = hiz
        self.oynat = oynat
        self.bitti = bitti
        self.baslangicKare = baslangicKare
        self.bitisKare = bitisKare
    }

    var body: some View {
        LottieGorunumuTemsilcisi(
            ad: ad,
            dongu: dongu,
            hiz: hiz,
            oynat: oynat,
            bitti: bitti,
            baslangicKare: baslangicKare,
            bitisKare: bitisKare
        )
        .allowsHitTesting(false)
    }
}

// MARK: - Dosya yükleme

/// Animasyon dosyasını uygulama paketinden (Bundle.main) ada göre bulur.
enum LottieGorunumuYukleyici {
    /// Adın sonundaki ".json" eklerini (birden fazlaysa hepsini) atar.
    static func tabanAd(_ ad: String) -> String {
        let uzanti: String = ".json"
        var taban: String = ad.trimmingCharacters(in: .whitespacesAndNewlines)
        while taban.lowercased().hasSuffix(uzanti) {
            taban = String(taban.dropLast(uzanti.count))
        }
        return taban
    }

    /// Dosya pakette yoksa Lottie'ye hiç gidilmez; nil döner (görünüm boş çizilir).
    static func yukle(_ taban: String) -> LottieAnimation? {
        if taban.isEmpty {
            return nil
        }
        guard let yol: String = Bundle.main.path(forResource: taban, ofType: "json") else {
            return nil
        }
        // Çözülemeyen dosyada nil döner; çözülen animasyon Lottie'nin ortak önbelleğinde tutulur.
        return LottieAnimation.filepath(yol)
    }
}

// MARK: - UIKit köprüsü

/// SwiftUI ↔ `LottieAnimationView` köprüsü. Ekran kodu bunu değil `LottieGorunumu`'nu kullanır.
struct LottieGorunumuTemsilcisi: UIViewRepresentable {
    let ad: String
    let dongu: Bool
    let hiz: Double
    let oynat: Bool
    let bitti: (() -> Void)?
    let baslangicKare: Double?
    let bitisKare: Double?

    func makeCoordinator() -> LottieGorunumuKoordinatoru {
        return LottieGorunumuKoordinatoru()
    }

    func makeUIView(context: Context) -> LottieGorunumuKapsayici {
        return LottieGorunumuKapsayici(frame: .zero)
    }

    func updateUIView(_ uiView: LottieGorunumuKapsayici, context: Context) {
        context.coordinator.guncelle(
            gorunum: uiView.animasyon,
            ad: ad,
            dongu: dongu,
            hiz: hiz,
            oynat: oynat,
            bitti: bitti,
            baslangicKare: baslangicKare,
            bitisKare: bitisKare
        )
    }

    static func dismantleUIView(_ uiView: LottieGorunumuKapsayici, coordinator: LottieGorunumuKoordinatoru) {
        coordinator.birak(gorunum: uiView.animasyon)
    }
}

/// Animasyon görünümünü saran boş kapsayıcı. `LottieAnimationView` içsel boyut olarak animasyonun
/// kendi ölçüsünü (ör. 1024x1024) bildirir; kapsayıcı içsel boyut bildirmediği için SwiftUI
/// `.frame(...)` ile verilen alanı aynen kullanır, animasyon da o alana sığdırılır.
final class LottieGorunumuKapsayici: UIView {
    let animasyon: LottieAnimationView

    override init(frame: CGRect) {
        animasyon = LottieAnimationView(frame: .zero)
        super.init(frame: frame)
        backgroundColor = UIColor.clear
        isUserInteractionEnabled = false
        animasyon.backgroundColor = UIColor.clear
        animasyon.contentMode = .scaleAspectFit
        // Uygulama arka plana gidip dönünce (ve görünüm pencereden çıkıp geri girince)
        // animasyon kaldığı yerden sürer.
        animasyon.backgroundBehavior = .pauseAndRestore
        animasyon.frame = bounds
        addSubview(animasyon)
    }

    required init?(coder: NSCoder) {
        return nil
    }

    override var intrinsicContentSize: CGSize {
        return CGSize(width: UIView.noIntrinsicMetric, height: UIView.noIntrinsicMetric)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        animasyon.frame = bounds
    }
}

/// Oynatma durumunu tutar: SwiftUI `updateUIView`'ı her yeniden çizimde çağırır; animasyon yalnızca
/// girdiler gerçekten değişince yeniden başlatılır ya da duraklatılır.
@MainActor
final class LottieGorunumuKoordinatoru {
    /// Yüklü animasyonun (uzantısız) adı; nil ise henüz yüklenmedi.
    private var yuklenenAd: String? = nil
    private var dongu: Bool = false
    /// En güncel bitiş kapanışı (SwiftUI her çizimde yenisini verebilir).
    private var bitti: (() -> Void)? = nil
    /// `play` çağrıldı; henüz bitmedi ve duraklatılmadı.
    private var oynuyor: Bool = false
    /// Döngüsüz oynatma sonuna kadar oynadı (yeniden başlatılmaz).
    private var tamamlandi: Bool = false
    /// Her başlatma/duraklatma/ad değişiminde artar; eski oynatmaların geri çağrıları yok sayılır.
    private var nesil: Int = 0
    /// Geçerli kare aralığı (nil: verilmedi).
    private var baslangicKare: Double? = nil
    private var bitisKare: Double? = nil
    /// Ad ya da aralık değişti: sıradaki oynatma aralığın başından başlar.
    private var bastanBasla: Bool = false

    func guncelle(
        gorunum: LottieAnimationView,
        ad: String,
        dongu: Bool,
        hiz: Double,
        oynat: Bool,
        bitti: (() -> Void)?,
        baslangicKare: Double?,
        bitisKare: Double?
    ) {
        self.bitti = bitti
        self.dongu = dongu

        // Ad değiştiyse animasyonu yükle (bulunamazsa nil: boş çizilir).
        let taban: String = LottieGorunumuYukleyici.tabanAd(ad)
        if yuklenenAd != taban {
            yuklenenAd = taban
            nesil += 1
            oynuyor = false
            tamamlandi = false
            bastanBasla = true
            gorunum.animation = LottieGorunumuYukleyici.yukle(taban)
        }

        // Kare aralığı değiştiyse süren oynatma geçersizdir; yeni aralık baştan oynar.
        if self.baslangicKare != baslangicKare || self.bitisKare != bitisKare {
            self.baslangicKare = baslangicKare
            self.bitisKare = bitisKare
            nesil += 1
            if oynuyor {
                // Eski aralığın oynatması bulunduğu karede kesilir.
                gorunum.pause()
            }
            oynuyor = false
            tamamlandi = false
            bastanBasla = true
        }

        // Döngü ve hız: değer değişince atanır (atama süren animasyonu yeniden kurar).
        let hedefMod: LottieLoopMode = dongu ? LottieLoopMode.loop : LottieLoopMode.playOnce
        if gorunum.loopMode != hedefMod {
            gorunum.loopMode = hedefMod
        }
        let hedefHiz: CGFloat = CGFloat(hiz)
        if gorunum.animationSpeed != hedefHiz {
            gorunum.animationSpeed = hedefHiz
        }

        guard let animasyon: LottieAnimation = gorunum.animation else {
            return
        }

        if oynat {
            // Bitmiş bir animasyon sonradan döngüye alınırsa yeniden oynar.
            if dongu && tamamlandi {
                tamamlandi = false
            }
            if !oynuyor && !tamamlandi {
                baslat(gorunum, animasyon: animasyon)
            }
        } else {
            if oynuyor {
                // Duraklat: o karede kalır. Lottie geri çağrıyı `false` ile verir; nesil de eskir.
                nesil += 1
                oynuyor = false
                gorunum.pause()
            }
            // `oynat` yeniden true olunca bitmiş animasyon baştan oynasın.
            tamamlandi = false
            // Sabit kare: `baslangicKare` verilmişse o karede durur (Android `setFrame`).
            if let kare: Double = baslangicKare {
                let hedefKare: CGFloat = kareyiSinirla(kare, animasyon: animasyon)
                if gorunum.currentFrame != hedefKare {
                    gorunum.currentFrame = hedefKare
                }
            }
        }
    }

    /// Kareyi animasyonun kendi kare aralığına sıkıştırır.
    private func kareyiSinirla(_ kare: Double, animasyon: LottieAnimation) -> CGFloat {
        let alt: CGFloat = min(animasyon.startFrame, animasyon.endFrame)
        let ust: CGFloat = max(animasyon.startFrame, animasyon.endFrame)
        return min(max(CGFloat(kare), alt), ust)
    }

    /// Görünüm ağaçtan kalkarken: animasyonu durdur, bekleyen geri çağrıları geçersiz kıl.
    func birak(gorunum: LottieAnimationView) {
        nesil += 1
        oynuyor = false
        bitti = nil
        gorunum.stop()
    }

    private func baslat(_ gorunum: LottieAnimationView, animasyon: LottieAnimation) {
        nesil += 1
        let buNesil: Int = nesil
        oynuyor = true
        let geriCagri: LottieCompletionBlock = { [weak self] (tamam: Bool) in
            self?.oynatmaBitti(nesil: buNesil, tamam: tamam)
        }

        if baslangicKare == nil && bitisKare == nil {
            // Aralık yok: bulunduğu kareden sona oynar; sondaysa baştan başlar.
            bastanBasla = false
            gorunum.play(completion: geriCagri)
            return
        }

        // Aralık var: `baslangicKare ?? ilk kare` → `bitisKare ?? son kare`; bitince son karede kalır.
        let ilkKare: CGFloat = kareyiSinirla(baslangicKare ?? Double(animasyon.startFrame), animasyon: animasyon)
        let sonKare: CGFloat = kareyiSinirla(bitisKare ?? Double(animasyon.endFrame), animasyon: animasyon)
        if bastanBasla {
            // Lottie, bulunduğu kare aralığın içindeyse oradan sürdürür; yeni aralık baştan oynasın.
            bastanBasla = false
            if gorunum.currentFrame != ilkKare {
                gorunum.currentFrame = ilkKare
            }
        }
        // `loopMode: nil` → görünümün kendi döngü ayarı kullanılır.
        gorunum.play(fromFrame: ilkKare, toFrame: sonKare, loopMode: nil, completion: geriCagri)
    }

    /// Lottie'nin bitiş geri çağrısı: `tamam` yalnızca animasyon sonuna kadar oynadıysa true'dur.
    private func oynatmaBitti(nesil buNesil: Int, tamam: Bool) {
        if buNesil != nesil || !tamam || dongu {
            return
        }
        oynuyor = false
        tamamlandi = true
        // Lottie bazı durumlarda geri çağrıyı `play` içinden hemen verir (tek karelik animasyon);
        // SwiftUI çizimi sürerken durum değiştirmemek için bir sonraki tura ertelenir.
        Task { @MainActor in
            self.bittiyiCagir(nesil: buNesil)
        }
    }

    private func bittiyiCagir(nesil buNesil: Int) {
        if buNesil != nesil {
            return
        }
        bitti?()
    }
}
