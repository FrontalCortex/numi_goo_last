import Foundation
import Shared

// Avatar klasörünün ortak Kotlin koduna açılan TEK kapısı; Avatar dosyalarından yalnızca bu dosya
// `Shared`'i içe aktarır.
//
// Çağrılan Kotlin dosyası: ios/shared/src/commonMain/kotlin/com/sorobit/shared/profil/AvatarConfigLogic.kt
// (AvatarRules, AvatarParts, AvatarTab, ProfileHeaderColors). Bu dosya bu iş sırasında eklendi;
// `ios/docs/Shared.h` içinde HENÜZ YOK. Swift adları GELISTIRME.md kurallarıyla türetildi
// (object → `.shared`, companion → `.companion`, Int → Int32); Codemagic başlığıyla doğrulanmalı.

/// Düzenleme ekranındaki bir sekmenin Swift kopyası (ortak kod: AvatarTab).
struct AvatarSekmesi: Identifiable, Equatable {
    /// Stilin sekme listesindeki sırası.
    let id: Int
    /// `AvatarConfig.values` anahtarı ("hair", "hairColor" …).
    let anahtar: String
    let etiket: String
    /// Renk sekmesi mi (seçenekler palet).
    let renk: Bool
    /// Küçük resim yüze yaklaşır mı (göz, burun, ağız, sakal).
    let yakinPlan: Bool
    /// Kutuda avatar yerine yalnızca renk dairesi (arka plan).
    let renkDairesi: Bool
}

enum AvatarOrtak {
    private static var kurallar: AvatarRules { AvatarRules.shared }

    // MARK: Parça paketi

    /// `personas_parts.json` (Android: assets/avatar/personas_parts.json) paketin kökünden bir kez okunur.
    /// Bulunamazsa boş paket: avatarlar parçasız (yalnızca daire) çizilir, uygulama çökmez.
    static let paket: AvatarParts = AvatarOrtak.paketiYukle()

    private static func paketiYukle() -> AvatarParts {
        let ad: String = AvatarRules.shared.PARTS_RESOURCE_NAME
        let uzanti: String = AvatarRules.shared.PARTS_RESOURCE_EXTENSION
        guard let adres = Bundle.main.url(forResource: ad, withExtension: uzanti),
              let veri = try? Data(contentsOf: adres),
              let metin = String(data: veri, encoding: .utf8) else {
            #if DEBUG
            print("Avatar: personas_parts.json bulunamadı")
            #endif
            return AvatarParts.companion.EMPTY
        }
        return AvatarParts.companion.parse(json: metin)
    }

    // MARK: Biçim

    /// `users/{uid}.avatarConfig` / `publicProfiles/{uid}.avatarConfig`.
    static var firestoreAlani: String { kurallar.FIRESTORE_FIELD }

    static var varsayilan: String { kurallar.defaultEncoded() }

    /// Dizge geçerli bir avatar mı (değilse listelerde harfli daire, profilde `non_user`).
    static func avatarVar(_ config: String?) -> Bool {
        return kurallar.hasAvatar(encoded: config)
    }

    /// Okunup yeniden yazılmış hâli; okunamazsa varsayılan.
    static func duzgun(_ config: String?) -> String {
        return kurallar.normalized(encoded: config)
    }

    /// Android AvatarStore.applyRemote: uzaktaki değer yerele alınacaksa yazılacak dizge, değilse nil.
    static func yereleAlinacak(yerel: String?, uzak: String?) -> String? {
        return kurallar.remoteToApply(localEncoded: yerel, remoteEncoded: uzak)
    }

    static func degisenParcaSayisi(ilk: String?, son: String) -> Int {
        return Int(kurallar.changedPartCount(baseline: ilk, current: son))
    }

    // MARK: Sekmeler

    private static var kotlinSekmeleri: [AvatarTab] = []

    /// Avatarın stilinin sekmeleri (Android: style.tabs). Etiketler `Metin.al` ile.
    static func sekmeler(_ config: String) -> [AvatarSekmesi] {
        let liste: [AvatarTab] = kurallar.tabs(encoded: config)
        kotlinSekmeleri = liste
        var sonuc: [AvatarSekmesi] = []
        for (sira, sekme) in liste.enumerated() {
            var etiket: String = sekme.label
            if let ad = sekme.labelRes {
                etiket = Metin.al(ad)
            }
            sonuc.append(
                AvatarSekmesi(
                    id: sira,
                    anahtar: sekme.key,
                    etiket: etiket,
                    renk: sekme.isColor,
                    yakinPlan: sekme.closeUp,
                    renkDairesi: sekme.swatch
                )
            )
        }
        return sonuc
    }

    private static func kotlinSekmesi(_ sekme: AvatarSekmesi, _ config: String) -> AvatarTab? {
        if kotlinSekmeleri.isEmpty {
            kotlinSekmeleri = kurallar.tabs(encoded: config)
        }
        guard sekme.id >= 0 && sekme.id < kotlinSekmeleri.count else { return nil }
        let aday: AvatarTab = kotlinSekmeleri[sekme.id]
        return aday.key == sekme.anahtar ? aday : nil
    }

    /// Sekmenin seçenekleri; isteğe bağlı parçada başta "" (Yok). Android: AvatarArt.options.
    static func secenekler(_ sekme: AvatarSekmesi, config: String) -> [String] {
        guard let kotlin = kotlinSekmesi(sekme, config) else { return [] }
        return kurallar.options(parts: paket, encoded: config, tab: kotlin)
    }

    /// Sekmenin şu anki değeri (Android: currentValue).
    static func secili(_ sekme: AvatarSekmesi, config: String) -> String {
        guard let kotlin = kotlinSekmesi(sekme, config) else { return "" }
        return kurallar.currentValue(encoded: config, tab: kotlin)
    }

    /// Sekmede `deger` seçilmiş hâl (Android: withValue).
    static func sec(_ sekme: AvatarSekmesi, deger: String, config: String) -> String {
        guard let kotlin = kotlinSekmesi(sekme, config) else { return config }
        return kurallar.withTabValue(encoded: config, tab: kotlin, value: deger)
    }

    /// Android: AvatarConfig.random.
    static func rastgele(_ config: String) -> String {
        return kurallar.randomEncoded(parts: paket, encoded: config)
    }

    static var izgaraSutunu: Int { Int(kurallar.GRID_COLUMNS) }

    /// Seçili olmayan çipin zemini (RRGGBB).
    static var bosCipRengi: String { kurallar.CHIP_IDLE_COLOR }

    // MARK: Çizim

    private static var onbellek: [String: AvatarSvgBelgesi] = [:]
    private static var onbellekSirasi: [String] = []
    /// Android AvatarView: `LruCache<String, Picture>(120)`.
    private static let onbellekSiniri: Int = 120

    /// Çizime hazır SVG belgesi (önbellekli). Ana iş parçacığından çağrılmalı (görünüm gövdesi).
    static func belge(config: String, daire: Bool, yakinPlan: Bool) -> AvatarSvgBelgesi? {
        let anahtar: String = config + "|" + (daire ? "1" : "0") + (yakinPlan ? "1" : "0")
        if let hazir = onbellek[anahtar] {
            return hazir
        }
        let svg: String = kurallar.svg(parts: paket, encoded: config, circle: daire, closeUp: yakinPlan)
        guard let yeni = AvatarSvgBelgesi.coz(svg) else { return nil }
        onbellek[anahtar] = yeni
        onbellekSirasi.append(anahtar)
        if onbellekSirasi.count > onbellekSiniri {
            let eski: String = onbellekSirasi.removeFirst()
            onbellek[eski] = nil
        }
        return yeni
    }

    // MARK: Profil üst alanı (ProfileFragment.applyHeaderColor)

    /// Avatar alanının rengi (AARRGGBB); avatar yoksa ya da renk okunamazsa nil (`background_color`).
    static func ustAlanRengi(_ config: String?) -> String? {
        return kurallar.headerColorArgb(encoded: config)
    }

    /// Zemine göre yazı/ikon rengi (AARRGGBB).
    static func ustAlanYaziRengi(zemin: String) -> String {
        return ProfileHeaderColors.shared.foreground(backgroundArgb: zemin)
    }

    /// Kaydırınca görünen alt çizgi rengi (AARRGGBB).
    static func ustAlanCizgiRengi(zemin: String) -> String {
        return ProfileHeaderColors.shared.divider(backgroundArgb: zemin)
    }
}
