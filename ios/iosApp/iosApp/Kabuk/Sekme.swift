import SwiftUI

// Alt gezinme çubuğunun sekmeleri. Sıra Android res/menu/bottom_menu.xml ile aynıdır.
enum Sekme: Int, CaseIterable, Identifiable {
    case harita      // R.id.map -> PartSelectionFragment / MapFragment
    case profil      // R.id.profile -> ProfileFragment
    case gorevler    // R.id.tasks -> MissionsFragment
    case magaza      // R.id.shop -> AbacusCustomizationFragment
    case bildirim    // R.id.notification -> NotificationFragment
    case kesfet      // R.id.explore -> TasksFragment

    var id: Int { rawValue }

    // Asset catalog'daki görsel adı (Android drawable adıyla aynı).
    var simgeAdi: String {
        switch self {
        case .harita: return "home_ic"
        case .profil: return "profile_ic2"
        case .gorevler: return "tasks_ic"
        case .magaza: return "shop_ic"
        case .bildirim: return "chat_ic"
        case .kesfet: return "explore_ic"
        }
    }

    // Android menüsünde sekme başlığı yoktur (yalnızca simge); bu adlar yer tutucu ekranda
    // ve erişilebilirlik etiketinde kullanılır.
    var ad: String {
        switch self {
        case .harita: return "Harita"
        case .profil: return "Profil"
        case .gorevler: return "Görevler"
        case .magaza: return "Mağaza"
        case .bildirim: return "Bildirimler"
        case .kesfet: return "Keşfet"
        }
    }

    // Üst çubuk (cüzdan/enerji paneli) bu sekmede görünür mü.
    // Android: MainActivity.updateCurrencyPanelVisibility - yalnızca MapFragment,
    // PartSelectionFragment, TasksFragment ve MissionsFragment açıkken görünür.
    var ustCubukGorunur: Bool {
        switch self {
        case .harita, .gorevler, .kesfet: return true
        case .profil, .magaza, .bildirim: return false
        }
    }
}
