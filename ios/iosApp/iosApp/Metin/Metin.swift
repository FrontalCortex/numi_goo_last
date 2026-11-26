import Foundation

// Uygulama metinleri.
//
// Metinler Android'in `strings.xml` dosyasından üretilir ve aynı adla okunur:
//   Android: getString(R.string.offline_title)            -> Metin.al("offline_title")
//   Android: getString(R.string.otp_resend_cooldown, 30)  -> Metin.al("otp_resend_cooldown", 30)
//
// Kaynak dosyalar `<dil>.lproj/Localizable.strings` içindedir ve elle düzenlenmez; üretici:
//   node ios/tools/metin-donustur/donustur.js
//
// Dil seçimi: uygulama şimdilik Türkçe'ye sabittir. Paket yalnızca `tr.lproj` içerirken
// geliştirme bölgesi `en` olduğundan, `Bundle.main` üzerinden yapılan olağan arama
// (NSLocalizedString) telefonun dili Türkçe değilse `tr.lproj`'u seçmeyebilir ve anahtarın
// kendisini döndürür. Bu yüzden burada `tr.lproj` paketi açıkça yüklenir. İleride İngilizce ve
// Endonezce `.lproj` klasörleri eklendiğinde `diliAyarla` ile dil değiştirilebilir.
//
// Biçimlendirme `String(format:)`'a doğrudan bırakılmaz: Android'de `%s` her türden değeri
// kabul eder (sayı dahil), iOS'ta ise `%@` yerine sayı verilirse uygulama çöker. Buradaki
// biçimleyici değerin türüne bakar; yanlış türde ya da eksik değer çökertmez.
enum Metin {
    /// Uygulamanın sabitlendiği dil.
    static let varsayilanDil = "tr"

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

    // MARK: - Dil

    /// Uygulama dilini değiştirir. Paketi olmayan bir dil verilirse varsayılan dile düşer.
    /// `nil` verilirse telefonun dil tercihine göre paketteki en uygun dil seçilir.
    /// Ana iş parçacığından çağrılmalıdır; açık ekranlar kendiliğinden yenilenmez.
    static func diliAyarla(_ kod: String?) {
        let istenen: String = kod ?? (Bundle.main.preferredLocalizations.first ?? varsayilanDil)
        if let paket = paketBul(istenen) {
            etkinPaket = paket
            dil = istenen
        } else {
            etkinPaket = yedekPaket
            dil = varsayilanDil
        }
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

    /// Sırasıyla etkin dil, varsayılan dil ve ana pakette arar; yoksa `nil`.
    private static func ara(_ anahtar: String) -> String? {
        if anahtar.isEmpty {
            return nil
        }
        let paketler: [Bundle] = [etkinPaket, yedekPaket, Bundle.main]
        for paket in paketler {
            let deger = paket.localizedString(forKey: anahtar, value: yokIsareti, table: nil)
            if deger != yokIsareti {
                return deger
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
