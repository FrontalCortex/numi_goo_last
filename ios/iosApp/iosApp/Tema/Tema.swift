import CoreText
import SwiftUI
import UIKit

// Uygulamanın yazı tipi.
//
// Android tek bir özel yazı tipi kullanır: res/font/font_regular.ttf (Baloo 2, değişken font,
// kalınlık ekseni 400–800). Android temasında bütün metinler `textStyle="bold"` ile gelir;
// bu yüzden buradaki yardımcılarda da varsayılan kalındır.
//
// Dosya `Fontlar/font_regular.ttf` olarak pakete girer. Adlar dosyanın `name` tablosundan
// okundu (ios/tools/gorsel-donustur/donustur.js çıktısı): PostScript adı "Baloo2-Regular",
// aile "Baloo 2".
//
// Görseller ve renkler için yardımcı yoktur; Android kaynak adıyla doğrudan kullanılır:
// `Image("star_on_ic")`, `Color("background_color")`.
enum Tema {
    /// Yazı tipinin PostScript adı (font dosyasının `name` tablosu, kayıt 6).
    static let yaziTipiAdi = "Baloo2-Regular"

    /// Kalın metnin ağırlığı (değişken fontun `wght` ekseni; 400 = normal, en çok 800).
    static let kalinAgirlik: CGFloat = 700

    /// Normal metnin ağırlığı.
    static let normalAgirlik: CGFloat = 400

    private static var kaydedildi = false

    /// Paketteki .ttf/.otf dosyalarını bu süreç için sisteme tanıtır.
    /// Uygulama açılırken bir kez çağrılır; yeniden çağrılması zararsızdır.
    static func yaziTipleriniKaydet() {
        if kaydedildi { return }
        kaydedildi = true
        var adresler: [URL] = []
        for uzanti in ["ttf", "otf"] {
            adresler += Bundle.main.urls(forResourcesWithExtension: uzanti, subdirectory: nil) ?? []
            // Klasör, pakete klasör başvurusu olarak girerse dosyalar alt dizinde kalır.
            adresler += Bundle.main.urls(forResourcesWithExtension: uzanti, subdirectory: "Fontlar") ?? []
        }
        for adres in adresler {
            // Zaten kayıtlıysa hata döner; yok sayılır.
            CTFontManagerRegisterFontsForURL(adres as CFURL, .process, nil)
        }
    }

    /// Uygulama yazı tipi, UIKit karşılığı. Font bulunamazsa sistem fontuna düşer.
    static func uiYazi(_ boyut: CGFloat, kalin: Bool = true) -> UIFont {
        yaziTipleriniKaydet()
        let agirlik = kalin ? kalinAgirlik : normalAgirlik
        guard let temel = UIFont(name: yaziTipiAdi, size: boyut) else {
            return UIFont.systemFont(ofSize: boyut, weight: kalin ? .bold : .regular)
        }
        // Değişken fontta kalınlık, eksen kimliği ('wght' = 0x77676874) üzerinden verilir.
        let eksenKimligi = NSNumber(value: 0x7767_6874)
        let degiskenlikAnahtari = UIFontDescriptor.AttributeName(rawValue: kCTFontVariationAttribute as String)
        let tanim = temel.fontDescriptor.addingAttributes([
            degiskenlikAnahtari: [eksenKimligi: NSNumber(value: Double(agirlik))]
        ])
        return UIFont(descriptor: tanim, size: boyut)
    }

    /// Uygulama yazı tipi. `boyut` Android'deki sp değeriyle aynı sayıdır.
    /// Android'de bütün metinler kalın olduğu için `kalin` varsayılanı `true`.
    static func yazi(_ boyut: CGFloat, kalin: Bool = true) -> Font {
        Font(uiYazi(boyut, kalin: kalin) as CTFont)
    }
}
