import Foundation
import Shared

// Uygulama metinleri.
//
// Metinler Android'in `strings.xml` dosyasından üretilir ve aynı adla okunur:
//   Android: getString(R.string.offline_title)            -> Metin.al("offline_title")
//   Android: getString(R.string.otp_resend_cooldown, 30)  -> Metin.al("otp_resend_cooldown", 30)
//
// Kaynak dosyalar `<dil>.lproj/Localizable.strings` içindedir ve elle düzenlenmez; üretici:
//   node ios/tools/metin-donustur/donustur.js --diller tr,en
// Android'de karşılığı olmayan, iOS'a özgü metinler (`ios_` önekli) elle yazılan
// `<dil>.lproj/IosOzel.strings` dosyasındadır; araç bu dosyaya dokunmaz. Arama sırası:
// etkin dilin Localizable'ı, etkin dilin IosOzel'i, sonra aynı ikisi varsayılan dilde (Türkçe).
//
// Dil seçimi (Android: AppLanguage). Uygulama içi seçici Profil > Hesap Ayarları > Dil'dedir;
// seçim UserDefaults'ta saklanır, varsayılan "Telefonun dili"dir. Telefonun dili açık dillerden
// biri değilse İngilizce seçilir (Android: AppLanguage.supported). Eksik anahtar Türkçe'den okunur
// (Android: values-en'de olmayan anahtar varsayılan values/'tan gelir). Etkin dilin paketi
// `Bundle(path:)` ile açıkça yüklenir; `Bundle.main` üzerinden olağan arama (NSLocalizedString)
// telefonun diline göre seçim yaptığı için kullanılmaz. Dil değişince kök görünüm yeniden kurulur
// (`MetinDilDurumu`, `MetinDilKoku`; Android uygulamayı yeniden başlatır).
//
// Biçimlendirme `String(format:)`'a doğrudan bırakılmaz: Android'de `%s` her türden değeri
// kabul eder (sayı dahil), iOS'ta ise `%@` yerine sayı verilirse uygulama çöker. Buradaki
// biçimleyici değerin türüne bakar; yanlış türde ya da eksik değer çökertmez.
enum Metin {
    /// Varsayılan dil: eksik anahtarlar bu dilden okunur (Android: values/ Türkçe).
    static let varsayilanDil = "tr"

    /// Uygulamanın bütün dilleri, seçicideki sırayla (Android: AppLanguage.CHOICES, "system" hariç).
    static let tumDiller: [String] = ["tr", "en", "id", "ar"]

    /// Bu sürümde açık diller: `.lproj`'u pakette olan ve seçicide etkin görünenler. Endonezce ve
    /// Arapça açılırken buraya eklenir ve `donustur.js --diller tr,en,id,ar` çalıştırılır
    /// (Arapça için ayrıca sağdan sola düzen ve yazı tipi gerekir).
    static let acikDiller: [String] = ["tr", "en"]

    /// "Telefonun dili" seçimi (Android: AppLanguage.SYSTEM).
    static let sistemSecimi = "system"

    /// Seçimin UserDefaults anahtarı (Android: SharedPreferences "app_language" / "choice").
    static let secimAnahtari = "app_language_choice"

    /// Ekran galerisi için başlatma argümanı: `-dil en` seçimi kaydetmeden o dili açar.
    static let dilArgumani = "-dil"

    /// iOS'a özgü metinlerin tablosu (`<dil>.lproj/IosOzel.strings`).
    private static let iosOzelTablo = "IosOzel"

    /// Şu an kullanılan dil kodu ("tr", "en", "id" …).
    private(set) static var dil: String = varsayilanDil

    /// Varsayılan dilin paketi; etkin dilde bulunmayan anahtarlar buradan okunur.
    private static let yedekPaket: Bundle = paketBul(varsayilanDil) ?? Bundle.main

    /// Etkin dilin paketi.
    private static var etkinPaket: Bundle = paketBul(varsayilanDil) ?? Bundle.main

    /// Bulunamayan anahtarı ayırt etmek için kullanılan, hiçbir metinde geçmeyen değer.
    private static let yokIsareti = "\u{1}metin-yok\u{1}"

    // MARK: - Okuma

    /// Android string adına karşılık gelen metni döndürür. Bulunamazsa adın kendisini döndürür.
    static func al(_ ad: String) -> String {
        return hamMetin(ad)
    }

    /// Biçimli metin (`%1$@`, `%d`, `%1$d` …). Değerler Android'deki sırayla verilir.
    static func al(_ ad: String, _ args: CVarArg...) -> String {
        return bicimle(hamMetin(ad), args)
    }

    private static func hamMetin(_ ad: String) -> String {
        if let deger = ara(ad) {
            return deger
        }
        #if DEBUG
        print("Metin: anahtar bulunamadı: \(ad)")
        #endif
        return ad
    }

    /// Android `string-array` karşılığı. Bulunamazsa boş dizi döner.
    static func dizi(_ ad: String) -> [String] {
        guard let adetMetni = ara(ad + "#adet"), let adet = Int(adetMetni), adet > 0 else {
            return []
        }
        var sonuc: [String] = []
        for sira in 0..<adet {
            sonuc.append(ara(ad + "#" + String(sira)) ?? "")
        }
        return sonuc
    }

    /// Android `plurals` karşılığı (`getQuantityString`). Basit kural: adet 1 ise ve "one"
    /// biçimi tanımlıysa o, değilse "other" kullanılır (Türkçe, İngilizce ve Endonezce için yeterli).
    /// `args` boşsa metin biçimlenmeden döner (Android ile aynı).
    static func cogul(_ ad: String, _ adet: Int, _ args: CVarArg...) -> String {
        var kalip: String? = nil
        if adet == 1 {
            kalip = ara(ad + "#one")
        }
        if kalip == nil {
            kalip = ara(ad + "#other")
        }
        guard let bulunan = kalip else {
            return ad
        }
        if args.isEmpty {
            return bulunan
        }
        return bicimle(bulunan, args)
    }

    /// Anahtar tanımlı mı? (Ortak kodun verdiği adları denemek için.)
    static func varMi(_ ad: String) -> Bool {
        return ara(ad) != nil
    }

    /// Ortak katalogdaki Türkçe ders metnini (ad, ünite adı, yarış adı, ipucu) seçili dile çevirir
    /// (Android: GlobalLessonData.localizeLessonText). Tablo `DersMetinAnahtarlari.swift`'tedir
    /// (ios/tools/ders-katalog/uret.js üretir). Tanınmayan metin `cevir` ile denenir, o da
    /// bulamazsa olduğu gibi döner. Ders adları kayıtlarda kimlik olarak Türkçe kalır; yalnızca
    /// gösterirken çağrılmalıdır.
    static func ders(_ turkce: String) -> String {
        if dil == varsayilanDil || turkce.isEmpty {
            return turkce
        }
        if let anahtar = DersMetinAnahtarlari.tablo[turkce], let deger = ara(anahtar) {
            return deger
        }
        return cevir(turkce)
    }

    /// Ortak koddan gelen Türkçe sabit metni (Android'de bir string anahtarından okunan) seçili dile
    /// çevirir: metin `tr.lproj/Localizable.strings`'te bir anahtarın değeriyse o anahtarın etkin
    /// dildeki karşılığı döner, değilse metnin kendisi. Biçimli (`%1$@` içeren) kalıplar bu yolla
    /// bulunmaz. Aynı Türkçe metni taşıyan birden çok anahtar varsa herhangi biri seçilir.
    static func cevir(_ turkce: String) -> String {
        if dil == varsayilanDil || turkce.isEmpty {
            return turkce
        }
        guard let anahtar = turkceTers[turkce], let deger = ara(anahtar) else {
            return turkce
        }
        return deger
    }

    /// Türkçe değer → anahtar (ilk kullanımda varsayılan dilin dosyasından bir kez kurulur).
    private static let turkceTers: [String: String] = tersTabloKur()

    private static func tersTabloKur() -> [String: String] {
        var sonuc: [String: String] = [:]
        for tablo in ["Localizable", iosOzelTablo] {
            guard let yol = yedekPaket.path(forResource: tablo, ofType: "strings"),
                  let sozluk = NSDictionary(contentsOfFile: yol) as? [String: String] else {
                continue
            }
            for (anahtar, deger) in sozluk where !anahtar.contains("#") {
                if sonuc[deger] == nil {
                    sonuc[deger] = anahtar
                }
            }
        }
        return sonuc
    }

    // MARK: - Dil

    /// Kayıtlı seçim: `sistemSecimi` ya da açık dillerden biri (Android: AppLanguage.choice).
    static var secim: String {
        let kayitli: String = UserDefaults.standard.string(forKey: secimAnahtari) ?? sistemSecimi
        if kayitli == sistemSecimi || acikDiller.contains(kayitli) {
            return kayitli
        }
        return sistemSecimi
    }

    /// Uygulama açılırken bir kez çağrılır (SorobitApp.init): `-dil <kod>` başlatma argümanı varsa o dil
    /// (kaydedilmez; ekran galerisi), yoksa kayıtlı seçim uygulanır.
    static func baslat() {
        let argumanlar: [String] = ProcessInfo.processInfo.arguments
        if let i = argumanlar.firstIndex(of: dilArgumani), i + 1 < argumanlar.count {
            diliAyarla(cozumle(argumanlar[i + 1]))
            return
        }
        diliAyarla(cozumle(secim))
    }

    /// Seçimi kaydeder, dili değiştirir ve kök görünümü yeniden kurdurur (Android: AppLanguage.apply —
    /// kaydedip uygulamayı baştan açar). Seçim değişmediyse hiçbir şey yapmaz. Ana iş parçacığından.
    static func secimiUygula(_ yeni: String) {
        if yeni == secim {
            return
        }
        UserDefaults.standard.set(yeni, forKey: secimAnahtari)
        diliAyarla(cozumle(yeni))
        MetinDilDurumu.ortak.yenile()
    }

    /// Seçimi dil koduna çevirir (Android: AppLanguage.resolve / supported): açık bir dil olduğu gibi;
    /// "system" (ya da tanınmayan değer) telefonun ilk tercih ettiği dile göre: açık dillerden biriyse
    /// o, değilse İngilizce (açıksa). Endonezcenin eski kodu "in" "id" sayılır.
    static func cozumle(_ secilen: String) -> String {
        if acikDiller.contains(secilen) {
            return secilen
        }
        let telefon: String = Locale.preferredLanguages.first ?? varsayilanDil
        var kod: String = telefon
        if let tire = telefon.firstIndex(where: { $0 == "-" || $0 == "_" }) {
            kod = String(telefon[..<tire])
        }
        kod = kod.lowercased()
        if kod == "in" {
            kod = "id"
        }
        if acikDiller.contains(kod) {
            return kod
        }
        return acikDiller.contains("en") ? "en" : varsayilanDil
    }

    /// Seçili dilin yereli: tarih, sayı ve büyük harf dönüşümleri için (Android: Locale.getDefault(),
    /// AppLanguage.wrap ayarlar). Arapça Batı rakamlı (Android: ar-u-nu-latn).
    static var yerel: Locale {
        if dil == "ar" {
            return Locale(identifier: "ar@numbers=latn")
        }
        return Locale(identifier: dil)
    }

    /// Uygulama dilini değiştirir. Paketi olmayan bir dil verilirse varsayılan dile düşer.
    /// `nil` verilirse kayıtlı seçim çözülür. Ana iş parçacığından çağrılmalıdır; açık ekranlar
    /// kendiliğinden yenilenmez (bkz. `secimiUygula`).
    static func diliAyarla(_ kod: String?) {
        let istenen: String = kod ?? cozumle(secim)
        if let paket = paketBul(istenen) {
            etkinPaket = paket
            dil = istenen
        } else {
            etkinPaket = yedekPaket
            dil = varsayilanDil
        }
        // Ortak kodun tek dil ayarı (Android: AppLanguage.textLanguage() / Locale.getDefault()): cümleyi
        // kendisi kuran Sobi hata analizi (SobiAnalizDili buna bağlıdır) ve ondalık ayırıcı (Sobi testi saniyesi).
        OrtakDil.shared.language = dil
    }

    /// `<kod>.lproj` klasörünü paket olarak açar; "en-GB" bulunamazsa "en" denenir.
    private static func paketBul(_ kod: String) -> Bundle? {
        var adaylar: [String] = [kod]
        if let tire = kod.firstIndex(of: "-") {
            adaylar.append(String(kod[..<tire]))
        }
        for aday in adaylar {
            if let yol = Bundle.main.path(forResource: aday, ofType: "lproj"), let paket = Bundle(path: yol) {
                return paket
            }
        }
        return nil
    }

    /// Sırasıyla etkin dil, varsayılan dil ve ana pakette arar (her pakette önce Localizable, sonra
    /// IosOzel); yoksa `nil`.
    private static func ara(_ anahtar: String) -> String? {
        if anahtar.isEmpty {
            return nil
        }
        let paketler: [Bundle] = [etkinPaket, yedekPaket, Bundle.main]
        let tablolar: [String?] = [nil, iosOzelTablo]
        for paket in paketler {
            for tablo in tablolar {
                let deger = paket.localizedString(forKey: anahtar, value: yokIsareti, table: tablo)
                if deger != yokIsareti {
                    return deger
                }
            }
        }
        return nil
    }

    // MARK: - Biçimlendirme

    /// `kalip` içindeki belirteçleri `args` ile doldurur. Desteklenenler:
    /// `%@`/`%s` (her tür değer), `%d`/`%i`/`%u`, `%f`/`%e`/`%g`, `%x`/`%X`/`%o`, `%c`, `%%`;
    /// sıralı (`%1$@`) ve sırasız biçimler; `%05d`, `%.1f` gibi genişlik ve kesinlik.
    /// Karşılığı olmayan belirteç olduğu gibi bırakılır.
    static func bicimle(_ kalip: String, _ args: [CVarArg]) -> String {
        let harfler: [Character] = Array(kalip)
        let uzunluk: Int = harfler.count
        var sonuc = ""
        var i = 0
        var siradaki = 0

        while i < uzunluk {
            let harf = harfler[i]
            if harf != "%" {
                sonuc.append(harf)
                i += 1
                continue
            }
            if i + 1 < uzunluk && harfler[i + 1] == "%" {
                sonuc.append("%")
                i += 2
                continue
            }

            // "%" sonrası: [sıra$][bayrak][genişlik][.kesinlik][uzunluk]tür
            var j = i + 1
            var rakamlar = ""
            while j < uzunluk && rakamMi(harfler[j]) {
                rakamlar.append(harfler[j])
                j += 1
            }
            var acikSira: Int? = nil
            var govde = ""
            if !rakamlar.isEmpty && j < uzunluk && harfler[j] == "$" {
                acikSira = Int(rakamlar)
                j += 1
            } else {
                govde = rakamlar
            }
            while j < uzunluk && govdeHarfiMi(harfler[j]) {
                govde.append(harfler[j])
                j += 1
            }
            while j < uzunluk && uzunlukHarfiMi(harfler[j]) {
                j += 1
            }

            if j >= uzunluk || !turHarfiMi(harfler[j]) {
                // Belirteç değil (ör. "%50 indirim" sonundaki yalın yüzde): olduğu gibi yaz.
                sonuc.append("%")
                i += 1
                continue
            }
            let tur: Character = harfler[j]

            var sira: Int = siradaki
            if let verilen = acikSira {
                sira = verilen - 1
            } else {
                siradaki += 1
            }

            if sira < 0 || sira >= args.count {
                // Değer verilmemiş: belirteci olduğu gibi bırak.
                sonuc.append(String(harfler[i...j]))
            } else {
                sonuc.append(tekDeger(args[sira], tur: tur, govde: govde))
            }
            i = j + 1
        }
        return sonuc
    }

    /// Tek bir belirtecin metnini üretir.
    private static func tekDeger(_ deger: CVarArg, tur: Character, govde: String) -> String {
        switch tur {
        case "d", "D", "i", "u", "U":
            if let sayi = tamSayi(deger) {
                return String(format: "%" + govde + "lld", sayi)
            }
            return duzMetin(deger)
        case "x", "X", "o":
            if let sayi = tamSayi(deger) {
                return String(format: "%" + govde + "ll" + String(tur), sayi)
            }
            return duzMetin(deger)
        case "f", "F", "e", "E", "g", "G":
            if let sayi = ondalik(deger) {
                // Android varsayılan yerelle biçimler (Türkçe'de ondalık ayırıcı virgül).
                return String(format: "%" + govde + String(tur), locale: Locale(identifier: dil), sayi)
            }
            return duzMetin(deger)
        default:
            // "@", "s", "S", "c", "C": değer ne olursa olsun metne çevrilir.
            return duzMetin(deger)
        }
    }

    private static func duzMetin(_ deger: CVarArg) -> String {
        if let metin = deger as? String {
            return metin
        }
        return String(describing: deger)
    }

    /// Değer tam sayıya çevrilebiliyorsa döndürür (Swift ve Kotlin sayı türleri).
    private static func tamSayi(_ deger: CVarArg) -> Int64? {
        switch deger {
        case let v as Int: return Int64(v)
        case let v as Int32: return Int64(v)
        case let v as Int64: return v
        case let v as Int16: return Int64(v)
        case let v as Int8: return Int64(v)
        case let v as UInt: return Int64(truncatingIfNeeded: v)
        case let v as UInt32: return Int64(v)
        case let v as UInt64: return Int64(truncatingIfNeeded: v)
        case let v as UInt16: return Int64(v)
        case let v as UInt8: return Int64(v)
        case let v as Double: return tamKisim(v)
        case let v as Float: return tamKisim(Double(v))
        case let v as NSNumber: return v.int64Value
        default: return nil
        }
    }

    private static func tamKisim(_ v: Double) -> Int64? {
        if !v.isFinite || v > 9.0e18 || v < -9.0e18 {
            return nil
        }
        return Int64(v.rounded(.towardZero))
    }

    /// Değer ondalık sayıya çevrilebiliyorsa döndürür.
    private static func ondalik(_ deger: CVarArg) -> Double? {
        switch deger {
        case let v as Double: return v
        case let v as Float: return Double(v)
        case let v as NSNumber: return v.doubleValue
        default:
            if let tam = tamSayi(deger) {
                return Double(tam)
            }
            return nil
        }
    }

    private static func rakamMi(_ harf: Character) -> Bool {
        return harf >= "0" && harf <= "9"
    }

    /// Bayrak, genişlik ve kesinlik harfleri.
    private static func govdeHarfiMi(_ harf: Character) -> Bool {
        return rakamMi(harf) || harf == "-" || harf == "+" || harf == " " || harf == "#" || harf == "."
    }

    /// C uzunluk belirteçleri ("%ld", "%lld" …); atlanır.
    private static func uzunlukHarfiMi(_ harf: Character) -> Bool {
        return harf == "l" || harf == "h" || harf == "q" || harf == "z" || harf == "j" || harf == "t" || harf == "L"
    }

    private static func turHarfiMi(_ harf: Character) -> Bool {
        let turler: String = "@sSdDiuUfFeEgGxXocC"
        return turler.contains(harf)
    }
}
