import SwiftUI

// Sobi maskotunun hâlleri ve duruş kanalları.
// Kaynak: app/src/main/java/com/example/app/BunnyMascotView.kt (Emote, Eyes, Mouth, Ch).

/// Android `BunnyMascotView.Emote`; ham değer Android sabitinin adı (ör. "TEACH_TALK").
/// [sureMs] tur süresi, [dongu] süresi dolunca baştan dönüyor mu, [tutmaMs] ≥ 0 ise bir kez
/// oynayıp yalnızca o andan sonraki bölüm kendi içinde dönüyor (Android `holdFromMs`).
enum SobiMaskotHal: String, CaseIterable {
    case GREET, CHEER, BALLOON, SLEEP, POINT, PEACE, SAD, DANCE, THINK, CLAP, SURPRISED, SHY, WINK, HEART
    case ABACUS, CHAT, FLAME, CALENDAR, JETPACK, HERO, CROWN
    case AD_JUMP, AD_BUTTON, AD_PUSH, AD_ZAP, AD_MAGIC
    case BOARD, LISTEN, NOTEBOOK, NOTEBOOK_HOLD, NOTEBOOK_WRITE, ALARM
    case WEIGHTS, SKATE, GUITAR, JUGGLE, PERFECT, STARS, DETERMINED
    case COWBOY_FRONT, KARATE, NINJA, PIRATE, GLASSES, RAPPER
    case TEACH_TALK, TEACH_POINT, TEACH_WARN

    var sureMs: Double {
        switch self {
        case .GREET: return 1700
        case .CHEER: return 1400
        case .BALLOON: return 3500
        case .SLEEP: return 4200
        case .POINT: return 3000
        case .PEACE: return 2000
        case .SAD: return 3200
        case .DANCE: return 3200
        case .THINK: return 3500
        case .CLAP: return 2500
        case .SURPRISED: return 1800
        case .SHY: return 3200
        case .WINK: return 1500
        case .HEART: return 3000
        case .ABACUS: return 4200
        case .CHAT: return 6000
        case .FLAME: return 6000
        case .CALENDAR: return 5500
        case .JETPACK: return 4000
        case .HERO: return 4000
        case .CROWN: return 4000
        case .AD_JUMP: return 3200
        case .AD_BUTTON: return 4400
        case .AD_PUSH: return 3600
        case .AD_ZAP: return 5400
        case .AD_MAGIC: return 5600
        case .BOARD: return 5000
        case .LISTEN: return 4400
        case .NOTEBOOK: return 4600
        case .NOTEBOOK_HOLD: return 3000
        case .NOTEBOOK_WRITE: return 2300
        case .ALARM: return 4500
        case .WEIGHTS: return 4200
        case .SKATE: return 4400
        case .GUITAR: return 4000
        case .JUGGLE: return 3600
        case .PERFECT: return 3600
        case .STARS: return 4400
        case .DETERMINED: return 4300
        case .COWBOY_FRONT: return 7600
        case .KARATE: return 6400
        case .NINJA: return 7000
        case .PIRATE: return 6800
        case .GLASSES: return 5600
        case .RAPPER: return 6000
        case .TEACH_TALK: return 3000
        case .TEACH_POINT: return 2400
        case .TEACH_WARN: return 2400
        }
    }

    /// Android `loops`: ilk 14 hâl ve NOTEBOOK_WRITE bir kez oynayıp beklemeye dönüyor.
    var dongu: Bool {
        switch self {
        case .GREET, .CHEER, .BALLOON, .SLEEP, .POINT, .PEACE, .SAD, .DANCE, .THINK, .CLAP,
             .SURPRISED, .SHY, .WINK, .HEART, .ABACUS, .NOTEBOOK_WRITE:
            return false
        default:
            return true
        }
    }

    /// Android `holdFromMs`; −1 yok.
    var tutmaMs: Double {
        switch self {
        case .PERFECT: return 1600
        case .DETERMINED: return 3300
        case .COWBOY_FRONT: return 6900
        case .KARATE: return 5800
        case .NINJA: return 4600
        case .PIRATE: return 4600
        case .GLASSES: return 3600
        default: return -1
        }
    }

    /// Android `label` (oyun alanındaki ad).
    var etiket: String {
        switch self {
        case .GREET: return "Selam"
        case .CHEER: return "Sevinç"
        case .BALLOON: return "Balon"
        case .SLEEP: return "Uyuma"
        case .POINT: return "İşaret etme"
        case .PEACE: return "Zafer işareti"
        case .SAD: return "Üzgün"
        case .DANCE: return "Dans"
        case .THINK: return "Düşünme"
        case .CLAP: return "Alkış"
        case .SURPRISED: return "Şaşırma"
        case .SHY: return "Utangaç"
        case .WINK: return "Göz kırpma"
        case .HEART: return "Kalp"
        case .ABACUS: return "Abaküs"
        case .CHAT: return "Mesajlaşma"
        case .FLAME: return "Alev (seri)"
        case .CALENDAR: return "Takvim (seri)"
        case .JETPACK: return "Jetpack (Pro)"
        case .HERO: return "Süper kahraman (Pro)"
        case .CROWN: return "Taç (Pro)"
        case .AD_JUMP: return "Reklamı atla"
        case .AD_BUTTON: return "Atla düğmesi"
        case .AD_PUSH: return "Reklamı itme"
        case .AD_ZAP: return "Değnek + yıldırım"
        case .AD_MAGIC: return "Değnek + yasak işareti"
        case .BOARD: return "Kara tahta"
        case .LISTEN: return "Dinleyen tavşan"
        case .NOTEBOOK: return "Havuç kalem"
        case .NOTEBOOK_HOLD: return "Havuç kalem (bekleme)"
        case .NOTEBOOK_WRITE: return "Havuç kalem (yazma)"
        case .ALARM: return "Çalar saat (hatırlatma)"
        case .WEIGHTS: return "Boncuk halteri"
        case .SKATE: return "Kaykay"
        case .GUITAR: return "Gitar"
        case .JUGGLE: return "Boncuk hokkabazlığı"
        case .PERFECT: return "Mükemmel (taç + gözlük)"
        case .STARS: return "Yıldızlar (sandık)"
        case .DETERMINED: return "Üzgün → kararlı"
        case .COWBOY_FRONT: return "Kovboy önden (bandana)"
        case .KARATE: return "Karate (tahta kırma)"
        case .NINJA: return "Ninja (duman bombası)"
        case .PIRATE: return "Korsan (ters dürbün)"
        case .GLASSES: return "Gözlük (havalı bakış)"
        case .RAPPER: return "Rapçi (kep + zincir)"
        case .TEACH_TALK: return "Anlatma (havuç)"
        case .TEACH_POINT: return "Gösterme (havuç)"
        case .TEACH_WARN: return "Uyarı (havuç)"
        }
    }

    /// Durağan çizimde (animasyonsuz) gösterilen an, hâlin başından saniye. Android'de
    /// karşılığı yok: her hâlin en tanınır karesi seçildi (bekleme bölümü olanlarda o bölümden).
    var duraganAn: CGFloat {
        switch self {
        case .GREET: return 0.8
        case .CHEER: return 0.2
        case .BALLOON: return 2.0
        case .SLEEP: return 2.0
        case .POINT: return 1.5
        case .PEACE: return 1.0
        case .SAD: return 1.6
        case .DANCE: return 0.8
        case .THINK: return 2.0
        case .CLAP: return 0.44
        case .SURPRISED: return 0.17
        case .SHY: return 1.5
        case .WINK: return 0.8
        case .HEART: return 1.5
        case .ABACUS: return 1.5
        case .CHAT: return 4.2
        case .FLAME: return 5.5
        case .CALENDAR: return 2.5
        case .JETPACK: return 1.0
        case .HERO: return 1.0
        case .CROWN: return 1.0
        case .AD_JUMP: return 0.86
        case .AD_BUTTON: return 1.15
        case .AD_PUSH: return 1.5
        case .AD_ZAP: return 1.57
        case .AD_MAGIC: return 1.9
        case .BOARD: return 3.0
        case .LISTEN: return 1.0
        case .NOTEBOOK: return 1.0
        case .NOTEBOOK_HOLD: return 1.0
        case .NOTEBOOK_WRITE: return 1.0
        case .ALARM: return 1.8
        case .WEIGHTS: return 2.2
        case .SKATE: return 3.0
        case .GUITAR: return 1.0
        case .JUGGLE: return 0.45
        case .PERFECT: return 2.0
        case .STARS: return 3.0
        case .DETERMINED: return 3.5
        case .COWBOY_FRONT: return 5.6
        case .KARATE: return 6.0
        case .NINJA: return 5.0
        case .PIRATE: return 5.0
        case .GLASSES: return 4.0
        case .RAPPER: return 1.0
        case .TEACH_TALK, .TEACH_POINT, .TEACH_WARN: return 0
        }
    }
}

/// Android `Eyes`.
enum SobiMaskotGoz {
    case normal, mutlu, kapali, kirpma
}

/// Android `Mouth`.
enum SobiMaskotAgiz {
    case bekleme, mutlu, konusma, o, uzgun, cigneme, yanGulus
}

/// Android `Ch`: duruş kanalları. Açılarda artı = dışa/yukarı.
enum SobiMaskotKanal {
    static let hangLA = 0
    static let hangRA = 1
    static let hangL = 2
    static let hangR = 3
    static let catLA = 4
    static let catRA = 5
    static let catL = 6
    static let catR = 7
    static let peaceA = 8
    static let peace = 9
    static let frontA = 10
    static let frontGap = 11
    static let frontRot = 12
    static let frontDy = 13
    static let thinkA = 14
    static let think = 15
    static let legsOpen = 16
    static let upperRot = 17
    static let headRot = 18
    static let headDy = 19
    static let bodyDx = 20
    static let bodyDy = 21
    static let lift = 22
    static let squash = 23
    static let earL = 24
    static let earR = 25
    static let eyeScale = 26
    static let gazeX = 27
    static let gazeY = 28
    static let blush = 29
    static let brows = 30
    static let balloonA = 31
    static let balloonSway = 32
    static let abacusA = 33
    static let phoneA = 34
    static let flameA = 35
    static let flameH = 36
    static let calA = 37
    static let jetA = 38
    static let capeA = 39
    static let crownA = 40
    static let frontLX = 41
    static let frontRX = 42
    static let frontRY = 43
    static let boardA = 44
    static let noteA = 45
    static let glassesA = 46
    static let wandA = 47
    static let alarmA = 48
    static let tilt = 49
    static let kickL = 50
    static let kickR = 51
    static let barbellA = 52
    static let skateA = 53
    static let guitarA = 56
    static let juggleA = 57
    static let starsA = 58
    static let browsDet = 59
    static let frontLY = 60
    static let hatA = 61
    static let hatDy = 62
    static let holsterA = 63
    static let hcarrotA = 64
    static let hcarrotRot = 65
    static let squint = 66
    static let cheeks = 67
    static let headbandA = 68
    static let kboardA = 69
    static let maskA = 70
    static let spyglassA = 75
    static let rapA = 78
    static let headPitch = 79
    static let frontLRot = 80
    static let jesterA = 81
    static let sayi = 82
}
