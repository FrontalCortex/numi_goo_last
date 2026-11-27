import Foundation
import Shared

// "Sobi ile analiz et" penceresinin dış bağımlılıkları: sesli okuma, doğru/yanlış sesleri, kural
// istatistikleri, örnek ödemesi ve ortak kodun metin çözücüsü. Hepsi protokol + sahte uygulama
// (GELISTIRME.md: ses, ağ, ödeme şimdilik protokol arkasında).
//
// Android kaynakları: SobiSpeaker.kt (TextToSpeech), MistakeReviewDialog.playSound
// (R.raw.correct_answer_sound 0.3, R.raw.incorrect_answer_sound), SobiRuleStats.kt,
// MistakeReviewDialog.payForExample (ServerSobi.startExample).

// MARK: - Metin çözücü

/// Ortak koddaki `SobiMetinCozucu`: Android `getString(R.string.x, args)` karşılığı. Anahtarlar
/// strings.xml adlarıdır; Metin.al ile `tr.lproj/Localizable.strings`'ten okunur.
final class SobiAnalizMetinCozucusu: NSObject, SobiMetinCozucu {
    func metin(anahtar: String, argumanlar: [Any]) -> String {
        var degerler: [CVarArg] = []
        for arguman in argumanlar {
            if let yazi = arguman as? String {
                degerler.append(yazi)
            } else if let sayi = arguman as? NSNumber {
                // Kotlin Int, Swift'e KotlinInt (NSNumber alt sınıfı) olarak gelir.
                degerler.append(sayi.intValue)
            } else {
                degerler.append(String(describing: arguman))
            }
        }
        let kalip: String = Metin.al(anahtar)
        return Metin.bicimle(kalip, degerler)
    }
}

// MARK: - Sesli okuma

/// Android SobiSpeaker: Sobi'nin mesajlarını sesli okur. `kullanilabilir` false ise (Android: ses motoru
/// hazır değil) aç/kapat düğmesi ve mesajların yanındaki 🔊 gizlenir. `otomatikOku` çocuğun tercihidir
/// (Android: "sobi" tercih dosyası, "auto_read", varsayılan açık).
protocol SobiAnalizSeslendirici: AnyObject {
    var kullanilabilir: Bool { get }
    var otomatikOku: Bool { get set }
    func oku(_ metin: String)
    func durdur()
    func kapat()
}

extension SobiAnalizSeslendirici {
    /// Android speakIfAuto. (Android ayrıca "sound_enabled" ayarına bakar.)
    func otomatikseOku(_ metin: String) {
        if otomatikOku {
            oku(metin)
        }
    }
}

/// Sahte seslendirici: hiçbir şey okumaz ve kullanılamaz görünür (düğmeler gizli).
/// TODO: gerçek uygulama AVSpeechSynthesizer ile yazılacak (tr-TR ses, Android'deki gibi biraz yavaş:
/// setSpeechRate(0.92); tırnak ve eksi işareti temizliği SobiSpeaker.clean). Henüz YOK.
final class SahteSobiAnalizSeslendirici: SobiAnalizSeslendirici {
    var otomatikOku: Bool = true

    var kullanilabilir: Bool {
        return false
    }

    func oku(_ metin: String) {}
    func durdur() {}
    func kapat() {}
}

// MARK: - Ses efektleri

/// Android playSound: doğru (correct_answer_sound, ses 0.3) ve yanlış (incorrect_answer_sound) sesleri;
/// ayarlarda ses kapalıysa ("AppPrefs" / "sound_enabled") çalmaz.
protocol SobiAnalizSesEfektleri: AnyObject {
    func dogruSesi()
    func yanlisSesi()
}

final class SahteSobiAnalizSesEfektleri: SobiAnalizSesEfektleri {
    func dogruSesi() {}
    func yanlisSesi() {}
}

// MARK: - Kural istatistikleri

/// Android SobiRuleStats.recordMove: analiz ekranındaki her Kontrol Et, hamlenin kurallarına doğru ya da
/// yanlış yazılır (soru sayılmaz). Android'de cihazda "sobi_rule_stats" tercih dosyasına yazılır ve
/// 4 sn sonra `recordRuleTally` Cloud Function'ıyla gönderilir (sunucuda homeworkRuleStats/{uid}).
protocol SobiAnalizKuralDeposu: AnyObject {
    func hamleyiKaydet(_ hamle: StepExplainer.ColumnMove, dogru: Bool)
}

/// Sahte depo: ortak koddaki sayım kuralları (SobiRuleStatsStore) bellekte çalışır; uygulama kapanınca
/// gider, sunucuya gönderilmez.
final class SahteSobiAnalizKuralDeposu: SobiAnalizKuralDeposu {
    /// Uygulama boyunca tek depo (analiz pencereleri arasında sayım birikir).
    static let ortak = SahteSobiAnalizKuralDeposu()

    let depo: SobiRuleStatsStore

    init() {
        depo = SobiRuleStatsStore(prefs: InMemoryKeyValueStore())
    }

    func hamleyiKaydet(_ hamle: StepExplainer.ColumnMove, dogru: Bool) {
        depo.recordMove(m: hamle, correct: dogru)
    }
}

// MARK: - Örnek ödemesi

/// Android payForExample: oturum varsa yeni örnek 1 ücretsiz hak ya da 1 Sobi kredisi harcar (Cloud
/// Function `startSobiExample`); oturum yoksa ücretsiz. `tamam(false)`: ödeme başarısız, örnek açılmaz.
protocol SobiAnalizOrnekOdemesi: AnyObject {
    func ornekIcinOde(_ tamam: @escaping (Bool) -> Void)
}

/// Sahte ödeme: her zaman başarılı (Android'de oturumsuz kullanıcının durumu).
final class SahteSobiAnalizOrnekOdemesi: SobiAnalizOrnekOdemesi {
    func ornekIcinOde(_ tamam: @escaping (Bool) -> Void) {
        tamam(true)
    }
}

// MARK: - Toplu

struct SobiAnalizHizmetleri {
    let seslendirici: SobiAnalizSeslendirici
    let sesler: SobiAnalizSesEfektleri
    let kurallar: SobiAnalizKuralDeposu
    let odeme: SobiAnalizOrnekOdemesi

    static func sahte() -> SobiAnalizHizmetleri {
        return SobiAnalizHizmetleri(
            seslendirici: SahteSobiAnalizSeslendirici(),
            sesler: SahteSobiAnalizSesEfektleri(),
            kurallar: SahteSobiAnalizKuralDeposu.ortak,
            odeme: SahteSobiAnalizOrnekOdemesi()
        )
    }
}
